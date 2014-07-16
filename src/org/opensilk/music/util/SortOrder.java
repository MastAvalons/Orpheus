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

package org.opensilk.music.util;

import android.provider.MediaStore;

/**
 * Created by drew on 7/16/14.
 */
public class SortOrder {
    public static final String LOCAL_ALBUM_SONGS;
    public static final String SONG_GROUP;
    public static final String LAST_ADDED;
    public static final String PLAYLIST_SONGS;

    static {
        LOCAL_ALBUM_SONGS = MediaStore.Audio.Media.TRACK + ", " + MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        SONG_GROUP = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;
        LAST_ADDED = MediaStore.Audio.Media.DATE_ADDED + " DESC";
        PLAYLIST_SONGS = MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER;
    }
}
