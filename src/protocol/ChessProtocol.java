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
	
	// The object responsible for 
	private ClientManager manager;
	
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
					// return processCreateGame(rest);
				}
				else if(keyword.equals("joingame")) {
					// return processJoingame(rest);
				}
				else if(keyword.equals("loadgame")) {
					
				}
				else if(keyword.equals("move")) {
					
				}
			}
			return 0;
			// If keyword is not in the list of valid keywords, do nothing
		}
		else {
			if(command.equals("logout")) {
				// Reassign manager reference so that manager can be garbage-collected
				manager = null;
			}
			return 0;
			// Otherwise do nothing as we have received an invalid command
		}
	}
	
//	/**
//	 * Process a "joingame gameID" request. gameID is of course
//	 * assumed to be the "gameID" part of the joingame request
//	 * 
//	 * @param gameID
//	 * @return 0 if the socket that this object is writing to is still connected <br>
//	 *         1 if the socket is found to have been disconnected
//	 */
//	private int processJoingame(String gameID) {
//		
//	}
 
//	/**
//	 * Process a "creategame gameID" request. gameID is of course
//	 * assumed to be the "gameID" part of the creategame request
//	 * 
//	 * @param gameID - The gameID of the new game to create
//	 * @return 0 if the socket that this object is writing to is still connected <br>
//	 *         1 if the socket is found to have been disconnected
//	 */
//	private int processCreateGame(String gameID) {
//		
//	}

	/**
	 * Process a "create username password" request. rest is assumed
	 * to be the "username password" portion of the create command
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
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String username = splitted[0];
		String password = splitted[1];
		AccountManager accountManager = AccountManagerFactory.build();
		
		// Check that the desired username and password are correctly formatted
		if(!accountManager.validUsername(username) || !accountManager.validPassword(password)) {
			return this.writeToClient(Create.FORMAT_INVALID);
		}
		
		int result = accountManager.addAccount(username, password);
		if(result == 0) {
			this.manager = ClientManagerFactory.build(username);
			return this.writeToClient(Create.SUCCESS);
		}
		else if (result == 1) {
			return this.writeToClient(Create.USERNAME_IN_USE);
		}
		else {
			return this.writeToClient(SERVER_ERROR);
		}
	}

	/**
	 * Process a "login username password" request. rest is assumed to be
	 * the "username password" portion of the login command
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
			Log.log("ERROR: Server error encountered in AccountManager.usernameExists()");
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
			Log.log("ERROR: Server error encountered in AccountManager.validCredentials()");
			return this.writeToClient(SERVER_ERROR);
		}
		
		// If the user has successfully been logged in, we need to send the client all of
		// the user's game data
		this.manager = ClientManagerFactory.build(username);
		List<HashMap<GameData, Object>> games = manager.getGameData();
		
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
	 * Write the given integer to this object's DataOutputStream. Returns an integer
	 * according to the state of the connection with the client. Calls System.exit(1)
	 * if the write fails due to an IOException
	 * @param code - The int to write to the client
	 * @return 0 if the write succeeds and the connection with the client remains intact <br>
	 * 		   1 if the client is found to have disconnected
	 */
	private int writeToClient(int code) {
		try {
			 out.writeInt(code);
			 return 0;
		}
		catch(SocketException e) {
			return 1;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("Encountered exception writing to " + socket.getInetAddress());
			System.exit(1);
			return 1; // We'll never get here, but have to do this to make the compiler happy
		}
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
		try {
			for(int i = 0; i < msg.length(); i++) {
				out.write(msg.charAt(i));
			}
			out.write('\r');
			out.write('\n');
			return 0;
		}
		catch(SocketException e) {
			return 1;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("Encountered exception writing to " + socket.getInetAddress());
			System.exit(1);
			return 1; // We'll never get here, but have to do this to make the compiler happy
		}
	}
}
