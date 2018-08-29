package com.example.imageupload;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    Button GetImageFromGalleryButton, UploadImageOnServerButton;
    ImageView ShowSelectedImage;
    EditText imageName, imageRealPath;

    Bitmap FixBitmap;
    String ImageTag = "image_tag";
    String ImageName = "image_data";
    String ImagePath = "image_path";
    ProgressDialog progressDialog;
    ByteArrayOutputStream byteArrayOutputStream;
    byte[] byteArray;
    String ConvertImage;
    String GetImageNameFromEditText;
    String ImageRealPath;

    HttpURLConnection httpURLConnection;
    URL url;
    OutputStream outputStream;
    BufferedWriter bufferedWriter;
    int RC;
    BufferedReader bufferedReader;
    StringBuilder stringBuilder;
    boolean check = true;

    private int GALLERY = 1, CAMERA = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GetImageFromGalleryButton = (Button) findViewById(R.id.buttonSelect);   //갤러리
        UploadImageOnServerButton = (Button) findViewById(R.id.buttonUpload);   //서버 저장
        ShowSelectedImage = (ImageView) findViewById(R.id.imageView);   //xml -> java: 선택한 이미지 보여주는 이미지뷰
        imageName = (EditText) findViewById(R.id.imageName);    //xml -> java: 선택한 이미지 파일명 출력하는
        imageRealPath = (EditText) findViewById(R.id.imageRealPath);
        byteArrayOutputStream = new ByteArrayOutputStream();    //파일로부터 읽어들인 바이트 배열 출력하여 저장하는 변수

        GetImageFromGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPictureDialog();
            }
        });

        UploadImageOnServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GetImageNameFromEditText = imageName.getText().toString();
                ImageRealPath = imageRealPath.getText().toString();
                UploadImageToServer();
            }
        });

        if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 5);
            }
        }
    }

    private void showPictureDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {"Photo Gallery", "Camera"};

        pictureDialog.setItems(pictureDialogItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        choosePhotoFromGallary();
                        break;
                    case 1:
                        takePhotoFromCamera();
                        break;
                }
            }
        });

        pictureDialog.show();
    }

    public void choosePhotoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == this.RESULT_CANCELED) {
            return;
        }

        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();

                String name = getImageNameToUri(data.getData());
                imageName.setText(name);
                String realPath = getRealPathFromURI(data.getData());
                imageRealPath.setText(realPath);
                try {
                    FixBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
//                    String path = saveImage(bitmap);
//                    Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
                    ShowSelectedImage.setImageBitmap(FixBitmap);
                    UploadImageOnServerButton.setVisibility(View.VISIBLE);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed!", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CAMERA) {
            String name = getImageNameToUri(data.getData());
            imageName.setText(name);
            String realPath = getRealPathFromURI(data.getData());
            imageRealPath.setText(realPath);

            FixBitmap = (Bitmap) data.getExtras().get("data");
            ShowSelectedImage.setImageBitmap(FixBitmap);
            UploadImageOnServerButton.setVisibility(View.VISIBLE);
//            saveImage(thumbnail);
//            Toast.makeText(ShadiRegistrationPart5.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }
    }

    /*이미지 파일명 가져오는 메소드*/
    public String getImageNameToUri(Uri data) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        String imagePath = cursor.getString(column_index);
        String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1, imagePath.length() - 4);

        return imageName;
    }

    /*이미지 파일 경로 가져오는 메소드*/
    private String getRealPathFromURI(Uri data) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(data, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        String imagePath = cursor.getString(column_index);

        return imagePath;
    }

    /*이미지 회전*/
//    private int exifOrientationToDegrees(int exifOrientation) {
//        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
//            return 90;
//        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
//            return 180;
//        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
//            return 270;
//        }
//        return 0;
//    }
//
//    private Bitmap rotate(Bitmap bitmap, float degree) {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(degree);
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//    }

    /*서버로 보내는 메소드*/
    public void UploadImageToServer() {
        FixBitmap.compress(Bitmap.CompressFormat.JPEG, 40, byteArrayOutputStream);  //.compress(): 사진 압축
        byteArray = byteArrayOutputStream.toByteArray();
        ConvertImage = Base64.encodeToString(byteArray, Base64.DEFAULT);

        class AsyncTaskUploadClass extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressDialog = ProgressDialog.show(MainActivity.this, "Image is Uploading", "Please Wait", false, false);
            }

            @Override
            protected void onPostExecute(String string1) {
                super.onPostExecute(string1);
                progressDialog.dismiss();
                Toast.makeText(MainActivity.this, string1, Toast.LENGTH_LONG).show();
            }

            @Override
            protected String doInBackground(Void... params) {
                ImageProcessClass imageProcessClass = new ImageProcessClass();
                HashMap<String, String> HashMapParams = new HashMap<String, String>();
                HashMapParams.put(ImageTag, GetImageNameFromEditText);  //이미지 파일명
                HashMapParams.put(ImagePath, ImageRealPath); //이미지 파일 경로
                HashMapParams.put(ImageName, ConvertImage); //이미지 파일 정보
                String FinalData = imageProcessClass.ImageHttpRequest("http://172.30.1.43/ImageUpload/upload.php", HashMapParams);

                return FinalData;
            }
        }

        AsyncTaskUploadClass AsyncTaskUploadClassOBJ = new AsyncTaskUploadClass();
        AsyncTaskUploadClassOBJ.execute();
    }

    public class ImageProcessClass {
        public String ImageHttpRequest(String requestURL, HashMap<String, String> PData) {
            StringBuilder stringBuilder = new StringBuilder();
            try {
                url = new URL(requestURL);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(20000);
                httpURLConnection.setConnectTimeout(20000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                outputStream = httpURLConnection.getOutputStream();

                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                bufferedWriter.write(bufferedWriterDataFN(PData));
                bufferedWriter.flush();
                bufferedWriter.close();

                outputStream.close();

                RC = httpURLConnection.getResponseCode();
                if (RC == HttpsURLConnection.HTTP_OK) {
                    bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String RC2;
                    while ((RC2 = bufferedReader.readLine()) != null) {
                        stringBuilder.append(RC2);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return stringBuilder.toString();
        }

        private String bufferedWriterDataFN(HashMap<String, String> HashMapParams) throws UnsupportedEncodingException {
            stringBuilder = new StringBuilder();

            for (Map.Entry<String, String> KEY : HashMapParams.entrySet()) {
                if (check) {
                    check = false;
                } else {
                    stringBuilder.append("&");
                }
                stringBuilder.append(URLEncoder.encode(KEY.getKey(), "UTF-8"));
                stringBuilder.append("=");
                stringBuilder.append(URLEncoder.encode(KEY.getValue(), "UTF-8"));
            }

            return stringBuilder.toString();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 5) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Now user should be able to use camera
            } else {
                Toast.makeText(MainActivity.this, "Unable to use Camera..Please Allow us to use Camera", Toast.LENGTH_LONG).show();
            }
        }
    }
}
