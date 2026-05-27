package com.bloomscorp.alfred.cron.task;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.LOG_TYPE;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

/**
 * Runnable task that persists a structured log entry via {@link LogBook} using an API.
 *
 * <p>Instances of this class are created and started by {@link com.bloomscorp.alfred.cron.CronManager}
 * to perform general log persistence asynchronously via an API call. The task delegates to
 * {@link LogBook#log(String, String, LOG_TYPE, String, String, String, String)}.
 *
 * @param <B> the concrete {@link LogBook} implementation
 * @param <L> the concrete {@link Log} entity type
 * @param <A> the concrete {@link AuthenticationLog} entity type
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 *
 * @see com.bloomscorp.alfred.cron.CronManager
 * @see LogBook#log(String, String, LOG_TYPE, String, String, String, String)
 */
@AllArgsConstructor
public final class ApiLoggerTask<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements Runnable {

	/** The human-readable log message. Must not be null. */
	private final String message;

	/** The identifier of the component producing the log, conventionally {@code "ClassName#methodName"}. */
	private final String logger;

	/** The severity or category of this log entry. Must not be null. */
	private final LOG_TYPE type;

	/** Optional serialized context data (e.g., JSON, request body); may be null or empty. */
	private final String dataDump;

	/** The API key used for authentication with the Alfred API. */
	private final String apiKey;

	/** The API secret used for signing the request. */
	private final String apiSecret;

	/** The project identifier. */
	private final String projectId;

	/** The log book to which the log entry is delegated. Must not be null. */
	private final B logBook;

	/**
	 * Executes the log persistence operation by delegating to
	 * {@link LogBook#log(String, String, LOG_TYPE, String, String, String, String)}.
	 */
	@Override
	public void run() {
		this.logBook.log(
			this.message,
			this.logger,
			this.type,
			this.dataDump,
			this.apiKey,
			this.apiSecret,
			this.projectId
		);
	}
}
