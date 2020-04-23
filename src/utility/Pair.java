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
