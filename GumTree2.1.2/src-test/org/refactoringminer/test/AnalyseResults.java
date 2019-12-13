package org.refactoringminer.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.refactoringminer.api.RefactoringType;
import refdiff.core.ResultModels.ResultsOuterClass;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.toList;
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

    static class Tuple2<T,R>{

        public final T e1;
        public final R e2;

        Tuple2(T e1, R e2) {
            this.e1 = e1;
            this.e2 = e2;
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
//        results.stream().filter(x->x.getSha().equals("e78cda0fcf23de3973b659bc54f58a4e9b1f3bd3"))
//                .findFirst().ifPresent(System.out::println);
        List<RefactoringPopulator.Root> roots = null;
        try {
            roots = readJson();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total Commits: " + results.stream().map(x->x.getSha()).distinct().count());

        Map<String, ResultsOuterClass.Results> collect = results.stream().flatMap(fn).collect(toMap(x -> x.e2, fn1, binOp));

        collect.entrySet().stream().forEach(x -> System.out.println(getNameOfRefactoring(x.getKey()) + "  "
                + getString(x.getValue())));

        System.out.println(getString(collect.values().stream().reduce(ResultsOuterClass.Results.newBuilder().build(), binOp)));

        falsePos_vs_falseNeg(results);

        System.out.println("==========================================================================================================================");

        json_minus_protos(results,roots);

    }

    private static List<RefactoringPopulator.Root> readJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile ="D:\\MyProjects\\RefDiff-master\\refdiff-evaluation\\data\\icse" + "\\data.json";;
        return mapper.readValue(new File(jsonFile),
                mapper.getTypeFactory().constructCollectionType(List.class, RefactoringPopulator.Root.class));
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


    private static List<Tuple2<String, String>> json_minus_protos(List<ResultsOuterClass.Results> results, List<RefactoringPopulator.Root> roots) {
        Function<String, Optional<ResultsOuterClass.Results>> find = sha ->  results.stream().filter(x->x.getSha().equals(sha)).findFirst();

        List<Tuple2<ResultsOuterClass.Results, ResultsOuterClass.Results>> result_vs_oracle;
        result_vs_oracle = roots.stream()
                .map(r -> {
                    Map<String, List<String>> refsByValidation = r.refactorings.stream().filter(x -> x.detectionTools.contains("GumTree"))
                            .map(x -> new Tuple2<>(x.validation, x.description))
                            .collect(groupingBy(x -> x.e1, mapping(x -> x.e2, toList())));
                    ResultsOuterClass.Results.Builder res = ResultsOuterClass.Results.newBuilder()
                            .setSha(r.sha1);
                    if(refsByValidation.containsKey("TP"))
                        res.addAllTruePositives(refsByValidation.get("TP"));
                    if(refsByValidation.containsKey("CTP"))
                        res.addAllTruePositives(refsByValidation.get("CTP"));
                    if(refsByValidation.containsKey("FP"))
                        res.addAllFalsePositives(refsByValidation.get("FP"));
//                            if(refsByValidation.containsKey("TN"))
//                                res.addAllTrueNegatives(refsByValidation.get("TN"));
                    if(refsByValidation.containsKey("FN"))
                        res.addAllFalseNegatives(refsByValidation.get("FN"));
                    return res.build();

                })
                .map(x -> new Tuple2<>(x, find.apply(x.getSha())))
                .filter(x -> x.e2.isPresent())
                .map(x-> new Tuple2<>(x.e1,x.e2.get()))
                .collect(toList());

        BinaryOperator<List<String>> sub = (l1, l2) -> l1.stream().filter(x->!l2.contains(x)).collect(toList());

        Function<Tuple2<ResultsOuterClass.Results, ResultsOuterClass.Results>, ResultsOuterClass.Results> subtract = (t) ->
                ResultsOuterClass.Results.newBuilder()
                        .setSha(t.e1.getSha())
                        .addAllTruePositives(sub.apply(t.e1.getTruePositivesList(),t.e2.getTruePositivesList()))
//                    .addAllFalsePositives(sub.apply(t.e1.getFalsePositivesList(),t.e2.getFalsePositivesList()))
                        //     .addAllTrueNegatives(sub.apply(t.e1.getTrueNegativesList(),t.e2.getTrueNegativesList()))
//                    .addAllFalseNegatives(sub.apply(t.e1.getFalseNegativesList(),t.e2.getFalseNegativesList()))
                        .build();

        Predicate<ResultsOuterClass.Results> isEmpty = r -> r.getFalseNegativesCount() + r.getTruePositivesCount() + r.getTrueNegativesCount() + r.getFalsePositivesCount() == 0;

        List<ResultsOuterClass.Results> rs = result_vs_oracle.stream()
                .map(subtract)
                .filter(x -> !isEmpty.test(x))
                .collect(toList());

        rs.forEach(x -> {
            System.out.println(x.toString());
            System.out.println("-----------------------------------");
        });


        return Stream.concat(rs.stream().flatMap(x-> x.getTruePositivesList().stream().map(tp->new Tuple2<>(x.getSha(), tp))),
                Stream.concat(rs.stream().flatMap(x-> x.getFalseNegativesList().stream().map(fn ->new Tuple2<>(x.getSha(), fn))),
                        rs.stream().flatMap(x-> x.getFalsePositivesList().stream().map(fp->new Tuple2<>(x.getSha(), fp))))).collect(toList());

    }

}
