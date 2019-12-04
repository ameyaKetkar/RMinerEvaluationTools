package org.refactoringminer.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.refactoringminer.api.RefactoringType;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import refdiff.core.ResultModels.ResultsOuterClass;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Stream.concat;

public class AnalyseResults {


    public static void main(String[] a) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream("paths.properties"));
        String outputPath = prop.getProperty("ProtoPath");
        TestBuilder test = new TestBuilder(new GitHistoryRefactoringMinerImpl(), RefactoringPopulator.Refactorings.All.getValue(), outputPath);
        List<ResultsOuterClass.Results> results = new ArrayList<>(test.readAllResults());
        List<RefactoringPopulator.Root> roots = readJson();

        List<Tuple2<String, List<String>>> fp_refdiff_roots = roots.stream()
                .map(x -> new Tuple2<>(x.sha1, x.refactorings.stream()
                        .filter(r -> r.validation.equalsIgnoreCase("FP") && (r.detectionTools.contains("RefDiff") && r.detectionTools.contains("RD")))
                        .map(r -> r.description)
                        .collect(toList())))
                .filter(x->!x.e2.isEmpty())
                .collect(toList());

        System.out.println("Total Commits: " + results.stream().map(x->x.getSha()).distinct().count());
        Map<String, ResultsOuterClass.Results> collect = results.stream().flatMap(fn).collect(toMap(x -> x.e2, fn1, binOp));
        collect.entrySet().stream().forEach(x -> System.out.println(x.getKey() + getString(x.getValue())));
        System.out.println(getString(collect.values().stream().reduce(ResultsOuterClass.Results.newBuilder().build(), binOp)));
    }

    private static List<Tuple2<String, String>> json_minus_protos(List<ResultsOuterClass.Results> results, List<RefactoringPopulator.Root> roots) {
        Function<String, Optional<ResultsOuterClass.Results>> find = sha ->  results.stream().filter(x->x.getSha().equals(sha)).findFirst();

        List<Tuple2<ResultsOuterClass.Results, ResultsOuterClass.Results>> result_vs_oracle;
        result_vs_oracle = roots.stream()
                .map(r -> {
                    Map<String, List<String>> refsByValidation = r.refactorings.stream().filter(x -> x.detectionTools.contains("RD-2x"))
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
                    .addAllFalsePositives(sub.apply(t.e1.getFalsePositivesList(),t.e2.getFalsePositivesList()))
                    .addAllFalseNegatives(sub.apply(t.e1.getFalseNegativesList(),t.e2.getFalseNegativesList()))
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

    private static List<String> protos_minus_json(List<ResultsOuterClass.Results> results, List<RefactoringPopulator.Root> roots) {
        Function<String, Optional<ResultsOuterClass.Results>> findSha = sha ->  results.stream().filter(x->x.getSha().equals(sha)).findFirst();

        List<Tuple2<ResultsOuterClass.Results, ResultsOuterClass.Results>> result_vs_oracle = roots.stream()
                .map(r -> {
                        Map<String, List<String>> refsByValidation = r.refactorings.stream().filter(x -> x.detectionTools.contains("RefDiff") || x.detectionTools.contains("RD"))
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
                        if(refsByValidation.containsKey("TN"))
                            res.addAllTrueNegatives(refsByValidation.get("TN"));
                        if(refsByValidation.containsKey("FN"))
                            res.addAllFalseNegatives(refsByValidation.get("FN"));
                        return res.build();

                })
                .map(x -> new Tuple2<>(findSha.apply(x.getSha()),x))
                .filter(x -> x.e1.isPresent())
                .map(x-> new Tuple2<>(x.e1.get(),x.e2))
                .collect(toList());

        BinaryOperator<List<String>> sub = (l1, l2) -> l1.stream()
                .filter(x->!l2.contains(x)).collect(toList());

        Function<Tuple2<ResultsOuterClass.Results, ResultsOuterClass.Results>, ResultsOuterClass.Results> subtract = (t) ->
                ResultsOuterClass.Results.newBuilder()
                        .setSha(t.e1.getSha())
                        .addAllTruePositives(sub.apply(t.e1.getTruePositivesList(),t.e2.getTruePositivesList()))
                        .addAllFalsePositives(sub.apply(t.e1.getFalsePositivesList(),t.e2.getFalsePositivesList()))
                        .addAllTrueNegatives(sub.apply(t.e1.getTrueNegativesList(),t.e2.getTrueNegativesList()))
                  //      .addAllFalseNegatives(sub.apply(t.e1.getFalseNegativesList(),t.e2.getFalseNegativesList()))
                        .build();

        Predicate<ResultsOuterClass.Results> isEmpty = r -> r.getFalseNegativesCount() + r.getTruePositivesCount() + r.getTrueNegativesCount() + r.getFalsePositivesCount() == 0;

        List<ResultsOuterClass.Results> res = result_vs_oracle.stream()
                .map(subtract)
                .filter(x -> !isEmpty.test(x))
                .collect(toList());

        System.out.println("FP: " + res.stream().mapToLong(x->x.getFalsePositivesList().size()).sum());
        System.out.println("TP: " + res.stream().mapToLong(x->x.getTruePositivesList().size()).sum());


        return Stream.concat(res.stream().flatMap(x->x.getFalsePositivesList().stream().map(tp -> String.join(",", x.getSha(), tp, "FP"))),
                res.stream().flatMap(x->x.getTruePositivesList().stream().map(tp -> String.join(",", x.getSha(), tp, "TP")))).collect(toList());

    }



    private static List<RefactoringPopulator.Root> readJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile =System.getProperty("user.dir") + "/src-test/Data/data.json";
        return mapper.readValue(new File(jsonFile),
                mapper.getTypeFactory().constructCollectionType(List.class, RefactoringPopulator.Root.class));
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

    private static void falsePos_vs_falseNeg(List<ResultsOuterClass.Results> results, List<Tuple2<String, List<String>>> fp_refdiff_roots) {
        results.stream() //&& x.getFalseNegativesCount() > 0
                .filter(x->x.getFalsePositivesCount() > 0 )
                .forEach(x -> {
                    Optional<Tuple2<String, List<String>>> fp_refdiff_root = fp_refdiff_roots.stream()
                            .filter(z->z.e1.equalsIgnoreCase(x.getSha())).findAny();
                    List<String> fp_refdiff_l = x.getFalsePositivesList().stream()
                            .filter(r -> fp_refdiff_root.map(f -> f.e2.stream().noneMatch(r::equals)).orElse(true))
                            .collect(toList());
                    if(fp_refdiff_l.size()>0) {
                        System.out.println(x.getSha());
                        System.out.println( "+" + String.join("\n + ", fp_refdiff_l));
                        System.out.println("-" + String.join("\n - ", x.getFalseNegativesList()));
                        System.out.println("--------------------");
                    }

                });
    }

    static class Tuple3<T>{

        public final T e1, e2,e3;

        Tuple3(T e1, T e2, T e3) {
            this.e1 = e1;
            this.e2 = e2;
            this.e3 = e3;
        }
    }

    static class Tuple2<T, U>{

        public final T e1;
        public final U e2;

        Tuple2(T e1, U e2) {
            this.e1 = e1;
            this.e2 = e2;
        }
    }

    static Set<String> refTypes = Arrays.stream(RefactoringType.values()).map(x -> x.getDisplayName())
            .collect(Collectors.toSet());
    static BiFunction<String, List<String>, Stream<Tuple3<String>>> extract =  (k, rs) -> rs.stream()
            .map(r->new Tuple3<>(k,refTypes.stream().filter(r::startsWith).findFirst().orElse(""),r));

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

    static String getRefType(String description){
        return refTypes.stream().filter(description::startsWith).findFirst().get();
    }

}
