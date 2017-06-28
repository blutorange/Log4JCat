package com.github.blutorange.log4jcat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.easetech.easytest.annotation.DataLoader;
import org.easetech.easytest.annotation.Param;
import org.easetech.easytest.loader.LoaderType;
import org.easetech.easytest.runner.DataDrivenTestRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(DataDrivenTestRunner.class)
@DataLoader(filePaths = {"Log4JCatTest.xml"}, loaderType=LoaderType.XML, writeData = false)
public class Log4JCatTest {

	private final Logger LOG = LoggerFactory.getLogger(Log4JCatTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// Nothing to setup for now.
	}

	@Before
	public void setUp() throws Exception {
		// Nothing to setup for now.
	}

	@After
	public void tearDown() throws Exception {
		// Nothing to setup for now.
	}

	@Test
	public final void testEach(
			@Param(name="patternLayout") final String patternLayout,
			@Param(name="logFilePath") final String logFilePath,
			@Param(name="encoding") final String encoding,
			@Param(name="shouldEntries") final long shouldEntries) throws IOException {
		final Log4JCat cat = Log4J.of(patternLayout).get();
		final Charset charset = Charset.forName(encoding != null ? encoding : "UTF-8");
		final AtomicInteger counter = new AtomicInteger(0);

		long t1,t2;
		try (final IRandomAccessInput stream = InputFactory.open(Log4JCatTest.class.getResourceAsStream(logFilePath), charset)) {
			t1 = new Date().getTime();
			cat.each(stream, event -> counter.incrementAndGet() >= 1);
			t2 = new Date().getTime();
			Assert.assertEquals(shouldEntries, counter.get());
		}
		LOG.info("Each took " + (t2-t1)/1000f + "s.");
	}


	@Test
	public final void testFind(
			@Param(name="patternLayout") final String patternLayout,
			@Param(name="logFilePath") final String logFilePath,
			@Param(name="date") final String dateString,
			@Param(name="encoding") final String encoding,
			@Param(name="shouldPosition") final long shouldPosition,
			@Param(name="shouldPositionString") final long shouldPositionString) throws IOException {

		final ZonedDateTime dateTime = ZonedDateTime.parse(dateString);
		final Log4JCat cat = Log4J.of(patternLayout).get();
		final Charset charset = Charset.forName(encoding != null ? encoding : "UTF-8");

		long t1,t2;
		try (final IRandomAccessInput stream = InputFactory.open(Log4JCatTest.class.getResourceAsStream(logFilePath), charset)) {
			t1 = new Date().getTime();
			final long isPosition = cat.find(stream, Timestamp.of(dateTime));
			t2 = new Date().getTime();
			Assert.assertEquals(shouldPositionString, isPosition);
		}
		Assert.assertTrue(t2-t1<1000f);
		LOG.info("RAM find took " + (t2-t1)/1000f + "s.");

		final File temp = File.createTempFile("Log4JCatTest", ".log");
		try (final OutputStream output = new FileOutputStream(temp);
			final InputStream input = Log4JCatTest.class.getResourceAsStream(logFilePath)) {
			IOUtils.copy(input, output);
		}
		catch (final IOException e) {
			temp.delete();
			throw e;
		}
		try (final IRandomAccessInput stream = InputFactory.open(temp, charset)) {
			t1 = new Date().getTime();
			final long isPosition = cat.find(stream, Timestamp.of(dateTime));
			t2 = new Date().getTime();
			Assert.assertEquals(shouldPosition, isPosition);
		}
		finally {
			temp.delete();
		}
		Assert.assertTrue(t2-t1<1000f);
		LOG.info("File find took " + (t2-t1)/1000f + "s.");
	}
}
