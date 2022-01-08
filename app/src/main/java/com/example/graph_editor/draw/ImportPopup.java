package com.example.graph_editor.draw;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.graph_editor.R;
import com.example.graph_editor.graph_storage.GraphScanner;
import com.example.graph_editor.graph_storage.InvalidGraphStringException;
import com.example.graph_editor.model.DrawManager;
import com.example.graph_editor.model.Graph;
import com.example.graph_editor.model.InputSanitizer;
import com.example.graph_editor.model.graph_generators.GraphGenerator;
import com.example.graph_editor.model.graph_generators.Parameter;
import com.example.graph_editor.model.mathematics.Rectangle;
import com.example.graph_editor.model.state.State;
import com.example.graph_editor.model.state.UndoRedoStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ImportPopup {
    private final Context context;
    private final UndoRedoStack stateStack;

    private AlertDialog dialog;

    ImportPopup(Context context, UndoRedoStack stateStack) {
        this.context = context;
        this.stateStack = stateStack;
    }

    public void show() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater)context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.import_popup, null);

        EditText input = popupView.findViewById(R.id.import_popup_input);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        String pasteData = "";

        // If it does contain data, decide if you can handle the data.
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN)) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            pasteData = item.getText().toString();
        }
        input.setText(pasteData);


        popupView.findViewById(R.id.import_popup_confirm).setOnClickListener(v -> {
            Graph g;
            try {
                g = GraphScanner.fromExact(input.getText().toString());
            } catch (InvalidGraphStringException e) {
                Toast.makeText(context, "Invalid graph", Toast.LENGTH_SHORT).show();
                return;
            }
            Rectangle oldRec = stateStack.getCurrentState().getFrame().getRectangle();
            Rectangle optimalRec = DrawManager.getOptimalRectangle(g, 0.1, oldRec);
            State currentState = stateStack.getCurrentState();
            currentState.setGraph(g);
            currentState.setFrame(new Frame(optimalRec, optimalRec.getWidth()));
            stateStack.invalidateView();

            Toast.makeText(context, "Import complete", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setView(popupView);
        dialog = builder.create();
        dialog.show();
    }
}
