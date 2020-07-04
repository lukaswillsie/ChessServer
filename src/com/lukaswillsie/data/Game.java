package com.lukaswillsie.data;

import java.util.HashMap;
import java.util.List;

import Chess.com.lukaswillsie.chess.Board;

public class Game {
	/**
	 * An exception thrown when we try and create a Game object from faulty data
	 * @author Lukas Willsie
	 *
	 */
	public static class InvalidGameDataException extends Exception {
		private InvalidGameDataException(String message) {
			super(message);
		}
	}
	
	/**
	 * A collection of key,value pairs containing all the information about this game,
	 * like the names of the players, whose turn it is, etc.
	 */
	private HashMap<GameData, Object> data = new HashMap<GameData, Object>();
	
	/**
	 * A Board object containing the current state of this Game
	 */
	private Board board;
	
	/**
	 * Create a new Game to hold the given data. The list passed as a parameter
	 * should contain a line of the active_games.csv file, split about the commas.
	 * If the line of input is invalid, for example if there aren't as many items in
	 * the list as there are GameData enum values, or an item which is supposed to be
	 * an integral value is not, an InvalidGameDataException will be thrown.
	 * 
	 * @param data - a line of the active_games.csv file, split about its commas
	 * @throws InvalidGameDataException 
	 */
	public Game(String[] data, Board board) throws InvalidGameDataException {
		this.board = board;
		
		if(data.length != GameData.order.length) {
			throw new InvalidGameDataException("Data has " + data.length + " items when it should have " + GameData.order.length);
		}
		
		GameData dataType;
		for(int i = 0; i < GameData.order.length; i++) {
			dataType = GameData.order[i];
			
			if(dataType.type == 'i') {
				try {
					this.data.put(dataType, Integer.parseInt(data[i]));
				}
				catch(NumberFormatException e) {
					throw new InvalidGameDataException("Couldn't convert data " + data[i] + " of type " + dataType + " to int");
				}
			}
			else if(dataType.type == 's') {
				this.data.put(dataType, data[i]);
			}
		}
	}
	
	/**
	 * Takes a GameData enum and returns this game's corresponding value.
	 * 
	 * @param dataType - specifies which of this game's statistics to return
	 * @return the statistic corresponding to the given GameData enum value in this game
	 */
	public Object getData(GameData dataType) {
		return this.data.get(dataType);
	}
	
	/**
	 * Changes the value of the statistic given by dataType to val
	 * 
	 * @param dataType - which statistic should be changed
	 * @param val - the new value of the statistic
	 */
	public void setData(GameData dataType, Object val) {
		this.data.put(dataType, val);
	}
	
	public Board getBoard() {
		return this.board;
	}
}
