/*
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

package org.opensilk.music.ui.profile.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import org.opensilk.music.ui.cards.SongCollectionCard;
import org.opensilk.music.ui.cards.SongPlaylistCard;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.DaggerInjector;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;

/**
 * Created by drew on 2/18/14.
 */
public class PlaylistAdapter extends SongCollectionAdapter {

    private final long playlistId;

    public PlaylistAdapter(Context context, DaggerInjector mInjector,
                           Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder,
                           long playlistId) {
        super(context, mInjector, false, uri, projection, selection, selectionArgs, sortOrder);
        this.playlistId = playlistId;
    }

    @Override
    protected Card getCardFromCursor(Cursor cursor) {
        SongPlaylistCard c = new SongPlaylistCard(getContext(), CursorHelpers.makeLocalSongFromCursor(getContext(), cursor));
        c.setPosition(cursor.getPosition());
        c.setQueryParams(uri, projection, selection, selectionArgs, sortOrder);
        if (useSimpleLayout) {
            c.useSimpleLayout();
        }
        if (playlistId == -2) {
            c.forLastAdded();
        }
        injector.inject(c);
        return c;
    }

}
