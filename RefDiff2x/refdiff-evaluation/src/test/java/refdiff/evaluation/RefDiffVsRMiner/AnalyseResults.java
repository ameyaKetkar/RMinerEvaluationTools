package refdiff.evaluation.RefDiffVsRMiner;

import com.fasterxml.jackson.databind.ObjectMapper;
import refdiff.core.ResultModels.ResultsOuterClass;
import refdiff.evaluation.RefactoringType;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
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

    public static void main(String[] a) throws IOException {


        Properties prop = new Properties();
        InputStream input = new FileInputStream("paths.properties");
        prop.load(input);
        String outputPath = prop.getProperty("ProtoPath");
        String projectPath = prop.getProperty("ProjectsPath");

        List<ResultsOuterClass.Results> results = TestBuilderRD.readAllResults(outputPath).stream()
                .collect(toList());

        List<RefactoringPopulatorRD.Root> roots = readJson();

        protos_minus_json(results,roots).forEach(System.out::println);

//                .forEach(x -> {
//            try {
//              //  db.processExistingRefactoringInOracle(x, UpdateDb.Tool.REFDIFF);
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        });
////////
////
////
////
      //  json_minus_protos(results,roots);
////
////
//      //  printCommit(results,"2ef067fc70055fc4d55c75937303414ddcf07e0e");
//
//        List<Tuple2<String, List<String>>> fp_refdiff_roots = roots.stream()
//                .map(x -> new Tuple2<>(x.sha1, x.refactorings.stream()
//                        .filter(r -> r.validation.equalsIgnoreCase("FP") && (r.detectionTools.contains("RefDiff") && r.detectionTools.contains("RD")))
//                        .map(r -> r.description)
//                        .collect(toList())))
//                .filter(x->!x.e2.isEmpty())
//                .collect(toList());
////////
//////
//////
//       falsePos_vs_falseNeg(results, fp_refdiff_roots);
//////
//        System.out.println("Total Commits: " + results.stream().map(x->x.getSha()).distinct().count());
////
        Map<String, ResultsOuterClass.Results> collect = results.stream().flatMap(fn).collect(toMap(x -> x.e2, fn1, binOp));
//
        collect.entrySet().stream().forEach(x -> System.out.println(x.getKey() + getString(x.getValue())));
//
//
////        System.out.println("**************");
////        System.out.println();
////        collect.get("Extract And Move Method").getFalsePositivesList().forEach(System.out::println);
////        System.out.println("**************");
//
        System.out.println(getString(collect.values().stream().reduce(ResultsOuterClass.Results.newBuilder().build(), binOp)));




    }

    private static List<Tuple2<String, String>> json_minus_protos(List<ResultsOuterClass.Results> results, List<RefactoringPopulatorRD.Root> roots) {
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

    private static List<String> protos_minus_json(List<ResultsOuterClass.Results> results, List<RefactoringPopulatorRD.Root> roots) {
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
//        res.forEach(x -> {
//                    System.out.println(x.toString());
//                    System.out.println("-----------------------------------");
//                });
//        res.stream().flatMap(x->x.getFalsePositivesList().stream().map(tp -> String.join(",", x.getSha(), tp, "FP")))
//                .forEach(System.out::println);

        System.out.println("FP: " + res.stream().mapToLong(x->x.getFalsePositivesList().size()).sum());
        System.out.println("TP: " + res.stream().mapToLong(x->x.getTruePositivesList().size()).sum());


        return Stream.concat( Stream.empty(),
                //res.stream().flatMap(x->x.getFalsePositivesList().stream().map(tp -> String.join(",", x.getSha(), tp, "FP"))),
                res.stream().flatMap(x->x.getTruePositivesList().stream().map(tp -> String.join(",", x.getSha(), tp, "TP")))).collect(toList());

    }


    private static void printCommit(List<ResultsOuterClass.Results> results, String sha){
        results.stream()
                .filter(x->x.getSha().equals(sha))
                .forEach(System.out::println);
    }

    private static List<RefactoringPopulatorRD.Root> readJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile ="D:\\MyProjects\\RefDiff-master\\refdiff-evaluation\\data\\icse" + "\\data.json";;
        return mapper.readValue(new File(jsonFile),
                mapper.getTypeFactory().constructCollectionType(List.class, RefactoringPopulatorRD.Root.class));
    }

    private static void changeFormat(){

//        List<String> dontCopyCommits = Arrays.asList("8184a32a019b2ed956e8f24c18cb49a266af47bf",
//                "9f7de200c9aef900596b09327a52d33241a68d9c",
//                "19d1936c3b07d97d88646aeae30de747715e3248",
//                "7668c875dfa7240b1ec08eb60b42107bae1b4cd3",
//                "c55a8c3761e9aae9f375d312c14b1bbb9ee9c0fa",
//                "1cacbe2ad700275bc575234ff2b32ee0d6493817",
//                "03ade425dd5a65d3a713d5e7d85aa7605956fbd2",
//                "c53c6f45988db869d56abe3b1d831ff775f4fa73",
//                "92e98858e742bbb669ccbf790a71a618c581df21",
//                "3bdcaa336a6e6a9727c333b433bb9f5d3afc0fb1",
//                "d5f10a4958f5e870680be906689d92d1efb42480",
//                "c7b6a7aa878aabd6400d2df0490e1eb2b810c8f9",
//                "cb2deceea993128c22710b0f64f1b755c9d176f7",
//                "f1b8ae1c44e6ba46118c2f66eae1725259acdccc",
//                "f1e26fa73074a89680a2e1756d85eb80ad87c3bf",
//                "ab98bcacf6e5bf1c3a06f6bcca68f178f880ffc9",
//                "de50b3becb31c367f867382ff9cd898ba1628350");

//        TestBuilderRD.readResult()
//                .stream()
////                .filter(x->!dontCopyCommits.contains(x.getSha()))
//                .forEach(r -> TestBuilderRD.write(r));

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

}
