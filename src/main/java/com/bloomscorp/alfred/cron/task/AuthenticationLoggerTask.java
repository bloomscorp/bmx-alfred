package com.bloomscorp.alfred.cron.task;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

/**
 * Runnable task that records a user authentication event (login or logout) via {@link LogBook}.
 *
 * <p>Instances of this class are created and started by {@link com.bloomscorp.alfred.cron.CronManager}
 * to perform authentication logging asynchronously. The {@code isLogin} flag determines whether
 * the task delegates to {@link LogBook#logLogin} or {@link LogBook#logLogout}.
 *
 * @param <B> the concrete {@link LogBook} implementation
 * @param <L> the concrete {@link Log} entity type
 * @param <A> the concrete {@link AuthenticationLog} entity type
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 *
 * @see com.bloomscorp.alfred.cron.CronManager
 * @see LogBook#logLogin
 * @see LogBook#logLogout
 */
@AllArgsConstructor
public final class AuthenticationLoggerTask<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements Runnable {

	/** The user whose authentication event is being recorded. Must not be null. */
	private final T user;

	/** The log book to which the authentication event is delegated. Must not be null. */
	private final B logBook;

	/**
	 * Flag indicating the type of authentication event.
	 * {@code true} for login, {@code false} for logout.
	 */
	private final boolean isLogin;

	/**
	 * Executes the authentication logging operation.
	 *
	 * <p>Calls {@link LogBook#logLogin} when {@code isLogin} is {@code true},
	 * or {@link LogBook#logLogout} when {@code false}.
	 */
	@Override
	public void run() {
		if (this.isLogin)
			this.logBook.logLogin(this.user);
		else
			this.logBook.logLogout(this.user);
	}
}
