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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class GavSetTest {

    @Test
    public void defaults() {
        GavSet set = GavSet.builder().build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
    }

    @Test
    public void excludeArtifact() {
        GavSet set = GavSet.builder() //
                .exclude("org.group1:artifact1") //
                .build();
        Assertions.assertFalse(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertTrue(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertTrue(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertTrue(set.contains("org.group2", "artifact3", "jar", null, "6.7.8"));

        Assertions.assertTrue(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));
    }

    @Test
    public void excludeGroups() {
        GavSet set = GavSet.builder() //
                .exclude("org.group1") //
                .exclude("org.group2") //
                .build();
        Assertions.assertFalse(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertFalse(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertFalse(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertFalse(set.contains("org.group2", "artifact3", "jar", null, "6.7.8"));

        Assertions.assertTrue(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));

    }

    @Test
    public void includeArtifact() {
        GavSet set = GavSet.builder() //
                .include("org.group1:artifact1") //
                .build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertFalse(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertFalse(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertFalse(set.contains("org.group2", "artifact3", "jar", null, "6.7.8"));

        Assertions.assertFalse(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));
    }

    @Test
    public void includeExcludeGroups() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .exclude("org.group2") //
                .build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertTrue(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertFalse(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertFalse(set.contains("org.group2", "artifact3", "jar", null, "6.7.8"));

        Assertions.assertFalse(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));

    }

    @Test
    public void includeGroup() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertTrue(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertFalse(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
    }

    @Test
    public void includeGroups() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .include("org.group2") //
                .build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertTrue(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));

        Assertions.assertTrue(set.contains("org.group2", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertTrue(set.contains("org.group2", "artifact3", "jar", null, "6.7.8"));

        Assertions.assertFalse(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));

    }

    @Test
    public void includeGroupsExcludeArtifact() {
        GavSet set = GavSet.builder() //
                .include("org.group1") //
                .include("org.group2") //
                .include("com.group3") //
                .exclude("org.group1:artifact2") //
                .exclude("org.group1:artifact3") //
                .exclude("org.group2:artifact2") //
                .exclude("org.group2:artifact3") //
                .build();
        Assertions.assertTrue(set.contains("org.group1", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertFalse(set.contains("org.group1", "artifact2", "jar", null, "2.3.4"));
        Assertions.assertFalse(set.contains("org.group1", "artifact3", "jar", null, "2.3.4"));

        Assertions.assertTrue(set.contains("org.group2", "artifact1", "jar", null, "1.2.3"));
        Assertions.assertFalse(set.contains("org.group2", "artifact2", "jar", null, "2.3.4"));
        Assertions.assertFalse(set.contains("org.group2", "artifact3", "jar", null, "2.3.4"));

        Assertions.assertTrue(set.contains("com.group3", "artifact1", "jar", null, "5.6.7"));
        Assertions.assertTrue(set.contains("com.group3", "artifact2", "jar", null, "5.6.7"));
        Assertions.assertTrue(set.contains("com.group3", "artifact3", "jar", null, "5.6.7"));
        Assertions.assertTrue(set.contains("com.group3", "artifact4", "jar", null, "5.6.7"));

    }

}
