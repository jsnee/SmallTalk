package edu.drake.smalltalk;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.view.Menu;
import android.view.MenuItem;

public class SettingScreen extends PreferenceActivity {

	private static SettingsSingleton appSettings = SettingsSingleton.getSettings();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//setContentView(R.layout.activity_setting_screen);
		addPreferencesFromResource(R.xml.settings);
		
		updateTimeoutPreferenceSummary();
		updateTimerPreferenceSummary();
		updateNotificationSoundPreferenceEnabled();

		SharedPreferences.OnSharedPreferenceChangeListener listener = getPreferencesChangeListener();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
	}

	private SharedPreferences.OnSharedPreferenceChangeListener getPreferencesChangeListener() {
		return new SharedPreferences.OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
					String key) {
				if (key.equals("prefTimeout")) {
					updateTimeoutPreferenceSummary();
				} else if (key.equals("prefTimer")) {
					updateTimerPreferenceSummary();
				} else if (key.equals("prefSpeech")) {
					updateNotificationSoundPreferenceEnabled();
				}

			}
		};
	}
	
	private void updateNotificationSoundPreferenceEnabled() {
		boolean textToSpeechChecked = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("prefSpeech", true);
		RingtonePreference ringtonePreference = ((RingtonePreference)getPreferenceScreen().findPreference("prefSound"));
		ringtonePreference.setEnabled(!textToSpeechChecked);
	}

	private void updateTimeoutPreferenceSummary() {
		String timeoutSummary = "";
		String defaultTimeout = getResources().getString(R.string.pref_timeout_default);
		ListPreference timeoutPreference = ((ListPreference)getPreferenceScreen().findPreference("prefTimeout"));
		int timeoutValue = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("prefTimeout", defaultTimeout));
		if (timeoutValue == 0) {
			timeoutSummary = getResources().getString(R.string.pref_timeout_never_summary);
		} else {
			String timeoutSummaryFormat = getResources().getString(R.string.pref_timeout_summary);
			timeoutSummary = String.format(timeoutSummaryFormat, timeoutValue);
		}
		timeoutPreference.setSummary(timeoutSummary);
	}

	private void updateTimerPreferenceSummary() {
		String defaultTimer = getResources().getString(R.string.pref_timer_val_default);
		ListPreference timerPreference = ((ListPreference)getPreferenceScreen().findPreference("prefTimer"));
		int timerValue = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("prefTimer", defaultTimer));
		String timerSummaryFormat = getResources().getString(R.string.pref_timer_summary);
		String timerSummary = String.format(timerSummaryFormat, timerValue);
		timerPreference.setSummary(timerSummary);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.setting_screen, menu);
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
