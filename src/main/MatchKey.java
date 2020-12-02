package main;

import java.io.Serializable;

/**
 * Key for 2-diimensional hashmap, i.e. [x,y] -> value
 * The key is simmetric, i.e. [x,y] == [y,x]
 */
public class MatchKey implements Serializable {

	private final String x;
	private final String y;


	public MatchKey(String x, String y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MatchKey)) return false;
		MatchKey key = (MatchKey) o;
		return x.equals(key.x) && y.equals(key.y) || x.equals(key.y) && y.equals(key.x);
	}

	@Override
	public int hashCode() {
		int resultx = x.hashCode();
		int resulty = y.hashCode();
		if (resultx >= resulty) {
			return 31 * resultx + resulty;
		} else {
			return 31 * resulty + resultx;
		}
	}

	public String toString() {
		return x + " , " + y;
	}

	public String getX() {
		return x;
	}

	public String getY() {
		return y;
	}
}



