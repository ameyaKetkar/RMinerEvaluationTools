/*
 * Copyright 2014 the original author or authors.
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

package org.gradle.platform.base.internal;

import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.model.internal.type.ModelType;
import org.gradle.platform.base.BinarySpec;

import java.util.Set;

public interface BinarySpecInternal extends BinarySpec {
    ModelType<BinarySpec> PUBLIC_MODEL_TYPE = ModelType.of(BinarySpec.class);

    /**
     * Return all language source sets.
     * This method is overridden by NativeTestSuiteBinarySpec to include the source sets from the tested binary.
     *
     * @deprecated Use {@link BinarySpec#getInputs()} instead.
     */
    @Deprecated
    Set<LanguageSourceSet> getAllSources();

    void addSourceSet(LanguageSourceSet sourceSet);

    void setBinarySources(FunctionalSourceSet sources);

    void setBuildable(boolean buildable);

    BinaryBuildAbility getBuildAbility();

    boolean isLegacyBinary();
}
