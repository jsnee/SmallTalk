package edu.drake.smalltalk;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import edu.drake.smalltalk.dialogs.AmbientNoiseCaptureDialogFragment;

public class GameScreen extends Activity implements AmbientNoiseCaptureDialogFragment.AmbientNoiseCaptureDialogListener {

	protected static SettingsSingleton appSettings = SettingsSingleton.getSettings();

	protected MediaRecorder mediaRecorder;
	protected static int audioValue = 0;
	protected static int maxAudioValue = 0;
	protected static int audioTolerance = 5000;
	protected static long beginDetectionTime;
	protected static long beginPlayingTime;
	protected static ProgressBar _progressBar;
	protected static int quietTickCounts = 0;
	protected static boolean wasLastTickQuiet = false;
	protected boolean isDetectingNoise = false;
	protected boolean isListening = false;
	protected boolean isPlaying = false;
	protected static DialogFragment noiseDetectionDialog;
	protected ArrayList<String> questions = new ArrayList<String>();
	protected int currentQuestionIndex = 0;
	protected static String preserveCategory = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		QuestionCategory selectedCategory = new QuestionCategory(preserveCategory);
		setContentView(R.layout.activity_game_screen);
		this.setTitle(selectedCategory.getCategoryTitle());
		ImageView gameBackground = (ImageView) findViewById(R.id.gameBackground);
		gameBackground.setImageBitmap(BitmapUtils.decodeSampledBitmapFromResource(getResources(), R.drawable.game_background, 100, 100));

		loadQuestions(selectedCategory.categoryName);
		shuffleQuestions();

		_progressBar = (ProgressBar) findViewById(R.id.circularProgressBar);
		_progressBar.setProgress(0);

		startListening();
		displayInstructionsText();

		beginDetectionTime = System.currentTimeMillis();
		appSettings.setWeightedAverageAudioValue(mediaRecorder.getMaxAmplitude());

		isDetectingNoise = true;
		noiseDetectionDialog = new AmbientNoiseCaptureDialogFragment();
		noiseDetectionDialog.show(getFragmentManager(), "noise");
	}

	public void advanceQuestion() {
		if (questions.size() > currentQuestionIndex + 1) {
			Toast.makeText(getApplicationContext(), "New Question!", Toast.LENGTH_SHORT).show();
			currentQuestionIndex++;
			updateQuestionDisplay();
		}
	}

	private void enableKeepScreenAwake() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void disableKeepScreenAwake() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void updateQuestionDisplay() {
		TextView textView = (TextView) findViewById(R.id.textView1);
		char[] question = questions.get(currentQuestionIndex).toCharArray();
		textView.setText(question, 0, question.length);
	}
	
	public void displayInstructionsText() {
		TextView textView = (TextView) findViewById(R.id.textView1);
		char[] question = "When you are ready, press the play button!".toCharArray();
		textView.setText(question, 0, question.length);
	}

	protected void displayNextQuestionButton() {
		ImageButton playNextButton = (ImageButton) findViewById(R.id.imageButtonPlayNext);
		playNextButton.setBackgroundResource(R.drawable.skip);
		playNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				skipOnClick(view);
			}

		});
	}
	
	protected void skipOnClick(View view) {
		advanceQuestion();
		quietTickCounts = 0;
		wasLastTickQuiet = false;
	}

	protected void displayStartPlayingButtion() {
		ImageButton playNextButton = (ImageButton) findViewById(R.id.imageButtonPlayNext);
		playNextButton.setBackgroundResource(R.drawable.play);
		playNextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				playOnClick(view);
			}
		});

	}

	public void playOnClick(View view) {
		startPlaying();
		displayNextQuestionButton();
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
		stopPlaying();
	}

	protected void resetStartPlayingTime() {
		beginPlayingTime = System.currentTimeMillis();
	}

	public void startPlaying() {
		resetStartPlayingTime();
		isPlaying = true;
		shuffleQuestions();
		currentQuestionIndex = 0;
		updateQuestionDisplay();
	}

	protected void stopPlaying() {
		isPlaying = false;
		disableKeepScreenAwake();
		displayStartPlayingButtion();
		if (isListening) {
			stopListening();
		}
	}

	protected void stopListening() {
		_progressBar.setBackgroundResource(R.drawable.microphone_button_mute);
		isListening = false;
		mediaRecorder.stop();
		mediaRecorder.reset();
		mediaRecorder.release();
		audioValue = 0;
		_progressBar.setProgress(0);
	}

	protected void startListening() {
		if (isPlaying) {
			resetStartPlayingTime();
		}
		if (isListening) {
			stopListening();
		}
		enableKeepScreenAwake();
		_progressBar.setBackgroundResource(R.drawable.microphone);
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
			mediaRecorder.start();
			isListening = true;
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "Error :: " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
		new Thread(new Runnable() {
			public void run() {
				try {
					while(isListening) {

						audioValue = mediaRecorder.getMaxAmplitude();
						double unitValue = (maxAudioValue - audioTolerance);
						int progressLevel = (int) (audioValue / unitValue * 100);
						progressLevel = (progressLevel > 100 ? 100:progressLevel);
						_progressBar.setProgress(progressLevel);
						if (isDetectingNoise) {
							System.out.println("Still Detecting Noise...");
							if (audioValue > maxAudioValue) {
								maxAudioValue = audioValue;
							}
							appSettings.refineWeightedAverageAudioValue(audioValue);

							if (System.currentTimeMillis() > beginDetectionTime + 10000) {
								isDetectingNoise = false;
								audioTolerance = (int) (appSettings.getWeightedAverageAudioValue());
								noiseDetectionDialog.dismiss();
								appSettings.setWeightedAverageAudioValue(0);
							}
						} else {
							System.out.println("Not Detecting Noise");
							if (progressLevel < appSettings.getThresholdRatio() && isPlaying) {
								System.out.println("Detected Silence");
								quietTickCounts++;
								wasLastTickQuiet = true;
								int quietSeconds = quietTickCounts * 100 / 1000;
								System.out.println("Detected Silence for " + quietSeconds + " seconds");
								if (quietSeconds >= appSettings.getTimerSeconds()) {
									System.out.println("Attempting to advance question");
									quietTickCounts = 0;
									wasLastTickQuiet = false;
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											advanceQuestion();
											alarm();
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
						if (isListening && isPlaying && appSettings.getTimeout() != 0 && System.currentTimeMillis() >= (appSettings.getTimeout() * 60 * 1000 + beginPlayingTime)) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), "Time's Up!", Toast.LENGTH_LONG).show();
									stopPlaying();
								}
							});
						}
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	protected void alarm(){
		SharedPreferences getAlarms = PreferenceManager.
				getDefaultSharedPreferences(getBaseContext());
		String alarms = getAlarms.getString("prefSound", "default ringtone");
		Uri uri = Uri.parse(alarms);
		playSound(this, uri);

		//call mMediaPlayer.stop(); when you want the sound to stop
	}

	protected MediaPlayer mMediaPlayer;
	protected void playSound(Context context, Uri alert) {
		mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.setDataSource(context, alert);
			final AudioManager audioManager = (AudioManager) context
					.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (IOException e) {
			System.out.println("OOPS");
		}
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
		if (audioTolerance == 0) {
			audioTolerance = (int) (appSettings.getWeightedAverageAudioValue() * 1.25);
		}
		System.out.println("quit");

	}
}
