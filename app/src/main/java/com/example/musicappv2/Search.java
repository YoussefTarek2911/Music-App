package com.example.musicappv2;

import android.content.Intent;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Search extends AppCompatActivity implements MyAdapter.OnItemClickListener {

    private StorageReference storageReference;
    private MyAdapter adapter;
    private MediaPlayer mediaPlayer;
    private boolean isKeyboardVisible = false;
    SearchView searchView;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_search);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
                return true;
            } else if (id == R.id.navigation_search) {
                displayAllSongs();
                return true;
            } else if (id == R.id.navigation_shazam) {
                Intent intent = new Intent(getApplicationContext(), detetct_Activity.class);
                startActivityWithTransition(intent, false);
                finish();
                return true;
            }

            return false;
        });

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        searchView = findViewById(R.id.searchView);

        // Initialize Firebase Storage
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("AllSongs");
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MyAdapter(this);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        displayAllSongs();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return false;
            }
        });

        ViewGroup viewGroup = findViewById(android.R.id.content);
        viewGroup.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Rect r = new Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();

                int keypadHeight = screenHeight - r.bottom;
                if (keypadHeight > screenHeight * 0.15) {

                    if (!isKeyboardVisible) {
                        isKeyboardVisible = true;
                        hideBottomNavigationView();
                    }
                } else {

                    if (isKeyboardVisible) {
                        isKeyboardVisible = false;
                        showBottomNavigationView();
                    }
                }
            }
        });

        bottomNavigationView.setTransitionName("bottom_navigation");
    }
    private void displayAllSongs() {
        storageReference.listAll()
                .addOnSuccessListener(listResult -> {
                    List<String> allSongs = new ArrayList<>();
                    for (StorageReference item : listResult.getItems()) {
                        allSongs.add(item.getName());
                    }
                    adapter.setItems(allSongs);
                })
                .addOnFailureListener(e -> {

                });
    }


    private void hideBottomNavigationView() {
        bottomNavigationView.setVisibility(View.GONE);
    }

    private void showBottomNavigationView() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void startActivityWithTransition(Intent intent, boolean isToHome) {
        startActivity(intent);
        if (!isToHome) {
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        } else {
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
    }

    private void performSearch(String searchText) {
        if (TextUtils.isEmpty(searchText)) {
            adapter.clear();
            return;
        }

        String[] searchWords = searchText.toLowerCase().split(" ");

        storageReference.listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                if (task.isSuccessful()) {
                    List<String> searchResults = new ArrayList<>();
                    for (StorageReference fileReference : task.getResult().getItems()) {
                        String itemName = fileReference.getName().toLowerCase();

                        // Check if all search words are present in the item name
                        boolean isMatch = true;
                        for (String word : searchWords) {
                            if (!itemName.contains(word)) {
                                isMatch = false;
                                break;
                            }
                        }

                        if (isMatch) {
                            searchResults.add(fileReference.getName());
                        }
                    }
                    adapter.setItems(searchResults);
                } else {

                }
            }
        });
    }

    private void stopSong() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            ModelRecycleView.nowPlaying = null;
        }
    }

    private void playSong(String itemName) {
        mediaPlayer = ModelRecycleView.nowPlaying;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopSong();
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        storageReference.child(itemName).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                try {
                    mediaPlayer.setDataSource(uri.toString());
                    ModelRecycleView.nowPlaying = mediaPlayer;
                    mediaPlayer.prepareAsync();
                    DisplaySongs.smallPlayer.setVisibility(View.VISIBLE);
                    DisplaySongs.songPlay.setText(itemName);
                    DisplaySongs.buttonMain.setImageResource(R.drawable.ic_pause);
                    DisplaySongs.paused = false;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopSong();
            }
        });
    }

    @Override
    public void onItemClick(String itemName) {

            playSong(itemName);

    }


}
