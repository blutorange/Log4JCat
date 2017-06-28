package com.github.blutorange.log4jcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.EnhancedPatternLayout;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Some utility methods for working with log files more easily. These all take
 * the log file from an Appender or Logger, filter the log entries and write
 * them to the given OutputStream. The Appender must be a FileAppender.
 * RollingFileAppender with multiple older files is supported. When a Logger is
 * passed, it takes all its FileAppenders, filters their log files and writes
 * them to the OutputStream.
 *
 * @author madgaksha
 */
public final class Cyperus {
	private Cyperus() {
		// Static methods.
	}

	/**
	 * Takes the log files the given appender and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie after the given start date and before the given end date are copied.
	 *
	 * @param appender
	 *            Appender which writes the log files. Must be a
	 *            {@link FileAppender}.
	 * @param output
	 *            Stream to write the output to.
	 * @param start
	 *            Only log entries after this date are taken.
	 * @param end
	 *            Only log entries before this date are taken.
	 * @return Whether the appender could be processed or not. It cannot be
	 *         processed when it does not write to a file that can be read.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static boolean trim(@NonNull final Appender appender, @NonNull final OutputStream output, final long start,
			final long end) throws IOException {
		return processAppender(appender, output, start, end, Cyperus::trim);
	}

	/**
	 * Takes the log files the given appender and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie before the given date are copied.
	 *
	 * @param appender
	 *            Appender which writes the log files. Must be a
	 *            {@link FileAppender}.
	 * @param output
	 *            Stream to write the output to.
	 * @param date
	 *            Only log entries after this date are taken.
	 * @return Whether the appender could be processed or not. It cannot be
	 *         processed when it does not write to a file that can be read.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static boolean tail(@NonNull final Appender appender, @NonNull final OutputStream output, final long date)
			throws IOException {
		return processAppender(appender, output, date, date + 1, Cyperus::tail);
	}

	/**
	 * Takes the log files the given appender and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie after the given date are copied.
	 *
	 * @param appender
	 *            Appender which writes the log files. Must be a
	 *            {@link FileAppender}.
	 * @param output
	 *            Stream to write the output to.
	 * @param date
	 *            Only log entries after this date are taken.
	 * @return Whether the appender could be processed or not. It cannot be
	 *         processed when it does not write to a file that can be read.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static boolean head(@NonNull final Appender appender, @NonNull final OutputStream output, final long date)
			throws IOException {
		return processAppender(appender, output, date, date + 1, Cyperus::head);
	}

	private static boolean processAppender(@NonNull final Appender appender, final OutputStream output,
			final long date1, final long date2, final RollingFileLambda lambda) throws IOException {
		if (date2 <= date1)
			return false;
		final Layout layout = appender.getLayout();
		final String patternLayout;
		int maxIndex = -1;
		String fileName = null;
		String encoding = null;
		if (layout instanceof PatternLayout) {
			patternLayout = ((PatternLayout) layout).getConversionPattern();
		}
		else if (layout instanceof EnhancedPatternLayout) {
			patternLayout = ((EnhancedPatternLayout) layout).getConversionPattern();
		}
		else {
			patternLayout = null;
		}
		if (appender instanceof RollingFileAppender) {
			encoding = ((RollingFileAppender) appender).getEncoding();
			maxIndex = ((RollingFileAppender) appender).getMaxBackupIndex();
			fileName = ((RollingFileAppender) appender).getFile();
		}
		else if (appender instanceof DailyRollingFileAppender) {
			maxIndex = -1;
			encoding = ((DailyRollingFileAppender) appender).getEncoding();
			fileName = ((DailyRollingFileAppender) appender).getFile();
		}
		if (patternLayout != null && fileName != null) {
			lambda.lambda(fileName, encoding, maxIndex, patternLayout, date1, date2, output);
			return true;
		}
		return false;
	}

	/**
	 * Takes the log files of all appenders and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie after the given date are copied.
	 *
	 * @param logger
	 *            Logger which writes the log files. When <code>null</code>, the
	 *            {@link Logger#getRootLogger()} is taken.
	 * @param output
	 *            Stream to write the output to.
	 * @param date
	 *            Only log entries after this date are taken.
	 * @return How many Appenders of the Logger could be processed.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static int tail(@Nullable final Logger logger, @NonNull final OutputStream output, final long date)
			throws IOException {
		@SuppressWarnings("unchecked")
		final Enumeration<Appender> e = (logger == null ? Logger.getRootLogger() : logger).getAllAppenders();
		int count = 0;
		while (e.hasMoreElements()) {
			final Appender appender = e.nextElement();
			if (tail(appender, output, date)) {
				++count;
			}
		}
		return count;
	}

	/**
	 * Takes the log files of all appenders and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie after the given start and before the given end are copied.
	 *
	 * @param logger
	 *            Logger which writes the log files. When <code>null</code>, the
	 *            {@link Logger#getRootLogger()} is taken.
	 * @param output
	 *            Stream to write the output to.
	 * @param start
	 *            Only log entries after this date are taken.
	 * @param end
	 *            Only log entries before this date are taken.
	 * @return How many Appenders of the Logger could be processed.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static int trim(@Nullable final Logger logger, @NonNull final OutputStream output, final long start,
			final long end) throws IOException {
		@SuppressWarnings("unchecked")
		final Enumeration<Appender> e = (logger == null ? Logger.getRootLogger() : logger).getAllAppenders();
		int count = 0;
		while (e.hasMoreElements()) {
			final Appender appender = e.nextElement();
			if (trim(appender, output, start, end)) {
				++count;
			}
		}
		return count;
	}

	/**
	 * Takes the log files of all appenders and copies their content to the
	 * given output stream, filtering the log entries so that only those that
	 * lie before the given date are copied.
	 *
	 * @param logger
	 *            Logger which writes the log files. When <code>null</code>, the
	 *            {@link Logger#getRootLogger()} is taken.
	 * @param output
	 *            Stream to write the output to.
	 * @param date
	 *            Only log entries after this date are taken.
	 * @return How many Appenders of the Logger could be processed.
	 * @throws IOException
	 *             When the input could not be read from or the output not
	 *             written to.
	 */
	public static int head(@Nullable final Logger logger, @NonNull final OutputStream output, final long date)
			throws IOException {
		@SuppressWarnings("unchecked")
		final Enumeration<Appender> e = (logger == null ? Logger.getRootLogger() : logger).getAllAppenders();
		int count = 0;
		while (e.hasMoreElements()) {
			final Appender appender = e.nextElement();
			if (head(appender, output, date)) {
				++count;
			}
		}
		return count;
	}

	private static void tail(final String baseName, final String encoding, final int maxIndex,
			final String patternLayout, final long date, @SuppressWarnings("unused") final long unused,
			final OutputStream output) throws IOException {
		final List<String> list = new ArrayList<>(maxIndex + 2);
		final Log4JCat cat = Log4J.of(patternLayout).get();
		File file = new File(baseName);
		int index = -1;
		long pos = 0;
		do {
			++index;
			if (file.exists() && file.canRead()) {
				try (final IRandomAccessInput input = InputFactory.open(file, encoding)) {
					pos = cat.find(input, date);
					// Check whether the file contains any relevant content.
					if (pos < input.length()) {
						list.add(file.getCanonicalPath());
					}
					if (pos > 0) {
						// We are done, other files are definitely older.
						break;
					}
				}
			}
			file = new File(baseName + "." + index);
		}
		while (index <= maxIndex);

		// Copy the content of the log files to the output stream.
		for (int i = list.size(); i-- > 0;) {
			final String fileName = list.get(i);
			try (InputStream input = new FileInputStream(fileName)) {
				if (i == list.size() - 1) {
					input.skip(pos);
				}
				IOUtils.copy(input, output);
			}
		}
	}

	private static void head(final String baseName, final String encoding, final int maxIndex,
			final String patternLayout, final long date, @SuppressWarnings("unused") final long unused,
			final OutputStream output) throws IOException {
		final List<String> list = new ArrayList<>(maxIndex + 2);
		final Log4JCat cat = Log4J.of(patternLayout).get();
		File file;
		int index = maxIndex;
		long pos = 0;
		do {
			file = new File(index < 0 ? baseName : (baseName + "." + index));
			if (file.exists() && file.canRead()) {
				try (final IRandomAccessInput input = InputFactory.open(file, encoding)) {
					pos = cat.find(input, date);
					// Nothing that matches in the file.
					if (pos > 0) {
						list.add(file.getCanonicalPath());
					}
					if (pos < input.length()) {
						// We are done, other files are definitely younger.
						break;
					}
				}
			}
			--index;
		}
		while (index >= -1);

		// Copy the content of the log files to the output stream.
		for (int i = 0; i < list.size(); ++i) {
			final String fileName = list.get(i);
			try (InputStream input = new FileInputStream(fileName)) {
				if (i == list.size() - 1) {
					// BoundedInputStream only closes the delegate, which we
					// already close.
					@SuppressWarnings("resource")
					final InputStream bounded = new BoundedInputStream(input, pos);
					IOUtils.copy(bounded, output);
				}
				else {
					IOUtils.copy(input, output);
				}
			}
		}
	}

	private static void trim(final String baseName, final String encoding, final int maxIndex,
			final String patternLayout, final long start, final long end, final OutputStream output)
			throws IOException {
		final List<String> list = new ArrayList<>(maxIndex + 2);
		final Log4JCat cat = Log4J.of(patternLayout).get();
		File file;
		int index = maxIndex;
		long posStart = -1;
		long posEnd = -1;
		do {
			file = new File(index < 0 ? baseName : (baseName + "." + index));
			if (file.exists() && file.canRead()) {
				try (final IRandomAccessInput input = InputFactory.open(file, encoding)) {
					long pos = cat.find(input, posStart == -1 ? start : end);
					boolean added = false;
					if (posStart < 0) {
						// Looking for the start.
						if (pos < input.length()) {
							// Found the start.
							list.add(file.getCanonicalPath());
							posStart = pos;
							input.seek(0);
							pos = cat.find(input, end);
							added = true;
						}
					}
					// No 'else', start and end could be in the same file.
					if (posStart >= 0) {
						// Looking for the end.
						if (pos > 0 && !added) {
							list.add(file.getCanonicalPath());
						}
						if (pos < input.length()) {
							// Found the end.
							posEnd = pos;
							break;
						}
					}
				}
			}
			--index;
		}
		while (index >= -1);

		if (end <= start)
			return;

		// Copy the content of the log files to the output stream.
		for (int i = 0; i < list.size(); ++i) {
			final String fileName = list.get(i);
			try (InputStream input = new FileInputStream(fileName)) {
				if (i == 0) {
					input.skip(posStart);
				}
				if (i == list.size() - 1) {
					// When the start and end position point are in the same
					// file,
					// the number of bytes to copy is the difference between the
					// end and the start.
					final long length = list.size() == 1 ? posEnd - posStart : posEnd;
					// BoundedInputStream only closes the delegate, which we
					// already closed.
					@SuppressWarnings("resource")
					final InputStream bounded = new BoundedInputStream(input, length);
					IOUtils.copy(bounded, output);
				}
				else {
					IOUtils.copy(input, output);
				}
			}
		}
	}

	private static interface RollingFileLambda {
		public void lambda(final String baseName, final String encoding, final int maxIndex, final String patternLayout,
				final long date1, final long date2, final OutputStream output) throws IOException;
	}
}