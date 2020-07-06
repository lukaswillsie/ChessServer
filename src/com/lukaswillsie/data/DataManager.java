package com.lukaswillsie.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.lukaswillsie.data.Game.InvalidGameDataException;
import com.lukaswillsie.protocol.Protocol;
import com.lukaswillsie.utility.Log;

import Chess.com.lukaswillsie.chess.Board;
import Chess.com.lukaswillsie.chess.Pair;

/**
 * This class is responsible for managing the details of user's network requests,
 * and managing the server's local records in an efficient manner.
 * 
 * It does this by keeping in memory data about every single game in the system,
 * and every single user in the system. That way, when a user wants to make a move
 * or create a new game, it can speedily process the request and get back with a
 * response. However, to ensure that changes are persistent, we need to update the
 * server's local records occasionally. I chose to do so regularly, after a fixed number
 * of data-altering requests. This way, the average amount of time spent opening and writing
 * to files is lowered on a per-request basis.
 * 
 * This object also ensures that server requests are atomic, so that we don't see strange
 * behaviour if two requests come in close together and try to interact with the same data,
 * or interact with the same file at the same time.
 * 
 * @author Lukas Willsie
 *
 */
public class DataManager implements AccountManager {	
	private static final String accountsFile = "serverdata/accounts.csv";
	private static final String gamesFile = "serverdata/active_games.csv";
	private static final String gamesDir = "serverdata/games";
	private static final String newGameTemplate = "serverdata/games/standard/new_board.txt";
	private static final String templateFolder = "serverdata/games/standard";
	
	/**
	 * A collection of every game in our system, where keys are game IDs and values
	 * are the corresponding game objects.
	 */
	private HashMap<String, Game> games = new HashMap<String, Game>();
	
	/**
	 * A list of every single user account that the server has on record, sorted lexicographically,
	 * by username
	 */
	private List<User> users = new ArrayList<User>();
	
	/**
	 * Maps each user, via their username, to a list of every game that the user is
	 * a player in
	 */
	private HashMap<String, List<Game>> userGames = new HashMap<String, List<Game>>();
	
	/**
	 * A list of games that have had their boards altered through moves or promotions,
	 * but haven't had the changes saved yet
	 */
	private List<Game> unsavedGames = new ArrayList<Game>();
	
	/**
	 * Tracks the total number of data-altering requests made since the last save
	 */
	private int requestsMade = 0;
	
	/**
	 * How many data-altering requests we will let go by before attempting to update our local
	 * records
	 */
	private static final int REQUESTS_BEFORE_SAVE = 20;
	
	/**
	 * Keeps track of whether our list of users has changed SINCE THE LAST SAVE, for example via the
	 * creation of a new account
	 */
	private boolean usersChanged = false;
	
	/**
	 * Keeps track of whether any high-level game data has been changed SINCE THE LAST SAVE. High-level
	 * game data is the data that is stored in the .csv file containing a list of all games in the system.
	 */
	private boolean gamesChanged = false;
	
	/**
	 * A class used solely for the purpose of searching our list of users. When instantiated
	 * with a username, it becomes a User object with that username and an empty password.
	 * Can be used to take advantage of the Collections.binarySearch() method, since User
	 * implements Comparable. I decided to do this instead of implementing my own binary
	 * search because it's quicker and seems to me like it's cleaner.
	 * 
	 * Since User
	 * implements Comparable, we can use
	 * @author lukas
	 *
	 */
	private static class SearchUser extends User {
		public SearchUser(String username) {
			super(username, "");
		}
	}
	
	/**
	 * Returns the Game object in our records corresponding to the given gameID,
	 * or null if we don't have a record of the given gameID
	 * 
	 * @param gameID - the ID of the game to fetch
	 * @return The Game object corresponding to the given ID, or null if 
	 * we don't have a game for the given gameID in our records
	 */
	private Game getGame(String gameID) {
		// The HashMap get() function returns null if gameID isn't a key in the
		// HashMap, so we don't have to check ourselves
		return games.get(gameID);
	}
	
	/**
	 * Get a list of every Game in the system
	 * 
	 * @return A List of every Game object in the system
	 */
	private List<Game> getGames() {
		List<Game> allGames = new ArrayList<Game>();
		for(String gameID : games.keySet()) {
			allGames.add(games.get(gameID));
		}
		
		return allGames;
	}
	
	/**
	 * Check if the given username is associated with an account in the system.
	 * 
	 * @param username - the username to search for
	 * @return true if and only if there is a User object with the given username in
	 * this object's users field
	 */
	private boolean userExists(String username) {
		User search = new SearchUser(username);
		int result = Collections.binarySearch(users, search);
		return result >= 0;
	}
	
	/**
	 * Sets the DataManager up for use. No DataManager object should be used until
	 * this method has been called and returned a successful return code
	 * 
	 * @return 0 if the build process succeeded and this object is ready for use,
	 * 		   1 if the build process failed for some reason
	 */
	public synchronized int build() {
		// In case this is our first execution, create any requisite files
		createFiles();
		
		Scanner scanner;
		try {
			scanner = new Scanner(new File(accountsFile));
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open accounts file for scanning");
			return 1;
		}
		
		// Process the accounts file
		String line;
		String[] split;
		int lineNumber = 1;
		User user;
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			split = line.split(",");
			
			if(split.length != 2) {
				Log.error("ERROR: Line " + lineNumber + " of accounts file is incorrectly formatted");
				return 1;
			}
			
			// Add the user to our list of users and create an empty list of games for them in
			// userGames
			user = new User(split[0], split[1]);
			addUser(user);
			userGames.put(user.getUsername(), new ArrayList<Game>());
			
			lineNumber++;
		}
		
		try {
			scanner = new Scanner(new File(gamesFile));
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Couldn't open games file for scanning");
			return 1;
		}
		
		// Process the games file, creating a Game object corresponding to each line
		lineNumber = 1;
		String[] data;
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			data = line.split(",");
			
			Game game;
			Board board = getBoard(data[GameData.GAMEID.getColumn()]);
			
			// If there was an error and a Board object couldn't be created for the game
			if(board == null) {
				return 1;
			}
			
			try {
				game = new Game(data, board);
			} catch (InvalidGameDataException e) {
				e.printStackTrace();
				return 1;
			}
			
			// Add the game to our list of games, and add it to both players' lists of games that they're in
			addGame(game);
		}
		
		return 0;
	}	
	
	/**
	 * Try and fetch the data associated with the given game and initialize a Board with it.
	 * When returned, the Board will contain the current state of the given game.
	 * 
	 * @param gameID
	 * @return A Board object containing the current state of the given game. null if an
	 * error occurs and a Board object cannot be built
	 */
	private Board getBoard(String gameID) {
		try {
			Scanner scanner = new Scanner(new File(getFilename(gameID)));
			Board board = new Board();
			int result = board.initialize(scanner);
			
			if(result == 0) {
				return board;
			}
			else {
				Log.error("ERROR: Board object couldn't be initialized from \"" + getFilename(gameID) + "\"");
				return null;
			}
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open board data file for game \"" + gameID + "\"");
			return null;
		}
		
	}
	
	/**
	 * Take a game ID and convert it into a full file path to the board data file for the given
	 * game, ASSUMING the given game exists. 
	 */
	private String getFilename(String gameID) {
		return gamesDir + "/" + gameID + ".txt";
	}
	
	/**
	 * Prints the contents of this DataManager to the console for debugging
	 */
	public void display() {
		System.out.println("Users:");
		for(User user : users) {
			System.out.print(user.getUsername() + ",");
		}
		System.out.println();
		
		System.out.println("Games:");
		for(Game game : getGames()) {
			System.out.print(game.getData(GameData.GAMEID) + ",");
		}
		System.out.println();
		
		System.out.println("User's games:");
		for(String user: userGames.keySet()) {
			System.out.print(user + ": ");
			for(Game game : userGames.get(user)) {
				System.out.print(game.getData(GameData.GAMEID) + ",");
			}
			System.out.println();
		}
	}
	
	/**
	 * Insert the given user into our list of users, maintaining the sortedness of the list
	 * 
	 * @param user - the user to insert
	 */
	private void addUser(User user) {
		// Iterate until i points to a user with a username lexicographically greater than
		// that of user, or the end of the list, and then insert
		int i;
		for(i = 0; i < users.size() && users.get(i).getUsername().compareTo(user.getUsername()) < 0; i++);
		users.add(i, user);
	}
	
	/**
	 * Takes the given Game object and adds it to our collection of Game objects. Adds it to our
	 * HashMap of games, and also ensures that it's added to its player(s) respective lists of
	 * games.
	 * 
	 * @param game - the Game to add to our collection
	 */
	private void addGame(Game game) {
		games.put((String)game.getData(GameData.GAMEID), game);
		String white = (String)game.getData(GameData.WHITE);
		String black = (String)game.getData(GameData.BLACK);
		
		if(white.length() > 0) {
			userGames.get(white).add(game);
		}
		
		if(black.length() > 0) {
			userGames.get(black).add(game);
		}
		
	}
	
	/**
	 * Create all the files and folders that this object needs to exist before it
	 * can work properly. Does not modify files/folders that already exist.
	 * @return
	 */
	private int createFiles() {
		File gamesFolder = new File(gamesDir);
		if(!gamesFolder.exists() && !gamesFolder.mkdirs()) {
			Log.error("ERROR: Couldn't create folder(s) \"" + gamesDir + "\"");
			return 1;
		}
		
		File accounts = new File(accountsFile);
		try {
			// We don't care about createNewFile's return value because regardless,
			// we know that the file has been created the way we want it to
			accounts.createNewFile();
		} catch (IOException e) {
			Log.error("ERROR: Couldn't create accounts file with path \"" + accountsFile + "\"");
			return 1;
		}
		
		File games = new File(gamesFile);
		try {
			games.createNewFile();
		} catch (IOException e) {
			Log.error("ERROR: Couldn't create games file with path \"" + gamesFile + "\"");
			return 1;
		}
		
		return createTemplateFile();
		
	}
	
	/**
	 * Saves all game data we have stored in memory in the proper files. If an IO error occurs, logs the problem and 
	 * dumps what the contents of a file SHOULD BE to the console.
	 */
	private void save() {
		// Save all of our high-level game data in the proper file, if any of it has changed
		if(gamesChanged) {
			try(FileOutputStream gamesOutput = new FileOutputStream(new File(gamesFile))) {
				
				try {
					for(Game game : getGames()) {
						String line = toLine(game);
						gamesOutput.write((line + "\n").getBytes());
					}		
				}
				catch(IOException e) {
					Log.error("ERROR: Problem writing to games file \"" + gamesFile + "\"");
					dumpGames();
				}
			} catch (FileNotFoundException e) {
				Log.error("ERROR: games file \"" + gamesFile + "\" does not exist.");
				dumpGames();
			}
			// Thrown if gamesOutput fails to close
			catch (IOException e) {
				Log.error("ERROR: Couldn't close games FileOutputStream");
				e.printStackTrace();
			}	
		}
		
		// Save all of our user data in the proper file, if the collection has changed
		if(usersChanged) {
			try(FileOutputStream usersOutput = new FileOutputStream(new File(accountsFile))) {
				String line;
				try {
					for(User user : users) {
						line = user.getUsername() + "," + user.getPassword() + "\n";
						usersOutput.write(line.getBytes());
					}
				}
				catch(IOException e) {
					
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			// Thrown if usersOutput fails to close
			catch (IOException e) {
				Log.error("ERROR: Couldn't close accounts FileOutputStream");
				e.printStackTrace();
			}
		}
		
		// Try and save data for all of our as yet unsaved games
		for(Game game : unsavedGames) {
			// Get a File reference for this game's data file
			File file = new File(getFilename((String)game.getData(GameData.GAMEID)));
			try(FileOutputStream stream = new FileOutputStream(file);
				Scanner scanner = new Scanner(file)) {
				try {
					game.getBoard().saveGame(stream);
					// If we successfully save the game's data, we remove it from the list of unsaved games
					unsavedGames.remove(game);
				}
				catch(IOException e) {
					Log.error("ERROR: Couldn't save game data for game \"" + game.getData(GameData.GAMEID) + "\"");
					dumpGame(game);
				}
			}
			catch (FileNotFoundException e) {
				Log.error("ERROR: No game data file for game \"" +  game.getData(GameData.GAMEID) + "\"");
				dumpGame(game);
			} 
			// Thrown if stream fails to close
			catch (IOException e) {
				Log.error("ERROR: Couldn't close FileOutputStream opened on file \"" + getFilename((String)game.getData(GameData.GAMEID)) + "\"");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * If an IO error occurs and we can't save a game's data to disk, we use this method to display
	 * a brief error message and dump the game's save data to the console, so that we have a record
	 * in case subsequent attempts to save it fail
	 * @param game
	 */
	private void dumpGame(Game game) {
		List<String> errorMessage = new ArrayList<String>();
		errorMessage.add("Couldn't save game \"" + (String)game.getData(GameData.GAMEID) + "\"");
		
		List<String> gameSaveData = game.getBoard().getSaveData();
		errorMessage.addAll(gameSaveData);
		Log.error(errorMessage);
	}
	
	/**
	 * If an IO error causes an attempt at saving all our game data to fail, we use this method to display
	 * a brief error message and dump what the contents of our data file SHOULD have been to the console,
	 * that way if the error persists we at least have a safe record of what the state of the system is
	 */
	private void dumpGames() {
		List<String> errorMessage = new ArrayList<String>();
		errorMessage.add("Couldn't save game data. Dumping...");
		for(Game game : getGames()) {
			errorMessage.add(toLine(game));
		}
		Log.error(errorMessage);
	}
	
	/**
	 * If an IO error prevents us from saving an updated list of all registered users, we use
	 * this method to display a brief error message and dump what the contents of our records
	 * file SHOULD be, so we can go back later and update our file, if necessary.
	 */
	private void dumpUsers() {
		List<String> errorMessage = new ArrayList<String>();
		errorMessage.add("Couldn't save users data. Dumping...");
		for(User user : this.users) {
			errorMessage.add(user.getUsername() + "," + user.getPassword());
		}
		Log.error(errorMessage);
	} 
	
	/**
	 * Convert the data contained in the given Game object into a line of the active_games.csv
	 * file. In the order specified by GameData.order, place the values contained in the object
	 * in a String, separated by commas
	 * 
	 * @param game - the game that will be converted into a String
	 */
	private String toLine(Game game) {
		StringBuilder line = new StringBuilder();
		
		// Add each value but the last with a ',' directly after them
		for(int i = 0; i < GameData.order.length - 1; i++) {
			GameData data = GameData.order[i];
			line.append(game.getData(data).toString() + ",");
		}
		
		// Add the last value without a ',' after it
		line.append(game.getData(GameData.order[GameData.order.length - 1]).toString());
		
		return line.toString();
	}
	
	/**
	 * Records that another data-altering request has been made, and initiates a save if necessary.
	 */
	private void requestMade() {
		this.requestsMade++;
		if(this.requestsMade >= REQUESTS_BEFORE_SAVE) {
			save();
			this.requestsMade = 0;
		}
	}
	
	/**
	 * Create the "new board" file, a template used to initialize newly-created game files
	 * 
	 * @return 0 on success, 1 on failure
	 */
	private int createTemplateFile() {
		String[] lines = {
		"0",
		"0",
		"0",
		"0",
		"rnbqkbnr",
		"pppppppp",
		"xxxxxxxx",
		"xxxxxxxx",
		"xxxxxxxx",
		"xxxxxxxx",
		"PPPPPPPP",
		"RNBQKBNR",
		"0"
		};
		
		File standardFolder = new File(templateFolder);
		if(!standardFolder.exists() && !standardFolder.mkdirs()) {
			Log.error("ERROR: Couldn't create folder(s) \"" + templateFolder + "\""); 
			return 1;
		}
		
		File template = new File(newGameTemplate);
		try {
			template.createNewFile();
		} catch (IOException e) {
			Log.error("ERROR: Couldn't create file \"" + newGameTemplate + "\"");
			return 1;
		}
		
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(template);
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open new game template file");
			return 1;
		}
		for(String line : lines) {
			try {
				stream.write((line + "\n").getBytes());
			} catch (IOException e) {
				Log.error("ERROR: Couldn't write to and initialize new game template file");
				return 1;
			}
		}
		
		return 0;
	}
	
	/**
	 * Return a List of all games the given user is participating in.
	 * Each game is represented as a Game object, which is basically a wrapper for a HashMap.
	 * Keys take values in the GameData enum, which codifies exactly what pieces of data define
	 * a game. The values are Objects, guaranteed to either be Strings or Integers, and represent
	 * the value taken on by the corresponding key.
	 * 
	 * So (String) game.getData(GameData.GAMEID) == "lukas's" means that the game's ID is "lukas's".
	 * 
	 * Returns null if the given username doesn't correspond to a user in the system
	 * 
	 * @param username - the user whose games this method should fetch
	 * 
	 * @return - A list of Game objects corresponding to all games being played by this user
	 */
	public synchronized List<Game> getGameData(String username) {
		// We've already got a list of games for each user, so all we have to do is get it
		return userGames.get(username);
	}
	
	/**
	 * Create a game with the given gameID under the given user's name.
	 * 
	 * @param gameID - The ID of the game to create
	 * @param username - the user creating the game
	 * @return 	Protocol.SERVER_ERROR 				- an error is encountered <br>
	 * 			Protocol.CreateGame.SUCCESS 		- game created successfully <br>
	 * 			Protocol.CreateGame.GAMEID_IN_USE 	- game already exists and hence cannot be created
	 */
	public synchronized int createGame(String gameID, String username) {
		if(getGame(gameID) != null) {
			return Protocol.CreateGame.GAMEID_IN_USE;
		}
		else if (!userExists(username)) {
			// TODO: Consider adding a return code for this scenario
			Log.error("ERROR: Username \"" + username + "\" is not a valid username. Cannot process request to create game.");
			return Protocol.SERVER_ERROR;
		}
		else {
			/*
			 * We follow these steps in creating a new game:
			 * 
			 * 1. Create a board file for the game
			 * 2. Create Board object from the file
			 * 3. Create a Game object and add it to our records
			 */
			File file = new File(getFilename(gameID));
			boolean created;
			try {
				created = file.createNewFile();
			} catch (IOException e) {
				Log.error("ERROR: Couldn't create file for new game \"" + gameID + "\".");
				return Protocol.SERVER_ERROR;
			}
			// This means the file already exists, but we don't have a Game object for it, because of the
			// check we already did above, which is a problem
			if(!created) {
				Log.error("ERROR: There is no record of game \"" + gameID + "\" but there is a board data file.");
				return Protocol.SERVER_ERROR;
			}
			else {
				// Initialize the newly-created file from our new game template file
				try {
					FileOutputStream stream = new FileOutputStream(file);
					Scanner scanner = new Scanner(newGameTemplate);
					
					try {
						String line;
						while(scanner.hasNextLine()) {
							line = scanner.nextLine();
							stream.write((line + "\n").getBytes());
						}
					}
					catch(IOException e) {
						Log.error("ERROR: Couldn't initialize board data file for game \"" + gameID + "\" from template.");
						// We delete the board data file we were going to use because we don't want it sticking
						// around if we weren't able to initialize it
						if(!file.delete()) {
							Log.error("ERROR: Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
						}
						try {
							stream.close();
						} catch (IOException e1) {
							Log.error("ERROR: Couldn't close a FileOutputStream.");
							e1.printStackTrace();
						}
						scanner.close();
						return Protocol.SERVER_ERROR;
					}
					scanner.close();
					try {
						stream.close();
					} catch (IOException e) {
						Log.error("ERROR: Couldn't close a FileOutputStream.");
						e.printStackTrace();
					}
				}
				// Thrown if we can't open our newly-created file or the new game template
				catch (FileNotFoundException e) {
					Log.error("ERROR: Couldn't open either the file \"" + getFilename(gameID) + "\" or the file \"" + newGameTemplate + "\"");
					// Since we couldn't initialize the file, we delete it
					if(!file.delete()) {
						Log.error("ERROR: Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
					}
					return Protocol.SERVER_ERROR;
				}
			}
			
			// Now that we've created and initialized our new file, we just need to create a Game object for it and add it to
			// the records we're keeping in memory.
			Board board;
			try {
				Scanner scanner = new Scanner(file);
				board = new Board();
				board.initialize(scanner);
			} catch (FileNotFoundException e) {
				Log.error("ERROR: Couldn't open newly-created and initialized file \"" + getFilename(gameID) + "\" for scanning.");
				if(!file.delete()) {
					Log.error("ERROR: Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
				}
				return Protocol.SERVER_ERROR;
			}
			
			Game game = new Game(gameID, board);
			// Whoever creates the game plays white
			game.setData(GameData.WHITE, username);
			addGame(game);
			
			requestMade();
			return Protocol.CreateGame.SUCCESS;
		}
	}

	public int joinGame(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int canLoadGame(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Object> loadGame(String gameID) {
		// TODO Auto-generated method stub
		return null;
	}

	public int makeMove(String gameID, Pair src, Pair dest, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int promote(String gameID, char charRep, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int draw(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int reject(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int forfeit(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int archive(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int restore(String gameID, String username) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int validCredentials(String username, String password) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int usernameExists(String username) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int addAccount(String username, String password) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean validUsername(String username) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean validPassword(String password) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
