package de.homelab.madgaksha.log4jcat;

import org.apache.log4j.spi.LoggingEvent;

interface ILoggerCallback {
	public boolean doPost(LoggingEvent event);
}