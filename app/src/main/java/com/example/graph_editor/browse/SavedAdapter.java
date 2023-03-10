package com.example.graph_editor.browse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.graph_editor.R;
import com.example.graph_editor.android_view_wrappers.ViewWrapper;
import com.example.graph_editor.draw.graph_view.GraphView;
import com.example.graph_editor.draw.popups.ShareAsIntent;
import com.example.graph_editor.extensions.PluginsDrawerSource;
import com.example.graph_editor.extensions.StaticState;
import com.example.graph_editor.file_serialization.FileData;
import com.example.graph_editor.file_serialization.Loader;
import com.example.graph_editor.fs.FSDirectories;

import java.io.File;
import java.util.List;

import graph_editor.draw.point_mapping.PointMapperImpl;
import graph_editor.geometry.Point;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.Holder> {
    private final List<String> data;
    private final BrowseActivity browseActivity;

    SavedAdapter(BrowseActivity browseActivity, List<String> data) {
        this.data = data;
        this.browseActivity = browseActivity;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(browseActivity);
        View view = inflater.inflate(R.layout.saved_row, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        String name = data.get(position);
        holder.txtName.setText(name);

        FileData fileData = Loader.load(new File(browseActivity.getFilesDir(), FSDirectories.graphsDirectory), name);

        holder.dataGraph.initialize(
                new PluginsDrawerSource(StaticState.getExtensionsRepositoryInstance(browseActivity), fileData.type),
                new PointMapperImpl(new ViewWrapper(holder.dataGraph), new Point(0,0)),
                fileData.type,
                fileData.visualization
        );

        holder.editButton.setOnClickListener(v -> {
            browseActivity.changeActivity(name);
        });
        holder.deleteButton.setOnClickListener(v ->
                new ConfirmPopup(browseActivity, holder.dataGraph.getVisualization(), holder.dataGraph.getGraphType(), () -> {
                    data.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, data.size());
                    File graphsDirectory = new File(browseActivity.getFilesDir(), FSDirectories.graphsDirectory);
                    boolean deleted = new File(graphsDirectory, name).delete();
                    if (!deleted) { throw new RuntimeException("worrisome: delete did not happen"); }
                }).show()
        );
        holder.shareButton.setOnClickListener(
                v -> new ShareAsIntent(browseActivity, holder.dataGraph.getVisualization()).show());
    }
    @Override
    public int getItemCount() {
        return data.size();
    }
    public List<String> getData() {
        return data;
    }

    public static class Holder extends RecyclerView.ViewHolder {
        TextView txtName;
        GraphView dataGraph;
        ImageButton editButton;
        ImageButton deleteButton;
        ImageButton shareButton;

        public Holder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtName);
            dataGraph = itemView.findViewById(R.id.dataGraph);
            editButton = itemView.findViewById(R.id.btnEdit);
            deleteButton = itemView.findViewById(R.id.btnDelete);
            shareButton = itemView.findViewById(R.id.btnShare);
        }
    }
}
