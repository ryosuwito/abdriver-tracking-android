package com.abcar.driver;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class BuktiActivity extends AppCompatActivity {
    private Button buttonUpload;
    private ImageView buktiView1,buktiView2,buktiView3,buktiView4;
    private TextView pointTextView;
    private int point;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bukti);
        buttonUpload = (Button)findViewById(R.id.buttonUpload);
        buktiView1 = (ImageView)findViewById(R.id.buktiView1);
        buktiView2 = (ImageView)findViewById(R.id.buktiView2);
        buktiView3 = (ImageView)findViewById(R.id.buktiView3);
        buktiView4 = (ImageView)findViewById(R.id.buktiView4);
        buktiView1.setImageResource(R.drawable.ic_camera_black_24dp);
        buktiView2.setImageResource(R.drawable.ic_camera_black_24dp);
        buktiView3.setImageResource(R.drawable.ic_camera_black_24dp);
        buktiView4.setImageResource(R.drawable.ic_camera_black_24dp);
        pointTextView = (TextView)findViewById(R.id.pointTextView);
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), UploadActivity.class);
                startActivity(i);
            }
        });
        new FetchImageTask().execute();
    }
    public class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                connection.disconnect();
                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
            imageView.getLayoutParams().height = 200;
        }

    }
    private class FetchImageTask extends AsyncTask<URL, Integer, Long> {
        private JSONArray data = null;
        @Override
        protected void onPostExecute(Long aLong) {
            if (data!=null){
                point = data.length()*5000;
                pointTextView.setText("Total Point Anda : "+ String.valueOf(point));
                for(int i=0; i<data.length(); i++){

                    try {
                        switch (i){
                            case 0:
                                new ImageLoadTask(data.get(i).toString(), buktiView1).execute();
                                break;
                            case 1:
                                new ImageLoadTask(data.get(i).toString(), buktiView2).execute();
                                break;
                            case 2:
                                new ImageLoadTask(data.get(i).toString(), buktiView3).execute();
                                break;
                            case 3:
                                new ImageLoadTask(data.get(i).toString(), buktiView4).execute();
                                buttonUpload.setVisibility(View.GONE);
                                break;
                        }
                        Log.i("Response", data.get(i).toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            super.onPostExecute(aLong);
        }

        @Override
        protected Long doInBackground(URL... urls) {
            HttpURLConnection conn = null;
            try {
                String fetchUrl = "http://abreport.abplusscar.com/campaign/bukti/bankdbs/d1198ta/";
                Log.v("murls", fetchUrl);
                URL mUrl = new URL(fetchUrl);
                conn = (HttpURLConnection) mUrl.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                String connMessage = conn.getResponseMessage();
                conn.connect();
                InputStream inputStream;
                if (connMessage.equalsIgnoreCase("OK")) {
                    inputStream = conn.getInputStream();
                }
                else {
                    inputStream = conn.getErrorStream();
                }
                BufferedReader in = new BufferedReader( new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String currentLine;

                while ((currentLine = in.readLine()) != null){
                    response.append(currentLine);
                }
                in.close();
                JSONObject jsonResponse = new JSONObject(response.toString());
                data = jsonResponse.getJSONArray("data");
                Log.i("Response", response.toString());
                conn.disconnect();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                conn.disconnect();
            }
            return null;
        }
    }
}
