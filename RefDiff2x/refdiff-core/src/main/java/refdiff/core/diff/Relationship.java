package refdiff.core.diff;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import refdiff.core.cst.CstNode;

import static java.util.stream.Collectors.*;

public class Relationship {
	
	private final RelationshipType type;
	private final CstNode nodeBefore;
	private final CstNode nodeAfter;
	private final Double similarity;
	
	public Relationship(RelationshipType type, CstNode nodeBefore, CstNode nodeAfter) {
		this(type, nodeBefore, nodeAfter, null);
	}
	
	public Relationship(RelationshipType type, CstNode nodeBefore, CstNode nodeAfter, Double similarity) {
		this.type = type;
		this.nodeBefore = nodeBefore;
		this.nodeAfter = nodeAfter;
		this.similarity = similarity;
	}
	
	public RelationshipType getType() {
		return type;
	}
	
	public CstNode getNodeBefore() {
		return nodeBefore;
	}
	
	public CstNode getNodeAfter() {
		return nodeAfter;
	}
	
	public Double getSimilarity() {
		return similarity;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Relationship) {
			Relationship otherRelationship = (Relationship) obj;
			return Objects.equals(this.type, otherRelationship.type) &&
				Objects.equals(this.nodeBefore, otherRelationship.nodeBefore) &&
				Objects.equals(this.nodeAfter, otherRelationship.nodeAfter);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.type, this.nodeBefore, this.nodeAfter);
	}
	
	@Override
	public String toString() {
		return String.format("%s({%s}, {%s})", this.type, format(this.nodeBefore), format(this.nodeAfter));
	}
	
	public String getStandardDescription() {
		return String.format("%s\t{%s}\t{%s})", this.type, formatWithLineNum(this.nodeBefore), formatWithLineNum(this.nodeAfter));
	}

	public static List<String> getOracleStyleDescriptionsFor(Set<Relationship> relationships){
		return relationships.stream()
				.map(x-> getOracleStyleDescription(x, relationships))
				.filter(x->!x.isEmpty())
				.collect(toList());
	}

	private static boolean containsMoveAndOrRenameNonAbstractMethodLike(Set<Relationship> relationships, String b4, String aft){
		return relationships.stream()
				.filter(x -> x.type.equals(RelationshipType.MOVE_RENAME) || x.type.equals(RelationshipType.RENAME) || x.type.equals(RelationshipType.INTERNAL_MOVE_RENAME))
				.filter(x -> x.nodeBefore.getType().contains("Method") && x.nodeAfter.getType().contains("Method"))
				.filter(x -> x.nodeBefore.getLocalName().equals(b4) && x.nodeAfter.getLocalName().equals(aft))
				.peek(x -> System.out.println("----- " + b4 + " ---- " + aft + "----"))
				.anyMatch(x -> !x.nodeBefore.isAbstractMethod());
	}

	public static String getOracleStyleDescription(Relationship rel, Set<Relationship> relationships) {
		String nameAfter = rel.nodeAfter.getOracleStyleName();
		String nameBefore = rel.nodeBefore.getOracleStyleName();
		String containerNameBefore = rel.nodeBefore.getParent().map(x -> x.getOracleStyleName()).orElse("");
		String containerNameAfter = rel.nodeAfter.getParent().map(x -> x.getOracleStyleName()).orElse("");
		String typeBefore = rel.nodeBefore.getType();
		String typeAfter = rel.nodeAfter.getType();
		switch (rel.type) {
			case EXTRACT:
				if (typeBefore.contains("Method"))
					return "Extract Method " + nameAfter + " extracted from " +
							nameBefore + " in class " + containerNameAfter;
				break;
			case EXTRACT_MOVE:
				if (typeBefore.contains("Method"))
					return "Extract And Move Method " + nameAfter + " extracted from " +
                            nameBefore + " in class " + containerNameBefore + " & moved to class " +
							containerNameAfter;
				break;
//			case PULL_UP_SIGNATURE:
//				if(typeBefore.contains("Method"))
//					return "Pull Up Method Signature " + nameBefore + " from class " + containerNameBefore +
//							" to " + nameAfter + " from class " + containerNameAfter;
//				break;
            case PULL_UP:
                if(typeBefore.contains("Method"))
					return "Pull Up Method " + nameBefore + " from class " + containerNameBefore +
							" to " + nameAfter + " from class " + containerNameAfter;
				break;
			case EXTRACT_SUPER:
				if(typeAfter.contains("Interface"))
					return "Extract Interface " + nameAfter + " from class " + nameBefore;
				else if(typeAfter.contains("Class"))
				    return "Extract Superclass " + nameAfter + " from class " + nameBefore;
				break;
            case INTERNAL_MOVE:
            case MOVE:
                if(typeBefore.contains("Class") || typeBefore.contains("Interface") || typeBefore.contains("Enum"))
                    return "Move Class " + nameBefore + " moved to " + nameAfter;
                else if(typeBefore.contains("Method"))
                    return "Move Method " + nameBefore + " from class " + containerNameBefore +
                            " to " + nameAfter + " from class " + containerNameAfter;
                break;
            case RENAME:
                if(typeBefore.contains("Method"))
                    return "Rename Method " + nameBefore + " renamed to " + nameAfter +
                            " in class " + containerNameAfter;
                else if(typeBefore.contains("Class") || typeBefore.contains("Interface") || typeBefore.contains("Enum")) {
                	if((rel.nodeBefore.getNamespace() == null && rel.nodeAfter.getNamespace() == null) || rel.nodeBefore.getNamespace().equals(rel.nodeAfter.getNamespace()))
						return "Rename Class " + nameBefore + " renamed to " + nameAfter;
                	else
                		return "Move And Rename Class " + nameBefore + " moved and renamed to " + nameAfter;
				}
                break;
            case INTERNAL_MOVE_RENAME:
            case MOVE_RENAME:
                if(typeBefore.contains("Class") || typeBefore.contains("Enum"))
                    return "Move And Rename Class " + nameBefore + " moved and renamed to " + nameAfter;
                else if(typeBefore.contains("Method")) {
                	if(rel.nodeBefore.isAbstractMethod()){
                		if(!containsMoveAndOrRenameNonAbstractMethodLike(relationships, rel.getNodeBefore().getLocalName(), rel.getNodeAfter().getLocalName())){
                			return "";
						}
                		else{
							System.out.println();
						}
					}
					return "Move And Rename Method " + nameBefore + " from class " + containerNameBefore + " to "
							+ nameAfter + " from class " + containerNameAfter;
				}
                break;
            case INLINE:
                if(typeBefore.contains("Method")){
                    return "Inline Method " + nameBefore + " inlined to " +
                            nameAfter + " in class " + containerNameAfter;
            }break;
            case PUSH_DOWN:
            case PUSH_DOWN_IMPL:
                if(typeBefore.contains("Method")){
                    return "Push Down Method " + nameBefore + " from class "
                            + containerNameBefore + " to " + nameAfter + " from class "
                            + containerNameAfter;
                }break;
		}
		return "";
	}

    private String formatWithLineNum(CstNode node) {
		return String.format("%s %s at %s:%d", node.getType().replace("Declaration", ""),
				node.getLocalName(), node.getLocation().getFile(),
				node.getLocation().getLine());
	}
	
	private String format(CstNode node) {
		return String.join(" ", CstRootHelper.getNodePath(node));
	}
	
	public boolean isRefactoring() {
		return !type.equals(RelationshipType.SAME);
	}
}
