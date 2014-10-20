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

package org.opensilk.music.api.meta;

import android.content.ComponentName;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by drew on 7/2/14.
 */
public class LibraryInfo implements Parcelable {

    public final String libraryId;
    public final ComponentName libraryComponent;
    public final String currentFolderId;

    public LibraryInfo(String libraryId, ComponentName libraryComponent, String currentFolderId) {
        this.libraryId = libraryId;
        this.libraryComponent = libraryComponent;
        this.currentFolderId = currentFolderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof LibraryInfo)) return false;
        LibraryInfo that = (LibraryInfo) o;
        if (!TextUtils.equals(currentFolderId, that.currentFolderId)) return false;
        if (!libraryComponent.equals(that.libraryComponent)) return false;
        if (!TextUtils.equals(libraryId, that.libraryId)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = libraryId != null ? libraryId.hashCode() : 0;
        result = 31 * result + (libraryComponent != null ? libraryComponent.hashCode() : 0);
        result = 31 * result + (currentFolderId != null ? currentFolderId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "["+libraryId+","+libraryComponent.flattenToString()+","+currentFolderId+"]";
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(libraryId);
        dest.writeString(libraryComponent.flattenToString());
        dest.writeString(currentFolderId);
    }

    private static LibraryInfo fromParcel(Parcel source) {
        final String id = source.readString();
        final ComponentName cmpnt = ComponentName.unflattenFromString(source.readString());
        final String fid = source.readString();
        return new LibraryInfo(id, cmpnt, fid);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LibraryInfo> CREATOR = new Creator<LibraryInfo>() {
        @Override
        public LibraryInfo createFromParcel(Parcel source) {
            return LibraryInfo.fromParcel(source);
        }

        @Override
        public LibraryInfo[] newArray(int size) {
            return new LibraryInfo[size];
        }
    };
}
