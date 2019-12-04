package test;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.TextFormat;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.Assert;
import refdiff.core.ResultModels.ResultsOuterClass;
import refdiff.core.api.*;
import refdiff.core.rm2.analysis.GitHistoryStructuralDiffAnalyzer;
import refdiff.core.rm2.model.HashArray;
import refdiff.core.rm2.model.refactoring.SDExtractMethod;
import refdiff.core.rm2.model.refactoring.SDMoveAndRenameClass;
import refdiff.core.rm2.model.refactoring.SDMoveClass;
import refdiff.core.rm2.model.refactoring.SDRenameClass;
import refdiff.core.util.GitServiceImpl;


import java.io.*;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class TestBuilderRD {

	private final String tempDir;
	private final Map<String, ProjectMatcher> map;
	private final GitRefactoringDetector refactoringDetector;
	private final String outputPath;
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

	public TestBuilderRD(GitRefactoringDetector detector, String tempDir, String outputPath) {
		this.outputPath = outputPath;
		this.map = new HashMap<String, ProjectMatcher>();
		this.refactoringDetector = detector;
		this.tempDir = tempDir;
		this.verbose = false;
		this.aggregate = false;
	}


	public TestBuilderRD verbose() {
		this.verbose = true;
		return this;
	}

	public TestBuilderRD withAggregation() {
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

//	public TestBuilder() {
//		this(new GitHistoryRefactoringMinerImpl(), "tmp");
//	}

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
		cMap = new HashMap<>();
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
						if (!readResult(commitId, outputPath).isPresent()) {
							System.out.println("Processing " + commitId + " " + folder);
							refactoringDetector.detectAtCommit(rep, commitId, m);
						}
					}

				} else {
					// Iterate over each commit
					//refactoringDetector.detectAll(rep, m.branch, m);
				}
			}
		}
		System.out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

		String mainResultMessage = buildResultMessage(c);
		System.out.println("Total  " + mainResultMessage);
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				System.out.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
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

	public void getCommitParentCommit() {
		c = new Counter();
		cMap = new HashMap<>();
		commitsCount = 0;
		errorCommitsCount = 0;
		GitHistoryStructuralDiffAnalyzer g = new GitHistoryStructuralDiffAnalyzer();
		for (ProjectMatcher m : map.values()) {

			String projectName = m.cloneUrl.substring(m.cloneUrl.lastIndexOf('/') + 1, m.cloneUrl.lastIndexOf('.'));
				if (m.ignoreNonSpecifiedCommits) {
					for (String commitId : m.getCommits()) {
						System.out.println(commitId);
						try{
							Path fldrCommit = Paths.get("D:/tmp/"+projectName +"-" + commitId.substring(0,8));
							if(!fldrCommit.toFile().exists())
								System.out.println(materialize(m, projectName, commitId, g));
							else
								System.out.println("Not Materialized!!!");
						}catch (Exception e){
							System.out.println(commitId);
							e.printStackTrace();
							System.out.println("-------");
						}
					}

				}
		}
		System.out.println(String.format("Commits: %d  Errors: %d", commitsCount, errorCommitsCount));

		String mainResultMessage = buildResultMessage(c);
		System.out.println("Total  " + mainResultMessage);
		for (RefactoringType refType : RefactoringType.values()) {
			Counter refTypeCounter = cMap.get(refType);
			if (refTypeCounter != null) {
				System.out.println(String.format("%-7s", refType.getAbbreviation()) + buildResultMessage(refTypeCounter));
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

	private String materialize(ProjectMatcher m, String projectName, String commitId, GitHistoryStructuralDiffAnalyzer g) throws IOException, InterruptedException {
		HashMap<String, String> filesBefore = new HashMap<>();
		HashMap<String, String> filesCurrent = new HashMap<>();
		Path fldrCommit = Paths.get("D:/tmp/"+projectName +"-" + commitId.substring(0,8));
		String parentId = g.populateWithGitHubAPI(m.cloneUrl, commitId, filesBefore, filesCurrent, new HashMap<>());
		//Path fldrB4 = Paths.get("D:/tmp/" + projectName + "-" + parentId.substring(0, 8));
		StringBuilder str = new StringBuilder().append("Materialized ");
		if(!fldrCommit.toFile().exists()){
			Files.createDirectories(fldrCommit);
			Path fldrB4 = Paths.get(fldrCommit.toAbsolutePath().toString(), "/prev/");
			if(!fldrB4.toFile().exists()){
				Files.createDirectories(fldrB4);
				str.append("Before ");
				materializeAt(fldrB4, filesBefore);
			}
			Path fldrCurr = Paths.get(fldrCommit.toAbsolutePath().toString(), "/curr/");
			if(!fldrCurr.toFile().exists()){
				Files.createDirectories(fldrCurr);
				str.append("After ");
				try {
					materializeAt(fldrCurr, filesCurrent);
				}catch (Exception e){
					e.printStackTrace();
				}
			}else{
				str.append("Found After");
			}
			return str.toString();

		}
		return "Did Not Materialize!!! ";

	}

	public void materializeAt(Path folderPath, Map<String,String> filesAndContent) throws IOException {

		File folder = folderPath.toFile();
		int counter = 0;
		if (folder.exists() || folder.mkdirs()) {
			for (Map.Entry<String,String> sf : filesAndContent.entrySet()) {
				File destinationFile = new File(folder, sf.getKey());
				if (!destinationFile.exists()) {
					byte[] content = sf.getValue().getBytes();
					Files.createDirectories(destinationFile.getParentFile().toPath());
					Files.write(destinationFile.toPath(), sf.getValue().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
					counter ++;
				}
			}
//			checkoutFolder = folderPath;
		} else {
			throw new IOException("Failed to create directory " + folderPath);
		}
		System.out.println("Materialized: " + counter);
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
		return singletonList(refactoring);
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
		public void handle(String commitId, List<? extends Refactoring> refactorings) {
			ResultsOuterClass.Results.Builder result = ResultsOuterClass.Results.newBuilder();
			CommitMatcher commitMatcher;
			commitsCount++;
			//String commitId = curRevision.getId().getName();
			if (expected.containsKey(commitId)) {
				commitMatcher = expected.get(commitId);
			} else if (!this.ignoreNonSpecifiedCommits) {
				commitMatcher = this.atCommit(commitId);
				commitMatcher.containsOnly();
			} else {
				// ignore this commit
				commitMatcher = null;
			}
			if (commitMatcher != null) {
				commitMatcher.analyzed = true;
				Set<String> refactoringsFound = new HashSet<String>();
				for (Refactoring refactoring : refactorings) {

					if (refactoring.getRefactoringType().getDisplayName().equalsIgnoreCase("Extract Method")) {
						SDExtractMethod em = (SDExtractMethod) refactoring;
						String clsB4 = em.getEntityBefore().container().fullName();
						String clsAfter = em.getEntityAfter().container().fullName();

						boolean sideEffect = refactorings.stream().filter(x->x.getRefactoringType().getDisplayName().equalsIgnoreCase("Rename Class"))
								.map(x-> (SDRenameClass)x)
								.anyMatch(x -> x.getEntityBefore().fullName().equals(clsB4) && x.getEntityAfter().fullName().equals(clsAfter))
								||
								refactorings.stream().filter(x->x.getRefactoringType().getDisplayName().equalsIgnoreCase("Move Class"))
										.map(x-> (SDMoveClass)x)
										.anyMatch(x -> x.getEntityBefore().fullName().equals(clsB4) && x.getEntityAfter().fullName().equals(clsAfter))
								||
								refactorings.stream().filter(x->x.getRefactoringType().getDisplayName().equalsIgnoreCase("Move and Rename Class"))
										.map(x-> (SDMoveAndRenameClass)x)
										.anyMatch(x -> x.getEntityBefore().fullName().equals(clsB4) && x.getEntityAfter().fullName().equals(clsAfter));

						if (sideEffect) refactoringsFound.addAll(normalize(em.getAsExtractMethodString()));
						else refactoringsFound.addAll(normalize(refactoring.toString()));

					}else {
						refactoringsFound.addAll(normalize(refactoring.toString()));
					}
				}
				// count true positives
				for (Iterator<String> iter = commitMatcher.expected.iterator(); iter.hasNext();) {
					String expectedRefactoring = iter.next();
					if (refactoringsFound.contains(expectedRefactoring)) {
						iter.remove();
						refactoringsFound.remove(expectedRefactoring);
						this.truePositiveCount++;
						count(TP, expectedRefactoring);
						commitMatcher.truePositive.add(expectedRefactoring);
						result.addTruePositives(expectedRefactoring);
					}
				}

				// count false positives
				for (Iterator<String> iter = commitMatcher.falsePositives.iterator(); iter.hasNext();) {
					String notExpectedRefactoring = iter.next();
					if (refactoringsFound.contains(notExpectedRefactoring)) {
						refactoringsFound.remove(notExpectedRefactoring);
						this.falsePositiveCount++;
						count(FP, notExpectedRefactoring);
						result.addFalsePositives(notExpectedRefactoring);
					} else {
						this.trueNegativeCount++;
						count(TN, notExpectedRefactoring);
						result.addTrueNegatives(notExpectedRefactoring);
						iter.remove();
					}
				}
				// count false positives when using containsOnly
				if (commitMatcher.ignoreNonSpecified) {
					for (String refactoring : refactoringsFound) {
						result.addTrueNegatives(refactoring);
						commitMatcher.unknown.add(refactoring);
						this.unknownCount++;
						count(UNK, refactoring);
					}
				} else {
					for (String refactoring : refactoringsFound) {
						commitMatcher.falsePositives.add(refactoring);
						result.addFalsePositives(refactoring);
						this.falsePositiveCount++;
						count(FP, refactoring);
					}
				}

				// count false negatives
				for (String expectedButNotFound : commitMatcher.expected) {
					this.falseNegativeCount++;
					result.addFalseNegatives(expectedButNotFound);
					count(FN, expectedButNotFound);
				}
			}
			write(result.setSha(commitId).build(), outputPath);
		}

		private List<Refactoring> filterRefactoring(List<Refactoring> refactorings) {
			List<Refactoring> filteredRefactorings = new ArrayList<>();

			for (Refactoring refactoring : refactorings) {
				BigInteger value = Enum.valueOf(RefactoringPopulatorRD.Refactorings.class, refactoring.getName().replace(" ", "")).getValue();
				if (value.and(refactoringFilter).compareTo(BigInteger.ZERO) == 1) {
					filteredRefactorings.add(refactoring);
				}
			}
			
			return filteredRefactorings;
		}

		@Override
		public void handleException(String commitId, Exception e) {
			ResultsOuterClass.Results.Builder res = ResultsOuterClass.Results.newBuilder();
			if (expected.containsKey(commitId)) {
				CommitMatcher matcher = expected.get(commitId);
				matcher.error = e.toString();
			}
			errorCommitsCount++;
			String x = " error at commit " + commitId + ": " + e.getMessage() +"\n" + e.toString() + "\n----------------------------\n";
			//System.err.println(x);
			System.out.println(" error at commit " + commitId);
			res.setSha(commitId + " Exception");
			try {
				Files.write(Paths.get("D:\\MyProjects\\TSEAnalysis\\Evaluation\\exceptions1.txt"), x.getBytes(),
						StandardOpenOption.APPEND);
			}catch (Exception ex){
				throw new RuntimeException("WTF cannot read exception1 file");
			}
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
					if (verbose || !matcher.expected.isEmpty() || !matcher.falsePositives.isEmpty()
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
					if (!matcher.falsePositives.isEmpty()) {
						System.out.println(" false positives");
						for (String ref : matcher.falsePositives) {
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
			private Set<String> expected = new HashSet<>();
			private Set<String> falsePositives = new HashSet<>();
			private Set<String> truePositive = new HashSet<>();
			private Set<String> unknown = new HashSet<>();
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
				this.falsePositives = new HashSet<String>();
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
					falsePositives.addAll(normalize(refactoring));
				}
				return ProjectMatcher.this;
			}
		}
	}

	public static void write(ResultsOuterClass.Results msg, String outputPath) {
        String folderPath = Paths.get(".").toAbsolutePath().toString() + outputPath;
        try {
            if(!new File(folderPath).exists())
                Files.createDirectories(Paths.get(folderPath));

            String s = folderPath + msg.getSha() + ".txt";

            FileOutputStream output1 = new FileOutputStream(s,true);
            output1.write(msg.toByteArray());
        } catch (FileNotFoundException e) {
			System.out.println("File not found.  Creating a new file.");
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Optional<ResultsOuterClass.Results> readResult(String commitId, String outputPath){
		try {
			String s =Paths.get(".").toAbsolutePath().toString() + outputPath;
			return Files.walk(Paths.get(s)).filter(x -> x.getFileName().toString().contains(commitId)).findFirst()
					.filter(f -> f.toFile().exists())
					.flatMap(f -> getResults(f));
		} catch (Exception e) {
			System.out.println(e.toString());
			return Optional.empty();
		}
	}

	private static Optional<ResultsOuterClass.Results> getResults(Path file) {
		try {
			return Optional.of(ResultsOuterClass.Results.parseFrom(Files.readAllBytes(file)));
		} catch (IOException e) {
			return Optional.empty();
		}
	}


	public static List<ResultsOuterClass.Results> readResult(String outputPath){
		try {
			String s = Paths.get(".").toAbsolutePath().toString() + outputPath;
			String contents = new String(Files.readAllBytes(Paths.get(s + "BinSize.txt")));
			String[] x = contents.split(" ");
			List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
			InputStream is = new FileInputStream(s + "Bin.txt");
			List<ResultsOuterClass.Results> results = new ArrayList<>();
			for (Integer c : y) {
				byte[] b = new byte[c];
				int i = is.read(b);
				if (i > 0) {
					CodedInputStream input = CodedInputStream.newInstance(b);
					ResultsOuterClass.Results r = ResultsOuterClass.Results.parseFrom(input);
					results.add(r);
					//results.add(ResultsOuterClass.Results.parseFrom(input));
				}
			}
			results.addAll(readAllResults(outputPath));
			return results;
		} catch (Exception e) {
			System.out.println(e.toString());
			System.out.println( "TFG protos could not be deserialised");
			return new ArrayList<>();
		}
	}

	public static List<ResultsOuterClass.Results> readAllResults(String outputPath){
		try {
			String s = Paths.get(".").toAbsolutePath().toString() + outputPath;

			return Files.walk(Paths.get(s))
					.map(p -> getResults(p))
					.filter(x->x.isPresent())
					.map(x->x.get())
					.collect(Collectors.toList());
		} catch (Exception e) {
			System.out.println(e.toString());
			System.out.println( "TFG protos could not be deserialised");
			return new ArrayList<>();
		}
	}
}
