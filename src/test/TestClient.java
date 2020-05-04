package test;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {
	public static void main(String[] args) {
		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		try{
			Socket socket = new Socket(hostname, port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			DataInputStream in = new DataInputStream(socket.getInputStream());
			
			for(int i = 0; i < 5; i++) {
				System.out.println(in.readInt());
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			System.out.println(reader.readLine());
			
			for(int i = 0; i < 5; i++) {
				System.out.println(in.readInt());
			}
						
		}
		catch(IOException e) {
			e.printStackTrace();
			System.out.println("Could not connect to server " + hostname + " at " + port);
		}		
	}
	
	private static String readLine(DataInputStream in) throws IOException {
		StringBuffer buffer = new StringBuffer();
		char[] last = {'\0', '\0'};
		
		while(last[0] != '\r' || last[1] != '\n') {
			if(in.available() > 0) {
				char next = (char)in.read();
				buffer.append(next);
				last[0] = last[1];
				last[1] = next;
			}
		}
		
		buffer.delete(buffer.length() - 2, buffer.length());	
		return buffer.toString();
	}
}
