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
	 * A list of every single game that the server has on record, sorted lexicographically by gameID.
	 * That is, using String's compareTo() method
	 */
	private List<Game> games = new ArrayList<Game>();
	
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
	 * Sets the DataManager up for use. No DataManager object should be used until
	 * this method has been called and returned a successful return code
	 * 
	 * @return 0 if the build process succeeded and this object is ready for use,
	 * 		   1 if the build process failed for some reason
	 */
	public synchronized int build() {
		// In case this is our first execution, create any requisite files that need creating
		createFiles();
		
		Scanner scanner;
		try {
			scanner = new Scanner(new File(accountsFile));
		} catch (FileNotFoundException e) {
			Log.error("Couldn't open accounts file for scanning");
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
				Log.error("Line " + lineNumber + " of accounts file is incorrectly formatted");
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
			// Throw out the first line of the file because it is just a column header
			System.out.println(scanner.nextLine());
		}
		catch(FileNotFoundException e) {
			Log.error("Couldn't open games file for scanning");
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
			userGames.get((String)game.getData(GameData.WHITE)).add(game);
			userGames.get((String)game.getData(GameData.BLACK)).add(game);
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
				Log.error("Board object couldn't be initialized from \"" + getFilename(gameID) + "\"");
				return null;
			}
		} catch (FileNotFoundException e) {
			Log.error("Couldn't open board data file for game \"" + gameID + "\"");
			return null;
		}
		
	}
	
	/**
	 * Return the path for where the board data file for the given game should be
	 */
	private String getFilename(String gameID) {
		return gamesDir + "/" + gameID + ".txt";
	}
	
	public void display() {
		System.out.println("Users:");
		for(User user : users) {
			System.out.print(user.getUsername() + ",");
		}
		System.out.println();
		
		System.out.println("Games:");
		for(Game game : games) {
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
	 * Insert the given game into our list of games, maintaining the sortedness of the list
	 * 
	 * @param game - the game to insert
	 */
	private void addGame(Game game) {
		// Iterate until i points to a game with a gameID lexicographically greater than
		// that of game, or the end of the list, and then insert
		int i;
		for(i = 0; i < games.size() && ((String)games.get(i).getData(GameData.GAMEID)).compareTo((String)game.getData(GameData.GAMEID)) < 0; i++);
		games.add(i, game);	
	}
	
	/**
	 * Create all the files and folders that this object needs to exist before it
	 * can work properly. Does not modify files/folders that already exist.
	 * @return
	 */
	private int createFiles() {
		File gamesFolder = new File(gamesDir);
		if(!gamesFolder.exists() && !gamesFolder.mkdirs()) {
			Log.error("Couldn't create folder(s) \"" + gamesDir + "\"");
			return 1;
		}
		
		File accounts = new File(accountsFile);
		try {
			// We don't care about createNewFile's return value because regardless,
			// we know that the file has been created the way we want it to
			accounts.createNewFile();
		} catch (IOException e) {
			Log.error("Couldn't create accounts file with path \"" + accountsFile + "\"");
			return 1;
		}
		
		File games = new File(gamesFile);
		try {
			games.createNewFile();
		} catch (IOException e) {
			Log.error("Couldn't create games file with path \"" + gamesFile + "\"");
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
					for(Game game : this.games) {
						String line = toLine(game);
						gamesOutput.write((line + "\n").getBytes());
					}		
				}
				catch(IOException e) {
					Log.error("Problem writing to games file \"" + gamesFile + "\"");
					dumpGames();
				}
			} catch (FileNotFoundException e) {
				Log.error("games file \"" + gamesFile + "\" does not exist.");
				dumpGames();
			}
			// Thrown if gamesOutput fails to close
			catch (IOException e) {
				Log.error("Couldn't close games FileOutputStream");
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
				Log.error("Couldn't close accounts FileOutputStream");
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
					Log.error("Couldn't save game data for game \"" + game.getData(GameData.GAMEID) + "\"");
					dumpGame(game);
				}
			}
			catch (FileNotFoundException e) {
				Log.error("No game data file for game \"" +  game.getData(GameData.GAMEID) + "\"");
				dumpGame(game);
			} 
			// Thrown if stream fails to close
			catch (IOException e) {
				Log.error("Couldn't close FileOutputStream opened on file \"" + getFilename((String)game.getData(GameData.GAMEID)) + "\"");
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
		for(Game game : this.games) {
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
			Log.error("Couldn't create folder(s) \"" + templateFolder + "\""); 
			return 1;
		}
		
		File template = new File(newGameTemplate);
		try {
			template.createNewFile();
		} catch (IOException e) {
			Log.error("Couldn't create file \"" + newGameTemplate + "\"");
			return 1;
		}
		
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(template);
		} catch (FileNotFoundException e) {
			Log.error("Couldn't open new game template file");
			return 1;
		}
		for(String line : lines) {
			try {
				stream.write((line + "\n").getBytes());
			} catch (IOException e) {
				Log.error("Coudln't write to and initialize new game template file");
				return 1;
			}
		}
		
		return 0;
	}
	
	/**
	 * Return a List of all games the given user is participating in.
	 * Each game is represented as a Game object, which is basically a wrapper for a HashMap.
	 * Keys take values in the GameData enum, which defines exactly what pieces of data define
	 * a game. The values are Objects, guaranteed to either be Strings or Integers, and represent
	 * the value taken on by the corresponding key.
	 * 
	 * So (String) game.getData(GameData.GAMEID) == "lukas's" means that the game's ID is "lukas's".
	 * 
	 * Returns null if an error is encountered (for example, if the underlying data cannot be
	 * accessed or has been corrupted in some way).
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
		// TODO Auto-generated method stub
		return 0;
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
