/* Zum Lesen von Logdateien mit einem bestimmer patternLayout-Format
 * Nutzt dazu Code von Apache Chainsaw und Apache Log4J-Extras.
 *
 * https://logging.apache.org/chainsaw/
 * https://logging.apache.org/log4j/extras/
 */

package de.homelab.madgaksha.log4jcat;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.pattern.ClassNamePatternConverter;
import org.apache.log4j.pattern.DatePatternConverter;
import org.apache.log4j.pattern.FileLocationPatternConverter;
import org.apache.log4j.pattern.FormattingInfo;
import org.apache.log4j.pattern.FullLocationPatternConverter;
import org.apache.log4j.pattern.LevelPatternConverter;
import org.apache.log4j.pattern.LineLocationPatternConverter;
import org.apache.log4j.pattern.LineSeparatorPatternConverter;
import org.apache.log4j.pattern.LiteralPatternConverter;
import org.apache.log4j.pattern.LoggerPatternConverter;
import org.apache.log4j.pattern.LoggingEventPatternConverter;
import org.apache.log4j.pattern.MessagePatternConverter;
import org.apache.log4j.pattern.MethodLocationPatternConverter;
import org.apache.log4j.pattern.NDCPatternConverter;
import org.apache.log4j.pattern.PatternParser;
import org.apache.log4j.pattern.PropertiesPatternConverter;
import org.apache.log4j.pattern.RelativeTimePatternConverter;
import org.apache.log4j.pattern.SequenceNumberPatternConverter;
import org.apache.log4j.pattern.ThreadPatternConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

class Log4JReader {

	private static String getLogFormatFromPatternLayout(final String patternLayout) {
		final String input = OptionConverter.convertSpecialChars(patternLayout);
		final List<LoggingEventPatternConverter> converters = new ArrayList<>();
		final List<FormattingInfo> fields = new ArrayList<>();
		final Map<?,?> converterRegistry = null;

		PatternParser.parse(input, converters, fields, converterRegistry, PatternParser.getPatternLayoutRules());
		return getFormatFromConverters(converters);
	}

	private static String getTimeStampFormat(final String patternLayout) {
		final int basicIndex = patternLayout.indexOf("%d");
		if (basicIndex < 0) {
			return null;
		}

		final int index = patternLayout.indexOf("%d{");
		// %d - default
		if (index < 0) {
			return "yyyy-MM-dd HH:mm:ss,SSS";
		}

		final int length = patternLayout.substring(index).indexOf("}");
		final String timestampFormat = patternLayout.substring(index + "%d{".length(), index + length);
		if (timestampFormat.equals("ABSOLUTE")) {
			return "HH:mm:ss,SSS";
		}
		if (timestampFormat.equals("ISO8601")) {
			return "yyyy-MM-dd HH:mm:ss,SSS";
		}
		if (timestampFormat.equals("DATE")) {
			return "dd MMM yyyy HH:mm:ss,SSS";
		}
		return timestampFormat;
	}

	private static String getFormatFromConverters(final List<LoggingEventPatternConverter> converters) {
		final StringBuffer buffer = new StringBuffer();
		for (final LoggingEventPatternConverter converter : converters) {
			if (converter instanceof DatePatternConverter) {
				buffer.append("TIMESTAMP");
			} else if (converter instanceof MessagePatternConverter) {
				buffer.append("MESSAGE");
			} else if (converter instanceof LoggerPatternConverter) {
				buffer.append("LOGGER");
			} else if (converter instanceof ClassNamePatternConverter) {
				buffer.append("CLASS");
			} else if (converter instanceof RelativeTimePatternConverter) {
				buffer.append("PROP(RELATIVETIME)");
			} else if (converter instanceof ThreadPatternConverter) {
				buffer.append("THREAD");
			} else if (converter instanceof NDCPatternConverter) {
				buffer.append("NDC");
			} else if (converter instanceof LiteralPatternConverter) {
				final LiteralPatternConverter literal = (LiteralPatternConverter) converter;
				// format shouldn't normally take a null, but we're getting a
				// literal, so passing in the buffer will work
				literal.format(null, buffer);
			} else if (converter instanceof SequenceNumberPatternConverter) {
				buffer.append("PROP(log4jid)");
			} else if (converter instanceof LevelPatternConverter) {
				buffer.append("LEVEL");
			} else if (converter instanceof MethodLocationPatternConverter) {
				buffer.append("METHOD");
			} else if (converter instanceof FullLocationPatternConverter) {
				buffer.append("PROP(locationInfo)");
			} else if (converter instanceof LineLocationPatternConverter) {
				buffer.append("LINE");
			} else if (converter instanceof FileLocationPatternConverter) {
				buffer.append("FILE");
			} else if (converter instanceof PropertiesPatternConverter) {
				// PropertiesPatternConverter propertiesConverter =
				// (PropertiesPatternConverter) converter;
				// String option = propertiesConverter.getOption();
				// if (option != null && option.length() > 0) {
				// buffer.append("PROP(" + option + ")");
				// } else {
				buffer.append("PROP(PROPERTIES)");
				// }
			} else if (converter instanceof LineSeparatorPatternConverter) {
				// done
			}
		}
		return buffer.toString();
	}

	/**
	 * LogFilePatternReceiver can parse and tail log files, converting entries
	 * into LoggingEvents. If the file doesn't exist when the receiver is
	 * initialized, the receiver will look for the file once every 10 seconds.
	 * <p>
	 * This receiver relies on java.util.regex features to perform the parsing
	 * of text in the log file, however the only regular expression field
	 * explicitly supported is a glob-style wildcard used to ignore fields in
	 * the log file if needed. All other fields are parsed by using the supplied
	 * keywords.
	 * <p>
	 * <b>Features:</b><br>
	 * - specify the URL of the log file to be processed<br>
	 * - specify the timestamp format in the file (if one exists, using patterns
	 * from {@link java.text.SimpleDateFormat})<br>
	 * - specify the pattern (logFormat) used in the log file using keywords, a
	 * wildcard character (*) and fixed text<br>
	 * - 'tail' the file (allows the contents of the file to be continually read
	 * and new events processed)<br>
	 * - supports the parsing of multi-line messages and exceptions - 'hostname'
	 * property set to URL host (or 'file' if not available) - 'application'
	 * property set to URL path (or value of fileURL if not available)
	 * <p>
	 * <b>Keywords:</b><br>
	 * TIMESTAMP<br>
	 * LOGGER<br>
	 * LEVEL<br>
	 * THREAD<br>
	 * CLASS<br>
	 * FILE<br>
	 * LINE<br>
	 * METHOD<br>
	 * RELATIVETIME<br>
	 * MESSAGE<br>
	 * NDC<br>
	 * PROP(key)<br>
	 * <p>
	 * Use a * to ignore portions of the log format that should be ignored
	 * <p>
	 * Example:<br>
	 * If your file's patternlayout is this:<br>
	 * <b>%d %-5p [%t] %C{2} (%F:%L) - %m%n</b>
	 * <p>
	 * specify this as the log format:<br>
	 * <b>TIMESTAMP LEVEL [THREAD] CLASS (FILE:LINE) - MESSAGE</b>
	 * <p>
	 * To define a PROPERTY field, use PROP(key)
	 * <p>
	 * Example:<br>
	 * If you used the RELATIVETIME pattern layout character in the file, you
	 * can use PROP(RELATIVETIME) in the logFormat definition to assign the
	 * RELATIVETIME field as a property on the event.
	 * <p>
	 * If your file's patternlayout is this:<br>
	 * <b>%r [%t] %-5p %c %x - %m%n</b>
	 * <p>
	 * specify this as the log format:<br>
	 * <b>PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE</b>
	 * <p>
	 * Note the * - it can be used to ignore a single word or sequence of words
	 * in the log file (in order for the wildcard to ignore a sequence of words,
	 * the text being ignored must be followed by some delimiter, like '-' or
	 * '[') - ndc is being ignored in the following example.
	 * <p>
	 * Assign a filterExpression in order to only process events which match a
	 * filter. If a filterExpression is not assigned, all events are processed.
	 * <p>
	 * <b>Limitations:</b><br>
	 * - no support for the single-line version of throwable supported by
	 * patternlayout<br>
	 * (this version of throwable will be included as the last line of the
	 * message)<br>
	 * - the relativetime patternLayout character must be set as a property:
	 * PROP(RELATIVETIME)<br>
	 * - messages should appear as the last field of the logFormat because the
	 * variability in message content<br>
	 * - exceptions are converted if the exception stack trace (other than the
	 * first line of the exception)<br>
	 * is stored in the log file with a tab followed by the word 'at' as the
	 * first characters in the line<br>
	 * - tailing may fail if the file rolls over.
	 * <p>
	 * <b>Example receiver configuration settings</b> (add these as params,
	 * specifying a LogFilePatternReceiver 'plugin'):<br>
	 * param: "timestampFormat" value="yyyy-MM-d HH:mm:ss,SSS"<br>
	 * param: "logFormat"
	 * value="PROP(RELATIVETIME) [THREAD] LEVEL LOGGER * - MESSAGE"<br>
	 * param: "fileURL" value="file:///c:/events.log"<br>
	 * param: "tailing" value="true"
	 * <p>
	 * This configuration will be able to process these sample events:<br>
	 * 710 [ Thread-0] DEBUG first.logger first - <test> <test2>something
	 * here</test2> <test3 blah=something/> <test4> <test5>something
	 * else</test5> </test4></test><br>
	 * 880 [ Thread-2] DEBUG first.logger third - <test> <test2>something
	 * here</test2> <test3 blah=something/> <test4> <test5>something
	 * else</test5> </test4></test><br>
	 * 880 [ Thread-0] INFO first.logger first - infomsg-0<br>
	 * java.lang.Exception: someexception-first<br>
	 * at Generator2.run(Generator2.java:102)<br>
	 *
	 * @author Scott Deboy
	 */
	private final ArrayList<String> keywords = new ArrayList<>();

	private static final String PROP_START = "PROP(";
	private static final String PROP_END = ")";

	private static final String LOGGER = "LOGGER";
	private static final String MESSAGE = "MESSAGE";
	private static final String TIMESTAMP = "TIMESTAMP";
	private static final String NDC = "NDC";
	private static final String LEVEL = "LEVEL";
	private static final String THREAD = "THREAD";
	private static final String CLASS = "CLASS";
	private static final String FILE = "FILE";
	private static final String LINE = "LINE";
	private static final String METHOD = "METHOD";

	// all lines other than first line of exception begin with tab followed by
	// 'at' followed by text
	private static final String REGEXP_DEFAULT_WILDCARD = ".*?";
	private static final String REGEXP_GREEDY_WILDCARD = ".*";
	private static final String PATTERN_WILDCARD = "*";
	private static final String NOSPACE_GROUP = "(\\S*\\s*?)";
	private static final String DEFAULT_GROUP = "(" + REGEXP_DEFAULT_WILDCARD
			+ ")";
	private static final String GREEDY_GROUP = "(" + REGEXP_GREEDY_WILDCARD
			+ ")";
	private static final String MULTIPLE_SPACES_REGEXP = "[ ]+";
	private final String newLine = System.getProperty("line.separator");

	private final String[] emptyException = new String[] { "" };

	private SimpleDateFormat dateFormat;
	private String timestampFormat = "yyyy-MM-d HH:mm:ss,SSS";
	private final String logFormat;

	private static final String VALID_DATEFORMAT_CHARS = "GyMwWDdFEaHkKhmsSzZ";
	private static final String VALID_DATEFORMAT_CHAR_PATTERN = "["
			+ VALID_DATEFORMAT_CHARS + "]";

	private HashMap<String, String> currentMap;
	private ArrayList<String> additionalLines;
	private Stack<String> additionalLinesStack;
	private ArrayList<String> matchingKeywords;

	private String regexp;
	private Pattern regexpPattern;
	private String timestampPatternText;

	public static final int MISSING_FILE_RETRY_MILLIS = 10000;

	public Log4JReader(final String patternLayout) {
		keywords.add(TIMESTAMP);
		keywords.add(LOGGER);
		keywords.add(LEVEL);
		keywords.add(THREAD);
		keywords.add(CLASS);
		keywords.add(FILE);
		keywords.add(LINE);
		keywords.add(METHOD);
		keywords.add(MESSAGE);
		keywords.add(NDC);
		logFormat = getLogFormatFromPatternLayout(patternLayout);
		timestampFormat = getTimeStampFormat(patternLayout);
		initialize();
		createPattern();
	}

	/**
	 * Combine all message lines occuring in the additionalLines list, adding a
	 * newline character between each line
	 * <p>
	 * the event will already have a message - combine this message with the
	 * message lines in the additionalLines list (all entries prior to the
	 * exceptionLine index)
	 *
	 * @param firstMessageLine
	 *            primary message line
	 * @param exceptionLine
	 *            index of first exception line
	 * @return message
	 */
	private String buildMessage(final String firstMessageLine) {
		if (additionalLines.size() == 0) {
			return firstMessageLine;
		}
		final StringBuffer message = new StringBuffer();
		if (firstMessageLine != null) {
			message.append(firstMessageLine);
		}

		final int linesToProcess = additionalLines.size();
		for (int i = 0; i < linesToProcess; i++) {
			message.append(newLine);
			message.append(additionalLines.get(i));
		}
		return message.toString();
	}

	private String buildMessageStack(final String firstMessageLine) {
		if (additionalLinesStack.size() == 0) {
			return firstMessageLine;
		}
		final StringBuffer message = new StringBuffer();
		if (firstMessageLine != null) {
			message.append(firstMessageLine);
		}
		final int linesToProcess = additionalLinesStack.size();
		for (int i = 0; i < linesToProcess; i++) {
			message.append(newLine);
			message.append(additionalLinesStack.get(i));
		}
		return message.toString();
	}

	/**
	 * Construct a logging event from currentMap and additionalLines
	 * (additionalLines contains multiple message lines and any exception lines)
	 * <p>
	 * CurrentMap and additionalLines are cleared in the process
	 *
	 * @return event
	 */
	private LoggingEvent buildEvent() {
		if (currentMap.size() == 0) {
			additionalLines.clear();
			return null;
		}
		// the current map contains fields - build an event
		if (additionalLines.size() > 0) {
			currentMap.put(MESSAGE,
					buildMessage(currentMap.get(MESSAGE)));
		}
		final LoggingEvent event = convertToEvent(currentMap);
		currentMap.clear();
		additionalLines.clear();
		return event;
	}

	private LoggingEvent buildEventStack(final HashMap<String, String> currentMap) {
		if (currentMap.size() == 0) {
			additionalLinesStack.clear();
			return null;
		}
		// the current map contains fields - build an event
		// messages are listed before exceptions in additional lines
		if (additionalLinesStack.size() > 0) {
			currentMap.put(MESSAGE,
					buildMessageStack(currentMap.get(MESSAGE)));
		}
		final LoggingEvent event = convertToEvent(currentMap);
		additionalLinesStack.clear();
		return event;
	}

	/**
	 * Read, parse and optionally tail the log file, converting entries into
	 * logging events.
	 *
	 * A runtimeException is thrown if the logFormat pattern is malformed.
	 *
	 * @param bufferedReader
	 * @throws IOException
	 */
	private void process(final BufferedReader bufferedReader, final ILoggerCallback callback)
			throws IOException {
//		LoggingEvent event;
//		while ( (event = processSingle(bufferedReader)) != null) {
//			if (!callback.doPost(event)) return;
//		}
		Matcher eventMatcher;
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			// skip empty line entries
			eventMatcher = regexpPattern.matcher(line);
			if (eventMatcher.matches()) {
				// build an event from the previous match (held in current map)
				final LoggingEvent event = buildEvent();
				if (event != null && !callback.doPost(event))
					return;
				currentMap.putAll(processEvent(eventMatcher.toMatchResult()));
			} else {
				additionalLines.add(line);
			}
		}

		// process last event if one exists
		final LoggingEvent event = buildEvent();
		if (event != null && !callback.doPost(event))
			return;
	}

	/**
	 * Read, parse and optionally tail the log file, converting entries into
	 * logging events.
	 *
	 * A runtimeException is thrown if the logFormat pattern is malformed.
	 *
	 * @param bufferedReader
	 * @throws IOException
	 */
	private void processReverse(final ReversedLinesFileReader reader,
			final ILoggerCallback callback) throws IOException {
		Matcher eventMatcher;
		String line;
		while ((line = reader.readLine()) != null) {
			eventMatcher = regexpPattern.matcher(line);
			if (eventMatcher.matches()) {
				// build an event from the previous match (held in current map)
				final LoggingEvent event = buildEventStack(processEvent(eventMatcher
						.toMatchResult()));
				if (event != null && !callback.doPost(event))
					return;
			} else {
				additionalLinesStack.push(line);
			}
		}

		// process last event if one exists
		final LoggingEvent event = buildEvent();
		if (event != null && !callback.doPost(event))
			return;
	}

	/** Reads and returns the next logging event, skipping the current event.
	 * @param reader Input from which to read data.
	 * @return The logging event or null if none has been found.
	 */
	public LoggingEvent processSingle(final IRandomAccessInputStream input) throws IOException {
		Matcher eventMatcher;
		String line;
		boolean foundEvent = false;
		long pos = input.tell();
		additionalLinesStack.clear();
		currentMap.clear();
		while ((line = input.readLine()) != null) {
			// skip empty line entries
			eventMatcher = regexpPattern.matcher(line);
			if (eventMatcher.matches()) {
				// build an event from the previous match (held in current map)
				if (foundEvent){
					break;
				}
				foundEvent = true;
				currentMap.putAll(processEvent(eventMatcher.toMatchResult()));
			}
			else if (foundEvent) {
				additionalLines.add(line);
			}
			pos = input.tell();
		}

		// process last event if one exists
		input.seek(pos);
		return foundEvent ? buildEvent() : null;
	}

	/** Seeks to the next logging event and positions the file pointer at the beginning of the line.
	 * @param reader File to read from. The pointer should initially be at the beginning of a line.
	 * @return Whether a next event has been found.
	 */
	public boolean seekToNextEvent(final IRandomAccessInputStream input) throws IOException {
		Matcher eventMatcher;
		String line;
		long pos;
		additionalLinesStack.clear();
		currentMap.clear();
		pos = input.tell();
		while ((line = input.readLine()) != null) {
			eventMatcher = regexpPattern.matcher(line);
			if (eventMatcher.matches()) {
				input.seek(pos);
				return true;
			}
			pos = input.tell();
		}
		return false;
	}

	/** Reads and returns the last logging event.
	 * @param reader Input from which to read data.
	 * @return The logging event or null if none has been found.
	 */
	public LoggingEvent processSingleReverse(final ReversedLinesFileReader reader) throws IOException {
		LoggingEvent event = null;
		Matcher eventMatcher;
		String line;
		additionalLinesStack.clear();
		currentMap.clear();
		while ((line = reader.readLine()) != null) {
			eventMatcher = regexpPattern.matcher(line);
			if (eventMatcher.matches()) {
				event = buildEventStack(processEvent(eventMatcher.toMatchResult()));
				break;
			}
			additionalLinesStack.push(line);
		}
		return event;
	}


	private void createPattern() {
		regexpPattern = Pattern.compile(regexp);
	}

	/**
	 * Convert the match into a map.
	 * <p>
	 * Relies on the fact that the matchingKeywords list is in the same order as
	 * the groups in the regular expression
	 *
	 * @param result
	 * @return map
	 */
	private HashMap<String, String> processEvent(final MatchResult result) {
		final HashMap<String, String> map = new HashMap<>();
		// group zero is the entire match - process all other groups
		for (int i = 1; i < result.groupCount() + 1; i++) {
			final String key = matchingKeywords.get(i - 1);
			final String value = result.group(i);
			map.put(key, value);
		}
		return map;
	}

	/**
	 * Helper method that will convert timestamp format to a pattern
	 *
	 *
	 * @return string
	 */
	private String convertTimestamp() {
		// some locales (for example, French) generate timestamp text with
		// characters not included in \w -
		// now using \S (all non-whitespace characters) instead of /w
		String result = timestampFormat.replaceAll(
				VALID_DATEFORMAT_CHAR_PATTERN + "+", "\\\\S+");
		// make sure dots in timestamp are escaped
		result = result.replaceAll(Pattern.quote("."), "\\\\.");
		return result;
	}

	/**
	 * Build the regular expression needed to parse log entries
	 *
	 */
	private void initialize() {
		currentMap = new HashMap<>();
		additionalLines = new ArrayList<>();
		additionalLinesStack = new Stack<>();
		matchingKeywords = new ArrayList<>();

		if (timestampFormat != null) {
			dateFormat = new SimpleDateFormat(
					quoteTimeStampChars(timestampFormat));
			timestampPatternText = convertTimestamp();
		}

		final ArrayList<String> buildingKeywords = new ArrayList<>();

		String newPattern = logFormat;

		int index = 0;
		String current = newPattern;
		// build a list of property names and temporarily replace the property
		// with an empty string,
		// we'll rebuild the pattern later
		final ArrayList<String> propertyNames = new ArrayList<>();
		while (index > -1) {
			if (current.indexOf(PROP_START) > -1
					&& current.indexOf(PROP_END) > -1) {
				index = current.indexOf(PROP_START);
				final String longPropertyName = current.substring(
						current.indexOf(PROP_START),
						current.indexOf(PROP_END) + 1);
				final String shortProp = getShortPropertyName(longPropertyName);
				buildingKeywords.add(shortProp);
				propertyNames.add(longPropertyName);
				current = current.substring(longPropertyName.length() + 1
						+ index);
				newPattern = singleReplace(newPattern, longPropertyName,
						new Integer(buildingKeywords.size() - 1).toString());
			} else {
				// no properties
				index = -1;
			}
		}

		/*
		 * we're using a treemap, so the index will be used as the key to ensure
		 * keywords are ordered correctly
		 *
		 * examine pattern, adding keywords to an index-based map patterns can
		 * contain only one of these per entry...properties are the only
		 * 'keyword' that can occur multiple times in an entry
		 */
		final Iterator<String> iter = keywords.iterator();
		while (iter.hasNext()) {
			final String keyword = iter.next();
			final int index2 = newPattern.indexOf(keyword);
			if (index2 > -1) {
				buildingKeywords.add(keyword);
				newPattern = singleReplace(newPattern, keyword, new Integer(
						buildingKeywords.size() - 1).toString());
			}
		}

		String buildingInt = "";

		for (int i = 0; i < newPattern.length(); i++) {
			final String thisValue = String.valueOf(newPattern.substring(i, i + 1));
			if (isInteger(thisValue)) {
				buildingInt = buildingInt + thisValue;
			} else {
				if (isInteger(buildingInt)) {
					matchingKeywords.add(buildingKeywords.get(Integer
							.parseInt(buildingInt)));
				}
				// reset
				buildingInt = "";
			}
		}

		// if the very last value is an int, make sure to add it
		if (isInteger(buildingInt)) {
			matchingKeywords.add(buildingKeywords.get(Integer
					.parseInt(buildingInt)));
		}

		newPattern = replaceMetaChars(newPattern);

		// compress one or more spaces in the pattern into the [ ]+ regexp
		// (supports padding of level in log files)
		newPattern = newPattern.replaceAll(MULTIPLE_SPACES_REGEXP,
				MULTIPLE_SPACES_REGEXP);
		newPattern = newPattern.replaceAll(Pattern.quote(PATTERN_WILDCARD),
				REGEXP_DEFAULT_WILDCARD);
		// use buildingKeywords here to ensure correct order
		for (int i = 0; i < buildingKeywords.size(); i++) {
			final String keyword = buildingKeywords.get(i);
			// make the final keyword greedy (we're assuming it's the message)
			if (i == (buildingKeywords.size() - 1)) {
				newPattern = singleReplace(newPattern, String.valueOf(i),
						GREEDY_GROUP);
			} else if (TIMESTAMP.equals(keyword)) {
				newPattern = singleReplace(newPattern, String.valueOf(i), "("
						+ timestampPatternText + ")");
			} else if (LOGGER.equals(keyword) || LEVEL.equals(keyword)) {
				newPattern = singleReplace(newPattern, String.valueOf(i),
						NOSPACE_GROUP);
			} else {
				newPattern = singleReplace(newPattern, String.valueOf(i),
						DEFAULT_GROUP);
			}
		}

		regexp = newPattern;
	}

	private boolean isInteger(final String value) {
		try {
			Integer.parseInt(value);
			return true;
		} catch (@SuppressWarnings("unused") final NumberFormatException ignored) {
			return false;
		}
	}

	private String quoteTimeStampChars(final String input) {
		// put single quotes around text that isn't a supported dateformat char
		final StringBuffer result = new StringBuffer();
		// ok to default to false because we also check for index zero below
		boolean lastCharIsDateFormat = false;
		for (int i = 0; i < input.length(); i++) {
			final String thisVal = input.substring(i, i + 1);
			final boolean thisCharIsDateFormat = VALID_DATEFORMAT_CHARS
					.contains(thisVal);
			// we have encountered a non-dateformat char
			if (!thisCharIsDateFormat && (i == 0 || lastCharIsDateFormat)) {
				result.append("'");
			}
			// we have encountered a dateformat char after previously
			// encountering a non-dateformat char
			if (thisCharIsDateFormat && i > 0 && !lastCharIsDateFormat) {
				result.append("'");
			}
			lastCharIsDateFormat = thisCharIsDateFormat;
			result.append(thisVal);
		}
		// append an end single-quote if we ended with non-dateformat char
		if (!lastCharIsDateFormat) {
			result.append("'");
		}
		return result.toString();
	}

	private String singleReplace(String inputString, final String oldString,
			final String newString) {
		final int propLength = oldString.length();
		final int startPos = inputString.indexOf(oldString);
		if (startPos == -1) {
			return inputString;
		}
		if (startPos == 0) {
			inputString = inputString.substring(propLength);
			inputString = newString + inputString;
		} else {
			inputString = inputString.substring(0, startPos) + newString
					+ inputString.substring(startPos + propLength);
		}
		return inputString;
	}

	private String getShortPropertyName(final String longPropertyName) {
		final String currentProp = longPropertyName.substring(longPropertyName
				.indexOf(PROP_START));
		final String prop = currentProp.substring(0,
				currentProp.indexOf(PROP_END) + 1);
		final String shortProp = prop.substring(PROP_START.length(),
				prop.length() - 1);
		return shortProp;
	}

	/**
	 * Some perl5 characters may occur in the log file format. Escape these
	 * characters to prevent parsing errors.
	 *
	 * @param input
	 * @return string
	 */
	private String replaceMetaChars(String input) {
		// escape backslash first since that character is used to escape the
		// remaining meta chars
		input = input.replaceAll("\\\\", "\\\\\\");

		// don't escape star - it's used as the wildcard
		input = input.replaceAll(Pattern.quote("]"), "\\\\]");
		input = input.replaceAll(Pattern.quote("["), "\\\\[");
		input = input.replaceAll(Pattern.quote("^"), "\\\\^");
		input = input.replaceAll(Pattern.quote("$"), "\\\\$");
		input = input.replaceAll(Pattern.quote("."), "\\\\.");
		input = input.replaceAll(Pattern.quote("|"), "\\\\|");
		input = input.replaceAll(Pattern.quote("?"), "\\\\?");
		input = input.replaceAll(Pattern.quote("+"), "\\\\+");
		input = input.replaceAll(Pattern.quote("("), "\\\\(");
		input = input.replaceAll(Pattern.quote(")"), "\\\\)");
		input = input.replaceAll(Pattern.quote("-"), "\\\\-");
		input = input.replaceAll(Pattern.quote("{"), "\\\\{");
		input = input.replaceAll(Pattern.quote("}"), "\\\\}");
		input = input.replaceAll(Pattern.quote("#"), "\\\\#");
		return input;
	}

	/**
	 * Convert a keyword-to-values map to a LoggingEvent
	 *
	 * @param fieldMap
	 * @param exception
	 *
	 * @return logging event
	 */
	private LoggingEvent convertToEvent(final HashMap<String, String> fieldMap) {
		if (fieldMap == null) {
			return null;
		}

		// a logger must exist at a minimum for the event to be processed
		if (!fieldMap.containsKey(LOGGER)) {
			fieldMap.put(LOGGER, "Unknown");
		}

		Logger logger = null;
		long timeStamp = 0L;
		String level = null;
		String threadName = null;
		Object message = null;
		String ndc = null;
		String className = null;
		String methodName = null;
		String eventFileName = null;
		String lineNumber = null;
		final Hashtable<String, String> properties = new Hashtable<>();

		logger = Logger.getLogger(fieldMap.remove(LOGGER));

		if ((dateFormat != null) && fieldMap.containsKey(TIMESTAMP)) {
			try {
				timeStamp = dateFormat.parse(
						fieldMap.remove(TIMESTAMP)).getTime();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		// use current time if time stamp not parseable
		if (timeStamp == 0L) {
			timeStamp = System.currentTimeMillis();
		}

		message = fieldMap.remove(MESSAGE);
		if (message == null) {
			message = "";
		}

		level = fieldMap.remove(LEVEL);
		Level levelImpl;
		if (level == null) {
			levelImpl = Level.DEBUG;
		} else {
			// first try to resolve against custom level definition map, then
			// fall back to regular levels
			levelImpl = null;
			levelImpl = Level.toLevel(level.trim());
			if (!level.equals(levelImpl.toString())) {
				levelImpl = Level.DEBUG;
				// make sure the text that couldn't match a level is
				// added to the message
				message = level + " " + message;
			}
		}

		threadName = fieldMap.remove(THREAD);

		ndc = fieldMap.remove(NDC);

		className = fieldMap.remove(CLASS);

		methodName = fieldMap.remove(METHOD);

		eventFileName = fieldMap.remove(FILE);

		lineNumber = fieldMap.remove(LINE);

		// all remaining entries in fieldmap are properties
		properties.putAll(fieldMap);

		LocationInfo info = null;

		if ((eventFileName != null) || (className != null)
				|| (methodName != null) || (lineNumber != null)) {
			info = new LocationInfo(eventFileName, className, methodName,
					lineNumber);
		} else {
			info = LocationInfo.NA_LOCATION_INFO;
		}

		final LoggingEvent event = new LoggingEvent(null, logger, timeStamp, levelImpl, message, threadName,
				new ThrowableInformation(emptyException), ndc, info, properties);

		return event;
	}
}
