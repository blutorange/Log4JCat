package de.homelab.madgaksha.log4jcat;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The main entry point for getting a log file trimmer instance. At the least,
 * you need to specify the log file entry format. You should also specify the
 * time zone of the log file entries, for example:
 *
 * <pre>
 * Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").timeZone("America/Los_Angeles").get();
 * </pre>
 *
 * The instance obtained can the be used to trim log files.
 *
 * @see Log4JCat
 * @author madgaksha
 */
public final class Log4J {
	private static long MIN_THRESHOLD = 1000L; // 1 KB
	private static long DEFAULT_THRESHOLD = 10000L; // 10 KB

	private final String patternLayout;
	private long threshold;
	private TimeZone timeZone;
	private Locale locale;

	private Log4J(@NonNull final String patternLayout) {
		this.patternLayout = patternLayout;
	}

	/**
	 * The time zone of the dates in the log file. Defaults to
	 * {@link ZoneOffset#UTC}.
	 *
	 * @param timeZone
	 *            Time zone to use.
	 * @return this for chaining.
	 */
	@NonNull
	public Log4J timeZone(@Nullable final TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	/**
	 * The time zone of the dates in the log file. Defaults to
	 * {@link ZoneOffset#UTC}.
	 *
	 * @param timeZone
	 *            Time zone to use. Either an abbreviation such as "PST", a full
	 *            name such as "America/Los_Angeles", or a custom ID such as
	 *            "GMT-8:00". Note that the support of abbreviations is for JDK
	 *            1.1.x compatibility only and full names should be used.
	 * @return this for chaining.
	 * @see TimeZone#getTimeZone(String)
	 */
	@NonNull
	public Log4J timeZone(@Nullable final String timeZone) {
		this.timeZone = timeZone != null ? TimeZone.getTimeZone(timeZone) : null;
		return this;
	}

	/**
	 * The locale is used when the pattern layout contains abbreviations such as
	 * "Jan" or "Monday". Default to the {@link Locale#ENGLISH}.
	 *
	 * @param locale
	 *            Locale to use.
	 * @return this for chaining.
	 */
	@NonNull
	public Log4J locale(@Nullable final Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * When the search range has been narrowed down to this threshold, the log
	 * file trimmer switches to a linear search algorithm. Defaults to 10000
	 * bytes.
	 *
	 * @param threshold
	 *            The threshold in bytes.
	 * @return this for chaining.
	 */
	@NonNull
	public Log4J threshold(final long threshold) {
		this.threshold = threshold;
		return this;
	}

	/**
	 * @return The actual log file trimmer with the configured options.
	 */
	@NonNull
	public Log4JCat get() {
		if (threshold == 0)
			threshold = DEFAULT_THRESHOLD;
		else if (threshold < MIN_THRESHOLD)
			threshold = 1000L;
		if (timeZone == null)
			timeZone = TimeZone.getTimeZone(ZoneOffset.UTC);
		if (locale == null)
			locale = Locale.ENGLISH;
		final ILogReaderFactory factory = new Log4JReaderFactory(patternLayout, locale, timeZone);
		return new Log4JCat(factory, threshold);
	}

	/**
	 * Returns a new builder with the given pattern layout and the default
	 * values for the time zone (UTC), locale (English), and threshold (10 KB).
	 *
	 * @param patternLayout
	 *            The pattern layout, ie. the log file format. See <a href=
	 *            "https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">the
	 *            docs</a> for details.
	 * @return A new builder for further configuration.
	 */
	public static Log4J of(@NonNull final String patternLayout) {
		return new Log4J(patternLayout);
	}
}