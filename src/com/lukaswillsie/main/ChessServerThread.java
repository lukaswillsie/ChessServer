package com.lukaswillsie.main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;

import com.lukaswillsie.protocol.Protocol;
import com.lukaswillsie.protocol.ProtocolFactory;
import com.lukaswillsie.utility.Log;

/**
 * Every time a client connects to the server, an object of this class should be
 * created, and a new thread started via the run() method. The object will then endlessly loop
 * on input from the client, passing input off to a Protocol object to be processed,
 * until the client disconnects.
 * 
 * @author Lukas Willsie
 */
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
				Log.command(socket.getInetAddress().toString(), input);
				protocol.processCommand(input);
			}
		}
		catch(SocketException e) {
			Log.log("Client " + socket.getInetAddress() + " disconnected.");
			return;
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		Log.log("Client " + socket.getInetAddress() + " disconnected.");
	}
}
