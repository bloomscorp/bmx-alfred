package com.bloomscorp.alfred.dao.repository;

import com.bloomscorp.alfred.adapter.ILogBookDAO;
import com.bloomscorp.alfred.orm.AuthenticationLog;
import com.bloomscorp.alfred.orm.Log;

public interface LogBookRepository<A extends AuthenticationLog, L extends Log> extends ILogBookDAO<A, L> {
	long insertAuthenticationLog(A log);
	long insertLog(L log);
}
