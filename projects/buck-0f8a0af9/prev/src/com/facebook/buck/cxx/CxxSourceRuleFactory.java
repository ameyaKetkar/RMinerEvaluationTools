/*
 * Copyright 2015-present Facebook, Inc.
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

package com.facebook.buck.cxx;

import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.log.Logger;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargets;
import com.facebook.buck.model.Flavor;
import com.facebook.buck.model.ImmutableFlavor;
import com.facebook.buck.rules.BuildRule;
import com.facebook.buck.rules.BuildRuleParams;
import com.facebook.buck.rules.BuildRuleResolver;
import com.facebook.buck.rules.BuildRules;
import com.facebook.buck.rules.BuildTargetSourcePath;
import com.facebook.buck.rules.SourcePath;
import com.facebook.buck.rules.SourcePathResolver;
import com.facebook.buck.util.MoreIterables;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class CxxSourceRuleFactory {

  private static final Logger LOG = Logger.get(CxxSourceRuleFactory.class);
  private static final String COMPILE_FLAVOR_PREFIX = "compile-";
  private static final String PREPROCESS_FLAVOR_PREFIX = "preprocess-";

  private final BuildRuleParams params;
  private final BuildRuleResolver resolver;
  private final SourcePathResolver pathResolver;
  private final CxxPlatform cxxPlatform;
  private final CxxPreprocessorInput cxxPreprocessorInput;
  private final ImmutableList<String> compilerFlags;

  private final Supplier<ImmutableList<BuildRule>> preprocessDeps = Suppliers.memoize(
      new Supplier<ImmutableList<BuildRule>>() {
        @Override
        public ImmutableList<BuildRule> get() {
          return ImmutableList.<BuildRule>builder()
              // Depend on the rule that generates the sources and headers we're compiling.
              .addAll(
                  pathResolver.filterBuildRuleInputs(
                      ImmutableList.<SourcePath>builder()
                          .addAll(cxxPreprocessorInput.getIncludes().getPrefixHeaders())
                          .addAll(cxxPreprocessorInput.getIncludes().getNameToPathMap().values())
                          .build()))
              // Also add in extra deps from the preprocessor input, such as the symlink tree
              // rules.
              .addAll(
                  BuildRules.toBuildRulesFor(
                      params.getBuildTarget(),
                      resolver,
                      cxxPreprocessorInput.getRules()))
              .build();
        }
      });

  @VisibleForTesting
  public CxxSourceRuleFactory(
      BuildRuleParams params,
      BuildRuleResolver resolver,
      SourcePathResolver pathResolver,
      CxxPlatform cxxPlatform,
      CxxPreprocessorInput cxxPreprocessorInput,
      ImmutableList<String> compilerFlags) {
    this.params = params;
    this.resolver = resolver;
    this.pathResolver = pathResolver;
    this.cxxPlatform = cxxPlatform;
    this.cxxPreprocessorInput = cxxPreprocessorInput;
    this.compilerFlags = compilerFlags;
  }

  /**
   * Prefixes each of the given assembler arguments with "-Xassembler" so that the compiler
   * assembler driver will pass these arguments directly down to the linker rather than
   * interpreting them itself.
   *
   * e.g. ["--fatal-warnings"] -> ["-Xassembler", "--fatal-warnings"]
   *
   * @param args arguments for the assembler.
   * @return arguments to be passed to the compiler assembler driver.
   */
  private Iterable<String> iXassembler(Iterable<String> args) {
    return MoreIterables.zipAndConcat(
        Iterables.cycle("-Xassembler"),
        args);
  }

  private ImmutableList<String> getPreprocessFlags(CxxSource.Type type) {
    return ImmutableList.<String>builder()
        .addAll(CxxSourceTypes.getPlatformPreprocessFlags(cxxPlatform, type))
        .addAll(cxxPreprocessorInput.getPreprocessorFlags().get(type))
        .build();
  }

  /**
   * @return the preprocessed file name for the given source name.
   */
  private String getPreprocessOutputName(CxxSource.Type type, String name) {
    CxxSource.Type outputType = CxxSourceTypes.getPreprocessorOutputType(type);
    return name + "." + Iterables.get(outputType.getExtensions(), 0);
  }

  /**
   * @return a {@link BuildTarget} used for the rule that preprocesses the source by the given
   *     name and type.
   */
  @VisibleForTesting
  public BuildTarget createPreprocessBuildTarget(
      String name,
      CxxSource.Type type,
      PicType pic) {
    String outputName = Flavor.replaceInvalidCharacters(getPreprocessOutputName(type, name));
    return BuildTarget
        .builder(params.getBuildTarget())
        .addFlavors(cxxPlatform.getFlavor())
        .addFlavors(
            ImmutableFlavor.of(
                String.format(
                    PREPROCESS_FLAVOR_PREFIX + "%s%s",
                    pic == PicType.PIC ? "pic-" : "",
                    outputName)))
        .build();
  }

  public static boolean isPreprocessFlavoredBuildTarget(BuildTarget target) {
    Set<Flavor> flavors = target.getFlavors();
    for (Flavor flavor : flavors) {
      if (flavor.getName().startsWith(PREPROCESS_FLAVOR_PREFIX)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return the output path for an object file compiled from the source with the given name.
   */
  @VisibleForTesting
  Path getPreprocessOutputPath(BuildTarget target, CxxSource.Type type, String name) {
    return BuildTargets.getGenPath(target, "%s").resolve(getPreprocessOutputName(type, name));
  }

  @VisibleForTesting
  public CxxPreprocessAndCompile createPreprocessBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic) {

    Preconditions.checkArgument(CxxSourceTypes.isPreprocessableType(source.getType()));

    BuildTarget target = createPreprocessBuildTarget(name, source.getType(), pic);
    Tool tool = CxxSourceTypes.getPreprocessor(cxxPlatform, source.getType());

    // Build up the list of dependencies for this rule.
    ImmutableSortedSet<BuildRule> dependencies =
        ImmutableSortedSet.<BuildRule>naturalOrder()
            // Add dependencies on any build rules used to create the preprocessor.
            .addAll(tool.getBuildRules(pathResolver))
            // If a build rule generates our input source, add that as a dependency.
            .addAll(pathResolver.filterBuildRuleInputs(source.getPath()))
            // Depend on the rule that generates the sources and headers we're compiling.
            .addAll(preprocessDeps.get())
            .build();

    // Build up the list of extra preprocessor flags for this rule.
    ImmutableList<String> args =
        ImmutableList.<String>builder()
            // If we're using pic, add in the appropriate flag.
            .addAll(pic.getFlags())
            // Add in the source and platform specific preprocessor flags.
            .addAll(getPreprocessFlags(source.getType()))
            // Add custom per-file flags.
            .addAll(source.getFlags())
            .build();

    // Build the CxxCompile rule and add it to our sorted set of build rules.
    CxxPreprocessAndCompile result = CxxPreprocessAndCompile.preprocess(
        params.copyWithChanges(
            target,
            Suppliers.ofInstance(dependencies),
            Suppliers.ofInstance(ImmutableSortedSet.<BuildRule>of())),
        pathResolver,
        tool,
        args,
        getPreprocessOutputPath(target, source.getType(), name),
        source.getPath(),
        source.getType(),
        ImmutableList.copyOf(cxxPreprocessorInput.getIncludeRoots()),
        ImmutableList.copyOf(cxxPreprocessorInput.getSystemIncludeRoots()),
        ImmutableList.copyOf(cxxPreprocessorInput.getFrameworkRoots()),
        cxxPreprocessorInput.getIncludes(),
        cxxPlatform.getDebugPathSanitizer());
    resolver.addToIndex(result);
    return result;
  }

  @VisibleForTesting
  CxxPreprocessAndCompile requirePreprocessBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic) {

    BuildTarget target = createPreprocessBuildTarget(name, source.getType(), pic);
    Optional<CxxPreprocessAndCompile> existingRule = resolver.getRuleOptionalWithType(
        target, CxxPreprocessAndCompile.class);
    if (existingRule.isPresent()) {
      return existingRule.get();
    }

    return createPreprocessBuildRule(resolver, name, source, pic);
  }

  /**
   * @return the object file name for the given source name.
   */
  private String getCompileOutputName(String name) {
    return name + ".o";
  }

  /**
   * @return the output path for an object file compiled from the source with the given name.
   */
  @VisibleForTesting
  Path getCompileOutputPath(BuildTarget target, String name) {
    return BuildTargets.getGenPath(target, "%s").resolve(getCompileOutputName(name));
  }

  /**
   * @return a build target for a {@link CxxPreprocessAndCompile} rule for the source with the
   *    given name.
   */
  @VisibleForTesting
  public BuildTarget createCompileBuildTarget(
      String name,
      PicType pic) {
    String outputName = Flavor.replaceInvalidCharacters(getCompileOutputName(name));
    return BuildTarget
        .builder(params.getBuildTarget())
        .addFlavors(cxxPlatform.getFlavor())
        .addFlavors(
            ImmutableFlavor.of(
                String.format(
                    COMPILE_FLAVOR_PREFIX + "%s%s",
                    pic == PicType.PIC ? "pic-" : "",
                    outputName)))
        .build();
  }

  public static boolean isCompileFlavoredBuildTarget(BuildTarget target) {
    Set<Flavor> flavors = target.getFlavors();
    for (Flavor flavor : flavors) {
      if (flavor.getName().startsWith(COMPILE_FLAVOR_PREFIX)) {
        return true;
      }
    }
    return false;
  }

  // Pick the compiler to use.  Basically, if we're dealing with C++ sources, use the C++
  // compiler, and the C compiler for everything.
  private Tool getCompiler(CxxSource.Type type) {
    return CxxSourceTypes.needsCxxCompiler(type) ?
      cxxPlatform.getCxx() :
      cxxPlatform.getCc();
  }

  private ImmutableList<String> getCompileFlags(CxxSource.Type type) {
    ImmutableList.Builder<String> args = ImmutableList.builder();

    // If we're dealing with a C source that can be compiled, add the platform C compiler flags.
    if (type == CxxSource.Type.C_CPP_OUTPUT ||
        type == CxxSource.Type.OBJC_CPP_OUTPUT) {
      args.addAll(cxxPlatform.getCflags());
    }

    // If we're dealing with a C++ source that can be compiled, add the platform C++ compiler
    // flags.
    if (type == CxxSource.Type.CXX_CPP_OUTPUT ||
        type == CxxSource.Type.OBJCXX_CPP_OUTPUT) {
      args.addAll(cxxPlatform.getCxxflags());
    }

    // Add in explicit additional compiler flags, if we're compiling.
    if (type == CxxSource.Type.C_CPP_OUTPUT ||
        type == CxxSource.Type.OBJC_CPP_OUTPUT ||
        type == CxxSource.Type.CXX_CPP_OUTPUT ||
        type == CxxSource.Type.OBJCXX_CPP_OUTPUT) {
      args.addAll(compilerFlags);
    }

    // All source types require assembling, so add in platform-specific assembler flags.
    args.addAll(iXassembler(cxxPlatform.getAsflags()));

    return args.build();
  }

  /**
   * @return a {@link CxxPreprocessAndCompile} rule that preprocesses, compiles, and assembles the
   *    given {@link CxxSource}.
   */
  @VisibleForTesting
  public CxxPreprocessAndCompile createCompileBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic) {

    Preconditions.checkArgument(CxxSourceTypes.isCompilableType(source.getType()));

    BuildTarget target = createCompileBuildTarget(name, pic);
    Tool tool = getCompiler(source.getType());

    ImmutableSortedSet<BuildRule> dependencies =
        ImmutableSortedSet.<BuildRule>naturalOrder()
            // Add dependencies on any build rules used to create the compiler.
            .addAll(tool.getBuildRules(pathResolver))
            // If a build rule generates our input source, add that as a dependency.
            .addAll(pathResolver.filterBuildRuleInputs(source.getPath()))
            .build();

    // Build up the list of compiler flags.
    ImmutableList<String> args =
        ImmutableList.<String>builder()
            // If we're using pic, add in the appropriate flag.
            .addAll(pic.getFlags())
            // Add in the platform and source specific compiler flags.
            .addAll(getCompileFlags(source.getType()))
            // Add in per-source flags.
            .addAll(source.getFlags())
            .build();

    // Build the CxxCompile rule and add it to our sorted set of build rules.
    CxxPreprocessAndCompile result = CxxPreprocessAndCompile.compile(
        params.copyWithChanges(
            target,
            Suppliers.ofInstance(dependencies),
            Suppliers.ofInstance(ImmutableSortedSet.<BuildRule>of())),
        pathResolver,
        tool,
        args,
        getCompileOutputPath(target, name),
        source.getPath(),
        source.getType(),
        cxxPlatform.getDebugPathSanitizer());
    resolver.addToIndex(result);
    return result;
  }

  @VisibleForTesting
  CxxPreprocessAndCompile requireCompileBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic) {

    BuildTarget target = createCompileBuildTarget(name, pic);
    Optional<CxxPreprocessAndCompile> existingRule = resolver.getRuleOptionalWithType(
        target, CxxPreprocessAndCompile.class);
    if (existingRule.isPresent()) {
      return existingRule.get();
    }

    return createCompileBuildRule(resolver, name, source, pic);
  }

  /**
   * @return a {@link CxxPreprocessAndCompile} rule that preprocesses, compiles, and assembles the
   *    given {@link CxxSource}.
   */
  @VisibleForTesting
  public CxxPreprocessAndCompile createPreprocessAndCompileBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic,
      CxxPreprocessMode strategy) {

    Preconditions.checkArgument(CxxSourceTypes.isPreprocessableType(source.getType()));

    BuildTarget target = createCompileBuildTarget(name, pic);
    Tool tool = getCompiler(source.getType());

    ImmutableSortedSet<BuildRule> dependencies =
        ImmutableSortedSet.<BuildRule>naturalOrder()
            // Add dependencies on any build rules used to create the preprocessor.
            .addAll(tool.getBuildRules(pathResolver))
            // If a build rule generates our input source, add that as a dependency.
            .addAll(pathResolver.filterBuildRuleInputs(source.getPath()))
            // Add in all preprocessor deps.
            .addAll(preprocessDeps.get())
            .build();

    // Build up the list of compiler flags.
    ImmutableList<String> compilerFlags = ImmutableList.<String>builder()
        // If we're using pic, add in the appropriate flag.
        .addAll(pic.getFlags())
            // Add in the platform and source specific compiler flags.
        .addAll(getCompileFlags(CxxSourceTypes.getPreprocessorOutputType(source.getType())))
            // Add in per-source flags.
        .addAll(source.getFlags())
        .build();

    LOG.verbose("Creating preprocess and compile %s for %s", target, source);

    // Build the CxxCompile rule and add it to our sorted set of build rules.
    CxxPreprocessAndCompile result = CxxPreprocessAndCompile.preprocessAndCompile(
        params.copyWithChanges(
            target,
            Suppliers.ofInstance(dependencies),
            Suppliers.ofInstance(ImmutableSortedSet.<BuildRule>of())),
        pathResolver,
        tool,
        getPreprocessFlags(source.getType()),
        tool,
        compilerFlags,
        getCompileOutputPath(target, name),
        source.getPath(),
        source.getType(),
        ImmutableList.copyOf(cxxPreprocessorInput.getIncludeRoots()),
        ImmutableList.copyOf(cxxPreprocessorInput.getSystemIncludeRoots()),
        ImmutableList.copyOf(cxxPreprocessorInput.getFrameworkRoots()),
        cxxPreprocessorInput.getIncludes(), cxxPlatform.getDebugPathSanitizer(), strategy);
    resolver.addToIndex(result);
    return result;
  }

  @VisibleForTesting
  CxxPreprocessAndCompile requirePreprocessAndCompileBuildRule(
      BuildRuleResolver resolver,
      String name,
      CxxSource source,
      PicType pic,
      CxxPreprocessMode strategy) {

    BuildTarget target = createCompileBuildTarget(name, pic);
    Optional<CxxPreprocessAndCompile> existingRule = resolver.getRuleOptionalWithType(
        target, CxxPreprocessAndCompile.class);
    if (existingRule.isPresent()) {
      return existingRule.get();
    }

    return createPreprocessAndCompileBuildRule(resolver, name, source, pic, strategy);
  }

  private ImmutableMap<CxxPreprocessAndCompile, SourcePath> requirePreprocessAndCompileRules(
      BuildRuleResolver resolver,
      CxxPreprocessMode strategy,
      ImmutableMap<String, CxxSource> sources,
      PicType pic) {

    ImmutableList.Builder<CxxPreprocessAndCompile> objects = ImmutableList.builder();

    for (Map.Entry<String, CxxSource> entry : sources.entrySet()) {
      String name = entry.getKey();
      CxxSource source = entry.getValue();

      Preconditions.checkState(
          CxxSourceTypes.isPreprocessableType(source.getType()) ||
              CxxSourceTypes.isCompilableType(source.getType()));

      switch (strategy) {

        case PIPED:
        case COMBINED: {
          CxxPreprocessAndCompile rule;

          // If it's a preprocessable source, use a combine preprocess-and-compile build rule.
          // Otherwise, use a regular compile rule.
          if (CxxSourceTypes.isPreprocessableType(source.getType())) {
            rule = requirePreprocessAndCompileBuildRule(resolver, name, source, pic, strategy);
          } else {
            rule = requireCompileBuildRule(resolver, name, source, pic);
          }

          objects.add(rule);
          break;
        }

        case SEPARATE: {

          // If this is a preprocessable source, first create the preprocess build rule and
          // update the source and name to represent its compilable output.
          if (CxxSourceTypes.isPreprocessableType(source.getType())) {
            CxxPreprocessAndCompile rule = requirePreprocessBuildRule(resolver, name, source, pic);
            source = CxxSource.copyOf(source)
                .withType(CxxSourceTypes.getPreprocessorOutputType(source.getType()))
                .withPath(
                    new BuildTargetSourcePath(
                        params.getProjectFilesystem(),
                        rule.getBuildTarget()));
          }

          // Now build the compile build rule.
          CxxPreprocessAndCompile rule = requireCompileBuildRule(resolver, name, source, pic);
          objects.add(rule);

          break;
        }

        // $CASES-OMITTED$
        default:
          throw new IllegalStateException();
      }
    }

    final ProjectFilesystem projectFilesystem = params.getProjectFilesystem();
    return FluentIterable
        .from(objects.build())
        .toMap(new Function<CxxPreprocessAndCompile, SourcePath>() {
          @Override
          public SourcePath apply(CxxPreprocessAndCompile input) {
            return new BuildTargetSourcePath(projectFilesystem, input.getBuildTarget());
          }
        });
  }

  public static ImmutableMap<CxxPreprocessAndCompile, SourcePath> requirePreprocessAndCompileRules(
      BuildRuleParams params,
      BuildRuleResolver resolver,
      SourcePathResolver pathResolver,
      CxxPlatform cxxPlatform,
      CxxPreprocessorInput cxxPreprocessorInput,
      ImmutableList<String> compilerFlags,
      CxxPreprocessMode strategy,
      ImmutableMap<String, CxxSource> sources,
      PicType pic) {
    CxxSourceRuleFactory factory =
        new CxxSourceRuleFactory(
            params,
            resolver,
            pathResolver,
            cxxPlatform,
            cxxPreprocessorInput,
            compilerFlags);
    return factory.requirePreprocessAndCompileRules(resolver, strategy, sources, pic);
  }

  public enum PicType {

    // Generate position-independent code (e.g. for use in shared libraries).
    PIC("-fPIC"),

    // Generate position-dependent code.
    PDC;

    private final ImmutableList<String> flags;

    PicType(String... flags) {
      this.flags = ImmutableList.copyOf(flags);
    }

    public ImmutableList<String> getFlags() {
      return flags;
    }

  }

}
