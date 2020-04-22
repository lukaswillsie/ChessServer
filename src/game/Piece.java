package game;

import utility.Pair;

public abstract class Piece {
	// Represent the piece's position on the board.
	// row=0 means the first row from the bottom, when viewed from white's perspective
	// column=0 means the first column from the left, when viewed from
	// white's perspective
	// So row=0,column=0 means a1, in traditional chess notation
	private int row;
	private int column;
	
	// This piece's colour (white or black)
	private Colour colour;
	
	// The Board of which this piece is a member
	private Board board;
	
	public Piece(int row, int column, Colour colour) {
		this.row = row;
		this.column = column;
		this.colour = colour;
	}
	
	abstract Pair<Integer, Integer> getMoves();
	
	public void move(int row, int column) {
		this.row = row;
		this.column = column;
	}
	
	public int getRow() {
		return this.row;
	}
	
	public int getColumn() {
		return this.column;
	}
	
	public Colour getColour() {
		return this.colour;
	}
}
