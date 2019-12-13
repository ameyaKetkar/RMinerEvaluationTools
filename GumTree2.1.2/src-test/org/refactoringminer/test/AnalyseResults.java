package org.refactoringminer.test;

import org.refactoringminer.api.RefactoringType;
import refdiff.core.ResultModels.ResultsOuterClass;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

public class AnalyseResults {

    static class Tuple3<T>{

        public final T e1, e2,e3;

        Tuple3(T e1, T e2, T e3) {
            this.e1 = e1;
            this.e2 = e2;
            this.e3 = e3;
        }
    }

    static Set<String> refTypes = Arrays.stream(RefactoringType.values()).map(x -> x.getDisplayName())
            .collect(Collectors.toSet());
    static BiFunction<String, List<String>, Stream<Tuple3<String>>> extract =  (k, rs) -> rs.stream()
            .map(r->new Tuple3<>(k,refTypes.stream().filter(r::startsWith).findFirst().get(),r));

    static Function<ResultsOuterClass.Results, Stream<Tuple3<String>>> fn = res ->
            concat(concat(extract.apply("FN", res.getFalseNegativesList()), extract.apply("TN",res.getTrueNegativesList()))
                    , concat(extract.apply("FP",res.getFalsePositivesList()), extract.apply("TP",res.getTruePositivesList())));

   static Function<Tuple3<String>, ResultsOuterClass.Results> fn1 = t -> {
        ResultsOuterClass.Results.Builder bldr = ResultsOuterClass.Results.newBuilder();
        if (t.e1.equals("FN")) return bldr.addFalseNegatives(t.e3).build();
        if (t.e1.equals("TN")) return bldr.addTrueNegatives(t.e3).build();
        if (t.e1.equals("FP")) return bldr.addFalsePositives(t.e3).build();
        if (t.e1.equals("TP")) return bldr.addTruePositives(t.e3).build();
        return bldr.build();
    };

    static BinaryOperator<ResultsOuterClass.Results> binOp = (r1, r2) ->
            r1.toBuilder().addAllFalseNegatives(r2.getFalseNegativesList())
                    .addAllFalsePositives(r2.getFalsePositivesList())
                    .addAllTrueNegatives(r2.getTrueNegativesList())
                    .addAllTruePositives(r2.getTruePositivesList()).build();

    public static DecimalFormat df = new DecimalFormat("#.###");

    public static void main(String[] a){

        List<ResultsOuterClass.Results> results = TestBuilder.readAllResults();
        results.stream().filter(x ->x.getTruePositivesCount() > 0 || x.getFalsePositivesCount() > 0)
                .findFirst().ifPresent(System.out::println);
        falsePos_vs_falseNeg(results);

        System.out.println("Total Commits: " + results.stream().map(x->x.getSha()).distinct().count());

        Map<String, ResultsOuterClass.Results> collect = results.stream().flatMap(fn).collect(toMap(x -> x.e2, fn1, binOp));

        collect.entrySet().stream().forEach(x -> System.out.println(getNameOfRefactoring(x.getKey()) + "  "
                + getSbar(x.getValue())));

        System.out.println(getString(collect.values().stream().reduce(ResultsOuterClass.Results.newBuilder().build(), binOp)));

        //Move Method & \sbar{245}{254} & \sbar{245}{268} & \sbar{206}{738} & \sbar{206}{268} \\


    }

    private static String getNameOfRefactoring(String s){
        return s.chars().filter(Character::isUpperCase)
                .mapToObj(x -> Character.valueOf((char)x).toString())
                .collect(Collectors.joining());
    }

//    private static String getColumnFor(ResultsOuterClass.Results res){
//        return res.
//    }

    private static String getSbar(ResultsOuterClass.Results x){
        int tp = x.getTruePositivesCount();
        int falsePositivesCount = x.getFalsePositivesCount();
        int falseNegativesCount = x.getFalseNegativesCount();
        int trueNegativesCount = x.getTrueNegativesCount();
        return "& \\sbar{" + tp + "}{" + (tp + falsePositivesCount) + "}"
                + "    "
                + "& \\sbar{" + tp + "}{" + (tp + falseNegativesCount) + "}";
    }

    private static String getString(ResultsOuterClass.Results x) {
        int tp = x.getTruePositivesCount();
        return "    TP:" + tp +
                "    FP:" + x.getFalsePositivesCount() +
                "    FN:" + x.getFalseNegativesCount() +
                "    TN:" + x.getTrueNegativesCount() +
                "    Precision:" + df.format((double) tp / (tp + x.getFalsePositivesCount())) +
                "    Recall:" + df.format((double)tp /(tp + x.getFalseNegativesCount()));
    }

    private static void falsePos_vs_falseNeg(List<ResultsOuterClass.Results> results) {
        results.stream()
                .filter(x->x.getFalsePositivesCount() > 0 && x.getFalseNegativesCount() > 0)
                .forEach(x -> {
                    System.out.println(x.getSha());
                    System.out.println(x.getFalsePositivesList().stream().collect(Collectors.joining("\n + ")));
                    System.out.println(x.getFalseNegativesList().stream().collect(Collectors.joining("\n - ")));
                    System.out.println("--------------------");
                });
    }

}
