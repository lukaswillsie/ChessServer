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
		if(board.validSquare(this.row+direction, this.column-1) &&
		  !board.isEmpty(this.row+direction, this.column-1) &&
		   board.getPiece(this.row+direction, this.column-1).getColour() != colour) {
			moves.add(new Pair(this.row+direction, this.column-1));
		}
		
		// Check the square directly in front of the pawn
		if(board.isEmpty(this.row+direction, this.column)) {
			moves.add(new Pair(this.row+direction, this.column));
		}
		
		// Check the square in front and to the right (from white's perspective)
		// of the pawn to see if it has an enenmy on it
		if(board.validSquare(this.row+direction, this.column+1) &&
		  !board.isEmpty(this.row+direction, this.column+1) &&
		   board.getPiece(this.row+direction, this.column+1).getColour() != colour) {
			moves.add(new Pair(this.row+direction, this.column+1));
		}
		
		
		return moves;
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
