/*
 * Copyright (c) 2014 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui2.gallery;

import android.os.Parcel;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;
import org.opensilk.music.ui2.profile.PlaylistScreen;
import org.opensilk.music.util.SortOrder;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import mortar.ViewPresenter;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
@Layout(R.layout.gallery_page)
@WithModule(PlaylistsScreen.Module.class)
@GalleryPageTitle(R.string.page_playlists)
public class PlaylistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = GalleryPageView.class
    )
    public static class Module {

        @Provides @Singleton
        public ViewPresenter<GalleryPageView> provideGalleryPagePresenter(Presenter presenter) {
            return presenter;
        }

    }

    @Singleton
    public static class Presenter extends BasePresenter<Playlist> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<Playlist> loader, OverflowHandlers.Playlists popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
        }

        @Override
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.PLAYLIST_SORT_ORDER, SortOrder.PlaylistSortOrder.PLAYLIST_A_Z));
            subscription = loader.getObservable().subscribe(new SimpleObserver<Playlist>() {
                @Override
                public void onNext(Playlist playlist) {
                    addItem(playlist);
                }
                @Override
                public void onCompleted() {
                    if (viewNotNull() && getAdapter().isEmpty()) showEmptyView();
                }
            });
        }

        @Override
        protected void onItemClicked(BaseAdapter.ViewHolder holder, Playlist item) {
            AppFlow.get(holder.itemView.getContext()).goTo(new PlaylistScreen(item));
        }

        @Override
        protected BaseAdapter<Playlist> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        protected boolean isGrid() {
            return preferences.getString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.PLAYLIST_SORT_ORDER, sortOrder);
            reload();
        }

        @Override
        protected void ensureMenu() {
            if (actionBarMenu == null) {
                actionBarMenu = new ActionBarOwner.MenuConfig.Builder()
                        .withMenus(R.menu.playlist_sort_by, R.menu.view_as)
                        .setActionHandler(new Func1<Integer, Boolean>() {
                            @Override
                            public Boolean call(Integer integer) {
                                switch (integer) {
                                    case R.id.menu_sort_by_az:
                                        setNewSortOrder(SortOrder.PlaylistSortOrder.PLAYLIST_A_Z);
                                        return true;
                                    case R.id.menu_sort_by_za:
                                        setNewSortOrder(SortOrder.PlaylistSortOrder.PLAYLIST_Z_A);
                                        return true;
                                    case R.id.menu_sort_by_date_added:
                                        setNewSortOrder(SortOrder.PlaylistSortOrder.PLAYLIST_DATE);
                                        return true;
                                    case R.id.menu_view_as_simple:
                                        preferences.putString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.SIMPLE);
                                        resetRecyclerView();
                                        return true;
                                    case R.id.menu_view_as_grid:
                                        preferences.putString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.GRID);
                                        resetRecyclerView();
                                        return true;
                                    default:
                                        return false;
                                }
                            }
                        })
                        .build();
            }
        }

    }

    static class Adapter extends BaseAdapter<Playlist> {

        Adapter(BasePresenter<Playlist> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Playlist playlist) {
            holder.title.setText(playlist.mPlaylistName);
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
            if (mGridStyle) {
                loadMultiArtwork(artworkRequestor,
                        holder.subscriptions,
                        playlist.mAlbumIds,
                        holder.artwork,
                        holder.artwork2,
                        holder.artwork3,
                        holder.artwork4
                );
            } else {
                LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
                drawable.setText(playlist.mPlaylistName);
                holder.artwork.setImageDrawable(drawable);
            }
        }

        @Override
        protected boolean multiArtwork(int position) {
            return getItem(position).mAlbumIds.length >= 2;
        }
    }

    public static final Creator<PlaylistsScreen> CREATOR = new Creator<PlaylistsScreen>() {
        @Override
        public PlaylistsScreen createFromParcel(Parcel source) {
            PlaylistsScreen s = new PlaylistsScreen();
            s.restoreFromParcel(source);
            return s;
        }

        @Override
        public PlaylistsScreen[] newArray(int size) {
            return new PlaylistsScreen[size];
        }
    };
}
