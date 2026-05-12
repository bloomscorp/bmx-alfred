package com.bloomscorp.alfred.orm;

/**
 * Enumeration of log severity levels used by the Alfred logging system.
 *
 * <p>These levels follow the syslog severity convention (RFC 5424) and are persisted
 * to the {@code log} table via {@link Log} entities. Consumers should select the level
 * that most accurately reflects the urgency and impact of the event being logged.
 *
 * @see Log
 * @see com.bloomscorp.alfred.LogBook#log(String, String, LOG_TYPE, String)
 * @see com.bloomscorp.alfred.contract.LogContract#LOG_TYPE
 */
public enum LOG_TYPE {
	/** System is unusable; immediate action required. Maps to syslog severity 0. */
	EMERGENCY,
	/** Action must be taken immediately; condition requires urgent attention. Maps to syslog severity 1. */
	ALERT,
	/** Critical conditions, such as hard device errors. Maps to syslog severity 2. */
	CRITICAL,
	/** Error conditions that require attention but do not halt the system. Maps to syslog severity 3. */
	ERROR,
	/** Warning conditions indicating potential problems. Maps to syslog severity 4. */
	WARNING,
	/** Normal but significant conditions worth noting. Maps to syslog severity 5. */
	NOTICE,
	/** Informational messages confirming expected behavior. Maps to syslog severity 6. */
	INFO,
	/** Debug-level messages for development and troubleshooting purposes. Maps to syslog severity 7. */
	DEBUG
}
