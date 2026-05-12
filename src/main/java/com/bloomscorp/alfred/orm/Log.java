package com.bloomscorp.alfred.orm;

/**
 * Marker interface for general log entities persisted by the Alfred logging system.
 *
 * <p>Concrete implementations must carry the fields described in
 * {@link com.bloomscorp.alfred.contract.LogContract} and are expected to be JPA-annotated
 * entities mapped to the {@code log} table. Instances are constructed by
 * {@link com.bloomscorp.alfred.LogBook#buildLogInstance} and persisted via
 * {@link com.bloomscorp.alfred.adapter.ILogBookDAO#insertLog}.
 *
 * @see com.bloomscorp.alfred.contract.LogContract
 * @see com.bloomscorp.alfred.LogBook#buildLogInstance
 */
public interface Log {
}
