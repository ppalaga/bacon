package org.jboss.bacon.experimental.impl.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.jboss.pnc.dto.Environment;
import org.jboss.pnc.enums.SystemImageType;
import org.jgroups.util.UUID;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonCacheTest {

    @Test
    void get() throws IOException {
        final MutableClock clock = new MutableClock(Instant.now());
        final Path jsonFile = Path.of("target/JsonCacheTest/" + UUID.randomUUID().toString() + ".json");
        final ObjectMapper m = JsonCache.createMapper();
        final JavaType javaType = m.getTypeFactory().constructCollectionType(Collection.class, Environment.class);
        final JsonCache<Collection<Environment>> cache = new JsonCache(
                m,
                javaType,
                jsonFile,
                Duration.ofSeconds(20),
                clock);

        Assertions.assertThat(jsonFile).doesNotExist();
        final List<Environment> initialEnvs = envs(2);

        {
            final Collection<Environment> actual = cache.get(() -> initialEnvs);
            Assertions.assertThat(jsonFile)
                    .exists()
                    .content()
                    .contains("\"id\" : \"e0\"", "\"id\" : \"e1\"");
            Assertions.assertThat(actual).isSameAs(initialEnvs); // we have not loaded from the file
        }
        System.out.println("now " + Instant.now());
        System.out.println("now " + Instant.now());

        /* The file must still be valid, as long as we have not moved the clock */
        {
            final Collection<Environment> actual = cache.get(() -> {
                throw new IllegalStateException("The refresher should not have been called");
            });
            Assertions.assertThat(jsonFile)
                    .exists()
                    .content()
                    .contains("\"id\" : \"e0\"", "\"id\" : \"e1\"");
            Assertions.assertThat(actual).isNotSameAs(initialEnvs); // we have loaded from the file
            Assertions.assertThat(actual).isEqualTo(initialEnvs);
        }

        /* Move the clock 30s forwards to make the cache expire */
        clock.forwards(Duration.ofSeconds(30));
        final List<Environment> reloadedEnvs = envs(3);
        final Instant beforeStoring = Instant.now().minus(Duration.ofSeconds(1));
        {
            final Collection<Environment> actual = cache.get(() -> reloadedEnvs);
            final Instant afterStoring = Instant.now().plus(Duration.ofSeconds(1));
            Assertions.assertThat(jsonFile)
                    .exists()
                    .content()
                    .contains("\"id\" : \"e0\"", "\"id\" : \"e1\"", "\"id\" : \"e2\"");
            Assertions.assertThat(cache.lastModifiedTime()).isBetween(beforeStoring, afterStoring);
            Assertions.assertThat(actual).isSameAs(reloadedEnvs); // we have not loaded from the file
        }
        /* The file must still be valid, as long as we make sure that the clock is set before the expiration */
        clock.setInstant(beforeStoring.plus(Duration.ofSeconds(20)));
        {
            final Collection<Environment> actual = cache.get(() -> {
                throw new IllegalStateException("The refresher should not have been called");
            });
            Assertions.assertThat(jsonFile)
                    .exists()
                    .content()
                    .contains("\"id\" : \"e0\"", "\"id\" : \"e1\"", "\"id\" : \"e2\"");
            Assertions.assertThat(actual).isNotSameAs(reloadedEnvs); // we have loaded from the file
            Assertions.assertThat(actual).isEqualTo(reloadedEnvs);
        }

        /* Now write some garbage into the file and expect reloading */
        {
            final List<Environment> forcedEnvs = envs(4);
            Files.writeString(jsonFile, "deadbeef", StandardCharsets.UTF_8);
            final Collection<Environment> actual = cache.get(() -> forcedEnvs);
            Assertions.assertThat(jsonFile)
                    .exists()
                    .content()
                    .contains("\"id\" : \"e0\"", "\"id\" : \"e1\"", "\"id\" : \"e2\"", "\"id\" : \"e3\"");
            Assertions.assertThat(actual).isSameAs(forcedEnvs); // we have not loaded from the file
        }

    }

    static List<Environment> envs(int cnt) {
        List<Environment> result = new ArrayList<>(cnt);
        for (int i = 0; i < cnt; i++) {
            result.add(
                    Environment.builder()
                            .id("e" + i)
                            .name("n" + i)
                            .description("d" + i)
                            .systemImageRepositoryUrl("u" + i)
                            .systemImageId("i" + i)
                            .systemImageType(SystemImageType.DOCKER_IMAGE)
                            .build());
        }
        return Collections.unmodifiableList(result);
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            super();
            this.instant = instant;
            this.zone = zone;
        }

        MutableClock(Instant initialInstant) {
            this.instant = initialInstant;
            this.zone = ZoneId.systemDefault();
        }

        public void setInstant(Instant newInstant) {
            this.instant = newInstant;
        }

        public void forwards(TemporalAmount plus) {
            this.instant = instant.plus(plus);
        }

        public void backwards(TemporalAmount minus) {
            this.instant = instant.minus(minus);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
