package gr.uom.java.xmi.diff;

import gr.uom.java.xmi.UMLClass;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

public class RenameClassRefactoring implements Refactoring {
	private String originalClassName;
	private String renamedClassName;
	private String beforePackage;
	private String afterPackage;
	
	public RenameClassRefactoring(UMLClass originalClassName, UMLClass renamedClassName) {
		this.originalClassName = originalClassName.getName();
		this.beforePackage = originalClassName.getPackageName();
		this.renamedClassName = renamedClassName.getName();
		this.afterPackage = renamedClassName.getPackageName();
	}

	public String toString() {
		if(beforePackage.equals(afterPackage)) {
			StringBuilder sb = new StringBuilder();
			sb.append(getName()).append("\t");
			sb.append(originalClassName);
			sb.append(" renamed to ");
			sb.append(renamedClassName);
			return sb.toString();
		}else{
			StringBuilder sb = new StringBuilder();
			sb.append("Move And Rename Class").append("\t");
			sb.append(originalClassName);
			sb.append(" moved and renamed to ");
			sb.append(renamedClassName);
			return sb.toString();
		}
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.RENAME_CLASS;
	}

	public String getOriginalClassName() {
		return originalClassName;
	}

	public String getRenamedClassName() {
		return renamedClassName;
	}
	
}
