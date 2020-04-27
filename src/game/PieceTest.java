package game;

import java.util.ArrayList;
import java.util.List;

import utility.Pair;

public class PieceTest {
	public static void main(String[] args) {
		Board board = new Board();
		int i = 0;
		int j = 0;
		
		board.addPiece(new King(i,j,Colour.WHITE,board));
		Queen queen = new Queen(0,1,Colour.WHITE,board);
		board.addPiece(queen);
		Bishop bishop = new Bishop(2,2,Colour.BLACK,board);
		board.addPiece(bishop);
		Rook blackRook = new Rook(0,7,Colour.BLACK,board);
		board.addPiece(blackRook);
		System.out.println(board);
		System.out.println(blackRook.getProtectedSquares());
		System.out.println("Pinned: " + board.isPinned(queen));
		System.out.println("Check: " + board.isCheck(Colour.BLACK));
		
		List<Pair> moves = board.getPiece(i,j).getMoves();
		
		for(Pair pair : moves) {
			board.setPiece(new Rook(pair.first(),pair.second(),Colour.WHITE,board));
		}
		
		System.out.println(board);
	}
}
