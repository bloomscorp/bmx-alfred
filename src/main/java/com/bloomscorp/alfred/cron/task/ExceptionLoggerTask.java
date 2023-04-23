package com.bloomscorp.alfred.cron.task;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class ExceptionLoggerTask<
	A extends AuthenticationLog,
	L extends Log,
	T extends NVerseTenant<E>,
	E extends Enum<E>
> implements Runnable {

	private final Exception exception;
	private final String message;
	private final String logger;
	private final LogBook<A, L, T, E> logBook;

	@Override
	public void run() {
		this.logBook.log(exception, message, logger);
	}
}
