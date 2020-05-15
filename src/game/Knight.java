package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Knight extends Piece {
	// The character representation of this class. Is used to encode information when talking generally
	// about the type of a piece, when we don't care about colour. For example, when a player wants to
	// promote a pawn, they pass in a character as an argument to specify what they want their pawn to
	// promote to, and that character is checked against each piece's charRep
	public static final char charRep = 'n';
	
	/**
	 * Create a new Knight of the given colour, at the given location on the given Board.
	 * 
	 * Note: before any computations are done, the newly created Knight should be added to
	 * board via the Board.addPiece() method
	 * 
	 * @param row - the row the new Knight is on
	 * @param column - the column the new Knight is on
	 * @param colour - the colour of the new Knight
	 * @param board - the Board that this Knight has been placed on
	 */
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
			
			return board.getLegal(moves, colour);
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
				||  board.isEmpty(row+direction, column+offset)) {
					protectedSquares.add(new Pair(row+direction, column+offset));
				}
				if((board.isPiece(row+offset, column+direction)
				&&  board.getPiece(row+offset, column+direction).getColour() == colour)
				||  board.isEmpty(row+offset, column+direction)) {
					protectedSquares.add(new Pair(row+offset, column+direction));
				}
			}
		}
		
		return protectedSquares;
	}
	
	/**
	 * Compute whether or not this piece is giving check to the enemy King. Note that this is
	 * a little subtle. The piece does not have to actually be able to capture the enemy King
	 * to be attacking it. As an example, a pinned piece that can't actually move at all can still
	 * be giving check. To demonstrate:
	 * 
	 * kXX
	 * XrX
	 * XXB
	 * XKX
	 * 
	 * Here, the black Rook is pinned by the white Bishop, and can't move at all. That is, its
	 * getMoves() would return an empty list. But white is still in check because the black
	 * Rook and white King are in the same column. 
	 * @return true if and only if this piece is giving check to the enemy king
	 */
	@Override
	public boolean isCheckingKing() {		
		// Use arrays to generalize the process of checking each square
		// the knight can move to
		int[] directions = {1,-1};
		int[] offsets =    {2,-2};
		
		Piece enemyKing = board.getKing((colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE);
		if(enemyKing == null) {
			return false;
		}
		
		for(int offset : offsets) {
			for(int direction : directions) {
				// Check each square we can move to for the enemy King
				if(board.validSquare(row+direction, column+offset)
				&& board.getPiece(row+direction, column+offset) == enemyKing) {
					return true;
				}
				if(board.validSquare(row+offset, column+direction)
				&& board.getPiece(row+offset, column+direction) == enemyKing) {
					return true;
				}
			}
		}
		
		return false;
	}
}
