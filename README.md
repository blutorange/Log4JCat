# Log4JCat

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
    final Log4JCat cat = new Log4JCat("[%-5p] %d %c - %m%n");
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

