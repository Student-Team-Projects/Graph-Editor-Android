package com.example.graph_editor.draw.graph_view;

import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.graph_editor.draw.ActionModeType;
import com.example.graph_editor.draw.Frame;
import com.example.graph_editor.model.DrawManager;
import com.example.graph_editor.model.Edge;
import com.example.graph_editor.model.Graph;
import com.example.graph_editor.model.Vertex;
import com.example.graph_editor.model.mathematics.Point;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class GraphOnTouchListener implements View.OnTouchListener {

    private final DrawManager manager;
    private final GraphView graphView;
    private final Frame frame;
    private final ScaleGestureDetector scaleDetector;

    // global variables for easier management
    private final Graph graph;

    private Point relativePoint;
    private Point absolutePoint;
    private Vertex highlighted;

    public GraphOnTouchListener(GraphView graphView, DrawManager manager, Frame frame, ScaleGestureDetector scaleDetector) {
        this.graphView = graphView;
        this.manager = manager;
        this.frame = frame;
        this.scaleDetector = scaleDetector;

        this.graph = manager.getGraph();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();

        relativePoint = graphView.getRelative(new Point(event.getX(), event.getY()));
        absolutePoint = manager.getAbsolute(relativePoint);
        highlighted = graphView.highlighted;
        ActionModeType currentType = ActionModeType.getCurrentModeType();

        boolean result;
        switch (currentType) {
            case NEW_VERTEX:
                result = actionNewVertex(v, event);
                break;
            case NEW_EDGE:
                result = actionNewEdge(v, event);
                break;
            case MOVE_OBJECT:
                result = actionMoveObject(v, event);
                break;
            case REMOVE_OBJECT:
                result = actionRemoveObject(v, event);
                break;
            case MOVE_CANVAS:
                result = actionMoveCanvas(v, event);
                break;
            case ZOOM_CANVAS:
            case NONE:
            default:
                result = false;
                break;
        }
        scaleDetector.onTouchEvent(event);

        manager.updateRectangle(graphView.frame.getRectangle());       //TODO: manager update method without frame
        graphView.highlighted = highlighted;
        graphView.postInvalidate();
        return result;
    }

    private boolean actionNewVertex(View v, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                highlighted = graph.addVertex();
                highlighted.setPoint(absolutePoint);
                break;
            case MotionEvent.ACTION_MOVE:
                highlighted.setPoint(absolutePoint);
                break;
            case MotionEvent.ACTION_UP:
                highlighted = null;
                break;
            default:
                break;
        }
        return true;
    }

    private Vertex edgeFirst = null;
    private Vertex newVertex = null;
    private boolean firstAction = true;     // choose first vertex, the choose another
    private boolean actionNewEdge(View v, MotionEvent e) {
        Vertex nearest = manager.getNearestVertex(relativePoint, 0.1, Collections.emptySet());
        Vertex nearestNotNew = manager.getNearestVertex(relativePoint, 0.1, new HashSet<>(Collections.singleton(newVertex)));

        if (firstAction) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (nearest != null) {
                        edgeFirst = nearest;
                    } else {
                        edgeFirst = graph.addVertex();
                        edgeFirst.setPoint(absolutePoint);
                    }
                    highlighted = edgeFirst;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    firstAction = false;
                    break;
            }
            return false;
        } else {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    newVertex = graph.addVertex();
                    newVertex.setPoint(absolutePoint);
                    highlighted = newVertex;
                    manager.getGraph().addEdge(edgeFirst, newVertex);
                    if (nearest != null) {
                        newVertex.setPoint(nearest.getPoint());
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (nearestNotNew != null)
                        newVertex.setPoint(nearestNotNew.getPoint());
                    else
                        newVertex.setPoint(absolutePoint);
                    break;
                case MotionEvent.ACTION_UP:
                    if (nearestNotNew == edgeFirst && nearestNotNew != null) {
                        graph.removeVertex(newVertex);
                    }
                    if (nearestNotNew != edgeFirst && nearestNotNew != null) {
                        graph.removeVertex(newVertex);
                        graph.addEdge(edgeFirst, nearestNotNew);
                    }
                    edgeFirst = null;
                    newVertex = null;
                    highlighted = null;
                    firstAction = true;
                    break;
            }
        }
        return false;
    }

    private boolean actionMoveObject(View v, MotionEvent e) {
        Vertex nearest = manager.getNearestVertex(relativePoint, 0.1, Collections.emptySet());

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (highlighted == null && nearest != null)       // select a vertex
                    highlighted = nearest;
                else if (highlighted == nearest && highlighted != null) // move current vertex slightly
                    highlighted.setPoint(manager.getAbsolute(relativePoint));
                else if (nearest != null)     // select different vertex
                    highlighted = nearest;
                else if (highlighted != null)       // move selected vertex
                    highlighted.setPoint(manager.getAbsolute(relativePoint));
                break;
            case MotionEvent.ACTION_MOVE:
                if (highlighted != null) {
                    highlighted.setPoint(manager.getAbsolute(relativePoint));
                }
                break;
            case MotionEvent.ACTION_UP:
                highlighted = null;
                break;
        }
        return true;
    }

    private boolean actionRemoveObject(View v, MotionEvent e) {
        Edge nearestEdge = manager.getNearestEdge(relativePoint, 0.03);
        Vertex nearestVertex = manager.getNearestVertex(relativePoint, 0.03, Collections.emptySet());

        if (nearestVertex != null)
            graph.removeVertex(nearestVertex);
        if (nearestEdge != null)
            graph.removeEdge(nearestEdge);

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            default:
                break;
        }
        return true;
    }

    private enum Mode {
        NONE,
        DRAG,
        ZOOM
    }
    private float prevX = 0f;
    private float prevY = 0f;
    private float dx = 0f;
    private float dy = 0f;
    private Mode mode = Mode.NONE;
    private boolean actionMoveCanvas(View v, MotionEvent e) {
        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = Mode.DRAG;
                prevX = e.getX();
                prevY = e.getY();
                dx = 0;
                dy = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                dx = e.getX() - prevX;
                dy = e.getY() - prevY;
                prevX = e.getX();
                prevY = e.getY();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                System.out.println("Action down");
                mode = Mode.ZOOM;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
                mode = Mode.NONE;
                break;
        }
        if (mode == Mode.DRAG)
            frame.translate(dx/v.getWidth(), dy/v.getWidth());
        return true;
    }
}
