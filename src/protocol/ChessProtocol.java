package protocol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import data.AccountManager;
import data.AccountManagerFactory;
import data.ClientManager;
import data.ClientManagerFactory;
import data.GameData;
import protocol.Protocol;
import utility.Log;
import utility.Pair;

/**
 * This class is an implementation of the Protocol interface (in this package) that
 * processes commands and issues responses on behalf of a chess server. It is responsible
 * only for parsing input and issuing output. Processing and data manipulation is handed off
 * to a ClientManager when necessary
 * 
 * @author Lukas Willsie
 *
 */
class ChessProtocol implements Protocol {
	// The tool this object will use to write to the client
	private DataOutputStream out;
	
	// The socket that this object is writing to. Is used for logging purposes
	private Socket socket;
	
	// The object responsible for actually managing and accessing data for the client.
	// This oject is only initialized once the client logs in a user. So if this field
	// has null value, we know the client hasn't logged in a user
	private ClientManager manager;
	
	// The username of whatever user is logged in by the client at the moment.
	// Used for logging purposes.
	private String username;
	
	private static final String[] KEYWORDS =
		{
			"login",		// Usage: login username password
			"create",		// Usage: create username password
			"creategame",	// Usage: creategame gameID
			"joingame",		// Usage: joingame gameID
			"loadgame",		// Usage: loadgame gameID
			"move",			// Usage: TODO
			"logout"		// Usage: logout
		};	
	
	/**
	 * Create a new ChessProtocol object to write to the given
	 * socket
	 * @param socket The socket that this ChessProtocol object will write to
	 */
	ChessProtocol(Socket socket) {
		try{
			out = new DataOutputStream(socket.getOutputStream());
			this.socket = socket;
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Process the given command
	 * 
	 * @param command - The command to process
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have disconnected
	 */
	@Override
	public int processCommand(String command) {
		int space = command.indexOf(' ');
		if(space != -1) {
			String keyword = command.substring(0, space);
			String rest = command.substring(space+1).trim();
			
			if(Arrays.asList(KEYWORDS).contains(keyword)) {
				if(keyword.equals("login")) {
					return processLogin(rest);
				}
				else if(keyword.equals("create")) {
					return processCreateUser(rest);
				}
				else if(keyword.equals("creategame")) {
					return processCreateGame(rest);
				}
				else if(keyword.equals("joingame")) {
					return processJoingame(rest);
				}
				else if(keyword.equals("loadgame")) {
					return processLoadgame(rest);
				}
				else if(keyword.equals("move")) {
					return processMove(rest);
				}
				else if(keyword.equals("promote")) {
					
				}
				else {
					Log.log("Command \"" + command + "\" is invalid.");
				}
			}
			return 0;
			// If keyword is not in the list of valid keywords, do nothing
		}
		else {
			if(command.equals("logout")) {
				// Reassign manager reference so that manager can be garbage-collected
				this.manager = null;
				this.username = null;
				Log.log("Logged out user " + this.username + " for client " + socket.getInetAddress());
			}
			return 0;
			// Otherwise do nothing as we have received an invalid command
		}
	}
	
	/**
	 * Process a "login username password" request. rest is assumed to be
	 * the "username password" portion of the login command. This String is 
	 * not assumed to be formatted correctly, so no pre-processing is necessary.
	 * 
	 * @param rest - A String containing the part of the login command following "login "
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processLogin(String rest) {
		String[] splitted = rest.split(" ");
		
		// Check input for validity
		if(splitted.length != 2) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String username = splitted[0];
		String password = splitted[1];
		AccountManager accountManager = AccountManagerFactory.build();
		
		// Check whether or not the given username exists
		int result = accountManager.usernameExists(username);
		if(result == 0) {
			Log.log("Username " + username + " does not exist.");
			return this.writeToClient(Login.USERNAME_DOES_NOT_EXIST);
		}
		else if (result == 2) {
			Log.error("ERROR: Server error encountered in AccountManager.usernameExists()");
			return this.writeToClient(SERVER_ERROR);
		}
		
		// Check whether the given username-password combo is valid
		result = accountManager.validCredentials(username, password);
		if(result == 0) {
			Log.log("Username,password combination " + username + "," + password + " is invalid.");
			return this.writeToClient(Login.PASSWORD_INVALID);
		}
		else if (result == 1) {
			// Write the success code to the client, but don't return unless they disconnected.
			// We have more data to send
			Log.log("Login request " + username + "," + password + " is valid. Notifying client...");
			if(this.writeToClient(Login.SUCCESS) == 1) {
				return 1;
			}
		}
		else {
			Log.error("ERROR: Server error encountered in AccountManager.validCredentials()");
			return this.writeToClient(SERVER_ERROR);
		}
		
		// If the user has successfully been logged in, we need to send the client all of
		// the user's game data
		this.manager = ClientManagerFactory.build(username);
		this.username = username;
		
		List<HashMap<GameData, Object>> games = manager.getGameData();
		if(games == null) {
			Log.error("ERROR: Error encountered in ClientManager.getGameData()");
			return this.writeToClient(SERVER_ERROR);
		}
		
		// First we send the user the number of games to expect to receive
		writeToClient(games.size());
		
		// Then we take each game and write all of its data to the client in the proper order
		int status;
		for(HashMap<GameData, Object> game : games) {
			for(GameData data : GameData.order) {
				if(data.type == 'i') {
					status = writeToClient((Integer)game.get(data));
					if(status == 1) {
						return 1;
					}
				}
				else {
					status = writeToClient((String)game.get(data));
					if(status == 1) {
						return 1;
					}
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Process a "joingame gameID" request. gameID is assumed to be the
	 * "gameID" part of the joingame request. The gameID is not assumed to
	 * be properly formatted, so no pre-processing is necessary.
	 * 
	 * @param gameID
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processJoingame(String gameID) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.manager == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot create a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = manager.joinGame(gameID);
		if(code == 0) {
			Log.log("User " + this.username + " joined game \"" + gameID + "\" successfully");
			return this.writeToClient(JoinGame.SUCCESS);
		}
		else if (code == 1) {
			Log.log("Game \"" + gameID + "\" does not exist, so it could not be joined.");
			return this.writeToClient(JoinGame.GAME_DOES_NOT_EXIST);
		}
		else if (code == 2) {
			Log.log("Game \"" + gameID + "\" is already full, so it could not be joined");
			return this.writeToClient(JoinGame.GAME_FULL);
		}
		else if (code == 3) {
			Log.log("User " + this.username + " is already in game \"" + gameID + "\"");
			return this.writeToClient(JoinGame.USER_ALREADY_IN_GAME);
		}
		else {
			Log.error("ERROR: error encountered in manager.joinGame()");
			return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "create username password" request. rest is assumed
	 * to be the "username password" portion of the create command. This method
	 * does not assume that this String is properly formatted, so no pre-processing
	 * is necessary. 
	 * 
	 * @param rest - A String containing the part of the create command
	 * following "create"
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processCreateUser(String rest) {
		String[] splitted = rest.split(" ");
		
		// Check input for validity; we should have
		if(splitted.length != 2) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String username = splitted[0];
		String password = splitted[1];
		AccountManager accountManager = AccountManagerFactory.build();
		
		// Check that the desired username and password are correctly formatted
		if(!accountManager.validUsername(username) || !accountManager.validPassword(password)) {
			Log.log("One of " + username + "," + password + " is invalidly formatted");
			return this.writeToClient(Create.FORMAT_INVALID);
		}
		
		int result = accountManager.addAccount(username, password);
		if(result == 0) {
			Log.log("Adding account " + username + "," + password);
			this.manager = ClientManagerFactory.build(username);
			this.username = username;
			return this.writeToClient(Create.SUCCESS);
		}
		else if (result == 1) {
			Log.log("Username " + username + " is already in use.");
			return this.writeToClient(Create.USERNAME_IN_USE);
		}
		else {
			Log.error("ERROR: error encountered in AccountManager.addAccount()");
			return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "creategame gameID" request. gameID is assumed to be the
	 * "gameID" part of the creategame request. This method does not assume that
	 * this String is properly formatted, so no pre-processing is required.
	 * 
	 * @param gameID - The gameID of the new game to create
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processCreateGame(String gameID) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.manager == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot create a game.");
			return this.writeToClient(NO_USER);
		}
		
		// gameIDs that contain commas are invalid (messes up the .csv file),
		// as are those that are just empty strings
		if(gameID.indexOf(',') != -1 || gameID.length() == 0) {
			Log.log("GameID \"" + gameID + "\" is invalidly formatted");
			return this.writeToClient(CreateGame.FORMAT_INVALID);
		}
		
		int code = this.manager.createGame(gameID);
		if(code == 0) {
			Log.log("Game \"" + gameID + "\" successfully created.");
			return this.writeToClient(CreateGame.SUCCESS);
		}
		else if (code == 1) {
			Log.log("GameID \"" + gameID + "\" is already in use.");
			return this.writeToClient(CreateGame.GAMEID_IN_USE);
		}
		else {
			Log.error("ERROR: error encountered in ClientManager.createGame()");
			return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "load gameID" request. gameID is assumed to be the "gameID" part of the load request.
	 * This method checks if this part of the request is invalidly formatted before proceeding, so no
	 * pre-processing is necessary
	 * 
	 * @param gameID - The part of the "load gameID" request following "load "
	 * @return 0 if the command is processed and the client is still believed to be connected
	 * 			 when the method terminates <br>
	 * 		   1 if the client is found to have disconnected during the execution of this method
	 */
	private int processLoadgame(String gameID) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.manager == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot load a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = this.manager.canLoadGame(gameID);
		if(code == 0) {
			List<Object> lines = this.manager.loadGame(gameID);
			// Since we check canLoadGame() first, we know that if lines is null an error occurred
			if(lines == null) {
				Log.error("ERROR: An error occurred in ClientManager.loadGame()");
				return 0;
			}
			
			// The definition of loadGame() in ClientManager tells us that everything in lines
			// is guaranteed to either be an Integer or String
			for(Object o : lines) {
				if(o instanceof Integer) {
					code = this.writeToClient((Integer)o);
					if(code == 1) {
						return 1;
					}
				}
				else if (o instanceof String) {
					code = this.writeToClient((String)o);
					if(code == 1) {
						return 1;
					}
				}
			}
			
			return 0;
		}
		else if (code == 1) {
			Log.log("Game \"" + gameID + "\" does not exist, so it couldn't be joined");
			return this.writeToClient(LoadGame.GAME_DOES_NOT_EXIST);
		}
		else if(code == 2) {
			Log.log("User " + username + " is not in game \"" + gameID + "\", so game wasn't loaded");
			return this.writeToClient(LoadGame.USER_NOT_IN_GAME);
		}
		else {
			Log.log("ERROR: Error encountered in ClientManager.canLoadGame()");
			return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "move gameID src_row,src_col->dest_row,dest_col" command. rest
	 * is assumed to be that "gameID src_row,src_col->dest_row,dest_col" part of the
	 * command, but is not assumed to be formatted correctly, so no pre-processing
	 * is necessary.
	 * 
	 * @param rest - The "gameID src_row,src_col->dest_row,dest_col" part of a move command
	 * @return 0 if the command is processed and the client is still believed to be connected
	 * 			 when the method terminates <br>
	 * 		   1 if the client is found to have disconnected during the execution of this method
	 */
	private int processMove(String rest) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.manager == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot make a move.");
			return this.writeToClient(NO_USER);
		}
		
		// First, parse rest into its components, error-checking along the way
		int space = rest.indexOf(' ');
		if(space == -1) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid.");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String gameID = rest.substring(0,space);
		String move = rest.substring(space+1);
		
		int arrow = move.indexOf("->");
		if(arrow == -1) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid.");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String src = move.substring(0,arrow);
		String dest = move.substring(arrow+2);
		
		int src_comma = src.indexOf(',');
		int dest_comma = dest.indexOf(',');
		if(src_comma == -1 || dest_comma == -1) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid.");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		try {
			Pair src_square = new Pair(Integer.parseInt(src.substring(0,src_comma)), Integer.parseInt(src.substring(src_comma+1)));
			Pair dest_square = new Pair(Integer.parseInt(dest.substring(0,dest_comma)), Integer.parseInt(dest.substring(dest_comma+1)));
		}
		catch(NumberFormatException e) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid. NumberFormatException");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		
		return 0;
	}
	
	/**
	 * Write the given integer to this object's DataOutputStream. Returns an integer
	 * according to the state of the connection with the client. Calls System.exit(1)
	 * if the write fails due to an IOException
	 * @param num - The int to write to the client
	 * @return 0 if the write succeeds and the connection with the client remains intact <br>
	 * 		   1 if the client is found to have disconnected
	 */
	private int writeToClient(int num) {
//		try {
//			 out.writeInt(code);
//			 return 0;
//		}
//		catch(SocketException e) {
//			return 1;
//		}
//		catch(IOException e) {
//			e.printStackTrace();
//			Log.log("Encountered exception writing to " + socket.getInetAddress());
//			System.exit(1);
//			return 1; // We'll never get here, but have to do this to make the compiler happy
//		}
		System.out.println(num);
		return 0;
	}
	
	/**
	 * Write the given String as a full line of data to the client's socket. Here,
	 * "writing" means writing each character in msg to the socket as a ONE-BYTE character
	 * (by throwing away the most significant byte of each 2-byte char), followed by a
	 * network newline, "\r\n".
	 * 
	 * Note that the original message does NOT need to include a network newline.
	 * @param msg - The message to be written to the client
	 * @return 0 if the write succeeds and the connection with the client remains intact <br>
	 * 		   1 if the client is found to have disconnected
	 */
	private int writeToClient(String msg) {
//		try {
//			for(int i = 0; i < msg.length(); i++) {
//				out.write(msg.charAt(i));
//			}
//			out.write('\r');
//			out.write('\n');
//			return 0;
//		}
//		catch(SocketException e) {
//			return 1;
//		}
//		catch(IOException e) {
//			e.printStackTrace();
//			Log.log("Encountered exception writing to " + socket.getInetAddress() + ". Exiting thread");
//			System.exit(1);
//			return 1; // We'll never get here, but have to do this to make the compiler happy
//		}
		System.out.println(msg);
		return 0;
	}
}
