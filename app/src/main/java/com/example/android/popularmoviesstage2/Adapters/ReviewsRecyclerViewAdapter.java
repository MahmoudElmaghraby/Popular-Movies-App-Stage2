package com.example.android.popularmoviesstage2.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.popularmoviesstage2.MovieData.MovieReview;
import com.example.android.popularmoviesstage2.R;
import com.ms.square.android.expandabletextview.ExpandableTextView;

import java.util.ArrayList;

public class ReviewsRecyclerViewAdapter extends RecyclerView.Adapter<ReviewsRecyclerViewAdapter.ReviewViewHolder> {

    private Context mContext;
    private ArrayList<MovieReview> mMovieReviewsArray;
    private int mNumberOfItems;

    private final SparseBooleanArray mCollapsedStatus;

    public ReviewsRecyclerViewAdapter(ArrayList<MovieReview> reviewsArray, int numberOfItems, Context context) {
        mContext = context;
        mNumberOfItems = numberOfItems;
        mMovieReviewsArray = reviewsArray;
        mCollapsedStatus = new SparseBooleanArray();
    }

    public class ReviewViewHolder extends RecyclerView.ViewHolder {

        private TextView reviewTextView;
        private TextView reviewAuthorView;
        private ExpandableTextView expandTextView;

        public ReviewViewHolder(View itemView) {
            super(itemView);

            reviewAuthorView = itemView.findViewById(R.id.review_author);
            expandTextView = itemView.findViewById(R.id.review_author_expandable_view);
            reviewTextView = itemView.findViewById(R.id.expandable_text);
        }
    }

    @Override
    public ReviewsRecyclerViewAdapter.ReviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        int layoutIdItem = R.layout.review_item;
        boolean shouldAttachToParentImmediately = false;

        LinearLayout view = (LinearLayout) layoutInflater.inflate(layoutIdItem, parent, shouldAttachToParentImmediately);

        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReviewsRecyclerViewAdapter.ReviewViewHolder holder, int position) {

        MovieReview review = mMovieReviewsArray.get(position);

        if(review != null) {
            holder.reviewAuthorView.setText(review.getReviewAuthor());
            holder.expandTextView.setText(review.getReviewText(), mCollapsedStatus, position);
        }
    }

    @Override
    public int getItemCount() {
        return mNumberOfItems;
    }
}
