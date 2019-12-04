/*
 * Copyright 2014-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.android;

import com.facebook.buck.io.MorePaths;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.rules.AbstractBuildRule;
import com.facebook.buck.rules.AddToRuleKey;
import com.facebook.buck.rules.BuildContext;
import com.facebook.buck.rules.BuildOutputInitializer;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildableContext;
import com.facebook.buck.rules.InitializableFromDisk;
import com.facebook.buck.rules.OnDiskBuildInfo;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.rules.SourcePaths;
import com.facebook.buck.step.AbstractExecutionStep;
import com.facebook.buck.step.ExecutionContext;
import com.facebook.buck.step.Step;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nullable;

/**
 * Buildable that is responsible for taking a set of res/ directories and applying an optional
 * resource filter to them, ultimately generating the final set of res/ directories whose contents
 * should be included in an APK.
 * <p>
 * Clients of this Buildable may need to know:
 * <ul>
 *   <li>The set of res/ directories that was used to calculate the R.java file. (These are needed
 *       as arguments to aapt to create the unsigned APK, as well as arguments to create a
 *       ProGuard config, if appropriate.)
 *   <li>The set of non-english {@code strings.xml} files identified by the resource filter.
 * </ul>
 */
public class ResourcesFilter extends AbstractBuildRule
    implements FilteredResourcesProvider, InitializableFromDisk<ResourcesFilter.BuildOutput> {

  private static final String RES_DIRECTORIES_KEY = "res_directories";
  private static final String STRING_FILES_KEY = "string_files";

  static enum ResourceCompressionMode {
    DISABLED(/* isCompressResources */ false, /* isStoreStringsAsAssets */ false),
    ENABLED(/* isCompressResources */ true, /* isStoreStringsAsAssets */ false),
    ENABLED_WITH_STRINGS_AS_ASSETS(
      /* isCompressResources */ true,
      /* isStoreStringsAsAssets */ true),
    ;

    private final boolean isCompressResources;
    private final boolean isStoreStringsAsAssets;

    private ResourceCompressionMode(boolean isCompressResources, boolean isStoreStringsAsAssets) {
      this.isCompressResources = isCompressResources;
      this.isStoreStringsAsAssets = isStoreStringsAsAssets;
    }

    public boolean isCompressResources() {
      return isCompressResources;
    }

    public boolean isStoreStringsAsAssets() {
      return isStoreStringsAsAssets;
    }
  }

  // Rule key correctness is ensured by depping on all android_resource rules in
  // Builder.setAndroidResourceDepsFinder()
  private final ImmutableList<SourcePath> resDirectories;
  private final ImmutableSet<SourcePath> whitelistedStringDirs;
  @AddToRuleKey
  private final ImmutableSet<String> locales;
  @AddToRuleKey
  private final ResourceCompressionMode resourceCompressionMode;
  @AddToRuleKey
  private final FilterResourcesStep.ResourceFilter resourceFilter;
  private final BuildOutputInitializer<BuildOutput> buildOutputInitializer;

  public ResourcesFilter(
      BuildRuleParams params,
      SourcePathResolver resolver,
      ImmutableList<SourcePath> resDirectories,
      ImmutableSet<SourcePath> whitelistedStringDirs,
      ImmutableSet<String> locales,
      ResourceCompressionMode resourceCompressionMode,
      FilterResourcesStep.ResourceFilter resourceFilter) {
    super(params, resolver);
    this.resDirectories = resDirectories;
    this.whitelistedStringDirs = whitelistedStringDirs;
    this.locales = locales;
    this.resourceCompressionMode = resourceCompressionMode;
    this.resourceFilter = resourceFilter;
    this.buildOutputInitializer = new BuildOutputInitializer<>(params.getBuildTarget(), this);
  }

  @Override
  public ImmutableList<SourcePath> getResDirectories() {
    return buildOutputInitializer.getBuildOutput().resDirectories;
  }

  @Override
  public ImmutableList<SourcePath> getStringFiles() {
    return buildOutputInitializer.getBuildOutput().stringFiles;
  }

  @Override
  public ImmutableList<Step> getBuildSteps(
      BuildContext context,
      final BuildableContext buildableContext) {
    ImmutableList.Builder<Step> steps = ImmutableList.builder();

    final ImmutableList.Builder<Path> filteredResDirectoriesBuilder = ImmutableList.builder();
    ImmutableSet<Path> whitelistedStringPaths =
        ImmutableSet.copyOf(getResolver().getAllPaths(whitelistedStringDirs));
    ImmutableList<Path> resPaths = getResolver().getAllPaths(resDirectories);
    final FilterResourcesStep filterResourcesStep = createFilterResourcesStep(
        resPaths,
        whitelistedStringPaths,
        locales,
        filteredResDirectoriesBuilder);
    steps.add(filterResourcesStep);

    final ImmutableList.Builder<Path> stringFilesBuilder = ImmutableList.builder();
    // The list of strings.xml files is only needed to build string assets
    if (resourceCompressionMode.isStoreStringsAsAssets()) {
      GetStringsFilesStep getStringsFilesStep = new GetStringsFilesStep(
          resPaths,
          stringFilesBuilder,
          whitelistedStringPaths);
      steps.add(getStringsFilesStep);
    }

    final ImmutableList<Path> filteredResDirectories = filteredResDirectoriesBuilder.build();
    for (Path outputResourceDir : filteredResDirectories) {
      buildableContext.recordArtifact(outputResourceDir);
    }

    steps.add(new AbstractExecutionStep("record_build_output") {
      @Override
      public int execute(ExecutionContext context) {
        buildableContext.addMetadata(
            RES_DIRECTORIES_KEY,
            Iterables.transform(filteredResDirectories, Functions.toStringFunction()));
        buildableContext.addMetadata(
            STRING_FILES_KEY,
            Iterables.transform(stringFilesBuilder.build(), Functions.toStringFunction()));
        return 0;
      }
    });

    return steps.build();
  }

  /**
   * Sets up filtering of resources, images/drawables and strings in particular, based on build
   * rule parameters {@link #resourceFilter} and {@link #resourceCompressionMode}.
   *
   * {@link com.facebook.buck.android.FilterResourcesStep.ResourceFilter} {@code resourceFilter}
   * determines which drawables end up in the APK (based on density - mdpi, hdpi etc), and also
   * whether higher density drawables get scaled down to the specified density (if not present).
   *
   * {@link #resourceCompressionMode} determines whether non-english string resources are packaged
   * separately as assets (and not bundled together into the {@code resources.arsc} file).
   *
   * @param whitelistedStringDirs overrides storing non-english strings as assets for resources
   *     inside these directories.
   */
  @VisibleForTesting
  FilterResourcesStep createFilterResourcesStep(
      ImmutableList<Path> resourceDirectories,
      ImmutableSet<Path> whitelistedStringDirs,
      ImmutableSet<String> locales,
      ImmutableList.Builder<Path> filteredResDirectories) {
    ImmutableBiMap.Builder<Path, Path> filteredResourcesDirMapBuilder = ImmutableBiMap.builder();
    String resDestinationBasePath = getResDestinationBasePath();
    int count = 0;
    for (Path resDir : resourceDirectories) {
      Path filteredResourceDir = Paths.get(resDestinationBasePath, String.valueOf(count++));
      filteredResourcesDirMapBuilder.put(resDir, filteredResourceDir);
      filteredResDirectories.add(filteredResourceDir);
    }

    ImmutableBiMap<Path, Path> resSourceToDestDirMap = filteredResourcesDirMapBuilder.build();
    FilterResourcesStep.Builder filterResourcesStepBuilder = FilterResourcesStep.builder()
        .setInResToOutResDirMap(resSourceToDestDirMap)
        .setResourceFilter(resourceFilter);

    if (resourceCompressionMode.isStoreStringsAsAssets()) {
      filterResourcesStepBuilder.enableStringWhitelisting();
      filterResourcesStepBuilder.setWhitelistedStringDirs(whitelistedStringDirs);
    }

    filterResourcesStepBuilder.setLocales(locales);

    return filterResourcesStepBuilder.build();
  }

  private String getResDestinationBasePath() {
    return BuildTargets.getScratchPath(getBuildTarget(), "__filtered__%s__").toString();
  }

  @Override
  public BuildOutput initializeFromDisk(OnDiskBuildInfo onDiskBuildInfo) {
    ImmutableList<SourcePath> resDirectories =
        FluentIterable.from(onDiskBuildInfo.getValues(RES_DIRECTORIES_KEY).get())
            .transform(MorePaths.TO_PATH)
            .transform(
                SourcePaths.getToBuildTargetSourcePath(getProjectFilesystem(), getBuildTarget()))
            .toList();
    ImmutableList<SourcePath> stringFiles =
        FluentIterable.from(onDiskBuildInfo.getValues(STRING_FILES_KEY).get())
            .transform(MorePaths.TO_PATH)
            .transform(
                SourcePaths.getToBuildTargetSourcePath(getProjectFilesystem(), getBuildTarget()))
            .toList();

    return new BuildOutput(resDirectories, stringFiles);
  }

  @Override
  public BuildOutputInitializer<BuildOutput> getBuildOutputInitializer() {
    return buildOutputInitializer;
  }

  public static class BuildOutput {
    private final ImmutableList<SourcePath> resDirectories;
    private final ImmutableList<SourcePath> stringFiles;

    public BuildOutput(
        ImmutableList<SourcePath> resDirectories,
        ImmutableList<SourcePath> stringFiles) {
      this.resDirectories = resDirectories;
      this.stringFiles = stringFiles;
    }
  }

  @Nullable
  @Override
  public Path getPathToOutput() {
    return null;
  }
}
