package game;

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
		board.removePiece(this);
		
		Rook rook = new Rook(row, column, colour, board);
		board.addPiece(rook);
		List<Pair> moves = rook.getMoves();
		board.removePiece(rook);
		
		Bishop bishop = new Bishop(row, column, colour, board);
		board.addPiece(bishop);
		moves.addAll(bishop.getMoves());
		board.removePiece(bishop);

		board.addPiece(this);
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
		// Since the Queen is basically a Rook and Bishop simultaneously,
		// farm out the work to a pretend Bishop and Rook occupying the
		// same spot as the Queen
		List<Pair> protected_squares = new Rook(row, column, colour, board).getProtectedSquares();
		protected_squares.addAll(new Bishop(row, column, colour, board).getProtectedSquares());

		return protected_squares;
	}
	
	/**
	 * Create a String representation of this Queen.
	 * 
	 * Guaranteed to only be one character long
	 * 
	 * @return A String representation of this Queen
	 */
	public String toString () {
		return (colour == Colour.WHITE) ? "Q" : "q";
	}
}
