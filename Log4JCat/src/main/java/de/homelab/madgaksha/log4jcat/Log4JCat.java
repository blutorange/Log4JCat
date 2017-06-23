/** Zum Trimmen von Logdateien auf einen bestimmten Zeitraum.
 * Nutzt dazu RandomAccessFile und eine binäre Suche nach dem ersten Logeintrag.
 *
 * @author awa
 */
package de.homelab.madgaksha.log4jcat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Date;

import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class Log4JCat {
	private static long DEFAULT_THRESHOLD = 10000L; // 10 KB

	private final long threshold;
	private final String patternLayout;

	/**
	 * Constructs a new log file trimmer with the default
	 * threshold of 10000 bytes.
	 * @param patternLayout
	 */
	public Log4JCat(@NonNull final String patternLayout) {
		this(patternLayout, DEFAULT_THRESHOLD);
	}

	/**
	 * Constructs a new log file trimmer with the given
	 * log file format and threshold.
	 * @param patternLayout The pattern layout, ie. the log file format.
	 * See <a href="https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">the docs</a>
	 * for details.
	 * @param threshold In bytes. When the remaining range to search
	 * becomes smaller than this threshold, we switch to a linear scanning
	 * algorithm.
	 */
	public Log4JCat(@NonNull final String patternLayout, final long threshold) {
		this.patternLayout = patternLayout;
		this.threshold = threshold;
	}

	/**
	 * Performs a binary search for the first position.
	 *
	 * @param file
	 *            File to trim.
	 * @param date Target starting date.
	 * @return Starting position in the file. Uses the current date when <code>null</code>.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public long tail(@NonNull final IRandomAccessInputStream input, @Nullable final Date date)
			throws FileNotFoundException, IOException {
		return tail(input, date != null ? date.getTime() : new Date().getTime());
	}

	/**
	 * Performs a binary search for the first position.
	 *
	 * @param file
	 *            File to trim.
	 * @param target
	 *            Target time stamp.
	 * @return Starting position in the file.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public long tail(@NonNull final IRandomAccessInputStream input, final long target)
			throws FileNotFoundException, IOException {
		final LoggingEvent event;
		final Log4JReader log4JReader = createLog4JReader();
		long pos1, pos2, posCur, size;
		size = input.length();
		pos1 = 0;
		pos2 = size - 1;
		event = log4JReader.processSingle(input);
		input.seek(0);
		if (event == null || event.getTimeStamp() >= target)
			return 0;
		do {
			if (pos2 - pos1 < threshold) {
				// Narrowed it down enough, scan the rest sequentially.
				return scanForStart(log4JReader, input, pos1, pos2, target);
			}
			// Binary search.
			posCur = pos1 + (pos2 - pos1) / 2;
			input.seek(posCur);
			seekToStartOfLine(input);
			log4JReader.seekToNextEvent(input);
			posCur = input.tell();
			if (pos1 == posCur || pos2 == posCur) {
				return scanForStart(log4JReader, input, pos1, pos2, target);
			}
			switch (isStartPosition(log4JReader, input, target)) {
			case -1: // need to go further to the beginning of the file
				pos2 = posCur;
				break;
			case 0:
				return input.tell();
			case 1: // need to go further to the end of the file
				pos1 = posCur;
				break;
			default:
				// Does not contain any matching events.
				return size - 1;
			}
		} while (true);
	}

	private Log4JReader createLog4JReader() {
		return new Log4JReader(patternLayout);
	}

	private long scanForStart(final Log4JReader log4JReader, final IRandomAccessInputStream input, final long pos1,
			final long pos2, final long target) throws IOException {
		LoggingEvent event = null;
		long pos;
		input.seek(pos1);
		do {
			pos = input.tell();
			event = log4JReader.processSingle(input);
		}
		while (event.getTimeStamp() < target && input.tell() < pos2);
		return input.tell() >= pos2 ? pos2 : pos;
	}

	private void seekToStartOfLine(final IRandomAccessInputStream input) throws IOException {
		if (input.tell() > 0) {
			input.seek(input.tell() - 1);
			input.readLine();
		}
	}

	/**
	 * Seeks the stream to the nearest position starting a valid UTF-8 code
	 * point, ie. a byte with the highest bit 0.
	 *
	 * @param raf Stream to seek.
	 * @throws IOException
	 */
	private void seekToStartOfUtf8(final RandomAccessFile raf) throws IOException {
		byte b;
		long pos;
		do {
			pos = raf.getFilePointer();
			b = raf.readByte();
		}
		while ((b & 0b10000000) != 0);
		raf.seek(pos);
	}

	private int isStartPosition(final Log4JReader log4JReader, final IRandomAccessInputStream input, final long target)
			throws IOException {
		final LoggingEvent event, event2;
		long pos;
		event = log4JReader.processSingle(input);
		pos = input.tell();
		event2 = log4JReader.processSingle(input);
		if (event != null && event2 != null) {
			if (event.getTimeStamp() <= target && event2.getTimeStamp() > target) {
				input.seek(pos);
				return 0;
			}
			return event.getTimeStamp() < target ? 1 : -1;
		}
		else if (event == null) {
			return -2;
		}
		else {
			return event.getTimeStamp() < target ? 1 : -1;
		}
	}
}
