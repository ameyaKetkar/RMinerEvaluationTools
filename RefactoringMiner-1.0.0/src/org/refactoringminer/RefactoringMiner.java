package org.refactoringminer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.eclipse.jgit.lib.Repository;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

public class RefactoringMiner {

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			throw argumentException();
		}

		final String option = args[0];
		if (option.equalsIgnoreCase("-h") || option.equalsIgnoreCase("--h") || option.equalsIgnoreCase("-help")
				|| option.equalsIgnoreCase("--help")) {
			printTips();
			return;
		}

		if (option.equalsIgnoreCase("-a")) {
			detectAll(args);
		} else if (option.equalsIgnoreCase("-bc")) {
			detectBetweenCommits(args);
		} else if (option.equalsIgnoreCase("-bt")) {
			detectBetweenTags(args);
		} else if (option.equalsIgnoreCase("-c")) {
			detectAtCommit(args);
		} else {
			throw argumentException();
		}
	}

	private static void detectAll(String[] args) throws Exception {
		if (args.length > 3) {
			throw argumentException();
		}
		String folder = args[1];
		String branch = null;
		if (args.length == 3) {
			branch = args[2];
		}
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = (branch == null) ? "all_refactorings.csv" : "all_refactorings_" + branch + ".csv";
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectAll(repo, branch, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);

						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectBetweenCommits(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startCommit = args[2];
		String endCommit = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = null;
			if (endCommit == null) {
				fileName = "refactorings_" + startCommit + "_begin" + ".csv";
			} else {
				fileName = "refactorings_" + startCommit + "_" + endCommit + ".csv";
			}
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectBetweenCommits(repo, startCommit, endCommit, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);
						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectBetweenTags(String[] args) throws Exception {
		if (!(args.length == 3 || args.length == 4)) {
			throw argumentException();
		}
		String folder = args[1];
		String startTag = args[2];
		String endTag = (args.length == 4) ? args[3] : null;
		GitService gitService = new GitServiceImpl();
		try (Repository repo = gitService.openRepository(folder)) {
			Path folderPath = Paths.get(folder);
			String fileName = null;
			if (endTag == null) {
				fileName = "refactorings_" + startTag + "_begin" + ".csv";
			} else {
				fileName = "refactorings_" + startTag + "_" + endTag + ".csv";
			}
			String filePath = folderPath.toString() + "/" + fileName;
			Files.deleteIfExists(Paths.get(filePath));
			saveToFile(filePath, getResultHeader());

			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectBetweenTags(repo, startTag, endTag, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId);
						for (Refactoring ref : refactorings) {
							saveToFile(filePath, getResultRefactoringDescription(commitId, ref));
						}
					}
				}

				@Override
				public void onFinish(int refactoringsCount, int commitsCount, int errorCommitsCount) {
					System.out.println("Finish mining, result is saved to file: " + filePath);
					System.out.println(String.format("Total count: [Commits: %d, Errors: %d, Refactorings: %d]",
							commitsCount, errorCommitsCount, refactoringsCount));
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		}
	}

	private static void detectAtCommit(String[] args) throws Exception {
		if (args.length != 3) {
			throw argumentException();
		}
		String folder = args[1];
		String commitId = args[2];
		GitService gitService = new GitServiceImpl();
		//try (Repository repo = gitService.openRepository(folder)) {
			GitHistoryRefactoringMiner detector = new GitHistoryRefactoringMinerImpl();
			detector.detectAtCommit(null, null, commitId, new RefactoringHandler() {
				@Override
				public void handle(String commitId, List<Refactoring> refactorings) {
					if (refactorings.isEmpty()) {
						System.out.println("No refactorings found in commit " + commitId);
					} else {
						System.out.println(refactorings.size() + " refactorings found in commit " + commitId + ": ");
						for (Refactoring ref : refactorings) {
							System.out.println("  " + ref);
						}
					}
				}

				@Override
				public void handleException(String commit, Exception e) {
					System.err.println("Error processing commit " + commit);
					e.printStackTrace(System.err);
				}
			});
		//}
	}

	private static void printTips() {
		System.out.println("-h\t\t\t\t\t\t\t\tShow tips");
		System.out.println(
				"-a <git-repo-folder> <branch>\t\t\t\t\tDetect all refactorings at <branch> for <git-repo-folder>. If <branch> is not specified, commits from all branches are analyzed.");
		System.out.println(
				"-bc <git-repo-folder> <start-commit-sha1> <end-commit-sha1>\tDetect refactorings Between <star-commit-sha1> and <end-commit-sha1> for project <git-repo-folder>");
		System.out.println(
				"-bt <git-repo-folder> <start-tag> <end-tag>\t\t\tDetect refactorings Between <start-tag> and <end-tag> for project <git-repo-folder>");
		System.out.println(
				"-c <git-repo-folder> <commit-sha1>\t\t\t\tDetect refactorings at specified commit <commit-sha1> for project <git-repo-folder>");
	}

	private static IllegalArgumentException argumentException() {
		return new IllegalArgumentException("Type `RefactoringMiner -h` to show usage.");
	}

	private static String getResultRefactoringDescription(String commitId, Refactoring ref) {
		StringBuilder builder = new StringBuilder();
		builder.append(commitId);
		builder.append(";");
		builder.append(ref.getName());
		builder.append(";");
		builder.append(ref);
		return builder.toString();
	}

	private static void saveToFile(String fileName, String content) {
		Path path = Paths.get(fileName);
		byte[] contentBytes = (content + System.lineSeparator()).getBytes();
		try {
			Files.write(path, contentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getResultHeader() {
		return "CommitId;RefactoringType;RefactoringDetail";
	}

}
