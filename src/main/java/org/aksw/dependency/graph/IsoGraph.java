package org.aksw.dependency.graph;

import java.io.Serializable;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import simpack.api.IGraphAccessor;
import simpack.api.IGraphNode;
import simpack.api.IGraphNodeComparator;
import simpack.api.impl.AbstractGraphAccessor;

public class IsoGraph implements IGraphAccessor {

	public IsoGraph() {
		super();
	}

	private static Logger logger = Logger.getLogger(AbstractGraphAccessor.class);

	/**
	 * This will contain the complete node set of the graph
	 */
	protected TreeSet<IGraphNode> nodeSet = new TreeSet<IGraphNode>(new NodeIdComparator());

	/**
	 * The first root (a node without incoming but only outgoing edges) found in
	 * the graph.
	 */
	protected IGraphNode root;

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#size()
	 */
	public int size() {
		return nodeSet.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#getNodeSet()
	 */
	public TreeSet<IGraphNode> getNodeSet() {
		return nodeSet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#getNode(java.lang.String)
	 */
	public IGraphNode getNode(String label) {
		for (IGraphNode node : nodeSet) {
			if (node.getLabel().equals(label)) {
				return node;
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#addNode(simpack.api.IGraphNode)
	 */
	public void addNode(IGraphNode node) {
		nodeSet.add(node);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#contains(simpack.api.IGraphNode)
	 */
	public boolean contains(IGraphNode node) {
		for (IGraphNode n : nodeSet) {
			if (n.equals(node)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#getRoot()
	 */
	public IGraphNode getRoot() {
		if (root == null) {
			for (IGraphNode node : nodeSet) {
				if (node.getPredecessorSet().size() == 0)
					root = node;
			}
		}
		return root;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see simpack.api.IGraphAccessor#setEdge(simpack.api.IGraphNode,
	 * simpack.api.IGraphNode)
	 */
	public void setEdge(IGraphNode tmpSourceNode, IGraphNode tmpTargetNode) {
		IGraphNode sourceNode = null;
		logger.debug("Want to set an edge for " + tmpSourceNode.getLabel() + " -> " + tmpTargetNode.getLabel());
		if (contains(tmpSourceNode)) {
			sourceNode = getNode(tmpSourceNode.getLabel());
			logger.debug("Node contained " + sourceNode.toString());
		} else {
			sourceNode = tmpSourceNode;
			logger.debug("Adding node " + sourceNode.toString());
			addNode(sourceNode);
		}
		IGraphNode targetNode = null;
		if (contains(tmpTargetNode)) {
			targetNode = getNode(tmpTargetNode.getLabel());
			logger.debug("Node contained " + targetNode.toString());
		} else {
			targetNode = tmpTargetNode;
			logger.debug("Adding node " + targetNode.toString());
			addNode(targetNode);
		}
		logger.debug("Adding " + sourceNode.toString() + " -> " + targetNode.toString());
		sourceNode.addSuccessor(targetNode);
		targetNode.addPredecessor(sourceNode);
	}

	public double getShortestPath(IGraphNode nodeA, IGraphNode nodeB) {
		return 0d;
	}

	public double getMaximumDirectedPathLength() {
		return 0d;
	}

	public Set<IGraphNode> getPredecessors(IGraphNode node, boolean direct) {
		return null;
	}

	public Set<IGraphNode> getSuccessors(IGraphNode node, boolean direct) {
		return null;
	}

	public IGraphNode getMostRecentCommonAncestor(IGraphNode nodeA, IGraphNode nodeB) {
		return null;
	}

	public double getMaxDepth() {
		return 0d;
	}

	private class NodeIdComparator implements IGraphNodeComparator<IGraphNode>, Serializable {

		private static final long serialVersionUID = 4997348735184341449L;

		/**
		 * Compares two IGraphNodes. Throws an exception if the nodes are
		 * <code>null</code>.
		 * 
		 * @param node1
		 *            first graph node
		 * @param node2
		 *            second graph node
		 * @return a negative integer, zero, or a positive integer as the first
		 *         argument is less than, equal to, or greater than the second.
		 */
		public int compare(IGraphNode node1, IGraphNode node2) {
			if (node1 == null || node2 == null) {
				throw new NullPointerException();
			} else {
				Node o1 = (Node) node1.getUserObject();
				Node o2 = (Node) node2.getUserObject();
				return o1.getId().compareTo(o2.getId());
			}
		}
	}

}
