package de.homelab.madgaksha.log4jcat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

/**
 * A simple wrapper for working with different time and date
 * APIs. All methods return a UNIX timestamp to be used
 * with {@link Log4JCat}.
 * @author madgaksha
 */
public final class Timestamp {
	private Timestamp() {
		// Contains only static methods.
	}

	/**
	 * @param date The date to process.
	 * @return The UNIX timestamp for the given date.
	 */
	public static long of(final Date date) {
		return date.getTime();
	}

	/**
	 * @param date The date to process, eg. a {@link DateTime}.
	 * @return The UNIX timestamp for the given date.
	 */
	public static long of(final ReadableInstant date) {
		return date.getMillis();
	}

	/**
	 * @param date The date to process.
	 * @return The UNIX timestamp for the given date.
	 */
	public static long of(final Calendar date) {
		return date.getTime().getTime();
	}

	/**
	 * Please note that it should support the field
	 * {@link ChronoField#INSTANT_SECONDS}. For example, this field is supported
	 * by {@link ZonedDateTime}, but not {@link LocalDateTime} as the latter is
	 * ambigious due to the missing time zone. If
	 * {@link ChronoField#INSTANT_SECONDS} is not supported, this method falls
	 * back to the local default time zone.
	 *
	 * @param date The date to process, eg. a {@link ZonedDateTime} or {@link Instant}.
	 * @return The UNIX timestamp for the date.
	 */
	public static long of(final TemporalAccessor date) {
		if (!date.isSupported(ChronoField.INSTANT_SECONDS)) {
			final int y = date.get(ChronoField.YEAR);
			final int m = date.get(ChronoField.MONTH_OF_YEAR);
			final int d = date.get(ChronoField.DAY_OF_MONTH);
			final int h = date.get(ChronoField.HOUR_OF_DAY);
			final int min = date.get(ChronoField.MINUTE_OF_DAY);
			final int s = date.get(ChronoField.SECOND_OF_DAY);
			@SuppressWarnings("deprecation")
			final Date someDate = new Date(y - 1900, m - 1, d, h, min, s);
			return someDate.getTime();
		}
		if (!date.isSupported(ChronoField.MILLI_OF_SECOND))
			return 1000L * date.getLong(ChronoField.INSTANT_SECONDS);
		return 1000L * date.getLong(ChronoField.INSTANT_SECONDS) + date.getLong(ChronoField.MILLI_OF_SECOND);
	}

	/**
	 * @return The UNIX timestamp for the current time.
	 */
	public static long now() {
		return System.currentTimeMillis();
	}
}