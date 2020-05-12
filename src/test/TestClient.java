package test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {
	public static void main(String[] args) {
		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		try(Socket socket = new Socket(hostname, port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			Scanner scanner = new Scanner(System.in);){
			
			
			
			while(true) {
				String command = scanner.nextLine();
				if(command.equals("q")) {
					break;
				}
				else {
					out.println(command);
				}
			}			
		}
		catch(IOException e) {
			e.printStackTrace();
			System.out.println("Could not connect to server " + hostname + " at " + port);
		}		
	}
}
