package utility;

public class Log {
	public static void log(String message) {
		System.out.println(message);
	}
	public static void error(String message) {
		System.out.println(message);
		System.err.println(message);
	}
}
