package game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import utility.Pair;

public class King extends Piece {

	public King(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	/**
	 * Compute where this piece can move to on the board, accounting for
	 * the King not being able to put himself into check
	 * 
	 * @return A List of pairs (row,column), where each pair represents
	 * a square that this piece can move to, according to its rules of movement
	 */
	@Override
	public List<Pair> getMoves() {
		List<Pair> moves = new ArrayList<Pair>();
		Colour enemy_colour = (colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
		
		// We remove the King from the board, temporarily, to get a complete
		// picture of where enemy pieces can move. Without this step,
		// the King may misinterpret certain squares as legal.
		// For example, suppose the King has an enemy rook on its left.
		// Then the square to the right of the King is not technically
		// a square that the rook can move to at the moment, but the King still
		// can't move there because if he did he would be in check. We need
		// to remove the King from the board to catch such subtleties. 
		board.setPiece(this.row, this.column, null);
		
		List<Pair> enemyMoves = board.getMoves(enemy_colour);
		List<Pair> enemyProtectedSquares = board.getProtectedSquares(enemy_colour);
		
		int[] row_offsets = {-1,1};
		int[] column_offsets= {-1,0,1};
		
		// Check the rows in front of and behind the King
		for(int row_offset : row_offsets) {
			for(int column_offset : column_offsets) {
				// The King can move to a square if
				// 1) It is on the board, and does not hold an ally
				// 2) It is not being attacked by the enemy
				// 3) It is not being protected by the enemy
				if(board.isMovable(this.row+row_offset, this.column+column_offset, this.colour)
				&& Collections.binarySearch(enemyMoves, new Pair(this.row+row_offset, this.column+column_offset)) < 0
				&& Collections.binarySearch(enemyProtectedSquares, new Pair(this.row+row_offset, this.column+column_offset)) < 0) {
					moves.add(new Pair(this.row + row_offset, this.column + column_offset));
				}
			}
		}
		
		// Check the square in the same row and to the right of the King
		if(board.isMovable(this.row, this.column+1, this.colour)
		&& Collections.binarySearch(enemyMoves, new Pair(this.row, this.column+1)) < 0
		&& Collections.binarySearch(enemyProtectedSquares, new Pair(this.row, this.column+1)) < 0) {
			moves.add(new Pair(this.row, this.column + 1));
		}
		
		// Check the square in the same row and to the left of the King
		if(board.isMovable(this.row, this.column-1, this.colour)
		&& Collections.binarySearch(enemyMoves, new Pair(this.row, this.column-1)) < 0
		&& Collections.binarySearch(enemyProtectedSquares, new Pair(this.row, this.column-1)) < 0) {
			moves.add(new Pair(this.row, this.column - 1));
		}
		
		// We can't forget to put the king back on the board
		board.setPiece(row, column, this);
		
		return moves;
	}
	
	/**
	 * Compute all squares that this piece is PROTECTING. A protected
	 * square is a square that is currently occupied by an allied piece,
	 * but which the enemy king can't move to without placing himself
	 * in check.
	 * 
	 * This is useful for computing where a King cannot legally move
	 * without placing himself in check.
	 * 
	 * Normally, a square being protected by a given piece is a square that is
	 * occupied by an ally, but which the piece could move to, were the ally not
	 * there, or should it be captured, for example. We note that the definition
	 * at the top of this Javadoc implies something slightly different for Kings.
	 * A King does not need to actually be able to move to a square to protect it,
	 * under our first definition, because regardless the enemy King can't capture
	 * a protected square without placing himself in check by our King.
	 * 
	 * To illustrate the distinction, suppose there is a white King and a white Pawn,
	 * and the Pawn is directly beside the King. Suppose there is a black Bishop
	 * attacking this white Pawn, and the black King is also beside the Pawn. The White
	 * King couldn't move to the square occupied by his pawn if it weren't there because
	 * of the Bishop. But the pawn still occupies a protected square because the Black
	 * King can't capture the pawn without placing himself in check by the White King.
	 * 
	 * This is why we define a protected square the way we do; to make sure we are
	 * properly compiling a list of all allies that an enemy King cannot capture.
	 * 
	 * @return A list of Pairs, where each pair represents a square protected by
	 * this piece
	 */
	@Override
	public List<Pair> getProtectedSquares() {
		List<Pair> protected_squares = new ArrayList<Pair>();
		
		int[] row_offsets = {-1,1};
		int[] column_offsets= {-1,0,1};
		
		// Check the rows in front of and behind the King
		for(int row_offset : row_offsets) {
			for(int column_offset : column_offsets) {
				// The King protects a square just like any other piece;
				// if it is occupied by an ally
				if(board.isPiece(this.row+row_offset,this.column+column_offset)
				&& board.getPiece(this.row+row_offset, this.column+column_offset).getColour() == this.colour) {
					protected_squares.add(new Pair(this.row + row_offset, this.column + column_offset));
				}
			}
		}
		
		// Check the square in the same row and to the right of the King
		if(board.isPiece(this.row,this.column+1)
		&& board.getPiece(this.row, this.column+1).getColour() == this.colour) {
			protected_squares.add(new Pair(this.row, this.column + 1));
		}
		
		// Check the square in the same row and to the left of the King
		if(board.isPiece(this.row,this.column-1)
		&& board.getPiece(this.row, this.column-1).getColour() == this.colour) {
			protected_squares.add(new Pair(this.row, this.column - 1));
		}
		
		return protected_squares;
	}
	
	/**
	 * Create a String representation of this bishop
	 * 
	 * @return A String representation of this bishop
	 */
	public String toString() {
		return (colour == Colour.WHITE) ? "K" : "k";
	}
}
