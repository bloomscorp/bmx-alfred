package com.bloomscorp.alfred.support;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class ReporterID {

	private ReporterID() {}

	@Contract(pure = true)
	public static @NotNull String prepareID(String className, String methodName) {
		return className + "#" + methodName;
	}
}
