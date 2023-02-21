package com.example.graph_editor.extensions;

import java.util.HashMap;
import java.util.Map;

import graph_editor.extensions.OnPropertyReaderSelection;
import graph_editor.extensions.Plugin;
import graph_editor.extensions.StackCapture;

public class PluginsProxyImpl implements Plugin.Proxy {
    GenericProxy<StackCapture> captureProxy;
    GenericProxy<OnPropertyReaderSelection> readerProxy;
    public PluginsProxyImpl(
            OnSelectionRepository<StackCapture> captureRepositoryDirected,
            OnSelectionRepository<StackCapture> captureRepositoryUndirected,
            OnSelectionRepository<OnPropertyReaderSelection> readingRepositoryDirected,
            OnSelectionRepository<OnPropertyReaderSelection> readingRepositoryUndirected) {
        this.captureProxy = new GenericProxy<>(captureRepositoryDirected, captureRepositoryUndirected);
        this.readerProxy = new GenericProxy<>(readingRepositoryDirected, readingRepositoryUndirected);
    }

    @Override
    public boolean registerStackCapture(Plugin plugin, String s, StackCapture stackCapture) {
        return captureProxy.register(plugin, s, stackCapture);
    }

    @Override
    public boolean unregisterStackCapture(Plugin plugin, String s) {
        return captureProxy.unregister(plugin, s);
    }

    @Override
    public boolean registerDeclaredPropertiesReader(Plugin plugin, String s, OnPropertyReaderSelection onPropertyReaderSelection) {
        return readerProxy.register(plugin, s, onPropertyReaderSelection);
    }

    @Override
    public boolean unregisterDeclaredPropertiesReader(Plugin plugin, String s) {
        return readerProxy.unregister(plugin, s);
    }

    @Override
    public void releasePluginResources(Plugin plugin) {
        captureProxy.releasePluginResources(plugin);
        readerProxy.releasePluginResources(plugin);
    }

    private static class GenericProxy<T> {
        private final OnSelectionRepository<T> directed;
        private final OnSelectionRepository<T> undirected;
        private final Map<Plugin, Map<String, Integer>> obtainedDirected = new HashMap<>();
        private final Map<Plugin, Map<String, Integer>> obtainedUndirected = new HashMap<>();

        private GenericProxy(OnSelectionRepository<T> directed, OnSelectionRepository<T> undirected) {
            this.directed = directed;
            this.undirected = undirected;
        }

        boolean register(Plugin plugin, String name, T onOptionSelection) {
            boolean added = false;
            if (plugin.supportsDirectedGraphs()) {
                added = added || registerOnSelection(plugin, name, onOptionSelection, directed, obtainedDirected);
            }
            if (plugin.supportsUndirectedGraphs()) {
                added = added || registerOnSelection(plugin, name, onOptionSelection, undirected, obtainedUndirected);
            }
            return added;
        }
        boolean unregister(Plugin plugin, String name) {
            boolean removed = unregisterOnSelection(plugin, name, directed, obtainedDirected);
            removed = removed || unregisterOnSelection(plugin, name, undirected, obtainedUndirected);
            return removed;
        }

        public void releasePluginResources(Plugin plugin) {
            Map<String, Integer> options = obtainedDirected.remove(plugin);
            if (options != null) {
                options.forEach((name, id) -> directed.deregisterOption(id));
            }
            Map<String, Integer> options2 = obtainedUndirected.remove(plugin);
            if (options2 != null) {
                options2.forEach((name, id) -> undirected.deregisterOption(id));
            }
        }

        private boolean registerOnSelection(
                Plugin plugin,
                String name,
                T onOptionSelection,
                OnSelectionRepository<T> repository,
                Map<Plugin, Map<String, Integer>> obtainedOptions
        ) {
            Map<String, Integer> options = obtainedOptions.get(plugin);
            if (options != null && options.get(name) != null) {
                return false;
            }
            if (options == null) {
                options = new HashMap<>();
                obtainedOptions.put(plugin, options);
            }
            int id = repository.registerOption(name, onOptionSelection);
            options.put(name, id);
            return true;
        }
        private boolean unregisterOnSelection(
                Plugin plugin,
                String name,
                OnSelectionRepository<T> repository,
                Map<Plugin, Map<String, Integer>> obtainedOptions
        ) {
            Map<String, Integer> options = obtainedOptions.get(plugin);
            if (options != null) {
                Integer id = options.remove(name);
                if (id != null) {
                    repository.deregisterOption(id);
                    return true;
                }
            }
            return false;
        }
    }
}
