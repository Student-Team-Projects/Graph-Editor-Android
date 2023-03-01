package com.example.graph_editor.draw;

import static graph_editor.visual.PlanarGraphVisualizer.PlanarAlgorithm.findPlanarArrange;
import static graph_editor.visual.PlanarGraphVisualizer.PlanarAlgorithm.fixArrangingWithForces;
import static graph_editor.visual.PlanarGraphVisualizer.PlanarAlgorithm.fixPlanarArranging;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;

import com.example.graph_editor.R;
import com.example.graph_editor.draw.graph_view.GraphView;
import com.example.graph_editor.draw.popups.GeneratePopup;
import com.example.graph_editor.draw.popups.ReaderPopup;
import com.example.graph_editor.draw.popups.SavePopup;
import com.example.graph_editor.model.GraphType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import graph_editor.extensions.Extension;
import graph_editor.extensions.ExtensionsRepository;
import graph_editor.extensions.OnPropertyReaderSelection;
import graph_editor.extensions.StackCapture;
import graph_editor.graph.Graph;
import graph_editor.graph.VersionStack;
import graph_editor.graph_generators.GraphGenerator;
import graph_editor.properties.PropertyGraphBuilder;
import graph_editor.properties.PropertySupportingGraph;
import graph_editor.visual.BuilderVisualizer;
import graph_editor.visual.GraphVisualization;
import graph_editor.visual.PlanarGraphVisualizer;

public class OptionsHandler {
    @SuppressLint("NonConstantResourceId")
    public static boolean handle(@NonNull MenuItem item, DrawActivity context, VersionStack<GraphVisualization<PropertySupportingGraph>> stack,
                                 GraphView graphView, Runnable makeSave,
                                 ActivityResultLauncher<Intent> importActivityResultLauncher,
                                 ActivityResultLauncher<Intent> exportActivityResultLauncher,
                                 Map<Integer, StackCapture> extensionsOptions,
                                 Map<Integer, OnPropertyReaderSelection> readersOptions,
                                 ExtensionsRepository repository,
                                 Map<Integer, Supplier<GraphGenerator<? extends Graph>>> graphGenerators,
                                 GraphType type) {
        if (graphGenerators.containsKey(item.getItemId())) {
            Supplier<GraphGenerator<? extends Graph>> generatorSupplier = Objects.requireNonNull(
                    graphGenerators.get(item.getItemId()));
            new GeneratePopup(context, stack, generatorSupplier.get(), type).show();
                return true;
        }
        if (extensionsOptions.containsKey(item.getItemId())) {
            Objects
                    .requireNonNull(extensionsOptions.get(item.getItemId()))
                    .handle(stack);
            return true;
        }
        if (readersOptions.containsKey(item.getItemId())) {
            Predicate<Extension> filter = type == GraphType.UNDIRECTED ? Extension::supportsUndirectedGraphs : Extension::supportsDirectedGraphs;
            Set<String> names = new HashSet<>();
            repository
                    .getExtensions()
                    .stream()
                    .filter(filter)
                    .map(Extension::usedDrawablesNames)
                    .forEach(i -> i.forEach(names::add));
            List<OnPropertyReaderSelection.SettingChoice> choices =
                    Objects.requireNonNull(readersOptions.get(item.getItemId())).handle(List.copyOf(names));
            new ReaderPopup(context, choices, graphView::postInvalidate).show();
            return true;
        }
        GraphVisualization<PropertySupportingGraph> visualization;
        switch (item.getItemId()) {
            case R.id.options_btn_save:
                makeSave.run();
                return true;
            case R.id.options_btn_redo:
                stack.redo();
                graphView.postInvalidate();
                return true;
            case R.id.options_btn_undo:
                stack.undo();
                graphView.postInvalidate();
                return true;
            //more actions
            case R.id.options_btn_clear:
                PropertySupportingGraph emptyGraph = new PropertyGraphBuilder(type.getGraphBuilderFactory().apply(0)).build();
                visualization = new BuilderVisualizer().generateVisual(emptyGraph);
                stack.push(visualization);
                graphView.postInvalidate();
                return true;
            case R.id.options_btn_arrange:
                visualization = new PlanarGraphVisualizer().generateVisual(stack.getCurrent().getGraph() , stack.getCurrent(), fixArrangingWithForces);
                stack.push(visualization);
                graphView.postInvalidate();
                return true;
            case R.id.options_btn_planar_arrange:
                visualization = new PlanarGraphVisualizer().generateVisual(stack.getCurrent().getGraph() , stack.getCurrent(), fixPlanarArranging);
                stack.push(visualization);
                graphView.postInvalidate();
                return true;
            case R.id.options_btn_makePlanar:
                visualization = new PlanarGraphVisualizer().generateVisual(stack.getCurrent().getGraph() , stack.getCurrent(), findPlanarArrange);
                stack.push(visualization);
                graphView.postInvalidate();
                return true;

//            case R.id.options_btn_normalize:
//                stateStack.backup();
//                State state = stateStack.getCurrentState();
//                DrawManager.normalizeGraph(state.getGraph());
//                Rectangle newRectangle = DrawManager.getOptimalRectangle(state.getGraph(), 0.1, state.getRectangle());
//                state.setRectangle(new Rectangle(newRectangle, 1.2));
//                graphView.postInvalidate();
//                return true;

            //TODO reimplement
//            case R.id.options_btn_recenter:
//                visualization = stack.getCurrent();
//                Rectangle newRectangle1 = DrawManager.getOptimalRectangle(visualization.getVisualization(), visualization.getGraph(), 0.1, state.getRectangle());
//                state.setRectangle(newRectangle1);
//                graphView.postInvalidate();
//                return true;

//            case R.id.options_btn_settings:
//                new SettingsPopup(context, graphView::postInvalidate).show();
//                return true;
            case R.id.options_btn_save_as:
                new SavePopup().show(stack.getCurrent(), type, context, ()->{});
                return true;
//            case R.id.options_btn_export_txt:
//                new ShareAsTxtIntent(context, stack.getCurrent()).show();
//                return true;
//            case R.id.options_btn_import_txt:
//                new ImportFromTxtPopup(context, stack, state).show();
//                return true;
            case R.id.options_btn_export_file:
                Intent exportAsFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportAsFileIntent.setType("text/plain");
                exportAsFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportAsFileIntent = Intent.createChooser(exportAsFileIntent, "Choose where to save the graph");
                exportActivityResultLauncher.launch(exportAsFileIntent);
                return true;
            case R.id.options_btn_import_file:
                Intent importFromFileIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                importFromFileIntent.setType("text/plain");

                importFromFileIntent = Intent.createChooser(importFromFileIntent, "Choose file containing a graph");
                importActivityResultLauncher.launch(importFromFileIntent);
                return true;
            default:
                return false;
        }
    }
}
