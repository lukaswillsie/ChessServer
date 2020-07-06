package com.lukaswillsie.data;

/**
 * A simple class containing data about a user's account at runtime
 */
public class User implements Comparable<User> {
	private String username;
	private String password;
	
	public User(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

	/**
	 * Order User objects according to their usernames
	 * 
	 * @param o - the User to compare this user to
	 */
	@Override
	public int compareTo(User o) {
		return username.compareTo(o.getUsername());
	}
}
