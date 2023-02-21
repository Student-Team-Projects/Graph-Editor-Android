package com.example.graph_editor.extensions;

import com.example.graph_editor.model.GraphType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import graph_editor.draw.IGraphDrawer;
import graph_editor.draw.point_mapping.CanvasDrawer;
import graph_editor.draw.point_mapping.PointMapper;
import graph_editor.extensions.Extension;
import graph_editor.extensions.ExtensionsRepository;
import graph_editor.extensions.Plugin;
import graph_editor.properties.PropertySupportingGraph;

public class PluginsDrawerSource implements DrawerSource {
    private final ExtensionsRepository repository;
    private final GraphType type;

    private static Map<GraphType, Predicate<Extension>> filters =
            Map.of(GraphType.DIRECTED, Extension::supportsDirectedGraphs,
                    GraphType.UNDIRECTED, Extension::supportsUndirectedGraphs);

    public PluginsDrawerSource(ExtensionsRepository repository, GraphType type) {
        this.repository = repository;
        this.type = type;
    }
    //TODO
    // consider adding setting to select which drawer is used
    // if there would be more than one drawing plugin
    @Override
    public Optional<IGraphDrawer<PropertySupportingGraph>> getDrawer(PointMapper mapper, CanvasDrawer canvasDrawer) {
        List<Iterable<Plugin.Drawer>> available = repository
                .getExtensions()
                .stream()
                .filter(filters.get(type))
                .filter(Extension::isEnabled)
                .map(Extension::getGraphDrawers)
                .collect(Collectors.toList());
        List<Plugin.Drawer> result = new ArrayList<>();
        available.forEach(it -> it.forEach(result::add));
        if (result.size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(result.get(0).getGraphDrawer(mapper, canvasDrawer));
        }
    }
}