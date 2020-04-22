package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {
	public static void main(String[] args) {
		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		try(Socket socket = new Socket(hostname, port);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		    BufferedReader in = new BufferedReader(
		        new InputStreamReader(socket.getInputStream()))){
			
			Scanner scanner = new Scanner(System.in);
			
			System.out.println("Command: login Vaskar hehe");
			out.println("login Vaskar hehe");
			System.out.println(in.readLine());
			scanner.nextLine();
			
			System.out.println("Command: creategame Lukas's");
			out.println("creategame Lukas's");
			System.out.println(in.readLine());
			scanner.nextLine();
			
			System.out.println("Command: joingame Lukas's");
			out.println("joingame Lukas's");
			System.out.println(in.readLine());
			scanner.nextLine();
			
			scanner.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.out.println("Could not connect to server " + hostname + " at " + port);
		}		
	}
}
