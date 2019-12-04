package org.refactoringminer.api;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.refactoringminer.util.AstUtils;
import org.refactoringminer.utils.RefactoringRelationship;

public enum RefactoringType {

	EXTRACT_OPERATION("Extract Method", "Extract Method (.+) extracted from (.+) in class (.+)", 2),
	RENAME_CLASS("Rename Class", "Rename Class (.+) renamed to (.+)"),
	MOVE_ATTRIBUTE("Move Attribute", "Move Attribute (.+) from class (.+) to class (.+)"),
	RENAME_METHOD("Rename Method", "Rename Method (.+) renamed to (.+) in class (.+)"),
	INLINE_OPERATION("Inline Method", "Inline Method (.+) inlined to (.+) in class (.+)", 2),
	MOVE_OPERATION("Move Method", "Move Method (.+) from class (.+) to (.+) from class (.+)"),
	PULL_UP_OPERATION("Pull Up Method", "Pull Up Method (.+) from class (.+) to (.+) from class (.+)", 1, 2),
	MOVE_CLASS("Move Class", "Move Class (.+) moved to (.+)"),
	MOVE_RENAME_CLASS("Move And Rename Class", ".+"),
	MOVE_SOURCE_FOLDER("Move Source Folder", "Move Source Folder (.+) to (.+)"),
	PULL_UP_ATTRIBUTE("Pull Up Attribute", "Pull Up Attribute (.+) from class (.+) to class (.+)", 2),
	PUSH_DOWN_ATTRIBUTE("Push Down Attribute", "Push Down Attribute (.+) from class (.+) to class (.+)", 3),
	PUSH_DOWN_OPERATION("Push Down Method", "Push Down Method (.+) from class (.+) to (.+) from class (.+)", 3, 4),
	EXTRACT_INTERFACE("Extract Interface", "Extract Interface (.+) from classes \\[(.+)\\]", 2),
	EXTRACT_SUPERCLASS("Extract Superclass", "Extract Superclass (.+) from classes \\[(.+)\\]", 2),
	MERGE_OPERATION("Merge Method", ".+"),
	EXTRACT_AND_MOVE_OPERATION("Extract And Move Method", ".+"),
	CONVERT_ANONYMOUS_CLASS_TO_TYPE("Convert Anonymous Class to Type", ".+"),
	INTRODUCE_POLYMORPHISM("Introduce Polymorphism", ".+"),
	RENAME_PACKAGE("Rename Package", "Rename Package (.+) to (.+)"),
  CHANGE_METHOD_SIGNATURE("Change Method Signature", "Change Method Signature (.+) to (.+) in class (.+)");

	private String displayName;
	private Pattern regex;
	private int[] aggregateGroups;

	private RefactoringType(String displayName, String regex, int ... aggregateGroups) {
		this.displayName = displayName;
		this.regex = Pattern.compile(regex);
		this.aggregateGroups = aggregateGroups;
	}

	public Pattern getRegex() {
        return regex;
    }

    public String getDisplayName() {
		return this.displayName;
	}

    public String getAbbreviation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.displayName.length(); i++) {
            char c = this.displayName.charAt(i);
            if (Character.isLetter(c) && Character.isUpperCase(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String aggregate(String refactoringDescription) {
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            StringBuilder sb = new StringBuilder();
            int current = 0;
            int replace = 0;
            for (int g = 1; g <= m.groupCount(); g++) {
                sb.append(refactoringDescription, current, m.start(g));
                if (aggregateGroups.length > replace && g == aggregateGroups[replace]) {
                    sb.append('*');
                    replace++;
                } else {
                    sb.append(refactoringDescription, m.start(g), m.end(g));
                }
                current = m.end(g);
            }
            sb.append(refactoringDescription, current, refactoringDescription.length());
            return sb.toString();
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    public static void parse(String refactoringDescription, Collection<RefactoringRelationship> result) {
        RefactoringType refType = extractFromDescription(refactoringDescription);
        Matcher m = refType.regex.matcher(refactoringDescription);
        if (m.matches()) {
            switch (refType) {
            case RENAME_CLASS:
            case MOVE_CLASS:
            case RENAME_PACKAGE: {
                String entityBefore = m.group(1);
                String entityAfter = m.group(2);
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case MOVE_OPERATION:
            case PULL_UP_OPERATION:
            case PUSH_DOWN_OPERATION: {
                String entityBefore = methodKey(m.group(1), m.group(2));
                String entityAfter = methodKey(m.group(3), m.group(4));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case RENAME_METHOD:
            case INLINE_OPERATION: {
                String entityBefore = methodKey(m.group(1), m.group(3));
                String entityAfter = methodKey(m.group(2), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case EXTRACT_OPERATION: {
                String entityBefore = methodKey(m.group(2), m.group(3));
                String entityAfter = methodKey(m.group(1), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            case EXTRACT_INTERFACE:
            case EXTRACT_SUPERCLASS: {
                String entityAfter = m.group(1);
                String[] entityBeforeArray = m.group(2).split(" *, *");
                for (String entityBefore : entityBeforeArray) {
                    result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                }
                return;
            }
            case MOVE_ATTRIBUTE:
            case PULL_UP_ATTRIBUTE:
            case PUSH_DOWN_ATTRIBUTE: {
                String entityBefore = attributeKey(m.group(1), m.group(2));
                String entityAfter = attributeKey(m.group(1), m.group(3));
                result.add(new RefactoringRelationship(refType, entityBefore, entityAfter));
                return;
            }
            default:
                throw new RuntimeException("Unable do parse: " + refactoringDescription);
            }
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    private static String methodKey(String methodSignature, String typeKey) {
        return typeKey + "#" + AstUtils.normalizeMethodSignature(methodSignature);
    }

    private static String attributeKey(String attribute, String typeKey) {
        return typeKey + "#" + AstUtils.normalizeAttribute(attribute);
    }

    public List<RefactoringRelationship> parseRefactoring(String refactoringDescription) {
        List<RefactoringRelationship> result;
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            
            for (int g = 1; g <= m.groupCount(); g++) {
                
            }
            return null;
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }

    public static RefactoringType extractFromDescription(String refactoringDescription) {
        for (RefactoringType refType : RefactoringType.values()) {
            if (refactoringDescription.startsWith(refType.getDisplayName())) {
                return refType;
            }
        }
        throw new RuntimeException("Unknown refactoring type: " + refactoringDescription);
    }

    public String getGroup(String refactoringDescription, int group) {
        Matcher m = regex.matcher(refactoringDescription);
        if (m.matches()) {
            return m.group(group);
        } else {
            throw new RuntimeException("Pattern not matched: " + refactoringDescription);
        }
    }
    
    public static RefactoringType fromName(String name) {
      String lcName = name.toLowerCase();
      for (RefactoringType rt : RefactoringType.values()) {
        if (lcName.equals(rt.getDisplayName().toLowerCase())) {
          return rt;
        }
      }
      throw new IllegalArgumentException("refactoring type not known " + name);
    }
}
