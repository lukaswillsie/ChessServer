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
	 * Defines return codes specific to the "login" command
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
	 * Defines return codes specific to the "create" command
	 * @author Lukas Willsie
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
}
