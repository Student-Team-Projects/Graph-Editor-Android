package com.example.graph_editor.browse;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.graph_editor.R;
import com.example.graph_editor.database.SavesDatabase;
import com.example.graph_editor.draw.DrawActivity;

import java.util.HashSet;
import java.util.List;

public class BrowseActivity extends AppCompatActivity {
    RecyclerView saved;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        saved = findViewById(R.id.saved);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SavesDatabase database = SavesDatabase.getDbInstance(getApplicationContext());
        SavedAdapter adapter = new SavedAdapter(this, database.saveDao().getAllSaves(),
                database.propertySaveDao().getAllPropertySaves(), this);
        saved.setAdapter(adapter);
        saved.setLayoutManager(new LinearLayoutManager(this));

        TextView noSavedGraphs = findViewById(R.id.txt_no_saved_graphs);
        if (adapter.getData().size() > 0) noSavedGraphs.setAlpha(0f);
    }

    public void changeActivity(String graphString, long graphId, List<String> propertyStrings) {
        SharedPreferences sharedPref = this.getSharedPreferences("GLOBAL", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("currentGraph", graphString);
        editor.putLong("currentGraphId", graphId);
        editor.putStringSet("currentGraphProperties", new HashSet<>(propertyStrings));
        editor.apply();

        Intent intent = new Intent(this, DrawActivity.class);
        startActivity(intent);
    }
}