package data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import game.Bishop;
import game.Board;
import game.Colour;
import game.King;
import game.Knight;
import game.Pawn;
import game.Queen;
import game.Rook;
import protocol.Protocol;
import protocol.Protocol.CreateGame;
import protocol.Protocol.JoinGame;
import protocol.Protocol.LoadGame;
import protocol.Protocol.Move;
import utility.Log;
import utility.Pair;

/**
 * This class is responsible for accessing and managing data on behalf of the server
 * for a particular client.
 * 
 * See the GameData enum for details
 * @author lukas
 *
 */
class FileClientManager extends ClientManager {
	// The path to the active_games.csv file relative to the compiled .jar file for this program
	private static final String ACTIVE_GAMES_FILENAME = "serverdata/active_games.csv";
	
	// The path to the directory containing all the game data files relative to the compiled
	// .jar file for this program
	private static final String GAMES_FOLDER = "serverdata/games";
	
	// The path to the file defining exactly what a newly-created game's board data file
	// should look like
	private static final String NEW_BOARD = "serverdata/games/standard/new_board.txt";
	private String username;
	private File active_games;
	private File games_folder;
	
	public static void main(String[] args) {
		FileClientManager test = new FileClientManager("Vaskar");
	}
	
	/**
	 * Create a new FileClientManager on behalf of the user with the given username
	 * @param username - The username to create this FileClientManager for
	 */
	FileClientManager(String username) {
		this.username = username;
		active_games = new File(FileClientManager.ACTIVE_GAMES_FILENAME);
		games_folder = new File(FileClientManager.GAMES_FOLDER);
	}
	
	/**
	 * Return a List of all games this Manager's user is participating in.
	 * Each game is represented by a list of HashMaps. Each
	 * HashMap represents a piece of data about the game, like the ID of the game,
	 * or the name of the user playing white, etc. Keys take values in the GameData
	 * enum, which defines exactly what pieces of data define a game. The values are
	 * Objects, guaranteed to either be Strings or Integers, and represent the value 
	 * taken on by the corresponding key.
	 * 
	 * So the pair {GAMEID: lukas's} means that the game's ID is "lukas's".
	 * 
	 * Each List within the overall List has as many elements as the GameData enum has values.
	 * 
	 * Returns null if an error is encountered (for example, if the underlying data cannot be
	 * accessed or has been corrupted in some way).
	 * 
	 * @return - The array of HashMaps corresponding to all games being played by this user
	 */
	@Override
	public List<HashMap<GameData, Object>> getGameData() {
		List<HashMap<GameData, Object>> games = new ArrayList<HashMap<GameData, Object>>();
		
		Scanner scanner;
		try {
			scanner = new Scanner(this.active_games);
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("ERROR: Could not open active_games file for reading in getGameData().");
			return null;
		}
		
		String line;
		int lineNum = 1;
		String[] data;
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			// Split line of csv into columns
			data = line.split(",");
			
			// We should have as many columns as we do GameData values
			if(data.length != GameData.values().length) {
				Log.error("ERROR: Error in active_games file. Line number " + lineNum + " does not have correct number of columns.");
				scanner.close();
				return null;
			}
			
			// If this object's user is involved in the game
			if(data[GameData.WHITE.getColumn()].equals(this.username)
			|| data[GameData.BLACK.getColumn()].equals(this.username)) {
				// Create a HashMap, then iterate through all GameData values, getting their
				// value from the file, and adding the key,value pair to the HashMap
				HashMap<GameData, Object> game = new HashMap<GameData, Object>();
				for(GameData dataType : GameData.values()) {
					if(dataType.type == 'i') {
						try {game.put(dataType, Integer.parseInt(data[dataType.getColumn()]));}
						catch(NumberFormatException e) {
							Log.error("ERROR: In active_games file, line " + lineNum + ", couldn't cast data " + dataType + " to int");
							scanner.close();
							return null;
						}
					}
					else {
						game.put(dataType, data[dataType.getColumn()]);
					}
				}
				
				games.add(game);
			}
			lineNum++;
		}
		
		
		scanner.close();
		return games;
	}
	
	/**
	 * Create a game with the given gameID under the user's name.
	 * 
	 * @param gameID - The ID of the game to create
	 * @return 	Protocol.SERVER_ERROR 				- an error is encountered <br>
	 * 			Protocol.CreateGame.SUCCESS 		- game created successfully <br>
	 * 			Protocol.CreateGame.GAMEID_IN_USE 	- game already exists and hence cannot be created
	 */
	@Override
	public int createGame(String gameID) {
		try(Scanner scanner = new Scanner(active_games)) {
			String line, id;
			// Read each line of the active_games file one at a time,
			// checking if the game already exists
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				
				int comma = line.indexOf(',');
				if(comma == -1) {
					Log.error("active_games file is incorrectly formatted");
					return Protocol.SERVER_ERROR;
				}
				
				id = line.substring(0, comma);
				if(id.equals(gameID)) {
					return CreateGame.GAMEID_IN_USE;
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("Error opening active_games file.");
			return Protocol.SERVER_ERROR;
		}
		
		try {
			// We've reached the end of the file without finding the given gameID
			// So we can go ahead and create the game
			FileOutputStream out = new FileOutputStream(active_games, true);
			
			// Add the new game to the file
			for(int i = 0; i < GameData.order.length; i++) {
				if(i != 0) {
					out.write(",".getBytes());
				}
				
				// Write the given gameID instead of the default value
				if(GameData.order[i] == GameData.GAMEID) {
					out.write(gameID.getBytes());
				}
				// Make the user who's creating the game white
				else if(GameData.order[i] == GameData.WHITE) {
					out.write(username.getBytes());
				}
				// Otherwise, just write the default values
				else {
					out.write(GameData.values()[i].getInitial().getBytes());
				}
			}
			// Write a newline so that the next game we add goes on its own line
			out.write('\n');
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("ERROR: Failed to open/write to active_games file. New game could not be created.");
			return Protocol.SERVER_ERROR;
		}
		
		// Create a board data file for the new game
		File game_file = new File(this.games_folder, this.getFilename(gameID));
		try {
			game_file.createNewFile();
		}
		catch(IOException e) {
			Log.error("ERROR: Could not create board data file for game \"" + gameID + "\"");
			return Protocol.SERVER_ERROR;
		}
		
		// Open the file that defines what newly-created board data files should look like for reading
		File new_board = new File(FileClientManager.NEW_BOARD);
		Scanner scanner = null;
		try {
			scanner = new Scanner(new_board);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Couldn't open new_board file. Path is probably wrong.");
			return Protocol.SERVER_ERROR;
		}
		
		// Create a FileOutputStream for initializing the new board data file
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(game_file);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Could not open new board data file for initialization");
			scanner.close();
			return Protocol.SERVER_ERROR;
		}
		
		// Copy the contents of new_board into the new 
		while(scanner.hasNextLine()) {
			try {
				out.write((scanner.nextLine()+"\n").getBytes());
			} catch (IOException e) {
				Log.error("ERROR: Failed to write to new board data file for game \"" + gameID + "\"");
				try {
					out.close();
				} catch (IOException e1) {
					Log.error("ERROR: Failed to close FileOutputStream");
				}
				scanner.close();
				return Protocol.SERVER_ERROR;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
			Log.error("Couldn't close FileOutputStream. Changes to new board data file may not have been saved.");
		}
		scanner.close();
		return CreateGame.SUCCESS;
	}

	/**
	 * Try to have the user join the game with the given gameID
	 * 
	 * @param gameID - The ID of the game to join
	 * @return 	Protocol.SERVER_ERROR 					- an error is encountered <br>
	 * 			Protocol.JoinGame.SUCCESS 				- game joined successfully <br>
	 * 			Protocol.JoinGame.GAME_DOES_NOT_EXIST 	- game does not exist <br>
	 * 			Protocol.JoinGame.GAME_FULL 			- game is already full <br>
	 * 			Protocol.JoinGame.USER_ALREADY_IN_GAME 	- the user has already joined that game
	 */
	@Override
	public int joinGame(String gameID) {
		try(Scanner scanner = new Scanner(active_games)) {
			// Read the contents of the file into a List
			List<String> lines = new ArrayList<String>();
			while(scanner.hasNextLine()) {
				lines.add(scanner.nextLine());
			}
			
			String line;
			String[] data;
			FileOutputStream out = new FileOutputStream(active_games);
			// Loop through each line of the file until we find the line corresponding
			// to the game the user wants to join
			for(int i = 0; i < lines.size(); i++) {
				line = lines.get(i);
				
				// Split line about the commas into its columns
				data = line.split(",");
				
				// If we've found the game we want to join
				if(data[GameData.GAMEID.getColumn()].equals(gameID)) {
					// If the player is already in the game
					if(data[GameData.WHITE.getColumn()].equals(username) ||
					   data[GameData.BLACK.getColumn()].equals(username)) {
						writeLines(out, lines);
						return JoinGame.USER_ALREADY_IN_GAME;
					}
					
					// If the game has no black player yet
					// (Game creators are immediately assigned the white pieces,
					//  so a game can only be joined if it exists and there is no
					//  black player)
					else if(data[GameData.BLACK.getColumn()].length() == 0) {
						// Assign this user to the game and then write this change back to the file
						data[GameData.BLACK.getColumn()] = username;
						lines.set(i, toCSV(data));
						
						writeLines(out, lines);
						out.close();
						return JoinGame.SUCCESS;
					}
					
					// If the game is already full, simply restore the file and return accordingly
					else {
						writeLines(out, lines);
						return JoinGame.GAME_FULL;
					}
				}
			}
			
			// We've looped through the whole file and not found the game
			// So restore the file and return accordingly
			writeLines(out, lines);
			return JoinGame.GAME_DOES_NOT_EXIST;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("Error processing active_games file");
			return Protocol.SERVER_ERROR;
		}
	}
	
	/**
	 * Write each line in lines to stream after placing a newline at the end
	 * 
	 * @param stream - The stream to write to
	 * @param lines - The lines to be written to the file
	 * @throws IOException On failure of stream.write()
	 */
	private void writeLines(FileOutputStream stream, List<String> lines) throws IOException {
		StringBuffer all = new StringBuffer();
		for(String line : lines) {
			all.append(line + "\n");
		}
		
		// Write everything to the file all at once to avoid partially writing the data
		// before encountering an error
		stream.write(all.toString().getBytes());
	}
	
	/**
	 * Converts the given String[], into a single line of a .csv file.
	 * 
	 * That is, turn the contents of data into a single string, placing a
	 * comma in between every two consecutive elements, and then places
	 * a newline at the end
	 * 
	 * @param data - The String[] to convert to a .csv line
	 */
	private String toCSV(String[] data) {
		StringBuffer buffer = new StringBuffer();
		for(int i = 0; i < data.length; i++) {
			if(i != 0) {
				buffer.append(",");
			}
			buffer.append(data[i]);
		}
		
		return buffer.toString();
	}
	
	/**
	 * Checks whether or not it's appropriate for the user this client is managing to load the given game.
	 * In particular, checks that the given game exists and that this client's user is a player in the game.
	 * 
	 * THIS METHOD SHOULD BE CALLED before calling loadGame().
	 * 
	 * @param gameID - The game to check
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered <br>
	 * 			Protocol.LoadGame.SUCCESS 				- if and only if the user is a player in the given game, which exists <br>
	 * 		   	Protocol.LoadGame.GAME_DOES_NOT_EXIST	- if the given game does not exist <br>
	 * 		    Protocol.LoadGame.USER_NOT_IN_GAME 		- if the user is not in the given game
 	 */
	@Override
	public int canLoadGame(String gameID) {
		try(Scanner scanner = new Scanner(this.active_games)) {
			List<String> lines = new ArrayList<String>();
			while(scanner.hasNextLine()) {
				lines.add(scanner.nextLine());
			}
			
			int lineNumber = 1;
			String[] data;
			for(String line : lines) {
				// Split the line of the .csv file into its columns
				data = line.split(",");
				if(data.length != GameData.values().length) {
					Log.error("ERROR: Error in active_games file. Line number " + lineNumber + " does not have the right number of columns");
					return Protocol.SERVER_ERROR;
				}
				if(data[GameData.GAMEID.getColumn()].equals(gameID)) {
					// If this object's user is a player in the game
					if(data[GameData.WHITE.getColumn()].equals(this.username)
					|| data[GameData.BLACK.getColumn()].equals(this.username)) {
						return LoadGame.SUCCESS;
					}
					else {
						return LoadGame.USER_NOT_IN_GAME;
					}
				}
				lineNumber++;
			}
			
			// If we never found a line of the active_games file with the given gameID,
			// the game doesn't exist
			return LoadGame.GAME_DOES_NOT_EXIST;
		}
		catch(FileNotFoundException e) {
			Log.error("Could not find active_games file");
			return Protocol.SERVER_ERROR;
		}
	}
	
	/**
	 * In our current implementation, a game's board data is stored in a text file whose name
	 * is the same as the game's gameID. So if the gameID is "lukas's game", the corresponding
	 * board data file is "lukas's game.txt". This method finds the corresponding text file and
	 * returns its contents as a List, guaranteed to consist only of Strings and Integers. 
	 * 
	 * Returns null if the given gameID does not have a board data file, or if an error is encountered.
	 * 
	 * SHOULD ONLY BE CALLED after canLoadGame(gameID) has returned 0.
	 * @param gameID - the game to be loaded
	 * @return A List of only Strings and Integers, where each element of the List represents a line
	 * of the given game's board data file, in order. Returns null if the given gameID does not have a
	 * board data file, or if an error is encountered. 
	 */
	@Override
	public List<Object> loadGame(String gameID) {
		if(!this.games_folder.isDirectory()) {
			Log.error("ERROR: games_folder is not a directory, path must be wrong.");
			return null;
		}

		String data_filename = this.getFilename(gameID);
		
		// Get a File reference to the board data file for this game
		File data_file = null;
		File[] files = this.games_folder.listFiles();
		for(File file : files) {
			System.out.println("File: " + file.getName());
			if(file.getName().equals(data_filename)) {
				data_file = file;
			}
		}
		if(data_file == null) {
			Log.error("Could not find data file for game \"" + gameID + "\". The game may not exist.");
			return null;
		}
		
		Scanner scanner;
		try {
			scanner = new Scanner(data_file);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Could not open board data file for game \"" + gameID + "\"");
			return null;
		}
		
		List<Object> lines = new ArrayList<Object>();
		
		int lineNumber = 1;
		// The definition of the board data files we're working with is that the first 4 lines consist of
		// integers, the following 8 of Strings, and the last line of integers. See Data.pdf for specifics.
		for(int i = 0; i < 4; i++) {
			try {
				int line = Integer.parseInt(scanner.nextLine());
				if(line != 0 && line != 1) {
					Log.error("ERROR: Line " + lineNumber + " of " + data_filename + " is not 1 or 0");
					scanner.close();
					return null;
				}
				lines.add(line);
			}
			catch(NumberFormatException e) {
				Log.error("ERROR: Line " + lineNumber + " of " + data_filename + " could not be parsed to int");
				scanner.close();
				return null;
			}
			catch(NoSuchElementException e) {
				Log.error("ERROR: End of file " + data_filename + " reached too early");
				scanner.close();
				return null;
			}
			lineNumber++;
		}
		
		// Read the next 8 lines, checking for invalid formatting as we go, and otherwise adding
		// to lines
		String line;
		for(int i = 0; i < 8; i++) {
			try {
				line = scanner.nextLine();
			}
			catch(NoSuchElementException e) {
				Log.error("ERROR: End of file " + data_filename + " reached too early");
				scanner.close();
				return null;
			}
			
			
			if(line.length() != 8) {
				Log.error("ERROR: Line " + lineNumber + " of file " + data_filename + " does not have 8 characters");
				scanner.close();
				return null;
			}
			for(int j = 0; j < line.length(); j++) {
				char c = line.charAt(j);
				if(!this.validBoardChar(c)) {
					Log.error("ERROR: Line " + lineNumber + " of " + data_filename + " contains an invalid character.");
					scanner.close();
					return null;
				}
			}
			
			lines.add(line);
			lineNumber++;
		}
		
		// Finally, check the last line
		try {
			line = scanner.nextLine();
		}
		catch(NoSuchElementException e) {
			Log.error("ERROR: End of file " + data_filename + " reached too early");
			scanner.close();
			return null;
		}
		
		try {
			int last = Integer.parseInt(line);
			if(last != 1 && last != 0) {
				Log.error("ERROR: Line " + lineNumber + " of " + data_filename + " is not 1 or 0");
				scanner.close();
				return null;
			}
			
			lines.add((Integer)last);
		}
		catch(NumberFormatException e) {
			Log.error("ERROR: Line " + lineNumber + " of " + data_filename + " could not be parsed to int");
			scanner.close();
			return null;
		}
		
		scanner.close();
		return lines;
	}
	
	/**
	 * Compute whether or not the given char is a valid character to be included in a board data file,
	 * on one of the lines encoding the state of the board.
	 * 
	 * @param c - The character to evaluate
	 * @return true if and only if the given character is allowed to appear in a board data file,
	 * on one of the lines encoding the appearance of the board
	 */
	private boolean validBoardChar(char c) {
		return Character.toLowerCase(c) == Pawn.charRep
			|| Character.toLowerCase(c) == Rook.charRep
			|| Character.toLowerCase(c) == Knight.charRep
			|| Character.toLowerCase(c) == Bishop.charRep
			|| Character.toLowerCase(c) == Queen.charRep
			|| Character.toLowerCase(c) == King.charRep
			|| c == 'X'; // Empty square character
	}
	
	/**
	 * Try and make the given move in the given game. src is the square occupied by the piece
	 * making the move, and dest is the square it is moving to. This method returns a variety of
	 * integers to represent various possible problems with the move command.
	 * 
	 * @param gameID - The game to try and make the move in
	 * @param src - The square the piece that is moving occupies
	 * @param dest - The square that the piece is moving to
	 * @return 	Protocol.SERVER_ERROR 				- if an error is encountered <br>
	 * 			Protocol.Move.SUCCESS				- if the move is successfully made, and the game records are properly updated <br>
	 *			Protocol.Move.GAME_DOES_NOT_EXIST	- if the given game does not exist <br>
	 *			Protocol.Move.USER_NOT_IN_GAME		- if the user this object is managing is not in the given game <br>
	 *			Protocol.Move.NO_OPPONENT			- if the user is in the given game, but does not have an opponent yet <br>
	 *			Protocol.Move.GAME_IS_OVER			- if the given game is already over <br>
	 *			Protocol.Move.NOT_USER_TURN			- if it is not the user's turn to make a move <br>
	 *			Protocol.Move.HAS_TO_PROMOTE		- if it is the user's turn, but they have to promote a pawn rather than make a normal move <br>
	 *			Protocol.Move.RESPOND_TO_DRAW		- if is is the user's turn, but they have to respond to a draw offer <br>
	 *			Protocol.Move.MOVE_INVALID			- if the given move is invalid (for example, the selected piece can't move to the selected square)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int makeMove(String gameID, Pair src, Pair dest) {
		// This will keep a list of every line in the file, so that if we need to edit the game's listing
		// we can do it easily, without iterating over the file again
		List<String> lines;
		String[] data;
		int lineNumber;
		HashMap<String, Object> gameData = this.getGameData(gameID);
		// Check the returned HashMap for signs that an error occurred or the game doesn't exist
		if((Integer)gameData.get("error") == 1) {
			return Protocol.SERVER_ERROR;
		}
		else {
			if((Integer)gameData.get("lineNumber") == 0) {
				return Protocol.Promote.GAME_DOES_NOT_EXIST;
			}
		}
		// Extract the necessary data from the HashMap
		data = (String[])gameData.get("data");
		lines = (ArrayList<String>)gameData.get("lines");
		lineNumber = (Integer)gameData.get("lineNumber");
		
		Scanner scanner;
		try {
			scanner = new Scanner(this.active_games);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Couldn't open active_games file for reading");
			return Protocol.SERVER_ERROR;
		}
		
		try {
			// If the user is the only player in the game
			if (data[GameData.BLACK.getColumn()].length() == 0 || data[GameData.WHITE.getColumn()].length() == 0) {
				scanner.close();
				return Move.NO_OPPONENT;
			}
			// If the game is drawn, or a winner has been declared, the game is over and no more moves can be accepted
			else if(this.gameIsOver(data)) {
				scanner.close();
				return Move.GAME_IS_OVER;
			}
			// If it's not the user's turn, return the appropriate error code
			else if(!this.isUserTurn(data, this.username)) {
				scanner.close();
				return Move.NOT_USER_TURN;
			}			
			// If it is the user's turn, but a promotion is needed rather than a normal move
			else if(Integer.parseInt(data[GameData.PROMOTION_NEEDED.getColumn()]) == 1) {
				scanner.close();
				return Move.HAS_TO_PROMOTE;
			}
			// If it is the user's turn, but they need to respond to a draw offer rather than make a normal move
			else if (Integer.parseInt(data[GameData.DRAW_OFFERED.getColumn()]) == 1) {
				scanner.close();
				return Move.RESPOND_TO_DRAW;
			}
			
		}
		catch(NumberFormatException e) {
			Log.error("ERROR: Line number " + lineNumber + " contains an entry that could not be converted to int");
			return Protocol.SERVER_ERROR;
		}
		
		// Now that we've error-checked every aspect of the move request except the validity of the move
		// in the context of chess, we proceed.
		if(!this.games_folder.isDirectory()) {
			Log.error("ERROR: games folder is not directory. Path might be wrong.");
			scanner.close();
			return Protocol.SERVER_ERROR;
		}
		
		
		try {
			scanner = new Scanner(new File(this.games_folder, this.getFilename(gameID)));
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Could not open game file for game: " + gameID);
			return Protocol.SERVER_ERROR;
		}
		Board board = new Board();
		board.initialize(scanner);
		
		int result = board.move(src, dest);
		
		if(result == 1) {
			return Move.MOVE_INVALID;
		}
		else if(result == 2) {
			return Move.NOT_USER_TURN;
		}
		else if(result == 3) {
			System.out.println("Board says promotion needed");
			return Move.HAS_TO_PROMOTE;
		}
		// The other possible return codes, 0 and -1, mean that the move was valid and we can proceed
		// with updating our records.
		// We need to update the board's data file, and the game's listing in our active_games.csv file
		// The data we may need to change:
		// 1) State - the boolean keeping track of whose turn it is (if a promotion is now necessary, we don't flip it.
		//	  Otherwise, we do)
		// 2) Turn number - increment if it was black's turn
		// 3) Winner - check if the enemy colour has been checkmated
		// 4) White & Black check - simply call board and check if either colour is in check
		// 5) Promotion needed - check if the player who made the move now needs to promote a pawn
		
		
		// 1) STATE
		// If result is -1, the player who just moved still needs to promote a pawn, so we don't change the state
		// of the game
		if(result != -1) {
			try {
				int state = Integer.parseInt(data[GameData.STATE.getColumn()]);
				if(state != 0 && state != 1) {
					Log.error("Line number " + lineNumber + " of active_games.csv has STATE value that isn't 0 or 1.");
					return Protocol.SERVER_ERROR;
				}
				
				// Simply flip the state bit
				data[GameData.STATE.getColumn()] = (state == 0) ? "1" : "0";
			}
			catch(NumberFormatException e) {
				Log.error("Line number " + lineNumber + " of active_games.csv has STATE value that could not be converted to int.");
				return Protocol.SERVER_ERROR;
			}
		}
		
		// 2) TURN
		// If result == -1, whoever just made a move still has to promote, which means their
		// turn hasn't ended and we shouldn't increment the turn counter
		if(result != -1) {
			try {
				int state = Integer.parseInt(data[GameData.STATE.getColumn()]);
				
				// If it's black's turn, we increment the turn counter
				if(state == 1) {
					int turn = Integer.parseInt(data[GameData.TURN.getColumn()]);
					turn++;
					
					data[GameData.TURN.getColumn()] = Integer.toString(turn);
				}
			}
			catch(NumberFormatException e) {
				Log.error("Line number " + lineNumber + " of active_games.csv has STATE or TURN value that could not be converted to int.");
				return Protocol.SERVER_ERROR;
			}
		}
		
		// 3) WINNER
		// We just check if whoever made the move has checkmated their opponent
		try {
			int state = Integer.parseInt(data[GameData.STATE.getColumn()]);
			if(state != 0 && state != 1) {
				Log.error("Line number " + lineNumber + " of active_games.csv has STATE value that isn't 0 or 1.");
				return Protocol.SERVER_ERROR;
			}
			
			Colour enemy = (state == 0) ? Colour.BLACK : Colour.WHITE;
			if(board.isCheckmate(enemy)) {
				data[GameData.WINNER.getColumn()] = this.username;
			}
		}
		catch(NumberFormatException e) {
			Log.error("Line number " + lineNumber + " of active_games.csv has STATE value that could not be converted to int.");
			return Protocol.SERVER_ERROR;
		}
		
		// 4) WHITE_CHK & BLACK_CHK
		data[GameData.WHITE_CHK.getColumn()] = (board.isCheck(Colour.WHITE)) ? "1" : "0";
		data[GameData.BLACK_CHK.getColumn()] = (board.isCheck(Colour.BLACK)) ? "1" : "0";
		
		// 5) PROMOTION_NEEDED
		data[GameData.PROMOTION_NEEDED.getColumn()] = (result == -1) ? "1" : "0";
		
		lines.set(lineNumber-1, this.toCSV(data));
		// lines now has the updated listing for the given game
		
		// We proceed with actually saving our new data to the relevant files.
		FileOutputStream active_games_stream;
		FileOutputStream board_data_stream;
		try {
			active_games_stream = new FileOutputStream(this.active_games);
			board_data_stream = new FileOutputStream(new File(this.games_folder, this.getFilename(gameID)));
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open either active_games file or board data file for game \"" + gameID + "\"");
			return Protocol.SERVER_ERROR;
		}
		
		try {
			this.writeLines(active_games_stream, lines);
			active_games_stream.close();
		} catch (IOException e) {
			Log.error("ERROR: Couldn't write to active_games file");
			return Protocol.SERVER_ERROR;
		}
		
		try {
			board.saveGame(board_data_stream);
			board_data_stream.close();
		}
		catch(IOException e) {
			Log.error("ERROR: Couldn't write to board data file for game \"" + gameID + "\"");
			return Protocol.SERVER_ERROR;
		}
		
		scanner.close();
		return Move.SUCCESS;
	}
	
	/**
	 * Return a HashMap which stores the following data: <br>
	 * 		"data" 			: a String[] containing the data for the given game, split into columns from the .csv file <br>
	 * 		"lines"			: a List of Strings of every line in the active_games.csv file, in order <br>
	 * 		"lineNumber"	: marks the number of the line that corresponds to the given game (starts counting from 1) <br>
	 * 		"error"			: 1 if an error occurred, 0 otherwise <br>
	 * 
	 * If an error is encountered, for example if the file can't be opened or a problem is found in the formatting
	 * of the .csv file, "error" is set to 1, "data" and "lines" are set to null, and "lineNumber" is set to 0. <br>
	 * 
	 * If the given game isn't found, error is set to 0, "data" and "lines" are set to null, and "lineNumber" is set to 0. <br>
	 * 
	 * Otherwise, if the given game is found and no errors are encountered, "data", "lines", and "lineNumber" are set according
	 * to their definitions and "error" is set to 0. <br>
	 * 
	 * @param gameID - the game to retrieve information about
	 * @return A HashMap containing information about the given game
	 */
	private HashMap<String, Object> getGameData(String gameID) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		Scanner scanner;
		try {
			scanner = new Scanner(this.active_games);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Couldn't open active_games file");
			map.put("data", null);
			map.put("lines", null);
			map.put("lineNumber", 0);
			map.put("error", 1);
			return map;
		}
		boolean found = false;
		
		// We iterate through the active_games file, searching for the line corresponding to the given game
		// We keep a list of every line in the file, so that if we need to change some data for the given game
		// as a result of the move, we don't have to iterate through the file again for overwriting
		int lineNumber = 1; // Will keep track of what line of the file corresponds to the given game
		List<String> lines = new ArrayList<String>();
		String line = "";
		String[] data = {}; // Will store the data associated with the given game, split into columns
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			lines.add(line);
			if(!found) {
				data = line.split(",");
				
				if(data.length != GameData.order.length) {
					Log.error("ERROR: Line number " + lineNumber + " does not have correct number of columns");
					scanner.close();
					map.put("data", null);
					map.put("lines", null);
					map.put("lineNumber", 0);
					map.put("error", 1);
					return map;
				}
				
				if(data[GameData.GAMEID.getColumn()].equals(gameID)) {
					found = true; 	// Setting found to true means that for the rest of the loop we're just reading lines from the file
									// Also freezes lineNumber and data, so they refer to the line we want when the loop ends
				}
				else {
					lineNumber++;
				}
			}
		}
		// If we haven't found the game, return accordingly
		if(!found) {
			scanner.close();
			map.put("data", null);
			map.put("lines", null);
			map.put("lineNumber", 0);
			map.put("error", 0);
			return map;
		}
		
		scanner.close();
		map.put("data", data);
		map.put("lines", lines);
		map.put("lineNumber", lineNumber);
		map.put("error", 0);
		return map;
	}
	
	/**
	 * Return whether or not it's the given user's turn. Assumes data is a valid line from the active_games.csv file,
	 * split into its columns, and username is a player in the given game.
	 * @param data - the data corresponding to the game in question
	 * @param username - the username of the user whose turn we are inquiring about
	 * @return true if and only if it is the given user's turn in the game corresponding to the given data
	 */
	private boolean isUserTurn(String[] data, String username) {
		// It's the user's turn if it's white's turn and the user is white, or it's black's turn and the user is black
		return (Integer.parseInt(data[GameData.STATE.getColumn()]) == 0 && data[GameData.WHITE.getColumn()].equals(this.username))
		   ||  (Integer.parseInt(data[GameData.STATE.getColumn()]) == 1 && data[GameData.BLACK.getColumn()].equals(this.username));
	}
	
	/**
	 * Compute whether or not the given game is over. data is assumed to be a valid line from the
	 * active_games.csv file, split into its columns
	 * @param data
	 * @return
	 */
	private boolean gameIsOver(String[] data) {
		// Game is over if it's been drawn, or winner has been declared
		return Integer.parseInt(data[GameData.DRAWN.getColumn()]) == 1
			|| data[GameData.WINNER.getColumn()].length() > 0;
	}
	
	/**
	 * Return what the name of the given game's board data file should be.
	 * @param gameID - The game to process
	 * @return The name (not the path) of the given game's board data file
	 */
	private String getFilename(String gameID) {
		return gameID + ".txt";
	}

	
	/**
	 * Attempt to promote a pawn to the piece given by charRep, in the given game, on behalf of the user. 
	 * 
	 * @param gameID - The game in which to try to make the promotion
	 * @param charRep - A character denoting which piece to upgrade into. One of 'r', 'n', 'b', or 'q'.
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered
				Protocol.Promote.SUCCESS 				- if promotion is successful
				Protocol.Promote.GAME_DOES_NOT_EXIST 	- if given game does not exist
				Protocol.Promote.USER_NOT_IN_GAME 		- if the user isn't a player in the given game
				Protocol.Promote.NO_OPPONENT			- if the user doesn't have an opponent yet in the game
				Protocol.Promote.GAME_IS_OVER			- if the given game is already over
				Protocol.Promote.NOT_USER_TURN 			- if it's not the user's turn
				Protocol.Promote.NO_PROMOTION 			- if no promotion is able to be made
				Protocol.Promote.CHAR_REP_INVALID 		- if the given charRep is not valid
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int promote(String gameID, char charRep) {		
		// This will keep a list of every line in the file, so that if we need to edit the game's listing
		// we can do it easily, without iterating over the file again
		List<String> lines;
		String[] data;
		int lineNumber;
		HashMap<String, Object> gameData = this.getGameData(gameID);
		// Check the returned HashMap for signs that an error occurred or the game doesn't exist
		if((Integer)gameData.get("error") == 1) {
			return Protocol.SERVER_ERROR;
		}
		else {
			if((Integer)gameData.get("lineNumber") == 0) {
				return Protocol.Promote.GAME_DOES_NOT_EXIST;
			}
		}
		// Extract the necessary data from the HashMap
		data = (String[])gameData.get("data");
		lines = (ArrayList<String>)gameData.get("lines");
		lineNumber = (Integer)gameData.get("lineNumber");
		
		// Compute a couple of possible error cases
		if(!data[GameData.WHITE.getColumn()].equals(this.username)
		&& !data[GameData.BLACK.getColumn()].equals(this.username)) {
			return Protocol.Promote.USER_NOT_IN_GAME;
		}
		if(data[GameData.WHITE.getColumn()].length() == 0
		|| data[GameData.BLACK.getColumn()].length() == 0) {
			return Protocol.Promote.NO_OPPONENT;
		}
		if(this.gameIsOver(data)) {
			return Protocol.Promote.GAME_IS_OVER;
		}
		if(!this.isUserTurn(data, this.username)) {
			return Protocol.Promote.NOT_USER_TURN;
		}
		
		
		// Check if a promotion is actually needed
		try {
			int promotion = Integer.parseInt(data[GameData.PROMOTION_NEEDED.getColumn()]);
			if(promotion != 0 && promotion != 1) {
				Log.error("ERROR: Line number " + lineNumber + " of active_games file has " + GameData.PROMOTION_NEEDED + " value not 0 or 1");
				return Protocol.SERVER_ERROR;
			}
			
			// If no promotion is needed
			if(promotion == 0) {
				return Protocol.Promote.NO_PROMOTION;
			}
		}
		catch(NumberFormatException e) {
			Log.error("ERROR: Line number " + lineNumber + " of active_games file has " + GameData.PROMOTION_NEEDED + " value that couldn't be converted to int.");
			return Protocol.SERVER_ERROR;
		}
		
		// Now we actually process the promotion order
		
		if(!this.games_folder.isDirectory()) {
			Log.error("ERROR: games folder is not directory. Path might be wrong.");
			return Protocol.SERVER_ERROR;
		}
		
		// Create a Board and initialize it to the game we're interested in
		Scanner scanner;
		try {
			scanner = new Scanner(new File(this.games_folder, this.getFilename(gameID)));
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Could not open game file for game: " + gameID);
			return Protocol.SERVER_ERROR;
		}
		Board board = new Board();
		board.initialize(scanner);
		
		int result = board.promote(charRep);
		if(result == 0) {
			// This means the promotion succeeded. All we need to do is update the game's listing in the
			// active_games file and its board data file
			data[GameData.PROMOTION_NEEDED.getColumn()] = "0"; 	// No more than a single piece can require promotion at a time,
																// so we can safely set this to 0
			
			// We need to update other data about the game, like whose turn it is, the turn counter, whether or not
			// either colour is in check, and whether or not either player has been checkmated
			try {
				// Update whose turn it is
				int state = Integer.parseInt(data[GameData.STATE.getColumn()]);
				if(state != 0 && state != 1) {
					Log.error("ERROR: Line number " + lineNumber + " has " + GameData.STATE + " value that is not 0 or 1.");
					return Protocol.SERVER_ERROR;
				}
				data[GameData.STATE.getColumn()] = (state == 0) ? "1" : "0";
				
				// Update the turn counter, if necessary
				int turnNum = Integer.parseInt(data[GameData.TURN.getColumn()]);
				// Increment the turn counter if this was black's move
				data[GameData.TURN.getColumn()] = (state == 1) ? Integer.toString(turnNum+1) : Integer.toString(turnNum);
			
				// Update the white & black check boolean
				data[GameData.WHITE_CHK.getColumn()] = (board.isCheck(Colour.WHITE)) ? "1" : "0";
				data[GameData.BLACK_CHK.getColumn()] = (board.isCheck(Colour.BLACK)) ? "1" : "0";
				
				// Check if the promotion checkmated the enemy
				Colour enemy = (state == 0) ? Colour.BLACK : Colour.WHITE;
				if(board.isCheckmate(enemy)) {
					data[GameData.WINNER.getColumn()] = this.username;
				}
			}
			catch(NumberFormatException e) {
				Log.error("ERROR: Line number " + lineNumber + " of active_games.csv has value that couldn't be converted to int.");
				return Protocol.SERVER_ERROR;
			}
			
			lines.set(lineNumber-1, this.toCSV(data));			// Update the actual line of the file corresponding to the game
																// We'll use this list to write to the file later
			// Try to update the active_games file
			FileOutputStream stream;
			try {
				stream = new FileOutputStream(this.active_games);
				this.writeLines(stream, lines);
			}
			catch(FileNotFoundException e) {
				Log.error("ERROR: Couldn't open active_games file for writing.");
				return Protocol.SERVER_ERROR;
			}
			catch(IOException e) {
				Log.error("ERROR: Couldn't write to active_games file to save changes");
				return Protocol.SERVER_ERROR;
			}
			
			// Try to update the game's board data file
			try {
				stream = new FileOutputStream(new File(this.games_folder, this.getFilename(gameID)));
				board.saveGame(stream);
			}
			catch(FileNotFoundException e) {
				Log.error("ERROR: Couldn't open file " + this.getFilename(gameID) + " for writing.");
				return Protocol.SERVER_ERROR;
			} catch (IOException e) {
				Log.error("ERROR: Couldn't write to file " + this.getFilename(gameID));
				return Protocol.SERVER_ERROR;
			}
			
			return Protocol.Promote.SUCCESS;
		}
		else if (result == 1) {
			return Protocol.Promote.NO_PROMOTION;
		}
		else {
			return Protocol.Promote.CHAR_REP_INVALID;
		}
	}
	
	/**
	 * Attempt to offer/accept a draw on behalf of the user in the given game.
	 * @param gameID - the game in which to offer/accept a draw
	 * @return 	Protocol.SERVER_ERROR 				- if an error is encountered  <br>
				Protocol.Draw.SUCCESS 				- if draw offer/accept is successful  <br>
				Protocol.Draw.GAME_DOES_NOT_EXIST 	- if given game does not exist  <br>
				Protocol.Draw.USER_NOT_IN_GAME 		- if the user isn't a player in the given game  <br>
				Protocol.Draw.NO_OPPONENT 			- if the user doesn't have an opponent in the given game yet  <br>
				Protocol.Draw.GAME_IS_OVER			- if the given game is already over
				Protocol.Draw.NOT_USER_TURN 		- if it's not the user's turn in the given game
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int draw(String gameID) {
		// This will keep a list of every line in the file, so that if we need to edit the game's listing
		// we can do it easily, without iterating over the file again
		List<String> lines;
		String[] data;
		int lineNumber;
		HashMap<String, Object> gameData = this.getGameData(gameID);
		// Check the returned HashMap for signs that an error occurred or the game doesn't exist
		if((Integer)gameData.get("error") == 1) {
			return Protocol.SERVER_ERROR;
		}
		else {
			if((Integer)gameData.get("lineNumber") == 0) {
				return Protocol.Promote.GAME_DOES_NOT_EXIST;
			}
		}
		// Extract the necessary data from the HashMap
		data = (String[])gameData.get("data");
		lines = (ArrayList<String>)gameData.get("lines");
		lineNumber = (Integer)gameData.get("lineNumber");
		
		if(!data[GameData.BLACK.getColumn()].equals(this.username)
		&& !data[GameData.WHITE.getColumn()].equals(this.username)) {
			return Protocol.Draw.USER_NOT_IN_GAME;
		}
		// Otherwise, one of the players is our user.
		// So if one of the entries is empty, we know the user doesn't have an opponent
		if(data[GameData.BLACK.getColumn()].length() == 0
		|| data[GameData.WHITE.getColumn()].length() == 0) {
			return Protocol.Draw.NO_OPPONENT;
		}
		if(this.gameIsOver(data)) {
			return Protocol.Draw.GAME_IS_OVER;
		}
		if(!this.isUserTurn(data, this.username)) {
			return Protocol.Draw.NOT_USER_TURN;
		}
		
		int draw_offered;
		try {
			draw_offered = Integer.parseInt(data[GameData.DRAW_OFFERED.getColumn()]);
		}
		catch(NumberFormatException e) { 
			Log.error("ERROR: Line number " + lineNumber + " has " + GameData.DRAW_OFFERED + " value that couldn't be converted to int.");
			return Protocol.SERVER_ERROR;
		}
		
		// If no draw has been offered, we offer one
		if(draw_offered == 0) {
			data[GameData.DRAW_OFFERED.getColumn()] = "1";
			try {
				int state = Integer.parseInt(data[GameData.STATE.getColumn()]);
				if(state != 0 && state != 1) {
					Log.error("ERROR: Line number " + lineNumber + " has " + GameData.STATE + " value that is not 0 or 1.");
					return Protocol.SERVER_ERROR;
				}
				data[GameData.STATE.getColumn()] = (state == 0) ? "1" : "0";
			}
			catch(NumberFormatException e) {
				Log.error("ERROR: Line number " + lineNumber + " has " + GameData.STATE + " value that couldn't be converted to int.");
				return Protocol.SERVER_ERROR;
			}
		}
		// If a draw has been offered, we accept it
		else if (draw_offered == 1) {
			data[GameData.DRAWN.getColumn()] = "1";
			// The game is now over
		}
		else {
			Log.error("ERROR: Line number " + lineNumber + " has " + GameData.DRAW_OFFERED + " value that is not 0 or 1.");
			return Protocol.SERVER_ERROR;
		}
		
		FileOutputStream stream;
		try {
			stream = new FileOutputStream(this.active_games);
		}
		catch(FileNotFoundException e) {
			Log.error("Couldn't open active_games file for writing.");
			return Protocol.SERVER_ERROR;
		}
		
		lines.set(lineNumber-1, this.toCSV(data));
		try {
			this.writeLines(stream, lines);
		} catch (IOException e) {
			Log.error("ERROR: Couldn't write to activeP_games file.");
			return Protocol.SERVER_ERROR;
		}
		
		return Protocol.Promote.SUCCESS;
	}
}