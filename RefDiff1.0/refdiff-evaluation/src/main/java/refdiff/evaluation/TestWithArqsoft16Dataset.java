package refdiff.evaluation;

import java.util.EnumSet;
import java.util.Locale;

import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import refdiff.core.RefDiff;
import refdiff.core.api.GitRefactoringDetector;
import refdiff.core.api.RefactoringType;
import refdiff.evaluation.rm.RmAdapter;
import refdiff.evaluation.utils.RefFinderResultReader;
import refdiff.evaluation.utils.RefactoringCrawlerResultReader;
import refdiff.evaluation.utils.RefactoringSet;
import refdiff.evaluation.utils.ResultComparator;
import refdiff.evaluation.utils.ResultComparator.CompareResult;

public class TestWithArqsoft16Dataset {

    public static void main(String[] args) {
        new TestWithArqsoft16Dataset().run();
    }

    ResultComparator rcRDiff;
    ResultComparator rcRCraw;
    ResultComparator rcRCraw2;
    ResultComparator rcRFind;
    ResultComparator rcRFind2;

    EnumSet<RefactoringType> refTypesOracle = EnumSet.of(
        RefactoringType.RENAME_CLASS,
        RefactoringType.MOVE_CLASS,
        RefactoringType.EXTRACT_SUPERCLASS,
        RefactoringType.EXTRACT_INTERFACE,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION,
        RefactoringType.EXTRACT_OPERATION,
        RefactoringType.INLINE_OPERATION,
        RefactoringType.MOVE_ATTRIBUTE,
        RefactoringType.PULL_UP_ATTRIBUTE,
        RefactoringType.PUSH_DOWN_ATTRIBUTE);

    EnumSet<RefactoringType> refTypesRCraw = EnumSet.of(
        RefactoringType.RENAME_CLASS,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION);

    EnumSet<RefactoringType> refTypesRFind = EnumSet.of(
        RefactoringType.EXTRACT_SUPERCLASS,
        RefactoringType.EXTRACT_INTERFACE,
        RefactoringType.RENAME_METHOD,
        RefactoringType.MOVE_OPERATION,
        RefactoringType.PUSH_DOWN_OPERATION,
        RefactoringType.PULL_UP_OPERATION,
        RefactoringType.EXTRACT_OPERATION,
        RefactoringType.INLINE_OPERATION,
        RefactoringType.MOVE_ATTRIBUTE,
        RefactoringType.PUSH_DOWN_ATTRIBUTE,
        RefactoringType.PULL_UP_ATTRIBUTE);
    
    public void run() {
        Arqsoft16Dataset oracle = new Arqsoft16Dataset();
        GitHistoryRefactoringMinerImpl rm1 = new GitHistoryRefactoringMinerImpl();
        GitRefactoringDetector rm2 = new RefDiff();

        RefactoringSet[] rmResults = getRmResults(rm1);
        RefactoringSet[] refDiffResults = getRmResults(rm2);
        RefactoringSet[] refFinderResults = readRefFinderResults();
        RefactoringSet[] refactoringCrawlerResults = readRefactoringCrawlerResults();

        rcRDiff = new ResultComparator();
        rcRDiff.expect(oracle.all());
        rcRDiff.compareWith("RDiff", refDiffResults);
        rcRDiff.compareWith("RMinr", rmResults);
        
        rcRCraw = new ResultComparator();
        rcRCraw.expect(oracle.all());
        rcRCraw.compareWith("RCraw", refactoringCrawlerResults);

        rcRCraw2 = new ResultComparator();
        rcRCraw2.expect(oracle.all());
        rcRCraw2.setIgnoreMoveToMovedType(true);
        rcRCraw2.compareWith("RCraw", refactoringCrawlerResults);
        
        rcRFind = new ResultComparator(false, true);
        rcRFind.expect(oracle.all());
        rcRFind.compareWith("RFind", refFinderResults);

        rcRFind2 = new ResultComparator(false, true);
        rcRFind2.expect(oracle.all());
        rcRFind2.setIgnoreMoveToMovedType(true);
        rcRFind2.setIgnoreMoveToRenamedType(true);
        rcRFind2.compareWith("RFind", refFinderResults);
        
//        rcRDiff.printSummary(System.out, refTypesOracle);
//        rcRDiff.printDetails(System.out, refTypesOracle);
        
//        rcRCraw.printSummary(System.out, refTypesRCraw);
//        rcRCraw2.printSummary(System.out, refTypesRCraw);
//        rcRCraw.printDetails(System.out, refTypesRCraw);
        
//        rcRFind.printSummary(System.out, refTypesRFind);
//        rcRFind2.printSummary(System.out, refTypesRFind);
//        rcRFind.printDetails(System.out, refTypesRFind);
        
        //printTable1();
        
        printTable3();
        
//        printTableOracle();
    }

    private void printTableOracle() {
        System.out.println("\\begin{tabular}{@{}l@{}}");
        System.out.println("\\toprule");
        System.out.println("Repository\\\\");
        System.out.println("\\midrule");
        
        System.out.println(String.format(Locale.US, "%s & %d\\\\", "Total", rcRDiff.getExpectedCount(refTypesOracle)));
        System.out.println(String.format(Locale.US, "%s & %d\\\\", "RCraw", rcRDiff.getExpectedCount(refTypesRCraw)));
        System.out.println(String.format(Locale.US, "%s & %d\\\\", "RFind", rcRDiff.getExpectedCount(refTypesRFind)));
        System.out.println("\\bottomrule");
        System.out.println("\\end{tabular}");
    }
    
    private void printTable1() {
        String[] tools = new String[] {"RDiff", "RMinr", "RCraw", "RCraw*", "RFind", "RFind*"};
        CompareResult[] results = new CompareResult[] {
            rcRDiff.getCompareResult("RDiff", refTypesOracle),
            rcRDiff.getCompareResult("RMinr", refTypesOracle),
            rcRCraw.getCompareResult("RCraw", refTypesRCraw),
            rcRCraw2.getCompareResult("RCraw", refTypesRCraw),
            rcRFind.getCompareResult("RFind", refTypesRFind),
            rcRFind2.getCompareResult("RFind", refTypesRFind)
        };
        System.out.println("\\begin{tabular}{@{}lrrrll@{}}");
        System.out.println("\\toprule");
        System.out.println("Approach & TP & FP & FN & Precision & Recall\\\\");
        System.out.println("\\midrule");
        for (int i = 0; i < tools.length; i++) {
            String tool = tools[i];
            CompareResult r = results[i];
            System.out.println(String.format(Locale.US, "%s & %d & %d & %d & \\xbar{%.3f} & \\xbar{%.3f} \\\\", tool, r.getTPCount(), r.getFPCount(), r.getFNCount(), r.getPrecision(), r.getRecall()));
        }
        System.out.println("\\bottomrule");
        System.out.println("\\end{tabular}");
    }

    private void printTable3() {
        String[] tools = new String[] {"RDiff ", "RMinr ", "RCraw", "RFind"};
        CompareResult[] results = new CompareResult[] {
            rcRDiff.getCompareResult("RDiff", refTypesOracle),
            rcRDiff.getCompareResult("RMinr", refTypesOracle),
            rcRCraw.getCompareResult("RCraw", refTypesRCraw),
            rcRFind.getCompareResult("RFind", refTypesRFind)
        };
        System.out.println("\\begin{tabular}{@{}lcccccccc@{}}");
        System.out.println("\\toprule");
        System.out.println("          & \\multicolumn{2}{c}{RDiff} & \\multicolumn{2}{c}{RMinr} & \\multicolumn{2}{c}{RCraw} & \\multicolumn{2}{c}{RFind}\\\\");
        System.out.println("\\cmidrule(lr){2-3} \\cmidrule(lr){4-5} \\cmidrule(lr){6-7} \\cmidrule(lr){8-9}");
        System.out.println("Ref. Type & Precision & Recall & Precision & Recall & Precision & Recall & Precision & Recall\\\\");
        System.out.println("\\midrule");
        
        table3Row(tools, results, "Rename Type", RefactoringType.RENAME_CLASS);
        table3Row(tools, results, "Move Type", RefactoringType.MOVE_CLASS);
        table3Row(tools, results, "Extract Superclass", RefactoringType.EXTRACT_SUPERCLASS, RefactoringType.EXTRACT_INTERFACE);
        table3Row(tools, results, "Rename Method", RefactoringType.RENAME_METHOD);
        table3Row(tools, results, "Pull Up Method", RefactoringType.PULL_UP_OPERATION);
        table3Row(tools, results, "Push Down Method", RefactoringType.PUSH_DOWN_OPERATION);
        table3Row(tools, results, "Move Method ", RefactoringType.MOVE_OPERATION);
        table3Row(tools, results, "Extract Method", RefactoringType.EXTRACT_OPERATION);
        table3Row(tools, results, "Inline Method", RefactoringType.INLINE_OPERATION);
        table3Row(tools, results, "Pull Up Field", RefactoringType.PULL_UP_ATTRIBUTE);
        table3Row(tools, results, "Push Down Field", RefactoringType.PUSH_DOWN_ATTRIBUTE);
        table3Row(tools, results, "Move Field ", RefactoringType.MOVE_ATTRIBUTE);
        
        System.out.println("\\bottomrule");
        System.out.println("\\end{tabular}");
    }
    
    private void table3Row(String[] tools, CompareResult[] results, String name, RefactoringType ... refactoringTypes) {
        System.out.print(String.format(Locale.US, "%s", name));
        for (int i = 0; i < tools.length; i++) {
            CompareResult r = results[i];
            CompareResult fr = r.filterBy(refactoringTypes);
            if ((fr.getTPCount() + fr.getFNCount()) > 0) {
                System.out.print(String.format(Locale.US, " & \\xbar{%.3f} & \\xbar{%.3f}", fr.getPrecision(), fr.getRecall()));
            } else {
                System.out.print(String.format(Locale.US, " &              &             "));
            }
        }
        System.out.println("\\\\");
    }

    private static RefactoringSet[] readRefactoringCrawlerResults() {
        String basePath = "data/refactoring-crawler-results/";
        return new RefactoringSet[] {
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317"),
            RefactoringCrawlerResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a")
        };
    }

    private static RefactoringSet[] readRefFinderResults() {
        String basePath = "data/ref-finder-results/";
        return new RefactoringSet[] {
            RefFinderResultReader.read("https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1", basePath + "atmosphere-cc2b3f1"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/clojure.git", "17217a1", basePath + "clojure-17217a1"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/guava.git", "79767ec", basePath + "guava-79767ec"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/metrics.git", "276d5e4", basePath + "metrics-276d5e4"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/orientdb.git", "b213aaf", basePath + "orientdb-b213aaf"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/retrofit.git", "f13f317", basePath + "retrofit-f13f317"),
            RefFinderResultReader.read("https://github.com/aserg-ufmg/spring-boot.git", "48e893a", basePath + "spring-boot-48e893a")
        };
    }

    private static RefactoringSet[] getRmResults(org.refactoringminer.api.GitHistoryRefactoringMiner rm) {
        return getRmResults(new RmAdapter(rm));
    }

    private static RefactoringSet[] getRmResults(GitRefactoringDetector rm) {
        return new RefactoringSet[] {
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/atmosphere.git", "cc2b3f1"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/clojure.git", "17217a1"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/guava.git", "79767ec"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/metrics.git", "276d5e4"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/orientdb.git", "b213aaf"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/retrofit.git", "f13f317"),
            ResultComparator.collectRmResult(rm, "https://github.com/aserg-ufmg/spring-boot.git", "48e893a") };
    }

}
