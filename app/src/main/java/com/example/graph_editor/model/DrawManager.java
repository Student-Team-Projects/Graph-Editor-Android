package com.example.graph_editor.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import graph_editor.geometry.GeometryUtils;
import graph_editor.geometry.Point;
import graph_editor.graph.Edge;
import graph_editor.graph.Graph;
import graph_editor.graph.Vertex;

public class DrawManager {
    public static Point getRelative(Rectangle rectangle, Point point) {
        double x = (point.getX() - rectangle.getLeft())/rectangle.getWidth();
        double y = (point.getY() - rectangle.getTop())/rectangle.getHeight();
        return new Point(x, y);
    }

    public static Point getAbsolute(Rectangle rectangle, Point point) {
        double x = rectangle.getLeft() + point.getX() * rectangle.getWidth();
        double y = rectangle.getTop() + point.getY() * rectangle.getHeight();
        return new Point(x, y);
    }
    public static double getRelativeDistanceFrom(Map<Vertex, Point> mapping, Rectangle rectangle, Point relativePoint, Vertex vertex) {
        return GeometryUtils.distance(relativePoint, getRelative(rectangle, mapping.get(vertex)));
    }
    public static double getRelativeDistanceFrom(Map<Vertex, Point> mapping, Rectangle rectangle, Point relativePoint, Edge edge) {
        return GeometryUtils.distanceFromSegment(relativePoint,
                getRelative(rectangle, mapping.get(edge.getSource())),
                getRelative(rectangle, mapping.get(edge.getTarget()))
        );
    }

    //returns null if there are no vertices
    public static Vertex getNearestVertex(Map<Vertex, Point> mapping, Graph graph, Rectangle rectangle, Point relativePoint, double delta, Set<Vertex> excluded) {
        Point point = getAbsolute(rectangle, relativePoint);
        double nearest = Double.MAX_VALUE;
        Vertex result = null;
        for(Vertex vertex : graph.getVertices()) {
            if (excluded.contains(vertex))
                continue;
            double distance = GeometryUtils.distance(point, mapping.get(vertex));
            if( distance < nearest){
                result = vertex;
                nearest = distance;
            }
        }

        if (result != null && getRelativeDistanceFrom(mapping, rectangle, relativePoint, result) > delta)
            return null;
        return result;
    }

    //returns null if there are no edges
    public static Edge getNearestEdge(Map<Vertex, Point> mapping, Graph graph, Rectangle rectangle, Point relativePoint, double delta) {
        Point point = getAbsolute(rectangle, relativePoint);
        double nearest = Double.MAX_VALUE;
        Edge result = null;
        for(Edge edge : graph.getEdges()) {
            double distance = GeometryUtils.distanceFromSegment(point,
                    mapping.get(edge.getSource()), mapping.get(edge.getTarget()));
            if( distance < nearest){
                result = edge;
                nearest = distance;
            }
        }
        if (result != null && getRelativeDistanceFrom(mapping, rectangle, relativePoint, result) > delta)
            return null;

        return result;
    }

    private static Rectangle getExtremeRectangle(Map<Vertex, Point> mapping, Graph graph) {
        Point extremeLeftTop = new Point(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Point extremeRightBot = new Point(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for(Vertex vertex : graph.getVertices()) {
            Point point = mapping.get(vertex);
            if(point.getX()<extremeLeftTop.getX())
                extremeLeftTop = new Point(point.getX(), extremeLeftTop.getY());
            if(point.getY()<extremeLeftTop.getY())
                extremeLeftTop = new Point(extremeLeftTop.getX(), point.getY());
            if(point.getX()>extremeRightBot.getX())
                extremeRightBot = new Point(point.getX(), extremeRightBot.getY());
            if(point.getY()>extremeRightBot.getY())
                extremeRightBot = new Point(extremeRightBot.getX(), point.getY());
        }
        return new Rectangle(extremeLeftTop, extremeRightBot);
    }

    public static void normalizeGraph(Map<Vertex, Point> mapping, Graph graph) {
        Rectangle extremeRectangle = getExtremeRectangle(mapping, graph);
        Point extremeLeftTop = extremeRectangle.getLeftTop();
        Point extremeRightBot = extremeRectangle.getRightBot();
        double extremeWidth = extremeRightBot.getX() - extremeLeftTop.getX();
        double extremeHeight = extremeRightBot.getY() - extremeLeftTop.getY();
        for(Vertex vertex : graph.getVertices()) {
            Point point = mapping.get(vertex);
            double x = (extremeWidth == 0.0)? 0.0 : (point.getX()-extremeLeftTop.getX())/extremeWidth;
            double y = (extremeHeight == 0.0)? 0.0 : (point.getY()-extremeLeftTop.getY())/extremeHeight;
            mapping.put(vertex, new Point(x, y));
        }
    }

    public static Rectangle getOptimalRectangle(Map<Vertex, Point> mapping, Graph graph, double paddingPercent, Rectangle rectangle) {
        List<Vertex> vertices = graph.getVertices();
        if(vertices.isEmpty()){
            return new Rectangle(new Point(0,0), new Point(1, rectangle.getHeight()/rectangle.getWidth()));
        }

        Rectangle extremeRectangle = getExtremeRectangle(mapping, graph);
        Point extremeLeftTop = extremeRectangle.getLeftTop();
        Point extremeRightBot = extremeRectangle.getRightBot();

        double extremeWidth = extremeRightBot.getX() - extremeLeftTop.getX();
        double extremeHeight = extremeRightBot.getY() - extremeLeftTop.getY();
        double scale = Math.max(extremeWidth/rectangle.getWidth(), extremeHeight/rectangle.getHeight());
        Point center = new Point(extremeLeftTop.getX()+extremeWidth/2, extremeLeftTop.getY()+extremeHeight/2);
        Point resultLeftTop = new Point(center.getX()-(0.5+paddingPercent)*rectangle.getWidth()*scale,
                center.getY()-(0.5+paddingPercent)*rectangle.getHeight()*scale);
        Point resultRightBot = new Point(center.getX()+rectangle.getWidth()*scale*(0.5+paddingPercent),
                center.getY()+rectangle.getHeight()*scale*(0.5+paddingPercent));

        if(vertices.size() == 1) {
            resultLeftTop = new Point(resultLeftTop.getX()-1, resultLeftTop.getY()-1);
            resultRightBot = new Point(resultRightBot.getX()+1, resultRightBot.getY()+1);
        }

        return new Rectangle(resultLeftTop, resultRightBot);
    }

    public static void translate(Rectangle rectangle, double dxNew, double dyNew) {
        Rectangle temp = new Rectangle(rectangle, dxNew*rectangle.getScale(), dyNew*rectangle.getScale());
        rectangle.setLeftTop(temp.getLeftTop());
        rectangle.setRightBot(temp.getRightBot());
    }
    static int x = 0;
    public static Rectangle getZoomedRectangle(Rectangle original, Point startA, Point startB, Point endARelative, Point endBRelative) {
        Point endA = getAbsolute(original, endARelative), endB = getAbsolute(original, endBRelative);
        double scale = GeometryUtils.distance(startA, startB)/GeometryUtils.distance(endA, endB);
        Point startCenter = GeometryUtils.centerPoint(startA, startB), endCenter = GeometryUtils.centerPoint(endA, endB);

        double toLeft = endCenter.getX() - original.getLeft();
        double toTop = endCenter.getY() - original.getTop();

        Point newLeftTop = new Point(startCenter.getX()-toLeft*scale, startCenter.getY()-toTop*scale);
        return new Rectangle(newLeftTop, original.getWidth()*scale, original.getHeight()*scale);
    }

    public static class Rectangle {
        private Point leftTop;
        private Point rightBot;

        public Rectangle(Point leftTop, Point rightBot) {
            this.leftTop = leftTop;
            this.rightBot = rightBot;
        }

        public Rectangle(Point leftTop, double width, double height) {
            this.leftTop = leftTop;
            this.rightBot = new Point(leftTop.getX()+width, leftTop.getY()+height);
        }

        public Rectangle(Rectangle original, double scale) {
            double width = original.getWidth() * scale;
            double height = original.getHeight() * scale;

            Point centre = original.getCentre();

            leftTop = new Point(centre.getX() - width/2, centre.getY() - height/2);
            rightBot = new Point(centre.getX() + width/2, centre.getY() + height/2);
        }

        public Rectangle(Rectangle original, double dx, double dy) {
            leftTop = original.getLeftTop();
            rightBot = original.getRightBot();

            leftTop = new Point(leftTop.getX() - dx, leftTop.getY() - dy);
            rightBot = new Point(rightBot.getX() - dx, rightBot.getY() - dy);
        }

        public Point getLeftTop() { return leftTop; }
        public Point getRightBot() { return rightBot; }

        public double getLeft() { return  leftTop.getX(); }
        public double getTop() { return  leftTop.getY(); }

        public void setLeftTop(Point leftTop) { this.leftTop = leftTop; }
        public void setRightBot(Point rightBot) { this.rightBot = rightBot; }

        public double getHeight() { return rightBot.getY() - leftTop.getY(); }
        public double getWidth() { return rightBot.getX() - leftTop.getX(); }
        public double getScale() { return getWidth(); }

        public Point getCentre() { return new Point((rightBot.getX() + leftTop.getX())/2, (rightBot.getY() + leftTop.getY())/2); }

        public Rectangle deepCopy() {
            return new Rectangle(leftTop, rightBot);
        }
    }
}