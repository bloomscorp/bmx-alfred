package com.bloomscorp.alfred.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation that instructs the Alfred Gson instance to exclude the annotated field from JSON serialization.
 *
 * <p>Apply this annotation to fields within reporter POJOs (such as {@link com.bloomscorp.alfred.orm.LogReporter}
 * or {@link com.bloomscorp.alfred.orm.UnauthorizedLogReporter}) that must not appear in the serialized
 * reporter JSON payload. The exclusion is enforced at runtime by {@link GsonExclusionStrategy}, which
 * is registered on the shared {@link com.google.gson.Gson} instance in {@link com.bloomscorp.alfred.LogBook}.
 *
 * @see GsonExclusionStrategy
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GsonExclude {
}
