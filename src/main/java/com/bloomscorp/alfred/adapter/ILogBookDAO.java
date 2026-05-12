package com.bloomscorp.alfred.adapter;

import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;

/**
 * Data access interface for persisting log and authentication log entries.
 *
 * <p>This interface defines the persistence contract that consuming applications must implement
 * to integrate Alfred logging with their data store. Implementations are typically Spring-managed
 * repository beans (e.g., JPA repositories or JDBC-based DAOs) supplied to
 * {@link com.bloomscorp.alfred.LogBook} via constructor injection.
 *
 * @param <A> the concrete {@link AuthenticationLog} entity type to persist
 * @param <L> the concrete {@link Log} entity type to persist
 *
 * @see com.bloomscorp.alfred.LogBook
 */
public interface ILogBookDAO<A extends AuthenticationLog, L extends Log> {

	/**
	 * Persists an authentication log entry and returns the generated record identifier.
	 *
	 * @param log the fully populated authentication log entity to persist; must not be null
	 * @return the generated primary key of the persisted record
	 */
	long insertAuthenticationLog(A log);

	/**
	 * Persists a general log entry and returns the generated record identifier.
	 *
	 * @param log the fully populated log entity to persist; must not be null
	 * @return the generated primary key of the persisted record
	 */
	long insertLog(L log);
}
