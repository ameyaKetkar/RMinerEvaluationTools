package refdiff.core.cst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class CstRoot implements HasChildrenNodes {
	
	private List<CstNode> nodes = new ArrayList<>();
	
	private Set<CstNodeRelationship> relationships = new HashSet<>();
	
	private Map<String, TokenizedSource> tokenizedSource = new HashMap<>();
	
	public List<CstNode> getNodes() {
		return nodes;
	}
	
	@Override
	public void addNode(CstNode node) {
		nodes.add(node);
	}
	
	public void addTokenizedFile(TokenizedSource tokenizedSource) {
		this.tokenizedSource.put(tokenizedSource.getFile(), tokenizedSource);
	}
	
	public Set<CstNodeRelationship> getRelationships() {
		return relationships;
	}
	
	public void forEachNode(BiConsumer<CstNode, Integer> consumer) {
		forEachNodeInList(nodes, consumer, 0);
	}
	
	private void forEachNodeInList(List<CstNode> list, BiConsumer<CstNode, Integer> consumer, int depth) {
		for (CstNode node : list) {
			consumer.accept(node, depth);
			forEachNodeInList(node.getNodes(), consumer, depth + 1);
		}
	}

	public Map<String, TokenizedSource> getTokenizedSource() {
		return tokenizedSource;
	}

}
