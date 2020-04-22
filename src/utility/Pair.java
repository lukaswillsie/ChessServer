package utility;

public class Pair<T,V> {
	private T firstElement;
	private V secondElement;
	
	public Pair(T first, V second) {
		this.firstElement = first;
		this.secondElement = second;
	}
	
	public void setFirst(T firstElem) {
		this.firstElement = firstElem;
	}
	
	public T first() {
		return this.firstElement;
	}
	
	public void setSecond(V secondElem) {
		this.secondElement = secondElem;
	}
	
	public V second() {
		return this.secondElement;
	}
}
