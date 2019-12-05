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
* to run the evaluation, execute [TestAllRefactorings.testAllRefactorings](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/248be92cc60a59f2980a79f6b8962cdbe86b8a80/RefDiff0.1.1/refdiff-core/src-test/test/TestAllRefactoringsRD.java#L12)
* In the end, the output will be found in the folder named 'Output' in the working directory.
* Execute the main method in [AnalyseResults](https://github.com/ameyaKetkar/TSE_Evaluation_Tools/blob/master/RefDiff0.1.1/refdiff-core/src-test/test/AnalyseResults.java) to calculate precision and recall per refactoring.

### [RefDiff 0.1.0](https://github.com/aserg-ufmg/RefDiff/releases/tag/0.1.0)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the Git checkout process:
** if git checkout for a commit fails, try 'git checkout master -f' and then try to checkout the commit again
** If this does not work, we use the Github API to download the zip of the project at the particular commit
* Added Extract And Move method feature by :
** If the 'from' and the 'to' methods belong to different classes, we classify it as Extract And Move Method Refactoring
** Ensuring that no rename, move and move & rename class refactoring led to this conclusion



### [RefDiff 2x](https://github.com/aserg-ufmg/RefDiff)

We made the following updates to this tool:
* Updated the description format of the reported refactorings to match that of the oracle
* Updated the tool to not report Move and/or Rename method refactorings when the method itself is abstract



### [RefactoringMiner 1.0](https://github.com/tsantalis/RefactoringMiner/releases/tag/1.0.0)

### [RefactoringMiner 2.0](https://github.com/tsantalis/RefactoringMiner)
