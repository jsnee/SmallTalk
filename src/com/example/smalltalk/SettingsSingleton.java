package com.example.smalltalk;

public class SettingsSingleton {
	
	private static SettingsSingleton appSettings = null;
	private static int weightedAverageAudioValue = 50000;
	private static double thresholdRatio = 45.0;
	private static int timerSeconds = 5;
	
	private SettingsSingleton() {
	}
	
	private static void loadSettings() {
		if (appSettings == null) {
			appSettings = new SettingsSingleton();
		}
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

	public int getTimerSeconds() {
		return timerSeconds;
	}
	
	public void setTimerSeconds(int timerSeconds) {
		this.timerSeconds = timerSeconds;
	}
}
