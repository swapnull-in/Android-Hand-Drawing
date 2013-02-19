package com.swap.handdrawing;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class MainActivity extends Activity {
	
	private ImageView eraser;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		final DrawingView drawingView = (DrawingView) findViewById(R.id.drawing);
		
		eraser = (ImageView) findViewById(R.id.eraser);
		eraser.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (drawingView.isEraserActive) {
					drawingView.isEraserActive = false;

					eraser.setImageResource(R.drawable.eraser);

				} else {
					drawingView.isEraserActive = true;

					eraser.setImageResource(R.drawable.pencil);
				}

			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
