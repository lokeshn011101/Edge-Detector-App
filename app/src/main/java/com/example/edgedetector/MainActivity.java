package com.example.edgedetector;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String DETECT_EDGES_FROM_PHONE_SERVER_URL = "http://192.168.56.1:5000/detect_edges_image_from_phone";
    private static final String DETECT_EDGES_FROM_URL_SERVER_URL = "http://192.168.56.1:5000/detect_edges_image_from_url";
    private String currentPhotoPath = "";
    private final OkHttpClient client = new OkHttpClient();
    private final ActivityResultLauncher<Intent> uploadImageFromCameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    sendImageToServer(currentPhotoPath);
                }
            });
    private final ActivityResultLauncher<Intent> uploadImageFromGalleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String filePath = getPath(data);
                    sendImageToServer(filePath);
                }
            });

    private void sendImageToServer(String filePath) {
        String[] splitFilePath = filePath.split("/");
        String fileName = splitFilePath[splitFilePath.length - 1];

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        BitmapFactory.decodeFile(filePath, options).compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        RequestBody postBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", fileName, RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                .build();

        Request request = new Request.Builder()
                .url(DETECT_EDGES_FROM_PHONE_SERVER_URL)
                .post(postBody)
                .build();
        makeNetworkCall(request);
    }

    private String getPath(Intent data) {
        Uri selectedImage = data.getData();
        String fileId = DocumentsContract.getDocumentId(selectedImage);
        Log.d("App", fileId);
        String id = fileId.split(":")[1];
        String[] column = {MediaStore.Images.Media.DATA};
        String selector = MediaStore.Images.Media._ID + "=?";
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, selector, new String[]{id}, null);
        int columnIndex = cursor.getColumnIndex(column[0]);
        String filePath = "";
        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private void makeNetworkCall(Request request) {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        new Thread(() -> client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, final Response response) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    try {
                        Toast.makeText(getApplicationContext(), response.body().string(), Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Log.d("App", e.toString());
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), "Failed to Connect to Server", Toast.LENGTH_SHORT).show();
                });
            }
        })).start();
    }

    private void sendUrlToServer(String url) {
        RequestBody requestBody = new FormBody.Builder().add("url", url).build();
        Request request = new Request.Builder().post(requestBody).url(DETECT_EDGES_FROM_URL_SERVER_URL).build();
        TextView urlTextView = (TextView) findViewById(R.id.url_text_view);
        urlTextView.setText("");
        makeNetworkCall(request);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mGalleryUploadButton = findViewById(R.id.gallery_upload);
        mGalleryUploadButton.setOnClickListener(view -> {
            Intent selectImageIntent = new Intent();
            selectImageIntent.setType("image/*");
            selectImageIntent.setAction(Intent.ACTION_GET_CONTENT);
            uploadImageFromGalleryActivityResultLauncher.launch(selectImageIntent);
        });
        Button mCameraUploadButton = findViewById(R.id.camera_upload);
        mCameraUploadButton.setOnClickListener(view -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    Log.e("App", ex.toString());
                }
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                            "com.example.edgedetector.provider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    uploadImageFromCameraActivityResultLauncher.launch(takePictureIntent);
                }
            }
        });
        Button mUrlUploadButton = findViewById(R.id.url_upload);
        mUrlUploadButton.setOnClickListener(view -> {
            TextView urlTextView = (TextView) findViewById(R.id.url_text_view);
            String url = urlTextView.getText().toString();
            if (validURL(url)) {
                sendUrlToServer(url);
            }
        });
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private boolean validURL(String url) {
        Pattern urlPattern = Pattern.compile("https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)");
        return urlPattern.matcher(url).matches() && (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png"));
    }

    public void viewEdgeDetectedImages(View view) {
        Intent scrollingIntent = new Intent(MainActivity.this, ScrollingActivity.class);
        startActivity(scrollingIntent);
    }
}