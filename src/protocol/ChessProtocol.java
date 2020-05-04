package protocol;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;

import data.AccountManager;
import data.AccountManagerFactory;
import data.ClientManager;
import data.ClientManagerFactory;
import utility.Log;

class ChessProtocol implements Protocol {
	private PrintWriter out;
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
		try{out = new PrintWriter(socket.getOutputStream(), true);}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Process the given command
	 */
	@Override
	public void processCommand(String command) {
		int space = command.indexOf(' ');
		if(space != -1) {
			String keyword = command.substring(0, space);
			String rest = command.substring(space+1);
			if(Arrays.asList(KEYWORDS).contains(keyword)) {
				if(keyword.equals("login")) {
					loginUser(rest);
				}
				else if(keyword.equals("create")) {
					createUser(rest);
				}
				else if(keyword.equals("creategame")) {
					createGame(rest);
				}
				else if(keyword.equals("joingame")) {
					joinGame(rest);
				}
				else if(keyword.equals("loadgame")) {
					
				}
				else if(keyword.equals("move")) {
					
				}
			}
			// If keyword is not in the list of valid keywords, do nothing
		}
		else {
			if(command.equals("logout")) {
				// Reassign manager reference so that manager can be garbage-collected
				manager = null;
			}
			// Otherwise do nothing as we have received an invalid command
		}
	}
	
	/**
	 * Process a "joingame gameID" request. gameID is of course
	 * assumed to be the "gameID" part of the joingame request
	 * 
	 * @param gameID
	 */
	private void joinGame(String gameID) {
		if(manager == null) {
			out.println("-2");
			return;
		}
		
		int result = manager.joinGame(gameID);
		if(result == 0) {
			out.println("0");
			Log.log(manager.getUsername() + " joined game " + gameID);
		}
		else if(result == 1) {
			out.println("1");
			Log.log(manager.getUsername() + " tried to join game " + gameID + " which does not exist");
		}
		else if (result == 2) {
			out.println("2");
			Log.log(manager.getUsername() + " tried to join game " + gameID + " which is already full");
		}
		else if (result == 3) {
			out.println("3");
			Log.log(manager.getUsername() + " tried to join game " + gameID + " which they have already joined");
		}
		else {
			out.println("-1");
		}
	}
 
	/**
	 * Process a "creategame gameID" request. gameID is of course
	 * assumed to be the "gameID" part of the creategame request
	 * 
	 * @param gameID - The gameID of the new game to create
	 */
	private void createGame(String gameID) {
		if(manager == null) {
			out.println("0 No user has been logged in.");
			return;
		}
		
		int result = manager.createGame(gameID);
		if(result == 0) {
			out.println("0");
			Log.log(manager.getUsername() + " created game " + gameID);
		}
		else if (result == 1) {
			out.println("1");
			Log.log("Could not create game " + gameID + " for " + manager.getUsername() + ". Game already exists.");
		}
		else {
			out.println("-1");
		}
	}

	/**
	 * Process a "create username password" request. rest is assumed
	 * to be the "username password" portion of the create command
	 * 
	 * @param rest - A String containing the part of the create command
	 * following "create"
	 */
	private void createUser(String rest) {
		String username, password;
		
		int space = rest.indexOf(' ');
		if(space == -1) {
			out.println("0 Invalid input");
			return;
		}
		
		username = rest.substring(0,space);
		password = rest.substring(space+1);
		
		AccountManager accountManager = AccountManagerFactory.build();
		
		if(!(accountManager.validPassword(password) && accountManager.validUsername(username))) {
			out.println("0 Invalid format. Usernames and passwords must be non-empty.");
			Log.log("\"create " + username + " " + password + "\" request failed. Invalid format.");
			return;
		}
		
		int created = accountManager.addAccount(username, password);
		if(created == 0) {
			out.println("1");
			Log.log("Account " + username + "," + password + " created.");
		}
		else if (created == 1) {
			out.println("0 That username is taken.");
			Log.log("\"create " + username + " " + password + "\" command failed. Username is taken.");
		}
		else {
			out.println("0 An error occurred, please try again later.");
		}
		
	}

	/**
	 * Process a "login username password" request. rest is assumed to be
	 * the "username password" portion of the login command
	 * 
	 * @param rest - A String containing the part of the login command following "login "
	 */
	private void loginUser(String rest) {
		String username, password;
		
		int space = rest.indexOf(' ');
		if(space == -1) {
			out.println("0 Invalid input");
			return;
		}
		
		username = rest.substring(0,space);
		password = rest.substring(space+1);
		
		AccountManager accountManager = AccountManagerFactory.build();
		
		int result = accountManager.validCredentials(username, password);
		
		// User's credentials are invalid
		if(result == 0) {
			out.println("0 That username and password do not match.");
			Log.log("Could not log in user " + username + "," + password);
		}
		// User's credentials are valid; notify the client and begin to send board data
		else if (result == 1) {
			out.println("1");
			Log.log("Logged in user " + username);
			manager = ClientManagerFactory.build(username);
		}
		else {
			out.println("0 An error occurred. Please try again later.");
		}
	}
}
