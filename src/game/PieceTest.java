package game;

import java.util.List;

import utility.Pair;

public class PieceTest {
	public static void main(String[] args) {
		Board board = new Board();
		int i = 0;
		int j = 0;
		
		board.board[i][j] = new Rook(i,j,Colour.WHITE,board);
		board.board[5][0] = new Pawn(5,0,Colour.WHITE,board);
		board.board[0][2] = new Pawn(0,2,Colour.WHITE,board);
		System.out.println(board);
		
		System.out.println(board.board[i][j].getMoves().toString());
		List<Pair> moves = board.board[i][j].getMoves();
		
		for(Pair pair : moves) {
			board.board[pair.first()][pair.second()] = new Rook(0,0,Colour.WHITE,board);
		}
		
		System.out.println(board);
	}
}
