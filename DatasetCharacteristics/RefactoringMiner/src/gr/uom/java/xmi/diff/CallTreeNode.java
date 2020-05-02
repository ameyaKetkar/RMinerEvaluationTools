package gr.uom.java.xmi.diff;

import java.util.ArrayList;
import java.util.List;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.OperationInvocation;

public class CallTreeNode {
	private UMLOperation originalOperation;
	private UMLOperation invokedOperation;
	private OperationInvocation invocation;
	private List<CallTreeNode> children = new ArrayList<CallTreeNode>();
	
	public CallTreeNode(UMLOperation originalOperation, UMLOperation invokedOperation,
			OperationInvocation invocation) {
		this.originalOperation = originalOperation;
		this.invokedOperation = invokedOperation;
		this.invocation = invocation;
	}

	public UMLOperation getOriginalOperation() {
		return originalOperation;
	}

	public UMLOperation getInvokedOperation() {
		return invokedOperation;
	}

	public OperationInvocation getInvocation() {
		return invocation;
	}

	public void addChild(CallTreeNode node) {
		children.add(node);
	}

	public List<CallTreeNode> getChildren() {
		return children;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((invocation == null) ? 0 : invocation.hashCode());
		result = prime * result + ((invokedOperation == null) ? 0 : invokedOperation.hashCode());
		result = prime * result + ((originalOperation == null) ? 0 : originalOperation.hashCode());
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
		CallTreeNode other = (CallTreeNode) obj;
		if (invocation == null) {
			if (other.invocation != null)
				return false;
		} else if (!invocation.equals(other.invocation))
			return false;
		if (invokedOperation == null) {
			if (other.invokedOperation != null)
				return false;
		} else if (!invokedOperation.equals(other.invokedOperation))
			return false;
		if (originalOperation == null) {
			if (other.originalOperation != null)
				return false;
		} else if (!originalOperation.equals(other.originalOperation))
			return false;
		return true;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(invokedOperation);
		sb.append(" called from ");
		sb.append(originalOperation);
		return sb.toString();
	}
}
