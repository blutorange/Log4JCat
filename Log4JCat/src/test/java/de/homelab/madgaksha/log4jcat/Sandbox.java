package de.homelab.madgaksha.log4jcat;


import java.io.IOException;
import java.util.Date;

import de.homelab.madgaksha.log4jcat.IRandomAccessInputStream;
import de.homelab.madgaksha.log4jcat.Log4JCat;
import de.homelab.madgaksha.log4jcat.StreamFactory;

public class Sandbox {
	public static void main(final String[] args) throws IOException {
		final Log4JCat cat = new Log4JCat("[%-5p] %d %c - %m%n");
		try (final IRandomAccessInputStream stream = StreamFactory.open(Sandbox.class.getResourceAsStream("/logfile001"))) {
//		try (final IRandomAccessInputStream stream = StreamFactory.open(new RandomAccessFile(new File("/tmp/logfile001.log"), "r"))) {
			final long pos = cat.tail(stream, new Date());
			System.out.println(pos);
			stream.seek(pos);
			System.out.print(stream.readLines());
		}
	}
}