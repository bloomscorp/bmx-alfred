package com.bloomscorp.alfred.contract;

/**
 * Database column name constants for the {@code log} table.
 *
 * <p>This non-instantiable utility class centralizes all column identifiers used in
 * general log persistence. Consuming applications should reference these constants
 * when writing native queries or JPA mappings to ensure consistency with the library's
 * expected schema.
 *
 * @see com.bloomscorp.alfred.orm.Log
 */
public final class LogContract {

	private LogContract() {}

	/** Database table name for general log records. */
	public static final String TABLE = "log";

	/** Column name for the primary key of a log record. */
	public static final String ID = "id";

	/** Column name for the optimistic locking version field. */
	public static final String VERSION = "version";

	/** Column name storing the identifier of the component or method that produced the log entry. */
	public static final String LOGGER = "logger";

	/** Column name storing the severity or category of the log entry. */
	public static final String LOG_TYPE = "log_type";

	/** Column name storing the human-readable log message. */
	public static final String MESSAGE = "message";

	/** Column name storing an optional serialized payload (e.g., stack trace, JSON body) for additional context. */
	public static final String DATA_DUMP = "data_dump";

	/** Column name storing the timestamp of the log event. */
	public static final String TIME = "time";
}
