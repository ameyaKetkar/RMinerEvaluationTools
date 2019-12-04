package refdiff.evaluation.RefDiffVsRMiner;

import org.junit.Test;
import refdiff.core.RefDiff;
import refdiff.evaluation.RefactoringSet;
import refdiff.evaluation.icse.IcseDataset;
import refdiff.parsers.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

public class TestAllRefactoringsRD {
	@Test
	public void testAllRefactorings() throws Exception {
		Properties prop = new Properties();
		InputStream input = new FileInputStream("paths.properties");
		prop.load(input);
		String outputPath = prop.getProperty("ProtoPath");
		String projectPath = prop.getProperty("ProjectsPath");

		projectPath = Paths.get(".").toAbsolutePath().toString().replace("RefDiff2x\\refdiff-evaluation","")+ projectPath;
		TestBuilderRD test = new TestBuilderRD(projectPath, outputPath);
		RefactoringPopulatorRD.feedRefactoringsInstances(RefactoringPopulatorRD.Refactorings.All.getValue(), RefactoringPopulatorRD.Systems.FSE.getValue(), test, outputPath);
		test.assertExpectations1();
	}
}
