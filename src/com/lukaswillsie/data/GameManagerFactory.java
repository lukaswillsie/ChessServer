package com.lukaswillsie.data;

/**
 * This class exists to create FileClientManager (implementation) objects and return
 * them as ClientManager (interface) instances, to maintain dependency inversion
 * @author lukas
 *
 */
public class GameManagerFactory {
	/**
	 * Create a ClientManager for querying and updating game data
	 *
	 * @return A ClientManager object ready for use
	 */
	public static GameManager build() {
		return Managers.getGameManager();
	}
}
