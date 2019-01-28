package com.example.android.popularmoviesstage2.Activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.example.android.popularmoviesstage2.Adapters.MovieRecyclerViewAdapter;
import com.example.android.popularmoviesstage2.DataUtils.FavoritesUtils;
import com.example.android.popularmoviesstage2.DataUtils.MoviesDBContract;
import com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils;
import com.example.android.popularmoviesstage2.GeneralUtils.NetworkUtils;
import com.example.android.popularmoviesstage2.MovieData.Movie;
import com.example.android.popularmoviesstage2.MovieData.MovieTrailerThumbnail;
import com.example.android.popularmoviesstage2.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static com.example.android.popularmoviesstage2.DataUtils.FavoritesUtils.SHARED_PREFERENCES_FAVORITES_STRING;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.FAVORITE_MOVIES_LOADER;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.MAIN_SEARCH_LOADER;


public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener,
        MovieRecyclerViewAdapter.MovieAdapterOnClickHandler {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int SPINNER_FAVORITES_POSITION = 2;
    private static final int SPINNER_MOST_POPULAR_POSITION = 0;

    public static final String INTENT_MOVIE_OBJECT_KEY = "movieObject";

    public static final String BUNDLE_MOVIES_ARRAY_KEY = "movies";
    public static final String BUNDLE_CRITERIA_KEY = "criteria";
    public static final String BUNDLE_GRID_SCROLL_KEY = "gridScroll";

    public static final String FAVORITES_CRITERIA_STRING = "Favorites";
    public static final String MOST_POPULAR_CRITERIA_STRING = "Most Popular";
    public static final String TOP_RATED_CRITERIA_STRING = "Top Rated";


    private String mSearchCriteria = "Most Popular";
    private ArrayList<Movie> mMoviesArray = null;

    private String mMoviesArrayString = null;

    private ProgressBar mProgressBar;
    private GridLayoutManager mGridLayoutManager;
    private MovieRecyclerViewAdapter mAdapter;
    private RecyclerView mMainRecyclerView;
    private Spinner mSpinnerView;

    private Parcelable mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_recycler);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        if (savedInstanceState == null
                || !savedInstanceState.containsKey(BUNDLE_MOVIES_ARRAY_KEY)
                || !savedInstanceState.containsKey(BUNDLE_CRITERIA_KEY)
                || !savedInstanceState.containsKey(BUNDLE_GRID_SCROLL_KEY)) {

            makeSearchQuery(mSearchCriteria);

        } else {

            mMoviesArray = savedInstanceState.getParcelableArrayList(BUNDLE_MOVIES_ARRAY_KEY);
            mSearchCriteria = savedInstanceState.getString(BUNDLE_CRITERIA_KEY);

            if (mMoviesArray != null) {
                setMainActivityAdapter();
                restoreScrollPosition(savedInstanceState);
            }
        }
    }

    private void makeSearchQuery(String searchCriteria) {

        if (NetworkUtils.isNetworkAvailable(this)) {
            LoaderManager loaderManager = getSupportLoaderManager();
            Loader<String> searchLoader = loaderManager.getLoader(LoaderUtils.MAIN_SEARCH_LOADER);

            Bundle bundle = new Bundle();
            bundle.putString("searchCriteria", searchCriteria);

            if (searchLoader == null) {
                loaderManager.initLoader(LoaderUtils.MAIN_SEARCH_LOADER, bundle, new InternetMoviesLoader(this));
            } else {
                loaderManager.restartLoader(LoaderUtils.MAIN_SEARCH_LOADER, bundle, new InternetMoviesLoader(this));
            }
        } else {
            NetworkUtils.createNoConnectionDialog(this);
        }
    }

    private void restoreScrollPosition(Bundle savedInstanceState) {
        int position = savedInstanceState.getInt(BUNDLE_GRID_SCROLL_KEY);
        mMainRecyclerView.smoothScrollToPosition(position);
    }


    public void createMovieObjects(String JSONString) {

        JSONObject JSONObject;

        try {
            JSONObject = new JSONObject(JSONString);
            JSONArray resultsArray = JSONObject.optJSONArray("results");

            ArrayList<Movie> movieArray = createMoviesArrayFromJSONArray(resultsArray);

            if (movieArray.size() > 0) {
                mMoviesArray = movieArray;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private ArrayList<Movie> createMoviesArrayFromJSONArray(JSONArray resultsArray) {

        ArrayList<Movie> movieArray = new ArrayList<Movie>();

        for (int i = 0; i < resultsArray.length(); i++) {

            try {
                JSONObject movie = resultsArray.getJSONObject(i);
                Movie movieObject = createMovie(movie);

                if (movieObject != null) {
                    movieArray.add(createMovie(movie));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return movieArray;
    }

    private Movie createMovie(JSONObject movie) {
        try {
            int id = movie.getInt("id");
            String title = movie.getString("title");
            String posterPath = DetailsActivity.MOVIEDB_POSTER_BASE_URL +
                    getString(R.string.poster_size) +
                    movie.getString("poster_path");
            String plot = movie.getString("overview");
            String releaseDate = movie.getString("release_date");
            Double voteAverage = movie.getDouble("vote_average");

            return new Movie(id, title, releaseDate, posterPath, voteAverage, plot,
                    null, 0.0, null, null, false, null, new ArrayList<MovieTrailerThumbnail>());

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setMainActivityAdapter() {

        mMainRecyclerView = (RecyclerView) findViewById(R.id.root_recycler_view);

        setMainActivityLayoutManager();

        mAdapter = new MovieRecyclerViewAdapter(mMoviesArray, mMoviesArray.size(), this, this, mSearchCriteria);

        if(mMoviesArray.size() > 0) {
            mMainRecyclerView.setAdapter(mAdapter);
        }
    }


    private void setMainActivityLayoutManager() {
        int numberOfColumns = MovieRecyclerViewAdapter.calculateColumns(this);
        mGridLayoutManager = new GridLayoutManager(this, numberOfColumns);
        mMainRecyclerView.setLayoutManager(mGridLayoutManager);
    }

    @Override
    public void onClick(Movie movie) {
        Context context = this;
        Class destinationActivity = DetailsActivity.class;

        Intent intent = new Intent(context, destinationActivity);
        intent.putExtra(INTENT_MOVIE_OBJECT_KEY, movie);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        createSpinner(menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

        String searchCriteria = parent.getItemAtPosition(pos).toString();

        if (!mSearchCriteria.equals(searchCriteria)) {

            mSearchCriteria = searchCriteria;

            switch (searchCriteria) {
                case TOP_RATED_CRITERIA_STRING:
                case MOST_POPULAR_CRITERIA_STRING:
                    makeSearchQuery(searchCriteria);
                    break;
                case FAVORITES_CRITERIA_STRING:
                    makeDatabaseQuery();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    private void createSpinner(Menu menu) {

        MenuItem spinner = menu.findItem(R.id.sort_spinner);
        mSpinnerView = (Spinner) spinner.getActionView();

        mSpinnerView.setOnItemSelectedListener(this);

        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.sorting_options_array, R.layout.spinner_item);

        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        mSpinnerView.setAdapter(spinnerAdapter);

        if (NetworkUtils.isNetworkAvailable(this)) {
            mSpinnerView.setSelection(spinnerAdapter.getPosition(mSearchCriteria));
        } else {
            mSpinnerView.setSelection(spinnerAdapter.getPosition(FAVORITES_CRITERIA_STRING));
        }
    }

    @Override
    public void onRestart() {
        super.onRestart();

        if (mMoviesArray == null) {

            if (mSearchCriteria.equals(FAVORITES_CRITERIA_STRING)) {
                makeDatabaseQuery();
            } else {
                makeSearchQuery(mSearchCriteria);
            }
        } else if (!mSearchCriteria.equals(FAVORITES_CRITERIA_STRING)){
            setMainActivityAdapter();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putParcelableArrayList(BUNDLE_MOVIES_ARRAY_KEY, mMoviesArray);
        outState.putString(BUNDLE_CRITERIA_KEY, mSearchCriteria);

        if (mMainRecyclerView != null) {
            outState.putInt(BUNDLE_GRID_SCROLL_KEY, mGridLayoutManager.findFirstVisibleItemPosition());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mState = mGridLayoutManager.onSaveInstanceState();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private class InternetMoviesLoader implements LoaderManager.LoaderCallbacks<String> {

        private Context mContext;

        private InternetMoviesLoader(Context context) {
            mContext = context;
        }

        @Override
        public Loader<String> onCreateLoader(final int id, final Bundle args) {
            return new AsyncTaskLoader<String>(mContext) {

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();

                    if (NetworkUtils.isNetworkAvailable(mContext)) {

                        if (id == MAIN_SEARCH_LOADER) {
                            if (mProgressBar != null && mMoviesArray == null) {
                                mProgressBar.setVisibility(View.VISIBLE);
                            }
                            forceLoad();
                        } else {
                            deliverResult(mMoviesArrayString);
                        }
                    } else if (!mSearchCriteria.equals(FAVORITES_CRITERIA_STRING)) {
                        NetworkUtils.createNoConnectionDialog(mContext);
                        mSpinnerView.setSelection(SPINNER_FAVORITES_POSITION);
                    }
                }

                @Override
                public void deliverResult(String data) {
                    mMoviesArrayString = data;
                    mProgressBar.setVisibility(View.GONE);

                    super.deliverResult(data);
                }

                @Override
                public String loadInBackground() {
                    String searchResults = null;

                    if (id == MAIN_SEARCH_LOADER && !mSearchCriteria.equals(FAVORITES_CRITERIA_STRING)) {

                        try {
                            URL searchURL = NetworkUtils.buildSearchUrl(NetworkUtils.SEARCH_TYPE_GENERAL_DATA, mSearchCriteria, 0);
                            searchResults = NetworkUtils.getResponseFromHttpUrl(searchURL);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return searchResults;
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String data) {

            mMoviesArrayString = data;

            if (loader.getId() == LoaderUtils.MAIN_SEARCH_LOADER && data != null) {

                mProgressBar.setVisibility(View.GONE);
                createMovieObjects(data);
                setMainActivityAdapter();

                if (mState != null) {
                    mGridLayoutManager.onRestoreInstanceState(mState);
                    mState = null;
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    private void makeDatabaseQuery() {

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> searchLoader = loaderManager.getLoader(LoaderUtils.FAVORITE_MOVIES_LOADER);

        Bundle bundle = new Bundle();
        bundle.putString("searchCriteria", FAVORITES_CRITERIA_STRING);

        if (searchLoader == null) {
            loaderManager.initLoader(LoaderUtils.FAVORITE_MOVIES_LOADER, bundle, new DatabaseMoviesLoader(this));
        } else {
            loaderManager.restartLoader(LoaderUtils.FAVORITE_MOVIES_LOADER, bundle, new DatabaseMoviesLoader(this));
        }
    }

    private class DatabaseMoviesLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        private Context mContext;

        private DatabaseMoviesLoader(Context context) {
            mContext = context;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            switch (id) {
                case FAVORITE_MOVIES_LOADER:
                    return new CursorLoader(mContext,
                            MoviesDBContract.FavoriteMoviesEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            MoviesDBContract.FavoriteMoviesEntry._ID);
                default:
                    throw new RuntimeException("Loader not implemented: " + id);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            if (data.getCount() >= 0 && mSearchCriteria.equals(FAVORITES_CRITERIA_STRING)) {
                convertCursorIntoMoviesArray(data);
            }

        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            loader.cancelLoad();
        }
    }

    private void convertCursorIntoMoviesArray(Cursor cursor) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        ArrayList<Movie> moviesDBArray = new ArrayList<Movie>();

        if (cursor.getCount() == 0 || sharedPreferences.getStringSet(SHARED_PREFERENCES_FAVORITES_STRING, null).size() == 0) {

            handleNoFavoriteMoviesSelected();

        } else {

            while (cursor.moveToNext()) {

                String movieDBId = LoaderUtils.getStringFromCursor(cursor,
                        MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_MOVIEDB_ID);

                if(sharedPreferences.getStringSet(SHARED_PREFERENCES_FAVORITES_STRING, null).contains(movieDBId)) {

                    String movieTitle = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_TITLE);
                    String movieReleaseDate = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_RELEASE_DATE);

                    String moviePosterPath = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_POSTER_PATH);

                    String movieDatabasePosterPath = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_POSTER_PATH);


                    String movieVoteAverage = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_VOTE_AVERAGE);
                    String moviePlot = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_PLOT);
                    String movieIsForAdults = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_IS_FOR_ADULTS);

                    String backdropPath = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_BACKDROP);
                    String databaseBackdropPath = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_BACKDROP_PATH);

                    String databaseInternetTrailerThumbnails = LoaderUtils.getStringFromCursor(cursor,
                            MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_TRAILERS_THUMBNAILS);

                    Movie movie = new Movie(
                            Integer.parseInt(movieDBId),
                            movieTitle,
                            movieReleaseDate,
                            moviePosterPath,
                            Double.parseDouble(movieVoteAverage),
                            moviePlot,
                            null,
                            0.0,
                            null,
                            null,
                            Boolean.parseBoolean(movieIsForAdults),
                            backdropPath,
                            null);

                    movie.setMovieDatabasePosterPath(movieDatabasePosterPath);
                    movie.setMovieDatabaseBackdropPath(databaseBackdropPath);
                    movie.setMovieTrailersThumbnails(DetailsActivity.formatTrailersFromDB(databaseInternetTrailerThumbnails));

                    movie.setIsMovieFavorite(true);

                    // Add the movie to an ArrayList
                    moviesDBArray.add(movie);
                }
            }

            mMoviesArray = moviesDBArray;
            setMainActivityAdapter();
        }
    }

    private void handleNoFavoriteMoviesSelected() {
        mMoviesArray = new ArrayList<>();

        FavoritesUtils.createNoFavoritesDialog(this);

        if (NetworkUtils.isNetworkAvailable(this)) {
            mSpinnerView.setSelection(SPINNER_MOST_POPULAR_POSITION);
        }
    }
}

