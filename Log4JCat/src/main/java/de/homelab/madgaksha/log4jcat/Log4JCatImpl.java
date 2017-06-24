/**
 * For trimming log files to a specified range.
 * Requires random access to the log file and performs
 * a binary search.
 * @author madgaksha
 */
package de.homelab.madgaksha.log4jcat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class Log4JCatImpl {
	private final long threshold;
	private final String patternLayout;
	private final TimeZone timeZone;
	private final Locale locale;

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
	Log4JCatImpl(final String patternLayout, final Locale locale, final TimeZone timeZone, final long threshold) {
		this.patternLayout = patternLayout;
		this.locale = locale;
		this.timeZone = timeZone;
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
			throws IOException {
		return tail(input, date != null ? date.getTime() : new Date().getTime());
	}

	/**
	 * Performs a binary search for the first entry equal to or after the given date.
	 * @param stream
	 *            Log file to trim. Use the methods provided by {@link StreamFactory}.
	 * @param date Target starting date. Must support getting {@link ChronoField#INSTANT_SECONDS}.
	 * @return Starting position in the file. Uses the current date when <code>null</code>.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public long tail(final IRandomAccessInputStream stream, final TemporalAccessor date) throws IOException {
		final long timeStamp = timeStampFromTemporal(date);
		return tail(stream, timeStamp);
	}

	@SuppressWarnings("deprecation")
	private long timeStampFromTemporal(final TemporalAccessor date) {
		if (!date.isSupported(ChronoField.INSTANT_SECONDS)) {
			final int y = date.get(ChronoField.YEAR);
			final int m = date.get(ChronoField.MONTH_OF_YEAR);
			final int d = date.get(ChronoField.DAY_OF_MONTH);
			final int h = date.get(ChronoField.HOUR_OF_DAY);
			final int min = date.get(ChronoField.MINUTE_OF_DAY);
			final int s = date.get(ChronoField.SECOND_OF_DAY);
			return new Date(y-1900,m-1,d,h,min,s).getTime();
		}
		if (!date.isSupported(ChronoField.MILLI_OF_SECOND))
			return 1000L * date.getLong(ChronoField.INSTANT_SECONDS);
		return 1000L * date.getLong(ChronoField.INSTANT_SECONDS) + date.getLong(ChronoField.MILLI_OF_SECOND);
	}

	/**
	 * Performs a binary search for the first position.
	 *
	 * @param file
	 *            File to trim.
	 * @param date
	 *            Target time stamp.
	 * @return Starting position in the file.
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public long tail(@NonNull final IRandomAccessInputStream input, final long date)
			throws IOException {
		final LoggingEvent event;
		final Log4JReader log4JReader = createLog4JReader();
		long pos1, pos2, posCur, size;
		size = input.length();
		pos1 = 0;
		pos2 = size - 1;
		event = log4JReader.processSingle(input);
		input.seek(0);
		if (event == null || event.getTimeStamp() >= date)
			return 0;
		do {
			if (pos2 - pos1 < threshold) {
				// Narrowed it down enough, scan the rest sequentially.
				return scanForStart(log4JReader, input, pos1, pos2, date);
			}
			// Binary search.
			posCur = pos1 + (pos2 - pos1) / 2;
			input.seek(posCur);
			seekToStartOfLine(input);
			log4JReader.seekToNextEvent(input);
			posCur = input.tell();
			if (pos1 == posCur || pos2 == posCur) {
				return scanForStart(log4JReader, input, pos1, pos2, date);
			}
			switch (isStartPosition(log4JReader, input, date)) {
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
		return new Log4JReader(patternLayout, locale, timeZone);
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
		return event.getTimeStamp() >= target ? pos : input.isEof() ? pos2+1 : pos2;
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
