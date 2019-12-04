package refdiff.parsers.java;

import static org.junit.Assert.*;
import static refdiff.test.util.RastDiffMatchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import refdiff.core.diff.RastComparator;
import refdiff.core.diff.RastDiff;
import refdiff.core.diff.RelationshipType;
import refdiff.core.io.FileSystemSourceFile;
import refdiff.core.io.SourceFile;

public class TestRastComparator {
	
	private static JavaParser parser = new JavaParser();
	private static JavaSourceTokenizer tokenizer = new JavaSourceTokenizer();
	
	@Test
	public void shouldMatchExtractMethod() throws Exception {
		assertThat(diff("java"), containsOnly(
			relationship(RelationshipType.SAME, node("Foo"), node("Foo")),
			relationship(RelationshipType.SAME, node("Foo", "m1(String)"), node("Foo", "m1(String)")),
			relationship(RelationshipType.SAME, node("Foo", "m3(String)"), node("Foo", "m3(String)")),
			relationship(RelationshipType.EXTRACT, node("Foo", "m1(String)"), node("Foo", "m2()")),
			relationship(RelationshipType.EXTRACT, node("Foo", "m3(String)"), node("Foo", "m2()"))
		));
	}
	
	@Test
	public void shouldMatchPullUpAndPushDown() throws Exception {
		assertThat(diff("java2"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.SAME, node("p1.A1"), node("p1.A1")),
			relationship(RelationshipType.SAME, node("p1.A2"), node("p1.A2")),
			relationship(RelationshipType.PUSH_DOWN, node("p1.A", "m1()"), node("p1.A1", "m1()")),
			relationship(RelationshipType.PUSH_DOWN, node("p1.A", "m1()"), node("p1.A2", "m1()")),
			relationship(RelationshipType.PULL_UP, node("p1.A1", "m2(String)"), node("p1.A", "m2(String)")),
			relationship(RelationshipType.PULL_UP, node("p1.A2", "m2(String)"), node("p1.A", "m2(String)"))
		));
	}
	
	@Test
	public void shouldCompareMethodSignature() throws Exception {
		assertThat(diff("java3"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.CHANGE_SIGNATURE, node("p1.A", "m1(Integer)"), node("p1.A", "m1(Integer, boolean)")),
			relationship(RelationshipType.CHANGE_SIGNATURE, node("p1.A", "m2(Long)"), node("p1.A", "m2(Long, boolean)")),
			relationship(RelationshipType.CHANGE_SIGNATURE, node("p1.A", "m3(Double)"), node("p1.A", "m3(Double, boolean)"))
		));
	}
	
	@Test
	public void shouldMatchExtractOverloadedMethod() throws Exception {
		assertThat(diff("java4"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.SAME, node("p1.A", "fetch(Feed)"), node("p1.A", "fetch(Feed)")),
			relationship(RelationshipType.EXTRACT, node("p1.A", "fetch(Feed)"), node("p1.A", "fetch(String)"))
		));
	}
	
	@Test
	public void shouldMatchExtractSuperclass() throws Exception {
		assertThat(diff("java5"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.SAME, node("p1.A", "m1()"), node("p1.A", "m1()")),
			relationship(RelationshipType.PULL_UP, node("p1.A", "m2(int)"), node("p1.B", "m2(int)")),
			relationship(RelationshipType.EXTRACT_SUPER, node("p1.A"), node("p1.B"))
		));
	}
	
	@Test
	public void shouldNotMatchPullUpWhenSuperclassIsRenamed() throws Exception {
		assertThat(diff("java6"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.SAME, node("p1.A", "m1()"), node("p1.A", "m1()")),
			relationship(RelationshipType.SAME, node("p1.A", "m2(int)"), node("p1.A", "m2(int)")),
			relationship(RelationshipType.RENAME, node("p1.B"), node("p1.C")),
			relationship(RelationshipType.SAME, node("p1.B", "m2(int)"), node("p1.C", "m2(int)"))
		));
	}
	
	@Test
	public void shouldNotMatchExtractGetterAndSetter() throws Exception {
		assertThat(diff("java7"), containsOnly(
			relationship(RelationshipType.SAME, node("p1.A"), node("p1.A")),
			relationship(RelationshipType.SAME, node("p1.A", "m1()"), node("p1.A", "m1()")),
			relationship(RelationshipType.SAME, node("p1.A", "m2()"), node("p1.A", "m2()")),
			relationship(RelationshipType.SAME, node("p1.A", "m3()"), node("p1.A", "m3()")),
			relationship(RelationshipType.SAME, node("p1.A", "m4()"), node("p1.A", "m4()"))
		));
	}
	
	private RastDiff diff(String folder) throws Exception {
		String basePath = "test-data/diff/" + folder;
		List<SourceFile> sourceFilesBefore = getSourceFiles(Paths.get(basePath, "v0"));
		List<SourceFile> sourceFilesAfter = getSourceFiles(Paths.get(basePath, "v1"));
		RastComparator comparator = new RastComparator(parser, tokenizer);
		return comparator.compare(sourceFilesBefore, sourceFilesAfter);
	}
	
	private List<SourceFile> getSourceFiles(Path basePath) throws IOException {
		try (Stream<Path> stream = Files.walk(basePath)) {
			return stream.filter(path -> path.getFileName().toString().endsWith(".java"))
				.map(path -> new FileSystemSourceFile(basePath, basePath.relativize(path)))
				.collect(Collectors.toList());
		}
	}
	
}
