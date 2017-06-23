package de.homelab.madgaksha.log4jcat;

import java.io.IOException;

/**
 * The interface for reading the data of the log file to be trimmed.
 * As the algorithm uses a binary search, it must support random access.
 * Implementation are responsible for the encoding.
 * @author madgaksha
 */
abstract class ARandomAccessInputStream implements IRandomAccessInputStream {
	@Override
	public boolean isEof() throws IOException {
		return tell() >= length();
	}

	@Override
	public String readLines() throws IOException {
		if (isEof())
			return null;
		final StringBuilder sb = new StringBuilder((int)(length()-tell()));
		do {
			sb.append(readLine());
		} while (!isEof());
		return sb.toString();
	}
}