package org.eoin.route.routetesting;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;


public class Route {

    @JsonProperty("edges")
	private ArrayList<OSMEdge> edges;
	private double weight;
	
	public Route() {
		this.edges = new ArrayList<OSMEdge>();
	}
	
	public void setWeight(double w) {
		this.weight = w;
	}
	
	public double getWeight() {
		return this.weight;
	}
	
	public ArrayList<OSMEdge> getRoute() {
		return this.edges;
	}
	
	public void addToRoute(ArrayList<OSMEdge> path) {
		double weightToAdd = 0.0;
		for (int i = 0; i < path.size(); i++) {
			weightToAdd += path.get(i).getDistance();
		}
		this.weight += weightToAdd;
		
		this.edges.addAll(path);
	}
	
	public void addToRoute(List<OSMEdge> path) {
		double weightToAdd = 0.0;
		for (int i = 0; i < path.size(); i++) {
			weightToAdd += path.get(i).getDistance();
		}
		this.weight += weightToAdd;
		
		this.edges.addAll(path);
	}
	
	public void removeFromRoute(ArrayList<OSMEdge> edgesToRemove) {
		double weightToRemove = 0.0;
		for (int i = 0; i < edgesToRemove.size(); i++) {
			weightToRemove += edgesToRemove.get(i).getDistance();
		}
		this.weight = this.weight - weightToRemove;
		this.edges.removeAll(edgesToRemove);
	}
}
