package com.bloomscorp.alfred.adapter;

import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;

public interface ILogBookDAO<A extends AuthenticationLog, L extends Log> {
	long insertAuthenticationLog(A log);
	long insertLog(L log);
}
