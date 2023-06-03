package com.example.musicappv2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.motion.widget.OnSwipe;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.provider.OpenableColumns;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;

public class DisplaySongs extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private ModelRecycleView adapter;
    RecyclerView recyclerView;
    FirebaseAuth auth;
//    Button logoutbutton ;


    public static String songName;
    FirebaseUser user;
    Button uploadButton;
    public static LinearLayout smallPlayer;
    public static TextView songPlay;
    public static ImageButton buttonMain;

    public static SeekBar seekBar;

    public static boolean paused;

    private MediaPlayer nowPlaying;

    public static StorageReference storageReference;

    BottomNavigationView bottomNavigationView;


    LinearLayout mediaPlayerButton;
    ArrayList<MediaPlayer> arrayList=new ArrayList<>();
    ArrayList<String> trackNames = new ArrayList<>();
    ArrayList<Boolean> ispreparedMedia = new ArrayList<>();

    SwipeRefreshLayout swipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_songs);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        swipe = findViewById(R.id.swipeLayout);

        bottomNavigationView.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                return true;
            }   else if (id == R.id.navigation_search) {
                Intent intent = new Intent(this, Search.class);
                startActivityWithTransition(intent);
                return true;
            }   else if (id == R.id.navigation_shazam) {
                Intent intent = new Intent(this, detetct_Activity.class);
                startActivityWithTransition(intent);
                return true;
            }

            return false;
        });

        auth = FirebaseAuth.getInstance();
        //logoutbutton =findViewById(R.id.logoutButton);
        recyclerView =findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ModelRecycleView(this, arrayList, trackNames, ispreparedMedia);
        //recyclerView.setAdapter(adapter);

        smallPlayer = findViewById(R.id.smallPlayer);
        songPlay = findViewById(R.id.songPlay);
        buttonMain = findViewById(R.id.buttonMain);

        mediaPlayerButton = findViewById(R.id.mediaPlayerButton);

        songPlay.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songPlay.setSelected(true);

        paused = true;
        checkPlaying();
        user = auth.getCurrentUser();
        //mlhash lazma
        if(user==null)
        {
            Intent intent =new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }

        mediaPlayerButton.setOnClickListener(v -> {
            SharedPreferences sp = getApplicationContext().getSharedPreferences("songPref", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("name", songPlay.getText().toString());
            editor.apply();
            Intent intent = new Intent(DisplaySongs.this, SongPlayer.class);
            startActivity(intent);
        });

        buttonMain.setOnClickListener(v -> {
            nowPlaying = ModelRecycleView.nowPlaying;
            if (paused) {
                nowPlaying.start();
                buttonMain.setImageResource(R.drawable.ic_pause);
                paused = false;
            } else {
                nowPlaying.pause();
                buttonMain.setImageResource(R.drawable.ic_play);
                paused = true;
            }
        });

        // Create a Cloud Storage reference from the app
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        storageReference = FirebaseStorage.getInstance().getReference().child("Songs/" + userEmail + "/");

        storageReference.listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                if(listResult.getItems().size() == 0)
                    startActivity(new Intent(DisplaySongs.this, Search.class));

                for (StorageReference fileRef : listResult.getItems()) {
                    if(fileRef.getName().equals(".folderMarker")){
                        fileRef.delete();
                        continue;
                    }
                    fileRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String downloadUrl = uri.toString();
                            arrayList.add(setUpMediaPlayer(downloadUrl));
                            trackNames.add(fileRef.getName());
                            ispreparedMedia.add(false);
                        }
                    }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            recyclerView.setAdapter(adapter);
                        }
                    });
                }

            }
        });

        ImageButton popupButton = findViewById(R.id.popup);
        popupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showpopup(v);
            }
        });

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshPlaylist();
                swipe.setRefreshing(false);
            }
        });
    }



    private void refreshPlaylist() {
        arrayList.clear();
        trackNames.clear();

        // Retrieve the updated list of songs from Firebase Storage
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Songs/" + userEmail + "/");

        storageReference.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference fileRef : listResult.getItems()) {
                if (fileRef.getName().equals(".folderMarker")) {
                    fileRef.delete();
                    continue;
                }
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    String fileName = fileRef.getName(); // Get the file name

                    addSongToList(downloadUrl, fileName);

                }).addOnSuccessListener(uri -> {
                    recyclerView.setAdapter(adapter);
                });
            }
        }).addOnFailureListener(e -> {
            Log.e("Refresh Playlist", "Failed to retrieve updated playlist", e);
        });
    }

    // Method to add a song to the playlist
    private void addSongToList(String downloadUrl, String fileName) {
        MediaPlayer mediaPlayer = setUpMediaPlayer(downloadUrl);
        arrayList.add(mediaPlayer);
        trackNames.add(fileName);
    }

    private void startActivityWithTransition(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        nowPlaying = ModelRecycleView.nowPlaying;
        checkPlaying();
    }

    private void uploadSongToFirebaseStorage(Uri fileUri) {
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String fileName = songName;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Songs/" + userEmail + "/" + fileName);

        storageReference.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // File uploaded successfully
                    Toast.makeText(this, "Track uploaded", Toast.LENGTH_SHORT).show();

                    // Get the download URL of the uploaded file
                    storageReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                // Add the song to the RecyclerView
                                String downloadUrl = uri.toString();
                                arrayList.add(setUpMediaPlayer(downloadUrl));

                                trackNames.add(songName);

                                // Notify the adapter of the new song
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                // Handle any errors in getting the download URL
                                Log.e("Upload", "Failed to get download URL", e);
                            });
                })
                .addOnFailureListener(e -> {
                    // Handle any errors in uploading the file
                    Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show();
                });
    }


    private MediaPlayer setUpMediaPlayer(String mp3URL){
        MediaPlayer mediaPlayer = new MediaPlayer();

        mediaPlayer.setAudioAttributes(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
        );

        try {
            mediaPlayer.setDataSource(mp3URL);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return mediaPlayer;
    }

    private void checkPlaying() {
        if (nowPlaying != null) {
            if (nowPlaying.isPlaying()) {
                buttonMain.setImageResource(R.drawable.ic_pause);
                paused = false;
            } else {
                buttonMain.setImageResource(R.drawable.ic_play);
                paused = true;
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Get the selected song URI
            Uri fileUri = data.getData();

             songName = getSongNameFromUri(fileUri);

            // Call the upload function
            uploadSongToFirebaseStorage(fileUri);

            Intent reloadingIntent = new Intent(DisplaySongs.this, Reloading.class);
            startActivity(reloadingIntent);
        }
    }

    private String getSongNameFromUri(Uri uri) {
        String songName = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                songName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return songName;
    }

    public void showpopup(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.uploadButton) {
            // Open a file picker to select a song
            Intent uploadIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            uploadIntent.addCategory(Intent.CATEGORY_OPENABLE);

            uploadIntent.setType("audio/*");
            startActivityForResult(uploadIntent, 1);

            return true;
        } else if (item.getItemId() == R.id.logoutButton) {
            // Handle logout action
            SongPlayer.stopMusicPlayer();
            FirebaseAuth.getInstance().signOut();
            finish();
            return true;
        }
        return false;
    }


    private void playSong(String songName) {

        nowPlaying.reset();
        try {
            // Assuming you have the audio files stored in the "raw" folder
            int resourceId = getResources().getIdentifier(songName, "raw", getPackageName());
            AssetFileDescriptor afd = getResources().openRawResourceFd(resourceId);
            if (afd != null) {
                nowPlaying.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
                nowPlaying.prepare();
                nowPlaying.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}