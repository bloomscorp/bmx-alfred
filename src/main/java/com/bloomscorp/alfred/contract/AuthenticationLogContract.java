package com.bloomscorp.alfred.contract;

public final class AuthenticationLogContract {

	private AuthenticationLogContract() {}

	public static final String TABLE = "authentication_log";
	public static final String ID = "id";
	public static final String VERSION = "version";
	public static final String ACTION = "action";
	public static final String USER_ID = "user_id";
	public static final String TIME = "time";
	public static final String ATTEMPT = "attempt";
	public static final String INFORMATION = "information";
}
