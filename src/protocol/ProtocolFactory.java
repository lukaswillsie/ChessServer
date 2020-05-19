package protocol;

import java.net.Socket;

/**
 * This class exists solely for the purpose of creating ChessProtocol (implementation)
 * objects and returning them as Protocol (interface) instances, for the purpose of maintaining
 * dependency inversion.
 * 
 * @author Lukas Willsie
 *
 */
public class ProtocolFactory {
	/**
	 * Return a Protocol instance, set up to write responses to the given Socket.
	 * 
	 * @param socket - the Socket that the created Protocol instance should write the server's
	 * responses to
	 * @return A Protocol instance, set up to receive input and write to the given socket
	 */
	public static Protocol build(Socket socket) {
		return new ChessProtocol(socket);
	}
}
