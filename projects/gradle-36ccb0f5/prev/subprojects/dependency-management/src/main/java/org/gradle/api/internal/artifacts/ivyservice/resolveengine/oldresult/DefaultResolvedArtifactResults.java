/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.oldresult;

import com.google.common.collect.Maps;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.ArtifactSet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DefaultResolvedArtifactResults implements ResolvedArtifactResults {
    private Map<Long, ArtifactSet> artifactSets = Maps.newHashMap();
    private Set<ResolvedArtifact> artifacts;
    private Map<Long, Set<ResolvedArtifact>> resolvedArtifactsById;

    @Override
    public Set<ResolvedArtifact> getArtifacts() {
        assertArtifactsResolved();
        return new LinkedHashSet<ResolvedArtifact>(artifacts);
    }

    @Override
    public Set<ResolvedArtifact> getArtifacts(long id) {
        assertArtifactsResolved();
        Set<ResolvedArtifact> a = resolvedArtifactsById.get(id);
        assert a != null : "Unable to find artifacts for id: " + id;
        return a;
    }

    public void addArtifactSet(long id, ArtifactSet artifactSet) {
        artifactSets.put(id, artifactSet);
    }

    public void resolveNow() {
        if (artifacts == null) {
            artifacts = new LinkedHashSet<ResolvedArtifact>();
            resolvedArtifactsById = new LinkedHashMap<Long, Set<ResolvedArtifact>>();
            for (Map.Entry<Long, ArtifactSet> entry : artifactSets.entrySet()) {
                Set<ResolvedArtifact> resolvedArtifacts = entry.getValue().getArtifacts();
                artifacts.addAll(resolvedArtifacts);
                resolvedArtifactsById.put(entry.getKey(), resolvedArtifacts);
            }

            // Release ResolvedArtifactSet instances so we're not holding onto state
            artifactSets = null;
        }
    }

    private void assertArtifactsResolved() {
        if (artifacts == null) {
            throw new IllegalStateException("Cannot access artifacts before they are explicitly resolved.");
        }
    }
}
