package data;

/**
 * A class that exists only to create FileAccountManager (implementation)
 * instances and return them as AccountManager (interface) instances for maintaining
 * dependency inversion
 * @author lukas
 *
 */
public class AccountManagerFactory {
	/**
	 * Create and return an AccountManager object
	 * 
	 * @return An AccountManager object
	 */
	public static AccountManager build() {
		return new FileAccountManager();
	}
}
