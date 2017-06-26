package de.homelab.madgaksha.log4jcat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.easetech.easytest.annotation.DataLoader;
import org.easetech.easytest.annotation.Param;
import org.easetech.easytest.loader.LoaderType;
import org.easetech.easytest.runner.DataDrivenTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataDrivenTestRunner.class)
@DataLoader(filePaths = {"CyperusTest.xml"}, loaderType=LoaderType.XML, writeData = false)
public class CyperusTest {

	@Test
	public final void testHeadAppender() throws IOException {
		final String pattern = "[%-5p] %d %c - %m%n";
		final String basename = "log.out";
		final ZonedDateTime date = ZonedDateTime.parse("2017-06-26T19:14:02+00:00[UTC]");

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
				try (final InputStream input = Sandbox.class
						.getResourceAsStream("/de/homelab/madgaksha/log4jcat/" + name);
						final OutputStream output = new FileOutputStream(outfile)) {
					IOUtils.copy(input, output);
				}
				name = basename + "." + i;
				outfile = new File(outbase + "." + i);
			}
			while (i <= 5);
			toRemove.add(outfile);
			// Create an appender and try it out.
			final RollingFileAppender appender = new RollingFileAppender(new PatternLayout(pattern), outbase, true);
			appender.setEncoding("UTF-8");
			appender.setMaxBackupIndex(5);
			appender.setMaxFileSize("100KB");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final long t1 = System.currentTimeMillis();
			Cyperus.head(appender, out, Timestamp.of(date));
			final long t2 = System.currentTimeMillis();
			final String result = out.toString("UTF-8");
			System.out.println("Head took " + (t2 - t1) / 1000f + "s.");
			Assert.assertEquals(239029, out.size());
			Assert.assertEquals(
					"[ERROR] 2017-06-26 19:14:01,715 de.homelab.madgaksha.log4jcat.Sandbox - This is a rather great error message for your convenience or pleasure of testing and I would like to say this starts looking like a really good idea and I really like it, although I do see the flaws with this solution albeit I shall refrain from naming them here or you may get confused, so let's call it a day and end this message here.",
					result.substring(result.length() - 410, result.length() - 1));
		}
		finally {
			// Clean up temporary files.
			for (final File file : toRemove)
				file.delete();
		}
	}

	@Test
	public final void testTrimAppender(
			@Param(name="baseName") final String baseName,
			@Param(name="logPath") final String logPath,
			@Param(name="encoding") final String encoding,
			@Param(name="dateStart") final String dateStart,
			@Param(name="dateEnd") final String dateEnd,
			@Param(name="offsetStart") final int offsetStart,
			@Param(name="lengthStart") final int lengthStart,
			@Param(name="offsetEnd") final int offsetEnd,
			@Param(name="lengthEnd") final int lengthEnd,
			@Param(name="expectedLength") final int expectedLength,
			@Param(name="expectedFirst") final String expectedFirst,
			@Param(name="expectedLast") final String expectedLast
			) throws IOException {
		final String pattern = "[%-5p] %d %c - %m%n";
		final ZonedDateTime date = ZonedDateTime.parse(dateStart);
		final ZonedDateTime date2 = ZonedDateTime.parse(dateEnd);

		// Extract the test files.
		final List<File> toRemove = new ArrayList<>();
		File outfile = File.createTempFile("test", ".log");
		final String outbase = outfile.getAbsolutePath();
		String name = baseName;
		int i = 0;
		try {
			do {
				++i;
				toRemove.add(outfile);
				try (
						final InputStream input = Sandbox.class.getResourceAsStream(logPath + name);
						final OutputStream output = new FileOutputStream(outfile)) {
					IOUtils.copy(input, output);
				}
				name = baseName + "." + i;
				outfile = new File(outbase + "." + i);
			} while (i <= 5);
			toRemove.add(outfile);
			// Create an appender and try it out.
			final RollingFileAppender appender = new RollingFileAppender(new PatternLayout(pattern), outbase, true);
			appender.setEncoding(encoding);
			appender.setMaxBackupIndex(5);
			appender.setMaxFileSize("100KB");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final long t1 = System.currentTimeMillis();
			Cyperus.trim(appender, out, Timestamp.of(date), Timestamp.of(date2));
			final long t2 = System.currentTimeMillis();
			final String result = out.toString(encoding);
			System.out.println("Trim took " + (t2 - t1) / 1000f + "s.");
			Assert.assertEquals(expectedLength, out.size());
			Assert.assertEquals(expectedFirst,
					out.toString(encoding).substring(offsetStart, lengthStart));
			Assert.assertEquals(expectedLast,
					result.substring(result.length()-offsetEnd, result.length()-offsetEnd+lengthEnd));
		}
		finally {
			// Clean up temporary files.
			for (final File file : toRemove)
				file.delete();
		}
	}


	@Test
	public final void testTailAppender() throws IOException {
		final String pattern = "[%-5p] %d %c - %m%n";
		final String basename = "log.out";
		final ZonedDateTime date = ZonedDateTime.parse("2017-06-26T19:14:02+00:00[UTC]");

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
				try (final InputStream input = Sandbox.class
						.getResourceAsStream("/de/homelab/madgaksha/log4jcat/" + name);
						final OutputStream output = new FileOutputStream(outfile)) {
					IOUtils.copy(input, output);
				}
				name = basename + "." + i;
				outfile = new File(outbase + "." + i);
			}
			while (i <= 5);
			toRemove.add(outfile);
			// Create an appender and try it out.
			final RollingFileAppender appender = new RollingFileAppender(new PatternLayout(pattern), outbase, true);
			appender.setEncoding("UTF-8");
			appender.setMaxBackupIndex(5);
			appender.setMaxFileSize("100KB");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final long t1 = System.currentTimeMillis();
			Cyperus.tail(appender, out, Timestamp.of(date));
			final long t2 = System.currentTimeMillis();
			System.out.println("Tail took " + (t2 - t1) / 1000f + "s.");
			Assert.assertEquals(293970, out.size());
			Assert.assertEquals("[ERROR] 2017-06-26 19:14:02,263 de.homelab.madgaksha.log4jcat.Sandbox",
					out.toString("UTF-8").substring(0, 69));
		}
		finally {
			// Clean up temporary files.
			for (final File file : toRemove)
				file.delete();
		}
	}

}
