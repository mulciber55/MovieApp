package com.example.android.moviesapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    ArrayAdapter<String> movieGridAdapter;
    MovieInformation[] movies = new MovieInformation[20];

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //Making Adapter witch connect every strings from NUMBER, with correct TextView on movie_poster layout
        movieGridAdapter = new ImageAdapter(
                getActivity(),
                new ArrayList<String>());
        // connect this fragment class with xml file
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        //make reference to GridView
        GridView gridView = (GridView) rootView.findViewById(R.id.main_grid_view);
        //set Adapter on gridView
        gridView.setAdapter(movieGridAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getActivity(),DetailActivity.class);
                intent.putExtra("TITLE",movies[position].originalTitle);
                intent.putExtra("OVERVIEW",movies[position].overview);
                intent.putExtra("RATE",movies[position].rate);
                intent.putExtra("RELEASE_DATE",movies[position].releaseDate);
                intent.putExtra("POSTER",movies[position].posterURL);
                startActivity(intent);

            }
        });


        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FetchMovies fetchMovies = new FetchMovies();
        fetchMovies.execute("popularity");
    }

    public class ImageAdapter extends ArrayAdapter<String> {
        public ImageAdapter(Context context, ArrayList<String> images) {
            super(context, 0, images);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.movie_poster, parent, false);
            }
            ImageView view = (ImageView) convertView.findViewById(R.id.poster_image_view);
            String url = getItem(position);

            Picasso.with(getContext()).load(url).into(view);
            return view;
        }
    }

    public class MovieInformation {
        public String originalTitle;
        public String overview;
        public String rate;
        public String releaseDate;
        public String posterURL;
    }


    public class FetchMovies extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchMovies.class.getSimpleName();

        /**
         * method parse JSON from API
         *
         * @param jsonString JSON string downloaded from Movie API
         * @return table with poster patch to build URL
         * @throws JSONException
         */
        private String[] getMovieDataFromJson(String jsonString)
                throws JSONException {
            final String RESULTS = "results";
            final String POSTER_PATH = "poster_path";
            //final String TITLE = "title";
            final String ORIGINAL_TITLE = "original_title";
            final String RATING = "vote_average";
            final String RELEASE_DATE = "release_date";
            final String OVERVIEW = "overview";

            JSONObject movieJson = new JSONObject(jsonString);
            JSONArray movieArray = movieJson.getJSONArray(RESULTS);

            String[] resultStr = new String[20];
            for (int i = 0; i < movieArray.length(); i++) {
                resultStr[i] = movieArray.getJSONObject(i).getString(POSTER_PATH);
                movies[i] = new MovieInformation();
                movies[i].originalTitle = movieArray.getJSONObject(i).getString(ORIGINAL_TITLE);
                movies[i].rate = movieArray.getJSONObject(i).getString(RATING);
                movies[i].releaseDate = movieArray.getJSONObject(i).getString(RELEASE_DATE);
                movies[i].overview = movieArray.getJSONObject(i).getString(OVERVIEW);

            }
            return resultStr;
        }

        private String makePostersURLs (String path) {


                final String URL_BASE = "http://image.tmdb.org/t/p/";
                final String SIZE = "w185/";
                String url = URL_BASE + SIZE + path;
                return url;

        }

        @Override
        protected String[] doInBackground(String... param) {

            //needed references to connect with Movie API
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String movieJsonStr = null;

            try {
                //construct URL to connect with Movie API
                final String BASE_URL = "https://api.themoviedb.org/3/discover/movie";
                final String API_KEY = "api_key";
                final String YOUR_API_KEY = "!!!!!!PLACE YOUR KEY HERE!!!!";
                final String SORT = "sort_by";
                final String POPULARITY = "popularity.desc";
                final String RATE = "vote_average.desc";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendQueryParameter(API_KEY, YOUR_API_KEY)
                        .appendQueryParameter(SORT, POPULARITY)
                        .build();
                URL url = new URL(builtUri.toString());

                //Open connection with our url
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                //Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    return null;
                }


                movieJsonStr = buffer.toString();

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            //Now parse JSON
            try {
               return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }





            return null;
        }

        protected void onPostExecute(String[] result) {
            if (result != null) {
                movieGridAdapter.clear();
                for (int i = 0; i < result.length; i++) {
                    String finalResult = makePostersURLs(result[i]);
                    movies[i].posterURL = finalResult;
                    movieGridAdapter.add(finalResult);
                }
            }

        }
    }

}
