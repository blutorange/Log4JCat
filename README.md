# Log4JCat

## Overview

Sometimes you've only got the log files and want or need to trim them, eg. to the last 2 hours
programatically. Not a well defined task, but we can have a pretty good guess (unless the log
format is plain weird.) As log files can be pretty large, up to several GB in some cases, it
would take a long time to scan them line-by-line. However, since all entries within a log file
are sorted chronologically from oldest to newest, we can perform a quick binary search.

This is what this does in a nutshell, for LOG4J log files with its log entries formatted
according to a certain specified [Pattern Layout](https://logging.apache.org/log4j/1.2/apidocs/org/apache/log4j/PatternLayout.html).

Example:

```java
public class Sandbox {
  public static void main(final String[] args) throws IOException {
    // Create a new log file trimmer with a specified log entry format.
    // Log files with this format look like this
    //   [DEBUG] 2017-06-22 21:57:53,661 TimeClass - Past
    //   [INFO ] 2017-06-23 21:57:53,661 TimeClass - Present
    //   [WARN ] 2017-06-24 21:57:53,661 TimeClass - Future
    final Log4JCat cat = Log4JCat.of("[%-5p] %d %c - %m%n").get();
    // Open a stream for the log file.
    try (final IRandomAccessInputStream stream = StreamFactory.open(new File("~/mylogfile"))) {
      // Trim the log file so that it contains only entries more recent than the current date.
      // This returns the offset in the file where the entries start.
      final long pos = cat.tail(stream, new Date());
      // Seek to the starting position.
      stream.seek(pos);
      // And print the all lines from the starting position.
      System.out.print(stream.readLines());
    }
  }
}
```

## Documentation

### Trimming

The following methods are available for log file trimming, provided by
Log4JCatImpl:

* long tail(IRandomAccessInputStream, long|Date|TemporalAccessor)
* long head(IRandomAccessInputStream, long|Date|TemporalAccessor) not yet implemented
* more to come?

### Configuration

At the least, you need to pass a pattern layout to get a builder for
configuration.

* Log4JCat.of(String patternLayout) Returns a new builder.
* Log4JCat.get() Builds a log file trimmer instance and returns it.


There are more options available you can set via the builder before creating
a log file trimmer instance:

* locale(Locale) The locale used when the pattern layout contains abbreviations such as "Jan" or "Monday". Default to Locale.ENGLISH.
* timeZone(TimeZone) The time zone of the dates in the log file. Defaults to UTC.
* threshold(long) In bytes. When the search range has been narrowed down to this threshold, the log file trimmer switches to a linear search algorithm. Defaults to 10000.

### Input

To pass the log file, use the StreamFactory, which allows you to use both
streams, strings, and files as input. They all return an object you can pass
to the log file trimming methods provided by Log4JCat.

The following factory methods are available:

* StreamFactory.open(File)
* StreamFactory.open(Path)
* StreamFactory.open(RandomAccessFile)
* StreamFactory.open(String) The string represents the log file content.
* StreamFactory.open(InputStream[, Charset|String])
* StreamFactory.open(Reader)

Please note that the last two methods must read the entire stream into memory
and are not suitable for large streams.

All factory methods allow null and return an object representing an empty log
file in this case.
