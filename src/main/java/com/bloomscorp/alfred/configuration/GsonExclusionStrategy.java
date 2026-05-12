package com.bloomscorp.alfred.configuration;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.jetbrains.annotations.NotNull;

/**
 * Gson {@link ExclusionStrategy} that skips fields annotated with {@link GsonExclude} during serialization.
 *
 * <p>This strategy is registered on the shared {@link com.google.gson.Gson} instance used by
 * {@link com.bloomscorp.alfred.LogBook} to serialize reporter payloads. Any field carrying
 * {@code @GsonExclude} is transparently omitted from the resulting JSON without requiring
 * manual configuration per object.
 *
 * @see GsonExclude
 * @see com.bloomscorp.alfred.LogBook
 */
public class GsonExclusionStrategy implements ExclusionStrategy {

	/**
	 * Determines whether a field should be excluded from Gson serialization.
	 *
	 * <p>Returns {@code true} (exclude) if the field is annotated with {@link GsonExclude};
	 * otherwise returns {@code false} (include).
	 *
	 * @param fieldAttributes metadata about the field being evaluated; must not be null
	 * @return {@code true} if the field carries {@code @GsonExclude}, {@code false} otherwise
	 */
	@Override
	public boolean shouldSkipField(@NotNull FieldAttributes fieldAttributes) {
		return fieldAttributes.getAnnotation(GsonExclude.class) != null;
	}

	/**
	 * Determines whether an entire class should be excluded from Gson serialization.
	 *
	 * <p>Always returns {@code false} — class-level exclusion is not used by this strategy;
	 * only field-level exclusion via {@link GsonExclude} is supported.
	 *
	 * @param aClass the class being evaluated
	 * @return {@code false} unconditionally
	 */
	@Override
	public boolean shouldSkipClass(Class<?> aClass) {
		return false;
	}
}
