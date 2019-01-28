package com.example.android.popularmoviesstage2.GeneralUtils;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.example.android.popularmoviesstage2.Activities.MainActivity;
import com.example.android.popularmoviesstage2.DataUtils.DBServiceTasks;
import com.example.android.popularmoviesstage2.MovieData.Movie;

public class FavoritesDataIntentService extends IntentService {

    public FavoritesDataIntentService() {
        super("FavoritesDataIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        String action;

        if (intent != null) {

            action = intent.getAction();

            if(intent.hasExtra(MainActivity.INTENT_MOVIE_OBJECT_KEY)) {

                Movie movieObject = intent.getParcelableExtra(MainActivity.INTENT_MOVIE_OBJECT_KEY);

                switch (action) {
                    case DBServiceTasks.ACTION_INSERT_FAVORITE:
                    case DBServiceTasks.ACTION_REMOVE_FAVORITE:
                        DBServiceTasks.executeTask(this, action, movieObject);
                        break;
                    default:
                        throw new UnsupportedOperationException("Action not recognized: " + action);
                }
            }
        }
    }
}
