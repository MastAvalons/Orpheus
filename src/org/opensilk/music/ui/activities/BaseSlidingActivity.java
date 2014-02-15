/*
 * Copyright (C) 2012 Andrew Neal
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensilk.music.ui.activities;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.app.MediaRouteButton;
import android.support.v7.media.MediaRouter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.andrew.apollo.IApolloService;
import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.MusicStateListener;
import com.andrew.apollo.R;
import com.andrew.apollo.loaders.NowPlayingCursor;
import com.andrew.apollo.loaders.QueueLoader;
import com.andrew.apollo.menu.CreateNewPlaylist;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.Lists;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.MusicUtils.ServiceToken;
import com.andrew.apollo.utils.NavUtils;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.IVideoCastConsumer;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.opensilk.music.cast.CastUtils;
import org.opensilk.music.cast.CastWebServer;
import org.opensilk.music.ui.fragments.ArtFragment;
import org.opensilk.music.ui.fragments.QueueFragment;
import org.opensilk.music.widgets.PlayPauseButton;
import org.opensilk.music.widgets.RepeatButton;
import org.opensilk.music.widgets.RepeatingImageButton;
import org.opensilk.music.widgets.ShuffleButton;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.ArrayList;

import static com.andrew.apollo.utils.MusicUtils.mService;

/**
 * A base {@link FragmentActivity} used to update the bottom bar and
 * bind to Apollo's service.
 * <p>
 * {@link HomeSlidingActivity} extends from this skeleton.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public abstract class BaseSlidingActivity extends ActionBarActivity implements
        ServiceConnection,
        SeekBar.OnSeekBarChangeListener {

    private static final String TAG = BaseSlidingActivity.class.getSimpleName();
    // Message to refresh the time
    private static final int REFRESH_TIME = 1;

    /** Playstate and meta change listener */
    private final ArrayList<MusicStateListener> mMusicStateListener = Lists.newArrayList();

    /** The service token */
    private ServiceToken mToken;

    /** Broadcast receiver */
    private PlaybackStatus mPlaybackStatus;

    /** Handler used to update the current time */
    private TimeHandler mTimeHandler;

    private VideoCastManager mCastManager;

    /** Panel Header */
    private ViewGroup mPanelHeader;
    //play/pause
    private PlayPauseButton mHeaderPlayPauseButton;
    // Next button
    private RepeatingImageButton mHeaderNextButton;
    // Album art
    private ImageView mHeaderAlbumArt;
    // queue switch button
    private ImageButton mHeaderQueueSwitch;
    // overflow btn
    private ImageButton mHeaderOverflow;
    // Track name
    private TextView mHeaderTrackName;
    // Artist name
    private TextView mHeaderArtistName;
    //media router btn
    private MediaRouteButton mHeaderMediaRouteButton;

    /** Panel Footer */
    // Play and pause button
    private PlayPauseButton mFooterPlayPauseButton;
    // Repeat button
    private RepeatButton mFooterRepeatButton;
    // Shuffle button
    private ShuffleButton mFooterShuffleButton;
    // Previous button
    private RepeatingImageButton mFooterPreviousButton;
    // Next button
    private RepeatingImageButton mFooterNextButton;
    // Progess
    private SeekBar mFooterProgress;
    // Current time
    private TextView mFooterCurrentTime;
    // Total time
    private TextView mFooterTotalTime;

    /** Sliding panel */
    private SlidingUpPanelLayout mSlidingPanel;

    /** Whether the queue is showing */
    private boolean mQueueShowing;

    private long mPosOverride = -1;
    private long mStartSeekPos = 0;
    private long mLastSeekEventTime;
    private long mLastShortSeekEventTime;
    private boolean mIsPaused = false;
    private boolean mFromTouch = false;

    CastWebServer server;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        // Fade it in
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Control the media volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Bind Apollo's service
        mToken = MusicUtils.bindToService(this, this);

        // Initialize the broadcast receiver
        mPlaybackStatus = new PlaybackStatus(this);

        // Initialize the handler used to update the current time
        mTimeHandler = new TimeHandler(this);

        mCastManager = CastUtils.getCastManager(this);

        // Set the layout
        setContentView(getContentView());

        mSlidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mSlidingPanel.setShadowDrawable(getResources().getDrawable(R.drawable.above_shadow));
        mSlidingPanel.setDragView(findViewById(R.id.track_artist_info));
        mSlidingPanel.setPanelSlideListener(mPanelSlideListener);

        // Initialze the panel
        initPanel();

        try {
            server = new CastWebServer(this, CastUtils.getWifiIpAddress(this), 8080);
            server.start();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        startPlayback();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = IApolloService.Stub.asInterface(service);
        startPlayback();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Update the favorites icon
        invalidateOptionsMenu();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Media router
        getMenuInflater().inflate(R.menu.cast_player_menu, menu);
        // init router button
        mCastManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        // Settings
        getMenuInflater().inflate(R.menu.activity_base, menu);
        //TODO add back search
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                // Settings
                //NavUtils.openSettings(this);
                try {
                    mCastManager.loadMedia(CastUtils.buildSample(this), true, 0);
                } catch (TransientNetworkDisconnectionException e) {
                    e.printStackTrace();
                } catch (NoConnectionException e) {
                    e.printStackTrace();
                }
                return true;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Set the playback drawables
        updatePlaybackControls();
        // Current info
        updateNowPlayingInfo();
        // Refresh the queue
        refreshQueue();
        mCastManager = CastUtils.getCastManager(this);
        mCastManager.addVideoCastConsumer(mCastConsumer);
        mCastManager.incrementUiCounter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        // Play and pause changes
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        // Shuffle and repeat changes
        filter.addAction(MusicPlaybackService.SHUFFLEMODE_CHANGED);
        filter.addAction(MusicPlaybackService.REPEATMODE_CHANGED);
        // Track changes
        filter.addAction(MusicPlaybackService.META_CHANGED);
        // Update a list, probably the playlist fragment's
        filter.addAction(MusicPlaybackService.REFRESH);
        registerReceiver(mPlaybackStatus, filter);
        // Refresh the current time
        final long next = refreshCurrentTime();
        queueNextRefresh(next);
        MusicUtils.notifyForegroundStateChanged(this, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCastManager.decrementUiCounter();
        mCastManager.removeVideoCastConsumer(mCastConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
        MusicUtils.notifyForegroundStateChanged(this, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mIsPaused = false;
        mTimeHandler.removeMessages(REFRESH_TIME);
        // Unbind from the service
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
            mToken = null;
        }

        // Unregister the receiver
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
            //$FALL-THROUGH$
        }

        // Remove any music status listeners
        mMusicStateListener.clear();

        if (server != null) {
            server.stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onBackPressed() {
        if (mSlidingPanel.isExpanded()) {
            mSlidingPanel.collapsePane();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onProgressChanged(final SeekBar bar, final int progress, final boolean fromuser) {
        if (!fromuser || mService == null) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now - mLastSeekEventTime > 250) {
            mLastSeekEventTime = now;
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            MusicUtils.seek(mPosOverride);
            if (!mFromTouch) {
                // refreshCurrentTime();
                mPosOverride = -1;
            }
        } else if (now - mLastShortSeekEventTime > 5) {
            mLastShortSeekEventTime = now;
            mPosOverride = MusicUtils.duration() * progress / 1000;
            refreshCurrentTimeText(mPosOverride);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStartTrackingTouch(final SeekBar bar) {
        mLastSeekEventTime = 0;
        mFromTouch = true;
        mFooterCurrentTime.setVisibility(View.VISIBLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onStopTrackingTouch(final SeekBar bar) {
        if (mPosOverride != -1) {
            MusicUtils.seek(mPosOverride);
        }
        mPosOverride = -1;
        mFromTouch = false;
    }

    /**
     * Initializes the items in the bottom action bar.
     */
    private void initPanel() {
        //Load art
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.panel_main_content, new ArtFragment(), "art_bg")
                .commit();

        //Header
        mPanelHeader = (ViewGroup) findViewById(R.id.panel_header);

        // Play and pause button
        mHeaderPlayPauseButton = (PlayPauseButton)findViewById(R.id.header_action_button_play);
        // Next button
        mHeaderNextButton = (RepeatingImageButton) findViewById(R.id.header_action_button_next);
        // Set the repeat listner for the next button
        mHeaderNextButton.setRepeatListener(mFastForwardListener);
        // Track name
        mHeaderTrackName = (TextView)findViewById(R.id.header_track_info);
        // Artist name
        mHeaderArtistName = (TextView)findViewById(R.id.header_artist_info);
        // Album art
        mHeaderAlbumArt = (ImageView)findViewById(R.id.header_album_art);
        // Open to the currently playing album profile
        mHeaderAlbumArt.setOnClickListener(mOpenCurrentAlbumProfile);
        // Used to show and hide the queue fragment
        mHeaderQueueSwitch = (ImageButton) findViewById(R.id.header_switch_queue);
        mHeaderQueueSwitch.setOnClickListener(mToggleHiddenPanel);

        // overflow
        mHeaderOverflow = (ImageButton) findViewById(R.id.header_overflow);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(BaseSlidingActivity.this, mHeaderOverflow);
                popupMenu.getMenuInflater().inflate(R.menu.panel, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(mPanelOverflowMenuClickListener);
                popupMenu.show();
            }
        });

        mHeaderMediaRouteButton = (MediaRouteButton) findViewById(R.id.panel_mediarouter);
        // init router button TODO might need to hide if no routes available
        mHeaderMediaRouteButton.setRouteSelector(mCastManager.getMediaRouteSelector());

        if (!mSlidingPanel.isExpanded()) {
            mHeaderQueueSwitch.setVisibility(View.GONE);
            mHeaderOverflow.setVisibility(View.GONE);
            mHeaderMediaRouteButton.setVisibility(View.GONE);
        }

        // Play and pause button
        mFooterPlayPauseButton = (PlayPauseButton)findViewById(R.id.footer_action_button_play);
        // Shuffle button
        mFooterShuffleButton = (ShuffleButton)findViewById(R.id.footer_action_button_shuffle);
        // Repeat button
        mFooterRepeatButton = (RepeatButton)findViewById(R.id.footer_action_button_repeat);
        // Previous button
        mFooterPreviousButton = (RepeatingImageButton)findViewById(R.id.footer_action_button_previous);
        // Set the repeat listner for the previous button
        mFooterPreviousButton.setRepeatListener(mRewindListener);
        // Next button
        mFooterNextButton = (RepeatingImageButton)findViewById(R.id.footer_action_button_next);
        // Set the repeat listner for the next button
        mFooterNextButton.setRepeatListener(mFastForwardListener);
        // Current time
        mFooterCurrentTime = (TextView)findViewById(R.id.footer_player_current_time);
        // Total time
        mFooterTotalTime = (TextView)findViewById(R.id.footer_player_total_time);
        // Progress
        mFooterProgress = (SeekBar)findViewById(android.R.id.progress);
        // Update the progress
        mFooterProgress.setOnSeekBarChangeListener(this);
    }

    /**
     * Sets the track name, album name, and album art.
     */
    private void updateNowPlayingInfo() {
        // Set the track name
        mHeaderTrackName.setText(MusicUtils.getTrackName());
        // Set the artist name
        mHeaderArtistName.setText(MusicUtils.getArtistName());
        // Set the album art
        ApolloUtils.getImageFetcher(this).loadCurrentArtwork(mHeaderAlbumArt);
        // Set the total time
        mFooterTotalTime.setText(MusicUtils.makeTimeString(this, MusicUtils.duration() / 1000));
        // Set the album art
        ApolloUtils.getImageFetcher(this).loadCurrentArtwork(
                ((ArtFragment) getSupportFragmentManager().findFragmentByTag("art_bg")).getArtImage()
        );
        // Update the current time
        queueNextRefresh(1);
    }

    /**
     * Sets the correct drawable states for the playback controls.
     */
    private void updatePlaybackControls() {
        // Set the play and pause image
        mHeaderPlayPauseButton.updateState();
        // Set the play and pause image
        mFooterPlayPauseButton.updateState();
        // Set the shuffle image
        mFooterShuffleButton.updateShuffleState();
        // Set the repeat image
        mFooterRepeatButton.updateRepeatState();
    }

    /**
     * Checks whether the passed intent contains a playback request,
     * and starts playback if that's the case
     */
    private void startPlayback() {
        Intent intent = getIntent();

        if (intent == null || mService == null) {
            return;
        }

        Uri uri = intent.getData();
        String mimeType = intent.getType();
        boolean handled = false;

        if (uri != null && uri.toString().length() > 0) {
            MusicUtils.playFile(this, uri);
            handled = true;
        } else if (MediaStore.Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
            long id = intent.getLongExtra("playlistId", -1);
            if (id < 0) {
                String idString = intent.getStringExtra("playlist");
                if (idString != null) {
                    try {
                        id = Long.parseLong(idString);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
            if (id >= 0) {
                MusicUtils.playPlaylist(this, id);
                handled = true;
            }
        }

        if (handled) {
            // Make sure to process intent only once
            setIntent(new Intent());
            // Refresh the queue
            refreshQueue();
        }
    }

    /**
     * @param delay When to update
     */
    private void queueNextRefresh(final long delay) {
        if (!mIsPaused) {
            final Message message = mTimeHandler.obtainMessage(REFRESH_TIME);
            mTimeHandler.removeMessages(REFRESH_TIME);
            mTimeHandler.sendMessageDelayed(message, delay);
        }
    }

    /**
     * Used to scan backwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanBackward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos - delta;
            if (newpos < 0) {
                // move to previous track
                MusicUtils.previous(this);
                final long duration = MusicUtils.duration();
                mStartSeekPos += duration;
                newpos += duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    /**
     * Used to scan forwards in time through the curren track
     *
     * @param repcnt The repeat count
     * @param delta The long press duration
     */
    private void scanForward(final int repcnt, long delta) {
        if (mService == null) {
            return;
        }
        if (repcnt == 0) {
            mStartSeekPos = MusicUtils.position();
            mLastSeekEventTime = 0;
        } else {
            if (delta < 5000) {
                // seek at 10x speed for the first 5 seconds
                delta = delta * 10;
            } else {
                // seek at 40x after that
                delta = 50000 + (delta - 5000) * 40;
            }
            long newpos = mStartSeekPos + delta;
            final long duration = MusicUtils.duration();
            if (newpos >= duration) {
                // move to next track
                MusicUtils.next();
                mStartSeekPos -= duration; // is OK to go negative
                newpos -= duration;
            }
            if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
                MusicUtils.seek(newpos);
                mLastSeekEventTime = delta;
            }
            if (repcnt >= 0) {
                mPosOverride = newpos;
            } else {
                mPosOverride = -1;
            }
            refreshCurrentTime();
        }
    }

    private void refreshCurrentTimeText(final long pos) {
        mFooterCurrentTime.setText(MusicUtils.makeTimeString(this, pos / 1000));
    }

    /* Used to update the current time string */
    private long refreshCurrentTime() {
        if (mService == null) {
            return 500;
        }
        try {
            final long pos = mPosOverride < 0 ? MusicUtils.position() : mPosOverride;
            if (pos >= 0 && MusicUtils.duration() > 0) {
                refreshCurrentTimeText(pos);
                final int progress = (int)(1000 * pos / MusicUtils.duration());
                mFooterProgress.setProgress(progress);

                if (mFromTouch) {
                    return 500;
                } else if (MusicUtils.isPlaying()) {
                    mFooterCurrentTime.setVisibility(View.VISIBLE);
                } else {
                    // blink the counter
                    final int vis = mFooterCurrentTime.getVisibility();
                    mFooterCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
                            : View.INVISIBLE);
                    return 500;
                }
            } else {
                mFooterCurrentTime.setText("--:--");
                mFooterProgress.setProgress(1000);
            }
            // calculate the number of milliseconds until the next full second,
            // so
            // the counter can be updated at just the right time
            final long remaining = 1000 - pos % 1000;
            // approximate how often we would need to refresh the slider to
            // move it smoothly
            int width = mFooterProgress.getWidth();
            if (width == 0) {
                width = 320;
            }
            final long smoothrefreshtime = MusicUtils.duration() / width;
            if (smoothrefreshtime > remaining) {
                return remaining;
            }
            if (smoothrefreshtime < 20) {
                return 20;
            }
            return smoothrefreshtime;
        } catch (final Exception ignored) {

        }
        return 500;
    }

    public void refreshQueue() {
        if (mQueueShowing) {
            QueueFragment queue = (QueueFragment) getSupportFragmentManager().findFragmentByTag("queue");
            if (queue != null) {
                queue.refreshQueue();
            }
        }
    }

    private void pushQueueFragment() {
        getSupportFragmentManager().beginTransaction()
                .hide(getSupportFragmentManager().findFragmentByTag("art_bg"))
                .add(R.id.panel_main_content, new QueueFragment(), "queue")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        mHeaderQueueSwitch.setImageResource(R.drawable.ic_queue_inverse);
        mQueueShowing = true;
    }

    private void popQueueFragment() {
        getSupportFragmentManager().beginTransaction()
                .show(getSupportFragmentManager().findFragmentByTag("art_bg"))
                .remove(getSupportFragmentManager().findFragmentByTag("queue"))
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        mHeaderQueueSwitch.setImageResource(R.drawable.ic_queue);
        mQueueShowing = false;
    }

    /**
     * Used to scan backwards through the track
     */
    private final RepeatingImageButton.RepeatListener mRewindListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanBackward(repcnt, howlong);
        }
    };

    /**
     * Used to scan ahead through the track
     */
    private final RepeatingImageButton.RepeatListener mFastForwardListener = new RepeatingImageButton.RepeatListener() {
        /**
         * {@inheritDoc}
         */
        @Override
        public void onRepeat(final View v, final long howlong, final int repcnt) {
            scanForward(repcnt, howlong);
        }
    };

    /**
     * Switches from the large album art screen to show the queue and lyric
     * fragments, then back again
     */
    private final View.OnClickListener mToggleHiddenPanel = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            if (!mQueueShowing) {
                pushQueueFragment();
            } else {
                popQueueFragment();
            }

        }
    };

    private final SlidingUpPanelLayout.PanelSlideListener mPanelSlideListener =
            new SlidingUpPanelLayout.PanelSlideListener() {
        @Override
        public void onPanelSlide(View panel, float slideOffset) {
            Log.i(TAG, "onPanelSlide, offset " + slideOffset);
            if (slideOffset < 0.2) {
                if (getActionBar().isShowing()) {
                    getActionBar().hide();
                }
            } else {
                if (!getActionBar().isShowing()) {
                    getActionBar().show();
                }
            }
        }

        @Override
        public void onPanelExpanded(View panel) {
            Log.i(TAG, "onPanelExpanded");
            mHeaderQueueSwitch.setVisibility(View.VISIBLE);
            mHeaderOverflow.setVisibility(View.VISIBLE);
            mHeaderMediaRouteButton.setVisibility(View.VISIBLE);
            mHeaderPlayPauseButton.setVisibility(View.GONE);
            mHeaderNextButton.setVisibility(View.GONE);
            mPanelHeader.setBackgroundResource(R.color.app_background_light_transparent);
        }

        @Override
        public void onPanelCollapsed(View panel) {
            Log.i(TAG, "onPanelCollapsed");
            mHeaderQueueSwitch.setVisibility(View.GONE);
            mHeaderOverflow.setVisibility(View.GONE);
            mHeaderMediaRouteButton.setVisibility(View.GONE);
            mHeaderPlayPauseButton.setVisibility(View.VISIBLE);
            mHeaderNextButton.setVisibility(View.VISIBLE);
            if (mQueueShowing) {
                popQueueFragment();
            }
            mPanelHeader.setBackgroundResource(R.color.app_background_light);
        }

        @Override
        public void onPanelAnchored(View panel) {
            Log.i(TAG, "onPanelAnchored");

        }
    };

    /**
     * Opens the album profile of the currently playing album
     */
    private final View.OnClickListener mOpenCurrentAlbumProfile = new View.OnClickListener() {

        /**
         * {@inheritDoc}
         */
        @Override
        public void onClick(final View v) {
            if (MusicUtils.getCurrentAudioId() != -1) {
                NavUtils.openAlbumProfile(BaseSlidingActivity.this, MusicUtils.getAlbumName(),
                        MusicUtils.getArtistName(), MusicUtils.getCurrentAlbumId());
            } else {
                MusicUtils.shuffleAll(BaseSlidingActivity.this);
            }
//            if (BaseActivity.this instanceof ProfileActivity) {
//                finish();
//            }
        }
    };

    private final IVideoCastConsumer mCastConsumer = new VideoCastConsumerImpl() {
        @Override
        public void onApplicationConnected(ApplicationMetadata appMetadata,
                                           String sessionId, boolean wasLaunched) {
            Log.d(TAG, "onApplicationLaunched() is reached");

        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            Log.d(TAG, "onApplicationDisconnected() is reached with errorCode: " + errorCode);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected() is reached");
        }

        @Override
        public void onRemoteMediaPlayerMetadataUpdated() {

        }

        @Override
        public void onFailed(int resourceId, int statusCode) {

        }

        @Override
        public void onConnectionSuspended(int cause) {
            Log.d(TAG, "onConnectionSuspended() was called with cause: " + cause);
            Toast.makeText(BaseSlidingActivity.this, "connnection lost", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onConnectivityRecovered() {
            Toast.makeText(BaseSlidingActivity.this, "connnection recovered", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCastDeviceDetected(final MediaRouter.RouteInfo info) {

        }
    };

    /**
     * Handles panel overflow menu
     */
    private final PopupMenu.OnMenuItemClickListener mPanelOverflowMenuClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.panel_menu_shuffle_all:
                    // Shuffle all the songs
                    MusicUtils.shuffleAll(BaseSlidingActivity.this);
                    // Refresh the queue
                    refreshQueue();
                    return true;
                case R.id.panel_menu_share:
                    // Share the current meta data
                    if (MusicUtils.getTrackName() != null && MusicUtils.getArtistName() != null) {
                        final Intent shareIntent = new Intent();
                        final String shareMessage = getString(R.string.now_listening_to,
                                MusicUtils.getTrackName(), MusicUtils.getArtistName());
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_track_using)));
                    }
                    return true;
                case R.id.panel_menu_equalizer:
                    // Sound effects
                    NavUtils.openEffectsPanel(BaseSlidingActivity.this);
                    return true;
                case R.id.panel_menu_use_ringtone:
                    // Set the current track as a ringtone
                    MusicUtils.setRingtone(BaseSlidingActivity.this, MusicUtils.getCurrentAudioId());
                    return true;
                case R.id.menu_audio_player_delete:
                    // Delete current song
                    DeleteDialog.newInstance(MusicUtils.getTrackName(), new long[]{
                            MusicUtils.getCurrentAudioId()
                    }, null).show(getSupportFragmentManager(), "DeleteDialog");
                    return true;
                case R.id.menu_save_queue:
                    NowPlayingCursor queue = (NowPlayingCursor) QueueLoader
                            .makeQueueCursor(BaseSlidingActivity.this);
                    CreateNewPlaylist.getInstance(MusicUtils.getSongListForCursor(queue)).show(
                            getSupportFragmentManager(), "CreatePlaylist");
                    queue.close();
                    return true;
                case R.id.menu_clear_queue:
                    MusicUtils.clearQueue();
                    mSlidingPanel.collapsePane();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    /**
     * Used to update the current time string
     */
    private static final class TimeHandler extends Handler {

        private final WeakReference<BaseSlidingActivity> mAudioPlayer;

        /**
         * Constructor of <code>TimeHandler</code>
         */
        public TimeHandler(final BaseSlidingActivity player) {
            mAudioPlayer = new WeakReference<BaseSlidingActivity>(player);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case REFRESH_TIME:
                    final long next = mAudioPlayer.get().refreshCurrentTime();
                    mAudioPlayer.get().queueNextRefresh(next);
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Used to monitor the state of playback
     */
    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<BaseSlidingActivity> mReference;

        /**
         * Constructor of <code>PlaybackStatus</code>
         */
        public PlaybackStatus(final BaseSlidingActivity activity) {
            mReference = new WeakReference<BaseSlidingActivity>(activity);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action.equals(MusicPlaybackService.META_CHANGED)) {
                // Current info
                mReference.get().updateNowPlayingInfo();
                // Update the favorites icon
                mReference.get().invalidateOptionsMenu();
                // Let the listener know to the meta chnaged
                for (final MusicStateListener listener : mReference.get().mMusicStateListener) {
                    if (listener != null) {
                        listener.onMetaChanged();
                    }
                }
            } else if (action.equals(MusicPlaybackService.PLAYSTATE_CHANGED)) {
                // Set the play and pause image
                mReference.get().mHeaderPlayPauseButton.updateState();
                mReference.get().mFooterPlayPauseButton.updateState();
                // Refresh the queue
                mReference.get().refreshQueue();
            } else if (action.equals(MusicPlaybackService.REFRESH)) {
                // Let the listener know to update a list
                for (final MusicStateListener listener : mReference.get().mMusicStateListener) {
                    if (listener != null) {
                        listener.restartLoader();
                    }
                }
            } else if (action.equals(MusicPlaybackService.REPEATMODE_CHANGED)
                    || action.equals(MusicPlaybackService.SHUFFLEMODE_CHANGED)) {
                // Set the repeat image
                mReference.get().mFooterRepeatButton.updateRepeatState();
                // Set the shuffle image
                mReference.get().mFooterShuffleButton.updateShuffleState();
            }
        }
    }

    /**
     * @param status The {@link MusicStateListener} to use
     */
    public void setMusicStateListenerListener(final MusicStateListener status) {
        if (status != null) {
            mMusicStateListener.add(status);
        }
    }

    /**
     * @return The resource ID to be inflated.
     */
    public abstract int getContentView();
}