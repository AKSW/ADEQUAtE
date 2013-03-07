package org.aksw.dependency.util;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.aksw.dependency.graph.ColoredDirectedGraph;
import org.aksw.dependency.graph.ColoredDirectedSubgraph;
import org.aksw.dependency.graph.ColoredEdge;
import org.aksw.dependency.graph.DependencySubgraph;
import org.aksw.dependency.graph.IsoGraph;
import org.aksw.dependency.graph.Node;
import org.aksw.dependency.graph.SPARQLGraph;
import org.aksw.dependency.graph.SPARQLSubgraph;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DirectedSubgraph;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import simpack.api.IGraphAccessor;
import simpack.measure.graph.GraphIsomorphism;
import simpack.measure.graph.SubgraphIsomorphism;

import com.jgraph.layout.JGraphFacade;
import com.jgraph.layout.tree.JGraphTreeLayout;

public class GraphUtils {
	
	private static final Font nodeFont = GraphConstants.DEFAULTFONT.deriveFont(Font.PLAIN, 17);
	
	public static Collection<ColoredDirectedSubgraph> getSubgraphs(ColoredDirectedSubgraph graph, Node root){
		Set<ColoredDirectedSubgraph> subgraphs = new HashSet<ColoredDirectedSubgraph>();
		postorderTraversal(graph, root, null, subgraphs);
		return subgraphs;
	}
	
	public static Collection<ColoredDirectedSubgraph> getSubgraphs(ColoredDirectedGraph graph){
		Set<ColoredDirectedSubgraph> subgraphs = new HashSet<ColoredDirectedSubgraph>();
		Node root = findRoot(graph);
		postorderTraversal(graph, root, null, subgraphs);
		return subgraphs;
	}
	
	public static <V, E> Collection<DirectedSubgraph<V, E>> getSubgraphs(DirectedGraph<V, E> graph){
		Set<DirectedSubgraph<V, E>> subgraphs = new HashSet<DirectedSubgraph<V, E>>();
		V root = findRoot(graph);
//		postorderTraversal(graph, root, null, subgraphs);
		return subgraphs;
	}
	
	private static <V, E> V findRoot(DirectedGraph<V, E> graph){
		for(V vertex : graph.vertexSet()){
			if(graph.inDegreeOf(vertex) == 0 && graph.outDegreeOf(vertex) > 0){
				return vertex;
			}
		}
		//Should never happen
		return null;
	}
	
	
	private static Node findRoot(ColoredDirectedGraph graph){
		for(Node vertex : graph.vertexSet()){
			if(graph.inDegreeOf(vertex) == 0 && graph.outDegreeOf(vertex) > 0){
				return vertex;
			}
		}
		//Should never happen
		return null;
	}
	
//	private static ColoredDirectedSubgraph postorderTraversal(DirectedGraph<Node, ColoredEdge> graph, Node source, ColoredEdge sedge, Collection<ColoredDirectedSubgraph> allSubgraphs){
//		  Set<ColoredEdge> outgoingEdges = graph.outgoingEdgesOf(source);
//		  if(outgoingEdges.size() > 1){
//			  Map<ColoredEdge, ColoredDirectedSubgraph> map = new HashMap<ColoredEdge, ColoredDirectedSubgraph>();
//			  ColoredDirectedSubgraph sub;
//			  Set<Node> vertexSubset = new HashSet<Node>();
//			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
//			  
//			  //node
//			  vertexSubset.add(source);
//			  if(graph instanceof SPARQLGraph){
//				  allSubgraphs.add(new SPARQLSubgraph(graph, vertexSubset, edgeSubset));
//			  } else {
//				  allSubgraphs.add(new DependencySubgraph(graph, vertexSubset, edgeSubset));
//			  }
//			  
//			  //node+parent
//			  if(sedge != null){
//				  vertexSubset = new HashSet<Node>();
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  vertexSubset.add(source);
//				  vertexSubset.add(graph.getEdgeSource(sedge));
//				  edgeSubset.add(sedge);
//				  if(graph instanceof SPARQLGraph){
//					  allSubgraphs.add(new SPARQLSubgraph(graph, vertexSubset, edgeSubset));
//				  } else {
//					  allSubgraphs.add(new DependencySubgraph(graph, vertexSubset, edgeSubset));
//				  }
//			  }
//			  
//			  //node+subgraph
//			  for(ColoredEdge edge : outgoingEdges){
//				  Node target = graph.getEdgeTarget(edge);
//				  sub = postorderTraversal(graph, target, edge, allSubgraphs);
//				  vertexSubset = new HashSet<Node>();
//				  vertexSubset.add(source);
//				  vertexSubset.addAll(sub.vertexSet());
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  edgeSubset.addAll(sub.edgeSet());
//				  edgeSubset.add(edge);
//				  if(graph instanceof SPARQLGraph){
//					  sub = new SPARQLSubgraph(graph, vertexSubset, edgeSubset);
//				  } else {
//					  sub = new DependencySubgraph(graph, vertexSubset, edgeSubset);
//				  }
//				  allSubgraphs.add(sub);
//				  map.put(edge, sub);
//			  }
//			  
//			  //node+subgraph+subgraph
//			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry1 : map.entrySet()){
//				  ColoredDirectedSubgraph sub1 = entry1.getValue();
//				  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry2 : map.entrySet()){
//					  if(!entry1.equals(entry2)){
//						  ColoredDirectedSubgraph sub2 = entry2.getValue();
//						  
//						  vertexSubset = new HashSet<Node>();
//						  vertexSubset.addAll(sub1.vertexSet());
//						  vertexSubset.addAll(sub2.vertexSet());
//						  edgeSubset = new HashSet<ColoredEdge>();
//						  edgeSubset.addAll(sub1.edgeSet());
//						  edgeSubset.addAll(sub2.edgeSet());
//						  
//						  vertexSubset.add(source);
//						  edgeSubset.add(entry1.getKey());
//						  edgeSubset.add(entry2.getKey());
//						  
//						  if(graph instanceof SPARQLGraph){
//							  allSubgraphs.add(new SPARQLSubgraph(graph, vertexSubset, edgeSubset));
//						  } else {
//							  allSubgraphs.add(new DependencySubgraph(graph, vertexSubset, edgeSubset));
//						  }
//					  }
//				  }
//			  }
//			  //node+all subgraphs
//			  vertexSubset = new HashSet<Node>();
//			  edgeSubset = new HashSet<ColoredEdge>();
//			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry : map.entrySet()){
//				  ColoredDirectedSubgraph g = entry.getValue();
//				  vertexSubset.addAll(g.vertexSet());
//				  edgeSubset.addAll(g.edgeSet());
//				  edgeSubset.add(entry.getKey());
//			  }
//			  vertexSubset.add(source);
//			  if(graph instanceof SPARQLGraph){
//				  return new SPARQLSubgraph(graph, vertexSubset, edgeSubset);
//			  } else {
//				  return new DependencySubgraph(graph, vertexSubset, edgeSubset);
//			  }
//		  } else {
//			  Set<Node> vertexSubset = new HashSet<Node>();
//			  vertexSubset.add(source);
//			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
//			  ColoredDirectedSubgraph sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
//			  allSubgraphs.add(sub);
//			  if(outgoingEdges.size() == 1){
//				  ColoredEdge edge = outgoingEdges.iterator().next();
//				  Node target = graph.getEdgeTarget(edge);
//				  ColoredDirectedSubgraph sub2 = postorderTraversal(graph, target, edge, allSubgraphs);
//				  vertexSubset = new HashSet<Node>();
//				  vertexSubset.add(source);
//				  vertexSubset.addAll(sub2.vertexSet());
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  edgeSubset.addAll(sub2.edgeSet());
//				  edgeSubset.add(edge);
//				  if(graph instanceof SPARQLGraph){
//					  sub = new SPARQLSubgraph(graph, vertexSubset, edgeSubset);
//				  } else {
//					  sub = new DependencySubgraph(graph, vertexSubset, edgeSubset);
//				  }
//				  allSubgraphs.add(sub);
//			  } 
//			  return sub;
//		
//		  }
//	}
	
//	private static ColoredDirectedSubgraph postorderTraversal(DirectedGraph<Node, ColoredEdge> graph, Node source, ColoredEdge sedge, Collection<ColoredDirectedSubgraph> allSubgraphs){
//		  Set<ColoredEdge> outgoingEdges = graph.outgoingEdgesOf(source);
//		  if(outgoingEdges.size() > 1){
//			  Map<ColoredEdge, ColoredDirectedSubgraph> map = new HashMap<ColoredEdge, ColoredDirectedSubgraph>();
//			  ColoredDirectedSubgraph sub;
//			  Set<Node> vertexSubset = new HashSet<Node>();
//			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
//			  
//			  //node
//			  vertexSubset.add(source);
//			  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
//			  
//			  //node+parent
//			  if(sedge != null){
//				  vertexSubset = new HashSet<Node>();
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  vertexSubset.add(source);
//				  vertexSubset.add(graph.getEdgeSource(sedge));
//				  edgeSubset.add(sedge);
//				  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
//			  }
//			  
//			  //node+subgraph
//			  for(ColoredEdge edge : outgoingEdges){
//				  Node target = graph.getEdgeTarget(edge);
//				  sub = postorderTraversal(graph, target, edge, allSubgraphs);
//				  vertexSubset = new HashSet<Node>();
//				  vertexSubset.add(source);
//				  vertexSubset.addAll(sub.vertexSet());
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  edgeSubset.addAll(sub.edgeSet());
//				  edgeSubset.add(edge);
//				  sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
//				  allSubgraphs.add(sub);
//				  map.put(edge, sub);
//			  }
//			  
//			  //node+subgraph+subgraph
//			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry1 : map.entrySet()){
//				  ColoredDirectedSubgraph sub1 = entry1.getValue();
//				  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry2 : map.entrySet()){
//					  if(!entry1.equals(entry2)){
//						  ColoredDirectedSubgraph sub2 = entry2.getValue();
//						  
//						  vertexSubset = new HashSet<Node>();
//						  vertexSubset.addAll(sub1.vertexSet());
//						  vertexSubset.addAll(sub2.vertexSet());
//						  edgeSubset = new HashSet<ColoredEdge>();
//						  edgeSubset.addAll(sub1.edgeSet());
//						  edgeSubset.addAll(sub2.edgeSet());
//						  
//						  vertexSubset.add(source);
//						  edgeSubset.add(entry1.getKey());
//						  edgeSubset.add(entry2.getKey());
//						  
//						  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
//					  }
//				  }
//			  }
//			  //node+all subgraphs
//			  vertexSubset = new HashSet<Node>();
//			  edgeSubset = new HashSet<ColoredEdge>();
//			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry : map.entrySet()){
//				  ColoredDirectedSubgraph g = entry.getValue();
//				  vertexSubset.addAll(g.vertexSet());
//				  edgeSubset.addAll(g.edgeSet());
//				  edgeSubset.add(entry.getKey());
//			  }
//			  vertexSubset.add(source);
//			  return new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
//		  } else {
//			  Set<Node> vertexSubset = new HashSet<Node>();
//			  vertexSubset.add(source);
//			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
//			  ColoredDirectedSubgraph sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
//			  allSubgraphs.add(sub);
//			  if(outgoingEdges.size() == 1){
//				  ColoredEdge edge = outgoingEdges.iterator().next();
//				  Node target = graph.getEdgeTarget(edge);
//				  ColoredDirectedSubgraph sub2 = postorderTraversal(graph, target, edge, allSubgraphs);
//				  vertexSubset = new HashSet<Node>();
//				  vertexSubset.add(source);
//				  vertexSubset.addAll(sub2.vertexSet());
//				  edgeSubset = new HashSet<ColoredEdge>();
//				  edgeSubset.addAll(sub2.edgeSet());
//				  edgeSubset.add(edge);
//				  sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
//				  allSubgraphs.add(sub);
//			  } 
//			  return sub;
//		
//		  }
//	}
	
	private static Set<ColoredDirectedSubgraph> postorderTraversal(DirectedGraph<Node, ColoredEdge> graph, Node source,
			ColoredEdge sedge, Collection<ColoredDirectedSubgraph> allSubgraphs) {
		Set<ColoredDirectedSubgraph> newSubgraphs = new HashSet<ColoredDirectedSubgraph>();
		Set<Node> vertexSubset = new HashSet<Node>();
		vertexSubset.add(source);
		Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
		ColoredDirectedSubgraph sub;
		if(graph instanceof SPARQLGraph){
			sub = new SPARQLSubgraph(graph, vertexSubset, edgeSubset);
		} else {
			sub = new DependencySubgraph(graph, vertexSubset, edgeSubset);
		}
		
		allSubgraphs.add(sub);
		Set<ColoredEdge> outgoingEdges = graph.outgoingEdgesOf(source);
		for (ColoredEdge edge : outgoingEdges) {
			Set<ColoredDirectedSubgraph> subgraphs = postorderTraversal(graph, graph.getEdgeTarget(edge), edge, allSubgraphs);
			for (ColoredDirectedSubgraph s : subgraphs) {
				ColoredDirectedSubgraph merged = connect(sub, s, edge);
				allSubgraphs.add(merged);
				newSubgraphs.add(merged);
			}
		}
		
		Set<ColoredDirectedSubgraph> tmp = new HashSet<ColoredDirectedSubgraph>();
		for (ColoredDirectedSubgraph s1 : newSubgraphs) {
			ColoredDirectedSubgraph merged;
			for (ColoredDirectedSubgraph s2 : newSubgraphs) {
				if(!s1.equals(s2)){
					tmp.add(merge(s1, s2));
				}
			}
		}
		newSubgraphs.addAll(findAllFlat(newSubgraphs));
		newSubgraphs.add(sub);

		if(sedge == null){
			allSubgraphs.addAll(newSubgraphs);
		}
		return newSubgraphs;
	}
	
	private static ColoredDirectedSubgraph merge(ColoredDirectedSubgraph g1, ColoredDirectedSubgraph g2){
		Set<Node> vertexSubset = new HashSet<Node>();
		Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
		vertexSubset.addAll(g1.vertexSet());
		vertexSubset.addAll(g2.vertexSet());
		edgeSubset.addAll(g1.edgeSet());
		edgeSubset.addAll(g2.edgeSet());
		if(g1 instanceof SPARQLSubgraph){
			return new SPARQLSubgraph(g1.getBase(), vertexSubset, edgeSubset);
		} else {
			return new DependencySubgraph(g1.getBase(), vertexSubset, edgeSubset);
		}
		
	}
	
	private static ColoredDirectedSubgraph connect(ColoredDirectedSubgraph g1, ColoredDirectedSubgraph g2, ColoredEdge edge){
		Set<Node> vertexSubset = new HashSet<Node>();
		Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
		vertexSubset.addAll(g1.vertexSet());
		vertexSubset.addAll(g2.vertexSet());
		edgeSubset.addAll(g1.edgeSet());
		edgeSubset.addAll(g2.edgeSet());
		edgeSubset.add(edge);
		if(g1 instanceof SPARQLSubgraph){
			return new SPARQLSubgraph(g1.getBase(), vertexSubset, edgeSubset);
		} else {
			return new DependencySubgraph(g1.getBase(), vertexSubset, edgeSubset);
		}
	}
	
	public static <T extends ColoredDirectedGraph> SortedSet<T> sort(Collection<T> graphs){
		SortedSet<T> sortedGraphs = new TreeSet<T>(new Comparator<T>() {

			public int compare(T o1, T o2) {
				Set<Node> vertices1 = o1.vertexSet();
				Set<Node> vertices2 = o2.vertexSet();
				int diff = vertices1.size() - vertices2.size();
				if(diff == 0){
					Set<ColoredEdge> edges1 = o1.edgeSet();
					Set<ColoredEdge> edges2 = o2.edgeSet();
					diff = edges1.size() - edges2.size();
					if(diff == 0){
						boolean equalVertices = vertices1.equals(vertices2);
						boolean equalEdges = edges1.equals(edges2);
						if(equalVertices && equalEdges){
							return 0;
						} else {
							return -1;
						}
					} else {
						return diff;
					}
				} else {
					return diff;
				}
			}
		
		});
		sortedGraphs.addAll(graphs);
		return sortedGraphs;
	}
	
	public static SortedSet<ColoredDirectedSubgraph> sortSubgraphs(Collection<ColoredDirectedSubgraph> graphs){
		SortedSet<ColoredDirectedSubgraph> sortedGraphs = new TreeSet<ColoredDirectedSubgraph>(new Comparator<ColoredDirectedSubgraph>() {

			public int compare(ColoredDirectedSubgraph o1, ColoredDirectedSubgraph o2) {
				Set<Node> vertices1 = o1.vertexSet();
				Set<Node> vertices2 = o2.vertexSet();
				int diff = vertices1.size() - vertices2.size();
				if(diff == 0){
					Set<ColoredEdge> edges1 = o1.edgeSet();
					Set<ColoredEdge> edges2 = o2.edgeSet();
					diff = edges1.size() - edges2.size();
					if(diff == 0){
						boolean equalVertices = vertices1.equals(vertices2);
						boolean equalEdges = edges1.equals(edges2);
						if(equalVertices && equalEdges){
							return 0;
						} else {
							return -1;
						}
					} else {
						return diff;
					}
				} else {
					return diff;
				}
			}
		
		});
		sortedGraphs.addAll(graphs);
		return sortedGraphs;
	}
	
	public static void paint(ColoredDirectedGraph graph, String title) {
		JGraphModelAdapter model = new JGraphModelAdapter(graph);
		JGraph jgraph = new JGraph(model);
		JScrollPane scroller = new JScrollPane(jgraph);
		JFrame frame = new JFrame(title);
		frame.setAlwaysOnTop(true);
		frame.setSize(1000, 1000);
		frame.add(scroller);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
		// now add the data (the HumanOrgansGraph example from Chapter 5)
		// CircleGraphLayout layout = new CircleGraphLayout();
		// layout.run(jgraph, jgraph.getRoots(), new Object[]{});
		
		layout(jgraph, model, graph);
		
		
	    
	   frame.pack();
	}
	
	public static Set<ColoredDirectedSubgraph> findAllFlat(Set<ColoredDirectedSubgraph> in) {
		Set<ColoredDirectedSubgraph> out = new HashSet<ColoredDirectedSubgraph>();

		if (in.size() > 1) {
			for (ColoredDirectedSubgraph s1 : in) {
				for (ColoredDirectedSubgraph s2 : in) {
					if (!s2.equals(s1)) {
						ColoredDirectedSubgraph merged = merge(s1, s2);
						out.add(merged);
					}

				}
			}
			out.addAll(findAllFlat(out));
		}

		return out;
	}
	
	public <V,E> boolean contains(Graph<V, E> container, Graph<V, E> containee){
		if(container.vertexSet().size() < containee.vertexSet().size()){
			return false;
		}
		return false;
	}
	
	public static boolean contains(ColoredDirectedGraph container, ColoredDirectedGraph containee){
		if(container.vertexSet().size() < containee.vertexSet().size()){
			return false;
		}
		//get all subgraphs of the container graph
		Collection<ColoredDirectedSubgraph> subgraphs = getSubgraphs(container);
		//remove all subgraphs 
		//a) with a number of nodes different to the number of nodes in the containee
		//b) with a different set of node labels
		for (Iterator<ColoredDirectedSubgraph> iterator = subgraphs.iterator(); iterator.hasNext();) {
			ColoredDirectedSubgraph coloredDirectedSubgraph = (ColoredDirectedSubgraph) iterator.next();
			if(coloredDirectedSubgraph.vertexSet().size() != containee.vertexSet().size()){
				iterator.remove();
				continue;
			}
			boolean sameNodeLabels = true;
			for(Node node1 : coloredDirectedSubgraph.vertexSet()){
				boolean labelContained = false;
				for(Node node2 : containee.vertexSet()){
					if(node1.getLabel().equals(node2.getLabel())){
						labelContained = true;
						break;
					}
				}
				if(!labelContained){
					sameNodeLabels = false;
					break;
				}
			}
			if(!sameNodeLabels){
				iterator.remove();
			}
		}
		//check for each subgraph if it is isomorph to the containee
		for (ColoredDirectedSubgraph coloredDirectedSubgraph : subgraphs) {
			IGraphAccessor g1 = SimPackGraphWrapper.getGraphIdBased(coloredDirectedSubgraph);
			IGraphAccessor g2 = SimPackGraphWrapper.getGraphIdBased(containee);
			System.out.println("Nodes G1: " + g1.getNodeSet());
			System.out.println("Nodes G2: " + g2.getNodeSet());
	        
			GraphIsomorphism gi = new GraphIsomorphism(g1, g2);
			gi.calculate();
			System.out.println(gi.getSimilarity());
			
			g1 = SimPackGraphWrapper.getGraph(coloredDirectedSubgraph);
			g2 = SimPackGraphWrapper.getGraph(containee);
			System.out.println("Nodes G1: " + g1.getNodeSet());
			System.out.println("Nodes G2: " + g2.getNodeSet());
			gi = new GraphIsomorphism(g1, g2);
			gi.calculate();
			System.out.println(gi.getSimilarity());
			
			if(gi.getSimilarity() == 1){
				return true;
			}
		}
		return false;
	}
	
	private static Set<ColoredDirectedSubgraph> getSubsets(ArrayList<ColoredDirectedSubgraph> set) {

		Set<ColoredDirectedSubgraph> subsetCollection = new HashSet<ColoredDirectedSubgraph>();

		if (set.size() == 1) {
			subsetCollection.add(set.get(0));
		} else {
			ArrayList<ColoredDirectedSubgraph> reducedSet = new ArrayList<ColoredDirectedSubgraph>();

			reducedSet.addAll(set);

			ColoredDirectedSubgraph first = reducedSet.remove(0);
			Set<ColoredDirectedSubgraph> subsets = getSubsets(reducedSet);
			subsetCollection.addAll(subsets);

			subsets = getSubsets(reducedSet);

			Set<ColoredDirectedSubgraph> tmp = new HashSet<ColoredDirectedSubgraph>();
			for (ColoredDirectedSubgraph subset : subsets) {
				tmp.add(merge(first, subset));
			}
			subsets.addAll(tmp);

			subsetCollection.addAll(subsets);
		}

		return subsetCollection;
	}
	
	private static void layout(JGraph jgraph, JGraphModelAdapter model, ColoredDirectedGraph jgrapht){ 
		adjustNodeWidths(jgraph, model, jgrapht);
		JGraphTreeLayout layout = new JGraphTreeLayout();
		JGraphFacade jgf = new JGraphFacade(jgraph, new Object[] {});
		layout.run(jgf);
		final Map nestedMap = jgf.createNestedMap(true, true);
	    jgraph.getGraphLayoutCache().edit(nestedMap);
	    jgraph.getGraphLayoutCache().update();
	    jgraph.refresh();
	}
	
	private static void adjustNodeWidths(JGraph graph, JGraphModelAdapter model, ColoredDirectedGraph jgrapht) {
        Map<DefaultGraphCell,AttributeMap> viewMap = new HashMap<DefaultGraphCell,AttributeMap>();
        
        for(Node vertex  : jgrapht.vertexSet()) {
        	DefaultGraphCell node = model.getVertexCell(vertex);
            AttributeMap map = model.getDefaultVertexAttributes();
            
            GraphConstants.setBounds(map,
                    map.createRect(0,0,computeNodeWidth(graph, node), 30));
            viewMap.put(node, map);
        }
        
        graph.getGraphLayoutCache().edit(viewMap, null, null, null);
    }
	
	private static int computeNodeWidth(JGraph graph, DefaultGraphCell node) {
        return computeTextWidth(graph, getNodeData(node));
    }
	
	private static String getNodeData(DefaultGraphCell node) {
        return node.getUserObject().toString();
    }
	
	private static int computeTextWidth(JGraph graph, String str) {
        JRootPane pane = SwingUtilities.getRootPane(graph);
        int ret = 30; // a default node width
        
        if( (pane != null)  && !"".equals(str) ) {
            Graphics2D g2 = (Graphics2D) pane.getGraphics();
            
            TextLayout tl = new TextLayout(str, nodeFont, g2.getFontRenderContext());
            int trueWidth = (int) (1.2*tl.getBounds().getWidth() + 10);
            if( trueWidth > ret ) {
                ret = trueWidth;
            }
        }
        
        return ret;
    }
	
	public static void writeSVG(ColoredDirectedGraph graph, OutputStream out, int inset) throws UnsupportedEncodingException, SVGGraphics2DIOException{
		JGraphModelAdapter model = new JGraphModelAdapter(graph);
		JGraph jgraph = new JGraph(model);
		layout(jgraph, model, graph);
		Object[] cells = jgraph.getRoots();
		Rectangle2D bounds = jgraph.toScreen(jgraph.getCellBounds(cells));
		if (bounds != null) {// Constructs the svg generator used for painting the graph to
			DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
			Document doc = domImpl.createDocument(null, "svg", null);
			SVGGraphics2D svgGen = new SVGGraphics2D(doc);
			svgGen.translate(-bounds.getX()+inset, -bounds.getY()+inset);
			RepaintManager currentManager = RepaintManager.currentManager(jgraph);
			currentManager.setDoubleBufferingEnabled(false);
			jgraph.paint(svgGen);
			Writer writer = new OutputStreamWriter(out, "UTF-8");
			svgGen.stream(writer, false);
			currentManager.setDoubleBufferingEnabled(true);
		}
	}

}
