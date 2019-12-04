package refdiff.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiConsumer;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.GitHelper;
import refdiff.core.io.SourceFileSet;
import refdiff.core.util.PairBeforeAfter;
import refdiff.parsers.LanguagePlugin;

public class RefDiff {
	
	private final CstComparator comparator;
	private final FilePathFilter fileFilter;
	
	public RefDiff(LanguagePlugin parser) {
		this.comparator = new CstComparator(parser);
		this.fileFilter = parser.getAllowedFilesFilter();
	}
	
	public File cloneGitRepository(File destinationFolder, String cloneUrl) throws Exception {
		return GitHelper.cloneBareRepository(destinationFolder, cloneUrl);
	}
	
	public CstDiff computeDiffForCommit(File gitRepository, String commitSha1) {
		try (Repository repo = GitHelper.openRepository(gitRepository)) {
			PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, commitSha1, fileFilter);
			if(beforeAndAfter!=null)
				return comparator.compare(beforeAndAfter);
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public CstDiff computeDiffForCommit(String commitSha1, String cloneUrl, String projectPath) {
		String projectName = cloneUrl.substring(cloneUrl.lastIndexOf('/') + 1, cloneUrl.lastIndexOf('.'));
		try {
			PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit1( commitSha1, projectName, projectPath);
			if(beforeAndAfter!=null) {
				CstDiff comp = comparator.compare(beforeAndAfter);
				return comp;
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}


//	public CstDiff computeDiffForCommit(File gitRepository, String commitSha1) {
//		try (Repository repo = GitHelper.openRepository(gitRepository)) {
//			PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, commitSha1, fileFilter);
//			return comparator.compare(beforeAndAfter);
//		}
//	}
	
	public void computeDiffForCommitHistory(File gitRepository, int maxDepth, BiConsumer<RevCommit, CstDiff> diffConsumer) {
		computeDiffForCommitHistory(gitRepository, "HEAD", maxDepth, diffConsumer);
	}
	
	public void computeDiffForCommitHistory(File gitRepository, String startAt, int maxDepth, BiConsumer<RevCommit, CstDiff> diffConsumer) {
		try (Repository repo = GitHelper.openRepository(gitRepository)) {
			GitHelper.forEachNonMergeCommit(repo, startAt, maxDepth, (revBefore, revAfter) -> {
				PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, revBefore, revAfter, fileFilter);
				CstDiff diff = comparator.compare(beforeAndAfter);
				diffConsumer.accept(revAfter, diff);
			});
		}
	}
	
	public CstDiff computeDiffBetweenRevisions(Repository repo, RevCommit revBefore, RevCommit revAfter) {
		PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, revBefore, revAfter, fileFilter);
		return comparator.compare(beforeAndAfter);
	}
	
}
