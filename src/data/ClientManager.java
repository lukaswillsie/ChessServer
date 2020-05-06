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
	 * @return An integer corresponding to one of the following values: <br>
	 * 		0 - game joined successfully <br>
	 * 		1 - game does not exist <br>
	 * 		2 - game is already full <br>
	 * 		3 - The user has already joined that game <br>
	 * 		4 - an error/exception occurred
	 */
	public abstract int joinGame(String gameID);
	
	/**
	 * Checks whether or not it's appropriate for the user this client is managing to load the given game.
	 * In particular, checks that the given game exists and that this client's user is a player in the game.
	 * 
	 * THIS METHOD SHOULD BE CALLED before calling loadGame().
	 * 
	 * @param gameID - The game to check
	 * @return 0 if and only if the user is a player in the given game, which exists <br>
	 * 		   1 if the given game does not exist <br>
	 * 		   2 if the user is not in the given game <br>
	 * 		   3 if an error is encountered
 	 */
	public abstract int canLoadGame(String gameID);
	
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
	 * THIS METHOD SHOULD ONLY BE CALLED after canLoad(gameID) has returned 0.
	 * 
	 * Returns null if the given gameID does not have any board data, but does not check if this object's
	 * user is a player in the game.
	 * 
	 * @param gameID - The game to load
	 * @return  A List of Strings and Integers, representing the given gameID's board data, or null if the given game
	 * has no associated board data file, or if there is an error accessing data
	 */
	public abstract List<Object> loadGame(String gameID);
	
	// TODO: Determine what this method's signature should be
	public abstract void makeMove(String gameID, String move);
	
	public String getUsername() {
		return username;
	}
}
