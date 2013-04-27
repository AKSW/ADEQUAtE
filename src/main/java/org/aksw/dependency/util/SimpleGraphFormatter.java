package org.aksw.dependency.util;

import java.util.HashSet;
import java.util.Set;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.Node;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.trees.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.StringUtils;

public class SimpleGraphFormatter {
	
	private static final String LPAREN = "[";
	private static final String RPAREN = "]";
	private static final String SPACE = " ";
	private static final String COLON = ":";

	private static final int DEFAULT_WIDTH = 80;
	private static final int DEFAULT_INDENT = 4;
	private static final boolean DEFAULT_SMART_INDENT = true;
	private static final boolean DEFAULT_SHOW_RELNS = false;
	private static final boolean DEFAULT_SHOW_TAGS = true;
	private static final boolean DEFAULT_SHOW_ANNOS = false;
	private static final boolean DEFAULT_SHOW_INDICES = true;

	private int width = DEFAULT_WIDTH;
	private int indent = DEFAULT_INDENT;
	private boolean smartIndent = DEFAULT_SMART_INDENT;
	private boolean showRelns = DEFAULT_SHOW_RELNS;
	private boolean showTags = DEFAULT_SHOW_TAGS;
	private boolean showAnnos = DEFAULT_SHOW_ANNOS;
	private boolean showIndices = DEFAULT_SHOW_INDICES;

	private StringBuilder out;
	private Set<Node> used;

	public SimpleGraphFormatter() {
		this(DEFAULT_WIDTH, DEFAULT_INDENT, DEFAULT_SMART_INDENT, DEFAULT_SHOW_RELNS, DEFAULT_SHOW_TAGS,
				DEFAULT_SHOW_ANNOS, DEFAULT_SHOW_INDICES);
	}

	public SimpleGraphFormatter(int width, int indent, boolean smartIndent, boolean showRelns, boolean showTags,
			boolean showAnnos, boolean showIndices) {
		this.width = width;
		this.indent = indent;
		this.smartIndent = smartIndent;
		this.showRelns = showRelns;
		this.showTags = showTags;
		this.showAnnos = showAnnos;
		this.showIndices = showIndices;
	}

	public String formatGraph(ColoredDirectedGraph g) {
		if (g.vertexSet().isEmpty()) {
			return "[]";
		}
		out = new StringBuilder(); // not thread-safe!!!
		used = new HashSet<Node>();
		Set<Node> roots = g.getRoots();
		if (roots.size() == 1) {
			formatSGNode(g, roots.iterator().next(), 1);
		} else {
			int index = 0;
			for (Node root : roots) {
				index += 1;
				out.append("root_" + index + ": ");
				formatSGNode(g, root, 9);
				out.append("\n");
			}
		}
		String result = out.toString();
		if (!result.startsWith("[")) {
			result = "[" + result + "]";
		}
		return result;
	}

	private void formatSGNode(ColoredDirectedGraph g, Node node, int spaces) {
		used.add(node);
		String oneline = formatSGNodeOneline(g, node);
		boolean toolong = (spaces + oneline.length() > width);
		boolean breakable = g.outDegreeOf(node) >= 1;
		if (toolong && breakable) {
			formatSGNodeMultiline(g, node, spaces);
		} else {
			out.append(oneline);
		}
	}

	private String formatSGNodeOneline(ColoredDirectedGraph sg, Node node) {
		StringBuilder sb = new StringBuilder();
		Set<Node> usedOneline = new HashSet<Node>();
		formatSGNodeOnelineHelper(sg, node, sb, usedOneline);
		return sb.toString();
	}

	private void formatSGNodeOnelineHelper(ColoredDirectedGraph g, Node node, StringBuilder sb,
			Set<Node> usedOneline) {
		usedOneline.add(node);
		boolean isntLeaf = (g.outDegreeOf(node) > 0);
		if (isntLeaf) {
			sb.append(LPAREN);
		}
		sb.append(formatLabel(node));
		for (ColoredEdge depcy : g.outgoingEdgesOf(node)) {
			Node dep = g.getEdgeTarget(depcy);
			sb.append(SPACE);
			if (showRelns) {
				sb.append(depcy.getLabel());
				sb.append(COLON);
			}
			if (!usedOneline.contains(dep) && !used.contains(dep)) { // avoid
																		// infinite
																		// loop
				formatSGNodeOnelineHelper(g, dep, sb, usedOneline);
			} else {
				sb.append(formatLabel(dep));
			}
		}
		if (isntLeaf) {
			sb.append(RPAREN);
		}
	}

	private void formatSGNodeMultiline(ColoredDirectedGraph g, Node node, int spaces) {
		out.append(LPAREN);
		out.append(formatLabel(node));
		if (smartIndent) {
			spaces += 1;
		} else {
			spaces += indent;
		}
		for (ColoredEdge depcy : g.outgoingEdgesOf(node)) {
			Node dep = g.getEdgeTarget(depcy);
			out.append("\n");
			out.append(StringUtils.repeat(SPACE, spaces));
			int sp = spaces;
			if (showRelns) {
				String reln = depcy.getLabel();
				out.append(reln);
				out.append(COLON);
				if (smartIndent) {
					sp += (reln.length() + 1);
				}
			}
			if (!used.contains(dep)) { // avoid infinite loop
				formatSGNode(g, dep, sp);
			}
		}
		out.append(RPAREN);
	}

	private String formatLabel(Node node) {
		String s = node.getLabel();
		if(showIndices){
			s += "(" + node.getId() + ")";
		}
		return s;
	}

}
