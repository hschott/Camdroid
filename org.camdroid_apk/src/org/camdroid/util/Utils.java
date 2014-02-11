package org.camdroid.util;

import java.util.List;

public class Utils {

	public static int[] toIntArray(List<Integer> integerList) {
		int[] intArray = new int[integerList.size()];
		for (int i = 0; i < integerList.size(); i++) {
			intArray[i] = integerList.get(i);
		}
		return intArray;
	}

}
