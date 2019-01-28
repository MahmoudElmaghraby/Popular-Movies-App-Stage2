package com.example.android.popularmoviesstage2.Activities;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.popularmoviesstage2.DataUtils.DBServiceTasks;
import com.example.android.popularmoviesstage2.DataUtils.FavoritesUtils;
import com.example.android.popularmoviesstage2.DataUtils.ImagesDBUtils;
import com.example.android.popularmoviesstage2.DataUtils.MoviesDBContract;
import com.example.android.popularmoviesstage2.GeneralUtils.FavoritesDataIntentService;
import com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils;
import com.example.android.popularmoviesstage2.GeneralUtils.NetworkUtils;
import com.example.android.popularmoviesstage2.MovieData.Movie;
import com.example.android.popularmoviesstage2.MovieData.MovieReview;
import com.example.android.popularmoviesstage2.MovieData.MovieTrailerThumbnail;
import com.example.android.popularmoviesstage2.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static com.example.android.popularmoviesstage2.Activities.MainActivity.INTENT_MOVIE_OBJECT_KEY;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.CAST_SEARCH_LOADER;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.FAVORITE_MOVIES_LOADER_BY_ID;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.REVIEWS_LOADER;
import static com.example.android.popularmoviesstage2.GeneralUtils.LoaderUtils.TRAILERS_SEARCH_LOADER;
import static com.squareup.picasso.Picasso.with;

public class DetailsActivity extends AppCompatActivity {

    private static final String TAG = DetailsActivity.class.getSimpleName();

    private static final String NOT_AVAILABLE = "Not available";

    public static final String MOVIEDB_POSTER_BASE_URL = "http://image.tmdb.org/t/p/";

    public static final String TRAILER_THUMBNAIL_BASE_PATH = "https://img.youtube.com/vi/";

    private static final String YOUTUBE_BASE_PATH = "https://www.youtube.com/watch?v=";

    private static final int NUMBER_OF_ACTORS_TO_INCLUDE = 5;


    private Context mContext;

    public Movie movieSelected;
    public boolean mIsMovieSelectedFavorite;

    private ImageView mMoviePosterView;
    private TextView mMovieVoteAverageView;
    private TextView mMovieReleaseView;

    private TextView mMoviePlotView;
    private TextView mMovieTitleView;
    private TextView mMovieLanguageView;

    private TextView mMovieRuntimeView;
    private TextView mMovieCastView;
    private ImageView mMovieBackdropView;

    private TextView mReviewsReadMoreView;
    private LinearLayout mMovieDetailsTrailerLinearContainer;
    private FloatingActionButton mFloatingActionButtonFavorite;

    private RelativeLayout mDetailsLayout;
    private ProgressBar mDetailsProgressBar;

    private GradientDrawable mGradient;

    private String mCachedDetails;
    private String mCachedTrailers;
    private String mCachedCast;
    private String mCachedReviews;

    public static Uri mMovieSelectedUri;

    private boolean dataFromDBpopulated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_second);

        mContext = this;

        setupToolbar();
        getViewsReference();
        mMoviePlotView.setMovementMethod(new ScrollingMovementMethod());

        Intent intentThatStartedThisActivity = getIntent();

        if (intentThatStartedThisActivity.hasExtra("movieObject")) {

            movieSelected = intentThatStartedThisActivity.getExtras().getParcelable("movieObject");

            createMovieUri();

            setTitle(movieSelected.getMovieTitle());
            mIsMovieSelectedFavorite = movieSelected.getIsMovieFavorite();

            if (mIsMovieSelectedFavorite) {
                loadDataFromDatabase(LoaderUtils.FAVORITE_MOVIES_LOADER_BY_ID);
            } else {
                loadDataFromInternet(LoaderUtils.DETAILS_SEARCH_LOADER);
            }
        }
    }


    public void getViewsReference() {

        mMoviePosterView = (ImageView) findViewById(R.id.movie_details_poster_view);
        mMovieTitleView = (TextView) findViewById(R.id.movie_details_title_view);
        mMovieVoteAverageView = (TextView) findViewById(R.id.movie_details_vote_view);

        mMovieReleaseView = (TextView) findViewById(R.id.movie_details_release_view);
        mMoviePlotView = (TextView) findViewById(R.id.movie_details_plot_view);
        mMovieLanguageView = (TextView) findViewById(R.id.movie_details_language);

        mMovieRuntimeView = (TextView) findViewById(R.id.movie_details_runtime);
        mMovieBackdropView = (ImageView) findViewById(R.id.movie_details_backdrop);
        mMovieCastView = (TextView) findViewById(R.id.details_cast_text);

        mMovieDetailsTrailerLinearContainer = (LinearLayout) findViewById(R.id.movie_details_trailers_container);
        mFloatingActionButtonFavorite = (FloatingActionButton) findViewById(R.id.favorite_floating_button);
        mDetailsLayout = (RelativeLayout) findViewById(R.id.details_relative_layout);

        mDetailsProgressBar = (ProgressBar) findViewById(R.id.details_progress_bar);
        mReviewsReadMoreView = (TextView) findViewById(R.id.movie_details_reviews_read_more);
    }

    public void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void createMovieUri() {
        mMovieSelectedUri = MoviesDBContract.FavoriteMoviesEntry.CONTENT_URI.buildUpon()
                .appendPath(Integer.toString(movieSelected.getMovieId()))
                .build();
    }

    private void loadDataFromInternet(int loaderID) {

        Bundle detailsBundle = createDetailsBundle();

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> detailsLoader = loaderManager.getLoader(loaderID);

        if (detailsLoader == null) {
            loaderManager.initLoader(loaderID, detailsBundle, new InternetLoader(this));
        } else {
            loaderManager.restartLoader(loaderID, detailsBundle, new InternetLoader(this));
        }
    }

    private Bundle createDetailsBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("movieObject", movieSelected);

        return bundle;
    }

    private class InternetLoader implements LoaderManager.LoaderCallbacks<String> {

        private Context mContext;

        private InternetLoader(Context context) {
            mContext = context;
        }

        @Override
        public Loader<String> onCreateLoader(final int id, final Bundle args) {
            return new AsyncTaskLoader<String>(mContext) {

                @Override
                protected void onStartLoading() {
                    super.onStartLoading();

                    if (args == null) {
                        return;
                    }

                    switch (id) {
                        case LoaderUtils.DETAILS_SEARCH_LOADER:
                            mDetailsProgressBar.setVisibility(View.VISIBLE);
                            determineLoaderAction(mCachedDetails);
                            break;
                        case LoaderUtils.TRAILERS_SEARCH_LOADER:
                            determineLoaderAction(mCachedTrailers);
                            break;
                        case LoaderUtils.CAST_SEARCH_LOADER:
                            determineLoaderAction(mCachedCast);
                            break;
                        case LoaderUtils.REVIEWS_LOADER:
                            determineLoaderAction(mCachedReviews);
                            break;
                    }
                }

                @Override
                public void deliverResult(String data) {

                    switch (id) {
                        case LoaderUtils.DETAILS_SEARCH_LOADER:
                            mCachedDetails = data;
                            break;
                        case LoaderUtils.TRAILERS_SEARCH_LOADER:
                            mCachedTrailers = data;
                            break;
                        case LoaderUtils.CAST_SEARCH_LOADER:
                            mCachedCast = data;
                            break;
                        case LoaderUtils.REVIEWS_LOADER:
                            mCachedReviews = data;
                            break;
                    }

                    mDetailsProgressBar.setVisibility(View.GONE);
                    super.deliverResult(data);
                }

                @Override
                public String loadInBackground() {

                    String searchResults = null;
                    URL searchQueryURL = null;

                    Movie movie = args.getParcelable("movieObject");

                    switch (id) {
                        case LoaderUtils.DETAILS_SEARCH_LOADER:
                            searchQueryURL = NetworkUtils.buildSearchUrl(NetworkUtils.SEARCH_TYPE_DETAILS,
                                    null,
                                    movie.getMovieId());
                            break;
                        case LoaderUtils.TRAILERS_SEARCH_LOADER:
                            searchQueryURL = NetworkUtils.buildSearchUrl(NetworkUtils.SEARCH_TYPE_TRAILERS,
                                    null,
                                    movie.getMovieId());
                            break;
                        case LoaderUtils.CAST_SEARCH_LOADER:
                            searchQueryURL = NetworkUtils.buildSearchUrl(NetworkUtils.SEARCH_TYPE_CAST,
                                    null,
                                    movie.getMovieId());
                            break;
                        case LoaderUtils.REVIEWS_LOADER:
                            searchQueryURL = NetworkUtils.buildSearchUrl(NetworkUtils.SEARCH_TYPE_REVIEWS,
                                    null,
                                    movie.getMovieId());
                            break;
                    }

                    if (searchQueryURL != null) {
                        try {
                            // Request data and save the results
                            searchResults = NetworkUtils.getResponseFromHttpUrl(searchQueryURL);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return searchResults;
                }

                private void determineLoaderAction(String cachedVariableName) {
                    if (cachedVariableName == null) {
                        forceLoad();
                    } else {
                        deliverResult(cachedVariableName);
                    }
                }
            };
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String data) {

            switch (loader.getId()) {

                case LoaderUtils.DETAILS_SEARCH_LOADER:
                    loadDataFromInternet(CAST_SEARCH_LOADER);
                    addMovieDetails(data, movieSelected);
                    mDetailsProgressBar.setVisibility(View.GONE);
                    break;

                case LoaderUtils.CAST_SEARCH_LOADER:
                    loadDataFromInternet(TRAILERS_SEARCH_LOADER);
                    extractMovieCastArrayFromJSON(data);
                    fillMovieData(movieSelected);
                    mDetailsLayout.setVisibility(View.VISIBLE);
                    break;

                case LoaderUtils.TRAILERS_SEARCH_LOADER:
                    createMovieTrailers(data);
                    loadDataFromInternet(REVIEWS_LOADER);
                    break;

                case LoaderUtils.REVIEWS_LOADER:
                    movieSelected.setMovieReviews(ReviewsActivity.formatJSONfromReviewsString(data));
                    addOnClickListenerToFloatingActionButton();
                    mFloatingActionButtonFavorite.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
            //
        }
    }


    private void fillMovieData(Movie movie) {

        // Extract data from the Movie object
        String movieTitle = movie.getMovieTitle();
        String posterPath = movie.getMoviePosterPath();
        Double voteAverage = movie.getMovieVoteAverage();

        String releaseDate = extractReleaseYear(movie.getMovieReleaseDate());
        String moviePlot = movie.getMoviePlot();
        String movieLanguage = movie.getMovieLanguage();

        String movieRuntime = Integer.toString((int) movie.getMovieRuntime());
        boolean isMovieForAdults = movie.getIsMovieForAdults();
        String movieBackdropPath = createFullBackdropPath(this, movie.getMovieBackdropPath());

        addMovieImagesToUI(posterPath, movieBackdropPath);

        setFloatingButtonImage();

        setViewData(mMovieVoteAverageView, voteAverage.toString());
        setViewData(mMovieReleaseView, releaseDate);
        setViewData(mMoviePlotView, moviePlot);

        setViewData(mMovieTitleView, movieTitle);
        setViewData(mMovieLanguageView, movieLanguage);
        setViewData(mMovieRuntimeView, movieRuntime);

        // Append cast to UI
        appendCastToUI(movieSelected.getMovieCast());

        addReadReviewsOnClickListener();
    }

    private void setViewData(TextView view, String value) {
        if (value != null) {
            view.setText(value);
        } else {
            view.setText(NOT_AVAILABLE);
        }
    }


    private void appendCastToUI(ArrayList<String> movieCast) {
        // Add cast
        for (int i = 0; i < NUMBER_OF_ACTORS_TO_INCLUDE; i++) {
            mMovieCastView.append(movieCast.get(i) + "\n");
        }
    }


    private void addMovieImagesToUI(String posterPath, String movieBackdropPath) {

        if (mIsMovieSelectedFavorite) {

            fillMoviePosterDetailsFromDB(ImagesDBUtils.loadImageFromDatabase(
                    this,
                    movieSelected,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_POSTER_PATH,
                    FavoritesUtils.IMAGE_TYPE_POSTER));

            fillMovieBackdropDetailsFromDB(ImagesDBUtils.loadImageFromDatabase(
                    this,
                    movieSelected,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_BACKDROP_PATH,
                    FavoritesUtils.IMAGE_TYPE_BACKDROP));

            displayTrailersBitmapsOnUI(loadMovieTrailersFromDatabase(this, movieSelected),
                    loadTrailerKeysFromDatabase(this));

        } else {
            loadMoviePoster(posterPath);

            loadMovieBackdrop(movieBackdropPath);
        }
    }


    private void fillMoviePosterDetailsFromDB(Bitmap posterImage) {
        mMoviePosterView.setImageBitmap(posterImage);
    }


    private void fillMovieBackdropDetailsFromDB(Bitmap backdropImage) {
        mMovieBackdropView.setImageBitmap(backdropImage);
    }

    private void setFloatingButtonImage() {
        if (mIsMovieSelectedFavorite) {
            mFloatingActionButtonFavorite.setImageResource(R.drawable.heart_pressed_white);
        } else {
            mFloatingActionButtonFavorite.setImageResource(R.drawable.heart_not_pressed);
        }
    }

    private void loadMoviePoster(String posterPath) {
        if (posterPath != null) {
            with(this)
                    .load(posterPath)
                    .placeholder(R.drawable.placeholder)
                    .resize(200, 300)
                    .error(R.drawable.movie_details_error)
                    .into(mMoviePosterView);
        }
    }

    private void loadMovieBackdrop(String backdropPath) {
        if (backdropPath != null) {
            Picasso.with(this)
                    .load(backdropPath)
                    .placeholder(generateGradientDrawable())
                    .error(generateGradientDrawable())
                    .into(mMovieBackdropView);
        }
    }


    private String extractReleaseYear(String releaseDate) {
        return releaseDate.split("-")[0];
    }

    private void createMovieTrailers(String data) {
        try {
            JSONObject trailersJSON = new JSONObject(data);
            JSONArray trailersArray = trailersJSON.getJSONArray("results");


            for (int i = 0; i < trailersArray.length(); i++) {

                JSONObject trailer = trailersArray.getJSONObject(i);
                final String trailerKey = trailer.getString("key");

                ImageView trailerView = createTrailerView(this, mMovieDetailsTrailerLinearContainer, i, trailerKey);

                loadMovieTrailerThumbnail(trailerView, trailerKey);

                setTrailerOnClickListener(this, trailerView, trailerKey);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static ImageView createTrailerView(Context context, LinearLayout container, int index, String trailerKey) {
        ImageView trailerView = new ImageView(context);
        setTrailerViewProperties(context, trailerView, trailerKey);
        container.addView(trailerView, index);

        return trailerView;
    }

    private void loadMovieTrailerThumbnail(ImageView trailerView, String trailerKey) {

        String searchURL = TRAILER_THUMBNAIL_BASE_PATH + trailerKey + "/0.jpg";

        if (movieSelected.getMovieTrailersThumbnails() != null) {
            movieSelected.getMovieTrailersThumbnails().add(new MovieTrailerThumbnail(searchURL, trailerKey));
        }

        Picasso.with(this)
                .load(searchURL)
                .placeholder(generateGradientDrawable())
                .error(generateGradientDrawable())
                .into(trailerView);
    }

    private static void setTrailerOnClickListener(final Context context, ImageView trailerView, final String trailerKey) {
        trailerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchTrailer(context, trailerKey);
            }
        });
    }

    private static void launchTrailer(Context context, String trailerKey) {
        Uri youtubeLink = Uri.parse(YOUTUBE_BASE_PATH + trailerKey);
        Intent intent = new Intent(Intent.ACTION_VIEW, youtubeLink);

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    private void addReadReviewsOnClickListener() {
        mReviewsReadMoreView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = mContext;
                Class destinationActivity = ReviewsActivity.class;

                Intent intent = new Intent(context, destinationActivity);
                intent.putExtra(INTENT_MOVIE_OBJECT_KEY, movieSelected);

                startActivity(intent);
            }
        });
    }

    private void addOnClickListenerToFloatingActionButton() {

        mFloatingActionButtonFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (movieSelected.getIsMovieFavorite()) {
                    mIsMovieSelectedFavorite = false;
                    removeMovieFromFavorites(mContext, movieSelected);
                    updateRemovedFromFavoritesUI();
                } else {
                    mIsMovieSelectedFavorite = true;
                    addMovieToFavorites(mContext, movieSelected);
                    updateAddedToFavoritesUI();
                }
            }
        });
    }

    private static void removeMovieFromFavorites(Context context, Movie movieSelected) {

        movieSelected.setIsMovieFavorite(false);

        FavoritesUtils.removeFavoriteFromSharedPreferences(context, movieSelected);

        Intent intent = new Intent(context, FavoritesDataIntentService.class);
        intent.setAction(DBServiceTasks.ACTION_REMOVE_FAVORITE);
        intent.putExtra(MainActivity.INTENT_MOVIE_OBJECT_KEY, movieSelected);
        context.startService(intent);
    }

    private void updateRemovedFromFavoritesUI() {
        setFloatingButtonImage();
        Toast.makeText(mContext,
                '"' + movieSelected.getMovieTitle() + '"' + " removed from Favorites :-(", // Message
                Toast.LENGTH_SHORT)
                .show();
    }


    private static void addMovieToFavorites(Context context, Movie movieSelected) {

        movieSelected.setIsMovieFavorite(true);

        FavoritesUtils.addFavoriteToSharedPreferences(context, movieSelected);

        FavoritesUtils.addFavoriteToDatabase(context, movieSelected);
    }

    private void updateAddedToFavoritesUI() {
        setFloatingButtonImage();
        Toast.makeText(mContext,
                '"' + movieSelected.getMovieTitle() + '"' + " added to Favorites!",
                Toast.LENGTH_SHORT)
                .show();
    }


    private static void setTrailerViewProperties(Context context, ImageView trailerView, String trailerKey) {

        int width = convertDpToPixels(context.getResources().getInteger(R.integer.trailerWidth), context);
        int height = convertDpToPixels(context.getResources().getInteger(R.integer.trailerHeight), context);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);

        int marginEnd = convertDpToPixels(10, context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            params.setMarginEnd(marginEnd);
        } else {
            params.setMargins(marginEnd, marginEnd, marginEnd, marginEnd);
        }

        if (trailerKey != null) {
            trailerView.setTag(trailerKey);
        }

        trailerView.setLayoutParams(params);

        trailerView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        trailerView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    private static int convertDpToPixels(int dimensionInDp, Context context) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dimensionInDp,
                context.getResources().getDisplayMetrics());
    }

    private GradientDrawable generateGradientDrawable() {

        if (mGradient != null) {
            return mGradient;
        } else {
            GradientDrawable gradient = new GradientDrawable();
            gradient.setShape(GradientDrawable.RECTANGLE);
            gradient.setColor(ContextCompat.getColor(this, R.color.colorPrimary));

            mGradient = gradient;

            return gradient;
        }
    }

    public static void addMovieDetails(String movieDetailsString, Movie movieSelected) {

        JSONObject movieDetailsJSON;

        try {
            movieDetailsJSON = new JSONObject(movieDetailsString);

            movieSelected.setMovieLanguage(movieDetailsJSON.getString("original_language"));
            movieSelected.setMovieRuntime(movieDetailsJSON.getDouble("runtime"));
            movieSelected.setIsMovieForAdults(movieDetailsJSON.getBoolean("adult"));
            movieSelected.setMovieBackdropPath(movieDetailsJSON.getString("backdrop_path"));

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static String createFullBackdropPath(Context context, String backdropPath) {
        return MOVIEDB_POSTER_BASE_URL + context.getString(R.string.backdrop_size) + backdropPath;
    }

    private void extractMovieCastArrayFromJSON(String stringCast) {

        JSONObject jsonCast = null;
        ArrayList<String> castArray = new ArrayList<String>();

        try {
            jsonCast = new JSONObject(stringCast);
            JSONArray arrayJSONCast = jsonCast.getJSONArray("cast");

            for (int i = 0; i < NUMBER_OF_ACTORS_TO_INCLUDE; i++) {
                castArray.add(arrayJSONCast.getJSONObject(i).getString("name"));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        movieSelected.setMovieCast(castArray);
    }


    private void loadDataFromDatabase(int loaderID) {

        Bundle detailsBundle = createDetailsBundle();

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> detailsLoader = loaderManager.getLoader(loaderID);

        if (detailsLoader == null) {
            loaderManager.initLoader(loaderID, detailsBundle, new DatabaseMovieDetailsLoader(this));
        } else {
            loaderManager.restartLoader(loaderID, detailsBundle, new DatabaseMovieDetailsLoader(this));
        }
    }

    private class DatabaseMovieDetailsLoader implements LoaderManager.LoaderCallbacks<Cursor> {

        private Context mContext;

        private DatabaseMovieDetailsLoader(Context context) {
            mContext = context;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            Uri uri = MoviesDBContract.FavoriteMoviesEntry.CONTENT_URI
                    .buildUpon()
                    .appendPath(Integer.toString(movieSelected.getMovieId()))
                    .build();

            switch (id) {
                case FAVORITE_MOVIES_LOADER_BY_ID:
                    return new CursorLoader(mContext,
                            uri,
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

            if (data.getCount() > 0) {

                if(!dataFromDBpopulated) {
                    loadMovieDetailsFromDB(data);
                    fillMovieData(movieSelected);
                    addOnClickListenerToFloatingActionButton();
                    mFloatingActionButtonFavorite.setVisibility(View.VISIBLE);
                }

                mDetailsProgressBar.setVisibility(View.GONE);
                mDetailsLayout.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mDetailsProgressBar.setVisibility(View.GONE);
        }
    }

    private void loadMovieDetailsFromDB(Cursor movieDetailsCursor) {

        while (movieDetailsCursor.moveToNext()) {

            String movieLanguage = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_LANGUAGE);
            String movieRuntime = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_RUNTIME);
            String movieCast = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_CAST);
            String movieReviewsAuthor = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_REVIEWS_AUTHOR);
            String movieReviewsText = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_REVIEWS_TEXT);
            String movieIsForAdults = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_IS_FOR_ADULTS);
            String movieBackdropPath = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_BACKDROP);
            String movieDatabaseBackdropPath = LoaderUtils.getStringFromCursor(movieDetailsCursor,
                    MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_BACKDROP_PATH);

            movieSelected.setMovieLanguage(movieLanguage);
            movieSelected.setMovieRuntime(Double.parseDouble(movieRuntime));
            movieSelected.setIsMovieForAdults(Boolean.parseBoolean(movieIsForAdults));

            movieSelected.setMovieBackdropPath(movieBackdropPath);
            movieSelected.setMovieDatabaseBackdropPath(movieDatabaseBackdropPath);

            fillMovieReviewsFromDB(movieReviewsAuthor, movieReviewsText);
            fillMovieCastFromDB(movieCast);

            dataFromDBpopulated = true;
        }
    }

    public static ArrayList<MovieTrailerThumbnail> formatTrailersFromDB(String trailerThumbnailsString) {

        String[] trailersArray = trailerThumbnailsString.split(FavoritesUtils.CHARACTER_TO_SEPARATE_THUMBNAILS);

        ArrayList<MovieTrailerThumbnail> trailerThumbnailsObjectsArray = new ArrayList<>();

        for(String thumbnail : trailersArray) {

            String[] thumbnailData = thumbnail.split(FavoritesUtils.CHARACTER_TO_SEPARATE_THUMBNAIL_TAG);
            trailerThumbnailsObjectsArray.add(new MovieTrailerThumbnail(thumbnailData[0], thumbnailData[1]));
        }

        return trailerThumbnailsObjectsArray;
    };

    private void fillMovieReviewsFromDB(String movieReviewsAuthor, String movieReviewsText) {

        ArrayList<MovieReview> movieReviewsArrayList = new ArrayList<MovieReview>();

        String[] reviewAuthorsArray = movieReviewsAuthor.split(DBServiceTasks.CHARACTER_SEPARATING_REVIEWS_AUTHORS);
        String[] reviewTextArray = movieReviewsText.split(DBServiceTasks.CHARACTER_SEPARATING_REVIEWS_TEXT);

        for (int i = 0; i < reviewAuthorsArray.length; i++) {
            movieReviewsArrayList.add(new MovieReview(reviewAuthorsArray[i], reviewTextArray[i]));
        }

        movieSelected.setMovieReviews(movieReviewsArrayList);
    }


    private void fillMovieCastFromDB(String movieCast) {

        ArrayList<String> castArray = new ArrayList<>();

        for (String castMember : movieCast.substring(1, movieCast.length() - 1).split(DBServiceTasks.CHARACTER_SEPARATING_CAST_MEMBERS)) {
            castArray.add(castMember);
        }

        movieSelected.setMovieCast(castArray);
    }

    private static ArrayList<Bitmap> loadMovieTrailersFromDatabase(Context context, Movie movieSelected) {

        ArrayList<Bitmap> trailersBitmapArray = new ArrayList<>();

        String[] trailersArray = queryTrailersArray(context);

        int i = 0;
        for (String trailer : trailersArray) {

            String trailerPath = trailer.split(FavoritesUtils.CHARACTER_TO_SEPARATE_THUMBNAIL_TAG)[0];

            trailersBitmapArray.add(ImagesDBUtils.loadImageFromStorage(
                    trailerPath,
                    Integer.toString(movieSelected.getMovieId()),
                    FavoritesUtils.IMAGE_TYPE_TRAILER_THUMBNAIL,
                    i));

            i++;
        }

        return trailersBitmapArray;
    }

    private static ArrayList<String> loadTrailerKeysFromDatabase(Context context) {

        ArrayList<String> trailerTagArray = new ArrayList<String>();

        String[] trailersArray = queryTrailersArray(context);

        for (String trailer : trailersArray) {

            String[] trailerData = trailer.split(FavoritesUtils.CHARACTER_TO_SEPARATE_THUMBNAIL_TAG);

            if (trailerData.length > 1) {
                String trailerKey = trailerData[1];
                trailerTagArray.add(trailerKey);
            } else {
                trailerTagArray.add(null);
            }
        }

        return trailerTagArray;
    }

    public static String[] queryTrailersArray(Context context) {
        String[] posterProjection = {
                MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_TRAILERS_THUMBNAILS
        };

        Cursor trailersPathsCursor = context.getContentResolver().query(
                mMovieSelectedUri,
                posterProjection,
                null,
                null,
                MoviesDBContract.FavoriteMoviesEntry._ID);

        trailersPathsCursor.moveToFirst();

        String trailersDataString = LoaderUtils.getStringFromCursor(trailersPathsCursor,
                MoviesDBContract.FavoriteMoviesEntry.COLUMN_NAME_DATABASE_TRAILERS_THUMBNAILS);

        trailersPathsCursor.close();

        String[] trailersArray = trailersDataString.split(FavoritesUtils.CHARACTER_TO_SEPARATE_THUMBNAILS);

        return trailersArray;
    }


    private void displayTrailersBitmapsOnUI(ArrayList<Bitmap> trailersThumbnails, ArrayList<String> trailersKeys) {

        for (int i = 0; i < trailersThumbnails.size(); i++) {
            ImageView trailerView = new ImageView(this);
            setTrailerViewProperties(this, trailerView, trailersKeys.get(i));

            if (trailersKeys.get(i) != null) {
                setTrailerOnClickListener(this, trailerView, trailersKeys.get(i));
            }

            mMovieDetailsTrailerLinearContainer.addView(trailerView, i);
            trailerView.setImageBitmap(trailersThumbnails.get(i));
        }
    }
}