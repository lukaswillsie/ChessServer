package data;

/**
 * An interface that defines the basic necessary functionality
 * for managing and accessing user account data
 * @author Lukas Willsie
 */
public interface AccountManager {
	/**
	 * Compute whether or not the given username and password are a valid
	 * login combination
	 * @param username - The username to check
	 * @param password - The password to check
	 * @return One of the following: <br>
	 * 		0 - the credentials are not valid <br>
	 * 		1 - the credentials are valid and a login should be allowed <br>
	 * 		2 - an error/exception occurred <br>
	 */
	int validCredentials(String username, String password);
	
	/**
	 * Check if the given username is already being used in the system
	 * 
	 * @param username - The username to check
	 * @return One of the following: <br>
	 * 		0 - username does not exist <br>
	 * 		1 - the username exists <br>
	 * 		2 - an error/exception occurred <br>
	 */
	int usernameExists(String username);
	
	/**
	 * Create a new account under the given username and password.
	 * 
	 * Returns an integer indicating whether or not account creation was successful
	 * 
	 * @param username - The username to create the new account under
	 * @param password - The password to create the new account under
	 * @return One of the following: <br>
	 * 		0 - the account was successfully created <br>
	 * 		1 - the account cannot be created because the given username already exists <br>
	 * 		2 - an error/exception occurred <br>
	 */
	int addAccount(String username, String password);
	
	/**
	 * An implementation of this interface should implement this method and the one below so 
	 * that users are able to check that a given username,password combination is properly
	 * formatted and won't cause any problems for the implementation before attempting to
	 * create a new account for a user.
	 * 
	 * @param username - The username to check
	 * @return Whether or not the given username is properly formatted
	 */
	boolean validUsername(String username);
	
	/**
	 * An implementation of this interface should implement this method and the one above so 
	 * that users are able to check that a given username,password combination is properly
	 * formatted and won't cause any problems for the implementation before attempting to
	 * create a new account for a user.
	 * 
	 * @param username - The username to check
	 * @return Whether or not the given username is properly formatted
	 */
	boolean validPassword(String password);
}
