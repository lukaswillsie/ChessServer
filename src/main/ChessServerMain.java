package main;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import utility.Log;

/**
 * This class contains the main method that starts the whole server.
 * It's entire job is to wait for connections from clients before creating
 * ServerThread objects to handle them.
 * 
 * @author Lukas Willsie
 */
public class ChessServerMain {
	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		Log.log("Server started...");
		run(port);
	}
	
	/**
	 * Endlessly loop, creating threads to handle clients as necessary,
	 * listening on port
	 * 
	 * @param port - The port to listen on
	 */
	private static void run(int port) {
		try(ServerSocket serverSocket = new ServerSocket(port)){
			while(true) {
				Socket client = serverSocket.accept();
				Log.log("Accepted client " + client.getInetAddress());
				new ChessServerThread(client).start();
			}
		}
		catch(IOException e){
			System.out.println("Could not listen on port " + port);
			e.printStackTrace();
		}
	}
}
