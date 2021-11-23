package com.example.graph_editor.model;

import android.util.Pair;

import com.example.graph_editor.draw.Frame;
import com.example.graph_editor.model.mathematics.Geometry;
import com.example.graph_editor.model.mathematics.Point;

import java.util.ArrayList;
import java.util.List;

public class DrawManager {
    private Point leftTop;
    private double width;
    private double height;
    private final Graph graph;

    public DrawManager(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public void updateFrame(Frame frame) {
        //uses the Frame as if it was a struct, doesn't remember the reference
        Pair<Point, Point> points = frame.getPoints();
        Point leftTop = points.first;
        Point rightBot = points.second;
        this.leftTop = leftTop;
        this.width = rightBot.getX() - leftTop.getX();
        this.height = rightBot.getY() - leftTop.getY();
        if(new Point(width, height).equals(Point.ZERO)) {
            throw new RuntimeException("Frame size equals 0 or is negative");
        }
    }

    public Point getRelative(Point point) {
        double x = (point.getX() - leftTop.getX())/width;
        double y = (point.getY() - leftTop.getY())/height;
        return new Point(x, y);
    }

    public Point getAbsolute(Point point) {
        double x = leftTop.getX() + point.getX() * width;
        double y = leftTop.getY() + point.getY() * height;
        return new Point(x, y);
    }
    public double getRelativeDistanceFrom(Point relativePoint, Vertex vertex) {
        return Geometry.distance(relativePoint, getRelative(vertex.getPoint()));
    }
    public double getRelativeDistanceFrom(Point relativePoint, Edge edge) {
        return Geometry.distanceFromSegment(relativePoint,
                getRelative(edge.getSource().getPoint()), getRelative(edge.getTarget().getPoint()));
    }

    //returns null if there are no vertices
    public Vertex getNearestVertex(Point relativePoint, double delta) {
        Point point = getAbsolute(relativePoint);
        double nearest = Double.MAX_VALUE;
        Vertex result = null;
        for(Vertex vertex : graph.getVertices()) {
            double distance = Geometry.distance(point, vertex.getPoint());
            if( distance < nearest){
                result = vertex;
                nearest = distance;
            }
        }

        if (result != null && getRelativeDistanceFrom(relativePoint, result) > delta)
            return null;
        return result;
    }
//
//    //returns null if there are no edges
//    public Edge getNearestEdge(Point relativePoint, double delta) {
//        Point point = getAbsolute(relativePoint);
//        double nearest = Double.MAX_VALUE;
//        Edge result = null;
//        for(Edge edge : graph.getEdges()) {
//            double distance = Geometry.distanceFromSegment(point,
//                    edge.getSource().getPoint(), edge.getTarget().getPoint());
//            if( distance < nearest){
//                result = edge;
//                nearest = distance;
//            }
//        }
//
//
//        return result;
//    }

    public List<Vertex> getVertices() { return graph.getVertices(); }

    public List<Edge> getEdges() { return graph.getEdges(); }
}