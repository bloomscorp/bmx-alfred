package com.bloomscorp.alfred.orm;

/**
 * Enumeration of user authentication actions tracked by the Alfred logging system.
 *
 * <p>Values of this enum are persisted to the {@code authentication_log} table via
 * {@link AuthenticationLog} entities to distinguish between login and logout events.
 *
 * @see AuthenticationLog
 * @see com.bloomscorp.alfred.LogBook#logLogin
 * @see com.bloomscorp.alfred.LogBook#logLogout
 */
public enum AUTH_ACTION_ENUM {
	/** Represents a successful user login event. */
	LOGIN,
	/** Represents a user logout event. */
	LOGOUT
}
