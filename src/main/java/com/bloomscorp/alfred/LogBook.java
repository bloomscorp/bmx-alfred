package com.bloomscorp.alfred;

import com.bloomscorp.alfred.adapter.ILogBookDAO;
import com.bloomscorp.alfred.configuration.GsonExclusionStrategy;
import com.bloomscorp.alfred.orm.*;
import com.bloomscorp.alfred.support.AlfredConstants;
import com.bloomscorp.nverse.pojo.NVerseRole;
import com.bloomscorp.nverse.pojo.NVerseTenant;
import com.bloomscorp.pastebox.Pastebox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abstract base class for the Alfred logging system, providing generic log and authentication log recording.
 *
 * <p>This class serves as the central coordination point for all logging operations within the library.
 * It supports structured logging of general application events, exceptions with full stack traces,
 * and user authentication lifecycle events (login/logout). Concrete implementations must provide
 * entity construction logic via {@link #buildLogInstance} and {@link #buildAuthenticationLogInstance}.
 *
 * <p>Persistence is delegated to a provided {@link ILogBookDAO} implementation, decoupling the
 * logging logic from the underlying data store.
 *
 * <p>Reporter JSON payloads are serialized using a {@link Gson} instance configured with
 * {@link GsonExclusionStrategy} to honor {@link com.bloomscorp.alfred.configuration.GsonExclude}
 * annotations on fields that should be excluded from serialization.
 *
 * @param <L> the concrete {@link Log} entity type used to persist general log entries
 * @param <A> the concrete {@link AuthenticationLog} entity type used to persist authentication events
 * @param <T> the concrete {@link NVerseTenant} type representing the authenticated user
 * @param <E> the enum type representing application roles
 * @param <R> the concrete {@link NVerseRole} type bound to the role enum
 * @see ILogBookDAO
 * @see com.bloomscorp.alfred.cron.CronManager
 */
public abstract class LogBook<
    L extends Log,
    A extends AuthenticationLog,
    T extends NVerseTenant<E, R>,
    E extends Enum<E>,
    R extends NVerseRole<E>
    > {

    /**
     * Separator string inserted between chained exception causes when converting a stack trace to a string.
     */
    private static final String CAUSE_SEPARATOR = " [ CAUSE ] ";

    /**
     * The fixed API URL base for the logging service.
     */
    private final String logServiceBaseUrl;

    /**
     * Shared {@link Gson} instance configured with {@link GsonExclusionStrategy} to skip fields
     * annotated with {@link com.bloomscorp.alfred.configuration.GsonExclude} during serialization.
     */
    private static final Gson GSON = new GsonBuilder()
        .setExclusionStrategies(new GsonExclusionStrategy())
        .disableHtmlEscaping()
        .create();

    /**
     * Constructs a new LogBook with a repository, using {@link AlfredConstants#DEFAULT_LOGGER_SERVICE_BASE_URL}
     * as the log service base URL.
     *
     * @param repository the repository used for local persistence
     */
    public LogBook(ILogBookDAO<A, L> repository) {
        this(repository, AlfredConstants.DEFAULT_LOGGER_SERVICE_BASE_URL);
    }

    /**
     * Constructs a new LogBook with a repository and an explicit log service base URL,
     * e.g. sourced from a {@code @Value("${alfred.sync.baseUrl}")} property in the consuming app.
     *
     * @param repository  the repository used for local persistence
     * @param loggerServiceBaseUrl the base URL of the logger service
     */
    public LogBook(ILogBookDAO<A, L> repository, String loggerServiceBaseUrl) {
        this.repository = repository;
        this.logServiceBaseUrl = loggerServiceBaseUrl + "/log/";
    }

    /**
     * Calculates the HMAC-SHA256 signature for API requests.
     *
     * @param secret    the API secret
     * @param timestamp the current timestamp
     * @param body      the request body (JSON string)
     * @return the hex-encoded signature
     */
    private String prepareSignature(String secret, String timestamp, String body) {
        try {
            String data = timestamp + "." + body;
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return HexFormat.of().formatHex(sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Prepares log service request URI based on log type.
     * If the type is ERROR, then it will be considered as Exception Log; otherwise general Log.
     *
     * @param type      the severity/category of the log entry; must not be null
     * @param projectId the project identifier
     * @return the prepared URI
     */
    private URI prepareRequestURI(LOG_TYPE type, String projectId) {
        if (type != LOG_TYPE.ERROR) {
            return URI.create(this.logServiceBaseUrl + projectId);
        } else {
            return URI.create(this.logServiceBaseUrl + projectId + "/exception");
        }
    }

    /**
     * DAO used to persist log and authentication log entries to the underlying data store.
     * Must not be null.
     */
    private final ILogBookDAO<A, L> repository;

    /**
     * Persists an authentication event (login or logout) for the given user.
     *
     * @param action the authentication action taken; must not be null
     * @param user   the tenant/user who performed the action; must not be null
     */
    private void logAuthentication(AUTH_ACTION_ENUM action, T user) {
        this.repository.insertAuthenticationLog(
            this.buildAuthenticationLogInstance(
                action,
                user
            )
        );
    }

    /**
     * Serializes a reporter payload for an authenticated user to a JSON string.
     *
     * <p>The resulting JSON encodes the user's internal ID alongside a caller-supplied
     * reporter identifier (typically {@code ClassName#methodName}) to record the origin
     * of a log entry.
     *
     * @param user       the authenticated tenant whose ID is embedded in the reporter; must not be null
     * @param reporterID the identifier of the reporting location, conventionally formatted as
     *                   {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID}
     * @return a non-null JSON string representing a {@link LogReporter}
     */
    public String prepareLogReporter(@NotNull T user, String reporterID) {
        return LogBook.GSON.toJson(
            new LogReporter(
                user.getId(),
                reporterID
            )
        );
    }

    /**
     * Serializes a reporter payload for an unauthenticated or unauthorized request to a JSON string.
     *
     * <p>The user field in the resulting JSON is always set to {@code "unauthorized"}, indicating
     * that the log entry originated from a context where no valid user session exists.
     *
     * @param reporterID the identifier of the reporting location, conventionally formatted as
     *                   {@code "ClassName#methodName"} using {@link com.bloomscorp.alfred.support.ReporterID}
     * @return a non-null JSON string representing an {@link UnauthorizedLogReporter}
     */
    public String prepareUnauthorizedLogReporter(String reporterID) {
        return LogBook.GSON.toJson(
            new UnauthorizedLogReporter(
                "unauthorized",
                reporterID
            )
        );
    }

    /**
     * Records a login event for the given user.
     *
     * @param user the tenant who logged in; must not be null
     */
    public void logLogin(T user) {
        this.logAuthentication(AUTH_ACTION_ENUM.LOGIN, user);
    }

    /**
     * Records a logout event for the given user.
     *
     * @param user the tenant who logged out; must not be null
     */
    public void logLogout(T user) {
        this.logAuthentication(AUTH_ACTION_ENUM.LOGOUT, user);
    }

    /**
     * Persists a structured log entry with an explicit type and optional data dump.
     *
     * @param message  the human-readable log message; must not be null
     * @param logger   the identifier of the component or location producing the log,
     *                 conventionally {@code "ClassName#methodName"}; must not be null
     * @param type     the severity/category of the log entry; must not be null
     * @param dataDump an optional serialized payload (e.g., JSON, stack trace, request body)
     *                 providing additional context; may be null or empty
     */
    public void log(String message, String logger, LOG_TYPE type, String dataDump) {
        this.repository.insertLog(
            this.buildLogInstance(
                logger,
                type,
                message,
                this.decodeDataDump(dataDump)
            )
        );
    }

    /**
     * Decodes HTML entities in the data dump.
     *
     * @param dataDump the data dump to decode
     * @return the decoded data dump
     */
    private String decodeDataDump(String dataDump) {
        if (dataDump == null) return null;
        return dataDump
            .replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&");
    }

    /**
     * Persists an error-level log entry for a caught exception, including the full stack trace.
     *
     * <p>The log type is always set to {@link LOG_TYPE#ERROR}. The exception's stack trace is
     * converted to a string using {@link Pastebox#getStackTraceAsString}, with individual
     * cause messages separated by {@code " [ CAUSE ] "}.
     *
     * @param exception the caught exception to record; must not be null
     * @param message   a descriptive message providing context about where or why the exception occurred;
     *                  must not be null
     * @param logger    the identifier of the component catching the exception,
     *                  conventionally {@code "ClassName#methodName"}; must not be null
     */
    public void log(Exception exception, String message, String logger) {
        this.log(
            message,
            logger,
            LOG_TYPE.ERROR,
            Pastebox.getStackTraceAsString(
                exception,
                CAUSE_SEPARATOR
            )
        );
    }

    /**
     * Persists an error-level log entry for a caught exception via an API call.
     *
     * @param exception the caught exception to record; must not be null
     * @param message   a descriptive message providing context about where or why the exception occurred;
     *                  must not be null
     * @param logger    the identifier of the component catching the exception,
     *                  conventionally {@code "ClassName#methodName"}; must not be null
     * @param apiKey    the API key used for authentication with the Alfred API
     * @param apiSecret the API secret used for signing the request
     * @param projectId the project identifier
     */
    public void log(
        Exception exception,
        String message,
        String logger,
        String apiKey,
        String apiSecret,
        String projectId
    ) {
        this.log(
            message,
            logger,
            LOG_TYPE.ERROR,
            Pastebox.getStackTraceAsString(
                exception,
                CAUSE_SEPARATOR
            ),
            apiKey,
            apiSecret,
            projectId
        );
    }

    /**
     * Persists both structured log and error-level(exception) entry via an API call to logger service.
     *
     * @param message   the human-readable log message; must not be null
     * @param logger    the identifier of the component producing the log,
     *                  conventionally {@code "ClassName#methodName"}; must not be null
     * @param type      the severity/category of the log entry; must not be null
     * @param dataDump  optional serialized context data (JSON, request body, etc.); may be null or empty
     * @param apiKey    the API key used for authentication with the Alfred API
     * @param apiSecret the API secret used for signing the request
     * @param projectId the project identifier
     */
    public void log(
        String message,
        String logger,
        @NotNull LOG_TYPE type,
        String dataDump,
        String apiKey,
        String apiSecret,
        String projectId
    ) {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, Object> logData = new LinkedHashMap<>();
        logData.put("level", type.name());

        try {
            logData.put("logger", JsonParser.parseString(logger));
        } catch (Exception e) {
            logData.put("logger", logger);
        }

        logData.put("message", message);

        String decodedDataDump = this.decodeDataDump(dataDump);

        if (decodedDataDump != null) {
            try {
                logData.put("dataDump", JsonParser.parseString(decodedDataDump));
            } catch (Exception e) {
                logData.put("dataDump", decodedDataDump);
            }
        } else {
            logData.put("dataDump", (Object) null);
        }

        String timestamp = String.valueOf(Pastebox.getCurrentTimeInMillis());
        String json = LogBook.GSON.toJson(logData);
        String signature = this.prepareSignature(apiSecret, timestamp, json);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(this.prepareRequestURI(type, projectId))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .header("x-timestamp", timestamp)
            .header("x-signature", signature)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    this.log(message, logger, type, dataDump);
                }
            })
            .exceptionally(ex -> {
                this.log(message, logger, type, dataDump);
                return null;
            });
    }

    // TODO: implement
    /**
     * Persists an authentication event via an API call.
     *
     * @param action    the authentication action taken; must not be null
     * @param user      the tenant/user who performed the action; must not be null
     * @param apiKey    the API key used for authentication with the Alfred API
     * @param apiSecret the API secret used for signing the request
     * @param projectId the project identifier
     */
    // public void authenticationLog(AUTH_ACTION_ENUM action, T user, String apiKey, String apiSecret, String projectId) {
    // 	String message = action.name() + " event for user " + user.getId();
    // 	String logger = "Alfred#authenticationLog";
    // 	this.log(message, logger, LOG_TYPE.INFO, "", apiKey, apiSecret, projectId);
    // }

    /**
     * Constructs a concrete log entity from the provided fields.
     *
     * <p>Implementations must populate all required fields of the {@link Log} entity
     * (typically including a timestamp, logger name, log type, message, and data dump)
     * without persisting it — persistence is handled by {@link ILogBookDAO#insertLog}.
     *
     * @param logger   the identifier of the reporting location; must not be null
     * @param logType  the severity/category of the log entry; must not be null
     * @param message  the human-readable log message; must not be null
     * @param dataDump additional serialized data for the log entry; may be null or empty
     * @return a fully initialized, non-null log entity ready for persistence
     */
    public abstract L buildLogInstance(
        String logger,
        LOG_TYPE logType,
        String message,
        String dataDump
    );

    /**
     * Constructs a concrete authentication log entity from the provided fields.
     *
     * <p>Implementations must populate all required fields of the {@link AuthenticationLog} entity
     * (typically including a timestamp, user reference, and action) without persisting it —
     * persistence is handled by {@link ILogBookDAO#insertAuthenticationLog}.
     *
     * @param action the authentication action performed (LOGIN or LOGOUT); must not be null
     * @param user   the tenant who performed the action; must not be null
     * @return a fully initialized, non-null authentication log entity ready for persistence
     */
    public abstract A buildAuthenticationLogInstance(AUTH_ACTION_ENUM action, T user);
}
