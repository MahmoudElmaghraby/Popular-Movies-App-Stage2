package com.example.android.popularmoviesstage2.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;

import com.example.android.popularmoviesstage2.Adapters.ReviewsRecyclerViewAdapter;
import com.example.android.popularmoviesstage2.MovieData.Movie;
import com.example.android.popularmoviesstage2.MovieData.MovieReview;
import com.example.android.popularmoviesstage2.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReviewsActivity extends AppCompatActivity {

    private static final String TAG = ReviewsActivity.class.getSimpleName();

    private static final String REVIEWS_ACTIVITY_TITLE = "Reviews";

    private Movie mMovieSelected;
    private ArrayList<MovieReview> mMovieReviewsArray;
    private RecyclerView mReviewsRecyclerView;
    private ReviewsRecyclerViewAdapter mReviewsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);

        setTitle(REVIEWS_ACTIVITY_TITLE);

        Intent intentThatStartedThisActivity = getIntent();

        if(intentThatStartedThisActivity.hasExtra(MainActivity.INTENT_MOVIE_OBJECT_KEY)) {

            mMovieSelected = intentThatStartedThisActivity.getExtras().getParcelable(MainActivity.INTENT_MOVIE_OBJECT_KEY);

            mMovieReviewsArray = mMovieSelected.getMovieReviews();

            if(mMovieReviewsArray.size() > 0) {
                setReviewsAdapter();
            } else {
                createNoReviewsDialog(this);
            }
        }
    }

    public static ArrayList<MovieReview> formatJSONfromReviewsString(String reviewsString) {

        ArrayList<MovieReview> movieReviewsArray = new ArrayList<>();

        try {
            JSONObject reviewsJSON = new JSONObject(reviewsString);
            JSONArray reviewsJSONArray = reviewsJSON.optJSONArray("results");

            for(int i = 0; i < reviewsJSONArray.length(); i++) {
                JSONObject reviewObject = reviewsJSONArray.getJSONObject(i);
                String author = reviewObject.getString("author");
                String review = reviewObject.getString("content");

                movieReviewsArray.add(new MovieReview(author, review));
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return movieReviewsArray;
    }

    private void setReviewsAdapter() {

        mReviewsRecyclerView = (RecyclerView) findViewById(R.id.reviews_recycler_view);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mReviewsRecyclerView.setLayoutManager(layoutManager);

        mReviewsAdapter = new ReviewsRecyclerViewAdapter(mMovieReviewsArray, mMovieReviewsArray.size(), this);
        mReviewsRecyclerView.setAdapter(mReviewsAdapter);
    }

    public static void createNoReviewsDialog(final Context context) {

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.no_reviews_dialog_title))
                .setMessage(context.getString(R.string.no_reviews_dialog_message));
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                NavUtils.navigateUpFromSameTask((Activity) context);
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

        Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        okButton.setTextColor(Color.RED);
    }
}
