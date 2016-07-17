package app.com.example.android.sunshine;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {
    // Fake data to test our adapter
    private String[] fakeData = {"Test1", "Test2", "Test3", "Test4", "Test"};
    private List<String> arrayList = new ArrayList<>(Arrays.asList(fakeData));
    // Our adapter
    private ArrayAdapter<String> arrayAdapter;
    // Our log tag for debugging
    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    public ForecastFragment() {
    }

    /*Creating refresh menu (for debugging purposes only)
            REMOVE LATER
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            new FetchWeatherTask().execute("10303");
        }
        return super.onOptionsItemSelected(item);
    }

    // END REFRESH BUTTON
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Our adapter
        arrayAdapter = new ArrayAdapter<String>(getContext(),
                R.layout.list_item_forecast, // Layout where our textview resides
                R.id.list_item_forecast_textview, // ID of our textview
                arrayList); // Our data

        // Referencing our list view so that we can inflate it and tell it to list to our adapter
        ListView listView = (ListView) rootView.findViewById(R.id.listview_fragment);
        listView.setAdapter(arrayAdapter);
    /*
      -----Handling clicks on ListView items----------
     */
        // Create a new OnItemClickListener Object & override the onItemClick method
        AdapterView.OnItemClickListener messageClickedHandler = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//                Toast myToast = Toast.makeText(getContext(),arrayAdapter.getItem(i), Toast.LENGTH_SHORT);
//                myToast.show();
                Intent forecastDetail = new Intent(getContext(), DetailActivity.class);
                forecastDetail.putExtra(Intent.EXTRA_TEXT, arrayAdapter.getItem(i));
                startActivity(forecastDetail);
            }
        };
        // Pass the OnItemClickListener object we created to our ListView
        listView.setOnItemClickListener(messageClickedHandler);

        return rootView;
    }


    // Helper class to perform network operations on separate thread

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG_FWT = FetchWeatherTask.class.getSimpleName();

        /*
           --- JSON PARSING CODE -----------
         */

        // Code for date/time conversion

        // A helper method to format the time in a user readable fashion
        private String getReadableDateString(long time) {
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        // Helper method to prepare the weather high/lows data for presentation
        private String formatHighLows(double high, double low) {
            // Rounding our temp data for better readability
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        // Now we take the String representing our JSON data and pull the data
        // we need, putting it into an array of Strings

        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException {
            // Names of JSON objects to be extracted from String
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            // Start parsing
            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time(); // import android.text.format.Time
            dayTime.setToNow(); // Set the first day to current date

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now work exclusively in UTC
            dayTime = new Time();

            // Loop through the JSONArray, pulling our data and placing into our String array
            String[] resultStrs = new String[numDays]; // Set our array to hold the number of days requested

            for (int i = 0; i < weatherArray.length(); i++) {
                // Using the format day, description, high/low
                String day, description, highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".

                long dateTime;

                //convert to UTC time
                dateTime = dayTime.setJulianDay(julianStartDay + i);
                day = getReadableDateString(dateTime);

                // get 'description' which is in child object called "weather" which is 1 element long
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // get our high and low temperatures which are in a child object called "temp"
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX); // Get our high temp from "list"
                double low = temperatureObject.getDouble(OWM_MIN); // Get our low temp from "list"
                // Format our high and low temps as Strings
                highAndLow = formatHighLows(high, low);

                resultStrs[i] = day + " - " + description + " - " + highAndLow;


            }

            return resultStrs;
        }

        /*
            ----- END JSON PARSING CODE----------------------
         */

        // Works to get our data from the network and returns a string array with the data in it
        @Override
        protected String[] doInBackground(String... strings) {

            // If there's no zipcode, there's no point in continuing, so return null
            if (strings.length == 0) return null;

            /*
              -----NETWORKING CODE-------------------------
             */
            HttpURLConnection urlConnection = null; // Our connection object
            BufferedReader reader = null; // Buffered reader to read input stream data
            InputStream inputStream = null; // Holds our input stream from web
            String forecastJsonStr = null; // Our raw Json data as a string
            // Our values for the uri builder
            String unit = "metric";
            int days = 7;

            try {
                // Create our URL object by breaking it up into parameters
                final String BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String POSTALCODE_PARAM = "q";
                final String UNITS = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID = "appid";
                // Build our URI first, then convert it to our URL
                Uri builtUri = Uri.parse(BASE_URL).buildUpon().
                        appendQueryParameter(POSTALCODE_PARAM, strings[0]).
                        appendQueryParameter(UNITS, unit).
                        appendQueryParameter(DAYS_PARAM, Integer.toString(days)).
                        appendQueryParameter(APPID, BuildConfig.OPEN_WEATHER_MAP_API_KEY).build();
                URL url = new URL(builtUri.toString());
                // Pass url to our connection object to open a connection to it
                urlConnection = (HttpURLConnection) url.openConnection();
                // Set the method to obtain data
                urlConnection.setRequestMethod("GET");
                // Establish connection
                urlConnection.connect();

                // Obtain data from our connection and hold it in our InputStream object
                inputStream = urlConnection.getInputStream();
                // Create StringBuffer object to continually add in data from InputStream
                StringBuffer buffer = new StringBuffer();

                // Check if there is actually any data, else do nothing
                if (inputStream == null) {
                    return null;
                }
                // Set our BufferedReader object to read data from the InputStream
                reader = new BufferedReader(new InputStreamReader(inputStream));

                // Create a string to hold each line from input stream
                String line;
                // While there is still stuff to read from the input stream, add it to our BufferedReader
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                // Set up for data parsing

                // If there is nothing in the buffer, no point in parsing. Return null
                if (buffer.length() == 0) {
                    return null;
                }
                // Pass the data from our buffer to our string created earlier
                forecastJsonStr = buffer.toString();

            } // Catch any exceptions
            catch (IOException e) {
                Log.e(LOG_TAG_FWT, "Error", e);
            }
            // Close any open connections, checking if the connection is valid in the first place
            finally {
                // Check if our connection is valid. If so, disconnect
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                // Check if we got data from the input stream. If so, try to close it. Otherwise, throw exception
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG_FWT, "Error closing stream", e);
                    }
                }
            }
            /*
                ---END NETWORK CODE------------
             */

            // Finally, we return our data as a String array
            try {
                return getWeatherDataFromJson(forecastJsonStr, days);
            } catch (JSONException e) {
                Log.e(LOG_TAG_FWT, e.getMessage());
                e.printStackTrace();
            }
            // This will only happen if there is some kind of error getting or parsing the JSON data
            return null;
        }

        /*
            Returning our formatted JSON data to the UI thread by overriding onPostExecute and passing it our Json String Array
         */
        @Override
        public void onPostExecute(String[] forecastJsonData) {
            // First, test that our result is not null, only passing it to the main thread if so
            if (forecastJsonData != null) {
                arrayAdapter.clear(); // Clear the adapter of any previous data
                for (String data : forecastJsonData) { // Loop through our array list and adding each node to the adapter
                    arrayAdapter.add(data); // With API 11 and above we don't need to loop, just use addAll method to pass the array
                }
            }
        }

    }
}
