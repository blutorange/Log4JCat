package de.homelab.madgaksha.log4jcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;

public class Sandbox {
	public static void main(final String[] args) throws IOException {

		final Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").get();
		System.out.println("Expected result");
		System.out.println(6308028);

		final ZonedDateTime date = ZonedDateTime.parse("2017-10-21T00:00:00+00:00[UTC]");
		final File file = new File("/tmp/utf16.log");
		final String encoding = "UTF-16";

		System.out.println("==Open file==");
		try (final IRandomAccessInput stream = InputFactory.open(file, encoding)) {
			System.out.println("Tail");
			final long pos = cat.find(stream, Timestamp.of(date));
			System.out.println(pos);
			stream.seek(pos);
			System.out.println(stream.readLine());
			System.out.println("Done");
		}

		System.out.println("==Read file to RAM==");
		final String fileData;
		try (InputStream stream = new FileInputStream(file)) {
			fileData = IOUtils.toString(stream, encoding);
		}
		try (final IRandomAccessInput stream = InputFactory.open(fileData)) {
			System.out.println("Tail");
			final long pos = cat.find(stream, Timestamp.of(date));
			System.out.println(pos);
			stream.seek(pos);
			System.out.println(stream.readLine());
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

	static class Counter {
		int n;
		void inc(){++n;}
		int count(){return n;}
	}
}