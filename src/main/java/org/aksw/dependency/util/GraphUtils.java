package org.aksw.dependency.util;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
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
import org.aksw.dependency.graph.Node;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.JGraphModelAdapter;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

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
	
	private static Node findRoot(ColoredDirectedGraph graph){
		for(Node vertex : graph.vertexSet()){
			if(graph.inDegreeOf(vertex) == 0 && graph.outDegreeOf(vertex) > 0){
				return vertex;
			}
		}
		//Should never happen
		return null;
	}
	
	private static ColoredDirectedSubgraph postorderTraversal(DirectedGraph<Node, ColoredEdge> graph, Node source, ColoredEdge sedge, Collection<ColoredDirectedSubgraph> allSubgraphs){
		  Set<ColoredEdge> outgoingEdges = graph.outgoingEdgesOf(source);
		  if(outgoingEdges.size() > 1){
			  Map<ColoredEdge, ColoredDirectedSubgraph> map = new HashMap<ColoredEdge, ColoredDirectedSubgraph>();
			  ColoredDirectedSubgraph sub;
			  Set<Node> vertexSubset = new HashSet<Node>();
			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
			  
			  //node
			  vertexSubset.add(source);
			  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
			  
			  //node+parent
			  if(sedge != null){
				  vertexSubset = new HashSet<Node>();
				  edgeSubset = new HashSet<ColoredEdge>();
				  vertexSubset.add(source);
				  vertexSubset.add(graph.getEdgeSource(sedge));
				  edgeSubset.add(sedge);
				  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
			  }
			  
			  //node+subgraph
			  System.out.println(source);
			  System.out.println(outgoingEdges);
			  for(ColoredEdge edge : outgoingEdges){
				  Node target = graph.getEdgeTarget(edge);
				  sub = postorderTraversal(graph, target, edge, allSubgraphs);
				  vertexSubset = new HashSet<Node>();
				  vertexSubset.add(source);
				  vertexSubset.addAll(sub.vertexSet());
				  edgeSubset = new HashSet<ColoredEdge>();
				  edgeSubset.addAll(sub.edgeSet());
				  edgeSubset.add(edge);
				  sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
				  allSubgraphs.add(sub);
				  map.put(edge, sub);
			  }
			  
			  //node+subgraph+subgraph
			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry1 : map.entrySet()){
				  ColoredDirectedSubgraph sub1 = entry1.getValue();
				  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry2 : map.entrySet()){
					  if(!entry1.equals(entry2)){
						  ColoredDirectedSubgraph sub2 = entry2.getValue();
						  
						  vertexSubset = new HashSet<Node>();
						  vertexSubset.addAll(sub1.vertexSet());
						  vertexSubset.addAll(sub2.vertexSet());
						  edgeSubset = new HashSet<ColoredEdge>();
						  edgeSubset.addAll(sub1.edgeSet());
						  edgeSubset.addAll(sub2.edgeSet());
						  
						  vertexSubset.add(source);
						  edgeSubset.add(entry1.getKey());
						  edgeSubset.add(entry2.getKey());
						  
						  allSubgraphs.add(new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset));
					  }
				  }
			  }
			  //node+all subgraphs
			  vertexSubset = new HashSet<Node>();
			  edgeSubset = new HashSet<ColoredEdge>();
			  for(Entry<ColoredEdge, ColoredDirectedSubgraph> entry : map.entrySet()){
				  ColoredDirectedSubgraph g = entry.getValue();
				  vertexSubset.addAll(g.vertexSet());
				  edgeSubset.addAll(g.edgeSet());
				  edgeSubset.add(entry.getKey());
			  }
			  vertexSubset.add(source);
			  return new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
		  } else {
			  Set<Node> vertexSubset = new HashSet<Node>();
			  vertexSubset.add(source);
			  Set<ColoredEdge> edgeSubset = new HashSet<ColoredEdge>();
			  ColoredDirectedSubgraph sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
			  allSubgraphs.add(sub);
			  if(outgoingEdges.size() == 1){
				  ColoredEdge edge = outgoingEdges.iterator().next();
				  Node target = graph.getEdgeTarget(edge);
				  ColoredDirectedSubgraph sub2 = postorderTraversal(graph, target, edge, allSubgraphs);
				  vertexSubset = new HashSet<Node>();
				  vertexSubset.add(source);
				  vertexSubset.addAll(sub2.vertexSet());
				  edgeSubset = new HashSet<ColoredEdge>();
				  edgeSubset.addAll(sub2.edgeSet());
				  edgeSubset.add(edge);
				  sub = new ColoredDirectedSubgraph(graph, vertexSubset, edgeSubset);
				  allSubgraphs.add(sub);
			  } 
			  return sub;
		
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
