/**
 * For trimming log files to a specified range.
 * Requires random access to the log file and performs
 * a binary search.
 * @author madgaksha
 */
package de.homelab.madgaksha.log4jcat;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.event.LoggingEvent;

/**
 * The log file trimmer. Instances are constructed via {@link Log4JCat}. This
 * class provides several methods for searching for log entries based on their
 * date, such as {@link #find(IRandomAccessInput, long)}.
 *
 * @author madgaksha
 * @see Log4J
 */
public final class Log4JCat {
	private final long threshold;
	private final ILogReaderFactory factory;

	Log4JCat(final ILogReaderFactory factory, final long threshold) {
		this.factory = factory;
		this.threshold = threshold;
	}

	/**
	 * For your convenience, if you ever need to access each logging event
	 * serially. Iterates over each logging event and calls the given
	 * predicate.
	 * @param input Log file input.
	 * @param predicate Consumer for each logging event. When it returns false, the iteration is ended.
	 * @throws IOException When the file could not be read.
	 */
	public void each(@NonNull final IRandomAccessInput input, @NonNull final Predicate<LoggingEvent> predicate)
			throws IOException {
		LoggingEvent event;
		final ILogReader logReader = factory.create();
		while ((event = logReader.processSingle(input)) != null) {
			if (!predicate.test(event))
				break;
		}
	}

	/**
	 * Takes a log file and a UNIX timestamp. Some log entries lie before the
	 * given date, and some lie after the given date. This method finds the
	 * first log entry that lies after or on the given date. Call this method
	 * twice to perform a head-tail trim.
	 *
	 * @param input
	 *            Log file to trim. Use the methods provided by
	 *            {@link InputFactory}.
	 * @param date
	 *            The date to search the log file for. This is a unix timestamp
	 *            (milliseconds after January 1st, 1970). Use {@link Timestamp}
	 *            if you want to use date-time objects.
	 * @return The position in the stream or file pointing to the first log
	 *         entry after (or equal to) the given date.
	 * @throws IOException
	 *             When the log file could not be read.
	 * @see InputFactory
	 * @see Timestamp
	 */
	public long find(@NonNull final IRandomAccessInput input, final long date) throws IOException {
		final LoggingEvent event;
		final ILogReader logReader = factory.create();
		long pos1, pos2, posCur, size;
		size = input.length();
		pos1 = 0;
		pos2 = size - 1;
		event = logReader.processSingle(input);
		input.seek(0);
		if (event == null || event.getTimeStamp() >= date)
			return 0;
		do {
			if (pos2 - pos1 < threshold) {
				// Narrowed it down enough, scan the rest sequentially.
				return scanForStart(logReader, input, pos1, pos2, date);
			}
			// Binary search.
			posCur = pos1 + (pos2 - pos1) / 2;
			input.seek(posCur);
			seekToStartOfLine(input);
			logReader.seekToNextEvent(input);
			posCur = input.tell();
			if (pos1 == posCur || pos2 == posCur) {
				return scanForStart(logReader, input, pos1, pos2, date);
			}
			switch (isStartPosition(logReader, input, date)) {
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
		}
		while (true);
	}

	private long scanForStart(final ILogReader logReader, final IRandomAccessInput input, final long pos1,
			final long pos2, final long target) throws IOException {
		LoggingEvent event = null;
		long pos;
		input.seek(pos1);
		do {
			pos = input.tell();
			event = logReader.processSingle(input);
		}
		while (event.getTimeStamp() < target && input.tell() < pos2);
		return event.getTimeStamp() >= target ? pos : input.isEof() ? pos2 + 1 : pos2;
	}

	private void seekToStartOfLine(final IRandomAccessInput input) throws IOException {
		if (input.tell() > 0) {
			input.seek(input.tell() - 1);
			input.readLine();
		}
	}

	/**
	 * Seeks the stream to the nearest position starting a valid UTF-8 code
	 * point, ie. a byte with the highest bit 0.
	 *
	 * @param raf
	 *            Stream to seek.
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

	private int isStartPosition(final ILogReader logReader, final IRandomAccessInput input, final long target)
			throws IOException {
		final LoggingEvent event, event2;
		long pos;
		event = logReader.processSingle(input);
		pos = input.tell();
		event2 = logReader.processSingle(input);
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
