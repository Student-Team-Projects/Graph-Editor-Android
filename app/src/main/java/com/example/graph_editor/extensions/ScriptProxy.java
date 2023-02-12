package com.example.graph_editor.extensions;

import com.example.graph_editor.draw.graph_action.GraphActionManager;
import graph_editor.extensions.ExtensionInvoker;

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
                stack -> invoker.callFunction(functionCalledOnOptionSelected, stack)
        );
    }

    @Override
    public void deregisterGraphMenuOption(int id) {
        graphMenuManager.deregisterOption(id);
    }

    @Override
    public void customizeVertexDrawingBehaviour(String vertexDrawer) {
        canvasManager.setVertexDrawer((vertex, rectangle, canvas) -> {
            invoker.callFunction(vertexDrawer, vertex, rectangle, canvas);
        });
    }

    @Override
    public void restoreDefaultVertexDrawingBehaviour() {
        canvasManager.setVertexDrawer(null);
    }

    @Override
    public void customizeEdgeDrawingBehaviour(String edgeDrawer) {
        canvasManager.setEdgeDrawer((edge, rectangle, canvas) -> {
            invoker.callFunction(edgeDrawer, edge, rectangle, canvas);
        });
    }

    @Override
    public void restoreDefaultEdgeDrawingBehaviour() {
        canvasManager.setEdgeDrawer(null);
    }

    //TODO reimplement
    @Override
    public int registerGraphAction(String imageButtonPath, String functionCalled) {
        throw new UnsupportedOperationException();
//        return graphActionManager.registerAction(imageButtonPath, (mapper, event, stack) -> {
//            invoker.callFunction(functionCalled, mapper, event, stack);
//            return null;
//        });
    }

    @Override
    public void deregisterGraphAction(int id) {
        graphActionManager.deregisterAction(id);
    }
}
