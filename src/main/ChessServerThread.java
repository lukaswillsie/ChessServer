package main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import protocol.Protocol;
import protocol.ProtocolFactory;
import utility.Log;

public class ChessServerThread extends Thread {
	private Protocol protocol;
	private Socket socket;
	
	/**
	 * Create a ChessServerThread
	 * @param socket - The socket that this ChessServerThread should read from
	 */
	public ChessServerThread(Socket socket) {
		this.protocol = ProtocolFactory.build(socket);
		this.socket = socket;
	}
	
	/**
	 * Read input from socket until the underlying client disconnects.
	 * Pass input off to protocol for processing.
	 */
	@Override
	public void run() {
		String input;
		try(
		BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
		) 
		{
			while((input = in.readLine()) != null) {
				Log.log("Received command \"" + input + "\" from " + socket.getInetAddress());
				protocol.processCommand(input);
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
}
