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

package org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph;

import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact;
import org.gradle.api.internal.artifacts.ivyservice.dynamicversions.DefaultResolvedModuleVersion;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.ModuleResolutionFilter;
import org.gradle.internal.Factory;
import org.gradle.internal.component.model.ComponentArtifactIdentifier;
import org.gradle.internal.component.model.ComponentArtifactMetaData;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.resolve.resolver.ArtifactResolver;
import org.gradle.internal.resolve.result.DefaultBuildableArtifactResolveResult;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class AbstractArtifactSet implements ArtifactSet {
    private final ModuleVersionIdentifier moduleVersionIdentifier;
    private final ModuleSource moduleSource;
    private final ModuleResolutionFilter selector;
    private final ArtifactResolver artifactResolver;
    private final Map<ComponentArtifactIdentifier, ResolvedArtifact> allResolvedArtifacts;
    private final long id;

    public AbstractArtifactSet(ModuleVersionIdentifier ownerId, ModuleSource moduleSource, ModuleResolutionFilter selector, ArtifactResolver artifactResolver,
                               Map<ComponentArtifactIdentifier, ResolvedArtifact> allResolvedArtifacts, long id) {
        this.moduleVersionIdentifier = ownerId;
        this.moduleSource = moduleSource;
        this.selector = selector;
        this.artifactResolver = artifactResolver;
        this.allResolvedArtifacts = allResolvedArtifacts;
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public Set<ResolvedArtifact> getArtifacts() {
        Set<ComponentArtifactMetaData> componentArtifacts = resolveComponentArtifacts();
        Set<ResolvedArtifact> resolvedArtifacts = new LinkedHashSet<ResolvedArtifact>(componentArtifacts.size());
        for (ComponentArtifactMetaData artifact : componentArtifacts) {
            IvyArtifactName artifactName = artifact.getName();
            if (!selector.acceptArtifact(moduleVersionIdentifier.getModule(), artifactName)) {
                continue;
            }

            ResolvedArtifact resolvedArtifact = allResolvedArtifacts.get(artifact.getId());
            if (resolvedArtifact == null) {
                Factory<File> artifactSource = new LazyArtifactSource(artifact, moduleSource, artifactResolver);
                resolvedArtifact = new DefaultResolvedArtifact(new DefaultResolvedModuleVersion(moduleVersionIdentifier), artifactName, artifactSource);
                allResolvedArtifacts.put(artifact.getId(), resolvedArtifact);
            }
            resolvedArtifacts.add(resolvedArtifact);
        }
        return resolvedArtifacts;
    }

    protected ArtifactResolver getArtifactResolver() {
        return artifactResolver;
    }

    protected abstract Set<ComponentArtifactMetaData> resolveComponentArtifacts();

    private static class LazyArtifactSource implements Factory<File> {
        private final ArtifactResolver artifactResolver;
        private final ModuleSource moduleSource;
        private final ComponentArtifactMetaData artifact;

        private LazyArtifactSource(ComponentArtifactMetaData artifact, ModuleSource moduleSource, ArtifactResolver artifactResolver) {
            this.artifact = artifact;
            this.artifactResolver = artifactResolver;
            this.moduleSource = moduleSource;
        }

        public File create() {
            DefaultBuildableArtifactResolveResult result = new DefaultBuildableArtifactResolveResult();
            artifactResolver.resolveArtifact(artifact, moduleSource, result);
            return result.getFile();
        }
    }
}
