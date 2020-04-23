package game;

public class PieceTest {
	public static void main(String[] args) {
		Board board = new Board();
		board.board[0][0] = new Pawn(0,0,Colour.WHITE,board);
		board.board[1][0] = new Pawn(1,0,Colour.BLACK,board);
		board.board[1][1] = new Pawn(1,1,Colour.BLACK,board);
		
		System.out.println(board.board[0][0].getMoves().toString());
	}
}
