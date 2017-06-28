package com.github.blutorange.log4jcat;

import org.eclipse.jdt.annotation.NonNull;

interface ILogReaderFactory {
	@NonNull
	public ILogReader create();
}