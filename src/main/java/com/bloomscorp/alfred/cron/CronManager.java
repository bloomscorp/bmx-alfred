package com.bloomscorp.alfred.cron;

import com.bloomscorp.alfred.LogBook;
import com.bloomscorp.alfred.cron.task.AuthenticationLoggerTask;
import com.bloomscorp.alfred.cron.task.ExceptionLoggerTask;
import com.bloomscorp.alfred.cron.task.LoggerTask;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.LOG_TYPE;
import com.bloomscorp.alfred.orm.Log;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class CronManager<
	B extends LogBook<L, A, T, E, R>,
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private final B logBook;

	public void scheduleLoginLogTask(T user) {
		new Thread(
			new AuthenticationLoggerTask<>(
				user,
				this.logBook,
				true
			)
		).start();
	}

	public void scheduleLogoutLogTask(T user) {
		new Thread(
			new AuthenticationLoggerTask<>(
				user,
				this.logBook,
				false
			)
		).start();
	}

	public void scheduleLogTask(String message, String logger, LOG_TYPE type, String dataDump) {
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

	public void scheduleExceptionLogTask(Exception exception, String message, String logger) {
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
