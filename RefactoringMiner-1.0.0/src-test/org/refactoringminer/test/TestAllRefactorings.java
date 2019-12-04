package org.refactoringminer.test;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("paths.properties");
		prop.load(input);
		String outputPath = prop.getProperty("ProtoPath");

		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), RefactoringPopulator.Refactorings.All.getValue(), outputPath);
		RefactoringPopulator.feedRefactoringsInstances(RefactoringPopulator.Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations();
	}
}
