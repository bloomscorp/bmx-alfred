package com.bloomscorp.alfred.orm;

/**
 * Marker interface for authentication log entities persisted by the Alfred logging system.
 *
 * <p>Concrete implementations must carry the fields described in
 * {@link com.bloomscorp.alfred.contract.AuthenticationLogContract} and are expected to be
 * JPA-annotated entities mapped to the {@code authentication_log} table. Instances are
 * constructed by {@link com.bloomscorp.alfred.LogBook#buildAuthenticationLogInstance} and
 * persisted via {@link com.bloomscorp.alfred.adapter.ILogBookDAO#insertAuthenticationLog}.
 *
 * @see com.bloomscorp.alfred.contract.AuthenticationLogContract
 * @see com.bloomscorp.alfred.LogBook#buildAuthenticationLogInstance
 */
public interface AuthenticationLog {
}
