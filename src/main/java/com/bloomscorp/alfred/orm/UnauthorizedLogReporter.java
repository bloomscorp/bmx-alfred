package com.bloomscorp.alfred.orm;

import lombok.AllArgsConstructor;

/**
 * POJO representing the reporter context for a log entry originating from an unauthenticated or unauthorized request.
 *
 * <p>Instances are serialized to JSON by
 * {@link com.bloomscorp.alfred.LogBook#prepareUnauthorizedLogReporter} and embedded in log entries
 * where no valid user session exists. The {@code user} field is always set to the literal string
 * {@code "unauthorized"} to signal the absence of an authenticated principal.
 *
 * @see com.bloomscorp.alfred.LogBook#prepareUnauthorizedLogReporter
 * @see LogReporter
 */
@AllArgsConstructor
public final class UnauthorizedLogReporter {

	/**
	 * A fixed string identifying the request origin as unauthorized.
	 * Always set to {@code "unauthorized"} by the library.
	 */
	public String user;

	/**
	 * The identifier of the code location that produced the log, conventionally formatted as
	 * {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID#prepareID}.
	 */
	public String reporterID;
}
