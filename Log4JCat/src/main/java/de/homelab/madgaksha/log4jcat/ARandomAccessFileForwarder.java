package de.homelab.madgaksha.log4jcat;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;


abstract class ARandomAccessFileForwarder extends ARandomAccessInput {
	private final static Map<String, BiFunction<RandomAccessFile, Charset, IRandomAccessInput>> map = new HashMap<>();
	private final static BiFunction<RandomAccessFile, Charset, IRandomAccessInput> SINGLE = (raf,
			charset) -> new R_SingleByte(raf, charset, null, false);
	private final static BiFunction<RandomAccessFile, Charset, IRandomAccessInput> MULTI_2 = (raf,
			charset) -> new R_ConstantMultiByte(raf, charset, null, false, 2);
	private final static BiFunction<RandomAccessFile, Charset, IRandomAccessInput> MULTI_4 = (raf,
			charset) -> new R_ConstantMultiByte(raf, charset, null, false, 4);
	private final static BiFunction<RandomAccessFile, Charset, IRandomAccessInput> UTF_16_BOM = (raf,
			charset) -> new R_ConstantMultiByte(raf, charset, new byte[]{(byte)0xFF,(byte)0xFE}, true, 2);
	private final static BiFunction<RandomAccessFile, Charset, IRandomAccessInput> UTF_32_BOM = (raf,
			charset) -> new R_ConstantMultiByte(raf, charset, new byte[]{(byte)0xFF,(byte)0xFE,0,0}, false, 4);

	static {
		charset(StandardCharsets.UTF_8, (raf, charset) -> new R_UTF8(raf, charset, null, false));

		charset(StandardCharsets.US_ASCII, (raf, charset) -> new R_ASCII(raf));
		charset(StandardCharsets.ISO_8859_1, SINGLE);
		charset("JIS_X0201", SINGLE);
		charset("JIS_X0201", SINGLE);

		charset("x-windows-874", SINGLE);
		charset("MS874", SINGLE);
		charset("x-windows-932", SINGLE);
		charset("MS932", MULTI_2);
		charset("x-windows-936", MULTI_2);
		charset("MS936", MULTI_2);
		charset("x-windows-949", MULTI_2);
		charset("MS949", MULTI_2);
		charset("x-windows-950", MULTI_2);
		charset("MS950", MULTI_2);

		charset("windows-1250", SINGLE);
		charset("Cp1250", SINGLE);
		charset("windows-1251", SINGLE);
		charset("Cp1251", SINGLE);
		charset("windows-1252", SINGLE);
		charset("Cp1252", SINGLE);
		charset("windows-1253", SINGLE);
		charset("Cp1253", SINGLE);
		charset("windows-1254", SINGLE);
		charset("Cp1254", SINGLE);
		charset("windows-1255", SINGLE);
		charset("Cp1255", SINGLE);
		charset("windows-1256", SINGLE);
		charset("Cp1256", SINGLE);
		charset("windows-1257", SINGLE);
		charset("Cp1257", SINGLE);
		charset("windows-1258", SINGLE);
		charset("Cp1258", SINGLE);

		charset("UTF-16", UTF_16_BOM);
		charset("x-UTF-16LE-BOM", UTF_16_BOM);
		charset("UTF-16BE", MULTI_2);
		charset("UTF_16LE", MULTI_2);
		charset("UnicodeBigUnmarked", MULTI_2);
		charset("UnicodeLittleUnmarked", MULTI_2);

		charset("Big5", MULTI_2);

		charset("UTF-32", MULTI_4);
		charset("X-UTF-32BE-BOM", UTF_32_BOM);
		charset("X-UTF-32LE-BOM", UTF_32_BOM);
		charset("UTF-16BE", MULTI_4);
		charset("UTF_16LE", MULTI_4);
	}

	private static void charset(final Charset charset,
			final BiFunction<RandomAccessFile, Charset, IRandomAccessInput> factory) {
		map.put(charset.name().toLowerCase(Locale.ROOT), factory);
	}

	private static void charset(final String charset,
			final BiFunction<RandomAccessFile, Charset, IRandomAccessInput> factory) {
		map.put(charset.toLowerCase(Locale.ROOT), factory);
	}

	protected final RandomAccessFile file;

	protected ARandomAccessFileForwarder(final RandomAccessFile raf) {
		this.file = raf;
	}

	@Override
	public void seek(final long pos) throws IOException {
		file.seek(pos);
	}

	@Override
	public long tell() throws IOException {
		return file.getFilePointer();
	}

	@Override
	public String readLine() throws IOException {
		seekToNextCodepoint();
		return readEncodedLine();
	}

	@Override
	public long length() throws IOException {
		return file.length();
	}

	@Override
	public void close() throws IOException {
		file.close();
	}

	protected abstract void seekToNextCodepoint() throws IOException;

	protected abstract String readEncodedLine() throws IOException;

	/**
	 *
	 * @param raf
	 * @param charset
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static IRandomAccessInput of(final RandomAccessFile raf, final Charset charset)
			throws UnsupportedEncodingException {
		final BiFunction<RandomAccessFile, Charset, IRandomAccessInput> producer = map
				.get(charset.name().toLowerCase(Locale.ROOT));
		if (producer == null) {
			throw new UnsupportedEncodingException(String.format("The charset %s is not supported yet.", charset));
		}
		return producer.apply(raf, charset);
	}

	private final static class R_ASCII extends ARandomAccessFileForwarder {
		public R_ASCII(final RandomAccessFile raf) {
			super(raf);
		}

		@Override
		protected void seekToNextCodepoint() {
			// Single byte.
		}

		@Override
		protected String readEncodedLine() throws IOException {
			return file.readLine();
		}
	}

	private abstract static class R_CharsetAware extends ARandomAccessFileForwarder {
		protected final byte[] abuffer;
		protected final ByteBuffer buffer;
		protected final CharBuffer cbuffer;
		protected final CharsetDecoder decoder;
		protected final Charset charset;
		private final byte[] bom;
		boolean encBOM;

		public R_CharsetAware(final RandomAccessFile raf, final Charset charset, final byte[] bom, final boolean encBOM) {
			this(raf, charset, bom, encBOM, 128);
		}

		public R_CharsetAware(final RandomAccessFile raf, final Charset charset, final byte[] bom, final boolean encBOM, final int bufferSize) {
			super(raf);
			this.charset = charset;
			this.bom = bom;
			this.encBOM = encBOM;
			abuffer = new byte[bufferSize];
			buffer = ByteBuffer.allocate(bufferSize);
			cbuffer = CharBuffer.allocate(bufferSize);
			cbuffer.position(bufferSize);
			decoder = charset.newDecoder();
			decoder.onMalformedInput(CodingErrorAction.IGNORE);
			decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
		}

		@Override
		public void close() throws IOException {
			super.close();
		}

		@Override
		protected final String readEncodedLine() throws IOException {
			String eolChar = "";
			final long initial = file.getFilePointer();
			if (initial >= file.length())
				return null;
			final StringBuilder sb = new StringBuilder();
			int c = -1;
			boolean eol = false;
			while (!eol) {
				if (!cbuffer.hasRemaining()) {
					// Seek the file to the current position.
					final long bytesProcessed = sb.length() == 0 ? 0 : charset.encode(sb.toString()).remaining() - (encBOM?bom.length:0);
					file.seek(initial+bytesProcessed);
					// Read data into the buffer.
					final int read = file.read(abuffer);
					if (read == -1) {
						eol = true;
						continue;
					}
					buffer.clear();
					buffer.position(abuffer.length-read);
					buffer.put(abuffer, 0, read);
					buffer.position(abuffer.length-read);
					// Decode the data into characters.
					cbuffer.clear();
//					decoder.reset();
					Arrays.fill(cbuffer.array(), (char)-2);
					decoder.decode(buffer, cbuffer, true);
					cbuffer.position(0);
				}
				switch (c = cbuffer.get()) {
				case (char)-2:
					cbuffer.position(abuffer.length);
					break;
				case (char)-1:
					eol = true;
					break;
				case '\n':
					eol = true;
					eolChar = "\n";
					break;
				case '\r':
					eol = true;
					if (cbuffer.get() != '\n') {
						eolChar = "\r";
					}
					else {
						eolChar = "\r\n";
					}
					break;
				default:
					sb.append((char) c);
					break;
				}
			}

			if ((c == -1) && (sb.length() == 0)) {
				return null;
			}

			final String string = sb.toString();
			final int eolBytes = eolChar.isEmpty() ? 0
					: (charset.encode(eolChar).remaining() - (encBOM ? bom.length : 0));
			final int readBytes = string.isEmpty() ? 0
					: (charset.encode(string).remaining() - (encBOM ? bom.length : 0));
			file.seek(initial + readBytes + eolBytes);
			cbuffer.position(abuffer.length);
			return string;
		}

		protected void skipBOM() throws IOException {
			if (bom != null && file.getFilePointer() == 0) {
				final byte[] start = new byte[bom.length];
				if (file.read(start) == start.length) {
					if (Arrays.equals(start, bom)) {
						final ByteBuffer bb = ByteBuffer.wrap(bom);
						cbuffer.clear();
						decoder.decode(bb, cbuffer, false);
						cbuffer.position(abuffer.length);
					}
					else {
						file.seek(0);
					}
				}
				else {
					file.seek(0);
				}
			}
		}
	}

	private static class R_SingleByte extends R_CharsetAware {
		public R_SingleByte(final RandomAccessFile raf, final Charset charset, final byte[] bom,final boolean encBOM) {
			super(raf, charset, bom, encBOM);
		}

		@Override
		protected void seekToNextCodepoint() throws IOException {
			skipBOM();
		}
	}

	private static class R_ConstantMultiByte extends R_CharsetAware {
		private final long count;

		public R_ConstantMultiByte(final RandomAccessFile raf, final Charset charset, final byte[] bom, final boolean encBOM, final long count) {
			super(raf, charset, bom, encBOM);
			this.count = count;
		}

		@Override
		protected void seekToNextCodepoint() throws IOException {
			final long offset = tell() % count;
			if (offset != 0) {
				seek(tell() + count - offset);
			}
			skipBOM();
		}
	}

	private static class R_UTF8 extends R_CharsetAware {
		public R_UTF8(final RandomAccessFile raf, final Charset charset, final byte[] bom, final boolean encBOM) {
			super(raf, charset, bom, encBOM);
		}

		@Override
		protected void seekToNextCodepoint() throws IOException {
			int b;
			long pos;
			do {
				pos = file.getFilePointer();
				b = file.read();
			}
			while (b != -1 && (b & 0b10000000) != 0);
			file.seek(pos);
			skipBOM();
		}
	}

}