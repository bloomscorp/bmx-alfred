package com.bloomscorp.alfred.contract;

/**
 * Database column name constants for the {@code authentication_log} table.
 *
 * <p>This non-instantiable utility class centralizes all column identifiers used in
 * authentication log persistence. Consuming applications should reference these constants
 * when writing native queries or JPA mappings to ensure consistency with the library's
 * expected schema.
 *
 * @see com.bloomscorp.alfred.orm.AuthenticationLog
 */
public final class AuthenticationLogContract {

	private AuthenticationLogContract() {}

	/** Database table name for authentication log records. */
	public static final String TABLE = "authentication_log";

	/** Column name for the primary key of an authentication log record. */
	public static final String ID = "id";

	/** Column name for the optimistic locking version field. */
	public static final String VERSION = "version";

	/** Column name storing the authentication action (LOGIN or LOGOUT). */
	public static final String ACTION = "action";

	/** Column name storing the identifier of the user who performed the authentication action. */
	public static final String USER_ID = "user_id";

	/** Column name storing the timestamp of the authentication event. */
	public static final String TIME = "time";

	/** Column name storing the attempt number or sequence for the authentication event. */
	public static final String ATTEMPT = "attempt";

	/** Column name storing additional contextual information about the authentication event. */
	public static final String INFORMATION = "information";
}
