package com.sahdeepsingh.Bop.Activities;


import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.crossfadedrawerlayout.view.CrossfadeDrawerLayout;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.MiniDrawer;
import com.mikepenz.materialdrawer.holder.ImageHolder;
import com.mikepenz.materialdrawer.interfaces.ICrossfader;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.util.DrawerUIUtils;
import com.mikepenz.materialize.util.UIUtils;
import com.sahdeepsingh.Bop.R;
import com.sahdeepsingh.Bop.fragments.FragmentAlbum;
import com.sahdeepsingh.Bop.fragments.FragmentArtist;
import com.sahdeepsingh.Bop.fragments.FragmentGenre;
import com.sahdeepsingh.Bop.fragments.FragmentPlaylist;
import com.sahdeepsingh.Bop.fragments.FragmentSongs;
import com.sahdeepsingh.Bop.fragments.HomeFragment;
import com.sahdeepsingh.Bop.notifications.NotificationMusic;
import com.sahdeepsingh.Bop.playerMain.Main;
import com.sahdeepsingh.Bop.utils.utils;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.Objects;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.legacy.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import me.tankery.lib.circularseekbar.CircularSeekBar;


public class MainScreen extends BaseActivity implements MediaController.MediaPlayerControl {


    public static final String BROADCAST_ACTION = "lol";

    /**
     * How long to wait to disable double-pressing to quit
     */
    private static final int BACK_PRESSED_DELAY = 2000;

    /**
     * Action that actually disables double-pressing to quit
     */
    private final Runnable backPressedTimeoutAction = () -> backPressedOnce = false;
    /*AlbumArt in Sliding Panel*/
    ImageView albumArtSP, next, previous, forward, rewind;
    /*Song name, time left and Total time in Sliding Panel*/
    TextView songNameSP;
    Drawer drawer;
    AccountHeader accountHeader;
    /* Next two are for Navigation Drawer*/
    CrossfadeDrawerLayout crossfadeDrawerLayout;
    /* BroadCast receiver for every toggle and stuff*/
    ChangeSongBR changeSongBR;
    /*Our non Sliding Panel*/
    SlidingUpPanelLayout slidingUpPanelLayout;
    private boolean playbackPaused = false;
    private boolean backPressedOnce = false;
    private CircularSeekBar mProgressView;
    private Handler backPressedHandler = new Handler();
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    public static void addNowPlayingItem() {
        if (Main.mainMenuHasNowPlayingItem)
            return;
        Main.mainMenuHasNowPlayingItem = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        Main.settings.load(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        slidingUpPanelLayout = findViewById(R.id.sliding_layout);
        songNameSP = findViewById(R.id.bottomtextView);
        albumArtSP = findViewById(R.id.bottomImageview);
        next = findViewById(R.id.next);
        previous = findViewById(R.id.previous);
        rewind = findViewById(R.id.rewind);
        forward = findViewById(R.id.forward);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_ham)));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawer.isDrawerOpen()) {
                    drawer.closeDrawer();
                } else drawer.openDrawer();
            }
        });

        slidingUpPanelLayout.setTouchEnabled(false);

        mViewPager = findViewById(R.id.container);
        setupViewPager(mViewPager);

        changeSongBR = new ChangeSongBR();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_ACTION);
        registerReceiver(changeSongBR, intentFilter);

        createDrawer();
        if (Main.settings.get("modes", "Day").equals("Day"))
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.md_white_1000));
        else getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.md_grey_900));
    }

    private void createDrawer() {

        accountHeader = new AccountHeaderBuilder().withActivity(this)
                .withSelectionListEnabled(false)
                .addProfiles(
                        new ProfileDrawerItem().withName("Bop - Music Player").withIcon(R.mipmap.ic_launcher_round)
                ).build();

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withAccountHeader(accountHeader)
                .withDrawerLayout(R.layout.crossfade_material_drawer)
                .withHasStableIds(true)
                .withDrawerWidthDp(72)
                .withGenerateMiniDrawer(true)
                .withCloseOnClick(true)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Now Playing").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_play))).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName("Home").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_shuffle_on))).withIdentifier(6).withSelectable(true),
                        new PrimaryDrawerItem().withName("All Songs").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_music))).withIdentifier(2).withSelectable(true),
                        new PrimaryDrawerItem().withName("Playlist").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_playlist))).withIdentifier(3).withSelectable(true),
                        new PrimaryDrawerItem().withName("Genres").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_genre))).withIdentifier(4).withSelectable(true),
                        new PrimaryDrawerItem().withName("Albums").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_album))).withIdentifier(5).withSelectable(true),
                        new PrimaryDrawerItem().withName("Artists").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_artist))).withIdentifier(7).withSelectable(true),
                        new SectionDrawerItem().withName("More").withDivider(true),
                        new SecondaryDrawerItem().withName("Support").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_support))).withIdentifier(20).withSelectable(false),
                        new SecondaryDrawerItem().withName("Feedback").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_feedback))).withIdentifier(21).withSelectable(false))
                .addStickyDrawerItems(
                        new SecondaryDrawerItem().withName("Settings").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_setting))).withIdentifier(321).withSelectable(false),
                        new SecondaryDrawerItem().withName("Exit").withIcon(utils.getThemedIcon(this, getDrawable(R.drawable.ic_exit))).withIdentifier(342).withSelectable(false)
                ).withOnDrawerItemClickListener((view, position, drawerItem) -> {
                    if (drawerItem != null) {
                        Intent intent;
                        if (drawerItem.getIdentifier() == 1) {
                            if (Main.mainMenuHasNowPlayingItem) {
                                intent = new Intent(this, PlayingNowList.class);
                                startActivity(intent);
                            } else
                                Toast.makeText(getApplicationContext(), "no playlist", Toast.LENGTH_SHORT).show();
                        } else if (drawerItem.getIdentifier() == 2) {
                            mViewPager.setCurrentItem(1, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 3) {
                            mViewPager.setCurrentItem(2, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 4) {
                            mViewPager.setCurrentItem(3, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 5) {
                            mViewPager.setCurrentItem(4, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 6) {
                            mViewPager.setCurrentItem(0, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 7) {
                            mViewPager.setCurrentItem(5, true);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 20) {
                            utils.openCustomTabs(MainScreen.this, "https://github.com/iamSahdeep/Bop/issues");
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 21) {
                            utils.sendFeedback(MainScreen.this);
                            drawer.closeDrawer();
                        } else if (drawerItem.getIdentifier() == 321) {
                            intent = new Intent(this, SettingsActivity.class);
                            startActivity(intent);
                        } else if (drawerItem.getIdentifier() == 342) {
                            Main.forceExit(this);
                            finishAffinity();
                        }
                    }
                    return false;
                })
                .build();

        //noinspection deprecation
        crossfadeDrawerLayout = (CrossfadeDrawerLayout) drawer.getDrawerLayout();
        crossfadeDrawerLayout.setMaxWidthPx(DrawerUIUtils.getOptimalDrawerWidth(this));
        MiniDrawer miniDrawer = drawer.getMiniDrawer();
        View view = miniDrawer.build(this);
        if (currentMode.equals("Night")) {
            view.setBackgroundColor(UIUtils.getThemeColorFromAttrOrRes(this, com.mikepenz.materialdrawer.R.attr.material_drawer_background, com.mikepenz.materialize.R.color.background_material_dark));
        } else {
            view.setBackgroundColor(UIUtils.getThemeColorFromAttrOrRes(this, com.mikepenz.materialdrawer.R.attr.material_drawer_background, com.mikepenz.materialize.R.color.background_material_light));
        }
        crossfadeDrawerLayout.getSmallView().addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        miniDrawer.withCrossFader(new ICrossfader() {
            @Override
            public void crossfade() {
                boolean isFaded = isCrossfaded();
                crossfadeDrawerLayout.crossfade(500);
                if (isFaded) {
                    drawer.getDrawerLayout().closeDrawer(GravityCompat.START);
                }
            }

            @Override
            public boolean isCrossfaded() {
                return crossfadeDrawerLayout.isCrossfaded();
            }
        });


    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        viewPager.setAdapter(mSectionsPagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                switch (position) {
                    case 0:
                        setTitle(R.string.app_name);
                        break;
                    case 1:
                        setTitle("Songs");
                        break;
                    case 2:
                        setTitle("Playlists");
                        break;
                    case 3:
                        setTitle("Genres");
                        break;
                    case 4:
                        setTitle("Albums");
                        break;
                    case 5:
                        setTitle("Artists");
                        break;
                    default:
                        setTitle(R.string.app_name);

                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    public void openPlayer(View view) {
        startActivity(new Intent(this, PlayingNowList.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Main.startMusicService(this);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen()) {
            drawer.closeDrawer();
            return;
        } else if (mViewPager.getCurrentItem() != 0) {
            mViewPager.setCurrentItem(0, true);
            return;
        } else if (this.backPressedOnce) {
            super.onBackPressed();
            Main.forceExit(this);
            finishAffinity();
            return;
        }

        this.backPressedOnce = true;

        Toast.makeText(this, getString(R.string.menu_main_back_to_exit), Toast.LENGTH_SHORT).show();

        backPressedHandler.postDelayed(backPressedTimeoutAction, BACK_PRESSED_DELAY);
    }

    /**
     * When destroying the Activity.
     */
    @Override
    protected void onDestroy() {
        unregisterReceiver(changeSongBR);
        super.onDestroy();

        if (backPressedHandler != null)
            backPressedHandler.removeCallbacks(backPressedTimeoutAction);

        NotificationMusic.cancelAll(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        MainScreen.this.invalidateOptionsMenu();

        if (Main.mainMenuHasNowPlayingItem) {
            Main.musicService.notifyCurrentSong();
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            slidingUpPanelLayout.setCoveredFadeColor(getResources().getColor(R.color.transparent));
        } else {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
            slidingUpPanelLayout.setCoveredFadeColor(getResources().getColor(R.color.transparent));
        }

        if (isPlaying()) {
            if (playbackPaused) {
                playbackPaused = false;
            }
            workonSlidingPanel();
        }
    }


    /**
     * Let's set a context menu (menu that appears when
     * the user presses the "menu" button).
     * Its unnecessary as i have added these option in side NAV
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Default options specified on the XML
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_context, menu);

        // Extra option to go to Now Playing screen
        // (only activated when there's an actual Now Playing screen)
        if (Main.musicService.isPlaying())
            menu.findItem(R.id.nowPlayingIcon).setVisible(true);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * This method gets called whenever the user clicks an
     * item on the context menu.
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            // I know it's bad to force quiting the program,
            // but I just love when applications have this option. xD
            case R.id.context_menu_end:
                Main.forceExit(this);
                break;

            case R.id.context_menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.nowPlayingIcon:
                startActivity(new Intent(this, PlayingNowList.class));
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /*Why I have used refreshMode here?
         * It took me a lot of time to understand why my app was crashing and throwing error : changeSongBR not Registered
         * So, As we are using refreshMode() in BaseActivity in onResume i.e, in "super" of this activity
         * Like, when onResume of this activity is called its super.onResume is called first
         * Which  means we have not yet registered the BroadCast Receiver : changeSongBR, look at onResume of this activity
         * Now if the Mode has been changed then Activity will be recreated
         * And in Activity lifecycle we know, when destroying activity it will call onPause
         * And in onPause we are unregistering the BroadcastReceiver which was not actually registered
         * which gives throws the exception "changeSongBR" not Registered
         * So, we are using this Or we can use try and catch block but
         * That's a bummer :(
         * */




       /* if (refreshMode())
            unregisterReceiver(changeSongBR);*/

        /** *
         * TADA!!!
         * a simple trick here to prevent memory leak is
         * to transfer broadcast in onCreate and onDestroy methods
         * also call @unregisterBR before super.onDestroy in that method
         * **/
    }


    private void setControlListeners() {
        next.setImageDrawable(utils.getThemedIcon(this, getDrawable(R.drawable.ic_skip)));
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playNext();
            }
        });

        previous.setImageDrawable(utils.getThemedIcon(this, getDrawable(R.drawable.ic_previous)));
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playPrevious();
            }
        });

        forward.setImageDrawable(utils.getThemedIcon(this, getDrawable(R.drawable.ic_forward)));
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seekTo(getCurrentPosition() + 10000);
            }
        });

        rewind.setImageDrawable(utils.getThemedIcon(this, getDrawable(R.drawable.ic_rewind)));
        rewind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seekTo(getCurrentPosition() - 10000);
            }
        });

    }

    private void workonSlidingPanel() {

        setControlListeners();
        prepareSeekBar();
    }

    private void prepareSeekBar() {
        mProgressView = findViewById(R.id.footerseek);
        mProgressView.setMax((int) Main.musicService.currentSong.getDurationSeconds());
        mProgressView.setLockEnabled(true);

        Handler handler = new Handler();
        MainScreen.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!Main.mainMenuHasNowPlayingItem) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    slidingUpPanelLayout.setCoveredFadeColor(getResources().getColor(R.color.transparent));
                }
                if (isPlaying()) {
                    mProgressView.setProgress(getCurrentPosition() / 1000);
                }
                handler.postDelayed(this, 1000);
            }
        });
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

    /**
     * Jumps to the next song and starts playing it right now.
     */
    public void playNext() {
        Main.musicService.next(true);
        Main.musicService.playSong();

        // To prevent the MusicPlayer from behaving
        // unexpectedly when we pause the song playback.
        if (playbackPaused) {
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
            playbackPaused = false;
        }

/*
        musicController.show();
*/
    }

    @Override
    public int getBufferPercentage() {
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
        return Main.musicService.getAudioSession();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
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
                    return new HomeFragment();
                case 1:
                    return new FragmentSongs();
                case 2:
                    return new FragmentPlaylist();
                case 3:
                    return new FragmentGenre();
                case 4:
                    return new FragmentAlbum();
                case 5:
                    return new FragmentArtist();
                default:
                    return new HomeFragment();


            }
        }

        @Override
        public int getCount() {
            // Show 5 total pages.
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Home";
                case 1:
                    return "Songs";
                case 2:
                    return "Playlist";
                case 3:
                    return "Genres";
                case 4:
                    return "Albums";
                case 5:
                    return "Artists";
            }
            return null;
        }
    }

    class ChangeSongBR extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            songNameSP.setText(Main.musicService.currentSong.getTitle());
            Bitmap newImage;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            newImage = BitmapFactory.decodeFile(Main.songs.getAlbumArt(Main.musicService.currentSong));
            if (newImage != null) {
                albumArtSP.setImageBitmap(newImage);
                accountHeader.setHeaderBackground(new ImageHolder(newImage));
            } else {
                albumArtSP.setImageResource(R.mipmap.ic_launcher_foreground);
                accountHeader.setHeaderBackground(new ImageHolder(R.mipmap.ic_launcher));
            }
        }

    }

}
