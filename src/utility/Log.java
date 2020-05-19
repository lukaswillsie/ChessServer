package utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A class that provides various methods for logging information while the server is running
 * 
 * @author Lukas Willsie
 *
 */
public class Log {
	/**
	 * Simply log the given message to the console (standard out). This method exists for two
	 * reasons:<br> 	1) 	Log.log() is prettier than System.out.println() <br>
	 * 					2) 	In the event we want to change what logging means (for example if we decide
	 * 			   			to log things to a file instead of the console), it's a lot easier to change
	 * 			  			this method alone than it is to change code in every single place we logged
	 * 			   			something to the console
	 * 
	 * @param message - the message to be logged
	 */
	public static void log(String message) {
		System.out.println(message);
	}
	
	/**
	 * Log the given message to the console, as well as to standard error.
	 * 
	 * @param message - the message to log
	 */
	public static void error(String message) {
		System.out.println(message);
		System.err.println(message);
	}
	
	/**
	 * Log that the given command has been received from the given client, by writing it to the end
	 * of the file "commands" located in the same directory as the server's executable.
	 * 
	 * This is used to ensure that we have a list of every command ever received by the server.
	 * 
	 * @param address - the address of the client from whom the given command was received
	 * @param command - the command that was received
	 */
	public static void command(String address, String command) {
		File file = new File("commands");
		try {
			FileOutputStream stream = new FileOutputStream(file, true);
			stream.write(("=====" + address + "=====\n").getBytes());
			stream.write((command + "\n").getBytes());
			stream.close();
		} catch (FileNotFoundException e) {
			Log.error("INTERNAL ERROR: Couldn't open file for logging commands");
		} catch (IOException e) {
			Log.error("INTERNAL ERROR: Couldn't log command");
		}
	}
}
