package de.homelab.madgaksha.log4jcat;

import java.io.IOException;

import org.slf4j.event.LoggingEvent;

interface ILogReader {
	/**
	 * Reads and returns the next logging event, skipping the current event.
	 * @param reader Input from which to read data.
	 * @return The logging event or null if none has been found.
	 * @throws IOException When the stream could not be read.
	 */
	public LoggingEvent processSingle(IRandomAccessInput input) throws IOException;

	/**
	 * Seeks to the next logging event and positions the file pointer at the beginning of the line.
	 * @param reader File to read from. The pointer should initially be at the beginning of a line.
	 * @return Whether a next event has been found.
	 * @throws IOException When the stream could not be read.
	 */
	public boolean seekToNextEvent(IRandomAccessInput input) throws IOException;
}