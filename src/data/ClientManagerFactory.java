package data;

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
