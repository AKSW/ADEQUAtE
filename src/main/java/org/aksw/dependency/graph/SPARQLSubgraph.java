package org.aksw.dependency.graph;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;

public class SPARQLSubgraph extends ColoredDirectedSubgraph{
	
	public SPARQLSubgraph(DirectedGraph<Node, ColoredEdge> base, Set<Node> vertexSubset,
			Set<ColoredEdge> edgeSubset) {
		super(base, vertexSubset, edgeSubset);
	}
	
	public ColoredDirectedGraph toGeneralizedGraph(){
		SPARQLGraph graph = new SPARQLGraph();
		
		int varCnt = 0;
		int clsCnt = 0;
		int propCnt = 0;
		int resCnt = 0;
		for(ColoredEdge edge : edgeSet()){
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);
			
			source = source.asGeneralizedNode();
//			if(source instanceof ClassNode){
//				source = new ClassNode(source.getLabel() + "_" + clsCnt++);
//			} else if(source instanceof PropertyNode){
//				source = new PropertyNode(source.getLabel() + "_" + propCnt++);
//			} else if(source instanceof ResourceNode){
//				source = new ResourceNode(source.getLabel() + "_" + resCnt++);
//			} else if(source instanceof VariableNode){
//				source = new VariableNode(source.getLabel() + "_" + varCnt++);
//			}
			target = target.asGeneralizedNode();
//			if(target instanceof ClassNode){
//				target = new ClassNode(target.getLabel() + "_" + clsCnt++);
//			} else if(target instanceof PropertyNode){
//				target = new PropertyNode(target.getLabel() + "_" + propCnt++);
//			} else if(target instanceof ResourceNode){
//				target = new ResourceNode(target.getLabel() + "_" + resCnt++);
//			} else if(target instanceof VariableNode){
//				target = new VariableNode(target.getLabel() + "_" + varCnt++);
//			}
			graph.addVertex(source);
			graph.addVertex(target);
			graph.addEdge(source, target, new ColoredEdge(edge.getLabel(), edge.getColor()));
		}
		
		return graph;
	};

}
