package data;

public class AccountManagerFactory {
	public static AccountManager build() {
		return new FileAccountManager();
	}
}
