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
import game.King;
import game.Knight;
import game.Pawn;
import game.Queen;
import game.Rook;
import utility.Log;

/**
 * This class is responsible for accessing and managing data on behalf of the server
 * for a particular client. It interacts with the file "serverdata/active_games.csv",
 * which stores all currently active games in the following manner:
 * 
 * gameID | White Player | Black Player | State | Turn | White check | White checkmate | Black check | Black checkmate
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
	 * @return An integer corresponding to one of the following values:<br>
	 * 		0 - game created successfully <br>
	 * 		1 - game already exists and hence cannot be created
	 * 		2 - an error/exception occurred
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
					return 2;
				}
				
				id = line.substring(0, comma);
				if(id.equals(gameID)) {
					return 1;
				}
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("Error opening active_games file.");
			return 2;
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
			return 2;
		}
		
		// Create a board data file for the new game
		File game_file = new File(this.games_folder, this.getFilename(gameID));
		try {
			game_file.createNewFile();
		}
		catch(IOException e) {
			Log.error("ERROR: Could not create board data file for game \"" + gameID + "\"");
			return 2;
		}
		
		// Open the file that defines what newly-created board data files should look like for reading
		File new_board = new File(FileClientManager.NEW_BOARD);
		Scanner scanner = null;
		try {
			scanner = new Scanner(new_board);
		}
		catch(FileNotFoundException e) {
			Log.error("ERROR: Couldn't open new_board file. Path is probably wrong.");
			return 2;
		}
		
		// Create a FileOutputStream for initializing the new board data file
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(game_file);
		}
		catch(FileNotFoundException e) {
			Log.error("Could not open new board data file for initialization");
			scanner.close();
			return 2;
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
				return 2;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
			Log.error("Couldn't close FileOutputStream. Changes to new board data file may not have been saved.");
		}
		scanner.close();
		return 0;
	}

	/**
	 * Try to have the user join the game with the given gameID
	 * 
	 * @param gameID - The ID of the game to join
	 * @return An integer corresponding to one of the following values:<br>
	 * 		0 - game joined successfully
	 * 		1 - game does not exist
	 * 		2 - game is already full
	 * 		3 - The user has already joined that game
	 * 		4 - an error/exception occurred
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
						return 3;
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
						return 0;
					}
					
					// If the game is already full, simply restore the file and return accordingly
					else {
						writeLines(out, lines);
						return 2;
					}
				}
			}
			
			// We've looped through the whole file and not found the game
			// So restore the file and return accordingly
			writeLines(out, lines);
			return 1;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.error("Error processing active_games file");
			return 4;
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
		for(String line : lines) {
			stream.write((line + "\n").getBytes());
		}
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
	 * Checks two things: <br>
	 * 1) That the given gameID exists <br>
	 * 2) That this object's user is a player in the game <br>
	 * And returns a different value according to the result
	 * @param gameID - Represents the game to check
	 * @return 0 if and only if the user is a player in the given game, which exists <br>
	 * 		   1 if the given game does not exist <br>
	 * 		   2 if the user is not in the given game <br>
	 * 		   3 if an error is encountered
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
					return 3;
				}
				if(data[GameData.GAMEID.getColumn()].equals(gameID)) {
					// If this object's user is a player in the game
					if(data[GameData.WHITE.getColumn()].equals(this.username)
					|| data[GameData.BLACK.getColumn()].equals(this.username)) {
						return 0;
					}
					else {
						return 2;
					}
				}
				lineNumber++;
			}
			
			// If we never found a line of the active_games file with the given gameID,
			// the game doesn't exist
			return 1;
		}
		catch(FileNotFoundException e) {
			Log.error("Could not find active_games file");
			return 3;
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

	@Override
	public void makeMove(String gameID, String move) {
		// TODO Auto-generated method stub

	}
	
	/**
	 * Return what the name of the given game's board data file should be.
	 * @param gameID - The game to process
	 * @return The name (not the path) of the given game's board data file
	 */
	private String getFilename(String gameID) {
		return gameID + ".txt";
	}
}
