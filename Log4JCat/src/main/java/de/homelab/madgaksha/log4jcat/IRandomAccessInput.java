package de.homelab.madgaksha.log4jcat;

import java.io.Closeable;
import java.io.IOException;

/**
 * <p>
 * The interface for reading the data of the log file to be trimmed.
 * As the algorithm uses a binary search, it requires the input stream to
 * support random access. Implementation are responsible for handling
 * the encoding.
 * </p>
 * <p>
 * Usually you do not need to implement this yourself, you can use
 * the factory methods provided by {@link InputFactory}.
 * </p>
 * @author madgaksha
 * @see InputFactory
 */
public interface IRandomAccessInput extends Closeable {
	/**
	 * Sets the offset, measured from the beginning of this stream, at which the
	 * next read or write occurs. The offset may be set beyond the end of the
	 * stream. Setting the offset beyond the end of the stream does not change
	 * the stream length.
	 *
	 * @param pos
	 *            the offset position, measured in bytes from the beginning of
	 *            the file, at which to set the file pointer.
	 * @exception IOException
	 *                if {@code pos} is less than {@code 0} or if an I/O error
	 *                occurs.
	 */
	public void seek(long pos) throws IOException;

	/**
	 * Returns the current offset in this stream.
	 *
	 * @return The offset from the beginning of the stream, in bytes, at which
	 *         the next read or write occurs.
	 * @exception IOException
	 *                If an I/O error occurs.
	 */
	public long tell() throws IOException;

	/**
	 * Reads the next line of text from the input stream. It reads successive
	 * bytes until it encounters a line terminator or end of file; the bytes
	 * are then converted to a {@code String} with the proper encoding.
	 *
	 * @return the next line of text from the input stream, or {@code null} if
	 *         the end of file is encountered before a byte can be read.
	 * @exception IOException
	 *                If an I/O error occurs.
	 */
	public String readLine() throws IOException;

	/**
	 * Same as {@link #readLine}, but reads all the next lines of text from the input stream until the end of the stream.
	 *
	 * @return the next lines of text from the input stream, or {@code null} if
	 *         the end of file is encountered before a byte can be read.
	 * @exception IOException
	 *                If an I/O error occurs.
	 */
	public String readLines() throws IOException;

    /**
     * Returns the length of this stream.
     *
     * @return     The length of this stream, measured in bytes.
     * @exception  IOException  if an I/O error occurs.
     */
	public long length() throws IOException;

	/**
	 * @return Whether the file is at the end.
	 * @throws IOException if an I/O error occurs.
	 */
	public boolean isEof() throws IOException;
}