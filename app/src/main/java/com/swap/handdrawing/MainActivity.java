package com.swap.handdrawing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity implements OnClickListener {

    private final String tag = "MainActivity";

    private ImageView eraser;
    private Button btnClear, btnSave, btnShare;

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState == null) {

            setContentView(R.layout.activity_main);

            drawingView = (DrawingView) findViewById(R.id.drawing);

            btnClear = (Button) findViewById(R.id.btnClear);
            btnClear.setOnClickListener(this);

            btnSave = (Button) findViewById(R.id.btnSave);
            btnSave.setOnClickListener(this);

            btnShare = (Button) findViewById(R.id.btnShare);
            btnShare.setOnClickListener(this);

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

        } else if (v == btnSave) {

            saveImage();

        } else if (v == btnShare) {

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");

            share.putExtra(Intent.EXTRA_STREAM, Uri.parse(saveImage().getAbsolutePath())); //"file:///sdcard/temporary_file.jpg"
            startActivity(Intent.createChooser(share, "Share Image"));
        }
    }


    public File saveImage()
    {
        drawingView.setDrawingCacheEnabled(true);
        Bitmap bm = drawingView.getDrawingCache();

        File fPath = Environment.getExternalStorageDirectory();

        File f = null;

        f = new File(fPath,  UUID.randomUUID().toString()+".png");

        try {
            FileOutputStream strm = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 80, strm);
            strm.close();

            Toast.makeText(getApplicationContext(), "Image is saved successfully.", Toast.LENGTH_SHORT).show();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        return f;
    }

}
