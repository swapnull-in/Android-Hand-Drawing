package com.swap.handdrawing;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener {

    private final String tag = "MainActivity";

    private ImageView eraser;
    private Button btnClear;

    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawingView = (DrawingView) findViewById(R.id.drawing);

        btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(this);

        eraser = (ImageView) findViewById(R.id.eraser);
        eraser.setOnClickListener(this);

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

            if (drawingView.isEraserActive) {

                drawingView.isEraserActive = false;
                eraser.setImageResource(R.drawable.eraser);

            } else {

                drawingView.isEraserActive = true;
                eraser.setImageResource(R.drawable.pencil);
            }

        } else if (v == btnClear) {

            drawingView.reset();

         }


    }
}
