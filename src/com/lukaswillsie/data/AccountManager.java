package com.lukaswillsie.data;

/**
 * An interface that defines the basic functions necessary for
 * maintaining and querying a user account system
 * 
 * @author Lukas Willsie
 */
public interface AccountManager {
	/**
	 * Compute whether or not the given username and password are a valid
	 * login combination
	 * 
	 * @param username - The username to check
	 * @param password - The password to check
	 * @return true if and only if there is a user with the given username and password
	 * in the system
	 */
	boolean validCredentials(String username, String password);
	
	/**
	 * Check if the given username is already being used in the system
	 * 
	 * @param username - The username to check
	 * @return true if and only if the given username is already associated with an
	 * account in the sytem
	 */
	boolean usernameExists(String username);
	
	/**
	 * Create a new account under the given username and password. Returns false if the
	 * given username is already associated with a user in the system, or if either of the
	 * given username or password is invalidly formatted, according to validUsername() and
	 * validPassword().
	 * 
	 * @param username - The username to create the new account under
	 * @param password - The password to create the new account under
	 * @return true if and only if an account is successfully created with the given username
	 * and password
	 */
	boolean addAccount(String username, String password);
	
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
