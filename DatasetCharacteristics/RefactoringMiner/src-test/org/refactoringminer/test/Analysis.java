package org.refactoringminer.test;

import org.refactoringminer.MatchedStatementsOuterClass.MatchedStatements;
import org.refactoringminer.ProtoUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class Analysis {

    public static void main(String[] args){
        Path pathToData = Paths.get("/Users/ameya/Research/RMinerEvaluationTools/DatasetCharacteristics/ReplacementsData/");
        ProtoUtil.ReadWriteAt rw = new ProtoUtil.ReadWriteAt(pathToData);
        List<MatchedStatements> ms = rw.readAll("MatchedStatements", "MatchedStatements");

        // Collected matched statements for refactorings:
        var refactoringKinds = ms.stream().filter(x -> x.getStatementsCount() > 0).map(x -> x.getRefactoringKind()).collect(Collectors.toSet());

        var replacementTypes = ms.stream().flatMap(x -> x.getStatementsList().stream().flatMap(z -> z.getReplcementInferredList().stream()))
               .map(x -> x.getReplacementType()).distinct().collect(toList());

        // CSV1
        String header1 = String.join(",", "Refactoring Type" , "isReferenceKind"
                , "# Matched statements" ,  "# of Equal statements", "# of Replacements"
                , String.join(",", replacementTypes));
        var csv1 = header1 + "\n" +ms.stream().map(x -> getCsvEntry(x, replacementTypes)).collect(joining("\n"));

        // refactoring type, occurences ,isReferenceKind?, noOfMatchedStatements,noOfEqualStatements, TotalReplacements, [Replacements]
        // CSV1
        String header2 = String.join(",", "Refactoring Type" , "Occurences", "isReferenceKind"
                , "# Matched statements" ,  "# of Equal statements", "# of Replacements"
                , String.join(",", replacementTypes));

        var csv2 = header2 + "\n" + ms.stream().collect(groupingBy(x -> x.getRefactoringKind())).values()
                .stream().map(x -> getCsvEntry(x, replacementTypes)).collect(Collectors.joining("\n"));


        System.out.println();
    }

    public static boolean isReferenceKind(String refactoringType){
        return refactoringType.contains("Variable")
                || refactoringType.contains("Attribute")
                || refactoringType.contains("Parameter")
                || refactoringType.contains("return");
    }


    // refactoring type, isReferenceKind?, noOfMatchedStatements, noOfEqualStatements, TotalReplacements, [Replacements]
    public static String getCsvEntry(MatchedStatements m, List<String> replacements){
        StringJoiner str = new StringJoiner(",");
        str.add(m.getRefactoringKind());
        str.add(String.valueOf(isReferenceKind((m.getRefactoringKind()))));
        str.add(String.valueOf(m.getStatementsCount()));
        str.add(String.valueOf(m.getStatementsList().stream().filter(x-> x.getIsSame()).count()));
        str.add(String.valueOf(m.getStatementsList().stream().mapToInt(x -> x.getReplcementInferredCount()).sum()));
        var replacementsInferred = m.getStatementsList().stream().flatMap(x -> x.getReplcementInferredList().stream())
                .collect(Collectors.groupingBy(x -> x.getReplacementType(), counting()));
        replacements.stream().map(x -> String.valueOf(replacementsInferred.getOrDefault(x, 0L)))
                .forEach(str::add);
        return str.toString();
    }

    // refactoring type, occurences ,isReferenceKind?, noOfMatchedStatements,noOfEqualStatements, TotalReplacements, [Replacements]
    public static String getCsvEntry(List<MatchedStatements> ms, List<String> replacements){
        StringJoiner str = new StringJoiner(",");
        str.add(String.valueOf(ms.get(0).getRefactoringKind()));
        str.add(String.valueOf(ms.size()));
        str.add(String.valueOf(isReferenceKind((ms.get(0).getRefactoringKind()))));
        str.add(String.valueOf(ms.stream().mapToInt(x -> x.getStatementsCount()).sum()));
        str.add(String.valueOf(ms.stream().mapToLong(x -> x.getStatementsList().stream().filter(s -> s.getIsSame()).count()).sum()));
        str.add(String.valueOf(ms.stream().mapToLong(x -> x.getStatementsList().stream().mapToInt(s -> s.getReplcementInferredList().size()).sum()).sum()));
        var replacementsInferred = ms.stream().flatMap(x -> x.getStatementsList().stream()).flatMap(x -> x.getReplcementInferredList().stream())
                .collect(Collectors.groupingBy(x -> x.getReplacementType(), counting()));
        replacements.stream().map(x -> String.valueOf(replacementsInferred.getOrDefault(x, 0L)))
                .forEach(str::add);
        return str.toString();
    }
}
