package com.photo.photobomb;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.photo.photobomb.ml.Cartoongan;

import org.tensorflow.lite.support.image.TensorImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int SELECT_PHOTO = 2;
    ImageView imageView;
    Button btn,pcs,gal,clr,save;
    Bitmap imageBitmap=null;
    Uri uri=null;
    int width=0,height=0;
    private String file_path = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();
        imageView = findViewById(R.id.image);
        btn = findViewById(R.id.button);
        pcs = findViewById(R.id.process);
        gal = findViewById(R.id.gallery);
        clr = findViewById(R.id.clear);
        save = findViewById(R.id.save);

        btn.setOnClickListener(b1-> takePicture());
        gal.setOnClickListener(g1-> getImageFromStorage());
        pcs.setOnClickListener(p1-> processImage());
        clr.setOnClickListener(c1 ->imageView.setImageBitmap(null));
        save.setOnClickListener(s1-> {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                requestPermissions();
                return;
            }
            try {
                saveImageToStorage(imageBitmap);
            } catch (IOException e) {}
        });
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED) {
            requestPermissions();
        }
    }

    private void saveImageToStorage(Bitmap bitmap) throws IOException {
        OutputStream imageOutStream;
        String fileName = "Cartoonized_"+System.currentTimeMillis()+".jpg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME,
                    fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + "PhotoBomb");

            Uri uri =
                    getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values);

            imageOutStream = getContentResolver().openOutputStream(uri);

        } else {

            String imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES). toString() + "/PhotoBomb";
            File image = new File(imagesDir, fileName);
            imageOutStream = new FileOutputStream(image);
        }


        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
        Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
        imageOutStream.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
        if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            //getImageFromStorage();
        }
        if (grantResults.length > 0 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            //getImageFromStorage();
        }
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }

    private void getImageFromStorage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent,
                    "Select Picture"), SELECT_PHOTO);
        }
    }

    private void processImage() {
        if(imageBitmap==null){
            Toast.makeText(this, "Please Click a Image", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Cartoongan model = Cartoongan.newInstance(getApplicationContext());
            TensorImage sourceImage = TensorImage.fromBitmap(imageBitmap);

            Cartoongan.Outputs outputs = model.process(sourceImage);
            TensorImage cartoonizedImage = outputs.getCartoonizedImageAsTensorImage();
            Bitmap cartoonizedImageBitmap = cartoonizedImage.getBitmap();

            model.close();
            imageBitmap=Bitmap.createScaledBitmap(cartoonizedImageBitmap, width, height, false);
            imageView.setImageBitmap(imageBitmap);
            model.close();
        } catch (IOException e) {
            Log.e("Exception: ",e.getMessage());
        }
    }

    private void takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions();
            return;
        }
        String fileName = "temp_File";
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        try {
            File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);
            file_path = imageFile.getAbsolutePath();
            uri = FileProvider.getUriForFile(getApplicationContext(), "com.photo.photobomb.fileprovider", imageFile);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode ==SELECT_PHOTO && resultCode == RESULT_OK
                && data!=null && data.getData()!=null) {
            uri = data.getData();
            try{
                imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);
            }catch (IOException e) {
                e.printStackTrace();
            }
            width = imageBitmap.getWidth();
            height = imageBitmap.getHeight();
            imageView.setImageBitmap(imageBitmap);
        }
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageBitmap = BitmapFactory.decodeFile(file_path);
            imageView.setImageBitmap(imageBitmap);
            width = imageBitmap.getWidth();
            height = imageBitmap.getHeight();
        }
    }
}