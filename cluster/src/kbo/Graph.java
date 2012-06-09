package kbo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import hypertree.AbstractHTNode;

public class Graph extends AbstractHTNode{
	private ClusterNode node;
	private HashMap<String, Graph> children = null; // the children of this node
	public Graph(ClusterNode node){
		this.node = node;
		children = new HashMap<String, Graph>();
        if (! isLeaf()) {
        	ArrayList<ClusterNode> ancestor = node.getChildNodes();
            for (ClusterNode nd : ancestor) {
                Graph child = new Graph(nd);
                addChild(child);
            }

        }
	}
    protected void addChild(Graph child) {
        children.put(child.getName(), child);
    }
	@Override
	public Iterator<?> children() {
		return this.children.values().iterator();
	}

	@Override
	public boolean isLeaf() {
		if(node.getType() == ClusterNode.CLUSTERLEAF){
			return true;
		}
		else if (node.getChildNodes() == null || node.getChildNodes().isEmpty()){
			return true;
		}
		return false;
	}

	@Override
	public String getName() {
		return String.valueOf(node.clusterID);
	}
    public Color getColor() {
    	if(node.getType() == ClusterNode.CLUSTERLEAF){
    		return new Color(0,191,255);
    	}
    	else if (node.getChildNodes() == null || node.getChildNodes().isEmpty()){
    		return new Color(34,139,34);
    	}
    	else if(node.getChildNodes().size()<=2){
    		return new Color(255,215,0);
    	}
    	else
    		return new Color(178,34,34);
    }
}
