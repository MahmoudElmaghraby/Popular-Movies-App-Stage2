package com.example.android.popularmoviesstage2.GeneralUtils;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.Button;

import com.example.android.popularmoviesstage2.Activities.MainActivity;
import com.example.android.popularmoviesstage2.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


public class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private static final String MOVIEDB_URL = "https://api.themoviedb.org/3/movie/";

    private static final String PARAM_KEY = "api_key";

    public static final String SEARCH_TYPE_GENERAL_DATA = "general";
    public static final String SEARCH_TYPE_DETAILS = "details";
    public static final String SEARCH_TYPE_REVIEWS = "reviews";
    public static final String SEARCH_TYPE_TRAILERS = "trailers";
    public static final String SEARCH_TYPE_CAST = "cast";

    public static URL buildSearchUrl(String searchType, String sortBy, int movieId) {

        URL url = null;

        try {
            Uri buildUri = buildUri(searchType, sortBy, Integer.toString(movieId));
            url = new URL(buildUri.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    private static Uri buildUri(String searchType, String sortBy, String movieId) {

        Uri buildUri;

        String baseUri = buildBaseUri(searchType, sortBy, movieId);

        buildUri = Uri.parse(baseUri).buildUpon()
                .appendQueryParameter(PARAM_KEY, APIKey) // Set your API key here
                .build();

        return buildUri;
    }

    private static String buildBaseUri(String searchType, String sortBy, String movieId) {
        String baseUri;

        switch (searchType) {
            case SEARCH_TYPE_GENERAL_DATA:
                String criteria = determineSearchCriteria(sortBy);
                baseUri = MOVIEDB_URL + criteria;
                break;
            case SEARCH_TYPE_DETAILS:
                baseUri = MOVIEDB_URL + movieId;
                break;
            case SEARCH_TYPE_REVIEWS:
                baseUri = MOVIEDB_URL + movieId + "/reviews";
                break;
            case SEARCH_TYPE_TRAILERS:
                baseUri = MOVIEDB_URL + movieId + "/videos";
                break;
            case SEARCH_TYPE_CAST:
                baseUri = MOVIEDB_URL + movieId + "/credits";
                break;
            default:
                throw new UnsupportedOperationException("Unknown search type: " + searchType);
        }

        return baseUri;
    }

    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();

            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    public static void createNoConnectionDialog(Context context) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.connection_dialog_title))
                .setMessage(context.getString(R.string.connection_dialog_message));
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

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static String determineSearchCriteria(String sortBy) {

        String criteria;

        switch (sortBy) {
            case MainActivity.MOST_POPULAR_CRITERIA_STRING:
                criteria = "popular";
                break;
            case MainActivity.TOP_RATED_CRITERIA_STRING:
                criteria = "top_rated";
                break;
            default:
                criteria = "popular";
                break;
        }

        return criteria;
    }
}
