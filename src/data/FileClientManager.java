package data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

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
	private static final String GAMES_FILENAME = "serverdata/active_games.csv";
	private String username;
	private File games;
	
	public static void main(String[] args) {
		FileClientManager test = new FileClientManager("Vaskar");
		System.out.println(test.getGameData());
	}
	
	/**
	 * Create a new FileClientManager on behalf of the user with the given username
	 * @param username - The username to create this FileClientManager for
	 */
	FileClientManager(String username) {
		this.username = username;
		games = new File(GAMES_FILENAME);
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
			scanner = new Scanner(this.games);
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("ERROR: Could not open active_games file for reading in getGameData().");
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
				Log.log("ERROR: Error in active_games file. Line number " + lineNum + " does not have correct number of columns.");
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
							Log.log("ERROR: In active_games file, line " + lineNum + ", couldn't cast data " + dataType + " to int");
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
		try(Scanner scanner = new Scanner(games)) {
			String line, id;
			// Read each line of the active_games file one at a time,
			// checking if the game already exists
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				
				int comma = line.indexOf(',');
				if(comma == -1) {
					Log.log("active_games file is incorrectly formatted");
					return 2;
				}
				
				id = line.substring(0, comma);
				if(id.equals(gameID)) {
					return 1;
				}
			}
			// We've reached the end of the file without finding the given gameID
			// So we can go ahead and create the game
			FileOutputStream out = new FileOutputStream(games, true);
			
			// Add the new game to the file
			for(int i = 0; i < GameData.values().length; i++) {
				if(i != 0) {
					out.write(",".getBytes());
				}
				
				// Write the given gameID instead of the default value
				if(GameData.values()[i] == GameData.GAMEID) {
					out.write(gameID.getBytes());
				}
				// Make the user who's creating the game white
				else if(GameData.values()[i] == GameData.WHITE) {
					out.write(username.getBytes());
				}
				// Otherwise, just write the default values
				else {
					out.write(GameData.values()[i].getInitial().getBytes());
				}
			}
			out.close();
			
			return 0;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("Error processing active_games file.");
			return 2;
		}
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
		try(Scanner scanner = new Scanner(games)) {
			
			// Read the contents of the file into a List
			List<String> lines = new ArrayList<String>();
			while(scanner.hasNextLine()) {
				lines.add(scanner.nextLine());
			}
			
			String line;
			String[] data;
			FileOutputStream out = new FileOutputStream(games);
			// Loop through each line of the file until we find the line corresponding
			// to the game the user wants to join
			for(int i = 0; i < lines.size(); i++) {
				line = lines.get(i);
				
				// Split line about the commas into its columns
				data = line.split(",");
				
				// If we've found the game we want to join
				if(data[GameData.GAMEID.getColumn()].equals(gameID)) {
					// If the player is the owner of this game
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
			Log.log("Error processing active_games file");
			return 3;
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
		buffer.append("\n");
		
		return buffer.toString();
	}

	@Override
	public void loadGame(String gameID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void makeMove(String gameID, String move) {
		// TODO Auto-generated method stub

	}

}
