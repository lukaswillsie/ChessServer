package data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Scanner;

import utility.Log;

public class FileAccountManager implements AccountManager {
	// A String constant that holds the path to the accounts file, relative to
	// the executable for the server
	private static final String ACCOUNTS_FILENAME = "serverdata/accounts.csv";
	
	private File accounts;
	
	FileAccountManager(){
		accounts = new File(ACCOUNTS_FILENAME);
	}
	
	/**
	 * Compute whether or not the given username and password are a valid
	 * login combination by checking the accounts file
	 * 
	 * @param username - The username to check
	 * @param password - The password to check
	 * @return One of the following: <br>
	 * 		0 - the credentials are valid and a login should be allowed <br>
	 * 		1 - the credentials are not valid <br>
	 * 		2 - an error/exception occurred <br>
	 */
	@Override
	public int validCredentials(String username, String password) {
		try(Scanner scanner = new Scanner(accounts))
		{
			String line;
			// Iterate through the accounts file until the given username and password
			// are found together
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				int comma = line.indexOf(',');
				if(comma == -1) {
					Log.log("Accounts file is incorrectly formatted");
					return 2;
				}
				
				String name = line.substring(0, comma);
				
				if(username.equals(name)) {
					String rest = line.substring(comma+1);
					return password.equals(rest) ? 1 : 0;
				}
			}
			
			return 0;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("Encountered exception while validating credentials:\nUsername: " + username + "\nPassword: " + password);
			return 2;
		}
	}
	
	/**
	 * Check if the given username is already being used in the system
	 * 
	 * @param username - The username to check
	 * @return One of the following: <br>
	 * 		0 - username does not exist <br>
	 * 		1 - the username exists <br>
	 * 		2 - an error/exception occurred <br>
	 */
	@Override
	public int usernameExists(String username) {
		try(Scanner scanner = new Scanner(accounts)){
			String line;
			while(scanner.hasNextLine()) {
				line = scanner.nextLine();
				int comma = line.indexOf(',');
				if(comma == -1) {
					Log.log("Accounts file is incorrectly formatted");
					return 2;
				}
				
				String name = line.substring(0,comma);
				if(username.equals(name)) {
					return 1;
				}
			}
			
			return 0;
		}
		catch(IOException e) {
			e.printStackTrace();
			Log.log("Encountered exception while checking if username \"" + username + "\" exists ");
			return 2;
		}
	}
	
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
	@Override
	public int addAccount(String username, String password) {
		try(OutputStream out = new FileOutputStream(accounts, true)) {
			int exists = this.usernameExists(username);
			switch(exists) {
				case 0:
					String line = username + "," + password + "\n";
					out.write(line.getBytes());
					return 0;
				case 1:
					return 1;
				default:
					Log.log("Error encountered in usernameExists while trying to add account:\nUsername: " + username + "\nPassword: " + password);
					return 2;
			}
			
		}
		catch(IOException e) {
			Log.log("Error adding account:\nUsername: " + username + "\nPassword: " + password);
			return 2;
		}
	}

	@Override
	public boolean validUsername(String username) {
		return username.length() >= 1;
	}

	@Override
	public boolean validPassword(String password) {
		return password.length() >= 1;
	}

}
