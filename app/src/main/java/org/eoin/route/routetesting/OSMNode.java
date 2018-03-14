package org.eoin.route.routetesting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;

/*OSMNode is a custom vertex class for working with OSM data
 */
public class OSMNode {

    private int id;
    private long nodeID;
    private String lat;
    private String lon;

    @JsonIgnoreProperties({"sourceNode", "targetNode"})
    ArrayList<OSMEdge> edges = new ArrayList<OSMEdge>();

	/*
	 * 		CONSTRUCTORS
	 */

    public OSMNode() {

    }//End OSMNode

    public OSMNode(int passedID) {
        this.id = passedID;
    }//End passedOSMNode

    public OSMNode(long passedNodeID) {
        this.nodeID = passedNodeID;
    }//End passedOSMNode

    public OSMNode(int passedId, long passedNodeID, String passedLat, String passedLon) {
        this.id = passedId;
        this.nodeID = passedNodeID;
        this.lat = passedLat;
        this.lon = passedLon;
    }//End OSMNode constructor

    public OSMNode(long passedNodeID, String passedLat, String passedLon) {
        this.nodeID = passedNodeID;
        this.lat = passedLat;
        this.lon = passedLon;
    }//End OSMNode Constructor

	/*
	 * 		METHODS
	 */

    public void setNodeID(long passedNodeID) {
        this.nodeID = passedNodeID;
    }//End setNodeID

    public long getNodeID() {
        return this.nodeID;
    }//End getNodeID

    public void setLat(String passedLat) {
        this.lat = passedLat;
    }//End setLat

    public String getLat() {
        return this.lat;
    }//End getNodeID

    public void setLon(String passedLon) {
        this.lon = passedLon;
    }//End setLon

    public String getLon() {
        return this.lon;
    }//End getNodeID

    public void addEdge(OSMEdge e) {
        this.edges.add(e);
    }

    public ArrayList<OSMEdge> getEdges() {
        return this.edges;
    }

    @Override
    public String toString()
    {
        return "(" + this.nodeID + ")";
    }//End toString
}//End OSMNode
