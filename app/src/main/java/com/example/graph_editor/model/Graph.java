package com.example.graph_editor.model;

import java.util.List;
import java.util.Optional;

public interface Graph {
    Vertex addVertex();
    void addEdge(Vertex source, Vertex target);
    Edge getEdge(Vertex source, Vertex target);
    void removeVertex(Vertex vertex);
    void removeEdge(Edge edge);
    GraphType getType();
    List<Vertex> getVertices();
    List<Edge> getEdges();
    Graph deepCopy();
    void setVertexProperty(Vertex vertex, String name, String value);
    void removeVertexProperty(String name);
    List<Vertex> getVerticesWithProperty(String name);
    List<String> getVertexPropertyNames();
    void setEdgeProperty(Edge edge, String name, String value);
    void removeEdgeProperty(String name);
    List<Edge> getEdgesWithProperty(String name);
    List<String> getEdgePropertyNames();
}
