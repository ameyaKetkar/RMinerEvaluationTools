package test;

import org.junit.Test;
import refdiff.core.RefDiff;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class TestAllRefactoringsRD {
	@Test
	public void testAllRefactorings() throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("paths.properties");
		prop.load(input);
		String outputPath = prop.getProperty("ProtoPath");
		String projectPath = prop.getProperty("ProjectsPath");
		TestBuilderRD test = new TestBuilderRD(new RefDiff(), projectPath, outputPath);
		RefactoringPopulatorRD.feedRefactoringsInstances(RefactoringPopulatorRD.Refactorings.All.getValue(), RefactoringPopulatorRD.Systems.FSE.getValue(), test, outputPath);
		test.assertExpectations();
	}
}
