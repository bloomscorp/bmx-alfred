package com.bloomscorp.alfred.support;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for generating standardized reporter identifiers used in log entries.
 *
 * <p>Reporter identifiers follow the convention {@code "ClassName#methodName"} and are embedded
 * in {@link com.bloomscorp.alfred.orm.LogReporter} or
 * {@link com.bloomscorp.alfred.orm.UnauthorizedLogReporter} payloads to pinpoint the exact
 * code location that produced a log entry. This format is inspired by Java method references
 * and makes log entries self-describing without requiring additional context.
 *
 * @see com.bloomscorp.alfred.orm.LogReporter
 * @see com.bloomscorp.alfred.orm.UnauthorizedLogReporter
 * @see com.bloomscorp.alfred.LogBook#prepareLogReporter
 * @see com.bloomscorp.alfred.LogBook#prepareUnauthorizedLogReporter
 */
public final class ReporterID {

	private ReporterID() {}

	/**
	 * Constructs a reporter identifier by combining a class name and method name with {@code #} as separator.
	 *
	 * <p>The resulting string follows the pattern {@code "ClassName#methodName"}, for example
	 * {@code "UserService#createUser"}. This identifier is intended to be passed to
	 * {@link com.bloomscorp.alfred.LogBook#prepareLogReporter} or
	 * {@link com.bloomscorp.alfred.LogBook#prepareUnauthorizedLogReporter}.
	 *
	 * @param className  the simple or fully qualified name of the class producing the log; must not be null
	 * @param methodName the name of the method producing the log; must not be null
	 * @return a non-null string in the format {@code "className#methodName"}
	 */
	@Contract(pure = true)
	public static @NotNull String prepareID(String className, String methodName) {
		return className + "#" + methodName;
	}
}
