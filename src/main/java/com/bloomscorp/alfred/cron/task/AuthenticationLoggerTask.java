package com.bloomscorp.alfred.cron.task;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class AuthenticationLoggerTask<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> implements Runnable {

	private final T user;
	private final B logBook;
	private final boolean isLogin;

	@Override
	public void run() {
		if (this.isLogin)
			this.logBook.logLogin(this.user);
		else
			this.logBook.logLogout(this.user);
	}
}
