/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.bacon.pig.impl.utils;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A set of {@link Gav}s defined by includes and excludes {@link GavPattern}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class GavSet implements Serializable {
    public static class Builder {
        private List<GavPattern> excludes = new ArrayList<>();
        private List<GavPattern> includes = new ArrayList<>();

        private Builder() {
        }

        public GavSet build() {
            if (includes.isEmpty()) {
                includes.add(GavPattern.matchAll());
            }

            List<GavPattern> useIncludes = Collections.unmodifiableList(includes);
            List<GavPattern> useExcludes = Collections.unmodifiableList(excludes);

            this.includes = null;
            this.excludes = null;

            return new GavSet(useIncludes, useExcludes);
        }

        /**
         * Exclude a single GAV pattern.
         *
         * @param rawPattern
         * @return this {@link Builder}
         */
        public Builder exclude(String rawPattern) {
            this.excludes.add(GavPattern.of(rawPattern));
            return this;
        }

        /**
         * Parses the entries of the given {@link Collection} of {@code rawPatterns} and excludes those.
         *
         * @param rawPatterns {@link Collection} of GAV patterns to parse via {@link GavPattern#of(String)}
         * @return this {@link Builder}
         */
        public Builder excludes(Collection<String> rawPatterns) {
            if (rawPatterns != null) {
                for (String rawPattern : rawPatterns) {
                    this.excludes.add(GavPattern.of(rawPattern));
                }
            }
            return this;
        }

        /**
         * Parses the entries of the given array of {@code rawPatterns} and excludes those.
         *
         * @param rawPatterns a list of GAV patterns to parse via {@link GavPattern#of(String)}
         * @return this {@link Builder}
         */
        public Builder excludes(String... rawPatterns) {
            if (rawPatterns != null) {
                for (String rawPattern : rawPatterns) {
                    this.excludes.add(GavPattern.of(rawPattern));
                }
            }
            return this;
        }

        /**
         * Parses the given comma or whitespace separated list of {@code rawPatterns} and excludes those.
         *
         * @param rawPatterns a comma separated list of GAV patterns
         * @return this {@link Builder}
         */
        public Builder excludes(String rawPatterns) {
            if (rawPatterns != null) {
                StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r\f");
                while (st.hasMoreTokens()) {
                    this.excludes.add(GavPattern.of(st.nextToken()));
                }
            }
            return this;
        }

        /**
         * Adds {@link GavPattern#matchSnapshots()} to {@link #excludes}.
         *
         * @return this {@link Builder}
         */
        public Builder excludeSnapshots() {
            this.excludes.add(GavPattern.matchSnapshots());
            return this;
        }

        /**
         * Include a single GAV pattern.
         *
         * @param rawPattern
         * @return this {@link Builder}
         */
        public Builder include(String rawPattern) {
            this.includes.add(GavPattern.of(rawPattern));
            return this;
        }

        /**
         * Parses the entries of the given {@link Collection} of {@code rawPatterns} and includes those.
         *
         * @param rawPatterns {@link Collection} of GAV patterns to parse via {@link GavPattern#of(String)}
         * @return this {@link Builder}
         */
        public Builder includes(Collection<String> rawPatterns) {
            if (rawPatterns != null) {
                for (String rawPattern : rawPatterns) {
                    this.includes.add(GavPattern.of(rawPattern));
                }
            }
            return this;
        }

        /**
         * Parses the given comma or whitespace separated list of {@code rawPatterns} and includes those.
         *
         * @param rawPatterns a comma separated list of GAV patterns
         * @return this {@link Builder}
         */
        public Builder includes(String rawPatterns) {
            if (rawPatterns != null) {
                StringTokenizer st = new StringTokenizer(rawPatterns, ", \t\n\r\f");
                while (st.hasMoreTokens()) {
                    this.includes.add(GavPattern.of(st.nextToken()));
                }
            }
            return this;
        }

        /**
         * Parses the entries of the given array of {@code rawPatterns} and includes those.
         *
         * @param rawPatterns a list of GAV patterns to parse via {@link GavPattern#of(String)}
         * @return this {@link Builder}
         */
        public Builder includes(String... rawPatterns) {
            if (rawPatterns != null) {
                for (String rawPattern : rawPatterns) {
                    this.includes.add(GavPattern.of(rawPattern));
                }
            }
            return this;
        }

    }

    private static final List<GavPattern> EMPTY_LIST = Collections.emptyList();

    private static final GavSet INCLUDE_ALL = new GavSet(Collections.singletonList(GavPattern.matchAll()), EMPTY_LIST);
    /**  */
    private static final long serialVersionUID = 4495169649760950618L;

    private static void append(List<GavPattern> cludes, Appendable out) throws IOException {
        boolean first = true;
        for (GavPattern gavPattern : cludes) {
            if (first) {
                first = false;
            } else {
                out.append(',');
            }
            out.append(gavPattern.toString());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GavSet includeAll() {
        return INCLUDE_ALL;
    }

    private static boolean matches(
            String groupId,
            String artifactId,
            String type,
            String classifier,
            String version,
            List<GavPattern> patterns) {
        for (GavPattern pattern : patterns) {
            if (pattern.matches(groupId, artifactId, type, classifier, version)) {
                return true;
            }
        }
        return false;
    }

    private final List<GavPattern> excludes;
    private final transient int hashcode;;

    private final List<GavPattern> includes;

    GavSet(List<GavPattern> includes, List<GavPattern> excludes) {
        super();
        this.includes = includes;
        this.excludes = excludes;
        this.hashcode = 31 * (31 * 1 + excludes.hashCode()) + includes.hashCode();
    }

    /**
     * Appends {@link #excludes} to the given {@code out} separating them by comma.
     *
     * @param out an {@link Appendable} to append to
     * @throws IOException
     */
    public void appendExcludes(Appendable out) throws IOException {
        append(excludes, out);
    }

    /**
     * Appends {@link #includes} to the given {@code out} separating them by comma.
     *
     * @param out an {@link Appendable} to append to
     * @throws IOException
     */
    public void appendIncludes(Appendable out) throws IOException {
        append(includes, out);
    }

    /**
     *
     * @param groupId
     * @param artifactId
     * @param type cannot be {@code null}
     * @param classifier can be {@code null}
     * @param version
     * @return {@code true} if the given GAV triple is a member of this {@link GavSet} and {@code false} otherwise
     */
    public boolean contains(String groupId, String artifactId, String type, String classifier, String version) {
        return matches(groupId, artifactId, type, classifier, version, includes)
                && !matches(groupId, artifactId, type, classifier, version, excludes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GavSet other = (GavSet) obj;
        if (excludes == null) {
            if (other.excludes != null)
                return false;
        } else if (!excludes.equals(other.excludes))
            return false;
        if (includes == null) {
            if (other.includes != null)
                return false;
        } else if (!includes.equals(other.includes))
            return false;
        return true;
    }

    /**
     * @return the list of excludes
     */
    public List<GavPattern> getExcludes() {
        return excludes;
    }

    /**
     * @return the list of includes
     */
    public List<GavPattern> getIncludes() {
        return includes;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        return "GavSet [excludes=" + excludes + ", includes=" + includes + "]";
    }

}
