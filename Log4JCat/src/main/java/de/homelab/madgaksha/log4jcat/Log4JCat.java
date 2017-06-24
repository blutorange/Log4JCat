package de.homelab.madgaksha.log4jcat;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

public class Log4JCat {
	private static long MIN_THRESHOLD = 1000L; // 1 KB
	private static long DEFAULT_THRESHOLD = 10000L; // 10 KB

	private final String patternLayout;
	private long threshold;
	private TimeZone timeZone;
	private Locale locale;

	private Log4JCat(@NonNull final String patternLayout) {
		this.patternLayout = patternLayout;
	}

	public Log4JCat timeZone(@Nullable final TimeZone timeZone) {
		this.timeZone = timeZone;
		return this;
	}

	public Log4JCat locale(@Nullable final Locale locale) {
		this.locale = locale;
		return this;
	}

	public Log4JCat threshold(final long threshold) {
		this.threshold = threshold;
		return this;
	}

	public Log4JCatImpl get() {
		if (threshold == 0)
			threshold = DEFAULT_THRESHOLD;
		else if (threshold < MIN_THRESHOLD)
			threshold = 1000L;
		if (timeZone == null)
			timeZone = TimeZone.getTimeZone(ZoneOffset.UTC);
		if (locale == null)
			locale = Locale.ENGLISH;
		return new Log4JCatImpl(patternLayout, locale, timeZone, threshold);
	}

	/**
	 *
	 * @param patternLayout The pattern layout, ie. the log file format.
	 * See <a href="https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html">the docs</a>
	 * for details.
	 * @return
	 */
	public static Log4JCat of(@NonNull final String patternLayout) {
		return new Log4JCat(patternLayout);
	}
}