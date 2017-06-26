package de.homelab.madgaksha.log4jcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;

/**
 * <p>
 * The log file trimmer needs random access to the log file. This factory
 * contains several convenience method for using a log file via several
 * different APIs. Currently, it support in-memory log files via {@link String}
 * and {@link CharSequence}, locale files via {@link File},
 * {@link RandomAccessFile} and {@link Path}, as well as streams via
 * {@link InputStream} and {@link Reader}.
 * </p>
 * <p>
 * Using {@link InputStream} and {@link Reader} is discouraged as they do not
 * provide random access and need to be read into memory fully. do not pr
 * </p>
 *
 * @author madgaksha
 *
 */
public final class InputFactory {
	private InputFactory() {
		// Contains only static factory methods.
	}

	/**
	 * @param charSequence
	 *            The string containing the log file. Interpreted as the empty
	 *            string when <code>null</code>.
	 * @return A random access input for log file trimming.
	 */
	public static IRandomAccessInput open(@Nullable final CharSequence charSequence) {
		if (charSequence == null)
			return new RandomAccessDummy();
		return new RandomAccessCharSequence(charSequence);
	}

	/**
	 * Uses the default charset.
	 *
	 * @param path
	 *            Path to the log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final Path path) throws UnsupportedEncodingException {
		return open(path.toFile(), getCharset(null));
	}

	/**
	 * @param path
	 *            Path to the log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @param encoding
	 *            The encoding the file uses. Uses the default encoding when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final Path path, @Nullable final String encoding)
			throws UnsupportedEncodingException {
		return open(path, getCharset(encoding));
	}

	/**
	 * @param path
	 *            Path to the log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @param charset
	 *            The charset the file uses. Uses the default charset when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final Path path, @Nullable final Charset charset)
			throws UnsupportedEncodingException {
		if (path == null)
			return new RandomAccessDummy();
		return open(path.toFile(), charset);
	}

	/**
	 * Uses the default charset.
	 *
	 * @param file
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final File file) throws UnsupportedEncodingException {
		return open(file, getCharset(null));
	}

	/**
	 * @param file
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @param encoding
	 *            The encoding of this file. Uses the default encoding when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final File file, @Nullable final String encoding)
			throws UnsupportedEncodingException {
		return open(file, getCharset(encoding));
	}

	/**
	 * @param file
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @param charset
	 *            The charset of this file. Uses the default charset when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	@SuppressWarnings("resource") // We only open the stream.
	public static IRandomAccessInput open(@Nullable final File file, @Nullable final Charset charset)
			throws UnsupportedEncodingException {
		if (file == null)
			return new RandomAccessDummy();
		final RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		}
		catch (@SuppressWarnings("unused") final FileNotFoundException ignored) {
			return new RandomAccessDummy();
		}
		return open(raf, charset);
	}

	/**
	 * Uses the default encoding.
	 *
	 * @param randomAccessFile
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final RandomAccessFile randomAccessFile)
			throws UnsupportedEncodingException {
		return open(randomAccessFile, getCharset(null));
	}

	/**
	 * @param randomAccessFile
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code>.
	 * @param encoding
	 *            The encoding of this file. Uses the default encoding when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final RandomAccessFile randomAccessFile, final String encoding)
			throws UnsupportedEncodingException {
		return open(randomAccessFile, getCharset(encoding));
	}

	/**
	 * @param randomAccessFile
	 *            The log file. Interpreted as an empty file when
	 *            <code>null</code>.
	 * @param charset
	 *            The charset of this file. Uses the default charset when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws UnsupportedEncodingException When the charset is not supported.
	 */
	public static IRandomAccessInput open(@Nullable final RandomAccessFile randomAccessFile,
			@Nullable final Charset charset) throws UnsupportedEncodingException {
		if (randomAccessFile == null)
			return new RandomAccessDummy();
		return ARandomAccessFileForwarder.of(randomAccessFile, charset != null ? charset : Charset.defaultCharset());
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to
	 * be read into memory.
	 *
	 * @param stream
	 *            Stream containing the log file. Interpreted as an empty stream
	 *            when <code>null</code>.
	 * @param encoding
	 *            Encoding to use. Uses the default encoding when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws IOException
	 *             When the stream could not be read.
	 */
	public static IRandomAccessInput open(@Nullable final InputStream stream, @Nullable final String encoding)
			throws IOException {
		return open(stream, getCharset(encoding));
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to
	 * be read into memory.
	 *
	 * @param stream
	 *            Stream containing the log file. Interpreted as an empty stream
	 *            when <code>null</code>.
	 * @param charset
	 *            Charset to use. Uses the default charset when
	 *            <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws IOException
	 *             When the stream could not be read.
	 */
	public static IRandomAccessInput open(@Nullable final InputStream stream, @Nullable final Charset charset)
			throws IOException {
		if (stream == null)
			return new RandomAccessDummy();
		final String string = IOUtils.toString(stream, charset != null ? charset : Charset.defaultCharset());
		return open(string);
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to
	 * be read into memory. Uses the default encoding.
	 *
	 * @param stream
	 *            Stream containing the log file. Interpreted as an empty stream
	 *            when <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws IOException
	 *             When the stream could not be read.
	 */
	public static IRandomAccessInput open(@Nullable final InputStream stream) throws IOException {
		return open(stream, (Charset) null);
	}

	/**
	 * Use carefully. As we need random access, the entire reader needs to be
	 * read into memory.
	 *
	 * @param reader
	 *            Reader containing the log file. Interpreted as an empty reader
	 *            when <code>null</code>.
	 * @return A random access input for log file trimming.
	 * @throws IOException
	 *             When the stream could not be read.
	 */
	public static IRandomAccessInput open(@Nullable final Reader reader) throws IOException {
		if (reader == null)
			return new RandomAccessDummy();
		final String string = IOUtils.toString(reader);
		return open(string);
	}

	private static Charset getCharset(final String encoding) {
		return encoding != null ? Charset.forName(encoding) : Charset.defaultCharset();
	}
}