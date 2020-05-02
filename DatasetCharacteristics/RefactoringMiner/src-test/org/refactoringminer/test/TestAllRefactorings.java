package org.refactoringminer.test;

import org.junit.Test;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.test.RefactoringPopulator.Systems;

public class TestAllRefactorings {

	@Test
	public void testAllRefactorings() throws Exception {
		TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), "/Users/ameya/Research/RMinerEvaluationTools/DatasetCharacteristics/Corpus", Refactorings.All.getValue());
		RefactoringPopulator.feedRefactoringsInstances(Refactorings.All.getValue(), Systems.FSE.getValue(), test);
		test.assertExpectations();
	}


}
