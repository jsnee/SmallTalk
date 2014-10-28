package edu.drake.smalltalk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
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
import android.speech.tts.TextToSpeech;
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
	
	protected static final int millisecondsPerTick = 100;

	protected MediaRecorder mediaRecorder;
	protected static int audioValue = 0;
	protected static int maxAudioValue = 0;
	protected static int audioTolerance = 5000;
	protected static long beginDetectionTime;
	protected static long beginPlayingTime;
	protected static ProgressBar _progressBar;
	protected static int quietTickCounts = 0;
	protected static int totalContinuousQuietTickCounts = 0;
	protected static boolean wasLastTickQuiet = false;
	protected boolean isDetectingNoise = false;
	protected boolean isListening = false;
	protected boolean isPlaying = false;
	protected static DialogFragment noiseDetectionDialog;
	protected ArrayList<String> questions = new ArrayList<String>();
	protected int currentQuestionIndex = 0;
	protected static String preserveCategory = null;
	protected TextToSpeech speechObj;
	protected boolean textToSpeechEnabled = false;
	protected AlertDialog initDialog;
	protected Thread mainThread;
	protected static ArrayList<Integer> audioLevels = new ArrayList<Integer>();
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
		
		Builder initDialogBuilder = new AlertDialog.Builder(this).setTitle("Initializing Speech...").setMessage("Please wait while Text-to-Speech gets set up...").setIcon(android.R.drawable.ic_dialog_alert);
		initDialog = initDialogBuilder.create();

		//initializeMainThread();

		startListening();
		displayInstructionsText();

		beginDetectionTime = System.currentTimeMillis();
		appSettings.setWeightedAverageAudioValue(mediaRecorder.getMaxAmplitude());

		isDetectingNoise = true;
		noiseDetectionDialog = new AmbientNoiseCaptureDialogFragment();
		noiseDetectionDialog.show(getFragmentManager(), "noise");
	}

	public void onDestroy() {
		super.onDestroy();
		stopTextToSpeech();
		stopPlaying();
	}
	
	public void initializeMainThread() {
		mainThread = new Thread(new Runnable() {
			public void run() {
				try {
					while(isListening) {

						audioValue = mediaRecorder.getMaxAmplitude();
						if (audioLevels.size() < 100) {
							audioLevels.add(audioValue);
						}
						else {
							audioLevels.remove(0);
							audioLevels.add(audioValue);
						}
						double unitValue = (maxAudioValue - audioTolerance);
						int progressLevel = (int) (audioValue / unitValue * 100);
						progressLevel = (progressLevel > 100 ? 100:progressLevel);
						_progressBar.setProgress(progressLevel);
						if (isDetectingNoise) {
							if (audioValue > maxAudioValue) {
								maxAudioValue = audioValue;
							}
							appSettings.refineWeightedAverageAudioValue(audioValue);

							if (System.currentTimeMillis() > beginDetectionTime + 10000) {
								isDetectingNoise = false;
								audioTolerance = (int) (appSettings.getWeightedAverageAudioValue());
								noiseDetectionDialog.dismiss();
								appSettings.setWeightedAverageAudioValue(0);
								checkTextToSpeechEnabled();
							}
						} else {
							if (progressLevel < appSettings.getThresholdRatio() && isPlaying) {
								if (!isAppMakingNoise()) {
									quietTickCounts++;
									totalContinuousQuietTickCounts++;
								}
								wasLastTickQuiet = true;
								int quietSeconds = quietTickCounts * millisecondsPerTick / 1000;
								System.out.println("Detected Silence for " + quietSeconds + " seconds");
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
									if(!isAppMakingNoise() && quietTickCounts == 0) {
										totalContinuousQuietTickCounts = 0;
									}
									quietTickCounts = 0;
								}
								wasLastTickQuiet = false;
							}
						}
						if (isListening && isPlaying && appSettings.getTimeout() != 0 && (totalContinuousQuietTickCounts * millisecondsPerTick / 1000 / 60) >= appSettings.getTimeout()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									stopPlaying();
								}
							});
						}
						try {
							Thread.sleep(millisecondsPerTick);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (!isListening) {
							_progressBar.setProgress(0);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/* Text-To-Speech Section */
	
	protected void startTextToSpeech() {
		speechObj = new TextToSpeech(
			getApplicationContext(),
			new TextToSpeech.OnInitListener() {

				@Override
				public void onInit(int status) {
					if (status != TextToSpeech.ERROR) {
						speechObj.setLanguage(Locale.US);
						hideSpeechInitDialog();
					}
				}
			});
		textToSpeechEnabled = true;
	}
	
	protected boolean checkTextToSpeechEnabled() {
		if (textToSpeechEnabled && !appSettings.isTextToSpeechEnabled()) {
			stopTextToSpeech();
		} else if (!textToSpeechEnabled && appSettings.isTextToSpeechEnabled()) {
			showSpeechInitDialog();
			startTextToSpeech();
		}
		return textToSpeechEnabled;
	}
	
	protected void stopTextToSpeech() {
		if (speechObj != null) {
			speechObj.stop();
			speechObj.shutdown();
		}
		textToSpeechEnabled = false;
	}
	
	protected void speakText() {
		if (textToSpeechEnabled) {
			String toSpeak = ((TextView)findViewById(R.id.questionTextView)).getText().toString();
			System.out.println("Trying to say: " + toSpeak);
			speechObj.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
		}
	}
	
	protected void showSpeechInitDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				initDialog.show();
			}
		});
	}
	
	protected void hideSpeechInitDialog() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				initDialog.dismiss();
			}
		});
	}
	
	/* Screen Wake Utilities */

	private void enableKeepScreenAwake() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void disableKeepScreenAwake() {
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	/* Question Managment Section */

	public void advanceQuestion() {
		if (questions.size() > currentQuestionIndex + 1) {
			Toast.makeText(getApplicationContext(), "New Question!", Toast.LENGTH_SHORT).show();
			currentQuestionIndex++;
			updateQuestionDisplay();
			if (appSettings.isTextToSpeechEnabled()) {
				speakText();
			} else {
				playNotificationSound();
			}
		}
	}
	
	private void loadQuestionsFromCategory(String categoryName) {
		System.out.println("Category: " + categoryName);
		for (String eachQuestion : getResources().getStringArray(getResources().getIdentifier(categoryName, "array", "edu.drake.smalltalk"))) {
			questions.add(eachQuestion);
		}
	}

	public void loadQuestions(String categoryName) {
		questions.clear();
		if (QuestionCategory.isQuestionCategoryName(categoryName)) {
			for (String eachCategory : getResources().getStringArray(R.array.categories)) {
				loadQuestionsFromCategory((new QuestionCategory(eachCategory)).getCategoryName());
			}
		} else {
			loadQuestionsFromCategory(categoryName);
		}
	}

	public void shuffleQuestions() {
		Collections.shuffle(questions);
	}
	
	/* Game Screen Layout Section */

	public void displayInstructionsText() {
		TextView textView = (TextView) findViewById(R.id.questionTextView);
		char[] question = "When you are ready, press the play button!".toCharArray();
		textView.setText(question, 0, question.length);
	}

	public void updateQuestionDisplay() {
		TextView textView = (TextView) findViewById(R.id.questionTextView);
		char[] question = questions.get(currentQuestionIndex).toCharArray();
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

	/* Button Click Events Section */
	
	protected void skipOnClick(View view) {
		advanceQuestion();
		quietTickCounts = 0;
		wasLastTickQuiet = false;
	}

	public void playOnClick(View view) {
		startPlaying();
		displayNextQuestionButton();
	}
	
	/* Playing Section */

	public void startPlaying() {
		resetStartPlayingTime();
		isPlaying = true;
		shuffleQuestions();
		currentQuestionIndex = 0;
		updateQuestionDisplay();
		speakText();
	}

	protected void stopPlaying() {
		isPlaying = false;
		disableKeepScreenAwake();
		displayStartPlayingButtion();
		if (isListening) {
			stopListening();
		}
	}

	protected void resetStartPlayingTime() {
		beginPlayingTime = System.currentTimeMillis();
	}
	
	/* Listening Section */

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
						if (audioValue > maxAudioValue) {
							maxAudioValue = audioValue;
						}
						if (isDetectingNoise) {
							appSettings.refineWeightedAverageAudioValue(audioValue);

							if (System.currentTimeMillis() > beginDetectionTime + 10000) {
								isDetectingNoise = false;
								audioTolerance = (int) (appSettings.getWeightedAverageAudioValue());
								noiseDetectionDialog.dismiss();
								appSettings.setWeightedAverageAudioValue(0);
								checkTextToSpeechEnabled();
							}
						} else {
							if (progressLevel < appSettings.getThresholdRatio() && isPlaying) {
								if (!isAppMakingNoise()) {
									quietTickCounts++;
									totalContinuousQuietTickCounts++;
								}
								wasLastTickQuiet = true;
								int quietSeconds = quietTickCounts * millisecondsPerTick / 1000;
								System.out.println("Detected Silence for " + quietSeconds + " seconds");
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
									if(!isAppMakingNoise() && quietTickCounts == 0) {
										totalContinuousQuietTickCounts = 0;
									}
									quietTickCounts = 0;
								}
								wasLastTickQuiet = false;
							}
						}
						if (isListening && isPlaying && appSettings.getTimeout() != 0 && (totalContinuousQuietTickCounts * millisecondsPerTick / 1000 / 60) >= appSettings.getTimeout()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									stopPlaying();
								}
							});
						}
						try {
							Thread.sleep(millisecondsPerTick);
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
	
	/* Question Notification Section */
	
	protected void playNotificationSound() {
		SharedPreferences getAlarms = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String alarms = getAlarms.getString("prefSound", "default ringtone");
		Uri uri = Uri.parse(alarms);
		playSound(this, uri);
	}

	protected MediaPlayer mMediaPlayer;
	protected void playSound(Context context, Uri alert) {
		mMediaPlayer = new MediaPlayer();
		try {
			mMediaPlayer.setDataSource(context, alert);
			final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
			if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
				mMediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
				mMediaPlayer.prepare();
				mMediaPlayer.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* Menu Section */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.game_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		// Handle action bar item 
		switch(item.getItemId()) {
		case  R.id.settings:
			//Settings selected
			startActivity(new Intent(this, SettingScreen.class));
			return true;	
		case R.id.recalibrateMic:
			//Recalibrate Microphone selected
			beginDetectionTime = System.currentTimeMillis();
			isDetectingNoise = true;
			audioValue = 0;
			maxAudioValue = 0;
			appSettings.setWeightedAverageAudioValue(mediaRecorder.getMaxAmplitude());
			noiseDetectionDialog.show(getFragmentManager(), "noise");
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/* Noise Detection Section */
	
	private boolean isAppMakingNoise() {
		return ((mMediaPlayer != null && mMediaPlayer.isPlaying()) || (speechObj != null && speechObj.isSpeaking()));
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		if (audioTolerance == 0) {
			audioTolerance = (int) (appSettings.getWeightedAverageAudioValue() * 1.25);
		}
		isDetectingNoise = false;
		checkTextToSpeechEnabled();
	}
	
	public static double coeffA, coeffB, coeffC, coeffD, coeffE;

	public static void processList(ArrayList<Integer> l) {
		
		int size = 0;
		size = l.size();
		System.out.println(size);
		int index = 0;
		double[][] matrixA = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}};
		double[][] matrixB = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}};
		double[][] matrixC = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}}; 
		double[][] matrixD = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}};
		double[][] matrixE = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}};

		double[][] originalMatrix = {
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0},
				{0.0,0.0,0.0,0.0,0.0}};

		double[] answer = new double[5];
		try {
			for (int i = 0; i < 5; i++) {
				for (int j = 0; j < 5; j++) {
					if (index < 95) {
						double temp = l.get(index) + l.get(index+1) + l.get(index+2) + l.get(index+3) + l.get(index+4);
						temp /= 5;				

						matrixA[i][j] = Math.pow(index, i);
						matrixB[i][j] = Math.pow(index, i);
						matrixC[i][j] = Math.pow(index, i);
						matrixD[i][j] = Math.pow(index, i);
						matrixE[i][j] = Math.pow(index, i);
						originalMatrix[i][j] = Math.pow(index, i);
						answer[i] = temp;
						index += 4;
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		for (int i = 0; i < 5; i++) {
			matrixA[0][i] = answer[i];
			matrixB[1][i] = answer[i];
			matrixC[2][i] = answer[i];
			matrixD[3][i] = answer[i];
			matrixE[4][i] = answer[i];
		}


		RealMatrix mA = MatrixUtils.createRealMatrix(matrixA);
		RealMatrix mB = MatrixUtils.createRealMatrix(matrixB);
		RealMatrix mC = MatrixUtils.createRealMatrix(matrixC);
		RealMatrix mD = MatrixUtils.createRealMatrix(matrixD);
		RealMatrix mE = MatrixUtils.createRealMatrix(matrixE);
		RealMatrix mO = MatrixUtils.createRealMatrix(originalMatrix);

		EigenDecomposition mDetA = new EigenDecomposition(mA);
		double determinantA = mDetA.getDeterminant();

		EigenDecomposition mDetB = new EigenDecomposition(mB);
		double determinantB = mDetB.getDeterminant();

		EigenDecomposition mDetC = new EigenDecomposition(mC);
		double determinantC = mDetC.getDeterminant();

		EigenDecomposition mDetD = new EigenDecomposition(mD);
		double determinantD = mDetD.getDeterminant();

		EigenDecomposition mDetE = new EigenDecomposition(mE);
		double determinantE = mDetE.getDeterminant();

		EigenDecomposition mDet = new EigenDecomposition(mO);
		double determinant = mDet.getDeterminant();

		coeffA = determinantA / determinant;
		coeffB = determinantB / determinant;
		coeffC = determinantC / determinant;
		coeffD = determinantD / determinant;
		coeffE = determinantE / determinant;

	}

	public static double func(double x) {
		return coeffA * Math.pow(x, 4) + coeffB * Math.pow(x, 3) + coeffC * Math.pow(x, 2) + coeffD * Math.pow(x, 1) + coeffE * Math.pow(x, 0);
	}
}
