package refdiff.evaluation.RefDiffVsRMiner;


import com.google.protobuf.CodedInputStream;
import refdiff.core.RefDiff;
import refdiff.core.ResultModels.ResultsOuterClass;
import refdiff.core.diff.CstDiff;
import refdiff.core.diff.Relationship;
import refdiff.evaluation.EvaluationUtils;
import refdiff.evaluation.RefactoringType;
import refdiff.parsers.java.JavaPlugin;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class TestBuilderRD {

  //  private final String tempDir;
    private final Map<String, ProjectMatcher> map;
    //	private final GitRefactoringDetector refactoringDetector;
    //private RefDiff refDiff;
   // private final List<RefactoringSet> expected;
    private boolean verbose;
    private boolean aggregate;
    private int commitsCount;
    private int errorCommitsCount;
    private Counter c = new Counter();
    private Map<RefactoringType, Counter> cMap;
    private static final int TP = 0;
    private static final int FP = 1;
    private static final int FN = 2;
    private static final int TN = 3;
    private static final int UNK = 4;
    private EvaluationUtils evalUtils;

    private BigInteger refactoringFilter;

    //private File tempFolder = new File("D:/tmp1");
    private File tempFolder;
    private final String outputPath;

    static Set<String> refTypes = Arrays.stream(RefactoringType.values()).map(RefactoringType::getDisplayName)
            .collect(Collectors.toSet());

    public TestBuilderRD(String projectPath, String outputPath) {
        this.tempFolder = new File(projectPath);
        this.outputPath = outputPath;
        this.verbose = false;
        this.aggregate = false;
        map = new HashMap<>();
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
        if(refType!=null) {
            Counter refTypeCounter = cMap.get(refType);
            if (refTypeCounter == null) {
                refTypeCounter = new Counter();
                cMap.put(refType, refTypeCounter);
            }
            refTypeCounter.c[type]++;
        }
    }

    private int get(int type, Counter counter) {
        return counter.c[type];
    }

    public final ProjectMatcher project(String cloneUrl, String branch) {
        ProjectMatcher projectMatcher = this.map.get(cloneUrl);
        if (projectMatcher == null) {
            projectMatcher = new ProjectMatcher(cloneUrl, branch);
            this.map.put(cloneUrl, projectMatcher);
        }
        return projectMatcher;
    }


    public void assertExpectations1() throws Exception {

        map.values().forEach(m -> {
            JavaPlugin jp = new JavaPlugin(tempFolder);
            RefDiff rdj = new RefDiff(jp);
            for (String commitId : m.getCommits()) {
                CstDiff diff = rdj.computeDiffForCommit(commitId, m.cloneUrl, tempFolder.getAbsolutePath());
                System.out.println(commitId);
                if (diff != null) {
                    List<String> refs = Relationship.getOracleStyleDescriptionsFor(diff.getRefactoringRelationships());
                    m.handle(commitId, refs);
                }
            }
        });

    }

    private String repoName(String project) {
        return project.substring(project.lastIndexOf('/') + 1, project.lastIndexOf('.'));
    }

    private List<String> normalize(String refactoring) {
        RefactoringType refType = RefactoringType.extractFromDescription(refactoring);
        if(refType!=null) {
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
        }
        return Collections.singletonList(refactoring);
    }
//
//    /**
//     * Remove generics type information.
//     */
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

    public class ProjectMatcher {

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


//    @Override
    public void handle(String commitId, List<String> refactorings) {

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
            for (String refactoring : refactorings) {
                refactoringsFound.addAll(normalize(refactoring));
            }
            // count true positives
            for (Iterator<String> iter = commitMatcher.expected.iterator(); iter.hasNext();) {
                String expectedRefactoring = iter.next();
                if (refactoringsFound.contains(expectedRefactoring)) {
                    iter.remove();
                    refactoringsFound.remove(expectedRefactoring);
                    this.truePositiveCount++;
                //    count(TP, expectedRefactoring);
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
                  //  count(FP, notExpectedRefactoring);
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
                 //  count(UNK, refactoring);
                }
            } else {
                for (String refactoring : refactoringsFound) {
                    commitMatcher.falsePositives.add(refactoring);
                    result.addFalsePositives(refactoring);
                    this.falsePositiveCount++;
                    //count(FP, refactoring);
                }
            }

            // count false negatives
            for (String expectedButNotFound : commitMatcher.expected) {
                this.falseNegativeCount++;
                result.addFalseNegatives(expectedButNotFound);
              //  count(FN, expectedButNotFound);
            }
        }
        write(result.setSha(commitId).build(), outputPath);
    }

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
//}
