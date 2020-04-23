package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Queen extends Piece {

	public Queen(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	@Override
	public List<Pair> getMoves() {
		// Since the Queen is essentially just a Bishop and Rook at the same time,
		// prevent copying code by making use of the versions of getMoves() in
		// Rook and Bishop
		List<Pair> moves = new Rook(row, column, colour, board).getMoves();
		moves.addAll(new Bishop(row, column, colour, board).getMoves());

		return moves;
	}	

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
	@Override
	public List<Pair> getProtectedSquares() {
		List<Pair> protected_squares = new Rook(row, column, colour, board).getProtectedSquares();
		protected_squares.addAll(new Bishop(row, column, colour, board).getProtectedSquares());

		return protected_squares;
	}
	
	/**
	 * Create a String representation of this bishop
	 * 
	 * @return A String representation of this bishop
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "Q" : "q";
	}
}
