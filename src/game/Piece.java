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
	 * square is a square that this piece is preventing the enemy king
	 * from moving to. In other words, it's a square that the enemy
	 * king can't move to, lest he put himself in check.
	 * 
	 * For example, this might be a square the piece can move to,
	 * or a square occupied by an allied piece who this piece is protecting,
	 * or it might be a square diagonal to a pawn (the pawn can't move there,
	 * but it's neither can the enemy king, thanks to the pawn).
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
