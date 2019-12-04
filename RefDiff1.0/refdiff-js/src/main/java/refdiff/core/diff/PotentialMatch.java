package refdiff.core.diff;

import refdiff.core.rast.RastNode;

public class PotentialMatch implements Comparable<PotentialMatch> {
	
	private final RastNode nodeBefore;
	private final RastNode nodeAfter;
	private final int maxDepth;
	private final double score;
	
	public PotentialMatch(RastNode nodeBefore, RastNode nodeAfter, int maxDepth, double score) {
		this.nodeBefore = nodeBefore;
		this.nodeAfter = nodeAfter;
		this.maxDepth = maxDepth;
		this.score = score;
	}
	
	public RastNode getNodeBefore() {
		return nodeBefore;
	}
	
	public RastNode getNodeAfter() {
		return nodeAfter;
	}
	
	public double getScore() {
		return score;
	}
	
	public double getMaxDepth() {
		return score;
	}
	
	@Override
	public String toString() {
		return String.format("(%s, %s)", this.nodeBefore, this.nodeAfter);
	}
	
	@Override
	public int compareTo(PotentialMatch o) {
		int c1 = Integer.compare(maxDepth, o.maxDepth);
		int c2 = -Double.compare(score, o.score);
		return c1 != 0 ? c1 : c2;
	}
}
