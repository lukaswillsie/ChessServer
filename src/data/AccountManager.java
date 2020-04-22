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
	 * Returns a boolean indicating whether or not account creation was successful
	 * 
	 * @param username - The username to create the new account under
	 * @param password - The password to create the new account under
	 * @return One of the following:
	 * 		0 - the account was successfully created
	 * 		1 - the account cannot be created because the given username already exists
	 * 		2 - an error/exception occurred
	 */
	int addAccount(String username, String password);
	
	/**
	 * Checks whether or not the given username is properly formatted
	 * @param username - The username to check
	 * @return Whether or not the given username is properly formatted
	 */
	boolean validUsername(String username);
	
	/**
	 * Checks whether or not the given password is properly formatted
	 * @param password - The password to check
	 * @return Whether or not the given password is properly formatted
	 */
	boolean validPassword(String password);
}
