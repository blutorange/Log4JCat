package com.github.blutorange.log4jcat;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.spi.LoggingEvent;
import org.slf4j.Marker;
import org.slf4j.event.Level;

class LoggingEvent4JForwarder implements org.slf4j.event.LoggingEvent {
	private final LoggingEvent event;
	public LoggingEvent4JForwarder(final LoggingEvent event) {
		this.event = event;
	}

	@Override
	public String getMessage() {
		return event.getRenderedMessage();
	}

	@Override
	public Level getLevel() {
		switch (event.getLevel().toInt()) {
		case org.apache.log4j.Level.TRACE_INT:
			return Level.TRACE;
		case org.apache.log4j.Level.ALL_INT:
			return Level.TRACE;
		case org.apache.log4j.Level.DEBUG_INT:
			return Level.DEBUG;
		case org.apache.log4j.Level.ERROR_INT:
			return Level.DEBUG;
		case org.apache.log4j.Level.FATAL_INT:
			return Level.ERROR;
		case org.apache.log4j.Level.INFO_INT:
			return Level.INFO;
		case org.apache.log4j.Level.OFF_INT:
			return Level.ERROR;
		case org.apache.log4j.Level.WARN_INT:
			return Level.WARN;
		default:
			return Level.TRACE;
		}
	}
	@Override
	public Marker getMarker() {
		return null;
	}
	@Override
	public String getLoggerName() {
		return event.getLoggerName();
	}
	@Override
	public String getThreadName() {
		return event.getThreadName();
	}
	@Override
	public Object[] getArgumentArray() {
		return ArrayUtils.EMPTY_OBJECT_ARRAY;
	}
	@Override
	public long getTimeStamp() {
		return event.getTimeStamp();
	}
	@Override
	public Throwable getThrowable() {
		return event.getThrowableInformation().getThrowable();
	}
}