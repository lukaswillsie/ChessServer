package data;

import java.util.HashMap;
import java.util.List;

public abstract class ClientManager {
	// The username of the user this Manager is managing
	String username;
	
	/**
	 * Return a List of all games this Manager's user is participating in.
	 * Each game is represented as a HashMap. Keys take values in the GameData
	 * enum, which defines exactly what pieces of data define a game. The values are
	 * Objects, guaranteed to either be Strings or Integers, and represent the value 
	 * taken on by the corresponding key.
	 * 
	 * So the pair {GAMEID=lukas's} means that the game's ID is "lukas's".
	 * 
	 * Each HashMap within the List has as many key,value pairs as the GameData enum has values,
	 * one for each
	 * 
	 * Returns null if an error is encountered (for example, if the underlying data cannot be
	 * accessed or has been corrupted in some way).
	 * 
	 * @return - The array of HashMaps corresponding to all games being played by this user
	 */
	public abstract List<HashMap<GameData,Object>> getGameData();
	
	/**
	 * Create a game with the given gameID under the user's name
	 * 
	 * @param gameID - The ID of the game to create
	 * @return An integer corresponding to one of the following values:<br>
	 * 		0 - game created successfully <br>
	 * 		1 - game already exists and hence cannot be created
	 * 		2 - an error/exception occurred
	 */
	public abstract int createGame(String gameID);
	
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
	public abstract int joinGame(String gameID);
	
	/**
	 * Return the state of the board in the given game
	 * 
	 * @param gameID - The game to load
	 */
	// TODO: Determine return type of this method, and write underlying chess infrastructure
	// TODO: Determine whether or not to check if the user is playing this game
	public abstract void loadGame(String gameID);
	
	// TODO: Determine what this method's signature should be
	public abstract void makeMove(String gameID, String move);
	
	public String getUsername() {
		return username;
	}
}
