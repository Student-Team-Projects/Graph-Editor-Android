package com.example.graph_editor.extentions;

import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import com.example.graph_editor.draw.action_mode_type.GraphAction;
import com.example.graph_editor.draw.graph_view.GraphOnTouchListenerData;
import com.example.graph_editor.draw.graph_view.GraphView;
import com.example.graph_editor.model.extensions.CanvasManager;
import com.example.graph_editor.model.extensions.GraphActionManager;
import com.example.graph_editor.model.extensions.GraphMenuManager;
import com.example.graph_editor.model.extensions.ExtensionInvoker;
import com.example.graph_editor.model.state.StateStack;

public class ScriptProxy implements ExtensionInvoker.ExtensionProxy {
    private final ExtensionInvoker invoker;
    private final GraphMenuManager graphMenuManager;
    private final CanvasManager canvasManager;
    private final GraphActionManager graphActionManager;
    public ScriptProxy(
            ExtensionInvoker invoker,
            GraphMenuManager graphMenuManager,
            CanvasManager canvasManager, GraphActionManager graphActionManager) {
        this.invoker = invoker;
        this.graphMenuManager = graphMenuManager;
        this.canvasManager = canvasManager;
        this.graphActionManager = graphActionManager;
    }

    @Override
    public int registerGraphMenuOption(String optionName, String functionCalledOnOptionSelected) {
        return graphMenuManager.registerOption(
                optionName,
                arg -> invoker.callFunction(functionCalledOnOptionSelected, arg)
        );
    }

    @Override
    public void deregisterGraphMenuOption(int id) {
        graphMenuManager.deregisterOption(id);
    }

    @Override
    public void customizeVertexDrawingBehaviour(String vertexDrawer) {
        canvasManager.setVertexDrawer((point, rectangle, canvas) -> {
            invoker.callFunction(vertexDrawer, point, rectangle, canvas);
        });
    }

    @Override
    public void restoreDefaultVertexDrawingBehaviour() {
        canvasManager.setVertexDrawer(null);
    }

    @Override
    public void customizeEdgeDrawingBehaviour(String edgeDrawer) {
        canvasManager.setEdgeDrawer((p1, p2, rectangle, canvas) -> {
            invoker.callFunction(edgeDrawer, p1, p2, rectangle, canvas);
        });
    }

    @Override
    public void restoreDefaultEdgeDrawingBehaviour() {
        canvasManager.setEdgeDrawer(null);
    }

    @Override
    public int registerGraphAction(String imageButtonPath, String functionCalled) {
        return graphActionManager.registerOption(imageButtonPath, (v, event, stateStack, data, view) -> {
            invoker.callFunction(functionCalled, v, event, stateStack, data, view);
            return true; //TODO ok?
        });
    }

    @Override
    public void deregisterGraphAction(int id) {
        graphMenuManager.deregisterOption(id);
    }
}