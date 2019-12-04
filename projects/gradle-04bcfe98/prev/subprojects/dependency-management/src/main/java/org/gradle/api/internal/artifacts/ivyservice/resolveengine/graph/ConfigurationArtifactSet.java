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

import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.internal.artifacts.ResolvedConfigurationIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.ModuleResolutionFilter;
import org.gradle.internal.component.model.*;
import org.gradle.internal.resolve.resolver.ArtifactResolver;
import org.gradle.internal.resolve.result.BuildableArtifactSetResolveResult;
import org.gradle.internal.resolve.result.DefaultBuildableArtifactSetResolveResult;

import java.util.Map;
import java.util.Set;

/**
 * An ArtifactSet that resolves the artifacts for a configuration.
 */
class ConfigurationArtifactSet extends AbstractArtifactSet {
    private final Set<ComponentArtifactMetaData> artifacts;

    public ConfigurationArtifactSet(ComponentResolveMetaData component, ResolvedConfigurationIdentifier configurationId, ModuleResolutionFilter selector,
                                    ArtifactResolver artifactResolver, Map<ComponentArtifactIdentifier, ResolvedArtifact> allResolvedArtifacts,
                                    long id) {
        super(component.getId(), component.getSource(), selector, artifactResolver, allResolvedArtifacts, id);
        this.artifacts = doResolve(component, configurationId);
    }

    private Set<ComponentArtifactMetaData> doResolve(ComponentResolveMetaData component, ResolvedConfigurationIdentifier configurationId) {
        BuildableArtifactSetResolveResult result = new DefaultBuildableArtifactSetResolveResult();
        getArtifactResolver().resolveModuleArtifacts(component, new DefaultComponentUsage(configurationId.getConfiguration()), result);
        return result.getArtifacts();
    }

    @Override
    protected Set<ComponentArtifactMetaData> resolveComponentArtifacts() {
        return artifacts;
    }
}
