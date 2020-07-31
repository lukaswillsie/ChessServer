package com.lukaswillsie.data;

import java.awt.BufferCapabilities.FlipContents;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.lukaswillsie.data.Game.InvalidGameDataException;
import com.lukaswillsie.protocol.Protocol;
import com.lukaswillsie.utility.Log;

import Chess.com.lukaswillsie.chess.Board;
import Chess.com.lukaswillsie.chess.Colour;
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
public class GameDataManager implements GameManager {	
	private static final String gamesFile = "serverdata/active_games.csv";
	private static final String gamesDir = "serverdata/games";
	private static final String newGameTemplate = "serverdata/games/standard/new_board.txt";
	private static final String templateFolder = "serverdata/games/standard";
	
	/**
	 * Is used to access information about user accounts; namely if particular
	 * usernames are associated with actual users in the system
	 */
	private AccountDataManager accountManager;
	
	/**
	 * A collection of every game in our system, where keys are game IDs and values
	 * are the corresponding game objects.
	 */
	private HashMap<String, Game> games = new HashMap<String, Game>();
	
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
	 * A list of all open games. Open games are games that all users can see and join,
	 * without having to be given the gameID by the game's creator.
	 */
	private List<Game> openGames = new ArrayList<Game>();
	
	/**
	 * Tracks the total number of data-altering requests made since the last save
	 */
	private int requestsMade = 0;
	
	/**
	 * How many data-altering requests we will let go by before attempting to update our local
	 * records
	 */
	private static final int REQUESTS_BEFORE_SAVE = 20;
	
	private class SaveThread extends Thread {
		@Override
		public void run() {
			Log.log("Saving game data on exit...");
			save();
			Log.log("Finished saving game data");
		}
	}
	
	/**
	 * Create a new GameDataManager object. Note that this object shouldn't be used
	 * until build() has been called and has returned a successful code. This object
	 * will use the given AccountDataManager, which should already have been built at
	 * the time it's passed to this constructor, to query the system's user records, and
	 * make decisions regarding whether or not a given username exists.
	 * 
	 * @param accountManager - the object that THIS object will use to query the system's
	 * records of user accounts
	 */
	public GameDataManager(AccountDataManager accountManager) {
		this.accountManager = accountManager;
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
	 * Check if it is the given user's turn in the given game.
	 * 
	 * @param game - the game to examine
	 * @param username - the user to look for
	 * @return true if and only if it is the given user's turn in the given game
	 */
	private boolean isUserTurn(Game game, String username) {
		// Check if the user is white and it's white's turn, or the user is black and it's black's turn
		return (((String)game.getData(GameData.WHITE)).equals(username) && ((Integer)game.getData(GameData.STATE)) == 0)
			|| (((String)game.getData(GameData.BLACK)).equals(username) && ((Integer)game.getData(GameData.STATE)) == 1);
	}

	/**
	 * Check if the given user is a player in the given game
	 * @param game - the game to examine
	 * @param username - the user to look for
	 * @return true if and only if the given user is in the given game
	 */
	private boolean userInGame(Game game, String username) {
		return ((String)game.getData(GameData.WHITE)).equals(username)
			|| ((String)game.getData(GameData.BLACK)).equals(username);
	}

	/**
	 * Check whether the given game is over, meaning that one of the players won or
	 * the players agreed to a draw.
	 * 
	 * @param game - the game to examine
	 * @return true if and only if the given game is over, by draw or by victory
	 */
	private boolean gameIsOver(Game game) {
		return ((String)game.getData(GameData.WINNER)).length() != 0 || ((Integer)game.getData(GameData.DRAWN)) == 1;
	}

	/**
	 * Check whether the given game is full, meaning it already has two players.
	 * 
	 * @param game - the game to check
	 * @return true if and only if the given game is full
	 */
	private boolean gameIsFull(Game game) {
		return ((String)game.getData(GameData.WHITE)).length() > 0 && ((String)game.getData(GameData.BLACK)).length() > 0;
	}
	
	/**
	 * Switch whose turn it is in the given game
	 * 
	 * @param game - the Game object whose turn counter should be switched
	 */
	private void switchTurn(Game game) {
		if((Integer) game.getData(GameData.STATE) == 1) {
			game.setData(GameData.STATE, 0);
		}
		else {
			game.setData(GameData.STATE, 1);
		}
	}
	
	/**
	 * Check if the given username is associated with an account in the system.
	 * 
	 * @param username - the username to search for
	 * @return true if and only if the given username is associated with a user in the system
	 */
	private boolean userExists(String username) {
		return accountManager.usernameExists(username);
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
			scanner = new Scanner(new File(gamesFile));
		}
		catch(FileNotFoundException e) {
			Log.error("Couldn't open games file for scanning");
			return 1;
		}
		
		// Process the games file, creating a Game object corresponding to each line
		String line;
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
		
		
		Runtime.getRuntime().addShutdownHook(new SaveThread());
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
	 * Takes the given Game object and adds it to our collection of Game objects. Adds it to our
	 * HashMap of games, ensures that it's added to its player(s) respective lists of
	 * games, and adds it to our list of open games if it's an open game.
	 * 
	 * @param game - the Game to add to our collection
	 */
	private void addGame(Game game) {
		games.put((String)game.getData(GameData.GAMEID), game);
		String white = (String)game.getData(GameData.WHITE);
		String black = (String)game.getData(GameData.BLACK);
		
		// If the game is an open game, add it to our list
		if((Integer) game.getData(GameData.OPEN) == 1) {
			openGames.add(game);
		}
		
		// If there's a white player
		if(white.length() > 0) {
			// If we don't already have a list of games that the white player is in,
			// we have to create one for him and add the current game to it
			if(userGames.get(white) == null) {
				ArrayList<Game> whiteGames = new ArrayList<Game>();
				whiteGames.add(game);
				userGames.put(white, whiteGames);
			}
			// Otherwise, just add the new game to the list that already exists
			else {
				userGames.get(white).add(game);
			}
		}
		
		// If there's a black player
		if(black.length() > 0) {
			// If we don't already have a list of games that the black player is in,
			// we have to create one for him and add the current game to it
			if(userGames.get(black) == null) {
				ArrayList<Game> blackGames = new ArrayList<Game>();
				blackGames.add(game);
				userGames.put(black, blackGames);
			}
			// Otherwise, just add the new game to the list that already exists
			else {
				userGames.get(black).add(game);
			}
		}
		
	}
	
	/**
	 * Create all the files and folders that this object needs to exist before it
	 * 
	 * can work properly. Does not modify files/folders that already exist.
	 * @return 0 on success, 1 on failure
	 */
	private int createFiles() {
		File gamesFolder = new File(gamesDir);
		if(!gamesFolder.exists() && !gamesFolder.mkdirs()) {
			Log.error("Couldn't create folder(s) \"" + gamesDir + "\"");
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
	private synchronized void save() {
		// Save all of our high-level game data in the proper file, if any of it has changed
		try(FileOutputStream gamesOutput = new FileOutputStream(new File(gamesFile))) {
			
			try {
				for(Game game : getGames()) {
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
		
		List<String> gameSaveData = game.getBoard().getSaveFile();
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
			Log.log("Initiating automatic save...");
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
				Log.error("Couldn't write to and initialize new game template file");
				try {
					stream.close();
				} catch (IOException e1) {
					Log.error("Couldn't close new game template file's stream");
				}
				return 1;
			}
		}
		
		try {
			stream.close();
		} catch (IOException e) {
			Log.error("Couldn't close new game template file's stream");
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
	 * @param username - the username of the user whose game data is desired. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If this condition isn't satisfied, returns null.
	 * 
	 * @return - A list of Game objects corresponding to all games being played by this user
	 */
	public synchronized List<Game> getGames(String username) {
		// If the given username corresponds to a user in the system
		if(accountManager.usernameExists(username)) {
			// If we don't have any games for the current user, return an empty list
			List<Game> games = userGames.get(username);
			if(games == null) {
				List<Game> list = new ArrayList<Game>();
				userGames.put(username, list);
				return list;
			}
			else {
				return games;
			}
		}
		else {
			return null;
		}
	}
	
	/**
	 * Create a game with the given gameID under the given user's name.
	 * 
	 * @param gameID - The ID of the game to create
	 * @param username - the username of the user trying to create the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * @param open - whether or not the game to be created is "open", meaning all users can view and
	 * join it if they want
	 * 
	 * @return 	Protocol.SERVER_ERROR 				- an error is encountered <br>
	 * 			Protocol.CreateGame.SUCCESS 		- game created successfully <br>
	 * 			Protocol.CreateGame.GAMEID_IN_USE 	- game already exists and hence cannot be created
	 */
	public synchronized int createGame(String gameID, String username, boolean open) {
		if(getGame(gameID) != null) {
			return Protocol.CreateGame.GAMEID_IN_USE;
		}
		else if (!userExists(username)) {
			Log.error("Username \"" + username + "\" is not a valid username. Cannot process request to create game.");
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
				Log.error("Couldn't create file for new game \"" + gameID + "\".");
				return Protocol.SERVER_ERROR;
			}
			// This means the file already exists, but we don't have a Game object for it, because of the
			// check we already did above, which is a problem
			if(!created) {
				Log.error("There is no record of game \"" + gameID + "\" but there is a board data file.");
				return Protocol.SERVER_ERROR;
			}
			else {
				// Initialize the newly-created file from our new game template file
				try {
					FileOutputStream stream = new FileOutputStream(file);
					Scanner scanner = new Scanner(new File(newGameTemplate));
					
					try {
						String line;
						while(scanner.hasNextLine()) {
							line = scanner.nextLine();
							stream.write((line + "\n").getBytes());
						}
					}
					catch(IOException e) {
						Log.error("Couldn't initialize board data file for game \"" + gameID + "\" from template.");
						// We delete the board data file we were going to use because we don't want it sticking
						// around if we weren't able to initialize it
						if(!file.delete()) {
							Log.error("Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
						}
						try {
							stream.close();
						} catch (IOException e1) {
							Log.error("Couldn't close a FileOutputStream.");
							e1.printStackTrace();
						}
						scanner.close();
						return Protocol.SERVER_ERROR;
					}
					scanner.close();
					try {
						stream.close();
					} catch (IOException e) {
						Log.error("Couldn't close a FileOutputStream.");
						e.printStackTrace();
					}
				}
				// Thrown if we can't open our newly-created file or the new game template
				catch (FileNotFoundException e) {
					Log.error("Couldn't open either the file \"" + getFilename(gameID) + "\" or the file \"" + newGameTemplate + "\"");
					// Since we couldn't initialize the file, we delete it
					if(!file.delete()) {
						Log.error("Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
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
				Log.error("Couldn't open newly-created and initialized file \"" + getFilename(gameID) + "\" for scanning.");
				if(!file.delete()) {
					Log.error("Couldn't delete created but un-initialized file \"" + getFilename(gameID) + "\"");
				}
				return Protocol.SERVER_ERROR;
			}
			
			Game game = new Game(gameID, board);
			// Whoever creates the game plays white
			game.setData(GameData.WHITE, username);
			
			if(open) {
				game.setData(GameData.OPEN, 1);
			}
			addGame(game);
			
			requestMade();
			return Protocol.CreateGame.SUCCESS;
		}
	}

	/**
	 * Try to have the given user join the game with the given gameID
	 * 
	 * @param gameID - the ID of the game to join
	 * @param username - the username of the user trying to join the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					- an error is encountered <br>
	 * 			Protocol.JoinGame.SUCCESS 				- game joined successfully <br>
	 * 			Protocol.JoinGame.GAME_DOES_NOT_EXIST 	- game does not exist <br>
	 * 			Protocol.JoinGame.USER_ALREADY_IN_GAME 	- the user has already joined that game <br>
	 * 			Protocol.JoinGame.GAME_FULL 			- game is already full
	 */
	public synchronized int joinGame(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not associated with a user in the system. Cannot join a game.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.JoinGame.GAME_DOES_NOT_EXIST;
		}
		else if(userInGame(game, username)) {
			return Protocol.JoinGame.USER_ALREADY_IN_GAME;
		}
		else if(gameIsFull(game)) {
			return Protocol.JoinGame.GAME_FULL;
		}
		
		else {
			game.setData(GameData.BLACK, username);
			
			// Add game to the user's list of games
			List<Game> games = userGames.get(username);
			// If the user isn't in a game yet, this will happen
			if(games == null) {
				games = new ArrayList<Game>();
				games.add(game);
				userGames.put(username, games);
			}
			else {
				games.add(game);
			}
			
			// If the game was open, we mark it as closed
			boolean removed = openGames.remove(game);
			if(removed) {
				game.setData(GameData.OPEN, 0);
			}
			
			requestMade();
			return Protocol.JoinGame.SUCCESS;
		}
	}

	/**
	 * Checks whether or not it's appropriate for the given user to load the given game.
	 * In particular, checks that the given game exists and that this client's user is a player in the game.
	 * 
	 * THIS METHOD SHOULD BE CALLED before calling loadGame().
	 * 
	 * @param gameID - The game to check
	 * @param username - the username of the user who might want to load the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered <br>
	 * 			Protocol.LoadGame.SUCCESS 				- if and only if the user is a player in the given game, which exists <br>
	 * 		   	Protocol.LoadGame.GAME_DOES_NOT_EXIST	- if the given game does not exist <br>
	 * 		    Protocol.LoadGame.USER_NOT_IN_GAME 		- if the user is not in the given game
 	 */
	public synchronized int canLoadGame(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not associated with a user in the system. Cannot load a game.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.JoinGame.GAME_DOES_NOT_EXIST;
		}
		else if(userInGame(game, username)) {
			return Protocol.LoadGame.SUCCESS;
		}
		else {
			return Protocol.LoadGame.USER_NOT_IN_GAME;
		}
	}

	/**
	 * Return the state of the board in the given game. To see a detailed explanation of how board
	 * data is stored, see Data.pdf in the ChessServer root directory. All you really need to know
	 * for this method is that the board is represented essentially as a file, with lines of this file
	 * containing either Strings or ints.
	 * 
	 * What this method should do is access the board data associated with the game that has the
	 * provided gameID, and return each line of the file, in order, as either a String or Integer, according
	 * to the protocol detailed in the "Loading a game" section of Protocol.pdf. Once you look at
	 * how board data is stored, it is self-evident which lines are treated as Integers and which as Strings.
	 * 
	 * The List returned by this method is guaranteed to consist only of Integers and Strings.
	 * 
	 * Returns null if the given gameID does not have any board data (that is, there's no game with the given
	 * ID in the system)
	 * 
	 * @param gameID - The game to load
	 * 
	 * @return  A List of Strings and Integers, representing the given gameID's board data, or null if the given game
	 * is not in the system or the given user is not in the system
	 */
	public synchronized List<Object> loadGame(String gameID) {
		Game game = getGame(gameID);
		if(game == null) {
			Log.error("Game \"" + gameID + "\" cannot be loaded as it is not in the system");
			return null;
		}
		
		return game.getBoard().getSaveData();
	}
	
	/**
	 * Return the game object associated with the given gameID. Returns null if the given gameID isn't associated with
	 * a game in the system
	 * 
	 * @param gameID - the ID of the game to be searched for
	 * @return The Game object associated with the given ID, or null if no such Game exists
	 */
	@Override
	public Game getGameData(String gameID) {
		return getGame(gameID);
	}

	/**
	 * Get a list of all open games in the system. Returns null on error.
	 * 
	 * @return A list of every single open game in the system, or null if there is
	 * an error accessing this information.
	 */
	public synchronized List<Game> openGames() {
		return openGames;
	}
	
	/**
	 * Try and make the given move in the given game. src is the square occupied by the piece
	 * making the move, and dest is the square it is moving to. This method returns a variety of
	 * integers to represent various possible problems with the move command.
	 * 
	 * @param gameID - The game to try and make the move in
	 * @param src - The square the piece that is moving occupies
	 * @param dest - The square that the piece is moving to
	 * @param username - the username of the user trying to make the move in the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR					- if an error is encountered
	 * 			Protocol.Move.SUCCESS					- if the move is successfully made, and the game records are properly updated <br>
	 *			Protocol.Move.SUCCESS_PROMOTION_NEEDED	- if the move is successfully made, and the game records are properly updated, <br>
	 *													  and now the user needs to promote a pawn (i.e. their turn isn't over yet) <br>			
	 *			Protocol.Move.GAME_DOES_NOT_EXIST		- if the given game does not exist <br>
	 *			Protocol.Move.USER_NOT_IN_GAME			- if the user is not in the given game <br>
	 *			Protocol.Move.NO_OPPONENT				- if the user is in the given game, but does not have an opponent yet <br>
	 *			Protocol.Move.GAME_IS_OVER				- if the given game is already over <br>
	 *			Protocol.Move.NOT_USER_TURN				- if it is not the user's turn to make a move <br>
	 *			Protocol.Move.HAS_TO_PROMOTE			- if it is the user's turn, but they have to promote a pawn rather than make a normal move <br>
	 *			Protocol.Move.RESPOND_TO_DRAW			- if is is the user's turn, but they have to respond to a draw offer <br>
	 *			Protocol.Move.MOVE_INVALID				- if the given move is invalid (for example, the selected piece can't move to the selected square) <br>
	 */
	public synchronized int makeMove(String gameID, Pair src, Pair dest, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not associated with a user in the system. Cannot make a move.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Move.GAME_DOES_NOT_EXIST;
		}
		else if(!userInGame(game, username)) {
			return Protocol.Move.USER_NOT_IN_GAME;
		}
		// If the user does not have an opponent
		else if(((String)game.getData(GameData.BLACK)).length() == 0
			 || ((String)game.getData(GameData.WHITE)).length() == 0) {
			return Protocol.Move.NO_OPPONENT;
		}
		else if(gameIsOver(game)) {
			return Protocol.Move.GAME_IS_OVER;
		}
		else if(!isUserTurn(game, username)) {
			return Protocol.Move.NOT_USER_TURN;
		}
		else if ((Integer) game.getData(GameData.DRAW_OFFERED) == 1) {
			return Protocol.Move.RESPOND_TO_DRAW;
		}
		
		int result = game.getBoard().move(src, dest);
		switch(result) {
			// -1 means that the move was successful, and that a promotion is now required.
			// i.e. it is still the user's turn. 0 means a normal move was successful
			case -1:
				Colour colour = (Integer) game.getData(GameData.STATE) == 1 ? Colour.BLACK : Colour.WHITE;
				// The move could have brought checkmate
				if(game.getBoard().isCheckmate(colour)) {
					game.setData(GameData.WINNER, username);
				}
				
				unsavedGames.add(game);
				requestMade();
				return Protocol.Move.SUCCESS_PROMOTION_NEEDED;
			case 0:
				// Increment the turn counter if the user, who just moved, is black
				colour = (Integer) game.getData(GameData.STATE) == 1 ? Colour.BLACK : Colour.WHITE;
				if(colour == Colour.BLACK) {
					game.setData(GameData.TURN, (Integer)game.getData(GameData.TURN) + 1);
				}
				
				// The move could have brought checkmate or stalemate
				if(game.getBoard().isCheckmate(colour)) {
					game.setData(GameData.WINNER, username);
				}
				else if(game.getBoard().isStalemate()) {
					game.setData(GameData.DRAWN, 1);
				}
				else {
					// Make it the other player's turn now that we know the game isn't over
					switchTurn(game);
				}
				
				unsavedGames.add(game);
				requestMade();
				return Protocol.Move.SUCCESS;
			case 1:
				return Protocol.Move.MOVE_INVALID;
			case 2:
				return Protocol.Move.NOT_USER_TURN;
			// The only other case is 3, which means the user has to promote rather than make a normal move
			default:
				return Protocol.Move.HAS_TO_PROMOTE;
		}
	}
	
	/**
	 * Attempt to promote a pawn to the piece given by charRep, in the given game, on behalf of the given user. 
	 * 
	 * @param gameID - The game in which to try to make the promotion
	 * @param charRep - A character denoting which piece to upgrade into. One of 'r', 'n', 'b', or 'q'.
	 * @param username - the username of the user trying to make the move in the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered
	 *			Protocol.Promote.SUCCESS 				- if promotion is successful
	 *			Protocol.Promote.GAME_DOES_NOT_EXIST 	- if given game does not exist
	 *			Protocol.Promote.USER_NOT_IN_GAME 		- if the user isn't a player in the given game
	 *			Protocol.Promote.NO_OPPONENT			- if the user doesn't have an opponent yet in the game
	 *			Protocol.Promote.GAME_IS_OVER			- if the given game is already over
	 *			Protocol.Promote.NOT_USER_TURN 			- if it's not the user's turn
	 *			Protocol.Promote.NO_PROMOTION 			- if no promotion is required
	 *			Protocol.Promote.CHAR_REP_INVALID 		- if the given charRep is not valid
	 */
	public synchronized int promote(String gameID, char charRep, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot promote.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Promote.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Promote.USER_NOT_IN_GAME;
		}
		// We know the given user is already in the game, so this is a check that they have an opponent
		else if (!gameIsFull(game)) {
			return Protocol.Promote.NO_OPPONENT;
		}
		else if (gameIsOver(game)) {
			return Protocol.Promote.GAME_IS_OVER;
		}
		else if (!isUserTurn(game, username)) {
			return Protocol.Promote.NOT_USER_TURN;
		}
		
		int result = game.getBoard().promote(charRep);
		switch(result) {
			case 0:
				Colour colour = (Integer) game.getData(GameData.STATE) == 1 ? Colour.BLACK : Colour.WHITE;
				// If it's black's turn, increment the turn counter
				if(colour == Colour.BLACK) {
					game.setData(GameData.TURN, (Integer)game.getData(GameData.TURN) + 1);
				}
				
				// It's possible that the user who just promoted actually checkmated their opponent
				// via the promotion
				if(game.getBoard().isCheckmate(colour)) {
					game.setData(GameData.WINNER, username);
				}
				
				switchTurn(game);
				requestMade();
				return Protocol.Promote.SUCCESS;
			case 1:
				return Protocol.Promote.NO_PROMOTION;
			// The only other return code means that the given char rep was invalid
			default:
				return Protocol.Promote.CHAR_REP_INVALID;
		}
	}
	
	/**
	 * Attempt to offer/accept a draw on behalf of the given user in the given game.
	 * 
	 * @param gameID - the game in which to offer/accept a draw
	 * @param username - the username of the user trying to offer/accept the draw. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 				- if an error is encountered  <br>
	 *			Protocol.Draw.SUCCESS 				- if draw offer/accept is successful  <br>
	 *			Protocol.Draw.GAME_DOES_NOT_EXIST 	- if given game does not exist  <br>
	 *			Protocol.Draw.USER_NOT_IN_GAME 		- if the user isn't a player in the given game  <br>
	 *			Protocol.Draw.NO_OPPONENT 			- if the user doesn't have an opponent in the given game yet  <br>
	 *			Protocol.Draw.GAME_IS_OVER			- if the given game is already over
	 *			Protocol.Draw.NOT_USER_TURN 		- if it's not the user's turn in the given game
	 */
	public synchronized int draw(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot offer/accept a draw.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Draw.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Draw.USER_NOT_IN_GAME;
		}
		// Since we know the user is in the game, this is a check if they have an opponent
		else if (!gameIsFull(game)) {
			return Protocol.Draw.NO_OPPONENT;
		}
		else if (gameIsOver(game)) {
			return Protocol.Draw.GAME_IS_OVER;
		}
		else if(!isUserTurn(game, username)) {
			return Protocol.Draw.NOT_USER_TURN;
		}
		
		// If there currently is a draw offer, accept it
		if((Integer) game.getData(GameData.DRAW_OFFERED) == 1) {
			game.setData(GameData.DRAW_OFFERED, 0);
			game.setData(GameData.DRAWN, 1);
		}
		// Otherwise, offer one, and make it the opponent's turn
		else {
			game.setData(GameData.DRAW_OFFERED, 1);
			switchTurn(game);
		}
	
		requestMade();
		return Protocol.Draw.SUCCESS;
	}
	
	/**
	 * Attempt to reject a draw on behalf of the given user in the given game.
	 * 
	 * @param gameID - the game in which to reject a draw
	 * @param username - the username of the user trying to reject the draw. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered  <br>
	 *			Protocol.Reject.SUCCESS 				- if draw offer/accept is successful  <br>
	 *			Protocol.Reject.GAME_DOES_NOT_EXIST 	- if given game does not exist  <br>
	 *			Protocol.Reject.USER_NOT_IN_GAME 		- if the user isn't a player in the given game  <br>
	 *			Protocol.Reject.NO_OPPONENT 			- if the user doesn't have an opponent in the given game yet  <br>
	 *			Protocol.Reject.GAME_IS_OVER			- if the given game is already over <br>
	 *			Protocol.Reject.NOT_USER_TURN 			- if it's not the user's turn in the given game <br>
	 *			Protocol.Reject.NO_DRAW_OFFER			- if there is no draw offer for the user to reject in the given game <br>
	 */
	public synchronized int reject(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot reject a draw.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Reject.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Reject.USER_NOT_IN_GAME;
		}
		// Since we know the user is in the game, this is a check if they have an opponent
		else if (!gameIsFull(game)) {
			return Protocol.Reject.NO_OPPONENT;
		}
		else if (gameIsOver(game)) {
			return Protocol.Reject.GAME_IS_OVER;
		}
		else if(!isUserTurn(game, username)) {
			return Protocol.Reject.NOT_USER_TURN;
		}
		else if ((Integer) game.getData(GameData.DRAW_OFFERED) == 0) {
			return Protocol.Reject.NO_DRAW_OFFER;
		}
		else {
			// Record the rejection and pass the game back to the player who
			// offered the draw
			game.setData(GameData.DRAW_OFFERED, 0);
			switchTurn(game);
			
			requestMade();
			return Protocol.Reject.SUCCESS;
		}
	}
	
	/**
	 * Attempt to forfeit the given game on behalf of the given user.
	 * 
	 * @param gameID - the game to forfeit
	 * @param username - the username of the user trying to forfeit the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return  Protocol.SERVER_ERROR 					- if an error is encountered
	 *			Protocol.Forfeit.SUCCESS				- if forfeiture is successful
	 *			Protocol.Forfeit.GAME_DOES_NOT_EXIST 	- if the given game does not exist
	 *			Protocol.Forfeit.USER_NOT_IN_GAME 		- if the user is not in the given game
	 *			Protocol.Forfeit.NO_OPPONENT 			- if the user does not have an opponent in the given game
	 * 			Protocol.Forfeit.GAME_IS_OVER 			- if the given game is already over
	 *			Protocol.Forfeit.NOT_USER_TURN 			- if it is not the user’s turn
	 */
	public synchronized int forfeit(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot forfeit a game.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Forfeit.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Forfeit.USER_NOT_IN_GAME;
		}
		// Since we know the user is in the game, this is a check if they have an opponent
		else if (!gameIsFull(game)) {
			return Protocol.Forfeit.NO_OPPONENT;
		}
		else if (gameIsOver(game)) {
			return Protocol.Forfeit.GAME_IS_OVER;
		}
		else if(!isUserTurn(game, username)) {
			return Protocol.Forfeit.NOT_USER_TURN;
		}
		
		// Record that the opponent lost the game
		if(((String)game.getData(GameData.WHITE)).equals(username)) {
			game.setData(GameData.WINNER, game.getData(GameData.BLACK));
		}
		else {
			game.setData(GameData.WINNER, game.getData(GameData.WHITE));
		}
		
		requestMade();
		return Protocol.Forfeit.SUCCESS;
	}
	
	/**
	 * Attempt to mark the given game as archived for the given user. Has no effect if the given game
	 * is already archived.
	 * 
	 * @param gameID - the game to mark as archived
	 * @param username - the username of the user trying to archive the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					– if an error is encountered
	 *			Protocol.Archive.SUCCESS 				– if the archive is successful
	 *			Protocol.Archive.GAME_DOES_NOT_EXIST 	– if the given game does not exist
	 *			Protocol.Archive.USER_NOT_IN_GAME 		– if the user is not in the given game
	 */
	public synchronized int archive(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot archive a game.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Archive.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Archive.USER_NOT_IN_GAME;
		}
		
		if(((String)game.getData(GameData.WHITE)).equals(username)) {
			game.setData(GameData.WHITE_ARCHIVED, 1);
		}
		else {
			game.setData(GameData.BLACK_ARCHIVED, 1);
		}
		
		requestMade();
		return Protocol.Archive.SUCCESS;
	}
	
	/**
	 * Attempt to restore, or "un-archive", the given game for the given user. That is, simply mark
	 * it as not archived. Has no effect if the game is already not archived.
	 * 
	 * @param gameID - the game to un-archive
	 * @param username - the username of the user trying to restore the game. This username should
	 * be one associated with a user in the system. In particular, it should satisfy
	 * AccountManager.usernameExists(). So a username associated with a successful login or account
	 * creation is also good to go. If the given username doesn't satisfy this condition, this method
	 * logs the problem and returns Protocol.SERVER_ERROR.
	 * 
	 * @return 	Protocol.SERVER_ERROR 					– if an error is encountered
	 *			Protocol.Restore.SUCCESS 				– if the restoration is successful
	 *			Protocol.Restore.GAME_DOES_NOT_EXIST 	– if the given game does not exist
	 *			Protocol.Restore.USER_NOT_IN_GAME 		– if the user is not in the given game
	 */
	public synchronized int restore(String gameID, String username) {
		if(!userExists(username)) {
			Log.error("Username \"" + username + "\" is not in the system. Cannot archive a game.");
			return Protocol.SERVER_ERROR;
		}
		
		Game game = getGame(gameID);
		if(game == null) {
			return Protocol.Restore.GAME_DOES_NOT_EXIST;
		}
		else if (!userInGame(game, username)) {
			return Protocol.Restore.USER_NOT_IN_GAME;
		}
		
		if(((String)game.getData(GameData.WHITE)).equals(username)) {
			game.setData(GameData.WHITE_ARCHIVED, 0);
		}
		else {
			game.setData(GameData.BLACK_ARCHIVED, 0);
		}
		
		requestMade();
		return Protocol.Restore.SUCCESS;
	}	
}
