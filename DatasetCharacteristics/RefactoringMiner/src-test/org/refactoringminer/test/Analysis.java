package org.refactoringminer.test;

import org.refactoringminer.ProtoUtil;

import java.nio.file.Paths;

public class Analysis {

    public static void main(String a[]){
        ProtoUtil.ReadWriteAt rw = new ProtoUtil.ReadWriteAt(Paths.get("/Users/ameya/Research/RMinerEvaluationTools/DatasetCharacteristics/ReplacementsData/"));
        var ms = rw.readAll("MatchedStatements", "MatchedStatements");
        System.out.println();
    }
}
