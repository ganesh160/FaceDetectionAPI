package com.journaldev.facedetectionapi;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;


import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView imageView, imgTakePicture;
    Button btnProcessNext, btnTakePicture;
    TextView txtSampleDesc, txtTakenPicDesc;
    FaceDetector detector;
    Bitmap editedBitmap;
    int currentIndex = 0;
    int[] imageArray;
    //private Uri imageUri;
    private Uri apkURI;
    private static final int REQUEST_WRITE_PERMISSION = 200;
    private static final int CAMERA_REQUEST = 101;

    private static final String SAVED_INSTANCE_URI = "uri";
    private static final String SAVED_INSTANCE_BITMAP = "bitmap";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageArray = new int[]{R.drawable.sample_1, R.drawable.sample_2, R.drawable.sample_3};
        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
//                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_CLASSIFICATIONS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
//                .setMode(FaceDetector.FAST_MODE)
                .build();

        initViews();

    }

    private void initViews() {
        imageView = (ImageView) findViewById(R.id.imageView);
        imgTakePicture = (ImageView) findViewById(R.id.imgTakePic);
        btnProcessNext = (Button) findViewById(R.id.btnProcessNext);
        btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        txtSampleDesc = (TextView) findViewById(R.id.txtSampleDescription);
        txtTakenPicDesc = (TextView) findViewById(R.id.txtTakePicture);

        processImage(imageArray[currentIndex]);
        currentIndex++;

        btnProcessNext.setOnClickListener(this);
        btnTakePicture.setOnClickListener(this);
        imgTakePicture.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnProcessNext:
                imageView.setImageResource(imageArray[currentIndex]);
                processImage(imageArray[currentIndex]);
                if (currentIndex == imageArray.length - 1)
                    currentIndex = 0;
                else
                    currentIndex++;

                break;

            case R.id.btnTakePicture:
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
                break;

            case R.id.imgTakePic:
                ActivityCompat.requestPermissions(MainActivity.this, new
                        String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera();
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {

            try {
                launchMediaScanIntent();
                processCameraPicture();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Failed to load Image", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_CANCELED) {
            imgTakePicture.setImageResource(android.R.drawable.ic_menu_camera);
            btnTakePicture.setText("TAKE PICTURE");
            txtTakenPicDesc.setVisibility(View.GONE);
        }
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(apkURI);
        this.sendBroadcast(mediaScanIntent);
    }


    private void startCamera() {
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent intent;
        File photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
//
//        File photo = new File(this.getFilesDir().toString(), "photo.jpg");
//
//         apkURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photo);
//
//        //intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//        //apkURI = Uri.fromFile(photo);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, apkURI);
//
////        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        startActivityForResult(intent, CAMERA_REQUEST);


        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photo = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
        //apkURI = Uri.fromFile(photo);
        apkURI = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, apkURI);
        startActivityForResult(intent, CAMERA_REQUEST);


    }

//    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        if (apkURI != null) {
//            outState.putParcelable(SAVED_INSTANCE_BITMAP, editedBitmap);
//            outState.putString(SAVED_INSTANCE_URI, apkURI.toString());
//        }
//        super.onSaveInstanceState(outState);
//    }


    private void processImage(int image) {

        Bitmap bitmap = decodeBitmapImage(image);
        if (detector.isOperational() && bitmap != null) {
            editedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                    .getHeight(), Bitmap.Config.RGB_565);
            float scale = getResources().getDisplayMetrics().density;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.GREEN);
            paint.setTextSize((int) (16 * scale));
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(6f);
            Canvas canvas = new Canvas(editedBitmap);
            canvas.drawBitmap(bitmap, 0, 0, paint);


            Frame frame = new Frame.Builder().setBitmap(editedBitmap).build();
            SparseArray<Face> faces = detector.detect(frame);

            txtSampleDesc.setText(null);

            for (int index = 0; index < faces.size(); ++index) {
                Face face = faces.valueAt(index);
                canvas.drawRect(
                        face.getPosition().x,
                        face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);


                canvas.drawText("Face " + (index + 1), face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight(), paint);

                txtSampleDesc.setText(txtSampleDesc.getText() + "FACE " + (index + 1) + "\n");
                txtSampleDesc.setText(txtSampleDesc.getText() + "Smile probability:" + " " + face.getIsSmilingProbability() + "\n");
                txtSampleDesc.setText(txtSampleDesc.getText() + "Left Eye Is Open Probability: " + " " + face.getIsLeftEyeOpenProbability() + "\n");
                txtSampleDesc.setText(txtSampleDesc.getText() + "Right Eye Is Open Probability: " + " " + face.getIsRightEyeOpenProbability() + "\n\n");

                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
                    canvas.drawCircle(cx, cy, 8, paint);
                }


            }

            if (faces.size() == 0) {
                Toast.makeText(this, "Please take a valid photo", Toast.LENGTH_SHORT).show();
                startCamera();
                //txtSampleDesc.setText("Scan Failed: Found nothing to scan");
            } else {
                imageView.setImageBitmap(editedBitmap);
                txtSampleDesc.setText(txtSampleDesc.getText() + "No of Faces Detected: " + " " + String.valueOf(faces.size()));
            }
        } else {
            txtSampleDesc.setText("Could not set up the detector!");
        }
    }

    private Bitmap decodeBitmapImage(int image) {
        int targetW = 300;
        int targetH = 300;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(getResources(), image,
                bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeResource(getResources(), image,
                bmOptions);
    }

    private void processCameraPicture() throws Exception {
        Bitmap tempBitmap = decodeBitmapUri(this, apkURI);


        try {
            ExifInterface ei = new ExifInterface(getFilePathFromURI(MainActivity.this, apkURI));
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

            Bitmap rotatedBitmap = null;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(tempBitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(tempBitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(tempBitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:

                default:
                    rotatedBitmap = tempBitmap;
            }

            if (detector.isOperational() && rotatedBitmap != null) {
                editedBitmap = Bitmap.createBitmap(rotatedBitmap.getWidth(), rotatedBitmap
                        .getHeight(), rotatedBitmap.getConfig());
                float scale = getResources().getDisplayMetrics().density;
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(Color.GREEN);
                paint.setTextSize((int) (16 * scale));
                paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(6f);
                Canvas canvas = new Canvas(editedBitmap);
                canvas.drawBitmap(rotatedBitmap, 0, 0, paint);
                Frame frame = new Frame.Builder().setBitmap(editedBitmap).build();
                SparseArray<Face> faces = detector.detect(frame);
                txtTakenPicDesc.setText(null);

                for (int index = 0; index < faces.size(); ++index) {
                    Face face = faces.valueAt(index);
                /*canvas.drawRect(
                        face.getPosition().x,
                        face.getPosition().y,
                        face.getPosition().x + face.getWidth(),
                        face.getPosition().y + face.getHeight(), paint);


                //canvas.drawText("Face " + (index + 1), face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight(), paint);

                txtTakenPicDesc.setText("FACE " + (index + 1) + "\n");
                txtTakenPicDesc.setText(txtTakenPicDesc.getText() + "Smile probability:" + " " + face.getIsSmilingProbability() + "\n");
                txtTakenPicDesc.setText(txtTakenPicDesc.getText() + "Left Eye Is Open Probability: " + " " + face.getIsLeftEyeOpenProbability() + "\n");
                txtTakenPicDesc.setText(txtTakenPicDesc.getText() + "Right Eye Is Open Probability: " + " " + face.getIsRightEyeOpenProbability() + "\n\n");

                for (Landmark landmark : face.getLandmarks()) {
                    int cx = (int) (landmark.getPosition().x);
                    int cy = (int) (landmark.getPosition().y);
                    //canvas.drawCircle(cx, cy, 8, paint);
                }*/
                }

                if (faces.size() == 0) {
                    Toast.makeText(this, "Please take a valid photo", Toast.LENGTH_SHORT).show();
                    startCamera();
                } else {
                    imgTakePicture.setImageBitmap(editedBitmap);
                    // txtTakenPicDesc.setText(txtTakenPicDesc.getText() + "No of Faces Detected: " + " " + String.valueOf(faces.size()));
                    txtTakenPicDesc.setText("Valid photo");
                }
            } else {
                txtTakenPicDesc.setText("Could not set up the detector!");
            }

        } catch (Exception ex) {

            android.util.Log.e("TAG", ex.getMessage() + "Compressing image error");

        }
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap decodeBitmapUri(Context ctx, Uri uri) throws FileNotFoundException {
        int targetW = 300;
        int targetH = 300;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(ctx.getContentResolver().openInputStream(uri), null, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        return BitmapFactory.decodeStream(ctx.getContentResolver()
                .openInputStream(uri), null, bmOptions);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        detector.release();
    }

    public static String getFilePathFromURI(Context context, Uri contentUri) {
        //copy file and send new file path
        String fileName = getFileName(contentUri);
        if (!TextUtils.isEmpty(fileName)) {
            File copyFile = new File(context.getFilesDir() + File.separator + fileName);
            copy(context, contentUri, copyFile);
            return copyFile.getAbsolutePath();
        }
        return null;
    }

    public static String getFileName(Uri uri) {
        if (uri == null) return null;
        String fileName = null;
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        if (cut != -1) {
            fileName = path.substring(cut + 1);
        }
        return fileName;
    }

    public static void copy(Context context, Uri srcUri, File dstFile) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(srcUri);
            if (inputStream == null) return;
            OutputStream outputStream = new FileOutputStream(dstFile);
            IOUtils.copyStream(inputStream, outputStream);
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath(Context mContext, Uri uri, String type) {
        String document_id = null;
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
            document_id = cursor.getString(0);
            document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        }

        String path = "";
        if (type.equalsIgnoreCase("Photo")) {

            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            cursor = mContext.getContentResolver().query(uri, filePathColumn, null, null, null);

        /*cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);*/
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                cursor.close();
            }
        } else {
            cursor = mContext.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    null, MediaStore.Video.Media._ID + " = ? ", new String[]{document_id}, null);
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                cursor.close();
            }
        }

        if (path.equalsIgnoreCase("")) {
            path = document_id;
        }

        return path;
    }
}
