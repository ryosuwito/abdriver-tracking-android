package com.abcar.driver;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private Button uploadButton, uploadEditButton;
    int currentId;
    HttpURLConnection connection;
    Bitmap thumbnail;
    DataOutputStream request;
    String boundary, photoUrl;
    URL url = null;
    SharedPreferences sharedPref;
    String postUrl, currentFilePath, imageFileName;
    File destination;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        postUrl = "http://abreport.abplusscar.com/campaign/buktitayang/";
        photoUrl = "http://abreport.abplusscar.com/media/campaign_data_photo/Bukti_Tayang_20190624_105122.jpg";
        imageView = (ImageView)findViewById(R.id.imageView);
        uploadButton = (Button)findViewById(R.id.uploadButton);
        uploadButton.setVisibility(View.GONE);
        uploadEditButton = (Button)findViewById(R.id.uploadEditButton);
        uploadEditButton.setVisibility(View.GONE);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);

        }
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);

        }
        View.OnClickListener imageButton = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentId = v.getId();
                dispatchTakePictureIntent();
            }
        };
        uploadEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage();
            }
        });
        dispatchTakePictureIntent();
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(this, CameraActivity.class);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
//            Bitmap imageBitmap = (Bitmap) extras.get("data");
            String stringBitmap = (String) extras.get("data");
            Log.i("camera", stringBitmap);
            destination = new File(stringBitmap);
            Bitmap imageBitmap = BitmapFactory.decodeFile(stringBitmap);
            thumbnail = fixOrientation(imageBitmap);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            imageFileName = "Bukti_Tayang_" + timeStamp + ".jpg";
            FileOutputStream fo;
            try {
                fo = new FileOutputStream(destination);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentFilePath = destination.getPath();
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageBitmap(thumbnail);
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(destination);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            uploadButton.setVisibility(View.VISIBLE);
            uploadEditButton.setVisibility(View.VISIBLE);
        } else {
        }
    }

    public Bitmap fixOrientation(Bitmap mBitmap) {
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap newMBitmap = Bitmap.createBitmap(mBitmap , 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
        return newMBitmap;
    }

    public void uploadImage(){
        boundary = UUID.randomUUID().toString();
        new UploadImageTask().execute();
    }
    private class UploadImageTask extends AsyncTask<URL, Integer, Long> {
        private int connCode = 0;

        @Override
        protected void onPostExecute(Long aLong) {
            if(connCode==200){
                Toast.makeText(UploadActivity.this,"Upload foto berhasil", Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(UploadActivity.this,"Upload gagal", Toast.LENGTH_LONG).show();
            }
            Intent i = new Intent(UploadActivity.this, BuktiActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
            super.onPostExecute(aLong);
        }

        @Override
        protected Long doInBackground(URL... urls) {
            String license_no = "none";
            try {
                String baseUrl;
                String campaign =sharedPref.getString("campaign", "none");
                license_no =sharedPref.getString("plat", "none");
                baseUrl = postUrl + campaign.replace(" ", "").toLowerCase() +"/";
                Log.v("murls", baseUrl);
                url = new URL(baseUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("file", imageFileName);
                request = new DataOutputStream(connection.getOutputStream());

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"description\"\r\n\r\n");
                request.writeBytes("bukti" + "\r\n");

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"license_no\"\r\n\r\n");
                request.writeBytes(license_no + "\r\n");

                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"photos\"; filename=\"" + imageFileName + "\"\r\n\r\n");

                request.write(FileUtils.readFileToByteArray(destination));
                request.writeBytes("\r\n");

                request.writeBytes("--" + boundary + "--\r\n");
                request.flush();
                request.close();
                connection.connect();
                Log.v("response", connection.getResponseMessage());
                connCode = connection.getResponseCode();
                connection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
