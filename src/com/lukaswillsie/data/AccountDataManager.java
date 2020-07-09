package com.lukaswillsie.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import com.lukaswillsie.utility.Log;

/**
 * Manages data regarding user accounts at runtime in a reasonable and efficient way.
 * Keeps a registry of all the system's users in memory, and periodically saves information
 * to disk.
 * 
 * This class is intended to be used as a singleton. Also, an AccountDataManager
 * object SHOULD NOT be used until build() has been called and returned a successful
 * code.
 * 
 * @author Lukas Willsie
 */
public class AccountDataManager implements AccountManager {
	/**
	 * Define the locations on disk of our key files
	 */
	private static final String accountsFile = "serverdata/accounts.csv";
	private static final String dumpFile = "serverdata/error/users_error.txt";
	
	/**
	 * How many user accounts we'll allow to be added before we save to disk
	 */
	private static int USER_ACCOUNTS_BEFORE_SAVE = 10;
	
	/**
	 * A list of all new user accounts that we've created but so far but whose
	 * existence we haven't yet saved to disk
	 */
	private List<User> unsavedUsers = new ArrayList<User>();
	
	/**
	 * Keeps track of all the user accounts in the system at runtime. Maps usernames
	 * to User objects.
	 */
	private HashMap<String, User> users = new HashMap<String, User>();
	
	/**
	 * Set this object up for use. AccountDataManager objects should NOT be used unless
	 * this method has been called and has returned successfully.
	 * 
	 * @return 0 if the build process is successful, 1 otherwise
	 */
	public int build() {
		// In case this is our first execution, create any requisite file(s)
		if(createFiles() == 1) {
			return 1;
		}
		
		Scanner scanner;
		try {
			scanner = new Scanner(new File(accountsFile));
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open accounts file for scanning");
			return 1;
		}
		
		// Process the accounts file
		String line;
		String[] split;
		int lineNumber = 1;
		User user;
		while(scanner.hasNextLine()) {
			line = scanner.nextLine();
			split = line.split(",");
			
			if(split.length != 2) {
				Log.error("ERROR: Line " + lineNumber + " of accounts file is incorrectly formatted");
				return 1;
			}
			
			// Add the user to our HashMap
			user = new User(split[0], split[1]);
			addUser(user);
			
			lineNumber++;
		}
		
		return 0;
	}
	
	/**
	 * Save all of this object's unsaved Users to disk. Goes through unsavedUsers, removing
	 * Users if their information is successfully saved, leaving them in the list and logging
	 * their information to the console if not.
	 */
	private void save() {
		try(FileOutputStream usersOutput = new FileOutputStream(new File(accountsFile), true)) {
			String line;
			User unsaved;
			
			int i = 0;
			while(i < unsavedUsers.size()) {
				unsaved = unsavedUsers.get(i);
				line = unsaved.getUsername() + "," + unsaved.getPassword() + "\n";
				try {
					usersOutput.write(line.getBytes());
					unsavedUsers.remove(i);
				}
				catch(IOException e) {
					Log.error("ERROR: COULDN'T SAVE USER (" + unsaved.getUsername() + "," + unsaved.getPassword() + ")");
					i++;
				}
			}
		} catch (FileNotFoundException e) {
			Log.error("ERROR: Couldn't open accounts file to save user data");
		}
		// Thrown if usersOutput fails to close
		catch (IOException e) {
			Log.error("ERROR: Couldn't close accounts FileOutputStream");
			e.printStackTrace();
		}
	}
	
	/**
	 * "Dumps" the contents of this object to a file, specified by this class' dumpFile field.
	 * If this fails, due to an I/O error, we dump this object's data to the console, by
	 * printing what the contents of the save file WOULD be, had the save succeeded. The dump file,
	 * and the console if necessary, will contain EXACTLY what the save file would have contained,
	 * had the save succeeded.
	 * 
	 * We use this method as a backup in the event that a save operation fails, so that there's
	 * a record of the data contained in this object SOMEWHERE.
	 */
	private void dumpUsers() {
		List<String> contents = new ArrayList<String>();
		
		for(User user : unsavedUsers) {
			contents.add(user.getUsername() + "," + user.getPassword());
		}
		
		Log.error("DUMPING UNSAVED USERS TO DISK");
		try {
			FileOutputStream stream = new FileOutputStream(new File(dumpFile));
			for(String line : contents) {
				stream.write((line + "\n").getBytes());
			}
			Log.error("FINISHED DUMPING TO FILE \"" + dumpFile + "\"");
			return;
		}
		catch(FileNotFoundException e) {
			Log.error("COULDN'T OPEN DUMP FILE");
		} catch (IOException e) {
			Log.error("EXCEPTION WHILE WRITING TO DUMP FILE");
		}
		
		// Note that we only dump to the console, which is messy and unseemly, if the file
		// operation above fails
		Log.error("DUMPING UNSAVED USERS TO CONSOLE...");
		
		contents.add("FINISHED DUMPING TO CONSOLE");

		Log.error(contents);
	}
	
	/**
	 * Create any and all files that this object needs to exist in order to run. Files already in existence
	 * are not changed.
	 * 
	 * Currently, this just creates the text file that we keep our list of users in
	 * 
	 * @return 0 if there are no errors, 1 otherwise
	 */
	private int createFiles() {
		File accounts = new File(accountsFile);
		try {
			// We don't care about createNewFile's return value because regardless,
			// we know that the file exists
			accounts.createNewFile();
		} catch (IOException e) {
			Log.error("ERROR: Couldn't create accounts file with path \"" + accountsFile + "\"");
			return 1;
		}
		
		return 0;
	}

	/**
	 * Adds the given user to our records. Does nothing, but logs the issue as a warning,
	 * if the given user has the same username as another user already in our records.
	 * 
	 * @param user - the user to add to our collection
	 */
	private void addUser(User user) {
		if(users.get(user.getUsername()) == null) {
			users.put(user.getUsername(), user);
			unsavedUsers.add(user);
		}
		else {
			Log.log("WARNING: Tried to add users with username \"" + user.getUsername() + "\" twice");
		}
	}
	
	/**
	 * Compute whether or not the given username and password are a valid
	 * login combination
	 * 
	 * @param username - The username to check
	 * @param password - The password to check
	 * @return true if and only if there is a user with the given username and password
	 * in the system
	 */
	@Override
	public boolean validCredentials(String username, String password) {
		User user = users.get(username);
		
		if(user == null) {
			return false;
		}
		else if (user.getPassword().equals(password)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Check if the given username is already being used in the system
	 * 
	 * @param username - The username to check
	 * @return true if and only if the given username is already associated with an
	 * account in the sytem
	 */
	@Override
	public boolean usernameExists(String username) {
		return users.get(username) != null;
	}
	
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
	@Override
	public boolean addAccount(String username, String password) {
		if(usernameExists(username)) {
			return false;
		}
		else if (!validUsername(username) || !validPassword(password)) {
			return false;
		}
		else {
			User newUser = new User(username, password);
			addUser(newUser);
			// Check if we've added enough accounts that we now need to save
			if(unsavedUsers.size() > USER_ACCOUNTS_BEFORE_SAVE) {
				save();
			}
			
			return true;
		}
	}

	/**
	 * An implementation of this interface should implement this method and the one below so 
	 * that users are able to check that a given username,password combination is properly
	 * formatted and won't cause any problems for the implementation before attempting to
	 * create a new account for a user.
	 * 
	 * @param username - The username to check
	 * @return Whether or not the given username is properly formatted
	 */
	@Override
	public boolean validUsername(String username) {
		return username.length() >= 1 && username.indexOf(',') == -1 && username.indexOf(' ') == -1;
	}
	
	/**
	 * An implementation of this interface should implement this method and the one above so 
	 * that users are able to check that a given username,password combination is properly
	 * formatted and won't cause any problems for the implementation before attempting to
	 * create a new account for a user.
	 * 
	 * @param username - The username to check
	 * @return Whether or not the given username is properly formatted
	 */
	@Override
	public boolean validPassword(String password) {
		return password.length() >= 1 && password.indexOf(',') == -1 && password.indexOf(' ') == -1;
	}

}
