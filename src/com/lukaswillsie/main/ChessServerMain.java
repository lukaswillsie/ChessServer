package com.lukaswillsie.main;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import com.lukaswillsie.data.GameDataManager;
import com.lukaswillsie.utility.Log;

/**
 * This class contains the main method that starts the whole server.
 * It's entire job is to wait for connections from clients before creating
 * ServerThread objects to handle them.
 * 
 * @author Lukas Willsie
 */
public class ChessServerMain {
	/**
	 * Main method for the whole server. This program should be passed a single command-line
	 * argument: the port number that the server is to run on.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("Usage: This program takes only one command-line argument, a port number");
			System.exit(1);
		}
		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		}
		catch(NumberFormatException e) {
			System.out.println("Usage: This program takes one command-line argument, a port number");
			System.exit(1);
		}
		
		Log.log("Server started...");
		GameDataManager manager = new GameDataManager();
		manager.build();
		manager.display();
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
