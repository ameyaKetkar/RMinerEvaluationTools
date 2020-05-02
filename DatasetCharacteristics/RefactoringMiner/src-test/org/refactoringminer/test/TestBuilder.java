package org.refactoringminer.test;

import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.*;
import org.eclipse.jgit.lib.Repository;
import org.junit.Assert;
import org.refactoringminer.MatchedStatementsOuterClass.MatchedStatements.ReplacementInferred;
import org.refactoringminer.MatchedStatementsOuterClass.MatchedStatements.StatementReplacements;
import org.refactoringminer.ProtoUtil;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.test.RefactoringPopulator.Refactorings;
import org.refactoringminer.util.GitServiceImpl;

import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.refactoringminer.MatchedStatementsOuterClass.*;

public class TestBuilder {

	private final String tempDir;
	private final Map<String, ProjectMatcher> map;
	private final GitHistoryRefactoringMiner refactoringDetector;
	private boolean verbose;
	private boolean aggregate;
	private int commitsCount;
	private int errorCommitsCount;
	private Counter c;// = new Counter();
	private Map<RefactoringType, Counter> cMap;
	private static final int TP = 0;
	private static final int FP = 1;
	private static final int FN = 2;
	private static final int TN = 3;
	private static final int UNK = 4;

	private BigInteger refactoringFilter;

	ProtoUtil.ReadWriteAt readWriteProto;

	public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir) {
		this.map = new HashMap<String, ProjectMatcher>();
		this.refactoringDetector = detector;
		this.tempDir = tempDir;
		this.verbose = false;
		this.aggregate = false;
		this.readWriteProto = new ProtoUtil.ReadWriteAt(Paths.get("/Users/ameya/Research/RMinerEvaluationTools/DatasetCharacteristics/ReplacementsData/"));
	}

	public TestBuilder(GitHistoryRefactoringMiner detector, String tempDir, BigInteger refactorings) {
		this(detector, tempDir);

		this.refactoringFilter = refactorings;
	}

	public TestBuilder verbose() {
		this.verbose = true;
		return this;
	}

	public TestBuilder withAggregation() {
		this.aggregate = true;
		return this;
	}

	private static class Counter {
		int[] c = new int[5];
	}

	private void count(int type, String refactoring) {
		c.c[type]++;
		RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
		Counter refTypeCounter = cMap.get(refType);
		if (refTypeCounter == null) {
			refTypeCounter = new Counter();
			cMap.put(refType, refTypeCounter);
		}
		refTypeCounter.c[type]++;
	}

	private int get(int type) {
		return c.c[type];
	}

	private int get(int type, Counter counter) {
		return counter.c[type];
	}

	public TestBuilder() {
		this(new GitHistoryRefactoringMinerImpl(), "tmp");
	}

	public final ProjectMatcher project(String cloneUrl, String branch) {
		ProjectMatcher projectMatcher = this.map.get(cloneUrl);
		if (projectMatcher == null) {
			projectMatcher = new ProjectMatcher(cloneUrl, branch);
			this.map.put(cloneUrl, projectMatcher);
		}
		return projectMatcher;
	}

	public void assertExpectations() throws Exception {
		c = new Counter();
		cMap = new HashMap<RefactoringType, Counter>();
		commitsCount = 0;
		errorCommitsCount = 0;
		GitService gitService = new GitServiceImpl();

		for (ProjectMatcher m : map.values()) {
			String folder = tempDir + "/"
					+ m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
			try (Repository rep = gitService.cloneIfNotExists(folder,
					m.cloneUrl/* , m.branch */)) {
				if (m.ignoreNonSpecifiedCommits) {
					// It is faster to only look at particular commits
					for (String commitId : m.getCommits()) {
						refactoringDetector.detectAtCommit(rep, commitId, m);
					}
				} else {
					// Iterate over each commit
					refactoringDetector.detectAll(rep, m.branch, m);
				}
			}
		}
		System.out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

		String mainResultMessage = buildResultMessage(c);
		System.out.println("Total  " + mainResultMessage);
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				System.out
						.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
			}
		}

		boolean success = get(FP) == 0 && get(FN) == 0 && get(TP) > 0;
		if (!success || verbose) {
			for (ProjectMatcher m : map.values()) {
				m.printResults();
			}
		}
		Assert.assertTrue(mainResultMessage, success);
	}

	private String buildResultMessage(Counter c) {
		double precision = ((double) get(TP, c) / (get(TP, c) + get(FP, c)));
		double recall = ((double) get(TP, c)) / (get(TP, c) + get(FN, c));
		String mainResultMessage = String.format(
				"TP: %2d  FP: %2d  FN: %2d  TN: %2d  Unk.: %2d  Prec.: %.3f  Recall: %.3f", get(TP, c), get(FP, c),
				get(FN, c), get(TN, c), get(UNK, c), precision, recall);
		return mainResultMessage;
	}

	private List<String> normalize(String refactoring) {
		RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
		refactoring = normalizeSingle(refactoring);
		if (aggregate) {
			refactoring = refType.aggregate(refactoring);
		} else {
			int begin = refactoring.indexOf("from classes [");
			if (begin != -1) {
				int end = refactoring.lastIndexOf(']');
				String types = refactoring.substring(begin + "from classes [".length(), end);
				String[] typesArray = types.split(", ");
				List<String> refactorings = new ArrayList<String>();
				for (String type : typesArray) {
					refactorings.add(refactoring.substring(0, begin) + "from class " + type);
				}
				return refactorings;
			}
		}
		return Collections.singletonList(refactoring);
	}

	/**
	 * Remove generics type information.
	 */
	private static String normalizeSingle(String refactoring) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < refactoring.length(); i++) {
			char c = refactoring.charAt(i);
			if (c == '\t') {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}

	public class ProjectMatcher extends RefactoringHandler {

		private final String cloneUrl;
		private final String branch;
		private Map<String, CommitMatcher> expected = new HashMap<>();
		private boolean ignoreNonSpecifiedCommits = true;
		private int truePositiveCount = 0;
		private int falsePositiveCount = 0;
		private int falseNegativeCount = 0;
		private int trueNegativeCount = 0;
		private int unknownCount = 0;
		// private int errorsCount = 0;

		private ProjectMatcher(String cloneUrl, String branch) {
			this.cloneUrl = cloneUrl;
			this.branch = branch;
		}

		public ProjectMatcher atNonSpecifiedCommitsContainsNothing() {
			this.ignoreNonSpecifiedCommits = false;
			return this;
		}

		public CommitMatcher atCommit(String commitId) {
			CommitMatcher m = expected.get(commitId);
			if (m == null) {
				m = new CommitMatcher();
				expected.put(commitId, m);
			}
			return m;
		}

		public Set<String> getCommits() {
			return expected.keySet();
		}

		@Override
		public boolean skipCommit(String commitId) {
			if (this.ignoreNonSpecifiedCommits) {
				return !this.expected.containsKey(commitId);
			}
			return false;
		}

		@Override
		public void handle(String commitId, List<Refactoring> refactorings) {
			refactorings= filterRefactoring(refactorings);
			CommitMatcher matcher;
			commitsCount++;
			//String commitId = curRevision.getId().getName();
			if (expected.containsKey(commitId)) {
				matcher = expected.get(commitId);
			} else if (!this.ignoreNonSpecifiedCommits) {
				matcher = this.atCommit(commitId);
				matcher.containsOnly();
			} else {
				// ignore this commit
				matcher = null;
			}
			if (matcher != null) {
				matcher.analyzed = true;
				Set<String> refactoringsFound = new HashSet<String>();
				for (Refactoring refactoring : refactorings) {
					refactoringsFound.addAll(normalize(refactoring.toString()));
				}
				// count true positives
				for (Iterator<String> iter = matcher.expected.iterator(); iter.hasNext();) {
					String expectedRefactoring = iter.next();
					if (refactoringsFound.contains(expectedRefactoring)) {
						iter.remove();
						refactoringsFound.remove(expectedRefactoring);
						this.truePositiveCount++;
						count(TP, expectedRefactoring);
						matcher.truePositive.add(expectedRefactoring);
					}
				}

				// count false positives
				for (Iterator<String> iter = matcher.notExpected.iterator(); iter.hasNext();) {
					String notExpectedRefactoring = iter.next();
					if (refactoringsFound.contains(notExpectedRefactoring)) {
						refactoringsFound.remove(notExpectedRefactoring);
						this.falsePositiveCount++;
						count(FP, notExpectedRefactoring);
					} else {
						this.trueNegativeCount++;
						count(TN, notExpectedRefactoring);
						iter.remove();
					}
				}
				// count false positives when using containsOnly
				if (matcher.ignoreNonSpecified) {
					for (String refactoring : refactoringsFound) {
						matcher.unknown.add(refactoring);
						this.unknownCount++;
						count(UNK, refactoring);
					}
				} else {
					for (String refactoring : refactoringsFound) {
						matcher.notExpected.add(refactoring);
						this.falsePositiveCount++;
						count(FP, refactoring);
					}
				}

				// count false negatives
				for (String expectedButNotFound : matcher.expected) {
					this.falseNegativeCount++;
					count(FN, expectedButNotFound);
				}
			}

			refactorings.forEach(r -> readWriteProto.write(getAllMatchedStatements(r, commitId),"MatchedStatements", true ));

		}



		private MatchedStatements getAllMatchedStatements(Refactoring r, String commit){
				return MatchedStatements.newBuilder().setRefactoringKind(r.getName())
						.setDescription(r.toString())
						.setProject(cloneUrl).setCommit(commit)
						.addAllStatements(getStatementsFor(r)).build();

		}

		private List<StatementReplacements> getStatementsFor(Refactoring r) {
			if(r instanceof ExtractOperationRefactoring) // ExtractandMoveOperation
				return getStatementReplacement(((ExtractOperationRefactoring) r).getBodyMapper().getMappings());

			else if(r instanceof ReplaceAttributeRefactoring)
				return ((ReplaceAttributeRefactoring) r).getAttributeRenames().stream()
							.flatMap(x -> getStatementReplacement(x.getAttributeReferences()).stream())
						.collect(toList());

			else if(r instanceof RenameOperationRefactoring)
				return getStatementReplacement(((RenameOperationRefactoring)r).getBodyMapper().getMappings());
			else if(r instanceof InlineOperationRefactoring) // MoveAndInlineOperation
				return getStatementReplacement(((InlineOperationRefactoring) r).getBodyMapper().getMappings());
			else if (r instanceof MoveOperationRefactoring) // MoveAndRenameOperation, PullUpOperationRefactoring, PushDownOperationRefactoring
				return getStatementReplacement(((MoveOperationRefactoring)r).getBodyMapper().getMappings());

			else if(r instanceof ExtractVariableRefactoring)
				return getStatementReplacement(((ExtractVariableRefactoring)r).getReferences());
			else if(r instanceof ExtractAttributeRefactoring)
				return getStatementReplacement(((ExtractAttributeRefactoring)r).getReferences());
			else if(r instanceof InlineVariableRefactoring)
				return getStatementReplacement(((InlineVariableRefactoring)r).getReferences());
			else if(r instanceof RenameVariableRefactoring) //PARAMETERIZE_VARIABLE, REPLACE_VARIABLE_WITH_ATTRIBUTE, RENAME_PARAMETER
				return getStatementReplacement(((RenameVariableRefactoring)r).getVariableReferences());
			else if(r instanceof RenameAttributeRefactoring)
				return ((RenameAttributeRefactoring)r).getAttributeRenames().stream()
				.flatMap(x -> getStatementReplacement(x.getAttributeReferences()).stream()).collect(toList());
			else if(r instanceof MergeVariableRefactoring) // merger parameter
				return getStatementReplacement(((MergeVariableRefactoring)r).getVariableReferences());
			else if(r instanceof MergeAttributeRefactoring)
				return ((MergeAttributeRefactoring)r).getAttributeMerges().stream()
				.flatMap(x -> getStatementReplacement(x.getVariableReferences()).stream()).collect(toList());
			else if(r instanceof SplitVariableRefactoring) // Split parameter
				return getStatementReplacement(((SplitVariableRefactoring)r).getVariableReferences());
			else if(r instanceof SplitAttributeRefactoring)
				return ((SplitAttributeRefactoring)r).getAttributeSplits().stream()
				.flatMap(x -> getStatementReplacement(x.getVariableReferences()).stream()).collect(toList());
			else if(r instanceof ChangeReturnTypeRefactoring)
				return getStatementReplacement(((ChangeReturnTypeRefactoring)r).getReturnReferences());
			else if(r instanceof ChangeVariableTypeRefactoring)
				return getStatementReplacement(((ChangeVariableTypeRefactoring)r).getVariableReferences());
			else if(r instanceof ChangeAttributeTypeRefactoring)
				return getStatementReplacement(((ChangeAttributeTypeRefactoring)r).getAttributeReferences());

			//MoveAttributeRefactoring, MoveAndRenameAttribute, PullUpAttributeRefactoring, PushDownAttributeRefactoring
			// ConvertAnonymousClassToTypeRefactoring
			// ExtractSuperclassRefactoring, extract interface
			//ExtractClassRefactoring , subclass
			//MoveAndRenameClassRefactoring
			//MoveClassRefactoring
			//RenameClassRefactoring
			else return new ArrayList<>();


		}

		private List<StatementReplacements> getStatementReplacement(Set<AbstractCodeMapping> mappings) {
			return mappings.stream().map(x -> StatementReplacements.newBuilder()
					.setB4(x.getFragment1().getString()).setAfter((x.getFragment2().getString()))
				.setIsSame(x.isExact()).addAllReplcementInferred(getReplacementInferred(x.getReplacements())).build())
				.collect(toList());
		}

		private List<ReplacementInferred> getReplacementInferred(Set<Replacement> replacements) {
			return replacements.stream().map(x -> ReplacementInferred.newBuilder().setB4(x.getBefore())
					.setAftr(x.getAfter()).setReplacementType(x.getType().name()).build()).collect(toList());
		}


		private List<Refactoring> filterRefactoring(List<Refactoring> refactorings) {
			List<Refactoring> filteredRefactorings = new ArrayList<>();

			for (Refactoring refactoring : refactorings) {
				BigInteger value = Enum.valueOf(Refactorings.class, refactoring.getName().replace(" ", "")).getValue();
				if (value.and(refactoringFilter).compareTo(BigInteger.ZERO) == 1) {
					filteredRefactorings.add(refactoring);
				}
			}
			
			return filteredRefactorings;
		}

		@Override
		public void handleException(String commitId, Exception e) {
			if (expected.containsKey(commitId)) {
				CommitMatcher matcher = expected.get(commitId);
				matcher.error = e.toString();
			}
			errorCommitsCount++;
			// System.err.println(" error at commit " + commitId + ": " +
			// e.getMessage());
		}

		private void printResults() {
			// if (verbose || this.falsePositiveCount > 0 ||
			// this.falseNegativeCount > 0 || this.errorsCount > 0) {
			// System.out.println(this.cloneUrl);
			// }
			String baseUrl = this.cloneUrl.substring(0, this.cloneUrl.length() - 4) + "/commit/";
			for (Map.Entry<String, CommitMatcher> entry : this.expected.entrySet()) {
				String commitUrl = baseUrl + entry.getKey();
				CommitMatcher matcher = entry.getValue();
				if (matcher.error != null) {
					System.out.println("error at " + commitUrl + ": " + matcher.error);
				} else {
					if (verbose || !matcher.expected.isEmpty() || !matcher.notExpected.isEmpty()
							|| !matcher.unknown.isEmpty()) {
						if (!matcher.analyzed) {
							System.out.println("at not analyzed " + commitUrl);
						} else {
							System.out.println("at " + commitUrl);
						}
					}
					if (verbose && !matcher.truePositive.isEmpty()) {
						System.out.println(" true positives");
						for (String ref : matcher.truePositive) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.notExpected.isEmpty()) {
						System.out.println(" false positives");
						for (String ref : matcher.notExpected) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.expected.isEmpty()) {
						System.out.println(" false negatives");
						for (String ref : matcher.expected) {
							System.out.println("  " + ref);
						}
					}
					if (!matcher.unknown.isEmpty()) {
						System.out.println(" unknown");
						for (String ref : matcher.unknown) {
							System.out.println("  " + ref);
						}
					}
				}
			}
		}

		// private void countFalseNegatives() {
		// for (Map.Entry<String, CommitMatcher> entry :
		// this.expected.entrySet()) {
		// CommitMatcher matcher = entry.getValue();
		// if (matcher.error == null) {
		// this.falseNegativeCount += matcher.expected.size();
		// }
		// }
		// }

		public class CommitMatcher {
			private Set<String> expected = new HashSet<String>();
			private Set<String> notExpected = new HashSet<String>();
			private Set<String> truePositive = new HashSet<String>();
			private Set<String> unknown = new HashSet<String>();
			private boolean ignoreNonSpecified = true;
			private boolean analyzed = false;
			private String error = null;

			private CommitMatcher() {
			}

			public ProjectMatcher contains(String... refactorings) {
				for (String refactoring : refactorings) {
					expected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}

			public ProjectMatcher containsOnly(String... refactorings) {
				this.ignoreNonSpecified = false;
				this.expected = new HashSet<String>();
				this.notExpected = new HashSet<String>();
				for (String refactoring : refactorings) {
					expected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}

			public ProjectMatcher containsNothing() {
				return containsOnly();
			}

			public ProjectMatcher notContains(String... refactorings) {
				for (String refactoring : refactorings) {
					notExpected.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}
}
