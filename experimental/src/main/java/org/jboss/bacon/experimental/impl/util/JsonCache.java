package org.jboss.bacon.experimental.impl.util;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * A local filesystem cache for data serializable in JSON format.
 *
 * @param <T> the type of the cached object
 * @since 3.3.0
 */
public class JsonCache<T> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(JsonCache.class);

    private final JavaType typeRef;
    private final Path jsonFile;
    private final Duration expirationInterval;
    private final Clock clock;
    private final ObjectMapper mapper;
    private final Object lock = new Object();

    /**
     * Create a new {@link JsonCache} for storing a {@link Collection} of
     *
     * @param <T> the type of the collection elements
     * @param elementType type of the collection elements
     * @param jsonFile where to store the cached data locally
     * @param expirationInterval duration to be added to the last modification time of the {@code jsonFile} to get the
     *        expiration time
     * @return a new {@link JsonCache}
     *
     * @since 3.3.0
     */
    public static <T> JsonCache<Collection<T>> forCollection(
            Class<T> elementType,
            Path jsonFile,
            Duration expirationInterval) {
        final ObjectMapper m = createMapper();
        final JavaType javaType = m.getTypeFactory().constructCollectionType(Collection.class, elementType);
        return new JsonCache<>(m, javaType, jsonFile, expirationInterval);
    }

    /**
     * Create a new {@link JsonCache} for storing an object of a non-collection type.
     *
     * @param <T> the type of the stored object
     * @param type of the stored object
     * @param jsonFile where to store the cached data locally
     * @param expirationInterval duration to be added to the last modification time of the {@code jsonFile} to get the
     *        expiration time
     * @return a new {@link JsonCache}
     *
     * @since 3.3.0
     */
    public static <T> JsonCache<T> forSimpleType(Class<T> type, Path jsonFile, Duration expirationInterval) {
        ObjectMapper m = createMapper();
        JavaType javaType = m.getTypeFactory().constructType(type);
        return new JsonCache<>(m, javaType, jsonFile, expirationInterval);
    }

    JsonCache(ObjectMapper m, JavaType typeRef, Path jsonFile, Duration expirationInterval) {
        this(m, typeRef, jsonFile, expirationInterval, Clock.systemDefaultZone());
    }

    /* For testing purposes only */
    JsonCache(ObjectMapper m, JavaType typeRef, Path jsonFile, Duration expirationInterval, Clock clock) {
        super();
        this.jsonFile = jsonFile;
        this.typeRef = typeRef;
        this.expirationInterval = expirationInterval;
        this.clock = clock;
        this.mapper = m;
    }

    static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.enable(SerializationFeature.INDENT_OUTPUT);
        m.registerModule(new JavaTimeModule());
        return m;
    }

    public T get(Supplier<T> refresher) {
        if (shouldRefresh()) {
            return reloadAndStore(refresher);
        }
        synchronized (lock) {
            log.debug("Reading cached " + jsonFile);
            try (Reader r = Files.newBufferedReader(jsonFile)) {
                return mapper.readValue(r, typeRef);
            } catch (com.fasterxml.jackson.core.JsonParseException e) {
                /*
                 * refresh by calling reloadAndStore() after the implict finally that closes the file
                 * if there is some garbage in the file
                 */
                log.warn(
                        "Cache file " + jsonFile
                                + " might be corrupted or has an outdated schema; reloading the content",
                        e);
            } catch (IOException e) {
                throw new RuntimeException("Could not read from " + jsonFile, e);
            }
            return reloadAndStore(refresher);
        }
    }

    private T reloadAndStore(Supplier<T> refresher) {
        log.debug("Refreshing " + jsonFile);
        final T newValue = refresher.get();
        synchronized (lock) {
            try {
                Files.createDirectories(jsonFile.getParent());
            } catch (IOException e) {
                throw new RuntimeException("Could not create parent directory of " + jsonFile, e);
            }
            try (Writer w = Files.newBufferedWriter(jsonFile)) {
                mapper.writeValue(w, newValue);
            } catch (IOException e) {
                throw new RuntimeException("Could not write to " + jsonFile, e);
            }
        }
        return newValue;
    }

    boolean shouldRefresh() {
        if (!Files.exists(jsonFile)) {
            return true;
        }
        final Instant lastModifiedTime = lastModifiedTime();
        /* The first instant when the {@link #jsonFile} should be considered expired already */
        final Instant jsonFileExpiration = lastModifiedTime.plus(expirationInterval);
        final Instant now = clock.instant();
        if (now.compareTo(jsonFileExpiration) >= 0) {
            return true;
        }
        return false;
    }

    Instant lastModifiedTime() {
        try {
            final BasicFileAttributes attrs = Files.readAttributes(jsonFile, BasicFileAttributes.class);
            return attrs.lastModifiedTime().toInstant();
        } catch (IOException e) {
            throw new RuntimeException("Could not read attributes of " + jsonFile, e);
        }
    }
}
