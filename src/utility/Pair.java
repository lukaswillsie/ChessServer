package utility;

public class Pair implements Comparable<Pair> {
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
	
	public boolean equals(Object obj) {
		return obj instanceof Pair && (this.first() == ((Pair)obj).first() && this.second() == ((Pair)obj).second());
	}

 	/**
 	 * Impose an ordering on Pairs. One pair is greater
 	 * than another if the first has greater first element
 	 * or the two have equal first element and the first has
 	 * greater second element.
 	 * 
 	 * That is, we consult the first element first, and the 
 	 * second element second in our ordering
 	 * 
 	 * For example:
	 * (0,0), (0,1), (1,3), (1,4), (1,5), (1,6), (2,0)
	 * 
	 * Is sorted in increasing order but:
	 * 
	 * (0,0), (0,1), (1,3), (1,2), (1,5), (1,6), (2,1), (2,0)
	 * 
	 * is not.
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
