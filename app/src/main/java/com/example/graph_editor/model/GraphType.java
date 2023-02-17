package com.example.graph_editor.model;

import com.example.graph_editor.extensions.OnSelectionRepository;
import com.example.graph_editor.extensions.OnSelectionRepositoryImpl;

import java.io.Serializable;
import java.util.Map;
import java.util.function.IntFunction;

import graph_editor.extensions.OnPropertyReaderSelection;
import graph_editor.extensions.StackCapture;
import graph_editor.graph.DirectedGraph;
import graph_editor.graph.GenericGraphBuilder;
import graph_editor.graph.Graph;
import graph_editor.graph.UndirectedGraph;

public enum GraphType {
    DIRECTED("directed"),
    UNDIRECTED("undirected");

    static final Map<GraphType, IntFunction<GenericGraphBuilder<? extends Graph>>> graphBuilderFactoryMap =
            Map.of(DIRECTED, DirectedGraph.Builder::new, UNDIRECTED, UndirectedGraph.Builder::new);

    private static final Map<GraphType, OnSelectionRepository<StackCapture>> captures =
            Map.of(DIRECTED, new OnSelectionRepositoryImpl<>(), UNDIRECTED, new OnSelectionRepositoryImpl<>());
    private static final Map<GraphType, OnSelectionRepository<OnPropertyReaderSelection>> propertyReaders =
            Map.of(DIRECTED, new OnSelectionRepositoryImpl<>(), UNDIRECTED, new OnSelectionRepositoryImpl<>());
    private final String type;

    GraphType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static GraphType getFromString(String type) {
        if (type.equals("directed")) {
            return DIRECTED;
        } else if (type.equals("undirected")) {
            return UNDIRECTED;
        } else {
            throw new IllegalArgumentException("Unknown graph type: " + type);
        }
    }

    public IntFunction<GenericGraphBuilder<? extends Graph>> getGraphBuilderFactory() {
        return graphBuilderFactoryMap.get(this);
    }

    public OnSelectionRepository<StackCapture> getStackCaptureRepository() {
        return captures.get(this);
    }
    public OnSelectionRepository<OnPropertyReaderSelection> getPropertyReaderRepository() {
        return propertyReaders.get(this);
    }
}
