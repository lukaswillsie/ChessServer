package com.lukaswillsie.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * A rudimentary but effective way of testing the server's output. This class has a main method
 * that takes two command-line arguments: the hostname and port on which the server is operating,
 * assuming, of course, that the server is running when this program starts. This program then
 * infinitely loops, reading lines from standard input and sending them as commands to the server,
 * until the user types "q".
 * 
 * For the purposes of testing, the server currently prints its responses to standard output instead of
 * actually writing them to the socket. Therefore, to test the server, simply run it, then run this
 * program and observe the console window that the SERVER is running in to verify the results. 
 * 
 * @author Lukas Willsie
 *
 */
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
