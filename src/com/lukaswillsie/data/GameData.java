package com.lukaswillsie.data;

/**
 * This enum represents the different data values that are stored for games
 * that are actively being played. The enum values are used as keys in HashMaps
 * that represent existing games. Each game is stored as a row in a .csv file,
 * so each enum represents a column.
 * @author Lukas Willsie
 *
 */
public enum GameData {
	GAMEID("", 0, 's'),				// ID of the game
	WHITE("", 1, 's'),				// Name of the user playing white
	BLACK("", 2, 's'),				// Name of the user playing black
	OPEN("0", 3, 'i'),				// 0 if the game is "open", meaning that all players
									// can view and join it if they want; 1 otherwise
	STATE("0", 4, 'i'),				// State of the game; 0 if white's turn, 1 if black's turn
	TURN("1", 5, 'i'),				// Current turn number
	WHITE_ARCHIVED("0", 6, 'i'),	// 1 if the white user has archived this game, 0 otherwise
	BLACK_ARCHIVED("0", 7, 'i'),	// 1 if the black user has archived this game, 0 otherwise
	DRAW_OFFERED("0", 8, 'i'),		// 1 if a draw has been offered, and the player whose turn it is
									// needs to respond
	DRAWN("0", 9, 'i'),				// 1 if the players in this game have agreed to a draw, 0 otherwise
	WINNER("", 10, 's'),			// Contains the name of the winner, or the empty String if there isn't a winner
	FORFEIT("0", 11, 'i'),			// If somebody won the game (WINNER != ""), indicates whether or not that victory
									// was by forfeit or checkmate. 1 if the victory was by forfeit, 0 if the victory was
									// by checkmate. If neither player has won, means nothing.
	WHITE_CHK("0", 12, 'i'),		// 1 if white is in check, 0 if not
	BLACK_CHK("0", 13, 'i'),		// 1 if black is in check, 0 if not
	PROMOTION_NEEDED("0", 14, 'i');	// 1 if the player whose turn it is needs to promote a piece before their turn can end
	
	
	// This array contains all the GameData values, in the order in which they should be sent to clients,
	// and the order in which they should appear in the active_games.csv file. This is the exact same as
	// the order in which they are declared in the enum, but we choose to make it explicit here.
	public static final GameData[] order = {GAMEID, WHITE, BLACK, OPEN, STATE, TURN, WHITE_ARCHIVED, BLACK_ARCHIVED, DRAW_OFFERED, DRAWN, WINNER, FORFEIT, WHITE_CHK, BLACK_CHK, PROMOTION_NEEDED};
	
	// The initial value that this piece of GameData should take upon creation of a new game
	private String initial;
	
	// The column that this piece of data occupies in the .csv file, starting from 0
	// This number is assigned in order of appearance in the list of enum values
	private int column;
	
	// One of 'i' or 's'. Represents whether the type of this data should be processed as a 
	// String or integer
	public final char type;
	
	/**
	 * Create a GameData instance according to the given information
	 * 
	 * @param initial - The initial value that this piece of data should take, upon creation of a totally new game
	 * @param column - The column of the active_games.csv file that this piece of data occupies
	 * @param type - The type of data this GameData instance is. In particular, either 'i' - integer, or 's' - String. This field
	 * is used when processing data.
	 */
	GameData(String initial, int column, char type) {
		this.initial = initial;
		this.column = column;
		this.type = type;
	}
	
	/**
	 * Return the initial value of this instance
	 * 
	 * @return - the initial value of this instance
	 */
	String getInitial() {
		return this.initial;
	}
	
	/**
	 * Return this instance's column
	 * @return - this instance's column
	 */
	int getColumn() {
		return this.column;
	}
}
