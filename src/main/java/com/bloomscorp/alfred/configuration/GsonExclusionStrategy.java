package com.bloomscorp.alfred.configuration;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import org.jetbrains.annotations.NotNull;

public class GsonExclusionStrategy implements ExclusionStrategy {

	@Override
	public boolean shouldSkipField(@NotNull FieldAttributes fieldAttributes) {
		return fieldAttributes.getAnnotation(GsonExclude.class) != null;
	}

	@Override
	public boolean shouldSkipClass(Class<?> aClass) {
		return false;
	}
}
