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
		int direction = (this.colour == Colour.WHITE) ? 1 : -1;
		
		Piece pinner = board.isPinned(this);
		if(pinner == null) {	
			List<Pair> moves = new ArrayList<Pair>();
			// Check the square in front and to the left (from white's perspective)
			// of the pawn to see if it has an enemy on it, or is an en passant square
			// for this pawn
			if((board.isPiece(this.row+direction, this.column-1) &&
			    board.getPiece(this.row+direction, this.column-1).getColour() != colour) ||
			    board.isEnPassant(new Pair(this.row+direction,  this.column-1), this)) {
				moves.add(new Pair(this.row+direction, this.column-1));
			}
			
			// Check the square directly in front of the pawn
			if(board.isEmpty(this.row+direction, this.column)) {
				moves.add(new Pair(this.row+direction, this.column));
			}
			
			if(this.canDoubleMove()) {
				// Check the square two squares in front of the pawn
				if(board.isEmpty(this.row+(2*direction), this.column)) {
					moves.add(new Pair(this.row+(2*direction), this.column));
				}
			}
			
			// Check the square in front and to the right (from white's perspective)
			// of the pawn to see if it has an enemy on it, or is an en passant square
			// for this pawn
			if((board.isPiece(this.row+direction, this.column+1) &&
			    board.getPiece(this.row+direction, this.column+1).getColour() != colour) ||
				board.isEnPassant(new Pair(this.row+direction, this.column+1), this)) {
				moves.add(new Pair(this.row+direction, this.column+1));
			}
			
			
			return board.getLegal(moves, colour);
		}
		// If the pawn is getting pinned by a Queen or Rook in its column
		else if(pinner.getColumn() == this.getColumn()) {
			// The pawn can't take diagonally in this scenario, and so can only move forward,
			// and then only if the square in front of it is empty
			if(board.isEmpty(row+direction, column)) {
				List<Pair> moves = new ArrayList<Pair>();
				moves.add(new Pair(row+direction, column));
				
				if(this.canDoubleMove()) {
					// Check the square two squares in front of the pawn
					if(board.isEmpty(this.row+(2*direction), this.column)) {
						moves.add(new Pair(this.row+(2*direction), this.column));
					}
				}
				
				return board.getLegal(moves, colour);
			}
			// Otherwise, the square in front of it is occupied by someone, so the pawn
			// can't move anywhere. Then return moves, empty
			else {
				return new ArrayList<Pair>();
			}
		}
		// If the pawn is getting pinned by a Queen or Rook in its row, it can't move at
		// all, since moving forward would expose it's King
		else if(pinner.getRow() == this.getRow()) {
			return new ArrayList<Pair>();
		}
		// Otherwise, it's being pinned by a Bishop or Queen diagonal to it.
		// Then, it has only one option: to move diagonally, toward the pinning piece,
		// which is only possible if
		// 1) The pinning piece is immediately diagonal to this piece and hence
		//    capturable
		//    or
		// 2) There is an en passant square between the
		//	  pawn and whoever is pinning it (this is an incredibly rare scenario, but
		//	  still technically a possibility)
		else {
			List<Pair> moves = new ArrayList<Pair>();
			
			// Determine whether the pinner is in front of or behind the pawn
			int rowDirection = sign(pinner.getRow() - this.getRow());
			
			// Determine whether the pinner is to the left or to the right of the pawn
			int columnDirection = sign(pinner.getColumn() - this.getColumn());
			
			// If the pinner is in "front" of the pawn with the respect to which direction
			// the pawn is moving in
			if(rowDirection == direction) {
				// We can only move diagonally toward the pinner if the square is an en passant square
				// for this pawn, or it is occupied by the pinner
				if(board.isEnPassant(new Pair(this.row+direction, this.column+columnDirection), this)
				||
				(board.isPiece(this.row+direction, this.column+columnDirection)
				 && board.getPiece(this.row+direction, this.column+columnDirection) == pinner)) {
					moves.add(new Pair(this.row+direction, this.column+columnDirection));
				}
			}
			
			return moves;
		}
	}
	
	/**
	 * Return the sign of the given integer, or 0 if it is 0
	 * @param num - The integer whose sign should be computed
	 * @return -1 if num < 0, 1 if num > 0 and 0 if num == 0
	 */
	private int sign(int num) {
		if(num > 0) {
			return 1;
		}
		else if (num == 0) {
			return 0;
		}
		else {
			return -1;
		}
	}
	
	/**
	 * Determine whether or not this pawn can move forward 2 squares this turn
	 * 
	 * @return true if and only if this pawn has not moved yet, and so is able to
	 * move two squares forward if it wants
	 */
	private boolean canDoubleMove() {
		// Pawns only move forward, so this is the pawn's first move if it
		// is still in its starting row, either row 2 or 5, depending on colour
		return (this.getColour() == Colour.WHITE && this.getRow() == 1)
				||
			   (this.getColour() == Colour.BLACK && this.getRow() == 6);
				
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

	@Override
	public boolean isCheckingKing() {
		Piece enemyKing = board.getKing((colour == Colour.WHITE) ? Colour.BLACK : Colour.WHITE);
		if(enemyKing == null) {
			return false;
		}
		
		// Stores the direction of the pawn's movement (varies from white to 
		// black pawns). White pawns move up the board and black pawns move down
		int direction = (this.colour == Colour.WHITE) ? 1 : -1;
		
		// Check the square in front and to the left (from white's perspective)
		// of the pawn to see if it's the enemy king
		if(board.validSquare(this.row+direction, this.column-1)
		&& board.getPiece(this.row+direction, this.column-1) == enemyKing) {
			return true;
		}
		
		// Check the square in front and to the right (from white's perspective)
		// of the pawn to see if it's the enemy king
		if(board.validSquare(this.row+direction, this.column+1)
		&& board.getPiece(this.row+direction, this.column+1) == enemyKing) {
			return true;
		}
		
		
		return false;
	}
}
