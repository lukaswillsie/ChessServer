package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Pawn extends Piece {
	public Pawn(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}
	
	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this pawn can move to
	 */
	@Override
	public List<Pair> getMoves() {
		// Stores the direction of the pawn's movement (varies from white to 
		// black pawns). White pawns move up the board and black pawns move down
		int direction = this.colour == Colour.WHITE ? 1 : -1;
		
		List<Pair> moves = new ArrayList<Pair>();
		
		// Check the square in front and to the left (from white's perspective)
		// of the pawn to see if it has an enemy on it
		if(board.isPiece(this.row+direction, this.column-1) &&
		   board.getPiece(this.row+direction, this.column-1).getColour() != colour) {
			moves.add(new Pair(this.row+direction, this.column-1));
		}
		
		// Check the square directly in front of the pawn
		if(board.isEmpty(this.row+direction, this.column)) {
			moves.add(new Pair(this.row+direction, this.column));
		}
		
		// Check the square in front and to the right (from white's perspective)
		// of the pawn to see if it has an enenmy on it
		if(board.isPiece(this.row+direction, this.column+1) &&
		   board.getPiece(this.row+direction, this.column+1).getColour() != colour) {
			moves.add(new Pair(this.row+direction, this.column+1));
		}
		
		
		return moves;
	}
	
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
	@Override
	public List<Pair> getProtectedSquares() {
		// Stores the direction of the pawn's movement (varies from white to 
		// black pawns). White pawns move up the board and black pawns move down
		int direction = (this.colour == Colour.WHITE) ? 1 : -1;
		
		List<Pair> protected_squares = new ArrayList<Pair>();
		
		// Check the square in front and to the left (from white's perspective)
		// of the pawn to see if it's an ally, or empty
		// In either case, it's a protected square
		if((board.isPiece(this.row+direction, this.column-1)
		 && board.getPiece(this.row+direction, this.column-1).getColour() == colour)
		 || board.isEmpty(this.row+direction, this.column-1)) {
			protected_squares.add(new Pair(this.row+direction, this.column-1));
		}
		
		// Check the square in front and to the right (from white's perspective)
		// of the pawn to see if it's an ally, or empty
		// In either case, it's a protected square
		if((board.isPiece(this.row+direction, this.column+1)
		 && board.getPiece(this.row+direction, this.column+1).getColour() == colour)
		 || board.isEmpty(this.row+direction, this.column+1)) {
			protected_squares.add(new Pair(this.row+direction, this.column+1));
		}
		
		
		return protected_squares;
	}
	
	/**
	 * Create a String representation of this pawn
	 * 
	 * @return A String representation of this pawn
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "P" : "p";
	}
}
