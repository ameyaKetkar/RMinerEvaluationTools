package test;

import com.fasterxml.jackson.databind.ObjectMapper;
import refdiff.core.ResultModels.ResultsOuterClass;
import refdiff.core.api.RefactoringType;

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

    static String getRefType(String description){
        return refTypes.stream().filter(description::startsWith).findFirst().get();
    }

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

    public static void main(String[] a) throws IOException {

        Properties prop = new Properties();
        InputStream input = new FileInputStream("paths.properties");
        prop.load(input);
        String outputPath = prop.getProperty("ProtoPath");

        List<ResultsOuterClass.Results> results = TestBuilderRD.readAllResults(outputPath);

        System.out.println("Total Commits: " + results.stream().map(x->x.getSha()).distinct().count());

        Map<String, ResultsOuterClass.Results> collect = results.stream().flatMap(fn).collect(toMap(x -> x.e2, fn1, binOp));

        collect.entrySet().stream().forEach(x -> System.out.println(x.getKey() + getString(x.getValue())));

        System.out.println(getString(collect.values().stream().reduce(ResultsOuterClass.Results.newBuilder().build(), binOp)));


    }


    private static List<RefactoringPopulatorRD.Root> readJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFile ="C:\\RefDiff0.1.1\\RefDiff\\refdiff-core\\src-test\\Data\\data.json";
        return mapper.readValue(new File(jsonFile),
                mapper.getTypeFactory().constructCollectionType(List.class, RefactoringPopulatorRD.Root.class));
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

}
