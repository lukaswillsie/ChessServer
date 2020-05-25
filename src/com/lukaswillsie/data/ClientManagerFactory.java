package com.lukaswillsie.data;

/**
 * This class exists to create FileClientManager (implementation) objects and return
 * them as ClientManager (interface) instances, to maintain dependency inversion
 * @author lukas
 *
 */
public class ClientManagerFactory {
	/**
	 * Create a ClientManager to interact with data on behalf of the given user
	 * @param username - The user to create a ClientManager for
	 * @return A ClientManager object for the given user
	 */
	public static ClientManager build(String username) {
		return new FileClientManager(username);
	}
}
