package com.example.android.popularmoviesstage2.MovieData;

import android.os.Parcel;
import android.os.Parcelable;

public class MovieTrailerThumbnail implements Parcelable {

    private String thumbnailPath;
    private String thumbnailKey;

    public MovieTrailerThumbnail(String thumbnailPath, String thumbnailKey) {
        this.thumbnailPath = thumbnailPath;
        this.thumbnailKey = thumbnailKey;
    }

    private MovieTrailerThumbnail(Parcel in) {
        thumbnailPath = in.readString();
        thumbnailKey = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(thumbnailPath);
        parcel.writeString(thumbnailKey);
    }

    public static Parcelable.Creator<MovieTrailerThumbnail> CREATOR = new Parcelable.Creator<MovieTrailerThumbnail>() {

        @Override
        public MovieTrailerThumbnail createFromParcel(Parcel parcel) {
            return new MovieTrailerThumbnail(parcel);
        }

        @Override
        public MovieTrailerThumbnail[] newArray(int i) {
            return new MovieTrailerThumbnail[i];
        }
    };

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public String getThumbnailTag() {
        return thumbnailKey;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public void setThumbnailTag(String thumbnailTag) {
        this.thumbnailKey = thumbnailTag;
    }
}
