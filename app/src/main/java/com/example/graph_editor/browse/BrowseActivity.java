package com.example.graph_editor.browse;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.example.graph_editor.R;
import com.example.graph_editor.database.Save;
import com.example.graph_editor.database.SavesDatabase;
import com.example.graph_editor.draw.DrawActivity;

import java.util.Arrays;

public class BrowseActivity extends AppCompatActivity{
    RecyclerView saved;
    SavesDatabase database;
    SavedAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        saved = findViewById(R.id.saved);
        database = SavesDatabase.getDbInstance(getApplicationContext());
        adapter = new SavedAdapter(this,
                database.saveDao().getAllScores(), this);
        saved.setAdapter(adapter);
        saved.setLayoutManager(new LinearLayoutManager(this));

//        changeActivity("UNDIRECTED\n3 2\n0 0.0 0.0\n1 0.0 1.0\n2 1.0 1.0\n0 1\n1 2\n");
    }

    public void changeActivity(String graphString) {
        SharedPreferences sharedPref = this.getSharedPreferences("GLOBAL", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("currentGraph", graphString);
        editor.apply();

        Intent intent = new Intent(this, DrawActivity.class);
        startActivity(intent);
        finish();
    }
}