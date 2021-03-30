package com.furkanavcu.artbook;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddArtActivity extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText TextArtName, TextArtist, TextYear;
    Button buttonSave;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_art);

        imageView = findViewById(R.id.imageView);
        TextArtName = findViewById(R.id.setTextArtName);
        TextArtist = findViewById(R.id.setTextArtist);
        TextYear = findViewById(R.id.setTextYear);
        buttonSave = findViewById(R.id.buttonSave);
        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.matches("new")) {//Demekki yeni bir art ekleniyor
            TextArtName.setText(null);
            TextArtist.setText(null);
            TextYear.setText(null);
            buttonSave.setVisibility(View.VISIBLE);

            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.lina);
            imageView.setImageBitmap(selectImage);
        } else {// Demekki eski bir art görüntülenecek.
            int artId = intent.getIntExtra("artId", 1);
            buttonSave.setVisibility(View.INVISIBLE);
        }
        try {
            int ArtId = intent.getIntExtra("artId", 1);
            Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", new String[]{String.valueOf(ArtId)});

            int artNameIndex = cursor.getColumnIndex("artname");
            int artistNameIndex = cursor.getColumnIndex("paintername");
            int artYearIndex = cursor.getColumnIndex("year");
            int artImage = cursor.getColumnIndex("image");

            while (cursor.moveToNext()) {

                TextArtName.setText(cursor.getString(artNameIndex));
                TextArtist.setText(cursor.getString(artistNameIndex));
                TextYear.setText(cursor.getString(artYearIndex));

                byte[] byteImage = cursor.getBlob(artImage);
                Bitmap bitmap = BitmapFactory.decodeByteArray(byteImage, 0, byteImage.length);
                imageView.setImageBitmap(bitmap);
            }

            cursor.close();


        } catch (Exception e) {
        }
    }


    public void buttonSave(View view) {
        String artName = TextArtName.getText().toString();
        String painterName = TextArtist.getText().toString();
        String year = TextYear.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {
            database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,paintername VARCHAR,year VARCHAR,image BLOB)");


            String sqlString = "INSERT INTO arts(artname, paintername, year, image)VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1, artName);
            sqLiteStatement.bindString(2, painterName);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();
        } catch (Exception e) {

        }

        //finish();
        Intent intent = new Intent(AddArtActivity.this, MainActivity.class);
        intent.addFlags(intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) { //Yatay mı büyük?
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {    //Dikey mi nüyük ?
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void selectImage(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery, 2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery, 2);
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            Uri imageData = data.getData();//Datanın URI'ını veriyor nereye kayıtlı olduğunun yolunu alıyoruz.

            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                } else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                }
                imageView.setImageBitmap(selectedImage);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}