package protocol;

import java.net.Socket;

public class ProtocolFactory {
	public static Protocol build(Socket socket) {
		return new ChessProtocol(socket);
	}
}
