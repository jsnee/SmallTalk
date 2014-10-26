package edu.drake.smalltalk;

import android.content.SharedPreferences;

public class SettingsSingleton {
	
	private static SettingsSingleton appSettings = null;
	private static int weightedAverageAudioValue = 50000;
	private static double thresholdRatio = 45.0;
	private static int timerSeconds = 5;
	private static SharedPreferences sharedPreferences;
	
	private SettingsSingleton() {
	}
	
	private static void loadSettings() {
		if (appSettings == null) {
			appSettings = new SettingsSingleton();
		}
	}
	
	public static void loadSettings(SharedPreferences sharedPrefs) {
		loadSettings();
		sharedPreferences = sharedPrefs;
		timerSeconds = Integer.parseInt(sharedPrefs.getString("prefTimer", "5"));
	}
	
	public static SettingsSingleton getSettings() {
		loadSettings();
		return appSettings;
	}
	
	public int getWeightedAverageAudioValue() {
		return weightedAverageAudioValue;
	}
	
	public void setWeightedAverageAudioValue(int weightedAverageAudioValue) {
		this.weightedAverageAudioValue = weightedAverageAudioValue;
	}
	
	public void refineWeightedAverageAudioValue(int weightedAverageAudioValue) {
		this.weightedAverageAudioValue = (this.weightedAverageAudioValue + weightedAverageAudioValue) / 2;
	}
	
	public double getThresholdRatio() {
		return thresholdRatio;
	}
	
	public void setThresholdRatio(double thresholdRatio) {
		this.thresholdRatio = thresholdRatio;
	}
	
	public boolean isTextToSpeechEnabled() {
		return sharedPreferences.getBoolean("prefSpeech", false);
	}

	public int getTimerSeconds() {
		return Integer.parseInt(sharedPreferences.getString("prefTimer", "5"));
	}
	
	public void setTimerSeconds(int timerSeconds) {
		this.timerSeconds = timerSeconds;
	}
	
	public int getTimeout() {
		return Integer.parseInt(sharedPreferences.getString("prefTimeout", "0"));
	}
}
