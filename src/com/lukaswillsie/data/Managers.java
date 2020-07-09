package com.lukaswillsie.data;

/**
 * Provides centralized, global access to the two crucial data managers
 * used by this program. Also encapsulates the build process to make setting
 * up the server simpler.
 * @author Lukas Willsie
 *
 */
public class Managers {
	private static AccountDataManager accountManager;
	private static GameDataManager gameManager;
	
	/**
	 * Get a reference to the AccountDataManager stored in this class.
	 * Will return null if the class hasn't been built using the build()
	 * method.
	 * 
	 * @return A reference to the AccountDataManager the program is using to
	 * query and update account information
	 */
	public static AccountManager getAccountManager() {
		return accountManager;
	}
	
	/**
	 * Get a reference to the GameDataManager stored in this class. Will
	 * return null if the class hasn't been built using the build() method.
	 * 
	 * @return A reference to the GameDataManager the program is using to query
	 * and update game information
	 */
	public static GameDataManager getGameManager() {
		return gameManager;
	}
	
	/**
	 * Builds the manager objects contained in this class, so that they
	 * can be accessed at will from here on out. As the build process may fail,
	 * this method returns a code indicating success or failure.
	 * 
	 * @return 0 on succcess; this class' get() methods may now be called and will
	 * not return null. 1 if some aspect of the build failed and this class' get()
	 * methods will return null
	 */
	public static int build() {
		accountManager = new AccountDataManager();
		int built = accountManager.build();
		if(built == 1) {
			accountManager = null;
			return 1;
		}
		
		gameManager = new GameDataManager(accountManager);
		built = gameManager.build();
		
		if(built == 1) {
			accountManager = null;
			gameManager = null;
			return 1;
		}
		
		return 0;
	}
}
