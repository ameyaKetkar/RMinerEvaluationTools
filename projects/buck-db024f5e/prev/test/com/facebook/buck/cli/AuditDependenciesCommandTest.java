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

package com.facebook.buck.cli;

import static org.junit.Assert.assertEquals;

import com.facebook.buck.java.JavaLibraryBuilder;
import com.facebook.buck.model.BuildTarget;
import com.facebook.buck.model.BuildTargetFactory;
import com.facebook.buck.rules.TargetGraph;
import com.facebook.buck.rules.TargetNode;
import com.facebook.buck.testutil.FakeProjectFilesystem;
import com.facebook.buck.testutil.TargetGraphFactory;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.nio.file.Paths;

public class AuditDependenciesCommandTest {

  private AuditDependenciesCommand auditDependenciesCommand;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws IOException, InterruptedException{
    FakeProjectFilesystem projectFilesystem = new FakeProjectFilesystem();
    projectFilesystem.touch(Paths.get("src/com/facebook/TestAndroidLibrary.java"));
    projectFilesystem.touch(Paths.get("src/com/facebook/TestAndroidLibraryTwo.java"));
    projectFilesystem.touch(Paths.get("src/com/facebook/TestJavaLibrary.java"));
    projectFilesystem.touch(Paths.get("src/com/facebook/TestJavaLibraryTwo.java"));
    projectFilesystem.touch(Paths.get("src/com/facebook/TestJavaLibraryThree.java"));

    auditDependenciesCommand = new AuditDependenciesCommand();
  }

  @Test
  public void testGetTransitiveDependenciesWalksTheGraph() throws IOException {
    BuildTarget javaTarget = BuildTargetFactory.newInstance("//:test-java-library");
    TargetNode<?> javaNode = JavaLibraryBuilder
        .createBuilder(javaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibrary.java"))
        .build();

    BuildTarget secondLibraryTarget = BuildTargetFactory.newInstance("//:test-android-library-two");
    TargetNode<?> secondLibraryNode = JavaLibraryBuilder
        .createBuilder(secondLibraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibraryTwo.java"))
        .addDep(javaTarget)
        .build();

    BuildTarget libraryTarget = BuildTargetFactory.newInstance("//:test-android-library");
    TargetNode<?> libraryNode = JavaLibraryBuilder
        .createBuilder(libraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibrary.java"))
        .addDep(secondLibraryTarget)
        .build();

    ImmutableSet<TargetNode<?>> nodes = ImmutableSet.of(javaNode, libraryNode, secondLibraryNode);
    TargetGraph targetGraph = TargetGraphFactory.newInstance(nodes);

    ImmutableSet<BuildTarget> testInput = ImmutableSet.of(libraryTarget);
    ImmutableSet<BuildTarget> transitiveDependencies =
        auditDependenciesCommand.getTransitiveDependencies(testInput, targetGraph);
    assertEquals(ImmutableSet.of(secondLibraryTarget, javaTarget), transitiveDependencies);
  }

  @Test
  public void testGetImmediateDependenciesDoesntReturnTransitiveDependencies() throws IOException {
    BuildTarget javaTarget = BuildTargetFactory.newInstance("//:test-java-library");
    TargetNode<?> javaNode = JavaLibraryBuilder
        .createBuilder(javaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibrary.java"))
        .build();

    BuildTarget secondLibraryTarget = BuildTargetFactory.newInstance("//:test-android-library-two");
    TargetNode<?> secondLibraryNode = JavaLibraryBuilder
        .createBuilder(secondLibraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibraryTwo.java"))
        .addDep(javaTarget)
        .build();

    BuildTarget libraryTarget = BuildTargetFactory.newInstance("//:test-android-library");
    TargetNode<?> libraryNode = JavaLibraryBuilder
        .createBuilder(libraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibrary.java"))
        .addDep(secondLibraryTarget)
        .build();

    ImmutableSet<TargetNode<?>> nodes = ImmutableSet.of(javaNode, libraryNode, secondLibraryNode);
    TargetGraph targetGraph = TargetGraphFactory.newInstance(nodes);

    ImmutableSet<BuildTarget> immediateDependencies =
        auditDependenciesCommand.getImmediateDependencies(libraryTarget, targetGraph);
    assertEquals(ImmutableSet.of(secondLibraryTarget), immediateDependencies);
  }

  @Test
  public void testGetTransitiveDependenciesWithMultipleInputsReturnsAllDependencies()
      throws IOException {
    BuildTarget thirdJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-three");
    TargetNode<?> thirdJavaNode = JavaLibraryBuilder
        .createBuilder(thirdJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryThree.java"))
        .build();

    BuildTarget secondJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-two");
    TargetNode<?> secondJavaNode = JavaLibraryBuilder
        .createBuilder(secondJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryTwo.java"))
        .addDep(thirdJavaTarget)
        .build();

    BuildTarget javaTarget = BuildTargetFactory.newInstance("//:test-java-library");
    TargetNode<?> javaNode = JavaLibraryBuilder
        .createBuilder(javaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibrary.java"))
        .addDep(secondJavaTarget)
        .build();

    BuildTarget secondLibraryTarget = BuildTargetFactory.newInstance("//:test-android-library-two");
    TargetNode<?> secondLibraryNode = JavaLibraryBuilder
        .createBuilder(secondLibraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibraryTwo.java"))
        .build();

    BuildTarget libraryTarget = BuildTargetFactory.newInstance("//:test-android-library");
    TargetNode<?> libraryNode = JavaLibraryBuilder
        .createBuilder(libraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibrary.java"))
        .addDep(secondLibraryTarget)
        .build();

    ImmutableSet<TargetNode<?>> nodes =
        ImmutableSet.of(javaNode, secondJavaNode, thirdJavaNode, libraryNode, secondLibraryNode);
    TargetGraph targetGraph = TargetGraphFactory.newInstance(nodes);

    ImmutableSet<BuildTarget> testInput = ImmutableSet.of(libraryTarget, javaTarget);
    ImmutableSet<BuildTarget> transitiveDependencies =
        auditDependenciesCommand.getTransitiveDependencies(testInput, targetGraph);
    ImmutableSet<BuildTarget> expectedOutput =
        ImmutableSet.of(secondLibraryTarget, secondJavaTarget, thirdJavaTarget);
    assertEquals(expectedOutput, transitiveDependencies);
  }

  @Test
  public void testGetImmediateDependenciesIncludesExtraDependencies() throws IOException {
    BuildTarget thirdJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-three");
    TargetNode<?> thirdJavaNode = JavaLibraryBuilder
        .createBuilder(thirdJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryThree.java"))
        .build();

    BuildTarget secondJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-two");
    TargetNode<?> secondJavaNode = JavaLibraryBuilder
        .createBuilder(secondJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryTwo.java"))
        .build();

    BuildTarget javaTarget = BuildTargetFactory.newInstance("//:test-java-library");
    TargetNode<?> javaNode = JavaLibraryBuilder
        .createBuilder(javaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibrary.java"))
        .addExportedDep(thirdJavaTarget)
        .addDep(secondJavaTarget)
        .build();

    ImmutableSet<TargetNode<?>> nodes =
        ImmutableSet.of(javaNode, secondJavaNode, thirdJavaNode);
    TargetGraph targetGraph = TargetGraphFactory.newInstance(nodes);

    ImmutableSet<BuildTarget> transitiveDependencies =
        auditDependenciesCommand.getImmediateDependencies(javaTarget, targetGraph);
    ImmutableSet<BuildTarget> expectedOutput =
        ImmutableSet.of(secondJavaTarget, thirdJavaTarget);
    assertEquals(expectedOutput, transitiveDependencies);
  }

  @Test
  public void testGetTransitiveDependenciesIncludesExtraDependencies() throws IOException {
    BuildTarget thirdJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-three");
    TargetNode<?> thirdJavaNode = JavaLibraryBuilder
        .createBuilder(thirdJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryThree.java"))
        .build();

    BuildTarget secondJavaTarget = BuildTargetFactory.newInstance("//:test-java-library-two");
    TargetNode<?> secondJavaNode = JavaLibraryBuilder
        .createBuilder(secondJavaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibraryTwo.java"))
        .build();

    BuildTarget javaTarget = BuildTargetFactory.newInstance("//:test-java-library");
    TargetNode<?> javaNode = JavaLibraryBuilder
        .createBuilder(javaTarget)
        .addSrc(Paths.get("src/com/facebook/TestJavaLibrary.java"))
        .addExportedDep(thirdJavaTarget)
        .addDep(secondJavaTarget)
        .build();

    BuildTarget secondLibraryTarget = BuildTargetFactory.newInstance("//:test-android-library-two");
    TargetNode<?> secondLibraryNode = JavaLibraryBuilder
        .createBuilder(secondLibraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibraryTwo.java"))
        .addExportedDep(javaTarget)
        .build();

    BuildTarget libraryTarget = BuildTargetFactory.newInstance("//:test-android-library");
    TargetNode<?> libraryNode = JavaLibraryBuilder
        .createBuilder(libraryTarget)
        .addSrc(Paths.get("src/com/facebook/TestAndroidLibrary.java"))
        .addDep(secondLibraryTarget)
        .build();

    ImmutableSet<TargetNode<?>> nodes =
        ImmutableSet.of(javaNode, secondJavaNode, thirdJavaNode, libraryNode, secondLibraryNode);
    TargetGraph targetGraph = TargetGraphFactory.newInstance(nodes);

    ImmutableSet<BuildTarget> testInput = ImmutableSet.of(libraryTarget);
    ImmutableSet<BuildTarget> transitiveDependencies =
        auditDependenciesCommand.getTransitiveDependencies(testInput, targetGraph);
    ImmutableSet<BuildTarget> expectedOutput =
        ImmutableSet.of(secondLibraryTarget, javaTarget, secondJavaTarget, thirdJavaTarget);
    assertEquals(expectedOutput, transitiveDependencies);
  }

}
