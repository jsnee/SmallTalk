package com.example.smalltalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smalltalk.dialogs.AmbientNoiseCaptureDialogFragment;

public class GameScreen extends Activity implements AmbientNoiseCaptureDialogFragment.AmbientNoiseCaptureDialogListener {

	protected static SettingsSingleton appSettings = SettingsSingleton.getSettings();

	protected MediaRecorder mediaRecorder;
	protected static int audioValue = 0;
	protected static int maxAudioValue = 50000;
	protected static long beginDetectionTime;
	protected static ProgressBar _progressBar;
	protected static int quietTickCounts = 0;
	protected static boolean wasLastTickQuiet = false;
	protected boolean isDetectingNoise = false;
	protected boolean isListening = false;
	protected boolean isPlaying = false;
	protected static DialogFragment noiseDetectionDialog;
	protected ArrayList<String> questions = new ArrayList<String>();
	protected int currentQuestionIndex = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		QuestionCategory selectedCategory = new QuestionCategory(intent.getStringExtra(MainActivity.EXTRA_CATEGORY));
		setContentView(R.layout.activity_game_screen);
		this.setTitle(selectedCategory.getCategoryTitle());

		loadQuestions(selectedCategory.categoryName);
		shuffleQuestions();

		_progressBar = (ProgressBar) findViewById(R.id.circularProgressBar);
		_progressBar.setProgress(0);

		startListening();
		updateQuestionDisplay();

		beginDetectionTime = System.currentTimeMillis();
		appSettings.setWeightedAverageAudioValue(mediaRecorder.getMaxAmplitude());

		isDetectingNoise = true;
		noiseDetectionDialog = new AmbientNoiseCaptureDialogFragment();
		noiseDetectionDialog.show(getFragmentManager(), "noise");
	}

	public void advanceQuestion() {
		if (!isPlaying) {
			//return;
		}
		if (questions.size() > currentQuestionIndex + 1) {
			currentQuestionIndex++;
			updateQuestionDisplay();
		}
	}

	public void updateQuestionDisplay() {
		TextView textView = (TextView) findViewById(R.id.textView1);
		char[] question = questions.get(currentQuestionIndex).toCharArray();
		textView.setText(question, 0, question.length);
	}

	public void displayNextQuestionButton() {
		ImageButton playNextButton = (ImageButton) findViewById(R.id.imageButtonPlayNext);
		//playNextButton.setBackgroundResource(R.drawable.skip);
		playNextButton.setImageResource(R.drawable.skip);
		playNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				advanceQuestion();
			}

		});
	}

	public void playOnClick(View view) {
		isPlaying = true;
		startPlaying();
		displayNextQuestionButton();
	}

	public void startPlaying() {
		isPlaying = true;
		shuffleQuestions();
		currentQuestionIndex = 0;
		updateQuestionDisplay();
	}

	public void loadQuestions(String categoryName) {
		questions.clear();
		for (String eachQuestion : getResources().getStringArray(getResources().getIdentifier(categoryName, "array", "com.example.smalltalk"))) {
			questions.add(eachQuestion);
		}
	}

	public void shuffleQuestions() {
		Collections.shuffle(questions);
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
		_progressBar.setProgress(0);
	}

	protected static boolean isQuiet() {
		double thresholdRatio = audioValue / maxAudioValue * 100.0;
		return (thresholdRatio > appSettings.getThresholdRatio());
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
					if (isDetectingNoise) {
						appSettings.refineWeightedAverageAudioValue(audioValue);

						if (System.currentTimeMillis() > beginDetectionTime + 10000) {
							isDetectingNoise = false;
							maxAudioValue = appSettings.getWeightedAverageAudioValue() / 2;
							noiseDetectionDialog.dismiss();
						}
					} else {
						if (isQuiet()) {
							quietTickCounts++;
							wasLastTickQuiet = true;
							int quietSeconds = quietTickCounts * 100 / 1000;
							if (quietSeconds >= appSettings.getTimerSeconds()) {
								quietTickCounts = 0;
								wasLastTickQuiet = false;
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										advanceQuestion();
									}
								});
							}
						} else {
							if (!wasLastTickQuiet) {
								quietTickCounts = 0;
							}
							wasLastTickQuiet = false;
						}
					}
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
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item 
		switch(item.getItemId())
		{
		case  R.id.settings:
			//Settings selected
			startActivity(new Intent(this, SettingScreen.class));
			return true;	                
		default:
			return super.onOptionsItemSelected(item);

		}

	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		if (maxAudioValue == 50000) {
			maxAudioValue = appSettings.getWeightedAverageAudioValue() * 2;
		}
		System.out.println("quit");

	}
}
