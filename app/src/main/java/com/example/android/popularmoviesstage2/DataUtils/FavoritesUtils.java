package com.example.android.popularmoviesstage2.DataUtils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Button;

import com.example.android.popularmoviesstage2.Activities.MainActivity;
import com.example.android.popularmoviesstage2.GeneralUtils.FavoritesDataIntentService;
import com.example.android.popularmoviesstage2.MovieData.Movie;
import com.example.android.popularmoviesstage2.R;

import java.util.HashSet;
import java.util.Set;


public class FavoritesUtils {

    public static final String IMAGE_TYPE_POSTER = "poster";
    public static final String IMAGE_TYPE_BACKDROP = "backdrop";
    public static final String IMAGE_TYPE_TRAILER_THUMBNAIL = "trailerThumbnail";

    public static final String CHARACTER_TO_SEPARATE_THUMBNAIL_TAG = ">";
    public static final String CHARACTER_TO_SEPARATE_THUMBNAILS = "==>";

    public static final String SHARED_PREFERENCES_FAVORITES_STRING = "favoriteMoviesPreferences";

    public static boolean checkIfMovieIsFavorite(Context context, String movieDBId) {

        Uri uri = MoviesDBContract.FavoriteMoviesEntry.CONTENT_URI.buildUpon()
                .appendPath(movieDBId).build();

        Cursor cursor = context.getContentResolver().query(uri,
                null,
                "movieDBId=?",
                new String[]{movieDBId},
                null);

        if(cursor == null) {
            return false;
        } else if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }

        cursor.close();
        return true;
    }

    public static void addFavoriteToDatabase(Context context, Movie movieSelected) {

        Intent intent = new Intent(context, FavoritesDataIntentService.class);
        intent.setAction(DBServiceTasks.ACTION_INSERT_FAVORITE);
        intent.putExtra(MainActivity.INTENT_MOVIE_OBJECT_KEY, movieSelected);

        context.startService(intent);
    }

    public static boolean removeFavoriteFromSharedPreferences(Context context, Movie movieSelected) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return sharedPreferences.getStringSet(SHARED_PREFERENCES_FAVORITES_STRING, null)
                .remove(Integer.toString(movieSelected.getMovieId()));
    }

    public static boolean addFavoriteToSharedPreferences(Context context, Movie movieSelected) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (sharedPreferences.contains(SHARED_PREFERENCES_FAVORITES_STRING)) {
            return sharedPreferences.getStringSet(SHARED_PREFERENCES_FAVORITES_STRING, null)
                    .add(Integer.toString(movieSelected.getMovieId()));

        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            Set<String> stringSet = new HashSet<String>();
            stringSet.add(Integer.toString(movieSelected.getMovieId()));

            editor.putStringSet(SHARED_PREFERENCES_FAVORITES_STRING, stringSet);
            editor.apply();

            return sharedPreferences.contains(SHARED_PREFERENCES_FAVORITES_STRING);
        }
    }

    public static void createNoFavoritesDialog(Context context) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.favorites_dialog_title))
                .setMessage(context.getString(R.string.favorites_dialog_message));
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

        Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        okButton.setTextColor(Color.RED);
    }
}
