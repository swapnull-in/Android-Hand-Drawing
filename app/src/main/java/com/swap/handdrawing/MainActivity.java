package com.swap.handdrawing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends Activity implements OnClickListener {

    private final String tag = "MainActivity";

    private ImageView eraser;
    private Button btnChooseImage;
    private ImageButton btnClear, btnSave, btnShare, btnCamera;

    private DrawingView drawingView;

    private static final int SELECT_PHOTO = 100;
    private static final int CAMERA_REQUEST = 1888;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            setContentView(R.layout.activity_main);

            drawingView = (DrawingView) findViewById(R.id.drawing);

            btnChooseImage = (Button) findViewById(R.id.btnChooseImage);
            btnChooseImage.setOnClickListener(this);

            btnClear = (ImageButton) findViewById(R.id.btnClear);
            btnClear.setOnClickListener(this);

            btnSave = (ImageButton) findViewById(R.id.btnSave);
            btnSave.setOnClickListener(this);

            btnShare = (ImageButton) findViewById(R.id.btnShare);
            btnShare.setOnClickListener(this);

            btnCamera = (ImageButton) findViewById(R.id.btnCamera);
            btnCamera.setOnClickListener(this);

            eraser = (ImageView) findViewById(R.id.eraser);
            eraser.setOnClickListener(this);
        }
    }


//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.activity_main, menu);
//		return true;
//	}


    @Override
    public void onClick(View v) {

        if (v == eraser) {

            if (drawingView.isEraserActive()) {

                drawingView.deactivateEraser();

                eraser.setImageResource(R.drawable.eraser);

            } else {

                drawingView.activateEraser();

                eraser.setImageResource(R.drawable.pencil);
            }

        } else if (v == btnClear) {

            drawingView.reset();
            drawingView.setBackground(null);

        } else if (v == btnSave) {

            saveImage();

        } else if (v == btnCamera) {

            Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(cameraIntent, CAMERA_REQUEST);

        } else if (v == btnShare) {

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");

            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(saveImage().getAbsolutePath())); //"file:///sdcard/temporary_file.jpg"
            startActivity(Intent.createChooser(share, "Share Image"));

        } else if (v == btnChooseImage) {

            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);

        }

    }


    public File saveImage() {
        drawingView.setDrawingCacheEnabled(true);
        Bitmap bm = drawingView.getDrawingCache();

        File fPath = Environment.getExternalStorageDirectory();

        File f = null;

        f = new File(fPath, UUID.randomUUID().toString() + ".png");

        try {
            FileOutputStream strm = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 80, strm);
            strm.close();

            Toast.makeText(getApplicationContext(), "Image is saved successfully.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return f;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {

            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    InputStream imageStream = null;
                    try {
                        imageStream = getContentResolver().openInputStream(selectedImage);
                        Bitmap bitmap = BitmapFactory.decodeStream(imageStream);

                        BitmapDrawable ob = new BitmapDrawable(getResources(), bitmap);

                        drawingView.setBackground(ob);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case CAMERA_REQUEST:
                if (resultCode == RESULT_OK) {

                    Bitmap photo = (Bitmap) imageReturnedIntent.getExtras().get("data");

                    BitmapDrawable ob = new BitmapDrawable(getResources(), photo);

                    drawingView.setBackground(ob);

                }
        }
    }


}
