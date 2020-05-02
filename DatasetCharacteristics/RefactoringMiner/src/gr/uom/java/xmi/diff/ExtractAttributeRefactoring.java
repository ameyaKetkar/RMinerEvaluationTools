package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;

public class ExtractAttributeRefactoring implements Refactoring {
	private UMLAttribute attributeDeclaration;
	private UMLClass originalClass;
	private UMLClass nextClass;
	private Set<AbstractCodeMapping> references;

	public ExtractAttributeRefactoring(UMLAttribute variableDeclaration, UMLClass originalClass, UMLClass nextClass) {
		this.attributeDeclaration = variableDeclaration;
		this.originalClass = originalClass;
		this.nextClass = nextClass;
		this.references = new LinkedHashSet<AbstractCodeMapping>();
	}

	public void addReference(AbstractCodeMapping mapping) {
		references.add(mapping);
	}

	public RefactoringType getRefactoringType() {
		return RefactoringType.EXTRACT_ATTRIBUTE;
	}

	public String getName() {
		return this.getRefactoringType().getDisplayName();
	}

	public UMLAttribute getVariableDeclaration() {
		return attributeDeclaration;
	}

	public Set<AbstractCodeMapping> getReferences() {
		return references;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName()).append("\t");
		sb.append(attributeDeclaration);
		sb.append(" in class ");
		sb.append(attributeDeclaration.getClassName());
		return sb.toString();
	}

	/**
	 * @return the code range of the extracted variable declaration in the <b>child</b> commit
	 */
	public CodeRange getExtractedVariableDeclarationCodeRange() {
		return attributeDeclaration.codeRange();
	}

	public UMLClass getOriginalClass() {
		return originalClass;
	}

	public UMLClass getNextClass() {
		return nextClass;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributeDeclaration == null) ? 0 : attributeDeclaration.hashCode());
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
		ExtractAttributeRefactoring other = (ExtractAttributeRefactoring) obj;
		if (attributeDeclaration == null) {
			if (other.attributeDeclaration != null)
				return false;
		} else if (!attributeDeclaration.equals(other.attributeDeclaration))
			return false;
		return true;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesBeforeRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getOriginalClass().getLocationInfo().getFilePath(), getOriginalClass().getName()));
		return pairs;
	}

	public Set<ImmutablePair<String, String>> getInvolvedClassesAfterRefactoring() {
		Set<ImmutablePair<String, String>> pairs = new LinkedHashSet<ImmutablePair<String, String>>();
		pairs.add(new ImmutablePair<String, String>(getNextClass().getLocationInfo().getFilePath(), getNextClass().getName()));
		return pairs;
	}

	@Override
	public List<CodeRange> leftSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment1().codeRange().setDescription("statement with the initializer of the extracted attribute"));
		}
		return ranges;
	}

	@Override
	public List<CodeRange> rightSide() {
		List<CodeRange> ranges = new ArrayList<CodeRange>();
		ranges.add(attributeDeclaration.codeRange()
				.setDescription("extracted attribute declaration")
				.setCodeElement(attributeDeclaration.toString()));
		for(AbstractCodeMapping mapping : references) {
			ranges.add(mapping.getFragment2().codeRange().setDescription("statement with the name of the extracted attribute"));
		}
		return ranges;
	}
}
