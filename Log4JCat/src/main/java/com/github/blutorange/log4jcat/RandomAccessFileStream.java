package com.github.blutorange.log4jcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

class RandomAccessFileStream extends InputStream {
	final RandomAccessFile file;
	private long mark;

	public RandomAccessFileStream(final RandomAccessFile raf) {
		this.file = raf;
	}

	public RandomAccessFile getFile() {
		return file;
	}

	@Override
	public int read() throws IOException {
		return file.read();
	}

    @Override
	public int read(final byte[] b) throws IOException {
        return file.read(b);
    }

    @Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
    	return file.read(b, off, len);
    }

    @Override
	public boolean markSupported() {
        return true;
    }

    @Override
	public synchronized void mark(final int readlimit) {
    	try {
			this.mark = file.getFilePointer();
		}
		catch (final IOException e) {
			e.printStackTrace(System.err);
		}
    }

    @Override
	public synchronized void reset() throws IOException {
        file.seek(mark);
    }

    @Override
	public int available() throws IOException {
        return (int)(file.length()-file.getFilePointer());
    }

    @Override
	public void close() throws IOException {
		file.close();
	}
}