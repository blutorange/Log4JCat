package de.homelab.madgaksha.log4jcat;

import java.io.IOException;

/**
 * Allows the log file trimmer to be used with string.
 * @param string
 * @author madgaksha
 */
class RandomAccessString extends ARandomAccessInputStream {
	private final String string;
	private final int length;
	private int currentPos;

	/**
	 * Constructs a new reader from the given string.
	 * Both a \n and \r\n is recognized as a line break.
	 * @param string The string to be used.
	 */
	public RandomAccessString(final String string) {
		this.string = string;
		this.length = string.length();
		this.currentPos = 0;
	}

	@Override
	public void seek(final long pos) throws IOException {
		this.currentPos = pos < length ? (int)pos : length;
	}

	@Override
	public long tell() throws IOException {
		return currentPos;
	}

	@Override
	public String readLine() throws IOException {
		if (isEof()) return null;
		final StringBuilder sb = new StringBuilder();
		char c;
		boolean r = false;
		loop: while (currentPos < length) {
			c = string.charAt(currentPos++);
			switch (c) {
			case '\n':
				if (r)
					sb.setLength(sb.length() - 1);
				break loop;
			case '\r':
				r = true;
			default:
				r = false;
				sb.append(c);
			}
		}
		return sb.toString();
	}

	@Override
	public long length() {
		return length;
	}

	@Override
	public void close() {
		// String does not need to be closed.
	}
}