package org.eoin.route.routetesting;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/*
Route class that is send back from the server containing the information of the generated route
 */

public class Route {

    @JsonProperty("edges")
	private ArrayList<OSMEdge> edges;
	private double weight;
	
	public Route() {
		this.edges = new ArrayList<OSMEdge>();
	}//End Route Constructor
	
	public void setWeight(double w) {
		this.weight = w;
	}//end setWeight()
	
	public double getWeight() {
		return this.weight;
	}//end getWeight()
	
	public ArrayList<OSMEdge> getRoute() {
		return this.edges;
	}//end getRoute()
	
	public void addToRoute(ArrayList<OSMEdge> path) {
		double weightToAdd = 0.0;
		for (int i = 0; i < path.size(); i++) {
			weightToAdd += path.get(i).getDistance();
		}//end for
		this.weight += weightToAdd;
		
		this.edges.addAll(path);
	}//end addToRoute(ArrayList)
	
	public void addToRoute(List<OSMEdge> path) {
		double weightToAdd = 0.0;
		for (int i = 0; i < path.size(); i++) {
			weightToAdd += path.get(i).getDistance();
		}//end for
		this.weight += weightToAdd;
		
		this.edges.addAll(path);
	}//end addToRoute(List)
	
	public void removeFromRoute(ArrayList<OSMEdge> edgesToRemove) {
		double weightToRemove = 0.0;
		for (int i = 0; i < edgesToRemove.size(); i++) {
			weightToRemove += edgesToRemove.get(i).getDistance();
		}//end for
		this.weight = this.weight - weightToRemove;
		this.edges.removeAll(edgesToRemove);
	}//end removeFromList()
}//End Route()
