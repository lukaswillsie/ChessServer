package utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Log {
	public static void log(String message) {
		System.out.println(message);
	}
	public static void error(String message) {
		System.out.println(message);
		System.err.println(message);
	}
	public static void command(String address, String command) {
		File file = new File("commands");
		try {
			FileOutputStream stream = new FileOutputStream(file, true);
			stream.write(("=====" + address + "=====\n").getBytes());
			stream.write((command + "\n").getBytes());
		} catch (FileNotFoundException e) {
			Log.error("INTERNAL ERROR: Couldn't open file for logging commands");
		} catch (IOException e) {
			Log.error("INTERNAL ERROR: Couldn't log command");
		}
	}
}
