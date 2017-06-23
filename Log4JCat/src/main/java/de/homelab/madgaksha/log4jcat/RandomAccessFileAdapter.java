package de.homelab.madgaksha.log4jcat;

import java.io.IOException;
import java.io.RandomAccessFile;

class RandomAccessFileAdapter extends ARandomAccessInputStream {
	private final RandomAccessFile raf;

	public RandomAccessFileAdapter(final RandomAccessFile raf) {
		this.raf = raf;
	}

	@Override
	public void seek(final long pos) throws IOException {
		raf.seek(pos);
	}

	@Override
	public long tell() throws IOException {
		return raf.getFilePointer();
	}

	@Override
	public String readLine() throws IOException {
		return raf.readLine();
	}

	@Override
	public long length() throws IOException {
		return raf.length();
	}

	@Override
	public void close() throws IOException {
		raf.close();
	}
}