package de.homelab.madgaksha.log4jcat;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Date;

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
	public final void testTail(
			@Param(name="patternLayout") final String patternLayout,
			@Param(name="logFilePath") final String logFilePath,
			@Param(name="date") final String dateString,
			@Param(name="shouldPosition") final long shouldPosition) throws IOException, ParseException {
		final ZonedDateTime dateTime = ZonedDateTime.parse(dateString);
		final Log4JCatImpl cat = Log4JCat.of(patternLayout).get();
		final long t1,t2;
		try (final IRandomAccessInputStream stream = StreamFactory.open(Log4JCatTest.class.getResourceAsStream(logFilePath))) {
			t1 = new Date().getTime();
			final long isPosition = cat.tail(stream, dateTime);
			t2 = new Date().getTime();
			Assert.assertEquals(shouldPosition, isPosition);
		}
		LOG.info("Tail took " + (t2-t1)/1000f + "s.");
	}
}
