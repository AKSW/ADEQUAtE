package org.aksw.dependency.graph;

import java.util.HashMap;
import java.util.Map;

public class SPARQLGraph extends ColoredDirectedGraph{

	public ColoredDirectedGraph toGeneralizedGraph(){
		SPARQLGraph graph = new SPARQLGraph();
		
		Map<Node, Node> node2Newnode = new HashMap<Node, Node>(); 
		
		int varCnt = 0;
		int clsCnt = 0;
		int propCnt = 0;
		int resCnt = 0;
		for(ColoredEdge edge : edgeSet()){
			
			Node source = getEdgeSource(edge);
			Node target = getEdgeTarget(edge);

			Node newSource = node2Newnode.get(source);
			if(newSource == null){
				newSource = source.asGeneralizedNode();
				if(newSource instanceof ClassNode){
					newSource = new ClassNode(newSource.getLabel() + "_" + clsCnt++);
				} else if(newSource instanceof PropertyNode){
					newSource = new PropertyNode(newSource.getLabel() + "_" + propCnt++);
				} else if(newSource instanceof ResourceNode){
					newSource = new ResourceNode(newSource.getLabel() + "_" + resCnt++);
				} else if(newSource instanceof VariableNode){
					newSource = new VariableNode(newSource.getLabel() + "_" + varCnt++);
				}
				node2Newnode.put(source, newSource);
			}
			
			Node newTarget = node2Newnode.get(target);
			if(newTarget == null){
				newTarget = target.asGeneralizedNode();
				if(newTarget instanceof ClassNode){
					newTarget = new ClassNode(newTarget.getLabel() + "_" + clsCnt++);
				} else if(newTarget instanceof PropertyNode){
					newTarget = new PropertyNode(newTarget.getLabel() + "_" + propCnt++);
				} else if(target instanceof ResourceNode){
					newTarget = new ResourceNode(newTarget.getLabel() + "_" + resCnt++);
				} else if(target instanceof VariableNode){
					newTarget = new VariableNode(newTarget.getLabel() + "_" + varCnt++);
				}
				node2Newnode.put(target, newTarget);
			}
			
			graph.addVertex(newSource);
			graph.addVertex(newTarget);
			graph.addEdge(newSource, newTarget, new ColoredEdge(edge.getLabel(), edge.getColor()));
		}
		
		return graph;
	};
}
