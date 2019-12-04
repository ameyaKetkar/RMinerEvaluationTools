package refdiff.core.rm2.analysis;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javafx.scene.shape.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import refdiff.core.api.GitService;
import refdiff.core.rm2.model.SDModel;
import refdiff.core.util.GitServiceImpl;

import static java.util.stream.Collectors.*;

public class GitHistoryStructuralDiffAnalyzer {

	Logger logger = LoggerFactory.getLogger(GitHistoryStructuralDiffAnalyzer.class);
	private final RefDiffConfig config;
	private GitHub gitHub;

	public GitHistoryStructuralDiffAnalyzer() {
		this(new RefDiffConfigImpl());
        gitHub = connectToGitHub();
	}

	public GitHistoryStructuralDiffAnalyzer(RefDiffConfig config) {
		this.config = config;
	}

	private void detect(GitService gitService, Repository repository, final StructuralDiffHandler handler, Iterator<RevCommit> i) {
		int commitsCount = 0;
		int errorCommitsCount = 0;

		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		String projectName = projectFolder.getName();

		long time = System.currentTimeMillis();
		while (i.hasNext()) {
			RevCommit currentCommit = i.next();
			try {
				detectRefactorings(gitService, repository, handler, projectFolder, currentCommit);
			} catch (Exception e) {
				logger.warn(String.format("Ignored revision %s due to error", currentCommit.getId().getName()), e);
				handler.handleException(currentCommit.getId().getName(), e);
				errorCommitsCount++;
			}

			commitsCount++;
			long time2 = System.currentTimeMillis();
			if ((time2 - time) > 20000) {
				time = time2;
				logger.info(String.format("Processing %s [Commits: %d, Errors: %d]", projectName, commitsCount, errorCommitsCount));
			}
		}

		handler.onFinish(commitsCount, errorCommitsCount);
		logger.info(String.format("Analyzed %s [Commits: %d, Errors: %d]", projectName, commitsCount, errorCommitsCount));
	}

	public void detectAll(Repository repository, String branch, final StructuralDiffHandler handler) throws Exception {
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

	public void fetchAndDetectNew(Repository repository, final StructuralDiffHandler handler) throws Exception {
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

	public void detectAtCommit(Repository repository, String commitId, StructuralDiffHandler handler) {
		String cloneURL = repository.getConfig().getString("remote", "origin", "url");
		File metadataFolder = repository.getDirectory();
		File projectFolder = metadataFolder.getParentFile();
		GitService gitService = new GitServiceImpl();
		//RevWalk walk = new RevWalk(repository);
		try (RevWalk walk = new RevWalk(repository)) {
				RevCommit commit = walk.parseCommit(repository.resolve(commitId));
			if (commit.getParentCount() == 1) {
				RevCommit parentCommit = walk.parseCommit(commit.getParent(0));
				File f = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + commitId);
				File f1 = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + parentCommit.getId().getName());
				if(f.exists() && f1.exists() ){
					if(Files.walk(f1.toPath()).anyMatch(x -> x.toString().endsWith(".java")) && Files.walk(f.toPath()).anyMatch(x -> x.toString().endsWith(".java"))) {
						this.detectRefactorings(handler, projectFolder, cloneURL, commitId);
					}else{
						boolean b = Files.deleteIfExists(f1.toPath()) && Files.deleteIfExists(f.toPath());
						this.detectRefactorings(gitService, repository, handler, projectFolder, commit);
					}
				}else {
					this.detectRefactorings(gitService, repository, handler, projectFolder, commit);
				}
			}
		}catch (MissingObjectException | FileNotFoundException | JGitInternalException | CheckoutConflictException moe) {
			this.detectRefactorings(handler, projectFolder, cloneURL, commitId);
		}
		catch (Exception e) {
			try {
				this.detectRefactorings(handler, projectFolder, cloneURL, commitId);
			}catch (Exception ex){
				e.printStackTrace();
				logger.warn(String.format("Ignored revision %s due to error", commitId), e);
				handler.handleException(commitId, ex);
			}
		}
	}

	private void detectRefactorings(StructuralDiffHandler handler, File projectFolder, String cloneURL, String commitId)  {
//		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<>();
		List<String> filesCurrent = new ArrayList<>();
		Map<String, String> renamedFilesHint = new HashMap<>();
		try {
			String parentCommitId = populateWithGitHubAPI(cloneURL, commitId, filesBefore, filesCurrent, renamedFilesHint);
			// If no java files changed, there is no refactoring. Also, if there are
			// only ADD's or only REMOVE's there is no refactoring

			SDModelBuilder builder = new SDModelBuilder(config);
			if (filesBefore.isEmpty() || filesCurrent.isEmpty()) {
				return;
			}

			// Checkout and build model for current commit
			File folderAfter = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + commitId);
			if (!folderAfter.exists()) {
				downloadAndExtractZipFile(projectFolder, cloneURL,commitId);
			}
//	    if (folderAfter.exists()) {
//	        logger.info(String.format("Analyzing code after (%s) ...", commitId));
//	        builder.analyzeAfter(folderAfter, filesCurrent);
//	    } else {
			//gitService.checkout(repository, commitId);
			logger.info(String.format("Analyzing code after (%s) ...", commitId));

			long createModel1StartTimeNano = System.nanoTime();
			long createModel1StartTimeMilli = System.currentTimeMillis();

			builder.analyzeAfter(folderAfter, filesCurrent);

			long createModel1EndTimeNano = System.nanoTime();
			long createModel1EndTimeMilli = System.currentTimeMillis();

			File folderBefore = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + parentCommitId);

			if (!folderBefore.exists()) {
				downloadAndExtractZipFile(projectFolder, cloneURL, parentCommitId);
			}
			long createModel2StartTimeNano = System.nanoTime();
			long createModel2StartTimeMilli = System.currentTimeMillis();

			builder.analyzeBefore(folderBefore, filesBefore);

			long createModel2EndTimeNano = System.nanoTime();
			long createModel2EndTimeMilli = System.currentTimeMillis();
			long diffStartTimeNano = System.nanoTime();
			long diffStartTimeMilli = System.currentTimeMillis();
			final SDModel model = builder.buildModel();
			long diffEndTimeNano = System.nanoTime();
			long diffEndTimeMilli = System.currentTimeMillis();



			String timing = String.format("TIME: %s|%s|%s|%s|%s|%s|%s",
					commitId,
					createModel1EndTimeNano - createModel1StartTimeNano,
					createModel1EndTimeMilli - createModel1StartTimeMilli,
					createModel2EndTimeNano - createModel2StartTimeNano,
					createModel2EndTimeMilli - createModel2StartTimeMilli,
					diffEndTimeNano - diffStartTimeNano,
					diffEndTimeMilli - diffStartTimeMilli
			)+ "\n";
			System.out.println(timing);
			handler.handle(commitId, model);
		}catch (Exception e){
			e.printStackTrace();
			System.out.println("Could not read the local folder containing the specific commit!");
		}
	}

	protected void detectRefactorings(GitService gitService, Repository repository, final StructuralDiffHandler handler, File projectFolder, RevCommit currentCommit) throws Exception {
		String commitId = currentCommit.getId().getName();
		List<String> filesBefore = new ArrayList<String>();
		List<String> filesCurrent = new ArrayList<String>();
		Map<String, String> renamedFilesHint = new HashMap<String, String>();
		gitService.fileTreeDiff(repository, currentCommit, filesBefore, filesCurrent, renamedFilesHint, false);
		// If no java files changed, there is no refactoring. Also, if there are
		// only ADD's or only REMOVE's there is no refactoring

		SDModelBuilder builder = new SDModelBuilder(config);
		if (filesBefore.isEmpty() || filesCurrent.isEmpty()) {
			return;
		}

		gitService.checkout(repository, commitId);
		logger.info(String.format("Analyzing code after (%s) ...", commitId));

		long createModel1StartTimeNano = System.nanoTime();
		long createModel1StartTimeMilli = System.currentTimeMillis();

		builder.analyzeAfter(projectFolder, filesCurrent);
		long createModel1EndTimeNano = System.nanoTime();
		long createModel1EndTimeMilli = System.currentTimeMillis();

		String parentCommit = currentCommit.getParent(0).getName();

		gitService.checkout(repository, parentCommit);
		logger.info(String.format("Analyzing code before (%s) ...", parentCommit));

		long createModel2StartTimeNano = System.nanoTime();
		long createModel2StartTimeMilli = System.currentTimeMillis();

		builder.analyzeBefore(projectFolder, filesBefore);
		long createModel2EndTimeNano = System.nanoTime();
		long createModel2EndTimeMilli = System.currentTimeMillis();

		long diffStartTimeNano = System.nanoTime();
		long diffStartTimeMilli = System.currentTimeMillis();

		final SDModel model = builder.buildModel();

		long diffEndTimeNano = System.nanoTime();
		long diffEndTimeMilli = System.currentTimeMillis();

		String timing = String.format("TIME: %s|%s|%s|%s|%s|%s|%s",
				currentCommit.getId().getName(),
				createModel1EndTimeNano - createModel1StartTimeNano,
				createModel1EndTimeMilli - createModel1StartTimeMilli,
				createModel2EndTimeNano - createModel2StartTimeNano,
				createModel2EndTimeMilli - createModel2StartTimeMilli,
				diffEndTimeNano - diffStartTimeNano,
				diffEndTimeMilli - diffStartTimeMilli
		) + "\n";
		System.out.println(timing);

		handler.handle(currentCommit.getId().getName(), model);
	}


	private void downloadAndExtractZipFile(File projectFolder, String cloneURL, String commitId)
			throws IOException {
		String downloadLink = cloneURL.substring(0, cloneURL.indexOf(".git")) + "/archive/" + commitId + ".zip";
		File destinationFile = new File(projectFolder.getParentFile(), projectFolder.getName() + "-" + commitId + ".zip");
		logger.info(String.format("Downloading archive %s", downloadLink));
		FileUtils.copyURLToFile(new URL(downloadLink), destinationFile);
		logger.info(String.format("Unzipping archive %s", downloadLink));
		java.util.zip.ZipFile zipFile = new ZipFile(destinationFile);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File entryDestination = new File(projectFolder.getParentFile(),  entry.getName());
				if (entry.isDirectory()) {
					entryDestination.mkdirs();
				} else {
					entryDestination.getParentFile().mkdirs();
					InputStream in = zipFile.getInputStream(entry);
					OutputStream out = new FileOutputStream(entryDestination);
					IOUtils.copy(in, out);
					IOUtils.closeQuietly(in);
					out.close();
				}
			}
		} finally {
			zipFile.close();
		}
	}


	public String getPArentcommitFromGithub(String cloneURL, String currentCommitId) throws IOException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		GitHub gitHub = connectToGitHub();
		//https://github.com/ is 19 chars
		String repoName = cloneURL.substring(19, cloneURL.indexOf(".git"));
		GHRepository repository = gitHub.getRepository(repoName);
		GHCommit commit = repository.getCommit(currentCommitId);
		return commit.getParents().get(0).getSHA1();
	}

//	public String getParentCommitId(String cloneURL, String currentCommitId) throws IOException, InterruptedException {
//		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
//		GitHub gitHub = connectToGitHub();
//		//https://github.com/ is 19 chars
//		String repoName = cloneURL.substring(19, cloneURL.indexOf(".git"));
//		GHRepository repository = gitHub.getRepository(repoName);
//		GHCommit currentCommit = repository.getCommit(currentCommitId);
//		return currentCommit.getParents().get(0).getSHA1();
//	}

	public String populateWithGitHubAPI(String cloneURL, String currentCommitId,
									   Map<String, String> filesBefore, Map<String, String> filesCurrent, Map<String, String> renamedFilesHint) throws IOException, InterruptedException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
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
        System.out.println(commitFiles.stream().collect(groupingBy(GHCommit.File::getStatus, counting())));
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
		return parentCommitId;
		//repositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent, deletedAndRenamedFileParentDirectories);
		//allRepositoryDirectories(currentCommit.getTree(), "", repositoryDirectoriesCurrent);
		//GHCommit parentCommit = repository.getCommit(parentCommitId);
		//allRepositoryDirectories(parentCommit.getTree(), "", repositoryDirectoriesBefore);
	}


	public String populateWithGitHubAPI(String cloneURL, String currentCommitId,
										 List<String> filesBefore, List<String> filesCurrent, Map<String, String> renamedFilesHint) throws IOException {
		logger.info("Processing {} {} ...", cloneURL, currentCommitId);
		GitHub gitHub = connectToGitHub();
		//https://github.com/ is 19 chars
		String repoName = cloneURL.substring(19, cloneURL.indexOf(".git"));
		GHRepository repository = gitHub.getRepository(repoName);
		GHCommit commit = repository.getCommit(currentCommitId);
		String parentCommitId = commit.getParents().get(0).getSHA1();
		List<GHCommit.File> commitFiles = commit.getFiles();
		for (GHCommit.File commitFile : commitFiles) {
			if (commitFile.getFileName().endsWith(".java")) {
				if (commitFile.getStatus().equals("modified")) {
					filesBefore.add(commitFile.getFileName());
					filesCurrent.add(commitFile.getFileName());
				}
				else if (commitFile.getStatus().equals("added")) {
					filesCurrent.add(commitFile.getFileName());
				}
				else if (commitFile.getStatus().equals("removed")) {
					filesBefore.add(commitFile.getFileName());
				}
				else if (commitFile.getStatus().equals("renamed")) {
					filesBefore.add(commitFile.getPreviousFilename());
					filesCurrent.add(commitFile.getFileName());
					renamedFilesHint.put(commitFile.getPreviousFilename(), commitFile.getFileName());
				}
			}
		}
		return parentCommitId;
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

}
