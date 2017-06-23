package de.homelab.madgaksha.log4jcat;

import java.io.IOException;

/**
 * An empty random access input stream.
 * @param string
 * @author madgaksha
 */
class RandomAccessDummy extends ARandomAccessInputStream {
	/**
	 * Constructs a new reader from the given string.
	 * Both a \n and \r\n is recognized as a line break.
	 * @param string The string to be used.
	 */
	public RandomAccessDummy() {
	}

	@Override
	public void seek(final long pos) throws IOException {
		// Empty stream, no seeking to be done.
	}

	@Override
	public long tell() throws IOException {
		return 0;
	}

	@Override
	public String readLine() throws IOException {
		return null;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public void close() {
		// String does not need to be closed.
	}
}