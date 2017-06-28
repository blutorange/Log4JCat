/**
 * A small utility to trim log files from Log4J with a
 * {@link org.apache.log4j.PatternLayout}.
 *
 * <p>
 * Sometimes you've only got the log files and want or need to trim them, eg. to
 * get a file with the last 2 hours programmatically, and send it via mail. Not
 * a well defined task, but we can have a pretty good guess (unless the log
 * format is plain weird.) As log files can be pretty large, up to several GB in
 * some cases, it would take a long time to scan them line-by-line. However,
 * since all entries within a log file are sorted chronologically from oldest to
 * newest, we can perform a quick binary search.
 * </p>
 * <p>
 * This is what this does in a nutshell, for LOG4J log files with its log
 * entries formatted according to a certain specified [Pattern
 * Layout](https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html).
 * </p>
 * <p>
 * A simple example that reads a log file from a local file and gets all entries
 * starting after the current time.
 * </p>
 *
 * <pre>
 * public class Sandbox {
 * 	public static void main(final String[] args) throws IOException {
 * 		// Create a new log file trimmer with a specified log entry format.
 * 		// Log files with this format look like this
 * 		// [DEBUG] 2017-06-22 21:57:53,661 TimeClass - Past
 * 		// [INFO ] 2017-06-23 21:57:53,661 TimeClass - Present
 * 		// [WARN ] 2017-06-24 21:57:53,661 TimeClass - Future
 * 		final Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").get();
 * 		// Open a stream for the log file.
 * 		try (final IRandomAccessInput input = InputFactory.open(new File("~/mylogfile"))) {
 * 			// Trim the log file so that it contains only entries more recent
 * 			// than the current date.
 * 			// This returns the offset in the file where the entries start.
 * 			final long pos = cat.tail(input, new Date());
 * 			// Seek to the starting position.
 * 			input.seek(pos);
 * 			// And print the all lines from the starting position.
 * 			System.out.print(input.readLines());
 * 		}
 * 	}
 * }
 * </pre>
 */
package com.github.blutorange.log4jcat;