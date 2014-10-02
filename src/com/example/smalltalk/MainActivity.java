package com.example.smalltalk;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
//comment
public class MainActivity extends Activity {

	protected static int audioValue = 0;
	private MediaRecorder mRecorder;
	private String mFilename;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	public void onClick(View v) {
		mRecorder = new MediaRecorder();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		mRecorder.setOutputFile(mFilename);
		try {
			mRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			System.out.println("****");
			mRecorder.start();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Error :: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}

	}

	OnDataCaptureListener datacaptureListener = new OnDataCaptureListener() {
		@Override
		public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,int samplingRate) {
			System.out.println("1--->");
			//mVisualizerView.updateVisualizer(bytes);
		}

		public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
			System.out.println("2--->");
			//mVisualizerView.updateVisualizer(bytes);
			audioValue = visualizer.getFft(bytes);
		}
	};
}