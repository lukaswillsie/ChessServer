package game;

import java.util.List;

import utility.Pair;

public abstract class Piece {
	// Represent the piece's position on the board.
	// row=0 means the first row from the bottom, when viewed from white's perspective
	// column=0 means the first column from the left, when viewed from
	// white's perspective
	// So row=0,column=0 means a1, in traditional chess notation
	protected int row;
	protected int column;
	
	// This piece's colour (white or black)
	protected Colour colour;
	
	// The Board of which this piece is a member
	protected Board board;
	
	public Piece(int row, int column, Colour colour, Board board) {
		this.row = row;
		this.column = column;
		this.colour = colour;
		this.board = board;
	}
	
	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this piece can move to, according to its rules of movement
	 */
	public abstract List<Pair> getMoves();
	
	/**
	 * Compute all squares that this piece is PROTECTING. A protected
	 * square is a square that is currently occupied by an allied piece,
	 * but which the enemy king can't move to without placing himself
	 * in check.
	 * 
	 * This might sound like a strange definition, but see the Javadoc
	 * for the King class's implementation of this method to understand
	 * why we phrase it this way.
	 * 
	 * This is useful for computing where a King cannot legally move
	 * without placing himself in check.
	 * 
	 * @return A list of Pairs, where each pair represents a square protected by
	 * this piece
	 */
	public abstract List<Pair> getProtectedSquares();
	
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
