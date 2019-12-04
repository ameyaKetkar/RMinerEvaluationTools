/*
 * Copyright 2015-present Facebook, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License. You may obtain
 *  a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package com.facebook.buck.js;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.AbiRule;
import com.facebook.buck.rules.AbstractBuildRule;
import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.Sha1HashCode;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.shell.ShellStep;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.facebook.buck.step.fs.MakeCleanDirectoryStep;
import com.google.common.collect.ImmutableList;

import java.nio.file.Path;

/**
 * Responsible for running the React Native JS packager in order to generate a single {@code .js}
 * bundle along with resources referenced by the javascript code.
 */
public class ReactNativeBundle extends AbstractBuildRule implements AbiRule {

  @AddToRuleKey
  private final SourcePath entryPath;

  @AddToRuleKey
  private final boolean isDevMode;

  @AddToRuleKey
  private final SourcePath jsPackager;

  @AddToRuleKey
  private final ReactNativePlatform platform;

  private final ReactNativeDeps depsFinder;
  private final Path jsOutput;
  private final Path resource;

  protected ReactNativeBundle(
      BuildRuleParams ruleParams,
      SourcePathResolver resolver,
      SourcePath entryPath,
      boolean isDevMode,
      String bundleName,
      SourcePath jsPackager,
      ReactNativePlatform platform,
      ReactNativeDeps depsFinder) {
    super(ruleParams, resolver);
    this.entryPath = entryPath;
    this.isDevMode = isDevMode;
    this.jsPackager = jsPackager;
    this.platform = platform;
    this.depsFinder = depsFinder;
    BuildTarget buildTarget = ruleParams.getBuildTarget();
    this.jsOutput = BuildTargets.getGenPath(buildTarget, "__%s_js__/").resolve(bundleName);
    this.resource = BuildTargets.getGenPath(buildTarget, "__%s_res__/").resolve("res");
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context,
      BuildableContext buildableContext) {
    ImmutableList.Builder<Step> steps = ImmutableList.builder();
    steps.add(new MakeCleanDirectoryStep(jsOutput.getParent()));
    steps.add(new MakeCleanDirectoryStep(resource));

    steps.add(
        new ShellStep() {
          @Override
          public String getShortName() {
            return "bundle_react_native";
          }

          @Override
          protected ImmutableList<String> getShellCommandInternal(ExecutionContext context) {
            ProjectFilesystem filesystem = context.getProjectFilesystem();
            return ImmutableList.of(
                getResolver().getPath(jsPackager).toString(),
                "bundle",
                "--entry-file", filesystem.resolve(getResolver().getPath(entryPath)).toString(),
                "--platform", platform.toString(),
                "--dev", isDevMode ? "true" : "false",
                "--bundle-output", filesystem.resolve(jsOutput).toString(),
                "--assets-dest", filesystem.resolve(resource).toString());
          }
        });
    buildableContext.recordArtifact(jsOutput);
    buildableContext.recordArtifact(resource);
    return steps.build();
  }

  public Path getPathToJSBundleDir() {
    return jsOutput.getParent();
  }

  public Path getPathToResources() {
    return resource;
  }

  @Override
  public Path getPathToOutput() {
    return jsOutput;
  }

  @Override
  public Sha1HashCode getAbiKeyForDeps() {
    return depsFinder.getInputsHash();
  }
}
