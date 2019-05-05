package com.deshpande.camerademo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;
import com.androidnetworking.interfaces.UploadProgressListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

import static android.app.Activity.RESULT_OK;

public class MainActivity extends AppCompatActivity {

    private Button takePictureButton;
    private ImageView imageView;
    private Uri file;
    private String base64s;
    private TextView textView;
    private String CurrentPhotoPath;
    private Button process;
    TextToSpeech t1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takePictureButton = (Button) findViewById(R.id.button_image);
        imageView = (ImageView) findViewById(R.id.imageview);
        process = (Button) findViewById(R.id.processButton);

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            takePictureButton.setEnabled(false);
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, 0);
        }
        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPost("http://192.168.1.3:5000/");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                takePictureButton.setEnabled(true);
            }
        }
    }

    public void takePicture(View view) throws IOException {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);

        startActivityForResult(intent, 100);
    }

    private File getOutputMediaFile() throws IOException{
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("CameraDemo", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File image = File.createTempFile(
                "IMG_" + timeStamp + "_",  /* prefix */
                ".jpg",         /* suffix */
                mediaStorageDir      /* directory */
        );
        CurrentPhotoPath = image.getAbsolutePath();
        System.out.println("Photo Path: " + CurrentPhotoPath);

        return image;
    }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 100) {
                if (resultCode == RESULT_OK) {
                    File imageFile = new File(CurrentPhotoPath);
                    try {
                        Bitmap bMap = MediaStore.Images.Media
                                .getBitmap(this.getContentResolver(), Uri.fromFile(imageFile));
                        imageView.setImageURI(null);
                        imageView.setImageBitmap(bMap);
                        base64s = getEncoded64ImageStringFromBitmap(bMap);

                    }
                    catch (IOException e)
                    {}


//                    RequestParams rp = new RequestParams();
//                    rp.add("img",file.getPath());
//                    textView.setText(rp.toString());
//                    HttpUtils.post(url, rp, new JsonHttpResponseHandler() {
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                            // If the response is JSONObject instead of expected JSONArray
//                            Log.d("asd", "---------------- this is response : " + response);
//                            try {
//                                JSONObject serverResp = new JSONObject(response.toString());
//                            } catch (JSONException e) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace();
//                            }
//                        }
//
//                        @Override
//                        public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
//                            // Pull out the first event on the public timeline
//
//                        }
//                    });
//                    Bitmap finalImg = getBitmapFromEncoded64ImageString(base64s);
//                    imageView.setImageURI(null);
//                    imageView.setImageBitmap(finalImg);

                }
            }
        }
    public String getEncoded64ImageStringFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] byteFormat = stream.toByteArray();
        // get the base 64 string
        String imgString = Base64.encodeToString(byteFormat, Base64.NO_WRAP);

        return imgString;
    }
    public Bitmap getBitmapFromEncoded64ImageString(String base64ss) {
        byte[] byteFormat = Base64.decode(base64ss, Base64.DEFAULT);
        // get the base 64 string
        Bitmap decodedByte = BitmapFactory.decodeByteArray(byteFormat, 0, byteFormat.length);

        return decodedByte;
    }
    public void sendPost(final String urlAddress){

        final ProgressDialog progressDoalog = new ProgressDialog(MainActivity.this);
        progressDoalog.setMax(100);
        progressDoalog.setMessage("Please wait...");
        progressDoalog.setTitle("Checking");
        progressDoalog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        Drawable drawable = new ProgressBar(this).getIndeterminateDrawable().mutate();
        drawable.setColorFilter(Color.parseColor("#2764a5"),
                PorterDuff.Mode.SRC_IN);
        progressDoalog.setIndeterminateDrawable(drawable);
        progressDoalog.setCancelable(false);
        progressDoalog.show();



        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    AndroidNetworking.upload(urlAddress)
                            //.addMultipartFile("image",sourceFile)
                            .addMultipartParameter("img", base64s)
                            .build()
                            .setUploadProgressListener(new UploadProgressListener() {
                                @Override
                                public void onProgress(long bytesUploaded, long totalBytes) {
                                    // do anything with progress

                                }
                            })
                            .getAsJSONObject(new JSONObjectRequestListener() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    // do anything with response
//                                    Toast.makeText(getApplicationContext(),"Posted",LENGTH_SHORT).show();
                                    System.out.println("API Response: " + response.toString());
                                    final String img = response.optString("img");
                                    System.out.println("Parsed String: " + img);
                                    String outputPath;
                                    try {
                                        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_PICTURES), "CameraDemo");
                                        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                                        File image = File.createTempFile(
                                                "IMG_" + timeStamp + "_NEW",  /* prefix */
                                                ".jpg",         /* suffix */
                                                mediaStorageDir      /* directory */

                                        );
                                        outputPath = image.getAbsolutePath();
                                        FileOutputStream fos = new FileOutputStream(outputPath);
                                        fos.write(Base64.decode(img, Base64.NO_WRAP));
                                        fos.close();


                                        File outputFile = new File(outputPath);
                                        Bitmap bMap = MediaStore.Images.Media
                                                .getBitmap(MainActivity.this.getContentResolver(), Uri.fromFile(outputFile));
                                        imageView.setImageBitmap(bMap);
                                    }
                                    catch (Exception e){}
                                   // imageView.setImageBitmap(null);
                                    // imageView.setImageBitmap(processedImg);
                                    //final String desc = response.optString("description");
                                   // t1.speak(img , TextToSpeech.QUEUE_FLUSH, null, null);

//                                    t1.speak(desc, TextToSpeech.QUEUE_FLUSH, null, null);

//                                    AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
//                                            .setTitle("Result")
//                                            .setMessage(img)
//                                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
//
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    dialog.dismiss();
//                                                }
//                                            }).create();
//                                    dialog.show();

                                    runOnUiThread(new Runnable(){
                                        public void run() {
                                            progressDoalog.dismiss();
                                        }
                                    });

                                }
                                @Override
                                public void onError(final ANError error) {
                                    // handle error
                                    System.out.println(error.getErrorDetail());
                                    System.out.println(error.getErrorBody());
                                    System.out.println(error.getErrorCode());
                                    //t1.speak("Sorry Error Occurred", TextToSpeech.QUEUE_FLUSH, null, null);
                                    runOnUiThread(new Runnable(){
                                        public void run() {
                                            progressDoalog.dismiss();
                                        }
                                    });
                                }
                            });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

    }
}

class HttpUtils {
    private static final String BASE_URL = "http://api.twitter.com/1/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void getByUrl(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void postByUrl(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}
