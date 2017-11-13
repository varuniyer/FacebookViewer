package com.example.varuniyer.facebookviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.LayoutDirection;
import android.util.Log;
import android.util.MalformedJsonException;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private EditText search;
    private String[] photoUrls;
    private String[] photoDates;
    private String[] photoNames;
    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FacebookSdk.sdkInitialize(getApplicationContext());

        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton)findViewById(R.id.login_button);
        search = (EditText)findViewById(R.id.searchbox);

        loginButton.setReadPermissions("public_profile user_posts user_photos user_videos");
        search.setVisibility(View.GONE);

        if(AccessToken.getCurrentAccessToken() != null)
            display_content();

        else {
            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    accessToken = loginResult.getAccessToken().toString();
                    display_content();
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException e) {

                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    public void display_content() {
        search.setVisibility(View.VISIBLE);


        /*
         * This GraphRequest gets the user's photos' urls, dates, and names(captions). These can be
         * used later to add the photos to the layout with the rest of the user's posts.
         */

        new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/photos",
            null,
            HttpMethod.GET,
            new GraphRequest.Callback() {
                public void onCompleted(GraphResponse response) {
                Log.i("Photos", response.getJSONObject().toString());
                    try {
                        JSONArray p = response.getJSONObject().getJSONArray("data");
                        photoUrls = new String[p.length()];
                        photoDates = new String[p.length()];
                        photoNames = new String[p.length()];
                        for (int i = 0; i < p.length(); i++) {
                            photoUrls[i] = "https://graph.facebook.com/" +
                                    ((JSONObject) p.get(i)).get("id").toString() +
                                    "?access_token=" + accessToken;
                            photoDates[i] = ((JSONObject) p.get(i)) .get("created_time")
                                                           .toString().substring(0, 10);
                            try {
                                photoNames[i] = ((JSONObject) p.get(i)).get("name").toString();
                            } catch (JSONException j) {
                                photoNames[i] = "";
                            }
                        }
                    } catch (JSONException j) {
                        Log.e("Photo JSON Error", j.toString());
                    }
                }
            }).executeAsync();


        /*
         * This GraphRequest gets each of the user's posts and for each post, creates
         * a single horizontal LinearLayout, linlaysub, containing the user's profile picture,
         * the post's story, and the date it was posted. Then, this new LinearLayout is
         * added to the vertical LinearLayout, linlay, defined in XML. If there was a
         * message with the post, the message in the form of a TextView is added to linlay.
         */

        new GraphRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/feed",
            null,
            HttpMethod.GET,
            new GraphRequest.Callback() {
                public void onCompleted(GraphResponse response) {
                Log.i("JSON response",response.getJSONObject().toString());

                try {
                    JSONArray arr = response.getJSONObject().getJSONArray("data");
                    Log.i("arr", arr.toString());

                    for(int i = 0; i < arr.length(); i++) {
                        String message, story;
                        try {
                            message     = ( (JSONObject)(arr.get(i)) ).get("message").toString();
                        } catch(JSONException j) {
                            message = null;
                        }

                        try {
                            story       = ( (JSONObject)(arr.get(i)) ).get("story").toString();
                        } catch(JSONException j) {
                            story = null;
                        }
                        String datetime = ( (JSONObject)(arr.get(i)) ).get("created_time").toString();
                        String id =       ( (JSONObject)(arr.get(i)) ).get("id").toString();

                        String date = datetime.substring(0,10);
                        ImageView profile = new ImageView(getApplicationContext());
                        LinearLayout linlay = (LinearLayout) (findViewById(R.id.linear_layout));
                        LinearLayout linlaysub = new LinearLayout(getApplicationContext());
                        String profileUrl = "https://graph.facebook.com/" +
                                            id.substring(0,16) +
                                            "/picture?type=large";

                        LinearLayout.LayoutParams lp_llsub = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp_llsub.setMargins(0,0,0,20);
                        lp_llsub.setLayoutDirection(LinearLayout.HORIZONTAL);
                        linlaysub.setLayoutParams(lp_llsub);

                        LinearLayout.LayoutParams lp_profile = new LinearLayout.LayoutParams(100,101);
                        lp_profile.setMargins(0,0,20,0);
                        profile.setLayoutParams(lp_profile);
                        Picasso.with(getApplicationContext()).load(profileUrl).resize(100,101).into(profile);
                        linlaysub.addView(profile);

                        String[] months = {"January", "February", "March", "April", "May", "June",
                                "July", "August", "September", "October", "November", "December"};

                        String day = date.substring(8);
                        String month = months[Integer.parseInt(date.substring(5,7))];
                        String year = date.substring(0,4);

                        TextView s = new TextView(getApplicationContext());
                        LinearLayout.LayoutParams lp_story_datetime = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp_story_datetime.setMargins(10,0,0,0);
                        s.setLayoutParams(lp_story_datetime);
                        s.setTextColor(Color.parseColor("#2277cc"));

                        if(story != null) {
                            s.setText(story + "\n" + month + " " + day + ", " + year);
                        }
                        else {
                            s.setText(month + " " + day + ", " + year);
                        }
                        linlaysub.addView(s);
                        linlay.addView(linlaysub);

                        if(message != null) {
                            TextView m = new TextView(getApplicationContext());
                            LinearLayout.LayoutParams lp_message = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT);
                            lp_message.setMargins(0, 10, 0, 100);
                            m.setLayoutParams(lp_message);
                            m.setTextColor(Color.parseColor("#002040"));
                            m.setText(message);
                            linlay.addView(m);
                        }
                        // to add lower margin that the message would have otherwise added, 20 + 100 = 120

                        else
                            lp_llsub.setMargins(0,0,0,120);

                        /* This next part creates a layout for the photos and for each photo,
                         * creates an ImageView and loads the photo onto the ImageView. Then, each
                         * photo is added to the photoLayout and the photoLayout is added to linlay
                         *
                         * Creating photoLayout works fine, but accessing the photoDates array
                         * causes a memory leak because I am unable to access the value of a global
                         * variable that is used in another Async process. I am yet to fix this
                         * issue but all other parts of the app work.
                         */

                        /*LinearLayout photoLayout = new LinearLayout(getApplicationContext());
                        photoLayout.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
                        LinearLayout.LayoutParams lp_photo = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        lp_photo.setMargins(0,0,0,100);

                        for (i = 0; i < photoDates.length; i++) {
                            if (date.equals(photoDates[i])) {
                                ImageView pic = new ImageView(getApplicationContext());
                                Picasso.with(getApplicationContext()).load(photoUrls[i]).into(pic);
                                photoLayout.addView(pic);
                            }
                        }
                        linlay.addView(photoLayout);*/
                    }


                } catch(org.json.JSONException j) {
                    Log.e("JSON Error", j.toString());
                }
            }
        }).executeAsync();

        /*
         * Checks for keywords: "updated", "and", "by", and "with". In a post's story, names can be
         * found next to these keywords. Facebook uses these keywords to format their posts, so we
         * can use these words to determine where Facebook puts user's names in the story.
         *
         * Each name is added to fullstr after being found in search. Tab separation is used to
         * ensure the user will never be able to write two different names and get a match.
         * Afterwards, we can check to see if the user's query is in either fullstr or in the post's
         * message.
         */
        search.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String query =  search.getText().toString();
                LinearLayout linlay = (LinearLayout)(findViewById(R.id.linear_layout));

                for(int i = 0; i < linlay.getChildCount(); i++) {

                    //If true then we have found a story, which we store in header
                    if(linlay.getChildAt(i) instanceof LinearLayout) {
                        String fullstr = "";
                        String header = ((TextView) ((LinearLayout) linlay.getChildAt(i)).getChildAt(1) )
                                        .getText().toString();
                        String arr[] = header.split(" ");

                        //updated and added show up near the beginning of a post and are preceded
                        //by a person's name
                        if(header.contains("updated") || header.contains("added")) {
                            fullstr += arr[0] + " "  + arr[1] + " \t";
                        }

                        //and is always preceded by a name
                        if(header.contains("and")) {
                            for(int j = 0; j < arr.length; j++) {
                                if(arr[j].equals("and")) {
                                    fullstr += arr[j - 2] + " " + arr[j - 1] + "\t";
                                }
                            }
                        }

                        //both by and to are followed by a name but
                        if(header.contains("by") || header.contains("to")) {
                            for(int j = 0; j < arr.length; j++) {
                                if(arr[j].equals("by")) {
                                    fullstr += arr[j + 1] + " " + arr[j + 2] + "\t";
                                }
                                if(arr[j].equals("to")) {
                                    fullstr += arr[j + 1] + " " + arr[j + 2].substring(0,arr[j + 2].length() - 2) + "\t";
                                }
                            }
                        }

                        //with is always followed by a name but can either be preceded by a name
                        //or by a special dash, so I check to see if the string preceding it starts
                        //with a capitalized letter
                        if(header.contains("with")) {
                            for(int j = 0; j < arr.length; j++) {
                                if(arr[j].equals("with")) {
                                    fullstr += arr[j + 1] + " " + arr[j + 2] + "\t";
                                    if((int)arr[j - 1].charAt(0) > 64 && (int)arr[j - 1].charAt(0) < 91) {
                                        fullstr += arr[j - 2] + " " + arr[j - 1] + "\t";
                                    }
                                }
                            }
                        }

                        /* If the query is found in fullstr, the post is made visible. Otherwise, we
                         * check if there is a message after and see if the query is found there.
                         * If it is, the post is made visible and if it is not, the post is hidden.
                         * Search: Case-Insensitive
                         */
                        if(!(fullstr.toLowerCase()).contains(query.toLowerCase())) {
                            if(i < linlay.getChildCount() - 1) {
                                if(linlay.getChildAt(i + 1) instanceof TextView) {
                                    if(((TextView)(linlay.getChildAt(i + 1))).getText().toString()
                                            .toLowerCase().contains(query.toLowerCase())) {
                                        linlay.getChildAt(i).setVisibility(View.VISIBLE);
                                        linlay.getChildAt(i + 1).setVisibility(View.VISIBLE);
                                    }
                                    else {
                                        linlay.getChildAt(i).setVisibility(View.GONE);
                                        linlay.getChildAt(i + 1).setVisibility(View.GONE);
                                    }
                                }
                                else
                                    linlay.getChildAt(i).setVisibility(View.GONE);
                            }
                            else
                                linlay.getChildAt(i).setVisibility(View.GONE);
                        }
                        else if(i < linlay.getChildCount() - 1 && linlay.getChildAt(i + 1) instanceof TextView) {
                            linlay.getChildAt(i).setVisibility(View.VISIBLE);
                            linlay.getChildAt(i + 1).setVisibility(View.VISIBLE);
                        }
                        else
                            linlay.getChildAt(i).setVisibility(View.VISIBLE);
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });
    }
}
