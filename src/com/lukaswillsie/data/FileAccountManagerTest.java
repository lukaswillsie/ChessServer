package com.lukaswillsie.data;

import java.util.Scanner;

public class FileAccountManagerTest {
	public static void main(String[] args) {
		FileAccountManager manager = new FileAccountManager();
		Scanner scanner = new Scanner(System.in);
		System.out.println(manager.usernameExists("Peter"));
		scanner.nextLine();
		System.out.println(manager.addAccount("Peter", "lukasdabest"));
		scanner.nextLine();
		System.out.println(manager.validCredentials("Peter", "vaskardabest"));
		scanner.nextLine();
		System.out.println(manager.validCredentials("Peter", "lukasdabest"));
		scanner.nextLine();
		scanner.close();
	}
}
