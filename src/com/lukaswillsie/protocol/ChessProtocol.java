package com.lukaswillsie.protocol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

import com.lukaswillsie.data.AccountManager;
import com.lukaswillsie.data.AccountManagerFactory;
import com.lukaswillsie.data.Game;
import com.lukaswillsie.data.GameManager;
import com.lukaswillsie.data.GameManagerFactory;
import com.lukaswillsie.data.GameData;
import com.lukaswillsie.utility.Log;

import Chess.com.lukaswillsie.chess.Pair;

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
	private GameManager manager;
	
	// The username of whatever user is logged in by the client at the moment.
	private String username;
	
	// If a request from the client is of the following format: keyword <params>,
	// then this is a list of valid keyword strings.
	private static final String[] KEYWORDS =
		{
			"login",		// Usage: login username password
			"create",		// Usage: create username password
			"creategame",	// Usage: creategame gameID
			"joingame",		// Usage: joingame gameID
			"loadgame",		// Usage: loadgame gameID
			"loadgames",	// Usage: loadgames
			"getgamedata",	// Usage: gamedata gameID
			"opengames",	// Usage: opengames
			"move",			// Usage: move gameID src_row,src_col->dest_row,dest_col
			"promote",		// Usage: promote gameID charRep
			"draw",			// Usage: draw gameID
			"reject",		// Usage: reject gameID
			"forfeit",		// Usage: forfeit gameID
			"archive",		// Usage: archive gameID
			"restore",		// Usage: restore gameID
			"logout"		// Usage: logout
		};	
	
	/**
	 * Create a new ChessProtocol object to write to the given
	 * socket
	 * @param socket The socket that this ChessProtocol object will write to
	 */
	ChessProtocol(Socket socket) {
		manager = GameManagerFactory.build();
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
					return processJoinGame(rest);
				}
				else if(keyword.equals("loadgame")) {
					return processLoadGame(rest);
				}
				else if(keyword.equals("getgamedata")) {
					return processGameData(rest);
				}
				else if(keyword.equals("move")) {
					return processMove(rest);
				}
				else if(keyword.equals("promote")) {
					return processPromotion(rest);
				}
				else if(keyword.equals("draw")) {
					return processDraw(rest);
				}
				else if(keyword.equals("forfeit")) {
					return processForfeit(rest);
				}
				else if(keyword.equals("archive")) {
					return processArchive(rest);
				}
				else if(keyword.equals("restore")) {
					return processRestore(rest);
				}
				else if(keyword.equals("reject")) {
					return processReject(rest);
				}
			}
			else {
				Log.log("Command \"" + command + "\" is invalid.");
			}
			return 0;
			// If keyword is not in the list of valid keywords, do nothing
		}
		else {
			if(command.equals("logout")) {
				Log.log("Logged out user " + this.username + " for client " + socket.getInetAddress());
				
				this.username = null;
			}
			else if(command.equals("opengames")) {
				return processOpenGames();
			}
			else if(command.equals("loadgames")) {
				return processLoadGames();
			}
			else {
				Log.log("Command \"" + command + "\" is invalid.");
			}
			// We return 0 in either above case because here no communication is made with the client, so we 
			// assume they remain connected
			return 0;
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
		
		// Check input for validity; rest should be of the form "username password",
		// where "username" and "password" both don't contain spaces
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
		
		if(accountManager.addAccount(username, password)) {
			Log.log("Adding account " + username + "," + password);
			this.username = username;
			return this.writeToClient(Create.SUCCESS);
		}
		else {
			Log.log("Username " + username + " is already in use.");
			return this.writeToClient(Create.USERNAME_IN_USE);
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
		if(!accountManager.usernameExists(username)) {
			Log.log("Username " + username + " does not exist.");
			return this.writeToClient(Login.USERNAME_DOES_NOT_EXIST);
		}
		
		// Check whether the given username-password combo is valid
		if(!accountManager.validCredentials(username, password)) {
			Log.log("Username,password combination " + username + "," + password + " is invalid.");
			return this.writeToClient(Login.PASSWORD_INVALID);
		}
		else {
			// Write the success code to the client, but don't return unless they disconnected.
			// We have more data to send.
			Log.log("Login request " + username + "," + password + " is valid. Notifying client...");
			if(this.writeToClient(Login.SUCCESS) == 1) {
				return 1;
			}
		}
		
		// If the user has successfully been logged in, we need to send the client all of
		// the user's game data
		this.username = username;
		
		List<Game> games = manager.getGames(username);
		if(games == null) {
			Log.error("Error encountered in ClientManager.getGames()");
			return this.writeToClient(SERVER_ERROR);
		}
		
		// First we send the user the number of games to expect to receive
		int status = writeToClient(games.size());
		if(status == 1) {
			return 1;
		}
		
		// Then we take each game and write all of its data to the client in the proper order
		
		for(Game game : games) {
			for(GameData data : GameData.order) {
				if(data.type == 'i') {
					status = writeToClient((Integer)game.getData(data));
				}
				else {
					status = writeToClient((String)game.getData(data));
				}
				
				if(status == 1) {
					return 1;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Process a "creategame gameID open" request. rest is assumed to be the
	 * "gameID open" part of the creategame request. This method does not assume that
	 * this String is properly formatted, so no pre-processing is required.
	 * 
	 * @param rest - The part of a "creategame" command following "creategame "
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processCreateGame(String rest) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot create a game.");
			return this.writeToClient(NO_USER);
		}
		
		// rest should be of the form "gameID open". So we split it about the space
		// and parse it
		String[] splitted = rest.split(" ");
		if(splitted.length != 2) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		String gameID = splitted[0];
		boolean open;
		try {
			int val = Integer.parseInt(splitted[1]);
			if(val == 0) {
				open = false;
			}
			else if (val == 1) {
				open = true;
			}
			else {
				Log.log("\"open\" bit value from " + socket.getInetAddress() + " is not 0 or 1");
				return this.writeToClient(FORMAT_INVALID);
			}
		}
		catch(NumberFormatException e) {
			Log.log("\"open\" bit value from " + socket.getInetAddress() + " couldn't be converted to int");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		// gameIDs that contain commas are invalid (messes up the .csv file),
		// as are those that are just empty strings
		if(gameID.indexOf(',') != -1 || gameID.length() == 0) {
			Log.log("GameID \"" + gameID + "\" is invalidly formatted");
			return this.writeToClient(CreateGame.FORMAT_INVALID);
		}
		
		int code = this.manager.createGame(gameID, username, open);
		if(code == CreateGame.SUCCESS) {
			Log.log("Game \"" + gameID + "\" successfully created.");
			return this.writeToClient(CreateGame.SUCCESS);
		}
		else if (code == CreateGame.GAMEID_IN_USE) {
			Log.log("GameID \"" + gameID + "\" is already in use.");
			return this.writeToClient(CreateGame.GAMEID_IN_USE);
		}
		else {
			Log.error("Error encountered in ClientManager.createGame()");
			return this.writeToClient(SERVER_ERROR);
		}
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
	private int processJoinGame(String gameID) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot join a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = manager.joinGame(gameID, username);
		if(code == JoinGame.SUCCESS) {
			Log.log("User " + this.username + " joined game \"" + gameID + "\" successfully");
			return this.writeToClient(JoinGame.SUCCESS);
		}
		else if (code == JoinGame.GAME_DOES_NOT_EXIST) {
			Log.log("Game \"" + gameID + "\" does not exist, so it could not be joined.");
			return this.writeToClient(JoinGame.GAME_DOES_NOT_EXIST);
		}
		else if (code == JoinGame.USER_ALREADY_IN_GAME) {
			Log.log("User " + this.username + " is already in game \"" + gameID + "\"");
			return this.writeToClient(JoinGame.USER_ALREADY_IN_GAME);
		}
		else if (code == JoinGame.GAME_FULL) {
			Log.log("Game \"" + gameID + "\" is already full, so it could not be joined");
			return this.writeToClient(JoinGame.GAME_FULL);
		}
		else {
			Log.error("Error encountered in manager.joinGame()");
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
	private int processLoadGame(String gameID) {
		// The client can't create a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot load a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = this.manager.canLoadGame(gameID, username);
		if(code == LoadGame.SUCCESS) {
			List<Object> lines = this.manager.loadGame(gameID);
			// Since we checked canLoadGame() first, we know that if lines is null an error occurred
			if(lines == null) {
				Log.error("An error occurred in ClientManager.loadGame()");
				return this.writeToClient(SERVER_ERROR);
			}
			
			Game game = this.manager.getGameData(gameID);
			if(game == null) {
				Log.error("loadGame() returned fine but getGameData() returned null");
				return this.writeToClient(SERVER_ERROR);
			}
			
			// First we write to the client to tell them that we're about to send over the data
			if(this.writeToClient(LoadGame.SUCCESS) == 1) {
				return 1;
			}
			
			// First, we write all the high-level data stored in the Game object to the user
			for(GameData data : GameData.order) {
				if(data.type == 'i') {
					code = this.writeToClient((Integer) game.getData(data));
					if(code == 1) {
						return 1;
					}
				}
				else if(data.type == 's') {
					code = this.writeToClient((String) game.getData(data));
					if(code == 1) {
						return 1;
					}
				}
			}
			
			// Next, we write the game's board-level data to the client
			for(Object o : lines) {
				// The definition of loadGame() in GameManager tells us that everything in lines
				// is guaranteed to either be an Integer or String
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
		else if (code == LoadGame.GAME_DOES_NOT_EXIST) {
			Log.log("Game \"" + gameID + "\" does not exist, so it couldn't be joined");
			return this.writeToClient(LoadGame.GAME_DOES_NOT_EXIST);
		}
		else if(code == LoadGame.USER_NOT_IN_GAME) {
			Log.log("User " + username + " is not in game \"" + gameID + "\", so game wasn't loaded");
			return this.writeToClient(LoadGame.USER_NOT_IN_GAME);
		}
		else {
			Log.log("ERROR: Error encountered in GameManager.canLoadGame()");
			return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * If the client has a user logged in, we send the data associated with all that user's
	 * games to the client, in exactly the same way we do immediately after a user logs in. 
	 * @return 0 if the command is processed and the client is still believed to be connected
	 * 			 when the method terminates <br>
	 * 		   1 if the client is found to have disconnected during the execution of this method
	 */
	private int processLoadGames() {
		if(username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot load games.");
			return this.writeToClient(NO_USER);
		}
		
		List<Game> userGames = manager.getGames(username);
		if(userGames == null) {
			Log.error("Manager thinks username \"" + username + "\" that we haved logged in is not in system.");
			return this.writeToClient(SERVER_ERROR);
		}
		else {
			// Otherwise, notify the client that we are about to send them the games
			int status = this.writeToClient(LoadGames.SUCCESS);
			if(status == 1) {
				return 1;
			}
		}
		
		// First tell the client how many games to expect
		int status = this.writeToClient(userGames.size());
		if(status == 1) {
			return 1;
		}
		
		// Write all the games to the client
		for(Game game : userGames) {
			for(GameData data : GameData.order) {
				if(data.type == 'i') {
					status = this.writeToClient((int) game.getData(data));
				}
				else {
					status = this.writeToClient((String) game.getData(data));
				}
				
				if(status == 1) {
					return 1;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Process a "getgamedata gameID" request. Gets all the data associated with the game
	 * that has the given ID, if there is one, and sends it to the client. rest is assumed to
	 * be everything after "getgamedata " in the initial request (does not have to be pre-processed)
	 * 
	 * @param rest - everything following the "getgamedata " part of a request from a client
	 * @return 0 if the command is processed and the client is still believed to be connected
	 * 			 when the method terminates <br>
	 * 		   1 if the client is found to have disconnected during the execution of this method
	 */
	private int processGameData(String rest) {
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot get open games.");
			return this.writeToClient(NO_USER);
		}
		
		int res = manager.canLoadGame(rest, this.username);
		if(res == Protocol.SERVER_ERROR) {
			Log.log("Error encountered in GameManager.canLoadGame()");
			return this.writeToClient(SERVER_ERROR);
		}
		else if (res == Protocol.LoadGame.GAME_DOES_NOT_EXIST) {
			Log.log("Game \"" + rest + "\" does not exist");
			return this.writeToClient(GetGameData.GAME_DOES_NOT_EXIST);
			
		}
		else if (res == Protocol.LoadGame.USER_NOT_IN_GAME) {
			Log.log("User \"" + username + "\" is not in game \"" + rest + "\"");
			return this.writeToClient(GetGameData.USER_NOT_IN_GAME);
		}
		// Only other return code is success, so we proceed.
		
		Game game = manager.getGameData(rest);
		if(game == null) {
			Log.log("Manager told us game \"" + rest + "\" exists but returned null in getGame()");
			return this.writeToClient(SERVER_ERROR);
		}
		// First we notify the client that we're about to send along the data they wanted
		else {
			res = this.writeToClient(GetGameData.SUCCESS);
			if(res == 1) {
				return 1;
			}
		}
		
		for(GameData data : GameData.order) {
			if(data.type == 'i') {
				res = this.writeToClient((Integer)game.getData(data));
				if(res == 1) {
					return 1;
				}
			}
			else if (data.type == 's') {
				res = this.writeToClient((String)game.getData(data));
				if(res == 1) {
					return 1;
				}
			}
		}
		
		return 0;
	}

	/**
	 * Process an "opengames" request. Gets a list of all open games in the system and sends them
	 * to the client.
	 * 
	 * @return 0 if the command is processed and the client is still believed to be connected
	 * 			 when the method terminates <br>
	 * 		   1 if the client is found to have disconnected during the execution of this method
	 */
	private int processOpenGames() {
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot get open games.");
			return this.writeToClient(NO_USER);
		}
		
		List<Game> openGames = manager.openGames();
		// If there's an error, notify the client
		if(openGames == null) {
			return this.writeToClient(SERVER_ERROR);
		}
		
		// Otherwise, we first tell them how many games to expect
		int write = this.writeToClient(openGames.size());
		if(write == 1) {
			return 1;
		}
		
		// And then write each game's data to them in the order defined by GameData.order
		for(Game game : openGames) {
			for(GameData data : GameData.order) {
				if(data.type == 'i') {
					write = this.writeToClient((Integer) game.getData(data));
					if(write == 1) {
						return 1;
					}
				}
				else {
					write = this.writeToClient((String) game.getData(data));
					if(write == 1) {
						return 1;
					}
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Process a "move gameID src_row,src_col->dest_row,dest_col" command. rest
	 * is assumed to be the "gameID src_row,src_col->dest_row,dest_col" part of the
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
		if(this.username == null) {
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
		
		Pair src_square;
		Pair dest_square;
		try {
			src_square = new Pair(Integer.parseInt(src.substring(0,src_comma)), Integer.parseInt(src.substring(src_comma+1)));
			dest_square = new Pair(Integer.parseInt(dest.substring(0,dest_comma)), Integer.parseInt(dest.substring(dest_comma+1)));
		}
		catch(NumberFormatException e) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid. NumberFormatException");
			return this.writeToClient(FORMAT_INVALID);
		}
		
		int result = this.manager.makeMove(gameID, src_square, dest_square, username);
		switch(result) {
				case Move.SUCCESS:
					Log.log("Move " + src + "->" + dest + " successfully made by user \"" + this.username + "\"");
					return this.writeToClient(Move.SUCCESS);
				case Move.SUCCESS_PROMOTION_NEEDED:
					Log.log("Move " + src + "->" + dest + " successfully made by user \"" + this.username + "\", and now they need to promote");
					return this.writeToClient(Move.SUCCESS_PROMOTION_NEEDED);
				case Move.GAME_DOES_NOT_EXIST:
					Log.log("Game \"" + gameID + "\" does not exist");
					return this.writeToClient(Move.GAME_DOES_NOT_EXIST);
				case Move.USER_NOT_IN_GAME:
					Log.log("User \"" + this.username + "\" is not in game \"" + gameID + "\". Move couldn't be made.");
					return this.writeToClient(Move.USER_NOT_IN_GAME);
				case Move.NO_OPPONENT:
					Log.log("User \"" + this.username + "\" has no opponent in game \"" + gameID + "\". Move couldn't be made.");
					return this.writeToClient(Move.NO_OPPONENT);
				case Move.GAME_IS_OVER:
					Log.log("Game \"" + gameID + "\" is already over. Move couldn't be made.");
					return this.writeToClient(Move.GAME_IS_OVER);
				case Move.NOT_USER_TURN:
					Log.log("It is not user \"" + this.username + "\"'s turn in game \"" + gameID + "\". Move couldn't be made");
					return this.writeToClient(Move.NOT_USER_TURN);
				case Move.HAS_TO_PROMOTE:
					Log.log("User \"" + this.username + "\" has to promote a pawn. Move couldn't be made.");
					return this.writeToClient(Move.HAS_TO_PROMOTE);
				case Move.RESPOND_TO_DRAW:
					Log.log("User \"" + this.username + "\" has to respond to a draw offer. Move couldn't be made.");
					return this.writeToClient(Move.RESPOND_TO_DRAW);
				case Move.MOVE_INVALID:
					Log.log("The move " + src + "->" + dest + " is invalid");
					return this.writeToClient(Move.MOVE_INVALID);
				default: // The only other case is server error
					Log.log("Error encountered in ClientManager.makeMove");
					return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "promote gameID charRep" request. rest is assumed to be the "gameID charRep"
	 * part of the request, or more generally everything after "promote ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID charRep" part of a "promote gameID charRep" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processPromotion(String rest) {
		// The client can't promote a pawn for a user if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot process promotion.");
			return this.writeToClient(NO_USER);
		}
				
		String[] splitted = rest.split(" ");
		if(splitted.length != 2 || splitted[1].length() != 1) {
			Log.log("Command from " + socket.getInetAddress() + " is invalid");
			return this.writeToClient(FORMAT_INVALID);
		}
		String gameID = splitted[0];
		String charRep = splitted[1];
		
		int code = this.manager.promote(gameID, charRep.charAt(0), username);
		switch(code) {
			case Promote.SUCCESS:
				Log.log("Promotion to " + charRep + " successful");
				return this.writeToClient(Promote.SUCCESS);
			case Promote.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + gameID + "\" does not exist. Promotion wasn't successful");
				return this.writeToClient(Promote.GAME_DOES_NOT_EXIST);
			case Promote.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + gameID + "\". Promotion wasn't successful");
				return this.writeToClient(Promote.USER_NOT_IN_GAME);
			case Promote.NO_OPPONENT:
				Log.log("User \"" + this.username + "\" does not have opponent in game \"" + gameID + "\". Promotion wasn't successful");
				return this.writeToClient(Promote.NO_OPPONENT);
			case Promote.GAME_IS_OVER:
				Log.log("Game \"" + gameID + "\" is already over. Promotion wasn't successful");
				return this.writeToClient(Promote.GAME_IS_OVER);
			case Promote.NOT_USER_TURN:
				Log.log("It is not user \"" + this.username + "\"'s turn in game \"" + gameID + "\". Promotion wasn't successful");
				return this.writeToClient(Promote.NOT_USER_TURN);
			case Promote.NO_PROMOTION:
				Log.log("No promotion is necessary in game \"" + gameID + "\". Promotion wasn't successful");
				return this.writeToClient(Promote.NO_PROMOTION);
			case Promote.CHAR_REP_INVALID:
				Log.log("charRep " + charRep + " is invalid. Promotion wasn't successful");
				return this.writeToClient(Promote.CHAR_REP_INVALID);
			default: // Only other case is server error
				Log.error("Error encountered in ClientManager.promote()");
				return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "draw gameID" request. rest is assumed to be the "gameID"
	 * part of the request, or more generally everything after "draw ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID" part of a "draw gameID" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processDraw(String rest) {
		// The client can't offer/accept a draw for a user if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot process draw attempt.");
			return this.writeToClient(NO_USER);
		}
				
		int code = this.manager.draw(rest, username);
		
		switch(code) {
			case Draw.SUCCESS:
				Log.log("Draw offer/accept successful in game \"" + rest + "\"." );
				return this.writeToClient(Draw.SUCCESS);
			case Draw.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + rest + "\" does not exist. Draw offer/accept was not successful." );
				return this.writeToClient(Draw.GAME_DOES_NOT_EXIST);
			case Draw.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + rest + "\". Draw offer/accept was not successful.");
				return this.writeToClient(Draw.USER_NOT_IN_GAME);
			case Draw.NO_OPPONENT:
				Log.log("User \"" + this.username + "\" has no opponent in game \"" + rest + "\". Draw offer/accept was not successful.");
				return this.writeToClient(Draw.NO_OPPONENT);
			case Draw.GAME_IS_OVER:
				Log.log("Game \"" + rest + "\" is already over. Draw offer/accept was not successful.");
				return this.writeToClient(Draw.GAME_IS_OVER);
			case Draw.NOT_USER_TURN:
				Log.log("It is not user \"" + this.username + "\"'s turn in game \"" + rest + "\". Draw offer/accept was not successful.");
				return this.writeToClient(Draw.NOT_USER_TURN);
			default:	// The only other case is server error
				Log.error("Error encountered in ClientManager.draw(). Draw offer/accept was not successful.");
				return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "reject gameID" request. rest is assumed to be the "gameID"
	 * part of the request, or more generally everything after "reject ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID" part of a "reject gameID" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processReject(String rest) {
		// The client can't offer/accept a draw for a user if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot process draw rejection.");
			return this.writeToClient(NO_USER);
		}
				
		int code = this.manager.reject(rest, username);
		
		switch(code) {
			case Reject.SUCCESS:
				Log.log("Draw rejection successful in game \"" + rest + "\"." );
				return this.writeToClient(Reject.SUCCESS);
			case Reject.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + rest + "\" does not exist. Draw rejection was not successful." );
				return this.writeToClient(Reject.GAME_DOES_NOT_EXIST);
			case Reject.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + rest + "\". Draw rejection was not successful.");
				return this.writeToClient(Reject.USER_NOT_IN_GAME);
			case Reject.NO_OPPONENT:
				Log.log("User \"" + this.username + "\" has no opponent in game \"" + rest + "\". Draw rejection was not successful.");
				return this.writeToClient(Reject.NO_OPPONENT);
			case Reject.GAME_IS_OVER:
				Log.log("Game \"" + rest + "\" is already over. Draw rejection was not successful.");
				return this.writeToClient(Reject.GAME_IS_OVER);
			case Reject.NOT_USER_TURN:
				Log.log("It is not user \"" + this.username + "\"'s turn in game \"" + rest + "\". Draw rejection was not successful.");
				return this.writeToClient(Reject.NOT_USER_TURN);
			case Reject.NO_DRAW_OFFER:
				Log.log("There is no draw offer for user \"" + this.username + "\" to reject. Draw rejection was not successful.");
				return this.writeToClient(Reject.NO_DRAW_OFFER);
			default: 	// The only other case is server error
				Log.error("Error encountered in ClientManager.draw(). Draw rejection was not successful.");
				return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "forfeit gameID" request. rest is assumed to be the "gameID"
	 * part of the request, or more generally everything after "forfeit ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID" part of a "forfeit gameID" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processForfeit(String rest) {
		// The client can't forfeit a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot process forfeiture.");
			return this.writeToClient(NO_USER);
		}
				
		int code = this.manager.forfeit(rest, username);
		
		switch(code) {
			case Forfeit.SUCCESS:
				Log.log("Forfeit successful in game \"" + rest + "\"." );
				return this.writeToClient(Forfeit.SUCCESS);
			case Forfeit.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + rest + "\" does not exist. Forfeit was not successful." );
				return this.writeToClient(Forfeit.GAME_DOES_NOT_EXIST);
			case Forfeit.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + rest + "\". Forfeit was not successful.");
				return this.writeToClient(Forfeit.USER_NOT_IN_GAME);
			case Forfeit.NO_OPPONENT:
				Log.log("User \"" + this.username + "\" has no opponent in game \"" + rest + "\". Forfeit was not successful.");
				return this.writeToClient(Forfeit.NO_OPPONENT);
			case Forfeit.GAME_IS_OVER:
				Log.log("Game \"" + rest + "\" is already over. Forfeit was not successful.");
				return this.writeToClient(Forfeit.GAME_IS_OVER);
			case Forfeit.NOT_USER_TURN:
				Log.log("It is not user \"" + this.username + "\"'s turn in game \"" + rest + "\". Forfeit was not successful.");
				return this.writeToClient(Forfeit.NOT_USER_TURN);
			default: 
				Log.error("Error encountered in ClientManager.draw(). Forfeit was not successful.");
				return this.writeToClient(SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "archive gameID" request. rest is assumed to be the "gameID"
	 * part of the request, or more generally everything after "archive ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID" part of a "archive gameID" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processArchive(String rest) {
		// The client can't archive a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot archive a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = this.manager.archive(rest, username);
		
		switch(code) {
			case Protocol.Archive.SUCCESS:
				Log.log("Game \"" + rest + "\" archived successfully by user \"" + this.username + "\".");
				return this.writeToClient(Protocol.Archive.SUCCESS);
			case Protocol.Archive.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + rest + "\" does not exist. Archive was not successful.");
				return this.writeToClient(Protocol.Archive.GAME_DOES_NOT_EXIST);
			case Protocol.Archive.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + rest + "\". Archive was not successful.");
				return this.writeToClient(Protocol.Archive.USER_NOT_IN_GAME);
			default: // Only other case is server error
				Log.error("Error encountered in ClientManager.archive()");
				return this.writeToClient(Protocol.SERVER_ERROR);
		}
	}
	
	/**
	 * Process a "restore gameID" request. rest is assumed to be the "gameID"
	 * part of the request, or more generally everything after "restore ". This
	 * method checks the input for incorrect formatting.
	 * 
	 * @param rest - the "gameID" part of a "restore gameID" request
	 * @return 0 if the socket that this object is writing to is still connected <br>
	 *         1 if the socket is found to have been disconnected
	 */
	private int processRestore(String rest) {
		// The client can't archive a game if it hasn't logged in a user, so check if
		// it's logged anyone in and send the appropriate return code if they haven't.
		if(this.username == null) {
			Log.log("Client " + socket.getInetAddress() + " does not have a user logged in. Cannot restore a game.");
			return this.writeToClient(NO_USER);
		}
		
		int code = this.manager.restore(rest, username);
		
		switch(code) {
			case Protocol.Restore.SUCCESS:
				Log.log("Game \"" + rest + "\" restored successfully by user \"" + this.username + "\".");
				return this.writeToClient(Protocol.Restore.SUCCESS);
			case Protocol.Restore.GAME_DOES_NOT_EXIST:
				Log.log("Game \"" + rest + "\" does not exist. Restoration was not successful.");
				return this.writeToClient(Protocol.Restore.GAME_DOES_NOT_EXIST);
			case Protocol.Restore.USER_NOT_IN_GAME:
				Log.log("User \"" + this.username + "\" is not in game \"" + rest + "\". Restoration was not successful.");
				return this.writeToClient(Protocol.Restore.USER_NOT_IN_GAME);
			default: // Only other case is server error
				Log.error("Error encountered in ClientManager.restore()");
				return this.writeToClient(Protocol.SERVER_ERROR);
		}
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
		try {
			 out.writeInt(num);
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
		
		// These lines are here for testing purposes
//		System.out.println(num);
//		return 0;
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
			Log.log("Encountered exception writing to " + socket.getInetAddress() + ". Exiting thread");
			System.exit(1);
			return 1; // We'll never get here, but have to do this to make the compiler happy
		}
		
		// These lines are here for testing purposes
//		System.out.println(msg);
//		return 0;
	}
}
