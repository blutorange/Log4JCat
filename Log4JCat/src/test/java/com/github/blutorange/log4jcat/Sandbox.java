package com.github.blutorange.log4jcat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sandbox {
	public static void main(final String[] args) throws Exception {
		//multi();
		System.out.println(test(".478-"));
		System.exit(0);
	}

	static float test(final String s) {
		boolean dot = false;
		final long len = s.length();
		try {
			for (int i = 0 ;  i < len; ++i) {
				switch (s.charAt(i)) {
				case '.':
					if (dot)
						return Float.parseFloat(s.substring(0, i));
					dot = true;
					break;
				case '-':
					return Float.parseFloat(s.substring(0, i));
				}
			}
			return Float.parseFloat(s);
		}
		catch (final NumberFormatException ignored) {
			return Float.NaN;
		}
	}

	static void multi() throws IOException {
		final String pattern = "[%-5p] %d %c - %m%n";
		final String basename = "log.out";
		final ZonedDateTime date = ZonedDateTime.parse("2017-06-26T19:14:34+00:00[UTC]");
		final ZonedDateTime date2 = ZonedDateTime.parse("2017-06-26T19:14:33+00:00[UTC]");

		// Extract the test files.
		final List<File> toRemove = new ArrayList<>();
		File outfile = File.createTempFile("test", ".log");
		final String outbase = outfile.getAbsolutePath();
		String name = basename;
		int i = 0;
		try {
			do {
				++i;
				toRemove.add(outfile);
				try (
						final InputStream input = Sandbox.class.getResourceAsStream("/com/github/blutorange/log4jcat/" + name);
						final OutputStream output = new FileOutputStream(outfile)) {
					IOUtils.copy(input, output);
				}
				name = basename + "." + i;
				outfile = new File(outbase + "." + i);
			} while (i <= 5);
			toRemove.add(outfile);
			// Create an appender and try it out.
			final RollingFileAppender appender = new RollingFileAppender(new PatternLayout(pattern), outbase, true);
			appender.setEncoding("UTF-8");
			appender.setMaxBackupIndex(5);
			appender.setMaxFileSize("100KB");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			Cyperus.trim(appender, out, Timestamp.of(date), Timestamp.of(date2));
			final String result = out.toString("UTF-8");
			System.out.println(result);
//			System.out.println(result.substring(0, 69));
			System.out.println(out.size());
//			System.out.println(result.substring(result.length()-410, result.length()-410+69));
		}
		finally {
			// Clean up temporary files.
			for (final File file : toRemove)
				file.delete();
		}
	}

	static void single() throws IOException {
		final Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").get();
		System.out.println("Expected result");
		System.out.println(6308028);

		final ZonedDateTime date = ZonedDateTime.parse("2017-10-21T00:00:00+00:00[UTC]");
		final File file = new File("/tmp/large.log");
		final String encoding = "UTF-8";

		System.out.println("==Open file==");
		try (final IRandomAccessInput stream = InputFactory.open(file, encoding)) {
			System.out.println("Tail");
			final long t1 = System.currentTimeMillis();
			final long pos = cat.find(stream, Timestamp.of(date));
			final long t2 = System.currentTimeMillis();
			System.out.println(pos);
			stream.seek(pos);
			System.out.println(stream.readLine());
			System.out.println("Took " + (t2-t1)/1000f + "s.");
			System.out.println("Done");
		}

		System.out.println("==Read file to RAM==");
		final String fileData;
		try (InputStream stream = new FileInputStream(file)) {
			fileData = IOUtils.toString(stream, encoding);
		}
		try (final IRandomAccessInput stream = InputFactory.open(fileData)) {
			System.out.println("Tail");
			final long t1 = System.currentTimeMillis();
			final long pos = cat.find(stream, Timestamp.of(date));
			final long t2 = System.currentTimeMillis();
			System.out.println(pos);
			stream.seek(pos);
			System.out.println(stream.readLine());
			System.out.println("Took " + (t2-t1)/1000f + "s.");
			System.out.println("Done");
		}

		System.out.println("==Iteration==");
		try (final IRandomAccessInput stream = InputFactory.open(file, encoding)) {
			System.out.println("Each");
			final long t1 = System.currentTimeMillis();
			final Counter counter = new Counter();
			cat.each(stream, event -> {counter.inc();return true;});
			final long t2 = System.currentTimeMillis();
			System.out.println(counter.count() + " entries.");
			System.out.println("Took " + (t2-t1)/1000f + "s.");
			System.out.println("Done");
		}
	}

	static void log() throws InterruptedException {
		final Logger logger = LoggerFactory.getLogger(Sandbox.class);
		for (int i = 1300; i --> 0;) {
			Thread.sleep((int)(new Random().nextFloat()*500+50));
			logger.error("This is a rather great error message for your convenience or pleasure of testing and I would like to say this starts looking like a really good idea and I really like it, although I do see the flaws with this solution albeit I shall refrain from naming them here or you may get confused, so let's call it a day and end this message here.");
		}
	}

	static class Counter {
		int n;
		void inc(){++n;}
		int count(){return n;}
	}
}