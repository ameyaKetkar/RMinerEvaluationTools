package org.refactoringminer.rm1;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.github.*;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.util.GitServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitHistoryRefactoringMinerImpl implements GitHistoryRefactoringMiner {

	Logger logger = LoggerFactory.getLogger(GitHistoryRefactoringMinerImpl.class);
	private boolean analyzeMethodInvocations;
	private Set<RefactoringType> refactoringTypesToConsider = null;

	private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
		@Override
		public void write(int b) throws IOException {
		}
	};
	private GitHub gitHub;

	public GitHistoryRefactoringMinerImpl() {
		this(false);
	}

	private GitHistoryRefactoringMinerImpl(boolean analyzeMethodInvocations) {
		this.analyzeMethodInvocations = analyzeMethodInvocations;
		this.setRefactoringTypesToConsider(
			RefactoringType.RENAME_CLASS,
			RefactoringType.MOVE_CLASS,
			RefactoringType.MOVE_SOURCE_FOLDER,
			RefactoringType.RENAME_METHOD,
			RefactoringType.EXTRACT_OPERATION,
			RefactoringType.INLINE_OPERATION,
			RefactoringType.MOVE_OPERATION,
			RefactoringType.PULL_UP_OPERATION,
			RefactoringType.PUSH_DOWN_OPERATION,
			RefactoringType.MOVE_ATTRIBUTE,
			RefactoringType.PULL_UP_ATTRIBUTE,
			RefactoringType.PUSH_DOWN_ATTRIBUTE,
			RefactoringType.EXTRACT_INTERFACE,
			RefactoringType.EXTRACT_SUPERCLASS,
			RefactoringType.EXTRACT_AND_MOVE_OPERATION,
			RefactoringType.RENAME_PACKAGE
		);
	}

	public void setRefactoringTypesToConsider(RefactoringType ... types) {
		this.refactoringTypesToConsider = new HashSet<RefactoringType>();
		for (RefactoringType type : types) {
			this.refactoringTypesToConsider.add(type);
		}
	}

	private void detect(GitService gitService, Repository repository, final RefactoringHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;
		int refactoringsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();

		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				List<Refactoring> refactoringsAtRevision = detectRefactorings(gitService, repository, handler, projectFolder, currentCommit);
				refactoringsCount += refactoringsAtRevision.size();

			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				handler.handleException(currentCommit.getId().getName(),e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
			}
		}

		handler.onFinish(refactoringsCount, commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d, Refactorings: %d]", projectName, commitsCount, errorCommitsCount, refactoringsCount));
	}

	protected List<Refactoring> detectRefactorings(GitService gitService, Repository repository, final RefactoringHandler handler, File projectFolder, RevCommit currentCommit) throws Exception {
		List<Refactoring> refactoringsAtRevision;
//		String commitId = currentCommit.getId().getName();
//		List<String> filesBefore = new ArrayList<String>();
//		List<String> filesCurrent = new ArrayList<String>();
//		Map<String, String> renamedFilesHint = new HashMap<String, String>();
//		gitService.fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent, renamedFilesHint, true);
//		// If no java files changed, there is no refactoring. Also, if there are
//		// only ADD's or only REMOVE's there is no refactoring
//		if (!filesBefore.isEmpty() && !filesCurrent.isEmpty()) {
//			// Checkout and build model for parent commit
//			String parentCommit = currentCommit.getParent(0).getName();
//			gitService.checkout(repository, parentCommit);
//			long createModel1StartTimeNano = System.nanoTime();
//			UMLModel parentUMLModel = createModel(projectFolder, filesBefore);
//			long createModel1EndTimeNano = System.nanoTime();
//			// Checkout and build model for current commit
//			gitService.checkout(repository, commitId);
//			long createModel2StartTimeNano = System.nanoTime();
//			Map<String, String> map = new HashMap<>();
//			map = Files.walk(projectFolder.toPath())
//					.filter(x-> filesCurrent.stream().anyMatch(f -> x.toAbsolutePath().toString().endsWith(f)))
//					.collect(toMap(x->x.toAbsolutePath().toString(), x -> {
//						try {
//							return new String(Files.readAllBytes(x));
//						} catch (IOException e) {
//							return "";
//						}
//					}));
//			UMLModel currentUMLModel = createModel(new HashSet<>(), map);
//			long createModel2EndTimeNano = System.nanoTime();
//			long diffStartTimeNano = System.nanoTime();
//			refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
//			long diffEndTimeNano = System.nanoTime();
//			refactoringsAtRevision = filter(refactoringsAtRevision);
//			String timing = String.format("TIME: %s|%s|%s",
//					createModel1EndTimeNano - createModel1StartTimeNano,
//					createModel2EndTimeNano - createModel2StartTimeNano,
//					diffEndTimeNano - diffStartTimeNano
//			) + "\n";
//			System.out.println(timing);
//
//			try {
//				Files.write(Paths.get("D:\\MyProjects\\TSEAnalysis\\Evaluation\\RMiner1.txt"), timing.getBytes(), StandardOpenOption.APPEND);
//			}catch (Exception e){
//				System.out.println("Could Not persist timings!");
//			}
//
//		} else {
//			//logger.info(String.format("Ignored revision %s with no changes in java files", commitId));
//			refactoringsAtRevision = Collections.emptyList();
//		}
//		handler.handle(commitId, refactoringsAtRevision);
//		handler.handle(currentCommit, refactoringsAtRevision);

		return new ArrayList<>();
	}

	protected List<Refactoring> detectRefactorings(final RefactoringHandler handler, String cloneURL, String currentCommitId) {
		List<Refactoring> refactoringsAtRevision = Collections.emptyList();
		try {
			Set<String> repositoryDirectoriesBefore = ConcurrentHashMap.newKeySet();
			Set<String> repositoryDirectoriesCurrent = ConcurrentHashMap.newKeySet();
			Map<String, String> fileContentsBefore = new ConcurrentHashMap<String, String>();
			Map<String, String> fileContentsCurrent = new ConcurrentHashMap<String, String>();
			Map<String, String> renamedFilesHint = new HashMap<String, String>();
			populateWithGitHubAPI(cloneURL, currentCommitId, fileContentsBefore, fileContentsCurrent, renamedFilesHint, repositoryDirectoriesBefore, repositoryDirectoriesCurrent);
			long createModel1StartTimeNano = System.nanoTime();
			UMLModel currentUMLModel = createModel(repositoryDirectoriesCurrent, fileContentsCurrent);
			long createModel1EndTimeNano = System.nanoTime();
			long createModel2StartTimeNano = System.nanoTime();
			UMLModel parentUMLModel = createModel(repositoryDirectoriesBefore, fileContentsBefore);
			long createModel2EndTimeNano = System.nanoTime();
			// Diff between currentModel e parentModel
			long diffStartTimeNano = System.nanoTime();
			refactoringsAtRevision = parentUMLModel.diff(currentUMLModel, renamedFilesHint).getRefactorings();
			long diffEndTimeNano = System.nanoTime();
			refactoringsAtRevision = filter(refactoringsAtRevision);
			System.out.println("Number of refactorings: ");
			System.out.println(refactoringsAtRevision.size());
			String timing = String.format("TIME: %s|%s|%s",
					createModel1EndTimeNano - createModel1StartTimeNano,
					createModel2EndTimeNano - createModel2StartTimeNano,
					diffEndTimeNano - diffStartTimeNano) + "\n";
			System.out.println(timing);
		} catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", currentCommitId), e);
			handler.handleException(currentCommitId, e);
		}
		handler.handle(currentCommitId, refactoringsAtRevision);

		return refactoringsAtRevision;
	}


	private GitHub connectToGitHub() {
		if(gitHub == null) {
			try {
				Properties prop = new Properties();
				InputStream input = new FileInputStream("github-credentials.properties");
				prop.load(input);
				String username = prop.getProperty("username");
				String password = prop.getProperty("password");
				if (username != null && password != null) {
					gitHub = GitHub.connectUsingPassword(username, password);
					if(gitHub.isCredentialValid()) {
						logger.info("Connected to GitHub with account: " + username);
					}
				}
				else {
					gitHub = GitHub.connect();
				}
			} catch(FileNotFoundException e) {
				logger.warn("File github-credentials.properties was not found in RefactoringMiner's execution directory", e);
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return gitHub;
	}


	private void populateWithGitHubAPI(String cloneURL, String currentCommitId,
									   Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint,
									   Set<String> repositoryDirectoriesBefore, Set<String> repositoryDirectoriesCurrent) throws IOException, InterruptedException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		gitHub = connectToGitHub();
		//https://github.com/ is 19 chars
		String repoName = cloneURL.substring(19, cloneURL.indexOf(".git"));
		if(currentCommitId.equals("0f9d0b0bf1cd5fb58f47f22bd6d52a9fac31c530") || currentCommitId.equals("b0d5315e8ba95d099f93dc2d16339033a6525b59")){
			repoName = "vaadin/framework";
		}
		GHRepository repository = gitHub.getRepository(repoName);
		GHCommit currentCommit = repository.getCommit(currentCommitId);
		final String parentCommitId = currentCommit.getParents().get(0).getSHA1();
		Set<String> deletedAndRenamedFileParentDirectories = ConcurrentHashMap.newKeySet();
		List<GHCommit.File> commitFiles = currentCommit.getFiles();
		ExecutorService pool = Executors.newFixedThreadPool(commitFiles.size());
		for (GHCommit.File commitFile : commitFiles) {
			String fileName = commitFile.getFileName();
			if (commitFile.getFileName().endsWith(".java")) {
				if (commitFile.getStatus().equals("modified")) {
					Runnable r = () -> {
						try {
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							String rawURLInParentCommit = currentRawURL.toString().replace(currentCommitId, parentCommitId);
							InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
							String parentRawFile = IOUtils.toString(parentRawFileInputStream);
							filesBefore.put(fileName, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("added")) {
					Runnable r = () -> {
						try {
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							filesCurrent.put(fileName, currentRawFile);
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("removed")) {
					Runnable r = () -> {
						try {
							URL rawURL = commitFile.getRawUrl();
							InputStream rawFileInputStream = rawURL.openStream();
							String rawFile = IOUtils.toString(rawFileInputStream);
							filesBefore.put(fileName, rawFile);
							if(fileName.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(fileName.substring(0, fileName.lastIndexOf("/")));
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
				else if (commitFile.getStatus().equals("renamed")) {
					Runnable r = () -> {
						try {
							String previousFilename = commitFile.getPreviousFilename();
							URL currentRawURL = commitFile.getRawUrl();
							InputStream currentRawFileInputStream = currentRawURL.openStream();
							String currentRawFile = IOUtils.toString(currentRawFileInputStream);
							String rawURLInParentCommit = currentRawURL.toString().replace(currentCommitId, parentCommitId).replace(fileName, previousFilename);
							InputStream parentRawFileInputStream = new URL(rawURLInParentCommit).openStream();
							String parentRawFile = IOUtils.toString(parentRawFileInputStream);
							filesBefore.put(previousFilename, parentRawFile);
							filesCurrent.put(fileName, currentRawFile);
							renamedFilesHint.put(previousFilename, fileName);
							if(previousFilename.contains("/")) {
								deletedAndRenamedFileParentDirectories.add(previousFilename.substring(0, previousFilename.lastIndexOf("/")));
							}
						}
						catch(IOException e) {
							e.printStackTrace();
						}
					};
					pool.submit(r);
				}
			}
		}
		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		repositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent, deletedAndRenamedFileParentDirectories);
	}

	private void repositoryDirectories(GHTree tree, String pathFromRoot, Set<String> repositoryDirectories, Set<String> targetPaths) throws IOException {
		for(GHTreeEntry entry : tree.getTree()) {
			String path = null;
			if(pathFromRoot.equals("")) {
				path = entry.getPath();
			}
			else {
				path = pathFromRoot + "/" + entry.getPath();
			}
			if(atLeastOneStartsWith(targetPaths, path)) {
				if(targetPaths.contains(path)) {
					repositoryDirectories.add(path);
				}
				else {
					repositoryDirectories.add(path);
					GHTree asTree = entry.asTree();
					if(asTree != null) {
						repositoryDirectories(asTree, path, repositoryDirectories, targetPaths);
					}
				}
			}
		}
	}

	private boolean atLeastOneStartsWith(Set<String> targetPaths, String path) {
		for(String targetPath : targetPaths) {
			if(path.endsWith("/") && targetPath.startsWith(path)) {
				return true;
			}
			else if(!path.endsWith("/") && targetPath.startsWith(path + "/")) {
				return true;
			}
		}
		return false;
	}

	protected List<Refactoring> filter(List<Refactoring> refactoringsAtRevision) {
		if (this.refactoringTypesToConsider == null) {
			return refactoringsAtRevision;
		}
		List<Refactoring> filteredList = new ArrayList<Refactoring>();
		for (Refactoring ref : refactoringsAtRevision) {
			if (this.refactoringTypesToConsider.contains(ref.getRefactoringType())) {
				filteredList.add(ref);
			}
		}
		return filteredList;
	}

	@Override
	public void detectAll(Repository repository, String branch, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.createAllRevsWalk(repository, branch);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	@Override
	public void fetchAndDetectNew(Repository repository, final RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};
		RevWalk walk = gitService.fetchAndCreateNewRevsWalk(repository);
		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	protected UMLModel createModel(Set<String> repositoryDirectories, Map<String,String> files) throws Exception {
		return new UMLModelASTReader(files, repositoryDirectories).getUmlModel();
	}

	// This method only uses the Github API to get changes
	@Override
	public void detectAtCommit(String cloneUrl, String commitId, RefactoringHandler handler) {
			this.detectRefactorings(handler, cloneUrl, commitId);
	}

	@Override
	public void detectAtCommit(Repository repository, String cloneUrl, String commitId, RefactoringHandler handler) {
		String projectName = cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
		System.out.println(repository.getDirectory().getParent());
		File projectFolder = Paths.get("D:/tmp/" + projectName + "-" + commitId.substring(0, 8)).toFile();
		GitService gitService = new GitServiceImpl();
		RevWalk walk = new RevWalk(repository);
		try {
			RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			walk.parseCommit(commit.getParent(0));
			this.detectRefactorings(gitService, repository, handler, projectFolder, commit);
		}
		catch (MissingObjectException moe) {
			this.detectRefactorings(handler, cloneUrl, commitId);
		}
		catch (Exception e) {
			logger.warn(String.format("Ignored revision %s due to error", commitId), e);
			handler.handleException(commitId, e);
		} finally {
			walk.dispose();
		}
	}

	@Override
	public String getConfigId() {
	    return "RM1";
	}

	@Override
	public void detectBetweenTags(Repository repository, String startTag, String endTag, RefactoringHandler handler)
			throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};

		RevWalk walk = gitService.createRevsWalkBetweenTags(repository, startTag, endTag);

		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}

	@Override
	public void detectBetweenCommits(Repository repository, String startCommitId, String endCommitId,
			RefactoringHandler handler) throws Exception {
		GitService gitService = new GitServiceImpl() {
			@Override
			public boolean isCommitAnalyzed(String sha1) {
				return handler.skipCommit(sha1);
			}
		};

		RevWalk walk = gitService.createRevsWalkBetweenCommits(repository, startCommitId, endCommitId);

		try {
			detect(gitService, repository, handler, walk.iterator());
		} finally {
			walk.dispose();
		}
	}
}
