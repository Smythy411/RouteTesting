package org.eoin.route.routetesting;

/*OSMEdge class extends the Default Edge class to work better with OSMData
 * and allows for easier testing
 */
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

public class OSMEdge implements Serializable{

    private static final long serialVersionUID = 1L;

    private long edgeID;
    private long wayID;

    @JsonIgnoreProperties("edges")
    OSMNode sourceNode, targetNode;
    boolean visited;
    Double distance = null;

	/*
	 * 		CONSTRUCTORS
	 */

    public OSMEdge() {
        super();
    }//End constructor

    public OSMEdge(long wID) {
        this.wayID = wID;
        this.visited = false;
    }//End constructor

    public OSMEdge(long eID, long wID) {
        this.edgeID = eID;
        this.wayID = wID;
        this.visited = false;
    }//End constructor

    public OSMEdge(OSMNode source, OSMNode target) {
		/*
		DBManager db = new DBManager("GraphTesting");
		this.wayID = db.getWayIdByEdge(source.getNodeID(), target.getNodeID());
		db.close();
		this.label = Long.toString(wayID);
		*/
        this.sourceNode = source;
        this.targetNode = target;
        this.visited = false;
        this.distance = calculateDistance(source, target);
    }//End constructor

    public OSMEdge(long wID, OSMNode source, OSMNode target) {
        this.wayID = wID;
        this.sourceNode = source;
        this.targetNode = target;
        this.visited = false;
        this.distance = calculateDistance(source, target);
    }//End constructor

    public OSMEdge(long eID, long wID, OSMNode source, OSMNode target) {
        this.edgeID = eID;
        this.wayID = wID;
        this.sourceNode = source;
        this.targetNode = target;
        this.visited = false;
        this.distance = calculateDistance(source, target);
    }//End constructor

	/*
	 * 		METHODS
	 */

    public long getEdgeID() {
        return this.edgeID;
    }//End getWayID()

    public void setEdgeID(long eID) {
        this.edgeID = eID;
    }//End setWayID()

    public long getWayID() {
        return this.wayID;
    }//End getWayID()

    public void setWayID(long wID) {
        this.wayID = wID;
    }//End setWayID()

    public OSMNode getSourceNode() {
        return this.sourceNode;
    }//End getSourceNode()

    public void setSourceNode(OSMNode source) {
        this.sourceNode = source;
    }//End setSourceNode()

    public OSMNode getTargetNode() {
        return this.targetNode;
    }//End getTargetNode()

    public void setTargetNode(OSMNode target) {
        this.targetNode = target;
    }//End setTargetNode()

    public OSMNode getNeighbour(OSMNode v) {
        if (v == this.sourceNode ) {
            return this.targetNode;
        } else if (v == this.targetNode){
            return this.sourceNode;
        } else {
            return null;
        }//End if else
    }//End getNeighbour

    public OSMNode[] getBothNodes() {
        OSMNode[]  nodes = {this.sourceNode, this.targetNode};
        return nodes;
    }//End getBothNodes()

    public void setVisited() {
        this.visited = true;
    }//End setVisited()

    public boolean getVisited() {
        return this.visited;
    }//End getVisited()

    public Double calculateDistance(OSMNode source, OSMNode target) {
        return Haversine.distance(Double.parseDouble(source.getLat()), Double.parseDouble(source.getLon()),
                Double.parseDouble(target.getLat()), Double.parseDouble(target.getLon()));
    }//End clalculateDistance()

    public Double getDistance() {
        return this.distance;
    }//End getDistance()
    @Override
    public String toString()
    {
        return this.wayID + " : "+ this.sourceNode + "->" + this.targetNode;
    }//End toString()
}//End OSMEdge
