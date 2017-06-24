package de.homelab.madgaksha.log4jcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;

public final class StreamFactory {
	private StreamFactory() {
		// Contains only static factory methods.
	}

	/**
	 * @param string The string containing the log file. Interpreted as the empty string when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 */
	public static IRandomAccessInputStream open(@Nullable final String string) {
		if (string == null)
			return new RandomAccessDummy();
		return new RandomAccessString(string);
	}

	/**
	 * @param path
	 *            Path to the log file. Interpreted as an empty file when
	 *            <code>null</code> or when the file could not be found.
	 * @return A random access input stream for log file trimming.
	 */
	public static IRandomAccessInputStream open(@Nullable final Path path)  {
		if (path == null)
			return new RandomAccessDummy();
		return open(path.toFile());
	}

	/**
	 * @param file
	 *            The log file. Interpreted as an empty file when <code>null</code>
	 *            or when the file could not be found.
	 * @return A random access input stream for log file trimming.
	 */
	@SuppressWarnings("resource") // We only open the stream.
	public static IRandomAccessInputStream open(@Nullable final File file)  {
		if (file == null)
			return new RandomAccessDummy();
		final RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(file, "r");
		}
		catch (@SuppressWarnings("unused") final FileNotFoundException ignored) {
			return new RandomAccessDummy();
		}
		return new RandomAccessFileAdapter(raf);
	}

	/**
	 * @param string The string containing the log file. Interpreted as an empty stream when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 */
	public static IRandomAccessInputStream open(@Nullable final RandomAccessFile raf) {
		if (raf == null)
			return new RandomAccessDummy();
		return new RandomAccessFileAdapter(raf);
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to be read
	 * into memory.
	 * @param stream Stream containing the log file. Interpreted as an empty stream when <code>null</code>.
	 * @param encoding Encoding to use. Uses the default encoding when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 * @throws IOException When the stream could not be read.
	 */
	public static IRandomAccessInputStream open(@Nullable final InputStream stream, @Nullable final String encoding) throws IOException {
		final Charset charset = encoding == null ? Charset.defaultCharset() : Charsets.toCharset(encoding);
		return open(stream, charset);
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to be read
	 * into memory.
	 * @param stream Stream containing the log file. Interpreted as an empty stream when <code>null</code>.
	 * @param charset Charset to use. Uses the default charset when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 * @throws IOException When the stream could not be read.
	 */
	public static IRandomAccessInputStream open(@Nullable final InputStream stream, @Nullable final Charset charset) throws IOException {
		if (stream == null)
			return new RandomAccessDummy();
		final String string = IOUtils.toString(stream, charset != null ? charset : Charset.defaultCharset());
		return open(string);
	}

	/**
	 * Use carefully. As we need random access, the entire input stream needs to be read
	 * into memory. Uses the default encoding.
	 * @param stream Stream containing the log file. Interpreted as an empty stream when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 * @throws IOException When the stream could not be read.
	 */
	public static IRandomAccessInputStream open(@Nullable final InputStream stream) throws IOException {
		return open(stream, (Charset)null);
	}

	/**
	 * Use carefully. As we need random access, the entire reader needs to be read
	 * into memory.
	 * @param reader Reader containing the log file. Interpreted as an empty reader when <code>null</code>.
	 * @return A random access input stream for log file trimming.
	 * @throws IOException When the stream could not be read.
	 */
	public static IRandomAccessInputStream open(@Nullable final Reader reader) throws IOException {
		if (reader == null)
			return new RandomAccessDummy();
		final String string = IOUtils.toString(reader);
		return open(string);
	}
}