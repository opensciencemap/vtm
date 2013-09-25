package org.oscim.utils;

public class ArrayUtils {

	public static <T> void reverse(T[] data) {
		reverse(data, 0, data.length);
	}

	public static <T> void reverse(T[] data, int left, int right) {
		right--;

		while (left < right) {
			T tmp = data[left];
			data[left] = data[right];
			data[right] = tmp;

			left++;
			right--;
		}
	}

	public static void reverse(short[] data, int left, int right, int stride) {
		right -= stride;

		while (left < right) {
			for (int i = 0; i < stride; i++) {
				short tmp = data[left + i];
				data[left + i] = data[right + i];
				data[right + i] = tmp;
			}
			left += stride;
			right -= stride;
		}
	}

	public static void reverse(byte[] data, int left, int right) {
		right -= 1;

		while (left < right) {
			byte tmp = data[left];
			data[left] = data[right];
			data[right] = tmp;

			left++;
			right--;
		}
	}
}
