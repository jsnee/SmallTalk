package com.example.smalltalk;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class GameScreen extends Activity {

	protected MediaRecorder mediaRecorder;
	protected static int audioValue = 0;
	protected static int maxAudioValue = 500;
	protected static ProgressBar _progressBar;
	protected static boolean isListening = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		QuestionCategory selectedCategory = new QuestionCategory(intent.getStringExtra(MainActivity.EXTRA_CATEGORY));
		//TextView textView = (TextView) findViewById(R.id.textView1);
		//textView.setText(selectedCategory.categoryTitleAsArray(), 0, selectedCategory.categoryTitleAsArray().length);
		setContentView(R.layout.activity_game_screen);

		_progressBar = (ProgressBar)findViewById (R.id.circularProgressBar);

		_progressBar.setProgress(85);
		
		startListening();
	}
	
	public void toggleListening() {
		if (isListening) {
			stopListening();
		} else {
			startListening();
		}
	}
	
	public void toggleListening(View view) {
		toggleListening();
	}
	
	public void onDestroy() {
		super.onDestroy();
		stopListening();
	}
	
	protected void stopListening() {
		isListening = false;
		mediaRecorder.stop();
		mediaRecorder.reset();
		mediaRecorder.release();
		audioValue = 0;
	}
	
	protected void startListening() {
		mediaRecorder = new MediaRecorder();
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mediaRecorder.setOutputFile("/dev/null");
		try {
			mediaRecorder.prepare();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			System.out.println("Start Recording...");
			mediaRecorder.start();
			isListening = true;
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Error :: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		new Thread(new Runnable() {
			public void run() {
				while(isListening) {
					audioValue = mediaRecorder.getMaxAmplitude();
					int progressLevel = audioValue / maxAudioValue;
					progressLevel = (progressLevel > 100 ? 100:progressLevel);
					_progressBar.setProgress(progressLevel);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.game_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
