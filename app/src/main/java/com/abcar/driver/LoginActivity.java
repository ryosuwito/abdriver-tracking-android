package com.abcar.driver;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    EditText plat, campaign;
    TextView loginText;
    Button btnLogin;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    private String postUrl, currentLat, currentLong,connMessage,platString, campaignString;
    private HttpURLConnection conn;
    private DataOutputStream os;
    private URL mUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle("Sign In");
        plat = (EditText)findViewById(R.id.platView);
        campaign = (EditText)findViewById(R.id.campaignView);
        loginText = (TextView)findViewById(R.id.loginText);
        btnLogin = (Button)findViewById(R.id.btnLogin);
        postUrl = "http://dev.abplusscar.com/driver/login/";
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        boolean logged_in =sharedPref.getBoolean("logged_in", false);
        if(logged_in == true){
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String credentials = "Mencoba Login...\n";
                credentials += "Campaign Name : " + campaign.getText().toString() + "\n";
                credentials += "Plat No : " + plat.getText().toString();
                platString = plat.getText().toString();
                campaignString = campaign.getText().toString();
                loginText.setText(credentials);
                new LoginActivity.LoginTask().execute(mUrl);
            }
        });
    }
    private class LoginTask extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... urls) {
            Map<String,Object> params = new LinkedHashMap<>();
            params.put("plat", platString);
            params.put("campaign", campaignString);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String,Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                try {
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                postData.append('=');
                try {
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            byte[] postDataBytes = new byte[0];
            try {
                postDataBytes = postData.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                String baseUrl;
                baseUrl = postUrl;
                Log.v("murls", baseUrl);
                mUrl = new URL(baseUrl);
                conn = (HttpURLConnection) mUrl.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());
                connMessage = conn.getResponseMessage();
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
                String token = jsonResponse.getString("token");
                if(!token.equalsIgnoreCase("")){
                    // Store data. you may also use putFloat(), putInt(), putLong() as requirement
                    editor.putString("plat", plat.getText().toString());
                    editor.putString("campaign", campaign.getText().toString());
                    editor.putBoolean("logged_in", true);
                    editor.putString("token", token);
                    editor.commit();
                    Intent i = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(i);

                }
                Log.i("Response", response.toString());
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
