package org.refactoringminer.rm1;

import com.github.gumtreediff.actions.ActionGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.Mapping;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.CodeRange;
import org.eclipse.jdt.core.dom.ASTNode;
import org.refactoringminer.api.Refactoring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;

public class GumTreeDiff { 
//	Logger logger = LoggerFactory.getLogger(GumTreeDiff.class);
//	private String commitURL;
//
//	public GumTreeDiff(String commitURL) {
////		this.commitURL = commitURL;
//	}
 
	public enum Tool { 
		REFACTORING_MINER(1), REFDIFF(2), GUMTREEDIFF(3); 
		private int id; 
 
		private Tool(int id) { 
			this.id = id; 
		} 
 
		public int getId() { 
			return id; 
		} 
	} 
 
	public Set<RefactoringInfo> treeDiffFile(Map<String, String> fileContentsBefore, Map<String, String> fileContentsCurrent) throws IOException { 
		Set<RefactoringInfo> refactorings = new LinkedHashSet<RefactoringInfo>(); 
		for(String filePath : fileContentsBefore.keySet()) { 
			if(fileContentsCurrent.containsKey(filePath)) { 
				TreeContext src = new JdtTreeGenerator().generateFromString(fileContentsBefore.get(filePath)); 
				TreeContext dst = new JdtTreeGenerator().generateFromString(fileContentsCurrent.get(filePath)); 
				refactorings.addAll(treeDiffForTypeChanges(src, dst, filePath, fileContentsBefore.get(filePath), fileContentsCurrent.get(filePath))); 
			} 
		} 
		return refactorings; 
	}
	public static final String systemFileSeparator = Matcher.quoteReplacement(File.separator);
	public Set<RefactoringInfo> treeDiffGitHubAPI(List<String> filesBefore, List<String> filesCurrent, File currentFolder, File parentFolder) throws IOException { 
		Set<RefactoringInfo> refactorings = new LinkedHashSet<RefactoringInfo>(); 
		for(String filePath : filesBefore) { 
			if(filesCurrent.contains(filePath)) { 
				File f1 = new File(parentFolder + File.separator + filePath.replaceAll("/", systemFileSeparator));
				File f2 = new File(currentFolder + File.separator + filePath.replaceAll("/", systemFileSeparator));
				TreeContext src = new JdtTreeGenerator().generateFromFile(f1); 
				TreeContext dst = new JdtTreeGenerator().generateFromFile(f2); 
				refactorings.addAll(treeDiffForTypeChanges(src, dst, filePath, readFileContents(f1), readFileContents(f2))); 
			} 
		} 
		return refactorings; 
	} 
 
	private String readFileContents(File file) { 
		try { 
			InputStream in = new FileInputStream(file); 
			InputStreamReader isr = new InputStreamReader(in); 
			StringWriter sw = new StringWriter(); 
			int DEFAULT_BUFFER_SIZE = 1024 * 4; 
			char[] buffer = new char[DEFAULT_BUFFER_SIZE]; 
			int n = 0; 
			while (-1 != (n = isr.read(buffer))) { 
				sw.write(buffer, 0, n); 
			} 
			isr.close(); 
			return sw.toString(); 
		} catch (IOException e) { 
			e.printStackTrace(); 
		} 
		return ""; 
	} 
	 

 
	private Set<RefactoringInfo> treeDiffForTypeChanges(TreeContext src, TreeContext dst, String filePath, String fileContentsBefore, String fileContentsCurrent) { 
		Set<RefactoringInfo> refactorings = new LinkedHashSet<RefactoringInfo>(); 
		com.github.gumtreediff.matchers.Matcher m = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot()); 
		m.match(); 
		ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), m.getMappings()); 
		g.generate(); 
		List<Action> actions = g.getActions(); 
		Map<String, List<Action>> actionCountMap = new LinkedHashMap<String, List<Action>>(); 
		for(Action action : actions) { 
			String actionAsString = action.toString(); 
			ITree node = action.getNode(); 
			if(action.getName().equals("UPD") && 
					isTypeNode(src, node)) { 
				if(actionCountMap.containsKey(actionAsString)) { 
					actionCountMap.get(actionAsString).add(action); 
				} 
				else { 
					List<Action> renameActions = new ArrayList<Action>(); 
					renameActions.add(action); 
					actionCountMap.put(actionAsString, renameActions); 
				} 
			} 
		} 
		for(String key : actionCountMap.keySet()) { 
			List<Action> renameActions = actionCountMap.get(key); 
			for(Action action : renameActions) { 
				ITree actionNode = action.getNode(); 
				ITree actionParent = actionNode.getParent(); 
				ITree actionGrandParent = actionParent.getParent(); 
				ITree variableT2 = findMapping(m.getMappingsAsSet(), actionNode); 
				ITree variableT2Parent = variableT2.getParent(); 
				ITree variableT2GrandParent = variableT2Parent.getParent(); 
				RefactoringInfo refactoring = null; 
				CodeRange left = createCodeRange(actionNode, filePath, fileContentsBefore, CodeElementType.TYPE); 
				CodeRange right = createCodeRange(variableT2, filePath, fileContentsCurrent, CodeElementType.TYPE); 
				if(src.getTypeLabel(actionParent).equals("SingleVariableDeclaration") && 
						src.getTypeLabel(actionGrandParent).equals("MethodDeclaration") && 
						dst.getTypeLabel(variableT2GrandParent).equals("MethodDeclaration")) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, variableT2GrandParent); 
					String className = generateClassSignature(dst, variableT2GrandParent.getParent()); 
					left.setDescription("original variable declaration").setCodeElement(v1); 
					right.setDescription("changed-type variable declaration").setCodeElement(v2); 
					String description = "Change Parameter Type " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				else if(src.getTypeLabel(actionParent).equals("FieldDeclaration")) { 
					String t1 = actionNode.getLabel(); 
					String t2 = variableT2.getLabel(); 
					String v1 = null; 
					for(ITree child : actionParent.getChildren()) { 
						if(src.getTypeLabel(child).equals("VariableDeclarationFragment")) { 
							v1 = generateVariableSignature(src, child); 
							break; 
						} 
					} 
					String v2 = null; 
					for(ITree child : variableT2Parent.getChildren()) { 
						if(dst.getTypeLabel(child).equals("VariableDeclarationFragment")) { 
							v2 = generateVariableSignature(dst, child); 
							break; 
						} 
					} 
					String className = generateClassSignature(dst, variableT2Parent.getParent()); 
					if(v1 != null && v2 != null) { 
						left.setDescription("original attribute declaration").setCodeElement(v1); 
						right.setDescription("changed-type attribute declaration").setCodeElement(v2); 
						String description = "Change Attribute Type " + v1 + " to " + v2 + " in class " + className; 
						refactoring = new RefactoringInfo(description, left, right); 
					} 
				} 
				else if(src.getTypeLabel(actionParent).equals("VariableDeclarationStatement") || 
						src.getTypeLabel(actionParent).equals("VariableDeclarationExpression")) { 
					String t1 = actionNode.getLabel(); 
					String t2 = variableT2.getLabel(); 
					String v1 = null; 
					for(ITree child : actionParent.getChildren()) { 
						if(src.getTypeLabel(child).equals("VariableDeclarationFragment")) { 
							v1 = generateVariableSignature(src, child); 
							break; 
						} 
					} 
					String v2 = null; 
					for(ITree child : variableT2Parent.getChildren()) { 
						if(dst.getTypeLabel(child).equals("VariableDeclarationFragment")) { 
							v2 = generateVariableSignature(dst, child); 
							break; 
						} 
					} 
					ITree parentMethodDeclaration = findParentMethodDeclaration(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, parentMethodDeclaration); 
					String className = generateClassSignature(dst, parentMethodDeclaration.getParent()); 
					if(v1 != null && v2 != null) { 
						left.setDescription("original variable declaration").setCodeElement(v1); 
						right.setDescription("changed-type variable declaration").setCodeElement(v2); 
						String description = "Change Variable Type " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
						refactoring = new RefactoringInfo(description, left, right); 
					} 
				} 
				else if(src.getTypeLabel(actionParent).equals("SingleVariableDeclaration") && 
						src.getTypeLabel(actionGrandParent).equals("EnhancedForStatement")) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					ITree parentMethodDeclaration = findParentMethodDeclaration(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, parentMethodDeclaration); 
					String className = generateClassSignature(dst, parentMethodDeclaration.getParent()); 
					left.setDescription("original variable declaration").setCodeElement(v1); 
					right.setDescription("changed-type variable declaration").setCodeElement(v2); 
					String description = "Change Variable Type " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				else if(src.getTypeLabel(actionParent).equals("MethodDeclaration")) { 
					String t1 = actionNode.getLabel(); 
					String t2 = variableT2.getLabel(); 
					ITree parentMethodDeclaration = findParentMethodDeclaration(dst, variableT2); 
					if(parentMethodDeclaration != null) { 
						String signature = generateMethodSignature(dst, parentMethodDeclaration); 
						String className = generateClassSignature(dst, parentMethodDeclaration.getParent()); 
						left.setDescription("original return type").setCodeElement(t1); 
						right.setDescription("changed return type").setCodeElement(t2); 
						String description = "Change Return Type " + t1 + " to " + t2 + " in method " + signature + " in class " + className; 
						if(signature.endsWith(t2)) { 
							refactoring = new RefactoringInfo(description, left, right); 
						} 
					} 
				} 
				if(refactoring != null) { 
					refactorings.add(refactoring); 
				} 
			} 
		} 
		return refactorings; 
	} 
 
	private CodeRange createCodeRange(ITree node, String filePath, String fileContent, CodeElementType type) { 
		int startOffset = node.getPos(); 
		int endOffset = node.getEndPos();		 
		String linesBeforeAndIncludingOffset = fileContent.substring(0, startOffset - 1 ); 
		int startLine = getLines(linesBeforeAndIncludingOffset).length; 
		int startColumn = startOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, startLine - 1 ); 
		 
		linesBeforeAndIncludingOffset = fileContent.substring(0, endOffset - 1 ); 
		int endLine = getLines(linesBeforeAndIncludingOffset).length; 
		int endColumn = endOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, endLine - 1 ); 
		return new CodeRange(filePath, startLine, endLine, startColumn, endColumn, type); 
	} 
 
	private String[] getLines(String string) { 
		if (string.indexOf("\n") >= 0) { 
			return string.split("\n"); 
		} else if (string.indexOf("\r") >= 0) { 
			return string.split("\r"); 
		} 
		return new String[] { string }; 
	} 
 
	private int getNumberOfCharsForLines(String fileContents, int line) { 
		int charsBeforeLine = 0; 
		String[] lines = getLines(fileContents); 
		for (int i = 0; i < line && i < lines.length; i++) { 
			charsBeforeLine += lines[i].length() + 1; // 1 for Line Feed character 
		} 
		// Happens when the last char of the document is not a line feed character 
		if (charsBeforeLine > fileContents.length() - 1) { 
			charsBeforeLine = fileContents.length() - 1; 
		} 
		return charsBeforeLine; 
	} 
 
	private Set<RefactoringInfo> treeDiffForVariableRenames(TreeContext src, TreeContext dst, String filePath, String fileContentsBefore, String fileContentsCurrent) { 
		Set<RefactoringInfo> refactorings = new LinkedHashSet<RefactoringInfo>(); 
		com.github.gumtreediff.matchers.Matcher m = Matchers.getInstance().getMatcher(src.getRoot(), dst.getRoot()); 
		m.match(); 
		ActionGenerator g = new ActionGenerator(src.getRoot(), dst.getRoot(), m.getMappings()); 
		g.generate(); 
		List<Action> actions = g.getActions(); 
		Map<String, List<Action>> actionCountMap = new LinkedHashMap<String, List<Action>>(); 
		for(Action action : actions) { 
			String actionAsString = action.toString(); 
			ITree node = action.getNode(); 
			ITree parent = node.getParent(); 
			if(action.getName().equals("UPD") && 
					node.getType() == ASTNode.SIMPLE_NAME && 
					parent.getType() != ASTNode.SIMPLE_TYPE && 
					!src.getTypeLabel(parent).equals("MethodDeclaration")) { 
				if(actionCountMap.containsKey(actionAsString)) { 
					actionCountMap.get(actionAsString).add(action); 
				} 
				else { 
					List<Action> renameActions = new ArrayList<Action>(); 
					renameActions.add(action); 
					actionCountMap.put(actionAsString, renameActions); 
				} 
			} 
		} 
		for(String key : actionCountMap.keySet()) { 
			List<Action> renameActions = actionCountMap.get(key); 
			for(Action action : renameActions) { 
				ITree actionNode = action.getNode(); 
				ITree actionParent = actionNode.getParent(); 
				ITree actionGrandParent = actionParent.getParent(); 
				ITree variableT2 = findMapping(m.getMappingsAsSet(), actionNode); 
				ITree variableT2Parent = variableT2.getParent(); 
				ITree variableT2GrandParent = variableT2Parent.getParent(); 
				RefactoringInfo refactoring = null; 
				if(src.getTypeLabel(actionParent).equals("SingleVariableDeclaration") && 
						src.getTypeLabel(actionGrandParent).equals("MethodDeclaration") && 
						dst.getTypeLabel(variableT2GrandParent).equals("MethodDeclaration")) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, variableT2GrandParent); 
					String className = generateClassSignature(dst, variableT2GrandParent.getParent()); 
					CodeRange left = createCodeRange(actionNode, filePath, fileContentsBefore, CodeElementType.SINGLE_VARIABLE_DECLARATION); 
					CodeRange right = createCodeRange(variableT2, filePath, fileContentsCurrent, CodeElementType.SINGLE_VARIABLE_DECLARATION); 
					left.setDescription("original variable declaration").setCodeElement(v1); 
					right.setDescription("renamed variable declaration").setCodeElement(v2); 
					String description = "Rename Parameter " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				else if(src.getTypeLabel(actionParent).equals("VariableDeclarationFragment") && 
						src.getTypeLabel(actionGrandParent).equals("FieldDeclaration") && 
						renameActions.size() > 1) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					String className = generateClassSignature(dst, variableT2GrandParent.getParent()); 
					CodeRange left = createCodeRange(actionNode, filePath, fileContentsBefore, CodeElementType.FIELD_DECLARATION); 
					CodeRange right = createCodeRange(variableT2, filePath, fileContentsCurrent, CodeElementType.FIELD_DECLARATION); 
					left.setDescription("original attribute declaration").setCodeElement(v1); 
					right.setDescription("renamed attribute declaration").setCodeElement(v2); 
					String description = "Rename Attribute " + v1 + " to " + v2 + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				else if(src.getTypeLabel(actionParent).equals("VariableDeclarationFragment") && 
						(src.getTypeLabel(actionGrandParent).equals("VariableDeclarationStatement") || 
						src.getTypeLabel(actionGrandParent).equals("VariableDeclarationExpression")) && 
						renameActions.size() > 1) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					ITree parentMethodDeclaration = findParentMethodDeclaration(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, parentMethodDeclaration); 
					String className = generateClassSignature(dst, parentMethodDeclaration.getParent()); 
					CodeElementType type = src.getTypeLabel(actionGrandParent).equals("VariableDeclarationStatement") ? CodeElementType.VARIABLE_DECLARATION_STATEMENT : 
						src.getTypeLabel(actionGrandParent).equals("VariableDeclarationExpression") ? CodeElementType.VARIABLE_DECLARATION_EXPRESSION : null; 
					CodeRange left = createCodeRange(actionNode, filePath, fileContentsBefore, type); 
					CodeRange right = createCodeRange(variableT2, filePath, fileContentsCurrent, type); 
					left.setDescription("original variable declaration").setCodeElement(v1); 
					right.setDescription("renamed variable declaration").setCodeElement(v2); 
					String description = "Rename Variable " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				else if(src.getTypeLabel(actionParent).equals("SingleVariableDeclaration") && 
						src.getTypeLabel(actionGrandParent).equals("EnhancedForStatement") && 
						renameActions.size() > 1) { 
					String v1 = generateVariableSignature(src, actionParent); 
					String v2 = generateVariableSignature(dst, variableT2Parent); 
					ITree parentMethodDeclaration = findParentMethodDeclaration(dst, variableT2Parent); 
					String signature = generateMethodSignature(dst, parentMethodDeclaration); 
					String className = generateClassSignature(dst, parentMethodDeclaration.getParent()); 
					CodeRange left = createCodeRange(actionNode, filePath, fileContentsBefore, CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME); 
					CodeRange right = createCodeRange(variableT2, filePath, fileContentsCurrent, CodeElementType.ENHANCED_FOR_STATEMENT_PARAMETER_NAME); 
					left.setDescription("original variable declaration").setCodeElement(v1); 
					right.setDescription("renamed variable declaration").setCodeElement(v2); 
					String description = "Rename Variable " + v1 + " to " + v2 + " in method " + signature + " in class " + className; 
					refactoring = new RefactoringInfo(description, left, right); 
				} 
				if(refactoring != null) { 
					refactorings.add(refactoring); 
				} 
			} 
		} 
		return refactorings; 
	} 
 
	private ITree findParentMethodDeclaration(TreeContext context, ITree node) { 
		ITree parent = node.getParent(); 
		while(parent != null) { 
			if(context.getTypeLabel(parent).equals("MethodDeclaration")) { 
				return parent; 
			} 
			parent = parent.getParent(); 
		} 
		return null; 
	} 
 
	private ITree findMapping(Set<Mapping> mappings, ITree tree1) { 
		for(Mapping mapping : mappings) { 
			if(mapping.first.equals(tree1)) { 
				return mapping.second; 
			} 
		} 
		return null; 
	} 
 
	private String generateVariableSignature(TreeContext context, ITree variableDeclaration) { 
		StringBuilder sb = new StringBuilder(); 
		if(context.getTypeLabel(variableDeclaration).equals("SingleVariableDeclaration")) { 
			String type = null; 
			String name = null; 
			for(ITree child : variableDeclaration.getChildren()) { 
				if(isTypeNode(context, child)) { 
					type = child.getLabel(); 
				} 
				else if(context.getTypeLabel(child).equals("SimpleName")) { 
					name = child.getLabel(); 
				} 
			} 
			sb.append(name + " : " + type); 
		} 
		else if(context.getTypeLabel(variableDeclaration).equals("VariableDeclarationFragment")) { 
			String name = null; 
			String type = null; 
			for(ITree child : variableDeclaration.getChildren()) { 
				if(context.getTypeLabel(child).equals("SimpleName")) { 
					name = child.getLabel(); 
					break; 
				} 
			} 
			ITree parent = variableDeclaration.getParent(); 
			for(ITree child : parent.getChildren()) { 
				if(isTypeNode(context, child)) { 
					type = child.getLabel(); 
					break; 
				} 
			} 
			sb.append(name + " : " + type); 
		} 
		return sb.toString(); 
	} 
 
	private String generateClassSignature(TreeContext context, ITree typeDeclaration) { 
		String className = null; 
		ITree parent = typeDeclaration; 
		while(parent != null) { 
			for(ITree child : parent.getChildren()) { 
				if(context.getTypeLabel(child).equals("SimpleName") && !isArgument(context, parent, child)) { 
					if(className == null) { 
						className = child.getLabel(); 
					} 
					else { 
						className = child.getLabel() + "." + className; 
					} 
					break; 
				} 
				else if(context.getTypeLabel(child).equals("PackageDeclaration")) { 
					for(ITree child2 : child.getChildren()) { 
						if(child2.getLabel().length() > 0) { 
							className = child2.getLabel() + "." + className; 
							break; 
						} 
					} 
				} 
			} 
			parent = parent.getParent(); 
		} 
		return className; 
	} 
 
	private boolean isArgument(TreeContext context, ITree parent, ITree child) { 
		if(context.getTypeLabel(parent).equals("MethodInvocation")) { 
			 
		} 
		else if(context.getTypeLabel(parent).equals("SuperMethodInvocation")) { 
			 
		} 
		else if(context.getTypeLabel(parent).equals("ClassInstanceCreation")) { 
			boolean typeFound = false; 
			boolean bodyFound = false; 
			for(ITree child2 : parent.getChildren()) { 
				if(isTypeNode(context, child2)) { 
					typeFound = true; 
				} 
				else if(context.getTypeLabel(child).equals("Block")) { 
					bodyFound = true; 
				} 
				else if(typeFound && !bodyFound && child2.equals(child)) { 
					return true; 
				} 
			} 
		} 
		return false; 
	} 
 
	private String generateMethodSignature(TreeContext context, ITree methodDeclaration) { 
		StringBuilder sb = new StringBuilder(); 
		List<ITree> children = methodDeclaration.getChildren(); 
		String returnType = null; 
		boolean modifierFound = false; 
		boolean bodyFound = false; 
		for(int i = 0; i<children.size(); i++) { 
			ITree child = children.get(i); 
			if(context.getTypeLabel(child).equals("SimpleName")) { 
				sb.append(child.getLabel()).append("("); 
			} 
			else if(isTypeNode(context, child) && returnType == null) { 
				returnType = child.getLabel(); 
			} 
			else if(context.getTypeLabel(child).equals("Modifier")) { 
				modifierFound = true; 
				if(child.getLabel().equals("public") || 
						child.getLabel().equals("private") || 
						child.getLabel().equals("protected") || 
						child.getLabel().equals("abstract")) { 
					sb.append(child.getLabel()).append(" "); 
				} 
			} 
			else if(context.getTypeLabel(child).equals("SingleVariableDeclaration")) { 
				String type = null; 
				String name = null; 
				for(ITree child2 : child.getChildren()) { 
					if(isTypeNode(context, child2)) { 
						type = child2.getLabel(); 
					} 
					else if(context.getTypeLabel(child2).equals("SimpleName")) { 
						name = child2.getLabel(); 
					} 
				} 
				if((name.endsWith("s") || name.endsWith("List")) && i == children.size()-2 && context.getTypeLabel(children.get(i+1)).equals("Block") && 
						!type.endsWith("[]") && !type.contains("List") && !type.contains("Collection") && !type.contains("Iterable") && !type.contains("Set") && !type.contains("Iterator") && !type.contains("Array") && 
						!type.endsWith("s") && !type.toLowerCase().contains(name.toLowerCase()) && 
						!name.endsWith("ss") && !type.equals("boolean") && !type.equals("int")) { 
					//hack for varargs 
					sb.append(name + " " + type + "..."); 
				} 
				else { 
					sb.append(name + " " + type); 
				} 
				if(i<children.size()-1 && context.getTypeLabel(children.get(i+1)).equals("SingleVariableDeclaration")) { 
					sb.append(",").append(" "); 
				} 
			} 
			else if(context.getTypeLabel(child).equals("Block")) { 
				bodyFound = true; 
			} 
		} 
		sb.append(")"); 
		if(returnType != null) { 
			sb.append(" : ").append(returnType); 
		} 
		if(!modifierFound) { 
			if(bodyFound) { 
				return "package " + sb.toString(); 
			} 
			else { 
				return "public " + sb.toString(); 
			} 
		} 
		return sb.toString(); 
	} 
 
	private boolean isTypeNode(TreeContext context, ITree child) { 
		return context.getTypeLabel(child).equals("SimpleType") || 
				context.getTypeLabel(child).equals("PrimitiveType") || 
				context.getTypeLabel(child).equals("QualifiedType") || 
				context.getTypeLabel(child).equals("WildcardType") || 
				context.getTypeLabel(child).equals("ArrayType") || 
				context.getTypeLabel(child).equals("ParameterizedType") || 
				context.getTypeLabel(child).equals("NameQualifiedType"); 
	} 
 
	public class RefactoringInfo {
		private String description; 
		private CodeRange left; 
		private CodeRange right; 
		public RefactoringInfo(String description, CodeRange left, CodeRange right) { 
			super(); 
			this.description = description; 
			this.left = left; 
			this.right = right; 
		} 
		@Override 
		public int hashCode() { 
			final int prime = 31; 
			int result = 1; 
			result = prime * result + ((description == null) ? 0 : description.hashCode()); 
			return result; 
		} 
		@Override 
		public boolean equals(Object obj) { 
			if (this == obj) 
				return true; 
			if (obj == null) 
				return false; 
			if (getClass() != obj.getClass()) 
				return false; 
			RefactoringInfo other = (RefactoringInfo) obj; 
			if (description == null) { 
				if (other.description != null) 
					return false; 
			} else if (!description.equals(other.description)) 
				return false; 
			return true; 
		}

		public String getDescription() {
			return description;
		}
	}


	private String generateMoveMethod(ChangeReturnTypeRefactoring r, String refactoringName) { 
		StringBuilder sb = new StringBuilder(); 
		sb.append(refactoringName).append(" "); 
		sb.append(r.getOperationBefore()); 
		sb.append(" from class "); 
		sb.append(r.getOperationBefore().getClassName()); 
		sb.append(" to "); 
		sb.append(r.getOperationAfter()); 
		sb.append(" from class "); 
		sb.append(r.getOperationAfter().getClassName()); 
		return sb.toString(); 
	} 
 
	private String generateRenameMethod(ChangeReturnTypeRefactoring r) { 
		StringBuilder sb = new StringBuilder(); 
		sb.append("Rename Method").append(" "); 
		sb.append(r.getOperationBefore()); 
		sb.append(" renamed to "); 
		sb.append(r.getOperationAfter()); 
		sb.append(" in class ").append(getClassName(r.getOperationBefore(), r.getOperationAfter())); 
		return sb.toString(); 
	} 
 
	private String getClassName(UMLOperation originalOperation, UMLOperation renamedOperation) { 
		String sourceClassName = originalOperation.getClassName(); 
		String targetClassName = renamedOperation.getClassName(); 
		boolean targetIsAnonymousInsideSource = false; 
		if(targetClassName.startsWith(sourceClassName + ".")) { 
			String targetClassNameSuffix = targetClassName.substring(sourceClassName.length() + 1, targetClassName.length()); 
			targetIsAnonymousInsideSource = isNumeric(targetClassNameSuffix); 
		} 
		return sourceClassName.equals(targetClassName) || targetIsAnonymousInsideSource ? sourceClassName : targetClassName; 
	} 
 
	private static boolean isNumeric(String str) { 
		for(char c : str.toCharArray()) { 
			if(!Character.isDigit(c)) return false; 
		} 
		return true; 
	} 
 
} 

