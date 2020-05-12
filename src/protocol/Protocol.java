package protocol;

/**
 * Defines what a class that implements the protocol detailed in Protocols.pdf
 * should be able to do. Also centralizes and standardizes the definitions of 
 * all of the server's return codes, for use by implementations
 * @author Lukas Willsie
 *
 */
public interface Protocol {
	// Return code if a critical error is encountered while processing a command
	public static final int SERVER_ERROR = -1;
	
	// Return code if an invalidly formatted command is received
	public static final int FORMAT_INVALID = -2;
	
	// Return code if a client tries to make a request before logging in a user
	public static final int NO_USER = -3;
	
	/**
	 * Any Protocol object should be able to take a String, a command from a client,
	 * parse the command, and perform/delegate the necessary computations and send an
	 * appropriate response to the client.
	 * 
	 * Protocols should return an int detailing whether the protocol discovered the client
	 * to have disconnected in the course of its work.
	 * 
	 * @param command - A command received from a client
	 * @return 0 if the client is still thought to be connected to the server when this method terminates
	 * 		   1 if the client is found to have disconnected
	 */
	int processCommand(String command);
	
	/**
	 * Defines return codes specific to the "login username password" command, for
	 * logging in existing users
	 * @author Lukas Willsie
	 *
	 */
	public static class Login {
		// Return code on successful login
		public static final int SUCCESS = 0;
		
		// Return code if the provided username does not exist (is not in use)
		public static final int USERNAME_DOES_NOT_EXIST = 1;
		
		// Return code if the provided password is incorrect
		public static final int PASSWORD_INVALID = 2;
	}
	
	/**
	 * Defines return codes specific to the "create username password" command,
	 * for creating new user accounts
	 * @author Lukas Willsie Willsie
	 *
	 */
	public static class Create {
		// Return code on successful creation of new account
		public static final int SUCCESS = 0;
		
		// Return code if the username provided in the command is already in use
		public static final int USERNAME_IN_USE = 1;
		
		// Return code if either the username or password is formatted incorrectly
		// For example, is empty or contains a comma
		public static final int FORMAT_INVALID = 2;
	}
	
	/**
	 * Defines return codes specific to the "creategame gameID" command, for creating
	 * new games
	 * @author Lukas Willsie
	 *
	 */
	public static class CreateGame {
		// Return code on successful game creation
		public static final int SUCCESS = 0;
		
		// Return code if the gameID provided in the command is already in use
		public static final int GAMEID_IN_USE = 1;
		
		// Return code if the provided gameID is invalidly formatted
		public static final int FORMAT_INVALID = 2;
	}
	
	/**
	 * Defines return codes specific to the "joingame gameID" command, for joining existing
	 * games
	 * @author Lukas Willsie
	 *
	 */
	public static class JoinGame {
		// Return code if the user was able to successfully join the given games
		public static final int SUCCESS = 0;
		
		// Return code if provided gameID does not represent an existing game
		public static final int GAME_DOES_NOT_EXIST = 1;
		
		// Return code if the given gameID already has two players
		public static final int GAME_FULL = 2;
		
		// Return code if the user has already joined the game represented by the
		// provided gameID
		public static final int USER_ALREADY_IN_GAME = 3;
	}
	
	public static class LoadGame {
		public static final int SUCCESS = 0;
		public static final int GAME_DOES_NOT_EXIST = 1;
		public static final int USER_NOT_IN_GAME = 2;
	}
	
	/**
	 * Defines return codes specific to the "move gameID src_row,src_col->dest_row,dest_col"
	 * command, for making moves in a game
	 * 
	 * @author Lukas Willsie
	 *
	 */
	public static class Move {
		// Return code if the move is successfully made and the game is updated
		public static final int SUCCESS = 0;
		
		// Return code if the game specified in the command does not exist
		public static final int GAME_DOES_NOT_EXIST = 1;
		
		// Return code if the user attempts to make a move in a game they aren't
		// a part of
		public static final int USER_NOT_IN_GAME = 2;
		
		// Return code if the user atttempts to make a move in a game that they ARE
		// a part of, but in which they have no opponent
		public static final int NO_OPPONENT = 3;
		
		// Return code if the user is trying to make a move in a game that is already
		// over
		public static final int GAME_IS_OVER = 4;
		
		// Return code if it is not the user's turn
		public static final int NOT_USER_TURN = 5;
		
		// Return code if it is the user's turn, but they have to promote a piece,
		// not make a normal move
		public static final int HAS_TO_PROMOTE = 6;
		
		// Return code if it is the user's turn, but it's because they have to respond
		// to a draw offer, rather than because they have to make a normal move
		public static final int RESPOND_TO_DRAW = 7;
		
		// Return code if the requested move is invalid
		public static final int MOVE_INVALID = 8;
	}
	
	/**
	 * Defines return codes specific to the "promote gameID charRep" command.
	 * charRep must be one of 'r', 'n', 'b', 'q', as a pawn can be promoted
	 * to a Rook, Knight, Bishop, or Queen respectively.
	 * 
	 * @author lukas
	 *
	 */
	public static class Promote {
		// Return code if promotion is successful
		public static final int SUCCESS = 0;
		
		// Return code if given game does not exist
		public static final int GAME_DOES_NOT_EXIST = 1;
		
		// Return code if the user isn't a player in the given game
		public static final int USER_NOT_IN_GAME = 2;
		
		// Return code if the the user doesn't have an opponent in the given game yet
		public static final int NO_OPPONENT = 3;
		
		// Return code if the given game is already over
		public static final int GAME_IS_OVER = 4;
		
		// Return code if it's not the user's turn
		public static final int NOT_USER_TURN = 5;
		
		// Return code if no promotion is able to be made
		public static final int NO_PROMOTION = 6;
		
		// Return code if the given charRep is not valid
		public static final int CHAR_REP_INVALID = 7;
	}
	
	/**
	 * Defines return codes specific to the "draw gameID" command.
	 * 
	 * @author lukas
	 *
	 */
	public static class Draw {
		// Return code if draw offer/accept is successful
		public static final int SUCCESS = 0;
		
		// Return code if given game does not exist
		public static final int GAME_DOES_NOT_EXIST = 1;
		
		// Return code if the user isn't a player in the given game
		public static final int USER_NOT_IN_GAME = 2;
		
		// Return code if the user doesn't have an opponent in the given game yet
		public static final int NO_OPPONENT = 3;
		
		// Return code if the given game is already over
		public static final int GAME_IS_OVER = 4;
		
		// Return code if the user doesn't have an opponent in the given game yet
		public static final int NOT_USER_TURN = 5;
	}
}
