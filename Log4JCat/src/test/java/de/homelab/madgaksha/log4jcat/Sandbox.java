package de.homelab.madgaksha.log4jcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

import org.apache.commons.io.IOUtils;

public class Sandbox {
	public static void main(final String[] args) throws IOException {
		final Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").get();
		System.out.println("Expected result");
		System.out.println(5192574);
		final ZonedDateTime date = ZonedDateTime.parse("2017-10-21T00:00:00+00:00[UTC]");
		final File file = new File("/tmp/logjp");
		System.out.println("Open file");
		try (final IRandomAccessInput stream = InputFactory.open(file)) {
			System.out.println("Tail");
			final long pos = cat.find(stream, Timestamp.of(date));
			System.out.println(pos);
			System.out.println("Done");
		}

		System.out.println("Read file to RAM");
		final String fileData;
		try (InputStream stream = new FileInputStream(file)) {
			fileData = IOUtils.toString(stream, StandardCharsets.UTF_8);
		}
		try (final IRandomAccessInput stream = InputFactory.open(fileData)) {
			System.out.println("Tail");
			final long pos = cat.find(stream, Timestamp.of(date));
			System.out.println(pos);
			System.out.println("Done");
		}
	}
}