package game;

/**
 * Represents a pair of integers, with a first entry and a second entry.
 * 
 * Used mainly in this program to represent squares on a chessboard, where
 * the first entry represents what row the square is on and the second entry
 * represents the column the square is on. 
 * 
 * I decided to create this class because using it is a lot cleaner than passing
 * two integers around.
 * 
 * @author Lukas Willsie
 *
 */
public class Pair implements Comparable<Pair> {
	// Store this objects first and second entries
	private int firstElement;
	private int secondElement;
	
	/**
	 * Create a new Pair with the given initial first and second entries
	 * @param first - the initial first entry of this pair
	 * @param second - the initial second entry of this pair
	 */
	public Pair(int first, int second) {
		this.firstElement = first;
		this.secondElement = second;
	}
	
	/**
	 * Set this Pair's first entry
	 * @param firstElem - this Pair's new first entry 
	 */
	public void setFirst(int firstElem) {
		this.firstElement = firstElem;
	}
	
	/**
	 * Get this Pair's first entry
	 * @return This Pair's first entry
	 */
	public int first() {
		return this.firstElement;
	}
	
	/**
	 * Set this Pair's second entry.
	 * 
	 * @param secondElem - this Pair's new second entry 
	 */
	public void setSecond(int secondElem) {
		this.secondElement = secondElem;
	}
	
	/**
	 * Get this Pair's second entry.
	 * 
	 * @return This Pair's second entry
	 */
	public int second() {
		return this.secondElement;
	}
	
	/**
	 * Return the natural String representation of a Pair: <br>
	 * <br>
	 *"(first,second)"
	 */
	public String toString() {
		return "(" + this.firstElement + ", " + this.secondElement + ")";
	}
	
	/**
	 * Compare this and another Pair for equality
	 * 
	 * @param obj - The object to compare this Pair to
	 * @return true if and only if obj is a Pair AND this Pair and obj share the same first and second entries
	 */
	public boolean equals(Object obj) {
		return obj instanceof Pair && (this.first() == ((Pair)obj).first() && this.second() == ((Pair)obj).second());
	}

 	/**
 	 * Impose an ordering on Pairs. One pair is greater
 	 * than another if the first has greater first element
 	 * or the two have equal first element and the first has
 	 * greater second element. <br>
 	 * <br>
 	 * That is, we consult the first element first, and the 
 	 * second element second in our ordering. <br>
 	 * <br>
 	 * For example: <br>
	 * (0,0), (0,1), (1,3), (1,4), (1,5), (1,6), (2,0) <br>
	 * <br>
	 * Is sorted in increasing order but: <br>
	 * <br>
	 * (0,0), (0,1), (1,3), (1,2), (1,5), (1,6), (2,1), (2,0) <br>
	 * <br>
	 * is not. <br>
	 */
	@Override
	public int compareTo(Pair o) {
		if(this.first() < o.first() || (this.first() == o.first() && this.second() < o.second())) {
			return -1;
		}
		else if (this.equals(o)) {
			return 0;
		}
		else {
			return 1;
		}
	}
}
