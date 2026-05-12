package com.bloomscorp.alfred.orm;

import lombok.AllArgsConstructor;

/**
 * POJO representing the reporter context for a log entry originating from an authenticated user.
 *
 * <p>Instances are serialized to JSON by {@link com.bloomscorp.alfred.LogBook#prepareLogReporter}
 * and embedded in log entries to identify both the authenticated user who triggered the log and
 * the specific code location that produced it.
 *
 * <p>Fields annotated with {@link com.bloomscorp.alfred.configuration.GsonExclude} are omitted
 * from the JSON output. Neither field on this class is excluded by default.
 *
 * @see com.bloomscorp.alfred.LogBook#prepareLogReporter
 * @see UnauthorizedLogReporter
 */
@AllArgsConstructor
public final class LogReporter {

	/** The internal identifier of the authenticated user who triggered the log entry. */
	public long userID;

	/**
	 * The identifier of the code location that produced the log, conventionally formatted as
	 * {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID#prepareID}.
	 */
	public String reporterID;
}
