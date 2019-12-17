# TSE_Evaluation_Tools
Tools used for the evaluation of RefactoringMiner 2.0

## [Oracle](http://refactoring.encs.concordia.ca/oracle/)
We used the >7000 refactorings in this oracle as the ground truth for evaluation the 5 tools for their precision and recall.

## Tools

### [RefDiff 0.1.1](https://github.com/aserg-ufmg/RefDiff/releases/tag/0.1.1)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the Git checkout process:
** if git checkout for a commit fails, try 'git checkout master -f' and then try to checkout the commit again
** If this does not work, we use the Github API to download the zip of the project at the particular commit
* Added Extract And Move method feature by :
** If the 'from' and the 'to' methods belong to different classes, we classify it as Extract And Move Method Refactoring
** Ensuring that no rename, move and move & rename class refactoring led to this conclusion

To evaluate this tool over the oracle: 
* Download the latest JSON from the [oracle](http://refactoring.encs.concordia.ca/oracle/) and update [this](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff0.1.1/refdiff-core/src-test/Data/data.json) file.
* Fill in your GitHub credentials [here](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff0.1.1/refdiff-core/github-credentials.properties) 
* Provide a path to the folder where repositories could be cloned or found, [here](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff0.1.1/refdiff-core/paths.properties)
* Execute [TestAllRefactoringsRD.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/248be92cc60a59f2980a79f6b8962cdbe86b8a80/RefDiff0.1.1/refdiff-core/src-test/test/TestAllRefactoringsRD.java#L12) to run the evaluation
* Output of the evaluation will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff0.1.1/refdiff-core/src-test/test/AnalyseResults.java) to calculate precision and recall per refactoring.

### [RefDiff 1.0](https://github.com/aserg-ufmg/RefDiff/tree/1.x)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the Git checkout process:
** if git checkout for a commit fails, try 'git checkout master -f' and then try to checkout the commit again
** If this does not work, we use the Github API to download the zip of the project at the particular commit
* Added Extract And Move method feature by :
** If the 'from' and the 'to' methods belong to different classes, we classify it as Extract And Move Method Refactoring
** Ensuring that no rename, move and move & rename class refactoring led to this conclusion

To evaluate this tool over the oracle: 
* Download the latest JSON from the [oracle](http://refactoring.encs.concordia.ca/oracle/) and update [this](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff1.0/refdiff-core/src-test/Data/data.json) file.
* Fill in your GitHub credentials [here](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff1.0/refdiff-core/github-credentials.properties) 
* Provide a path to the folder where repositories could be cloned or found, [here](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff1.0/refdiff-core/paths.properties)
* Execute [TestAllRefactoringsRD.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff1.0/refdiff-core/src-test/test/TestAllRefactoringsRD.java#L13) to run the evaluation
* Output of the evaluation will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff1.0/refdiff-core/src-test/test/AnalyseResults.java) to calculate precision and recall per refactoring.

### [RefDiff 2x](https://github.com/aserg-ufmg/RefDiff)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the tool to not report Move and/or Rename method refactorings when the method itself is abstract

To evaluate this tool over the oracle:
* Download the latest JSON from the [oracle](http://refactoring.encs.concordia.ca/oracle/) and update [this](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff2x/refdiff-evaluation/data/icse/data.json) file.
* Since this version of RefDiff does not require the entire repository, it fetches the changed files from this [folder](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/tree/master/projects)
* Execute [TestAllRefactoringsRD.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff2x/refdiff-evaluation/src/test/java/refdiff/evaluation/RefDiffVsRMiner/TestAllRefactoringsRD.java#L18) to run the evaluation
* Output of the evaluation will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff2x/refdiff-evaluation/src/test/java/refdiff/evaluation/RefDiffVsRMiner/AnalyseResults.java) to calculate precision and recall per refactoring.

### [RefactoringMiner 1.0](https://github.com/tsantalis/RefactoringMiner/releases/tag/1.0.0)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the tool to not checkout each commit, instead query the changes from using the Github API

To evaluate this tool over the oracle:
* Download the latest JSON from the [oracle](http://refactoring.encs.concordia.ca/oracle/) and update [this](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefactoringMiner-1.0.0/src-test/Data/data.json) file.
* Execute [TestAllRefactorings.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/189ce825610f94ddf23f7e60ab9270aeecd84885/RefactoringMiner-1.0.0/src-test/org/refactoringminer/test/TestAllRefactorings.java#L16) to run the evaluation
* Output of the evaluation will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefactoringMiner-1.0.0/src-test/org/refactoringminer/test/AnalyseResults.java) to calculate precision and recall per refactoring.

### [RefactoringMiner 2.0](https://github.com/tsantalis/RefactoringMiner)

To evaluate RefactoringMiner 2.0:
* Get the latest version of it from [here](https://github.com/tsantalis/RefactoringMiner)
* Follow steps similar to RefactoringMiner 1.0

### [GumTree 2.1.2](https://github.com/GumTreeDiff/gumtree/releases/tag/v2.1.2)
GumTree is used for the evaluation of Rename and Change Type refactoring.
To evaluate GumTree 2.1.2:
To evaluate this tool over the oracle:
* Download the latest JSON from the [oracle](http://refactoring.encs.concordia.ca/oracle/) and update [this](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/GumTree2.1.2/src-test/Data/data.json) file.
* Execute [TestAllRefactorings.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/GumTree2.1.2/src-test/org/refactoringminer/test/TestAllRefactorings.java#L16) to run the evaluation
* Output of the evaluation will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/GumTree2.1.2/src-test/org/refactoringminer/test/AnalyseResults.java) to calculate precision and recall per refactoring.


## Results

![RefactoringMiner and RefDiff](https://github.com/ameyaKetkar/RMinerEvaluationTools/blob/master/RMinerEvaluation1.PNG)
![RefactoringMiner and Gumtree](https://github.com/ameyaKetkar/RMinerEvaluationTools/blob/master/RMinerEvaluation2.PNG)

