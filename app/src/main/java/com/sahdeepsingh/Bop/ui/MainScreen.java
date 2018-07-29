package com.sahdeepsingh.Bop.ui;


import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.sahdeepsingh.Bop.R;
import com.sahdeepsingh.Bop.controls.CircularSeekBar;
import com.sahdeepsingh.Bop.controls.MusicController;
import com.sahdeepsingh.Bop.fragments.FragmentAlbum;
import com.sahdeepsingh.Bop.fragments.FragmentGenre;
import com.sahdeepsingh.Bop.fragments.FragmentPlaylist;
import com.sahdeepsingh.Bop.fragments.FragmentSongs;
import com.sahdeepsingh.Bop.notifications.NotificationMusic;
import com.sahdeepsingh.Bop.playerMain.Main;
import com.sahdeepsingh.Bop.playerMain.SingleToast;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.picasso.Picasso;

import java.io.File;


public class MainScreen extends ActivityMaster implements MediaController.MediaPlayerControl, ActionBar.TabListener, FragmentSongs.OnListFragmentInteractionListener, FragmentPlaylist.OnListFragmentInteractionListener, FragmentGenre.OnListFragmentInteractionListener, FragmentAlbum.OnListFragmentInteractionListener {

    public static final String BROADCAST_ACTION = "lol";
    static final int USER_CHANGED_THEME = 1;
    /**
     * How long to wait to disable double-pressing to quit
     */
    private static final int BACK_PRESSED_DELAY = 2000;

    private static final float BLUR_RADIUS = 25f;
    CircularSeekBar circularSeekBar;
    ImageView blurimage, centreimage;
    ImageButton shuffletoggle, previousSong, PlayPause, nextSong, repeatToggle;
    private MusicController musicController;
    public boolean paused = false;
    private boolean playbackPaused = false;


    ChangeSongBR changeSongBR;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v13.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private boolean backPressedOnce = false;
    /**
     * Action that actually disables double-pressing to quit
     */
    private final Runnable backPressedTimeoutAction = new Runnable() {
        @Override
        public void run() {
            backPressedOnce = false;
        }
    };
    private Handler backPressedHandler = new Handler();
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private Toolbar toolbar;
    private TabLayout tabLayout;
    SlidingUpPanelLayout slidingUpPanelLayout;

    /**
     * Adds a new item "Now Playing" on the main menu, if
     * it ain't there yet.
     */
    public static void addNowPlayingItem(Context c) {

        if (Main.mainMenuHasNowPlayingItem)
            return;

        Main.mainMenuHasNowPlayingItem = true;

        // Refresh ListView
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // We need to load the settings right before creating
        // the first activity so that the user-selected theme
        // will be applied to the first screen.
        //
        // Loading default settings at the first time the app;
        // is loaded.
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Main.settings.load(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        slidingUpPanelLayout = findViewById(R.id.sliding_layout);

        Main.initialize(this);

        scanSongs(false);


        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FloatingActionButton floatingActionButton = findViewById(R.id.fab_Playall);

        mViewPager = (ViewPager) findViewById(R.id.container);

        setupViewPager(mViewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        changeSongBR = new ChangeSongBR();


    }

    private void setupViewPager(ViewPager viewPager) {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        viewPager.setAdapter(mSectionsPagerAdapter);
    }


    /**
     * Starts the background process of scanning the songs.
     *
     * @param forceScan If we should scan again. You should set
     *                  this to true if you want to scan again
     *                  the database.
     *                  Otherwise, leave it `false` so we don't
     *                  rescan the songs when this Activity
     *                  is created again for some reason.
     */
    void scanSongs(boolean forceScan) {

        // Loading all the songs from the device on a different thread.
        // We'll only actually do it if they weren't loaded already
        //
        // See the implementation right at the end of this class.
        if ((forceScan) || (!Main.songs.isInitialized())) {

            /*SingleToast.show(MainScreen.this,
                    getString(R.string.menu_main_scanning),
                    Toast.LENGTH_LONG);*/

            new ScanSongs().execute();
        }
    }

    @Override
    public void onListFragmentInteraction(int position, String type) {

        Intent intent = new Intent(this, PlayingNow.class);

        switch (type) {
            case "singleSong":
                Main.musicList.clear();
                Main.musicList.add(Main.songs.songs.get(position));
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("songPosition", position);
                startActivity(intent);


                break;
            case "playlist":
                Main.musicList.clear();
                String selectedPlaylist = Main.songs.playlists.get(position).getName();
                Main.musicList = Main.songs.getSongsByPlaylist(selectedPlaylist);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("playlistName", selectedPlaylist);
                startActivity(intent);

                break;
            case "GenreList":
                Main.musicList.clear();
                String selectedGenre = Main.songs.getGenres().get(position);
                Main.musicList = Main.songs.getSongsByGenre(selectedGenre);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("genreName", selectedGenre);
                startActivity(intent);
                break;
            case "AlbumList":
                Main.musicList.clear();
                String selectedAlbum = Main.songs.getAlbums().get(position);
                Main.musicList = Main.songs.getSongsByAlbum(selectedAlbum);
                Main.nowPlayingList = Main.musicList;
                intent.putExtra("albumName", selectedAlbum);
                startActivity(intent);
                break;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_screen, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            // Let's start the settings screen.
            // While doing so, we need to know if the user have
            // changed the theme.
            // If he did, we'll refresh the screen.
            // See `onActivityResult()`
            Intent settingsIntent = new Intent(this, ActivityMenuSettings.class);
            startActivityForResult(settingsIntent, USER_CHANGED_THEME);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Activity is about to become visible - let's start the music
     * service.
     */
    @Override
    protected void onStart() {
        super.onStart();

        Main.startMusicService(this);
    }

    @Override
    public void onBackPressed() {
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED)
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        else {
            if (this.backPressedOnce) {
                // Default behavior, quit it
                super.onBackPressed();
                Main.forceExit(this);

                return;
            }

            this.backPressedOnce = true;

            SingleToast.show(this, getString(R.string.menu_main_back_to_exit), Toast.LENGTH_SHORT);

            backPressedHandler.postDelayed(backPressedTimeoutAction, BACK_PRESSED_DELAY);
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * When destroying the Activity.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (backPressedHandler != null)
            backPressedHandler.removeCallbacks(backPressedTimeoutAction);

        // Need to clear all the items otherwise
        // they'll keep adding up.
        // Cancell all thrown Notifications
        NotificationMusic.cancelAll(this);
/*
        Main.stopMusicService(this);
*/
    }

    /* *//**
     * A placeholder fragment containing a simple view.
     *//*
    public static class PlaceholderFragment extends Fragment {
        *//**
     * The fragment argument representing the section number for this
     * fragment.
     *//*
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        *//**
     * Returns a new instance of this fragment for the given section
     * number.
     *//*
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main_screen, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    */

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(changeSongBR, intentFilter);

        if (Main.mainMenuHasNowPlayingItem) {
            TextView t = findViewById(R.id.bottomtextView);
            TextView a = findViewById(R.id.bottomtextartist);
            t.setText(Main.musicService.currentSong.getTitle());
            a.setText(new StringBuilder().append("by ").append(Main.musicService.currentSong.getArtist()).toString());
            t.setSelected(true);
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            slidingUpPanelLayout.setCoveredFadeColor(getResources().getColor(R.color.transparent));
        }
        /*LinearLayout ll = findViewById(R.id.layout_item);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainScreen.this,PlayingNow.class);
                startActivity(intent);
            }
        });*/

        if (Main.mainMenuHasNowPlayingItem) {
            setMusicController();

            if (playbackPaused) {
                setMusicController();
                playbackPaused = false;
            }
            workonSlidingPanel();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(changeSongBR);
    }

    class ChangeSongBR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            TextView name, artist;
            name = findViewById(R.id.bottomtextView);
            artist = findViewById(R.id.bottomtextartist);
            name.setText(Main.musicService.currentSong.getTitle());
            artist.setText(Main.musicService.currentSong.getArtist());
            workOnImages();
        }

    }

    /**
     * Does an action on another Thread.
     * <p>
     * On this case, we'll scan the songs on the Android device
     * without blocking the main Thread.
     * <p>
     * It gives a nice pop-up when finishes.
     * <p>
     * Source:
     * http://answers.oreilly.com/topic/2699-how-to-handle-threads-in-android-and-what-you-need-to-watch-for/
     */
    class ScanSongs extends AsyncTask<String, Integer, String> {

        /**
         * The action we'll do in the background.
         */
        @Override
        protected String doInBackground(String... params) {

            try {
                // Will scan all songs on the device
                Main.songs.scanSongs(MainScreen.this, "external");
                return MainScreen.this.getString(R.string.menu_main_scanning_ok);
            } catch (Exception e) {
                Log.e("Couldn't execute", e.toString());
                e.printStackTrace();
                return MainScreen.this.getString(R.string.menu_main_scanning_not_ok);
            }
        }

        /**
         * Called once the background processing is done.
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            /*SingleToast.show(MainScreen.this,
                    result,
                    Toast.LENGTH_LONG);*/
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
/*
            return PlaceholderFragment.newInstance(position + 1);
*/
            switch (position) {
                case 0:
                    return new FragmentSongs();
                case 1:
                    return new FragmentPlaylist();
                case 2:
                    return new FragmentGenre();
                case 3:
                    return new FragmentAlbum();

                default:
                    return new FragmentSongs();


            }
        }

        @Override
        public int getCount() {
            // Show 4 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Songs";
                case 1:
                    return "PlayList";
                case 2:
                    return "Genre";
                case 3:
                    return "Albums";
            }
            return null;
        }
    }


    private void workonSlidingPanel() {

        circularSeekBar = findViewById(R.id.circularSeekBar);
        blurimage = findViewById(R.id.BlurImage);
        centreimage = findViewById(R.id.CircleImage);
        shuffletoggle = findViewById(R.id.shuffle);
        previousSong = findViewById(R.id.previous);
        PlayPause = findViewById(R.id.playPause);
        nextSong = findViewById(R.id.skip_next);
        repeatToggle = findViewById(R.id.repeat);

        setControllListeners();
        prepareSeekBar();


    }


    private void setControllListeners() {

        if (Main.musicService.isShuffle())
            Picasso.get().load(R.drawable.ic_menu_shuffle_on).into(shuffletoggle);
        else Picasso.get().load(R.drawable.ic_menu_shuffle_off).into(shuffletoggle);


        if (Main.musicService.isRepeat())
            Picasso.get().load(R.drawable.ic_menu_repeat_on).into(repeatToggle);
        else Picasso.get().load(R.drawable.ic_menu_repeat_off).into(repeatToggle);


        shuffletoggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main.musicService.toggleShuffle();
                if (Main.musicService.isShuffle())
                    Picasso.get().load(R.drawable.ic_menu_shuffle_on).into(shuffletoggle);
                else Picasso.get().load(R.drawable.ic_menu_shuffle_off).into(shuffletoggle);

            }
        });
        previousSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrevious();
            }
        });
        PlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main.musicService.togglePlayback();
                if (Main.musicService.isPaused())
                    Picasso.get().load(R.drawable.ic_play_dark).into(PlayPause);
                else Picasso.get().load(R.drawable.ic_pause_dark).into(PlayPause);
            }
        });
        nextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        });
        repeatToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Main.musicService.toggleRepeat();
                if (Main.musicService.isRepeat())
                    Picasso.get().load(R.drawable.ic_menu_repeat_on).into(repeatToggle);
                else Picasso.get().load(R.drawable.ic_menu_repeat_off).into(repeatToggle);
            }
        });
    }

    private void prepareSeekBar() {

        circularSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                if (musicController != null && fromUser)
                    seekTo(progress);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });


        circularSeekBar.setMax((int) Main.musicService.currentSong.getDuration());
        final Handler handler = new Handler();
        MainScreen.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isPlaying())
                    circularSeekBar.setProgress((int) getCurrentPosition());

                handler.postDelayed(this, 1);
            }
        });

        workOnImages();
    }

    private void workOnImages() {
        File path;
        Log.e("wtr",String.valueOf(Main.songs.getAlbumArt(Main.musicService.currentSong)));
        if (Main.songs.getAlbumArt(Main.musicService.currentSong) != null)
            path = new File(Main.songs.getAlbumArt(Main.musicService.currentSong));
        else path = null;
        Bitmap bitmap;
        if (path != null && path.exists()) {
            bitmap = BitmapFactory.decodeFile(path.getAbsolutePath());
        } else bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        centreimage.setImageBitmap(bitmap);
        Bitmap blurredBitmap = blurMyImage(bitmap);
        blurimage.setImageBitmap(blurredBitmap);


    }

    private Bitmap blurMyImage(Bitmap image) {
        if (null == image) return null;

        Bitmap bitmaplol = image.copy(image.getConfig(),true);
        RenderScript renderScript = RenderScript.create(this);
        Allocation tmpIn = Allocation.createFromBitmap(renderScript, image);
        Allocation tmpOut = Allocation.createFromBitmap(renderScript, bitmaplol);

//Intrinsic Gausian blur filter
        ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        theIntrinsic.setRadius(BLUR_RADIUS);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);
        tmpOut.copyTo(bitmaplol);
        renderScript.destroy();
        return bitmaplol;

    }

    private void setMusicController() {

        musicController = new MusicController(MainScreen.this);

        // What will happen when the user presses the
        // next/previous buttons?
        musicController.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Calling method defined on ActivityNowPlaying
                playNext();
            }
        }, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Calling method defined on ActivityNowPlaying
                playPrevious();
            }
        });

        // Binding to our media player
        musicController.setMediaPlayer(this);
        musicController.setEnabled(true);
    }


    @Override
    public void start() {
        Main.musicService.unpausePlayer();
    }

    /**
     * Callback to when the user pressed the `pause` button.
     */
    @Override
    public void pause() {
        Main.musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (Main.musicService != null && Main.musicService.musicBound
                && Main.musicService.isPlaying())
            return Main.musicService.getDuration();
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (Main.musicService != null && Main.musicService.musicBound
                && Main.musicService.isPlaying())
            return Main.musicService.getPosition();
        else
            return 0;
    }

    @Override
    public void seekTo(int position) {
        Main.musicService.seekTo(position);
    }

    @Override
    public boolean isPlaying() {
        return Main.musicService != null && Main.musicService.musicBound && Main.musicService.isPlaying();

    }

    @Override
    public int getBufferPercentage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        // TODO Auto-generated method stub
        return 0;
    }

    // Back to the normal methods

    /**
     * Jumps to the next song and starts playing it right now.
     */
    public void playNext() {
        Main.musicService.next(true);
        Main.musicService.playSong();
        // To prevent the MusicPlayer from behaving
        // unexpectedly when we pause the song playback.
        if (playbackPaused) {
            setMusicController();
            playbackPaused = false;
        }

/*
        musicController.show();
*/
    }

    /**
     * Jumps to the previous song and starts playing it right now.
     */
    public void playPrevious() {
        Main.musicService.previous(true);
        Main.musicService.playSong();

        // To prevent the MusicPlayer from behaving
        // unexpectedly when we pause the song playback.
        if (playbackPaused) {
            setMusicController();
            playbackPaused = false;
        }

/*
        musicController.show();
*/
    }

}