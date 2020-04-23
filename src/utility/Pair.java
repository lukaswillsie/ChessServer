package utility;

public class Pair {
	private int firstElement;
	private int secondElement;
	
	public Pair(int first, int second) {
		this.firstElement = first;
		this.secondElement = second;
	}
	
	public void setFirst(int firstElem) {
		this.firstElement = firstElem;
	}
	
	public int first() {
		return this.firstElement;
	}
	
	public void setSecond(int secondElem) {
		this.secondElement = secondElem;
	}
	
	public int second() {
		return this.secondElement;
	}
	
	public String toString() {
		return "(" + this.firstElement + ", " + this.secondElement + ")";
	}
}
