package de.homelab.madgaksha.log4jcat;

import java.io.IOException;
import java.time.ZonedDateTime;

public class Sandbox {
	public static void main(final String[] args) throws IOException {
		final Log4JCat cat = Log4J.of("[%-5p] %d %c - %m%n").get();
		final ZonedDateTime date = ZonedDateTime.parse("2017-12-26T13:00:00+00:00[UTC]");
//		System.out.println("Open stream");
//		try (final IRandomAccessInputStream stream = InputFactory.open(new File("/tmp/huge"))) {
//			System.out.println("Tail");
//			final long pos = cat.tail(stream, date);
//			System.out.println("Done");
//			System.out.println(pos);
//		}

		try (final IRandomAccessInput stream = InputFactory.open(Sandbox.class.getResourceAsStream("/logfile001"))) {
			final long pos = cat.tail(stream, date);
			System.out.println(pos);
		}
	}
}