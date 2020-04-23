package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class Pawn extends Piece {
	public Pawn(int row, int column, Colour colour, Board board) {
		super(row, column, colour, board);
	}

	@Override
	public List<Pair> getMoves() {
		// Stores the direction of the pawn's movement (varies from white to 
		// black pawns). White pawns move up the board and black pawns move down
		int direction = this.colour == Colour.WHITE ? 1 : -1;
		
		List<Pair> moves = new ArrayList<Pair>();
		
		// Check the square in front and to the left (from white's perspective)
		// of the pawn
		if(board.isCapturable(this.row+direction, this.column-1, colour)) {
			moves.add(new Pair(this.row+direction, this.column-1));
		}
		
		// Check the square directly in front of the pawn
		if(board.isMovable(this.row+direction, this.column)) {
			moves.add(new Pair(this.row+direction, this.column));
		}
		
		// Check the square in front and to the right (from white's perspective)
		// of the pawn
		if(board.isCapturable(this.row+direction, this.column+1, colour)) {
			moves.add(new Pair(this.row+direction, this.column+1));
		}
		
		
		return moves;
	}
	
}
