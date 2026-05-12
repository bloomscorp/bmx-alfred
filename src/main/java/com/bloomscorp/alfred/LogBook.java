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

/**
 * Abstract base class for the Alfred logging system, providing generic log and authentication log recording.
 *
 * <p>This class serves as the central coordination point for all logging operations within the library.
 * It supports structured logging of general application events, exceptions with full stack traces,
 * and user authentication lifecycle events (login/logout). Concrete implementations must provide
 * entity construction logic via {@link #buildLogInstance} and {@link #buildAuthenticationLogInstance}.
 *
 * <p>Persistence is delegated to a provided {@link ILogBookDAO} implementation, decoupling the
 * logging logic from the underlying data store.
 *
 * <p>Reporter JSON payloads are serialized using a {@link Gson} instance configured with
 * {@link GsonExclusionStrategy} to honor {@link com.bloomscorp.alfred.configuration.GsonExclude}
 * annotations on fields that should be excluded from serialization.
 *
 * @param <L> the concrete {@link Log} entity type used to persist general log entries
 * @param <A> the concrete {@link AuthenticationLog} entity type used to persist authentication events
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 *
 * @see ILogBookDAO
 * @see com.bloomscorp.alfred.cron.CronManager
 */
@AllArgsConstructor
public abstract class LogBook<
	L extends Log,
	A extends AuthenticationLog,
	T extends NVerseTenant<E, R>,
	E extends Enum<E>,
	R extends NVerseRole<E>
> {

	/**
	 * Separator string inserted between chained exception causes when converting a stack trace to a string.
	 */
	private static final String CAUSE_SEPARATOR = " [ CAUSE ] ";

	/**
	 * Shared {@link Gson} instance configured with {@link GsonExclusionStrategy} to skip fields
	 * annotated with {@link com.bloomscorp.alfred.configuration.GsonExclude} during serialization.
	 */
	private static final Gson GSON = new GsonBuilder().setExclusionStrategies(
		new GsonExclusionStrategy()
	).create();

	/**
	 * DAO used to persist log and authentication log entries to the underlying data store.
	 * Must not be null.
	 */
	private final ILogBookDAO<A, L> repository;

	/**
	 * Persists an authentication event (login or logout) for the given user.
	 *
	 * @param action the authentication action taken; must not be null
	 * @param user   the tenant/user who performed the action; must not be null
	 */
	private void logAuthentication(AUTH_ACTION_ENUM action, T user) {
		this.repository.insertAuthenticationLog(
			this.buildAuthenticationLogInstance(
				action,
				user
			)
		);
	}

	/**
	 * Serializes a reporter payload for an authenticated user to a JSON string.
	 *
	 * <p>The resulting JSON encodes the user's internal ID alongside a caller-supplied
	 * reporter identifier (typically {@code ClassName#methodName}) to record the origin
	 * of a log entry.
	 *
	 * @param user       the authenticated tenant whose ID is embedded in the reporter; must not be null
	 * @param reporterID the identifier of the reporting location, conventionally formatted as
	 *                   {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID}
	 * @return a non-null JSON string representing a {@link LogReporter}
	 */
	public String prepareLogReporter(@NotNull T user, String reporterID) {
		return LogBook.GSON.toJson(
			new LogReporter(
				user.getId(),
				reporterID
			)
		);
	}

	/**
	 * Serializes a reporter payload for an unauthenticated or unauthorized request to a JSON string.
	 *
	 * <p>The user field in the resulting JSON is always set to {@code "unauthorized"}, indicating
	 * that the log entry originated from a context where no valid user session exists.
	 *
	 * @param reporterID the identifier of the reporting location, conventionally formatted as
	 *                   {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID}
	 * @return a non-null JSON string representing an {@link UnauthorizedLogReporter}
	 */
	public String prepareUnauthorizedLogReporter(String reporterID) {
		return LogBook.GSON.toJson(
			new UnauthorizedLogReporter(
				"unauthorized",
				reporterID
			)
		);
	}

	/**
	 * Records a login event for the given user.
	 *
	 * @param user the tenant who logged in; must not be null
	 */
	public void logLogin(T user) {
		this.logAuthentication(AUTH_ACTION_ENUM.LOGIN, user);
	}

	/**
	 * Records a logout event for the given user.
	 *
	 * @param user the tenant who logged out; must not be null
	 */
	public void logLogout(T user) {
		this.logAuthentication(AUTH_ACTION_ENUM.LOGOUT, user);
	}

	/**
	 * Persists a structured log entry with an explicit type and optional data dump.
	 *
	 * @param message  the human-readable log message; must not be null
	 * @param logger   the identifier of the component or location producing the log,
	 *                 conventionally {@code "ClassName#methodName"}; must not be null
	 * @param type     the severity/category of the log entry; must not be null
	 * @param dataDump an optional serialized payload (e.g., JSON, stack trace, request body)
	 *                 providing additional context; may be null or empty
	 */
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

	/**
	 * Persists an error-level log entry for a caught exception, including the full stack trace.
	 *
	 * <p>The log type is always set to {@link LOG_TYPE#ERROR}. The exception's stack trace is
	 * converted to a string using {@link Pastebox#getStackTraceAsString}, with individual
	 * cause messages separated by {@code " [ CAUSE ] "}.
	 *
	 * @param exception the caught exception to record; must not be null
	 * @param message   a descriptive message providing context about where or why the exception occurred;
	 *                  must not be null
	 * @param logger    the identifier of the component catching the exception,
	 *                  conventionally {@code "ClassName#methodName"}; must not be null
	 */
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

	/**
	 * Constructs a concrete log entity from the provided fields.
	 *
	 * <p>Implementations must populate all required fields of the {@link Log} entity
	 * (typically including a timestamp, logger name, log type, message, and data dump)
	 * without persisting it — persistence is handled by {@link ILogBookDAO#insertLog}.
	 *
	 * @param logger    the identifier of the reporting location; must not be null
	 * @param logType   the severity/category of the log entry; must not be null
	 * @param message   the human-readable log message; must not be null
	 * @param dataDump  additional serialized data for the log entry; may be null or empty
	 * @return a fully initialized, non-null log entity ready for persistence
	 */
	public abstract L buildLogInstance(
		String logger,
		LOG_TYPE logType,
		String message,
		String dataDump
	);

	/**
	 * Constructs a concrete authentication log entity from the provided fields.
	 *
	 * <p>Implementations must populate all required fields of the {@link AuthenticationLog} entity
	 * (typically including a timestamp, user reference, and action) without persisting it —
	 * persistence is handled by {@link ILogBookDAO#insertAuthenticationLog}.
	 *
	 * @param action the authentication action performed (LOGIN or LOGOUT); must not be null
	 * @param user   the tenant who performed the action; must not be null
	 * @return a fully initialized, non-null authentication log entity ready for persistence
	 */
	public abstract A buildAuthenticationLogInstance(AUTH_ACTION_ENUM action, T user);
}
