package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Knight extends Piece {
	public Knight(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}
	
	/**
	 * Compute where this piece can move to on the board
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this knight can move to
	 */
	@Override
	public List<Pair> getMoves() {
		// If this piece is not pinned, simply check all squares that a knight can move to
		if(board.isPinned(this) == null) {
			List<Pair> moves = new ArrayList<Pair>();
			
			// Use arrays to generalize the process of checking each square
			// the knight can move to
			int[] directions = {1,-1};
			int[] offsets = {2,-2};
			
			for(int offset : offsets) {
				for(int direction : directions) {
					if(board.isMovable(row+direction, column+offset, colour)) {
						moves.add(new Pair(row+direction, column+offset));
					}
					if(board.isMovable(row+offset, column+direction, colour)) {
						moves.add(new Pair(row+offset, column+direction));
					}
				}
			}
			
			return moves;
		}
		// If it is pinned, it can't move at all
		else {
			return new ArrayList<Pair>();
		}
	}
	
	/**
	 * Create a String representation of this knight
	 * 
	 * @return A String representation of this knight
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "N" : "n";
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
		// We don't have to take into account whether or not the Knight is
		// pinned, here, because the enemy King still can't move to any
		// squares under the Knight's influence
		List<Pair> protectedSquares = new ArrayList<Pair>();
		
		// Use arrays to generalize the process of checking each square
		// the knight can move to
		int[] directions = {1,-1};
		int[] offsets =    {2,-2};
		
		for(int offset : offsets) {
			for(int direction : directions) {
				// Add a square to the list if it's empty or occupied by an allied piece
				if((board.isPiece(row+direction, column+offset)
				&&  board.getPiece(row+direction, column+offset).getColour() == colour)
				||  board.isMovable(row+direction, column+offset, colour)) {
					protectedSquares.add(new Pair(row+direction, column+offset));
				}
				if((board.isPiece(row+offset, column+direction)
				&&  board.getPiece(row+offset, column+direction).getColour() == colour)
				||  board.isMovable(row+offset, column+direction, colour)) {
					protectedSquares.add(new Pair(row+offset, column+direction));
				}
			}
		}
		
		return protectedSquares;
	}
}
