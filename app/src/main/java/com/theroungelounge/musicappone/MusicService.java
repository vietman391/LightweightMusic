package com.theroungelounge.musicappone;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by Rounge on 6/2/2016.
 */
public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener {

    public static String SONG_PLAYING_TAG = "SONG_PLAYING_TAG";

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songs;
    //current position
    private int songIndex;
    private final IBinder musicBind = new MusicBinder(); //Represents the MusicBinder binder class
    private String songTitle = ""; //
    private String songArtist = "";
    private int songLength = 0;
    private static final int NOTIFY_ID = 1;
    private boolean shuffle = false;
    private Uri contentUri;

    public void onCreate() {
        //create the service
        super.onCreate();
        //initialize position
        songIndex = 0;
        //create player
        player = new MediaPlayer();
        //initializes the player
        initMusicPlayer();
        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    public void initMusicPlayer() {
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
    }

    /**
     * Takes an ArrayList of Songs from the MainActivity and assigns it to the ArrayList of Songs
     * in MusicService.
     *
     * @param theSongs list of songs passed by the MainActivity
     */
    public void setList(ArrayList<Song> theSongs) {
        songs = new ArrayList<Song>();
        songs.addAll(theSongs);
    }

    public void playSong() {
        player.reset();
        //get song
        Song playSong = songs.get(songIndex);
        songTitle = playSong.getTitle();
        songArtist = playSong.getArtist();
        songLength = playSong.getLength();
        String songInfo = songArtist + " - " + songTitle;
        //get id
        long currSong = playSong.getId();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                contentUri,
                currSong);
        try {
            player.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
        sendBroadcast(new Intent(SONG_PLAYING_TAG));
    }

    public void setContentUri(Uri uri) {
        contentUri = uri;
    }

    public void setSong(int songIndex) {
        this.songIndex = songIndex;
    }

    /**
     * Last update: 6/8/2016 by Rounge
     * Shuffles songs every time the "Shuffle" overflow
     * item is clicked
     */

    @Override
    public void onDestroy() {
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    /**
     * Binds the MusicService to the MainActivity
     * Method is accessed from the MainActivity using
     * a MusicBinder object.
     */
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    /**
     * Stops the music player when the MusicService
     * becomes unbound to the MainActivity, normally
     * when the user exits the app.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        player.stop();
        player.release();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (player.getCurrentPosition() > 0) {
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    /**
     * Starts the MediaPlayer, and sets the notification
     * outside the app to indicate to the user that a Song
     * is playing.
     *
     * @param mp The MediaPlayer used to play Songs.
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.play)
                .setTicker(songTitle)
                .setOngoing(true)
                .setContentTitle(songTitle) // Changed from "Playing"
                .setContentText(songArtist)
                .setContentInfo(SongAdapter.formatSongLength(songLength));
        Notification not = builder.build();

        startForeground(NOTIFY_ID, not);
    }

    /**
     * @return the current song playing or queued
     */
    public int getSongIndex() {
        return songIndex;
    }

    /**
     * @return the current position that the current song is at
     */
    public int getPosition() {
        return player.getCurrentPosition();
    }

    public int getDur() {
        return player.getDuration();
    }

    public boolean isPng() {
        return player.isPlaying();
    }

    public void pausePlayer() {
        player.pause();
    }

    public void seek(int posn) {
        player.seekTo(posn);
    }

    public void go() {
        player.start();
    }

    /**
     * Plays the previous song in the ordered or shuffled list.
     * If the current song is the first song in the list, then
     * the method will play the song that is last in the list.
     */
    public void playPrev() {
        songIndex--;
        if (songIndex < 0)
            songIndex = songs.size() - 1;
        playSong();
    }

    //skip to next
    public void playNext() {
        songIndex++;
        if (songIndex >= songs.size())
            songIndex = 0;
        playSong();
    }
}