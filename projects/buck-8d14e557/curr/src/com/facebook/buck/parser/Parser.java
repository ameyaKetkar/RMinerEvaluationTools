/*
 * Copyright 2012-present Facebook, Inc.
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

package com.facebook.buck.parser;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.facebook.buck.event.AbstractBuckEvent;
import com.facebook.buck.event.BuckEvent;
import com.facebook.buck.event.BuckEventBus;
import com.facebook.buck.graph.AbstractAcyclicDepthFirstPostOrderTraversal;
import com.facebook.buck.graph.MutableDirectedGraph;
import com.facebook.buck.io.MorePaths;
import com.facebook.buck.io.ProjectFilesystem;
import com.facebook.buck.json.BuildFileParseException;
import com.facebook.buck.json.DefaultProjectBuildFileParserFactory;
import com.facebook.buck.json.JsonObjectHashing;
import com.facebook.buck.json.ProjectBuildFileParser;
import com.facebook.buck.json.ProjectBuildFileParserFactory;
import com.facebook.buck.json.ProjectBuildFileParserOptions;
import com.facebook.buck.log.Logger;
import com.facebook.buck.model.BuckVersion;
import com.facebook.buck.model.BuildFileTree;
import com.facebook.buck.model.BuildId;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargetException;
import com.facebook.buck.model.BuildTargetPattern;
import com.facebook.buck.model.FilesystemBackedBuildFileTree;
import com.facebook.buck.model.Flavored;
import com.facebook.buck.model.Pair;
import com.facebook.buck.model.UnflavoredBuildTarget;
import com.facebook.buck.rules.ActionGraph;
import com.facebook.buck.rules.BuckPyFunction;
import com.facebook.buck.rules.BuildRuleFactoryParams;
import com.facebook.buck.rules.BuildRuleType;
import com.facebook.buck.rules.ConstructorArgMarshalException;
import com.facebook.buck.rules.ConstructorArgMarshaller;
import com.facebook.buck.rules.Description;
import com.facebook.buck.rules.Repository;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.rules.TargetNode;
import com.facebook.buck.util.Console;
import com.facebook.buck.util.HumanReadableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.eventbus.Subscribe;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * High-level build file parsing machinery.  Primarily responsible for producing a
 * {@link ActionGraph} based on a set of targets.  Also exposes some low-level facilities to
 * parse individual build files. Caches build rules to minimise the number of calls to python and
 * processes filesystem WatchEvents to invalidate the cache as files change.
 */
public class Parser {

  private final CachedState state;

  private final ImmutableSet<Pattern> tempFilePatterns;

  private final Repository repository;
  private final String buildFileName;
  private final ProjectBuildFileParserFactory buildFileParserFactory;

  /**
   * Key of the meta-rule that lists the build files executed while reading rules.
   * The value is a list of strings with the root build file as the head and included
   * build files as the tail, for example: {"__includes":["/jimp/BUCK", "/jimp/buck_includes"]}
   */
  private static final String INCLUDES_META_RULE = "__includes";

  /**
   * A map from absolute included files ({@code /jimp/BUILD_DEFS}, for example) to the build files
   * that depend on them (typically {@code /jimp/BUCK} files).
   */
  private final ListMultimap<Path, Path> buildFileDependents;

  private final boolean enforceBuckPackageBoundary;


  /**
   * A BuckEvent used to record the parse start time, which should include the WatchEvent
   * processing that occurs before the BuildTargets required to build a full ParseStart event are
   * known.
   */
  private Optional<BuckEvent> parseStartEvent = Optional.absent();

  private static final Logger LOG = Logger.get(Parser.class);

  private static final ConstructorArgMarshaller marshaller = new ConstructorArgMarshaller();

  /**
   * A cached BuildFileTree which can be invalidated and lazily constructs new BuildFileTrees.
   * TODO(user): refactor this as a generic CachingSupplier<T> when it's needed elsewhere.
   */
  @VisibleForTesting
  static class BuildFileTreeCache implements Supplier<BuildFileTree> {
    private final Supplier<BuildFileTree> supplier;
    @Nullable private BuildFileTree buildFileTree;
    private BuildId currentBuildId = new BuildId();
    private BuildId buildTreeBuildId = new BuildId();

    /**
     * @param buildFileTreeSupplier each call to get() must reconstruct the tree from disk.
     */
    public BuildFileTreeCache(Supplier<BuildFileTree> buildFileTreeSupplier) {
      this.supplier = buildFileTreeSupplier;
    }

    /**
     * Invalidate the current build file tree if it was not created during this build.
     * If the BuildFileTree was created during the current build it is still valid and
     * recreating it would generate an identical tree.
     */
    public synchronized void invalidateIfStale() {
      if (!currentBuildId.equals(buildTreeBuildId)) {
        buildFileTree = null;
      }
    }

    /**
     * @return the cached BuildFileTree, or a new lazily constructed BuildFileTree.
     */
    @Override
    public synchronized BuildFileTree get() {
      if (buildFileTree == null) {
        buildTreeBuildId = currentBuildId;
        buildFileTree = supplier.get();
      }
      return buildFileTree;
    }

    /**
     * Stores the current build id, which is used to determine when the BuildFileTree is invalid.
     */
    public synchronized void onCommandStartedEvent(BuckEvent event) {
      // Ideally, the type of event would be CommandEvent.Started, but that would introduce
      // a dependency on com.facebook.buck.cli.
      Preconditions.checkArgument(event.getEventName().equals("CommandStarted"),
          "event should be of type CommandEvent.Started, but was: %s.",
          event);
      currentBuildId = event.getBuildId();
    }
  }
  private final BuildFileTreeCache buildFileTreeCache;

  public static Parser createParser(
      final Repository repository,
      String pythonInterpreter,
      boolean allowEmptyGlobs,
      boolean enforceBuckPackageBoundary,
      ImmutableSet<Pattern> tempFilePatterns,
      final String buildFileName,
      Iterable<String> defaultIncludes)
      throws IOException, InterruptedException {
    return new Parser(
        repository,
        enforceBuckPackageBoundary,
        tempFilePatterns,
        buildFileName,
        /* Calls to get() will reconstruct the build file tree by calling constructBuildFileTree. */
        // TODO(simons): Consider momoizing the suppler.
        new Supplier<BuildFileTree>() {
          @Override
          public BuildFileTree get() {
            return new FilesystemBackedBuildFileTree(
                repository.getFilesystem(),
                buildFileName);
          }
        },
        // TODO(jacko): Get rid of this global BuildTargetParser completely.
        new DefaultProjectBuildFileParserFactory(
            ProjectBuildFileParserOptions.builder()
                .setProjectRoot(repository.getFilesystem().getRootPath())
                .setPythonInterpreter(pythonInterpreter)
                .setAllowEmptyGlobs(allowEmptyGlobs)
                .setBuildFileName(buildFileName)
                .setDefaultIncludes(defaultIncludes)
                .setDescriptions(repository.getAllDescriptions())
                .build()));
  }

  /**
   * @param buildFileTreeSupplier each call to getInput() must reconstruct the build file tree from
   */
  @VisibleForTesting
  Parser(
      Repository repository,
      boolean enforceBuckPackageBoundary,
      ImmutableSet<Pattern> tempFilePatterns,
      String buildFileName,
      Supplier<BuildFileTree> buildFileTreeSupplier,
      ProjectBuildFileParserFactory buildFileParserFactory)
      throws IOException, InterruptedException {
    this.repository = repository;
    this.buildFileName = buildFileName;
    this.buildFileTreeCache = new BuildFileTreeCache(buildFileTreeSupplier);
    this.buildFileParserFactory = buildFileParserFactory;
    this.enforceBuckPackageBoundary = enforceBuckPackageBoundary;
    this.buildFileDependents = ArrayListMultimap.create();
    this.tempFilePatterns = tempFilePatterns;
    this.state = new CachedState(buildFileName);
  }

  public Path getProjectRoot() {
    return repository.getFilesystem().getRootPath();
  }

  /**
   * The rules in a build file are cached if that specific build file was parsed or all build
   * files in the project were parsed and the includes and environment haven't changed since the
   * rules were cached.
   *
   * @param buildFile the build file to look up in the {@link CachedState}.
   * @param includes the files to include before executing the build file.
   * @param env the environment to execute the build file in.
   * @return true if the build file has already been parsed and its rules are cached.
   */
  private synchronized boolean isCached(
      Path buildFile,
      Iterable<String> includes,
      ImmutableMap<String, String> env) {
    boolean includesChanged = state.invalidateCacheOnIncludeChange(includes);
    boolean environmentChanged = state.invalidateCacheOnEnvironmentChange(env);
    boolean fileParsed = state.isParsed(buildFile);
    return !includesChanged && !environmentChanged && fileParsed;
  }

  private synchronized void invalidateCache() {
    state.invalidateAll();
  }

  /**
   * Invoke this after each command to clean any parts of the cache
   * that must not be retained between commands.
   */
  public synchronized void cleanCache() {
    state.cleanCache();
  }

  public LoadingCache<BuildTarget, HashCode> getBuildTargetHashCodeCache() {
    return state.getBuildTargetHashCodeCache();
  }

  /**
   * @return a set of {@link BuildTarget} objects that this {@link TargetNodeSpec} refers to.
   */
  private ImmutableSet<BuildTarget> resolveTargetSpec(
      TargetNodeSpec spec,
      ParserConfig parserConfig,
      ProjectBuildFileParser buildFileParser,
      ImmutableMap<String, String> environment)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {

    ImmutableSet.Builder<BuildTarget> targets = ImmutableSet.builder();

    // Iterate over the build files the given target node spec returns.
    for (Path buildFile : spec.getBuildFileSpec().findBuildFiles(
        repository.getFilesystem(),
        buildFileName)) {

      // Format a proper error message for non-existent build files.
      if (!repository.getFilesystem().isFile(buildFile)) {
        throw new MissingBuildFileException(spec, buildFile);
      }

      // Build up a list of all target nodes from the build file.
      List<Map<String, Object>> parsed = parseBuildFile(
          repository.getFilesystem().resolve(buildFile),
          parserConfig,
          buildFileParser,
          environment);
      List<TargetNode<?>> nodes = Lists.newArrayListWithCapacity(parsed.size());
      for (Map<String, Object> map : parsed) {
        BuildTarget target = parseBuildTargetFromRawRule(map);
        TargetNode<?> node = getTargetNode(target);
        nodes.add(node);
      }

      // Call back into the target node spec to filter the relevant build targets.
      targets.addAll(spec.filter(nodes));
    }

    return targets.build();
  }

  private ImmutableSet<BuildTarget> resolveTargetSpecs(
      Iterable<? extends TargetNodeSpec> specs,
      ParserConfig parserConfig,
      ProjectBuildFileParser buildFileParser,
      ImmutableMap<String, String> environment)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {

    ImmutableSet.Builder<BuildTarget> targets = ImmutableSet.builder();

    for (TargetNodeSpec spec : specs) {
      targets.addAll(
          resolveTargetSpec(
              spec,
              parserConfig,
              buildFileParser,
              environment));
    }

    return targets.build();
  }

  /**
   * @param targetNodeSpecs the specs representing the build targets to generate a target graph for.
   * @param eventBus used to log events while parsing.
   * @return the target graph containing the build targets and their related targets.
   */
  public synchronized Pair<ImmutableSet<BuildTarget>, TargetGraph>
      buildTargetGraphForTargetNodeSpecs(
          Iterable<? extends TargetNodeSpec> targetNodeSpecs,
          ParserConfig parserConfig,
          BuckEventBus eventBus,
          Console console,
          ImmutableMap<String, String> environment,
          boolean enableProfiling)
          throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {

    TargetGraph graph = null;
    // TODO(jacko): Instantiating one ProjectBuildFileParser here isn't enough. We a collection of
    //              repo-specific parsers.
    try (ProjectBuildFileParser buildFileParser = buildFileParserFactory.createParser(
        console,
        environment,
        eventBus)) {
      buildFileParser.setEnableProfiling(enableProfiling);

      // Resolve the target node specs to the build targets the represent.
      ImmutableSet<BuildTarget> buildTargets = resolveTargetSpecs(
          targetNodeSpecs,
          parserConfig,
          buildFileParser,
          environment);

      postParseStartEvent(buildTargets, eventBus);

      try {
        graph = buildTargetGraph(
            buildTargets,
            parserConfig,
            buildFileParser,
            environment,
            eventBus);
        return new Pair<>(buildTargets, graph);
      } finally {
        eventBus.post(ParseEvent.finished(buildTargets, Optional.fromNullable(graph)));
      }
    }
  }

  /**
   * @param buildTargets the build targets to generate a target graph for.
   * @param eventBus used to log events while parsing.
   * @return the target graph containing the build targets and their related targets.
   */
  public TargetGraph buildTargetGraphForBuildTargets(
      Iterable<BuildTarget> buildTargets,
      ParserConfig parserConfig,
      BuckEventBus eventBus,
      Console console,
      ImmutableMap<String, String> environment,
      boolean enableProfiling)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {
    return buildTargetGraphForTargetNodeSpecs(
        Iterables.transform(
            buildTargets,
            AbstractBuildTargetSpec.TO_BUILD_TARGET_SPEC),
        parserConfig,
        eventBus,
        console,
        environment,
        enableProfiling).getSecond();
  }

  @Nullable
  public synchronized TargetNode<?> getTargetNode(BuildTarget buildTarget)
      throws IOException, InterruptedException {
    return state.get(buildTarget);
  }

  /**
   * Build a {@link TargetGraph} from the {@code toExplore} targets. Note that this graph isn't
   * pruned in any way and needs to be transformed into an {@link ActionGraph} before being useful
   * in a build. The TargetGraph is useful for commands such as
   * {@link com.facebook.buck.cli.AuditOwnerCommand} which only need to understand the relationship
   * between modules.
   *
   * @param toExplore the {@link BuildTarget}s that {@link TargetGraph} is calculated for.
   * @param buildFileParser the parser for build files.
   * @return a {@link TargetGraph} containing all the nodes from {@code toExplore}.
   */
  private synchronized TargetGraph buildTargetGraph(
      Iterable<BuildTarget> toExplore,
      final ParserConfig parserConfig,
      final ProjectBuildFileParser buildFileParser,
      final ImmutableMap<String, String> environment,
      final BuckEventBus eventBus) throws IOException, InterruptedException {

    final MutableDirectedGraph<TargetNode<?>> graph = new MutableDirectedGraph<>();

    final Optional<BuckEventBus> eventBusOptional = Optional.of(eventBus);

    AbstractAcyclicDepthFirstPostOrderTraversal<BuildTarget> traversal =
        new AbstractAcyclicDepthFirstPostOrderTraversal<BuildTarget>() {
          @Override
          protected Iterator<BuildTarget> findChildren(BuildTarget buildTarget)
              throws IOException, InterruptedException {
            eventBus.post(GetTargetDependenciesEvent.started(buildTarget));
            try {
              BuildTargetPatternParser<BuildTargetPattern> buildTargetPatternParser =
                  BuildTargetPatternParser.forBaseName(buildTarget.getBaseName());

              // Verify that the BuildTarget actually exists in the map of known BuildTargets
              // before trying to recurse through its children.
              eventBus.post(GetTargetNodeEvent.started(buildTarget));
              TargetNode<?> targetNode = state.get(buildTarget, eventBusOptional);
              eventBus.post(GetTargetNodeEvent.finished(buildTarget));
              if (targetNode == null) {
                throw new HumanReadableException(
                    NoSuchBuildTargetException.createForMissingBuildRule(
                        buildTarget,
                        buildTargetPatternParser,
                        parserConfig.getBuildFileName()));
              }

              Set<BuildTarget> deps = Sets.newHashSet();
              for (BuildTarget buildTargetForDep : targetNode.getDeps()) {
                try {
                  eventBus.post(GetTargetNodeEvent.started(buildTargetForDep));
                  TargetNode<?> depTargetNode = state.get(buildTargetForDep, eventBusOptional);
                  eventBus.post(GetTargetNodeEvent.finished(buildTargetForDep));
                  if (depTargetNode == null) {
                    parseBuildFileContainingTarget(
                        buildTargetForDep,
                        parserConfig,
                        buildFileParser,
                        environment);
                    eventBus.post(GetTargetNodeEvent.started(buildTargetForDep));
                    depTargetNode = state.get(buildTargetForDep, eventBusOptional);
                    eventBus.post(GetTargetNodeEvent.finished(buildTargetForDep));
                    if (depTargetNode == null) {
                      throw new HumanReadableException(
                          NoSuchBuildTargetException.createForMissingBuildRule(
                              buildTargetForDep,
                              BuildTargetPatternParser.forBaseName(
                                  buildTargetForDep.getBaseName()),
                              parserConfig.getBuildFileName()));
                    }
                  }
                  depTargetNode.checkVisibility(buildTarget);
                  deps.add(buildTargetForDep);
                } catch (HumanReadableException | BuildTargetException | BuildFileParseException
                             e) {
                  throw new HumanReadableException(
                      e,
                      "Couldn't get dependency '%s' of target '%s':\n%s",
                      buildTargetForDep,
                      buildTarget,
                      e.getHumanReadableErrorMessage());
                }
              }

              return deps.iterator();
            } finally {
              eventBus.post(GetTargetDependenciesEvent.finished(buildTarget));
            }
          }

          @Override
          protected void onNodeExplored(BuildTarget buildTarget)
              throws IOException, InterruptedException {
            TargetNode<?> targetNode = getTargetNode(buildTarget);
            Preconditions.checkNotNull(targetNode, "No target node found for %s", buildTarget);
            graph.addNode(targetNode);
            for (BuildTarget target : targetNode.getDeps()) {
              graph.addEdge(targetNode, Preconditions.checkNotNull(getTargetNode(target)));
            }
          }

          @Override
          protected void onTraversalComplete(Iterable<BuildTarget> nodesInExplorationOrder) {
          }
        };

    try {
      traversal.traverse(toExplore);
    } catch (AbstractAcyclicDepthFirstPostOrderTraversal.CycleException e) {
      throw new HumanReadableException(e.getMessage());
    }

    return new TargetGraph(graph);
  }

  private synchronized void parseBuildFileContainingTarget(
      BuildTarget buildTarget,
      ParserConfig parserConfig,
      ProjectBuildFileParser buildFileParser,
      ImmutableMap<String, String> environment)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {

    if (buildTarget.getRepository().isPresent()) {
      throw new HumanReadableException(
          "Buck does not currently support multiple repositories: %d",
          buildTarget);
    }
    Repository targetRepo = repository;
    Path buildFile = targetRepo.getAbsolutePathToBuildFile(buildTarget);
    if (isCached(buildFile, parserConfig.getDefaultIncludes(), environment)) {
      throw new HumanReadableException(
          "The build file that should contain %s has already been parsed (%s), " +
              "but %s was not found. Please make sure that %s is defined in %s.",
          buildTarget,
          buildFile,
          buildTarget,
          buildTarget,
          buildFile);
    }

    parseBuildFile(buildFile, parserConfig, buildFileParser, environment);
  }

  public synchronized List<Map<String, Object>> parseBuildFile(
      Path buildFile,
      ParserConfig parserConfig,
      ImmutableMap<String, String> environment,
      Console console,
      BuckEventBus buckEventBus)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {
    try (ProjectBuildFileParser projectBuildFileParser = buildFileParserFactory.createParser(
        console,
        environment,
        buckEventBus)) {
      return parseBuildFile(buildFile, parserConfig, projectBuildFileParser, environment);
    }
  }

  /**
   * @param buildFile the build file to execute to generate build rules if they are not cached.
   * @param environment the environment to execute the build file in.
   * @return a list of raw build rules generated by executing the build file.
   */
  public synchronized List<Map<String, Object>> parseBuildFile(
      Path buildFile,
      ParserConfig parserConfig,
      ProjectBuildFileParser buildFileParser,
      ImmutableMap<String, String> environment)
      throws BuildFileParseException, BuildTargetException, IOException, InterruptedException {

    if (!isCached(buildFile, parserConfig.getDefaultIncludes(), environment)) {
      LOG.debug("Parsing %s file: %s", buildFileName, buildFile);
      parseRawRulesInternal(buildFileParser.getAllRulesAndMetaRules(buildFile));
    } else {
      LOG.debug("Not parsing %s file (already in cache)", buildFileName);
    }
    return state.getRawRules(buildFile);
  }

  /**
   * @param rules the raw rule objects to parse.
   */
  @VisibleForTesting
  synchronized void parseRawRulesInternal(Iterable<Map<String, Object>> rules)
      throws BuildTargetException, IOException {
    LOG.verbose("Parsing raw rules, state before parse %s", state);
    for (Map<String, Object> map : rules) {

      if (isMetaRule(map)) {
        parseMetaRule(map);
        continue;
      }

      BuildTarget target = parseBuildTargetFromRawRule(map);
      BuildRuleType buildRuleType = parseBuildRuleTypeFromRawRule(map);
      Description<?> description = repository.getDescription(buildRuleType);
      if (description == null) {
        throw new HumanReadableException("Unrecognized rule %s while parsing %s.",
            buildRuleType,
            repository.getAbsolutePathToBuildFile(target));
      }

      state.put(target, map);
    }
    LOG.verbose("Finished parsing raw rules, state after parse %s", state);
  }

  /**
   * @param map a build rule read from a build file.
   * @return true if map represents a meta rule.
   */
  private boolean isMetaRule(Map<String, Object> map) {
    return map.containsKey(INCLUDES_META_RULE);
  }

  /**
   * Processes build file meta rules and returns true if map represents a meta rule.
   * @param map a meta rule read from a build file.
   */
  @SuppressWarnings("unchecked") // Needed for downcast from Object to List<String>.
  private synchronized boolean parseMetaRule(Map<String, Object> map) {
    Preconditions.checkState(isMetaRule(map));

    // INCLUDES_META_RULE maps to a list of file paths: the head is a
    // dependent build file and the tail is a list of the files it includes.
    List<String> fileNames = ((List<String>) map.get(INCLUDES_META_RULE));
    Preconditions.checkNotNull(fileNames);
    Path dependent = normalize(Paths.get(fileNames.get(0)));
    for (String fileName : fileNames) {
      buildFileDependents.put(normalize(Paths.get(fileName)), dependent);
    }
    return true;
  }

  /**
   * @param map the map of values that define the rule.
   * @return the type of rule defined by the map.
   */
  private BuildRuleType parseBuildRuleTypeFromRawRule(Map<String, Object> map) {
    String type = (String) Preconditions.checkNotNull(map.get(BuckPyFunction.TYPE_PROPERTY_NAME));
    return repository.getBuildRuleType(type);
  }

  /**
   * @param map the map of values that define the rule.
   * @return the build target defined by the rule.
   */
  private BuildTarget parseBuildTargetFromRawRule(Map<String, Object> map) {
    String basePath = (String) Preconditions.checkNotNull(map.get("buck.base_path"));
    String name = (String) Preconditions.checkNotNull(map.get("name"));
    return BuildTarget.builder(UnflavoredBuildTarget.BUILD_TARGET_PREFIX + basePath, name).build();
  }


  /**
   * Called when a new command is executed and used to signal to the BuildFileTreeCache
   * that reconstructing the build file tree may result in a different BuildFileTree.
   */
  @Subscribe
  public synchronized void onCommandStartedEvent(BuckEvent event) {
    // Ideally, the type of event would be CommandEvent.Started, but that would introduce
    // a dependency on com.facebook.buck.cli.
    Preconditions.checkArgument(
        event.getEventName().equals("CommandStarted"),
        "event should be of type CommandEvent.Started, but was: %s.",
        event);
    buildFileTreeCache.onCommandStartedEvent(event);
  }

  /**
   * Called when file change events are posted to the file change EventBus to invalidate cached
   * build rules if required. {@link Path}s contained within events must all be relative to the
   * {@link ProjectFilesystem} root.
   */
  @Subscribe
  public synchronized void onFileSystemChange(WatchEvent<?> event) throws IOException {
    if (LOG.isVerboseEnabled()) {
      LOG.verbose(
          "Parser watched event %s %s",
          event.kind(),
          repository.getFilesystem().createContextString(event));
    }

    if (repository.getFilesystem().isPathChangeEvent(event)) {
      Path path = (Path) event.context();

      if (isPathCreateOrDeleteEvent(event)) {

        if (path.endsWith(new ParserConfig(repository.getBuckConfig()).getBuildFileName())) {

          // If a build file has been added or removed, reconstruct the build file tree.
          buildFileTreeCache.invalidateIfStale();
        }

        // Added or removed files can affect globs, so invalidate the package build file
        // "containing" {@code path} unless its filename matches a temp file pattern.
        if (!isTempFile(path)) {
          invalidateContainingBuildFile(path);
        }
      }

      LOG.verbose("Invalidating dependents for path %s, cache state %s", path, state);

      // Invalidate the raw rules and targets dependent on this file.
      state.invalidateDependents(path);

    } else {
      // Non-path change event, likely an overflow due to many change events: invalidate everything.
      LOG.debug("Parser invalidating entire cache on overflow.");
      buildFileTreeCache.invalidateIfStale();
      invalidateCache();
    }
  }

  /**
   * @param path The {@link Path} to test.
   * @return true if {@code path} is a temporary or backup file.
   */
  private boolean isTempFile(Path path) {
    final String fileName = path.getFileName().toString();
    Predicate<Pattern> patternMatches = new Predicate<Pattern>() {
      @Override
      public boolean apply(Pattern pattern) {
        return pattern.matcher(fileName).matches();
      }
    };
    return Iterators.any(tempFilePatterns.iterator(), patternMatches);
  }

  /**
   * Finds the build file responsible for the given {@link Path} and invalidates
   * all of the cached rules dependent on it.
   * @param path A {@link Path}, relative to the project root and "contained"
   *             within the build file to find and invalidate.
   */
  private synchronized void invalidateContainingBuildFile(Path path) throws IOException {
    List<Path> packageBuildFiles = Lists.newArrayList();

    // Find the closest ancestor package for the input path.  We'll definitely need to invalidate
    // that.
    Optional<Path> packageBuildFile = buildFileTreeCache.get().getBasePathOfAncestorTarget(path);
    packageBuildFiles.addAll(packageBuildFile.asSet());

    // If we're *not* enforcing package boundary checks, it's possible for multiple ancestor
    // packages to reference the same file
    if (!enforceBuckPackageBoundary) {
      while (packageBuildFile.isPresent() && packageBuildFile.get().getParent() != null) {
        packageBuildFile =
            buildFileTreeCache.get()
                .getBasePathOfAncestorTarget(packageBuildFile.get().getParent());
        packageBuildFiles.addAll(packageBuildFile.asSet());
      }
    }

    // Invalidate all the packages we found.
    for (Path buildFile : packageBuildFiles) {
      state.invalidateDependents(
          repository.getFilesystem().getPathForRelativePath(
              buildFile.resolve(
                  new ParserConfig(repository.getBuckConfig()).getBuildFileName())));
    }
  }

  private boolean isPathCreateOrDeleteEvent(WatchEvent<?> event) {
    return event.kind() == StandardWatchEventKinds.ENTRY_CREATE ||
        event.kind() == StandardWatchEventKinds.ENTRY_DELETE;
  }

  /**
   * Always use Files created from absolute paths as they are returned from buck.py and must be
   * created from consistent paths to be looked up correctly in maps.
   * @param path A File to normalize.
   * @return An equivalent file constructed from a normalized, absolute path to the given File.
   */
  private Path normalize(Path path) {
    return repository.getFilesystem().resolve(path);
  }

  /**
   * Record the parse start time, which should include the WatchEvent processing that occurs
   * before the BuildTargets required to build a full ParseStart event are known.
   */
  public void recordParseStartTime(BuckEventBus eventBus) {
    class ParseStartTime extends AbstractBuckEvent {

      @Override
      protected String getValueString() {
        return "Timestamp.";
      }

      @Override
      public String getEventName() {
        return "ParseStartTime";
      }
    }
    parseStartEvent = Optional.<BuckEvent>of(new ParseStartTime());
    eventBus.timestamp(parseStartEvent.get());
  }

  /**
   * @return an Optional BuckEvent timestamped with the parse start time.
   */
  public Optional<BuckEvent> getParseStartTime() {
    return parseStartEvent;
  }

  /**
   * Post a ParseStart event to eventBus, using the start of WatchEvent processing as the start
   * time if applicable.
   */
  private void postParseStartEvent(Iterable<BuildTarget> buildTargets, BuckEventBus eventBus) {
    if (parseStartEvent.isPresent()) {
      eventBus.post(ParseEvent.started(buildTargets), parseStartEvent.get());
    } else {
      eventBus.post(ParseEvent.started(buildTargets));
    }
  }

  private class CachedState {

    /**
     * The build files that have been parsed and whose build rules are in
     * {@link #memoizedTargetNodes}.
     */
    private final ListMultimap<Path, Map<String, Object>> parsedBuildFiles;

    /**
     * Cache of (symlink path: symlink target) pairs used to avoid repeatedly
     * checking for the existence of symlinks in the source tree.
     */
    private final Map<Path, Path> symlinkExistenceCache;

    /**
     * Build rule input files (e.g., paths in {@code srcs}) whose
     * paths contain an element which exists in {@code symlinkExistenceCache}.
     *
     * Used to invalidate build rules in {@code cleanCache} if their
     * inputs contain any files in this set.
     */
    private final Set<Path> buildInputPathsUnderSymlink;

    /**
     * Map from build file path to targets generated by that file.
     */
    private final ListMultimap<Path, BuildTarget> pathsToBuildTargets;

    /**
     * We parse a build file in search for one particular rule; however, we also keep track of the
     * other rules that were also parsed from it.
     */
    private final Map<BuildTarget, TargetNode<?>> memoizedTargetNodes;

    /**
     * Environment used by build files. If the environment is changed, then build files need to be
     * reevaluated with the new environment, so the environment used when populating the rule cache
     * is stored between requests to parse build files and the cache is invalidated and build files
     * reevaluated if the environment changes.
     */
    @Nullable
    private ImmutableMap<String, String> cacheEnvironment;

    /**
     * Files included by build files. If the default includes are changed, then build files need to
     * be reevaluated with the new includes, so the includes used when populating the rule cache are
     * stored between requests to parse build files and the cache is invalidated and build files
     * reevaluated if the includes change.
     */
    @Nullable
    private List<String> cacheDefaultIncludes;

    private final Map<BuildTarget, Path> targetsToFile;

    private final LoadingCache<BuildTarget, HashCode> buildTargetHashCodeCache;

    private final String buildFile;

    public CachedState(String buildFileName) {
      this.memoizedTargetNodes = Maps.newHashMap();
      this.symlinkExistenceCache = Maps.newHashMap();
      this.buildInputPathsUnderSymlink = Sets.newHashSet();
      this.parsedBuildFiles = ArrayListMultimap.create();
      this.targetsToFile = Maps.newHashMap();
      this.pathsToBuildTargets = ArrayListMultimap.create();
      this.buildTargetHashCodeCache = CacheBuilder.newBuilder().build(
          new CacheLoader<BuildTarget, HashCode>() {
            @Override
            public HashCode load(BuildTarget buildTarget) throws IOException, InterruptedException {
              return loadHashCodeForBuildTarget(buildTarget);
            }
          });
      this.buildFile = buildFileName;
    }

    public void invalidateAll() {
      LOG.debug("Invalidating all cached data.");
      parsedBuildFiles.clear();
      symlinkExistenceCache.clear();
      buildInputPathsUnderSymlink.clear();
      memoizedTargetNodes.clear();
      targetsToFile.clear();
      pathsToBuildTargets.clear();
      buildTargetHashCodeCache.invalidateAll();
    }

    @Override
    public String toString() {
      return String.format(
          "%s memoized=%s symlinks=%s build files under symlink=%s parsed=%s targets-to-files=%s " +
          "paths-to-targets=%s build-target-hash-code-cache=%s",
          super.toString(),
          memoizedTargetNodes,
          symlinkExistenceCache,
          buildInputPathsUnderSymlink,
          parsedBuildFiles,
          targetsToFile,
          pathsToBuildTargets,
          buildTargetHashCodeCache);
    }

    /**
     * Invalidates the cached build rules if {@code environment} has changed since the last call.
     * If the cache is invalidated the new {@code environment} used to build the new cache is
     * stored.
     *
     * @param environment the environment to execute the build file in.
     * @return true if the cache was invalidated, false if the cache is still valid.
     */
    private synchronized boolean invalidateCacheOnEnvironmentChange(
        ImmutableMap<String, String> environment) {
      if (!environment.equals(cacheEnvironment)) {
        if (cacheEnvironment != null) {
          LOG.warn(
              "Environment variables changed (%s). Discarding cache to avoid effects on build. " +
              "This will make builds very slow.",
              Maps.difference(cacheEnvironment, environment));
        }
        invalidateCache();
        this.cacheEnvironment = environment;
        return true;
      }
      return false;
    }

    /**
     * Invalidates the cached build rules if {@code includes} have changed since the last call.
     * If the cache is invalidated the new {@code includes} used to build the new cache are stored.
     *
     * @param includes the files to include before executing the build file.
     * @return true if the cache was invalidated, false if the cache is still valid.
     */
    private synchronized boolean invalidateCacheOnIncludeChange(Iterable<String> includes) {
      List<String> includesList = Lists.newArrayList(includes);
      if (!includesList.equals(this.cacheDefaultIncludes)) {
        LOG.debug("Parser invalidating entire cache on default include change.");
        invalidateCache();
        this.cacheDefaultIncludes = includesList;
        return true;
      }
      return false;
    }

    /**
     * Remove the targets and rules defined by {@code path} from the cache and recursively remove
     * the targets and rules defined by files that transitively include {@code path} from the cache.
     * @param path The File that has changed.
     */
    synchronized void invalidateDependents(Path path) {
      // Normalize path to ensure it hashes equally with map keys.
      path = normalize(path);

      // The path may have changed from being a symlink to not being a symlink.
      symlinkExistenceCache.remove(path);

      if (parsedBuildFiles.containsKey(path)) {
        LOG.debug("Parser invalidating %s cache", path);

        // Remove all rules defined in path from cache.
        List<?> removed = parsedBuildFiles.removeAll(path);
        LOG.verbose("Removed parsed build files %s defined by %s", removed, path);

        // If this build file contained inputs under a symlink, we'll be reparsing
        // it, so forget that.
        buildInputPathsUnderSymlink.remove(path);
      } else {
        LOG.debug("Parsed build files does not contain %s, not invalidating", path);
      }

      List<BuildTarget> targetsToRemove = pathsToBuildTargets.get(path);
      LOG.debug("Removing targets %s for path %s", targetsToRemove, path);
      for (BuildTarget target : targetsToRemove) {
        memoizedTargetNodes.remove(target);
      }
      buildTargetHashCodeCache.invalidateAll(targetsToRemove);
      pathsToBuildTargets.removeAll(path);

      List<Path> dependents = buildFileDependents.get(path);
      LOG.verbose("Invalidating dependents %s of path %s", dependents, path);
      // Recursively invalidate dependents.
      for (Path dependent : dependents) {

        if (!dependent.equals(path)) {
          invalidateDependents(dependent);
        }
      }

      // Dependencies will be repopulated when files are re-parsed.
      List<?> removedDependents = buildFileDependents.removeAll(path);
      LOG.verbose("Removed build file dependents %s defined by %s", removedDependents, path);
    }

    public boolean isParsed(Path buildFile) {
      return parsedBuildFiles.containsKey(normalize(buildFile));
    }

    public List<Map<String, Object>> getRawRules(Path buildFile) {
      return Preconditions.checkNotNull(parsedBuildFiles.get(normalize(buildFile)));
    }

    public void put(BuildTarget target, Map<String, Object> rawRules) {
      Path normalized = normalize(target.getBasePath().resolve(buildFile));
      LOG.verbose("Adding rules for parsed build file %s", normalized);
      parsedBuildFiles.put(normalized, rawRules);

      targetsToFile.put(
          target,
          normalize(Paths.get((String) rawRules.get("buck.base_path")))
              .resolve(buildFile).toAbsolutePath());
    }

    @Nullable
    public TargetNode<?> get(BuildTarget buildTarget) throws IOException, InterruptedException {
      return get(buildTarget, Optional.<BuckEventBus>absent());
    }

    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    public TargetNode<?> get(
        BuildTarget buildTarget,
        Optional<BuckEventBus> eventBus) throws IOException, InterruptedException {
      // Fast path.
      TargetNode<?> toReturn = memoizedTargetNodes.get(buildTarget);
      if (toReturn != null) {
        return toReturn;
      }

      if (buildTarget.getRepository().isPresent()) {
        throw new HumanReadableException(
            "Buck does not currently support multiple repos: %d",
            buildTarget);
      }
      Repository targetRepo = repository;
      Path buildFilePath;
      try {
        buildFilePath = targetRepo.getAbsolutePathToBuildFile(buildTarget);
      } catch (Repository.MissingBuildFileException e) {
        throw new HumanReadableException(e);
      }
      UnflavoredBuildTarget unflavored = buildTarget.getUnflavoredBuildTarget();
      List<Map<String, Object>> rules = state.getRawRules(buildFilePath);
      for (Map<String, Object> map : rules) {

        if (!buildTarget.getShortName().equals(map.get("name"))) {
          continue;
        }

        BuildRuleType buildRuleType = parseBuildRuleTypeFromRawRule(map);
        targetsToFile.put(
            BuildTarget.of(unflavored),
            normalize(Paths.get((String) map.get("buck.base_path")))
                .resolve(buildFile).toAbsolutePath());

        Description<?> description = repository.getDescription(buildRuleType);
        if (description == null) {
          throw new HumanReadableException("Unrecognized rule %s while parsing %s%s.",
              buildRuleType,
              UnflavoredBuildTarget.BUILD_TARGET_PREFIX,
              MorePaths.pathWithUnixSeparators(unflavored.getBasePath().resolve(buildFile)));
        }

        if (buildTarget.isFlavored()) {
          if (description instanceof Flavored) {
            if (!((Flavored) description).hasFlavors(
                    ImmutableSet.copyOf(buildTarget.getFlavors()))) {
              throw new HumanReadableException(
                  "Unrecognized flavor in target %s while parsing %s%s.",
                  buildTarget,
                  UnflavoredBuildTarget.BUILD_TARGET_PREFIX,
                  MorePaths.pathWithUnixSeparators(
                      buildTarget.getBasePath().resolve(buildFile)));
            }
          } else {
            LOG.warn(
                "Target %s (type %s) must implement the Flavored interface " +
                "before we can check if it supports flavors: %s",
                buildTarget.getUnflavoredBuildTarget(),
                buildRuleType,
                buildTarget.getFlavors());
            throw new HumanReadableException(
                "Target %s (type %s) does not currently support flavors (tried %s)",
                buildTarget.getUnflavoredBuildTarget(),
                buildRuleType,
                buildTarget.getFlavors());
          }
        }

        this.pathsToBuildTargets.put(buildFilePath, buildTarget);

        BuildRuleFactoryParams factoryParams = new BuildRuleFactoryParams(
            targetRepo.getFilesystem(),
            // Although we store the rule by its unflavoured name, when we construct it, we need the
            // flavour.
            buildTarget,
            buildFileTreeCache.get(),
            enforceBuckPackageBoundary);
        Object constructorArg = description.createUnpopulatedConstructorArg();
        TargetNode<?> targetNode;
        try {
          ImmutableSet.Builder<BuildTarget> declaredDeps = ImmutableSet.builder();
          ImmutableSet.Builder<BuildTargetPattern> visibilityPatterns = ImmutableSet.builder();
          if (eventBus.isPresent()) {
            eventBus.get().post(MarshalConstructorArgsEvent.started(buildTarget));
          }
          marshaller.populate(
              targetRepo.getFilesystem(),
              factoryParams,
              constructorArg,
              declaredDeps,
              visibilityPatterns,
              map);
          if (eventBus.isPresent()) {
            eventBus.get().post(MarshalConstructorArgsEvent.finished(buildTarget));
          }
          if (eventBus.isPresent()) {
            eventBus.get().post(CreateTargetNodeEvent.started(buildTarget));
          }
          targetNode = new TargetNode(
              description,
              constructorArg,
              factoryParams,
              declaredDeps.build(),
              visibilityPatterns.build());
          if (eventBus.isPresent()) {
            eventBus.get().post(CreateTargetNodeEvent.finished(buildTarget));
          }
        } catch (NoSuchBuildTargetException | TargetNode.InvalidSourcePathInputException e) {
          throw new HumanReadableException(e);
        } catch (ConstructorArgMarshalException e) {
          throw new HumanReadableException("%s: %s", buildTarget, e.getMessage());
        }

        Map<Path, Path> newSymlinksEncountered = Maps.newHashMap();
        if (inputFilesUnderSymlink(
                targetNode.getInputs(),
                targetRepo.getFilesystem(),
                symlinkExistenceCache,
                newSymlinksEncountered)) {
          LOG.warn(
              "Disabling caching for target %s, because one or more input files are under a " +
              "symbolic link (%s). This will severely impact performance! To resolve this, use " +
              "separate rules and declare dependencies instead of using symbolic links.",
              targetNode.getBuildTarget(),
              newSymlinksEncountered);
          buildInputPathsUnderSymlink.add(buildFilePath);
        }
        TargetNode<?> existingTargetNode = memoizedTargetNodes.put(buildTarget, targetNode);
        if (existingTargetNode != null) {
          throw new HumanReadableException("Duplicate definition for " + unflavored);
        }

        // PMD considers it bad form to return while in a loop.
      }

      return memoizedTargetNodes.get(buildTarget);
    }

    public synchronized void cleanCache() {
      LOG.debug(
          "Cleaning cache of build files with inputs under symlink %s",
          buildInputPathsUnderSymlink);
      Set<Path> buildInputPathsUnderSymlinkCopy = new HashSet<>(buildInputPathsUnderSymlink);
      buildInputPathsUnderSymlink.clear();
      for (Path buildFilePath : buildInputPathsUnderSymlinkCopy) {
        invalidateDependents(buildFilePath);
      }
    }

    private synchronized HashCode loadHashCodeForBuildTarget(BuildTarget buildTarget)
        throws IOException, InterruptedException{
      // Warm up the cache.
      get(buildTarget);

      Path buildTargetPath = targetsToFile.get(
          BuildTarget.of(buildTarget.getUnflavoredBuildTarget()));
      if (buildTargetPath == null) {
        throw new HumanReadableException("Couldn't find build target %s", buildTarget);
      }
      List<Map<String, Object>> rules = getRawRules(buildTargetPath);
      Hasher hasher = Hashing.sha1().newHasher();
      hasher.putString(BuckVersion.getVersion(), UTF_8);
      for (Map<String, Object> map : rules) {
        if (!buildTarget.getShortName().equals(map.get("name"))) {
          continue;
        }

        JsonObjectHashing.hashJsonObject(hasher, map);
      }
      return hasher.hash();
    }

    public LoadingCache<BuildTarget, HashCode> getBuildTargetHashCodeCache() {
      return buildTargetHashCodeCache;
    }
  }

  private static boolean inputFilesUnderSymlink(
      // We use Collection<Path> instead of Iterable<Path> to prevent
      // accidentally passing in Path, since Path itself is Iterable<Path>.
      Collection<Path> inputs,
      ProjectFilesystem projectFilesystem,
      Map<Path, Path> symlinkExistenceCache,
      Map<Path, Path> newSymlinksEncountered) throws IOException {
    boolean result = false;
    for (Path input : inputs) {
      for (int i = 1; i < input.getNameCount(); i++) {
        Path subpath = input.subpath(0, i);
        Path resolvedSymlink = symlinkExistenceCache.get(subpath);
        if (resolvedSymlink != null) {
          newSymlinksEncountered.put(subpath, resolvedSymlink);
          result = true;
        } else if (projectFilesystem.isSymLink(subpath)) {
          try {
            resolvedSymlink = projectFilesystem.getRootPath().relativize(subpath.toRealPath());
            LOG.debug("Detected symbolic link %s -> %s", subpath, resolvedSymlink);
            newSymlinksEncountered.put(subpath, resolvedSymlink);
            symlinkExistenceCache.put(subpath, resolvedSymlink);
          } catch (NoSuchFileException e) {
            LOG.verbose(e, "No such file detecting symlink at %s", subpath);
          } catch (IOException e) {
            LOG.error(e, "Couldn't detect symbolic link at %s", subpath);
          }
          result = true;
        }
      }
    }
    return result;
  }

}
