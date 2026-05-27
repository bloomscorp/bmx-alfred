package com.bloomscorp.alfred.cron;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.cron.task.ApiExceptionLoggerTask;
import com.bloomscorp.alfred.cron.task.ApiLoggerTask;
import com.bloomscorp.alfred.cron.task.AuthenticationLoggerTask;
import com.bloomscorp.alfred.cron.task.ExceptionLoggerTask;
import com.bloomscorp.alfred.cron.task.LoggerTask;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.LOG_TYPE;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

/**
 * Dispatches logging tasks asynchronously by spawning a dedicated thread per log operation.
 *
 * <p>This class acts as the non-blocking entry point for all Alfred logging calls. Each
 * {@code schedule*} method wraps the corresponding {@link LogBook} operation in a
 * {@link Runnable} task and starts a new {@link Thread}, ensuring that log persistence
 * does not block the calling thread.
 *
 * <p>This approach is suited for high-throughput request paths where synchronous I/O to
 * a log store would introduce unacceptable latency. Be aware that threads are not pooled;
 * in very high-frequency environments, consider wrapping log calls with a thread pool
 * or queue-based approach.
 *
 * @param <B> the concrete {@link LogBook} implementation
 * @param <L> the concrete {@link Log} entity type
 * @param <A> the concrete {@link AuthenticationLog} entity type
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 *
 * @see LogBook
 * @see AuthenticationLoggerTask
 * @see LoggerTask
 * @see ExceptionLoggerTask
 * @see ApiLoggerTask
 * @see ApiExceptionLoggerTask
 */
@AllArgsConstructor
public class CronManager<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	/**
	 * The {@link LogBook} instance to which all logging tasks are delegated.
	 * Must not be null.
	 */
	private final B logBook;

	/** The API key used for authentication with the Alfred API. */
	private final String apiKey;

	/** The API secret used for signing the request. */
	private final String apiSecret;

	/** The project identifier. */
	private final String projectId;

	/**
	 * Constructs a new CronManager with only a log book.
	 * Default API logging will not be available.
	 *
	 * @param logBook the log book to use
	 */
	public CronManager(B logBook) {
		this.logBook = logBook;
		this.apiKey = null;
		this.apiSecret = null;
		this.projectId = null;
	}

	/**
	 * Asynchronously records a login event for the given user.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#logLogin} on the underlying log book.
	 *
	 * @param user the tenant who logged in; must not be null
	 */
	public void scheduleLoginLogTask(T user) {
		new Thread(
			new AuthenticationLoggerTask<>(
				user,
				this.logBook,
				true
			)
		).start();
	}

	/**
	 * Asynchronously records a logout event for the given user.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#logLogout} on the underlying log book.
	 *
	 * @param user the tenant who logged out; must not be null
	 */
	public void scheduleLogoutLogTask(T user) {
		new Thread(
			new AuthenticationLoggerTask<>(
				user,
				this.logBook,
				false
			)
		).start();
	}

	/**
	 * Asynchronously persists a structured log entry with the specified severity and data dump.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#log(String, String, LOG_TYPE, String)}
	 * on the underlying log book. If API credentials were provided during construction,
	 * it will attempt API logging via {@link ApiLoggerTask} first.
	 *
	 * @param message  the human-readable log message; must not be null
	 * @param logger   the identifier of the component producing the log,
	 *                 conventionally {@code "ClassName#methodName"}; must not be null
	 * @param type     the severity/category of the log entry; must not be null
	 * @param dataDump optional serialized context data (JSON, request body, etc.); may be null or empty
	 */
	public void scheduleLogTask(String message, String logger, LOG_TYPE type, String dataDump) {
		if (this.apiKey != null && this.apiSecret != null && this.projectId != null) {
			this.scheduleLogTask(message, logger, type, dataDump, this.apiKey, this.apiSecret, this.projectId);
		} else {
			new Thread(
				new LoggerTask<>(
					message,
					logger,
					type,
					dataDump,
					this.logBook
				)
			).start();
		}
	}

	/**
	 * Asynchronously persists a structured log entry via an API call.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#log(String, String, LOG_TYPE, String, String, String, String)}
	 * on the underlying log book.
	 *
	 * @param message   the human-readable log message; must not be null
	 * @param logger    the identifier of the component producing the log,
	 *                  conventionally {@code "ClassName#methodName"}; must not be null
	 * @param type      the severity/category of the log entry; must not be null
	 * @param dataDump  optional serialized context data (JSON, request body, etc.); may be null or empty
	 * @param apiKey    the API key used for authentication with the Alfred API
	 * @param apiSecret the API secret used for signing the request
	 * @param projectId the project identifier
	 */
	public void scheduleLogTask(String message, String logger, LOG_TYPE type, String dataDump, String apiKey, String apiSecret, String projectId) {
		new Thread(
			new ApiLoggerTask<>(
				message,
				logger,
				type,
				dataDump,
				apiKey,
				apiSecret,
				projectId,
				this.logBook
			)
		).start();
	}

	/**
	 * Asynchronously persists an error log entry for a caught exception including the full stack trace.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#log(Exception, String, String)} on the
	 * underlying log book. The log type is always {@link com.bloomscorp.alfred.orm.LOG_TYPE#ERROR}.
	 * If API credentials were provided during construction, it will attempt API logging via
	 * {@link ApiExceptionLoggerTask} first.
	 *
	 * @param exception the caught exception to record; must not be null
	 * @param message   contextual description of where or why the exception occurred; must not be null
	 * @param logger    the identifier of the component catching the exception,
	 *                  conventionally {@code "ClassName#methodName"}; must not be null
	 */
	public void scheduleExceptionLogTask(Exception exception, String message, String logger) {
		if (this.apiKey != null && this.apiSecret != null && this.projectId != null) {
			this.scheduleExceptionLogTask(exception, message, logger, this.apiKey, this.apiSecret, this.projectId);
		} else {
			new Thread(
				new ExceptionLoggerTask<>(
					exception,
					message,
					logger,
					this.logBook
				)
			).start();
		}
	}

	/**
	 * Asynchronously persists an error log entry for a caught exception via an API call.
	 *
	 * <p>Spawns a new thread that calls {@link LogBook#log(Exception, String, String, String, String, String)}
	 * on the underlying log book. The log type is always {@link com.bloomscorp.alfred.orm.LOG_TYPE#ERROR}.
	 *
	 * @param exception the caught exception to record; must not be null
	 * @param message   contextual description of where or why the exception occurred; must not be null
	 * @param logger    the identifier of the component catching the exception,
	 *                  conventionally {@code "ClassName#methodName"}; must not be null
	 * @param apiKey    the API key used for authentication with the Alfred API
	 * @param apiSecret the API secret used for signing the request
	 * @param projectId the project identifier
	 */
	public void scheduleExceptionLogTask(Exception exception, String message, String logger, String apiKey, String apiSecret, String projectId) {
		new Thread(
			new ApiExceptionLoggerTask<>(
				exception,
				message,
				logger,
				apiKey,
				apiSecret,
				projectId,
				this.logBook
			)
		).start();
	}
}
