package refdiff.core.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import refdiff.core.util.PairBeforeAfter;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


public class GitHelper {
	
	private static final String REMOTE_REFS_PREFIX = "refs/remotes/origin/";

	
	DefaultCommitsFilter commitsFilter = new DefaultCommitsFilter();

	public static Repository cloneIfNotExists(String projectPath, String cloneUrl) throws Exception {
		File folder = new File(projectPath);
		Repository repository;
		if (folder.exists()) {
			RepositoryBuilder builder = new RepositoryBuilder();
			repository = builder
				.setGitDir(new File(folder, ".git"))
				.readEnvironment()
				.findGitDir()
				.build();
			
		} else {
			Git git = Git.cloneRepository()
				.setDirectory(folder)
				.setURI(cloneUrl)
				.setCloneAllBranches(true)
				.call();
			repository = git.getRepository();
		}
		return repository;
	}
	
	public static File cloneBareRepository(File folder, String cloneUrl) {
		if (!folder.exists()) {
			System.out.print("Cloning " + cloneUrl + "...");
			try {
				Git.cloneRepository()
					.setURI(cloneUrl)
					.setDirectory(folder)
					.setBare(true)
					.setCloneAllBranches(true)
					.call();
				System.out.println(" DONE");
			} catch (Exception e) {
				throw new RuntimeException(String.format("Unable to clone %s, cause: %s", cloneUrl, e.getMessage()), e);
			}
		}
		return folder;
	}
	
	public static Repository openRepository(String repositoryPath) throws Exception {
		return openRepository(new File(repositoryPath, ".git"));
	}
	
	public static Repository openRepository(File repositoryPath) {
		try {
			if (repositoryPath.exists()) {
				RepositoryBuilder builder = new RepositoryBuilder();
				Repository repository = builder
					.setGitDir(repositoryPath)
					.readEnvironment()
					.findGitDir()
					.build();
				return repository;
			} else {
				throw new FileNotFoundException(repositoryPath.getPath());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void forEachNonMergeCommit(Repository repo, String startAt, int maxDepth, BiConsumer<RevCommit, RevCommit> function) {
		try (RevWalk revWalk = new RevWalk(repo)) {
			RevCommit head = revWalk.parseCommit(repo.resolve(startAt));
			revWalk.markStart(head);
			revWalk.setRevFilter(RevFilter.NO_MERGES);
			
			int count = 0;
			for (RevCommit commit : revWalk) {
				if (commit.getParentCount() == 1) {
					function.accept(commit.getParent(0), commit);
				}
				
				count++;
				
				if (count >= maxDepth)
					break;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void forEachNonMergeCommit(Repository repo, int maxDepth, BiConsumer<RevCommit, RevCommit> function) {
		forEachNonMergeCommit(repo, "HEAD", maxDepth, function);
	}
	
//	public static void checkout(Repository repository, String commitId) throws Exception {
//		try (Git git = new Git(repository)) {
//			CheckoutCommand checkout = git.checkout().setName(commitId);
//			checkout.call();
//		}
//		// File workingDir = repository.getDirectory().getParentFile();
//		// ExternalProcess.execute(workingDir, "git", "checkout", commitId);
//	}
//
//	public RevCommit resolveCommit(Repository repository, String commitId) throws Exception {
//		ObjectId oid = repository.resolve(commitId);
//		if (oid == null) {
//			return null;
//		}
//		try (RevWalk rw = new RevWalk(repository)) {
//			return rw.parseCommit(oid);
//		} catch (MissingObjectException e) {
//			return null;
//		}
//	}

	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommitMissingObject(String commitBefore, String commitAfter, Path b4, Path aftr, String prjectName) {
		try {
			Map<SourceFile, String> filesBefore = Files.walk(b4)
					.filter(x -> x.toString().endsWith(".java"))
					.collect(Collectors.toMap(SourceFile::new, x -> {
						try {
							return String.join("\n", Files.readString(x));
						} catch (IOException e) {
							System.out.println(x.toAbsolutePath().toString());
							e.printStackTrace();
						}
						return "";
					}));

			GitSourceTree gitSourceTreeB4 = new GitSourceTree(prjectName, commitBefore, filesBefore, b4);
			Map<SourceFile, String> filesAfter = Files.walk(aftr)
					.filter(x -> x.toString().endsWith(".java"))
					.collect(Collectors.toMap(SourceFile::new, x -> { try { return String.join("\n", Files.readString(x)); }
					catch (IOException e) { return ""; }
					})).entrySet().stream().filter(x -> !x.getValue().isEmpty()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

			GitSourceTree gitSourceTreeAftr = new GitSourceTree(prjectName, commitAfter, filesAfter, aftr);
			return new PairBeforeAfter<>(
					gitSourceTreeB4,
					gitSourceTreeAftr);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(Repository repository, RevCommit commitBefore, RevCommit commitAfter, FilePathFilter fileExtensions) {
		return new PairBeforeAfter<>(
			new GitSourceTree(repository, commitBefore.getId(), new HashMap<>()),
			new GitSourceTree(repository, commitAfter.getId(), new HashMap<>()));
	}

	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(Repository repository, String commitId, FilePathFilter fileExtensions) {
		try (RevWalk rw = new RevWalk(repository)) {
			RevCommit commitAfter = rw.parseCommit(repository.resolve(commitId));
			if (commitAfter.getParentCount() != 1) {
				throw new RuntimeException("Commit should have one parent");
			}
			RevCommit commitBefore = rw.parseCommit(commitAfter.getParent(0));
			return getSourcesBeforeAndAfterCommit(repository, commitBefore, commitAfter, fileExtensions);
		} catch (Exception e) {
			throw new RuntimeException(e.toString());
		}
	}

	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit1(String commitId, String projectName, String projectPath) {
		String parentCommitIdGithubAPI = getParentCommitIdGithubAPI(commitId);
		Path currPath = Paths.get(projectPath + "/" + projectName + "-" + commitId.substring(0, 8) + "/curr/");
		Path prevPath = Paths.get(projectPath + "/" + projectName + "-" + commitId.substring(0, 8) + "/prev/");
		if(Files.exists(currPath)
					&& Files.exists(prevPath)){
				try {
					System.out.println("Found missing object in C:");
					return getSourcesBeforeAndAfterCommitMissingObject(parentCommitIdGithubAPI, commitId,  prevPath, currPath, projectName);
				}catch (Exception ex){
					ex.printStackTrace();
				}
			}
//			else {
//				e.printStackTrace();
			return null;
		}


	private static String getParentCommitIdGithubAPI(String commitId) {
		try {
			return Files.readAllLines(Paths.get(Paths.get(".").toAbsolutePath().toString() + "data/icse/commitParentCommit.txt"))
					.stream().filter(x -> x.startsWith(commitId))
					.map(x -> x.split(",")[1])
					.findFirst().get();
		}catch (Exception e){return "";
		}
	}

//	private static String getParentCommitIdGithubAPI(String cloneURL, String currentCommitId) throws IOException {
//		GitHub gitHub = connectToGitHub();
//		//https://github.com/ is 19 chars
//		String repoName = cloneURL.substring(19, cloneURL.indexOf(".git"));
//		GHRepository repository = gitHub.getRepository(repoName);
//		GHCommit commit = repository.getCommit(currentCommitId);
//		return commit.getParents().get(0).getSHA1();
////		List<GHCommit.File> commitFiles = commit.getFiles();
////		for (GHCommit.File commitFile : commitFiles) {
////			if (commitFile.getFileName().endsWith(".java")) {
////				if (commitFile.getStatus().equals("modified")) {
////					filesBefore.add(commitFile.getFileName());
////					filesCurrent.add(commitFile.getFileName());
////				}
////				else if (commitFile.getStatus().equals("added")) {
////					filesCurrent.add(commitFile.getFileName());
////				}
////				else if (commitFile.getStatus().equals("removed")) {
////					filesBefore.add(commitFile.getFileName());
////				}
////				else if (commitFile.getStatus().equals("renamed")) {
////					filesBefore.add(commitFile.getPreviousFilename());
////					filesCurrent.add(commitFile.getFileName());
////					renamedFilesHint.put(commitFile.getPreviousFilename(), commitFile.getFileName());
////				}
////			}
////		}
//	//	return parentCommitId;
//	}
//
//	private static GitHub connectToGitHub() {
//		GitHub gitHub = null;
////		if(gitHub == null) {
//			try {
//				Properties prop = new Properties();
//				InputStream input = new FileInputStream("D:\\MyProjects\\RefDiff-master\\refdiff-core\\github-credentials.properties");
//				prop.load(input);
//				String username = prop.getProperty("username");
//				String password = prop.getProperty("password");
//				if (username != null && password != null) {
//					gitHub = GitHub.connectUsingPassword(username, password);
//					if(gitHub.isCredentialValid()) {
//						System.out.println("Connected to GitHub with account: " + username);
//					}
//				}
//				else {
//					gitHub = GitHub.connect();
//				}
//			} catch(FileNotFoundException e) {
//				System.out.println("File github-credentials.properties was not found in RefactoringMiner's execution directory" + e.toString());
//			} catch(IOException ioe) {
//				ioe.printStackTrace();
//			}
////		}
//		return gitHub;
//	}
	
	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(Repository repository, String commitIdBefore, String commitIdAfter, FilePathFilter fileExtensions) {
		try (RevWalk rw = new RevWalk(repository)) {
			RevCommit commitBefore = rw.parseCommit(repository.resolve(commitIdBefore));
			RevCommit commitAfter = rw.parseCommit(repository.resolve(commitIdAfter));
			return getSourcesBeforeAndAfterCommit(repository, commitBefore, commitAfter, fileExtensions);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public int countCommits(Repository repository, String branch) throws Exception {
		RevWalk walk = new RevWalk(repository);
		try {
			Ref ref = repository.findRef(REMOTE_REFS_PREFIX + branch);
			ObjectId objectId = ref.getObjectId();
			RevCommit start = walk.parseCommit(objectId);
			walk.setRevFilter(RevFilter.NO_MERGES);
			return RevWalkUtils.count(walk, start, null);
		} finally {
			walk.dispose();
		}
	}
	
	private List<TrackingRefUpdate> fetch(Repository repository) throws Exception {
		try (Git git = new Git(repository)) {
			FetchResult result = git.fetch().call();
			
			Collection<TrackingRefUpdate> updates = result.getTrackingRefUpdates();
			List<TrackingRefUpdate> remoteRefsChanges = new ArrayList<TrackingRefUpdate>();
			for (TrackingRefUpdate update : updates) {
				String refName = update.getLocalName();
				if (refName.startsWith(REMOTE_REFS_PREFIX)) {
					remoteRefsChanges.add(update);
				}
			}
			return remoteRefsChanges;
		}
	}
	
	public RevWalk fetchAndCreateNewRevsWalk(Repository repository) throws Exception {
		return this.fetchAndCreateNewRevsWalk(repository, null);
	}
	
	public RevWalk fetchAndCreateNewRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>();
		for (Ref ref : repository.getAllRefs().values()) {
			String refName = ref.getName();
			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
				currentRemoteRefs.add(ref.getObjectId());
			}
		}
		
		List<TrackingRefUpdate> newRemoteRefs = this.fetch(repository);
		
		RevWalk walk = new RevWalk(repository);
		for (TrackingRefUpdate newRef : newRemoteRefs) {
			if (branch == null || newRef.getLocalName().endsWith("/" + branch)) {
				walk.markStart(walk.parseCommit(newRef.getNewObjectId()));
			}
		}
		for (ObjectId oldRef : currentRemoteRefs) {
			walk.markUninteresting(walk.parseCommit(oldRef));
		}
		walk.setRevFilter(commitsFilter);
		return walk;
	}
	
	public RevWalk createAllRevsWalk(Repository repository) throws Exception {
		return this.createAllRevsWalk(repository, null);
	}
	
	public RevWalk createAllRevsWalk(Repository repository, String branch) throws Exception {
		List<ObjectId> currentRemoteRefs = new ArrayList<ObjectId>();
		for (Ref ref : repository.getAllRefs().values()) {
			String refName = ref.getName();
			if (refName.startsWith(REMOTE_REFS_PREFIX)) {
				if (branch == null || refName.endsWith("/" + branch)) {
					currentRemoteRefs.add(ref.getObjectId());
				}
			}
		}
		
		RevWalk walk = new RevWalk(repository);
		for (ObjectId newRef : currentRemoteRefs) {
			walk.markStart(walk.parseCommit(newRef));
		}
		walk.setRevFilter(commitsFilter);
		return walk;
	}
	
	public boolean isCommitAnalyzed(String sha1) {
		return false;
	}
	
	private class DefaultCommitsFilter extends RevFilter {
		@Override
		public final boolean include(final RevWalk walker, final RevCommit c) {
			return c.getParentCount() == 1 && !isCommitAnalyzed(c.getName());
		}
		
		@Override
		public final RevFilter clone() {
			return this;
		}
		
		@Override
		public final boolean requiresCommitBody() {
			return false;
		}
		
		@Override
		public String toString() {
			return "RegularCommitsFilter";
		}
	}
	
	public void fileTreeDiff(Repository repository, RevCommit current, List<String> javaFilesBefore, List<String> javaFilesCurrent, Map<String, String> renamedFilesHint, boolean detectRenames, FilePathFilter fileExtensions) throws Exception {
		ObjectId oldHead = current.getParent(0).getTree();
		ObjectId head = current.getTree();
		
		// prepare the two iterators to compute the diff between
		ObjectReader reader = repository.newObjectReader();
		CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
		oldTreeIter.reset(reader, oldHead);
		CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
		newTreeIter.reset(reader, head);
		// finally get the list of changed files
		try (Git git = new Git(repository)) {
			List<DiffEntry> diffs = git.diff()
				.setNewTree(newTreeIter)
				.setOldTree(oldTreeIter)
				.setShowNameAndStatusOnly(true)
				.call();
			if (detectRenames) {
				RenameDetector rd = new RenameDetector(repository);
				rd.addAll(diffs);
				diffs = rd.compute();
			}
			
			for (DiffEntry entry : diffs) {
				ChangeType changeType = entry.getChangeType();
				if (changeType != ChangeType.ADD) {
					String oldPath = entry.getOldPath();
					if (fileExtensions.isAllowed(oldPath)) {
						javaFilesBefore.add(oldPath);
					}
				}
				if (changeType != ChangeType.DELETE) {
					String newPath = entry.getNewPath();
					if (fileExtensions.isAllowed(newPath)) {
						javaFilesCurrent.add(newPath);
						if (changeType == ChangeType.RENAME) {
							String oldPath = entry.getOldPath();
							renamedFilesHint.put(oldPath, newPath);
						}
					}
				}
			}
		}
	}
	
	public static void fileTreeDiff(Repository repository, RevCommit commitBefore, RevCommit commitAfter, List<SourceFile> filesBefore, List<SourceFile> filesAfter, FilePathFilter fileExtensions) {
		try {
			ObjectId oldHead = commitBefore.getTree();
			ObjectId head = commitAfter.getTree();
			
			// prepare the two iterators to compute the diff between
			ObjectReader reader = repository.newObjectReader();
			CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
			oldTreeIter.reset(reader, oldHead);
			CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
			newTreeIter.reset(reader, head);
			// finally get the list of changed files
			try (Git git = new Git(repository)) {
				List<DiffEntry> diffs = git.diff()
					.setNewTree(newTreeIter)
					.setOldTree(oldTreeIter)
					.setShowNameAndStatusOnly(true)
					.call();
				for (DiffEntry entry : diffs) {
					ChangeType changeType = entry.getChangeType();
					if (changeType != ChangeType.ADD) {
						String oldPath = entry.getOldPath();
						if (fileExtensions.isAllowed(oldPath)) {
							filesBefore.add(new SourceFile(Paths.get(oldPath)));
						}
					}
					if (changeType != ChangeType.DELETE) {
						String newPath = entry.getNewPath();
						if (fileExtensions.isAllowed(newPath)) {
							filesAfter.add(new SourceFile(Paths.get(newPath)));
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
