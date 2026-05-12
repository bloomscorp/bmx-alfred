package com.bloomscorp.alfred.cron.task;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

/**
 * Runnable task that records a caught exception as an error log entry via {@link LogBook}.
 *
 * <p>Instances of this class are created and started by {@link com.bloomscorp.alfred.cron.CronManager}
 * to perform exception logging asynchronously. The task delegates to
 * {@link LogBook#log(Exception, String, String)}, which serializes the full stack trace and
 * persists the entry with {@link com.bloomscorp.alfred.orm.LOG_TYPE#ERROR} severity.
 *
 * @param <B> the concrete {@link LogBook} implementation
 * @param <L> the concrete {@link Log} entity type
 * @param <A> the concrete {@link AuthenticationLog} entity type
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 *
 * @see com.bloomscorp.alfred.cron.CronManager
 * @see LogBook#log(Exception, String, String)
 */
@AllArgsConstructor
public final class ExceptionLoggerTask<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements Runnable {

	/** The exception to be logged. Must not be null. */
	private final Exception exception;

	/** A contextual message describing where or why the exception occurred. Must not be null. */
	private final String message;

	/** The identifier of the component catching the exception, conventionally {@code "ClassName#methodName"}. */
	private final String logger;

	/** The log book to which the exception log entry is delegated. Must not be null. */
	private final B logBook;

	/**
	 * Executes the exception logging operation by delegating to {@link LogBook#log(Exception, String, String)}.
	 */
	@Override
	public void run() {
		this.logBook.log(exception, message, logger);
	}
}
