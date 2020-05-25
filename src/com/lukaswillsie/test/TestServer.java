package com.lukaswillsie.test;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import com.lukaswillsie.utility.Log;

public class TestServer {
	public static void main(String[] args) {
		int port = Integer.parseInt(args[0]);
		Log.log("Server started...");
			
		try(ServerSocket serverSocket = new ServerSocket(port)){
				Socket client = serverSocket.accept();
				try {
					Log.log("Accepted client " + client.getInetAddress());
					Log.log("Writing data 1 2 3 4 5" );
					DataOutputStream out = new DataOutputStream(client.getOutputStream());
					out.writeInt(1);
					out.writeInt(2);
					out.writeInt(3);
					out.writeInt(4);
					out.writeInt(5);

					for(int i = 0; i < "What's up?\r\n".length(); i++) {
						out.write("What's up?\r\n".charAt(i));
					}
					
					out.writeInt(6);
					out.writeInt(7);
					out.writeInt(8);
					out.writeInt(9);
					out.writeInt(10);
					System.out.println("Done writing...");
					
				}
				catch(SocketException e) {
					Log.log("Client " + client.getInetAddress() + " disconnected");
				}
		}
		catch(IOException e){
			System.out.println("Could not listen on port " + port);
			e.printStackTrace();
		}		
	}
}
