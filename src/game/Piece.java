package game;

import java.util.List;

/**
 * Defines the abstraction of a chess piece: an entity that occupies a particular
 * square on a chessboard, has a colour (black or white), and has the ability to
 * move around the board and exert influence through its presence. 
 * 
 * @author Lukas Willsie
 *
 */
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
	
	/**
	 * Create a new Piece of the given colour, at the given location on the given Board.
	 * 
	 * Note: before any computations are done, the newly created Piece should be added to
	 * board via the Board.addPiece() method
	 * 
	 * @param row - the row the new Piece is on
	 * @param column - the column the new Piece is on
	 * @param colour - the colour of the new Piece
	 * @param board - the Board that this Piece has been placed on
	 */
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
	 * king can't move to, lest he put himself in check, but that he
	 * otherwise might be able to move to.
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
	public abstract boolean isCheckingKing();
	
	/**
	 * Inform this piece that its location on board has changed
	 * @param row - the piece's new row
	 * @param column - the piece's new column
	 */
	public void move(int row, int column) {
		this.row = row;
		this.column = column;
	}
	
	/**
	 * Get this piece's row (y-coordinate)
	 * @return This piece's row (y-coordinate)
	 */
	public int getRow() {
		return this.row;
	}
	
	/**
	 * Get this piece's column (x-coordinate)
	 * 
	 * @return This piece's column (x-coordinate)
	 */
	public int getColumn() {
		return this.column;
	}
	
	/**
	 * Get this piece's colour
	 * @return This piece's colour
	 */
	public Colour getColour() {
		return this.colour;
	}
}
