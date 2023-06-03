package com.example.musicappv2;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import android.widget.ImageView;
import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.tasks.OnSuccessListener;

public class SongPlayer extends AppCompatActivity {
    private ImageButton pause, previous, next;
    private ImageView musicLogo;
    private static MediaPlayer mediaPlayer;

    private TextView songInfo, currentDuration, totalDuration;
    private SharedPreferences sp;
    private String name;

    private SeekBar seekBar;

    private boolean isResume;

    Runnable runnable;
    Handler handler;

    private int nextSongIndex;

    public static ArrayList<MediaPlayer> arrayList=new ArrayList<>();
    public static ArrayList<String> trackNames = new ArrayList<>();
    private int currentPosition; // Index of the current song in the track list


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        songInfo = findViewById(R.id.songInfo);
        pause = findViewById(R.id.pauseButton);
        previous = findViewById(R.id.previousButton);
        next = findViewById(R.id.nextButton);
        currentDuration = findViewById(R.id.currentDuration);
        totalDuration = findViewById(R.id.totalDuration);

        musicLogo = findViewById(R.id.musicLogo);

        seekBar = findViewById(R.id.seekBarId);
        handler = new Handler();

        songInfo.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        songInfo.setSelected(true);

        setPlayer();

        //setNotification();

        //Rotation property of the musicLogo
        ObjectAnimator rotationAnimator = ObjectAnimator.ofFloat(musicLogo, View.ROTATION, 0f, 360f);
        rotationAnimator.setDuration(2000); // Duration of rotation
        rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE); // Set to repeat infinitely
        rotationAnimator.setInterpolator(new LinearInterpolator());

        // Start the rotation animation
        rotationAnimator.start();


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress);
                    seekBar.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sp = getApplicationContext().getSharedPreferences("songPref", Context.MODE_PRIVATE);

        if (sp != null) {
            name = sp.getString("name", "");
            currentPosition = sp.getInt("currentPosition", -1);
            nextSongIndex = currentPosition;
        } else {
            //Error Handling
        }

        songInfo.setText(name);

        pause.setOnClickListener(v -> {
            if (isResume) {
                isResume = false;

                if (mediaPlayer == null) {
                    Toast.makeText(this, "Media player is null", Toast.LENGTH_SHORT).show();
                } else {
                    mediaPlayer.start();
                    updateSeekbar();
                }

                pause.setImageDrawable(getResources().getDrawable((
                        R.drawable.baseline_pause_24
                )));

                // Resume the rotation animation
                rotationAnimator.resume();
            } else {
                isResume = true;

                if (mediaPlayer == null) {
                    Toast.makeText(this, "Media player is null", Toast.LENGTH_SHORT).show();
                } else {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                    }
                }

                pause.setImageDrawable(getResources().getDrawable(
                        R.drawable.baseline_play_arrow_1
                ));

                // Pause the rotation animation
                rotationAnimator.pause();
            }
        });


        //fix updateseekbar position
        next.setOnClickListener(view -> {
            if ((nextSongIndex < arrayList.size() - 1)) {

                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                nextSongIndex++;
                MediaPlayer nextSong = arrayList.get(nextSongIndex);
                name = trackNames.get(nextSongIndex);

                ModelRecycleView.nowPlaying = nextSong;
                //mediaPlayer = nextSong;
                mediaPlayer = ModelRecycleView.nowPlaying;


                //fix updateseekbar position
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        songInfo.setText(name);
                        updateSeekbar();
                    }
                });

                mediaPlayer.prepareAsync();
            } else {
                Toast.makeText(this, "End of Playlist Reached", Toast.LENGTH_SHORT).show();
            }
        });


        previous.setOnClickListener(view -> {

            if (nextSongIndex >= 0 || arrayList.size() == 1) {

                if (mediaPlayer != null && mediaPlayer.isPlaying())
                    mediaPlayer.stop();

                if (nextSongIndex != 0)
                    nextSongIndex--;

                MediaPlayer prevSong = arrayList.get(nextSongIndex);
                name = trackNames.get(nextSongIndex);


                ModelRecycleView.nowPlaying = prevSong;
                mediaPlayer = ModelRecycleView.nowPlaying;

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mediaPlayer) {
                        mediaPlayer.start();
                        songInfo.setText(name);
                        updateSeekbar();
                    }
                });

                mediaPlayer.prepareAsync();
            } else {
                Toast.makeText(this, "Beginning of Playlist Reached", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void setNotification() {
        // Request notification permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API level 26 and above
            NotificationChannel channel = new NotificationChannel("channel_id", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channel_id")
                .setSmallIcon(R.drawable.baseline_album_24)
                .setContentTitle("Music App")
                .setContentText(DisplaySongs.songName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void setPlayer()
    {
        mediaPlayer = ModelRecycleView.nowPlaying;
        seekBar.setMax(mediaPlayer.getDuration());
        seekBar.setSecondaryProgress(mediaPlayer.getDuration());
        updateSeekbar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DisplaySongs.songPlay.setText(name);
    }

    public void updateSeekbar() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int currPos = mediaPlayer.getCurrentPosition();
            int totalTime = mediaPlayer.getDuration();

            // Calculate the progress ratio
            int progress = (int) (((float) currPos / totalTime) * seekBar.getMax());

            // Update the SeekBar progress
            seekBar.setProgress(progress);

            // Schedule the next update
            //handler.postDelayed(this::updateSeekbar, 100);

            // Update the song info text
            int seconds = currPos / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;

            //Total Duration
            int secondsTotal = totalTime / 1000;
            int minutesTotal = secondsTotal / 60;
            secondsTotal = secondsTotal % 60;


            String currentTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
            String totalTimeString = String.format(Locale.getDefault(), "%02d:%02d", minutesTotal, secondsTotal);

            currentDuration.setText(currentTime);
            totalDuration.setText(totalTimeString);


            runnable = new Runnable() {
                @Override
                public void run() {
                    updateSeekbar();
                }
            };
            handler.postDelayed(runnable, 1000);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        updateSeekbar();
    }


    public static void stopMusicPlayer() {
        if (ModelRecycleView.nowPlaying != null && ModelRecycleView.nowPlaying.isPlaying()) {
            ModelRecycleView.nowPlaying.stop();
            ModelRecycleView.nowPlaying.release();
            ModelRecycleView.nowPlaying = null;
        }
    }


}