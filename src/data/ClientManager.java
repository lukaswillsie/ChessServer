package data;

import java.util.HashMap;
import java.util.List;

import utility.Pair;

/**
 * Defines what a ClientManager should be able to do. In general terms, the ClientManager is the
 * object that does the dirty work when executing commands on behalf of the user. The Protocol object
 * parses the command to determine what the client wants done, and then hands the request off to one of
 * these objects to handle the details. For example, the Protocol instance might receive a move request and
 * hand the particulars off to this object, which will go through the process of retrieving the relevant game data,
 * checking if the move is a valid move, and updating the affected files/data if necessary.
 * 
 * After a client has logged in a particular user, a ClientManager object should be created and assigned to
 * that user for the processing of future user-specific requests.
 * 
 * @author Lukas Willsie
 *
 */
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
	 * So the pair {GAMEID="lukas's"} means that the game's ID is "lukas's".
	 * 
	 * Each HashMap within the List has as many key,value pairs as the GameData enum has values
	 * 
	 * Returns null if an error is encountered (for example, if the underlying data cannot be
	 * accessed or has been corrupted in some way).
	 * 
	 * @return - The array of HashMaps corresponding to all games being played by this user
	 */
	public abstract List<HashMap<GameData,Object>> getGameData();
	
	/**
	 * Create a game with the given gameID under the user's name.
	 * 
	 * @param gameID - The ID of the game to create
	 * @return 	Protocol.SERVER_ERROR 				- an error is encountered <br>
	 * 			Protocol.CreateGame.SUCCESS 		- game created successfully <br>
	 * 			Protocol.CreateGame.GAMEID_IN_USE 	- game already exists and hence cannot be created
	 */
	public abstract int createGame(String gameID);
	
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
	public abstract int joinGame(String gameID);
	
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
	
	/**
	 * Try and make the given move in the given game. src is the square occupied by the piece
	 * making the move, and dest is the square it is moving to. This method returns a variety of
	 * integers to represent various possible problems with the move command.
	 * 
	 * @param gameID - The game to try and make the move in
	 * @param src - The square the piece that is moving occupies
	 * @param dest - The square that the piece is moving to
	 * @return 	Protocol.SERVER_ERROR				- if an error is encountered
	 * 			Protocol.Move.SUCCESS				- if the move is successfully made, and the game records are properly updated <br>
	 *			Protocol.Move.GAME_DOES_NOT_EXIST	- if the given game does not exist <br>
	 *			Protocol.Move.USER_NOT_IN_GAME		- if the user this object is managing is not in the given game <br>
	 *			Protocol.Move.NO_OPPONENT			- if the user is in the given game, but does not have an opponent yet <br>
	 *			Protocol.Move.GAME_IS_OVER			- if the given game is already over <br>
	 *			Protocol.Move.NOT_USER_TURN			- if it is not the user's turn to make a move <br>
	 *			Protocol.Move.HAS_TO_PROMOTE		- if it is the user's turn, but they have to promote a pawn rather than make a normal move <br>
	 *			Protocol.Move.RESPOND_TO_DRAW		- if is is the user's turn, but they have to respond to a draw offer <br>
	 *			Protocol.Move.MOVE_INVALID			- if the given move is invalid (for example, the selected piece can't move to the selected square) <br>
	 */
	public abstract int makeMove(String gameID, Pair src, Pair dest);
	
	/**
	 * Attempt to promote a pawn to the piece given by charRep, in the given game, on behalf of the user. 
	 * 
	 * @param gameID - The game in which to try to make the promotion
	 * @param charRep - A character denoting which piece to upgrade into. One of 'r', 'n', 'b', or 'q'.
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered
	 *			Protocol.Promote.SUCCESS 				- if promotion is successful
	 *			Protocol.Promote.GAME_DOES_NOT_EXIST 	- if given game does not exist
	 *			Protocol.Promote.USER_NOT_IN_GAME 		- if the user isn't a player in the given game
	 *			Protocol.Promote.NO_OPPONENT			- if the user doesn't have an opponent yet in the game
	 *			Protocol.Promote.GAME_IS_OVER			- if the given game is already over
	 *			Protocol.Promote.NOT_USER_TURN 			- if it's not the user's turn
	 *			Protocol.Promote.NO_PROMOTION 			- if no promotion is able to be made
	 *			Protocol.Promote.CHAR_REP_INVALID 		- if the given charRep is not valid
	 */
	public abstract int promote(String gameID, char charRep);
	
	/**
	 * Attempt to offer/accept a draw on behalf of the user in the given game.
	 * @param gameID - the game in which to offer/accept a draw
	 * @return 	Protocol.SERVER_ERROR 				- if an error is encountered  <br>
	 *			Protocol.Draw.SUCCESS 				- if draw offer/accept is successful  <br>
	 *			Protocol.Draw.GAME_DOES_NOT_EXIST 	- if given game does not exist  <br>
	 *			Protocol.Draw.USER_NOT_IN_GAME 		- if the user isn't a player in the given game  <br>
	 *			Protocol.Draw.NO_OPPONENT 			- if the user doesn't have an opponent in the given game yet  <br>
	 *			Protocol.Draw.GAME_IS_OVER			- if the given game is already over
	 *			Protocol.Draw.NOT_USER_TURN 		- if it's not the user's turn in the given game
	 */
	public abstract int draw(String gameID);
	
	/**
	 * Attempt to reject a draw on behalf of the user in the given game.
	 * @param gameID - the game in which to reject a draw
	 * @return 	Protocol.SERVER_ERROR 					- if an error is encountered  <br>
	 *			Protocol.Reject.SUCCESS 				- if draw offer/accept is successful  <br>
	 *			Protocol.Reject.GAME_DOES_NOT_EXIST 	- if given game does not exist  <br>
	 *			Protocol.Reject.USER_NOT_IN_GAME 		- if the user isn't a player in the given game  <br>
	 *			Protocol.Reject.NO_OPPONENT 			- if the user doesn't have an opponent in the given game yet  <br>
	 *			Protocol.Reject.GAME_IS_OVER			- if the given game is already over <br>
	 *			Protocol.Reject.NOT_USER_TURN 			- if it's not the user's turn in the given game <br>
	 *			Protoco.Reject.NO_DRAW_OFFERED			- if there is no draw offer for the user to reject in the given game <br>
	 */
	public abstract int reject(String gameID);
	
	/**
	 * Attempt to forfeit the given game on behalf of the user.
	 * 
	 * @param gameID - the game to forfeit
	 * @return  Protocol.SERVER_ERROR 					- if an error is encountered
	 *			Protocol.Forfeit.FORFEIT 				- if forfeiture is successful
	 *			Protocol.Forfeit.GAME_DOES_NOT_EXIST 	- if the given game does not exist
	 *			Protocol.Forfeit.USER_NOT_IN_GAME 		- if the user is not in the given game
	 *			Protocol.Forfeit.NO_OPPONENT 			- if the user does not have an opponent in the given game
	 * 			Protocol.Forfeit.GAME_IS_OVER 			- if the given game is already over
	 *			Protocol.Forfeit.NOT_USER_TURN 			- if it is not the user’s turn
	 */
	public abstract int forfeit(String gameID);
	
	/**
	 * Attempt to mark the given game as archived for the user.
	 * 
	 * @param gameID - the game to mark as archived
	 * @return 	Protocol.SERVER_ERROR 					– if an error is encountered
	 *			Protocol.Archive.SUCCESS 				– if the archive is successful
	 *			Protocol.Archive.GAME_DOES_NOT_EXIST 	– if the given game does not exist
	 *			Protocol.Archive.USER_NOT_IN_GAME 		– if the user is not in the given game
	 */
	public abstract int archive(String gameID);
	
	/**
	 * Attempt to restore, or "un-archive", the given game for the user. That is, simply mark
	 * it as not archived.
	 * 
	 * @param gameID - the game to un-archive
	 * @return 	Protocol.SERVER_ERROR 					– if an error is encountered
	 *			Protocol.Restore.SUCCESS 				– if the restoration is successful
	 *			Protocol.Restore.GAME_DOES_NOT_EXIST 	– if the given game does not exist
	 *			Protocol.Restore.USER_NOT_IN_GAME 		– if the user is not in the given game
	 */
	public abstract int restore(String gameID);
	
	public String getUsername() {
		return username;
	}
}
