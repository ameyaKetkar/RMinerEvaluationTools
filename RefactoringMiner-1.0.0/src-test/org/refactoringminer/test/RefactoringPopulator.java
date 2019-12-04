package org.refactoringminer.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RefactoringPopulator {

	public enum Systems {
		FSE(1), All(2);
		private int value;

		private Systems(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum Refactorings {
		MoveMethod(1), MoveAttribute(2), InlineMethod(4), ExtractMethod(8), PushDownMethod(16), PushDownAttribute(
				32), PullUpMethod(64), PullUpAttribute(128), ExtractInterface(256), ExtractSuperclass(512), MoveClass(
						1024), RenamePackage(2048), RenameMethod(4096), ExtractAndMoveMethod(
								8192), RenameClass(16384), MoveSourceFolder(32768), All(65535);
		private int value;

		private Refactorings(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static void feedRefactoringsInstances(int refactoringsFlag, int systemsFlag, TestBuilder test)
			throws JsonParseException, JsonMappingException, IOException {

		if ((systemsFlag & Systems.FSE.getValue()) > 0) {
			prepareFSERefactorings(test, refactoringsFlag);
		}
	}

	private static void prepareFSERefactorings(TestBuilder test, int flag)
			throws JsonParseException, JsonMappingException, IOException {
		List<Root> refactorings = getFSERefactorings(flag);

		for (Root root : refactorings) {
			test.project(root.repository, "master").atCommit(root.sha1)
					.containsOnly(extractRefactorings(root.refactorings));
		}
	}

	public static String[] extractRefactorings(List<Refactoring> refactoring) {
		int count = 0;
		for (Refactoring ref : refactoring) {
			if (ref.validation.contains("TP"))
				count++;
		}
		String[] refactorings = new String[count];
		int counter = 0;
		for (Refactoring ref : refactoring) {
			if (ref.validation.contains("TP")) {
				refactorings[counter++] = ref.description;
			}
		}
		return refactorings;
	}

	public static List<Root> getFSERefactorings(int flag) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		String jsonFile = System.getProperty("user.dir") + "/src-test/Data/data.json";

		List<Root> roots = mapper.readValue(new File(jsonFile),
				mapper.getTypeFactory().constructCollectionType(List.class, Root.class));

		List<Root> filtered = new ArrayList<>();

		for (Root root : roots) {
			List<Refactoring> refactorings = new ArrayList<>();

			root.refactorings.forEach((refactoring) -> {
				if (isAdded(refactoring, flag))
					refactorings.add(refactoring);
			});

			if (refactorings.size() > 0) {
				Root tmp = root;
				tmp.refactorings = refactorings;
				filtered.add(tmp);
			}
		}
		return filtered;
	}

	private static boolean isAdded(Refactoring refactoring, int flag) {
		try {
			return ((Enum.valueOf(Refactorings.class, refactoring.type.replace(" ", "")).getValue() & flag) > 0);

		} catch (Exception e) {
			return false;
		}
	}

	public static void printRefDiffResults(int flag) {
		Hashtable<String, Tuple> result = new Hashtable<>();
		try {
			List<Root> roots = getFSERefactorings(flag);
			for (Refactorings ref : Refactorings.values()) {
				if (ref == Refactorings.All)
					continue;
				result.put(ref.toString(), new Tuple());
			}
			for (Root root : roots) {
				for (Refactoring ref : root.refactorings) {
					Tuple tuple = result.get(ref.type.replace(" ", ""));
					tuple.totalTruePositives += ref.validation.contains("TP") ? 1 : 0;
					tuple.unknown += ref.validation.equals("UKN") ? 1 : 0;

					if (ref.detectionTools.contains("RefDiff")) {
						tuple.refDiffTruePositives += ref.validation.contains("TP") ? 1 : 0;
						tuple.refDiffFalsePositives += ref.validation.equals("FP") ? 1 : 0;
					}

				}
			}
			Tuple[] tmp = {};
			System.out.println("Total\t" + buildResultMessage(result.values().toArray(tmp)));
			for (String key : result.keySet()) {
				System.out.println(getInitials(key) + "\t" + buildResultMessage(result.get(key)));
			}
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String getInitials(String str) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < str.length(); i++) {
			String character = str.substring(i, i + 1);
			if (character == character.toUpperCase())
				sb.append(character);
		}
		return sb.toString();
	}

	private static String buildResultMessage(Tuple... result) {
		int trueP = 0;
		int total = 0;
		int ukn = 0;
		int falseP = 0;
		for (Tuple res : result) {
			trueP += res.refDiffTruePositives;
			total += res.totalTruePositives;
			ukn += res.unknown;
			falseP += res.refDiffFalsePositives;
		}
		double precision = trueP / (double) (trueP + falseP);
		double recall = trueP / (double) (total);
		try {
			String mainResultMessage = String.format("TP: %2d  FP: %2d  FN: %2d  Unk.: %2d  Prec.: %.3f  Recall: %.3f",
					(int) trueP, (int) falseP, (int) (total - trueP), ukn, precision, recall);
			return mainResultMessage;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static class Tuple {
		public int totalTruePositives;
		public int refDiffTruePositives;
		public int falseNegatives;
		public int unknown;
		public int refDiffFalsePositives;
	}

	public static class Root {
		public int id;
		public String repository;
		public String sha1;
		public String url;
		public String author;
		public String time;
		public List<Refactoring> refactorings;
		public long refDiffExecutionTime;

	}

	public static class Refactoring {
		public String type;
		public String description;
		public String comment;
		public String validation;
		public String detectionTools; 
		public String validators;

	}

	public static class Comment {
		public String refactored;
		public String link;
		public String message;
		public String type;
		public String reportedCase;
	}
 
}
