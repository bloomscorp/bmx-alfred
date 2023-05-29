package com.bloomscorp.alfred;

import com.bloomscorp.alfred.adapter.ILogBookDAO;
import com.bloomscorp.alfred.configuration.GsonExclusionStrategy;
import com.bloomscorp.alfred.orm.*;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

@AllArgsConstructor
public abstract class LogBook<
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	private static final String CAUSE_SEPARATOR = " [ CAUSE ] ";

	private static final Gson GSON = new GsonBuilder().setExclusionStrategies(
		new GsonExclusionStrategy()
	).create();

	private final ILogBookDAO<A, L> repository;

	private void logAuthentication(AUTH_ACTION_ENUM action, T user) {
		this.repository.insertAuthenticationLog(
			this.buildAuthenticationLogInstance(
				action,
				user
			)
		);
	}

	public String prepareLogReporter(@NotNull T user, String reporterID) {
		return LogBook.GSON.toJson(
			new LogReporter(
				user.getId(),
				reporterID
			)
		);
	}

	public String prepareUnauthorizedLogReporter(String reporterID) {
		return LogBook.GSON.toJson(
			new UnauthorizedLogReporter(
				"unauthorized",
				reporterID
			)
		);
	}

	public void logLogin(T user) {
		this.logAuthentication(AUTH_ACTION_ENUM.LOGIN, user);
	}

	public void logLogout(T user) {
		this.logAuthentication(AUTH_ACTION_ENUM.LOGOUT, user);
	}

	public void log(String message, String logger, LOG_TYPE type, String dataDump) {
		this.repository.insertLog(
			this.buildLogInstance(
				logger,
				type,
				message,
				dataDump
			)
		);
	}

	public void log(Exception exception, String message, String logger) {
		this.log(
			message,
			logger,
			LOG_TYPE.ERROR,
			Pastebox.getStackTraceAsString(
				exception,
				CAUSE_SEPARATOR
			)
		);
	}

	public abstract L buildLogInstance(
		String logger,
		LOG_TYPE logType,
		String message,
		String dataDump
	);
	public abstract A buildAuthenticationLogInstance(AUTH_ACTION_ENUM action, T user);
}
