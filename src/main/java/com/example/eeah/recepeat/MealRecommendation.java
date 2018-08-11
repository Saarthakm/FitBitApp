package com.example.eeah.recepeat;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.net.URL;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Arrays;

public class MealRecommendation extends Fragment implements View.OnClickListener/*,ReportSummary.InterfaceCalorieCommunicator*/ {
//    public ReportSummary.InterfaceCalorieCommunicator interfaceCalorieCommunicator = null;
    String url = "https://api.edamam.com/search";
    private TextView mTextView;
    private ImageView photo;
    private ListView ingredients;
    private TextView calorie;
    private Button instructions;
    private List<String> ingrList;
    private String instructionsURL;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    public long getCalories(){
        return 0;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_meal_recommendation, container, false);

        mTextView = (TextView)view.findViewById(R.id.recipe_photo);
        photo = (ImageView)view.findViewById(R.id.recipe_image);
        ingredients = (ListView)view.findViewById(R.id.ingredients);
        calorie = (TextView) view.findViewById(R.id.calories);
        instructions = (Button) view.findViewById(R.id.instructions);
        instructions.setOnClickListener(this);
        postRequest();
        return view;
    }


    public void postRequest() {
        url +=
         "?q=chicken&app_id=d6fb3429&app_key=b103f6bfc51c35aadda2c7cba12420ec&from=0&to=3&calories=gte%20591,%20lte%20722&health=alcohol-free";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            getResponse((JSONArray) response.get("hits"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                    }
                });

// Access the RequestQueue through your singleton class.
        SingletonClass.getInstance(getContext()).addToRequestQueue(jsObjRequest);
    }
    public void getResponse(JSONArray response){
        JSONObject recipes;
        String imageUrl;
        String [] allIngredients;
        String calories;
        String label_;
        try {
            //Getting values from Edamam's API response.
            recipes = response.getJSONObject(0).getJSONObject("recipe");
            imageUrl = recipes.get("image").toString();
            allIngredients = recipes.get("ingredientLines").toString().split(",");
            calories = (int)(Float.parseFloat((recipes.get("calories")).toString()))+"";//Rounding as int.
            label_ = recipes.get("label").toString();
            instructionsURL = recipes.get("url").toString();

            new DownloadImageTask(photo).execute(imageUrl);
            for(int i=0; i<allIngredients.length; i++){
                allIngredients[i] = "-"+allIngredients[i].replaceAll("\\[|\\]|\\\"","");
            }

            //Adding values to the view.
            ingrList = new ArrayList<String>(Arrays.asList(allIngredients));
            calorie.setText("Total Calories in Recipe: "+calories);
            mTextView.setText(label_);
            setListView();

        } catch (Exception e) {
            e.printStackTrace();
        }
//        calorie.setText(calorie.getText()+" My calories: "+interfaceCalorieCommunicator.getCalories());
    }


    @Override
    public void onClick(View view) {

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse(instructionsURL));
        startActivity(intent);

    }
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
    public void setListView(){
    // Create an ArrayAdapter from List
    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
            (getActivity(), android.R.layout.simple_list_item_1, ingrList);
    // DataBind ListView with items from ArrayAdapter
        ingredients.setAdapter(arrayAdapter);
    }
//    @Override
//    public void onAttach(Activity activity){
//        super.onAttach(activity);
//        try{
//            interfaceCalorieCommunicator =  (ReportSummary.InterfaceCalorieCommunicator) activity;
//        }catch (ClassCastException e){
//            throw new ClassCastException(activity.toString()+"Must Implement");
//        }
//    }
}