package com.github.blutorange.log4jcat;

import java.util.Locale;
import java.util.TimeZone;

class Log4JReaderFactory implements ILogReaderFactory {
	private final String patternLayout;
	private final TimeZone timeZone;
	private final Locale locale;
	public Log4JReaderFactory(final String patternLayout, final Locale locale, final TimeZone timeZone) {
		this.locale = locale;
		this.timeZone = timeZone;
		this.patternLayout = patternLayout;
	}
	@Override
	public ILogReader create() {
		return new Log4JReader(patternLayout, locale, timeZone);
	}
}