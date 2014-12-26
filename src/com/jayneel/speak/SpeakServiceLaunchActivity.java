package com.jayneel.speak;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.UtteranceProgressListener;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class SpeakServiceLaunchActivity extends Activity {
    public static final String PREFS_NAME = "SpeakPrefsFile";

	private boolean finishAfterIntent;
	private EditText speechText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speak_service_launch);
		speechText = (EditText) findViewById(R.id.speechText);

		Intent intent = getIntent();
        String action = intent.getAction();
		finishAfterIntent = true;
        if (Intent.ACTION_MAIN.equals(action)) {
        	finishAfterIntent = false;
            // Restore preferences
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            String text = settings.getString(SpeakService.TEXT, "");
            speechText.setText(text);
        } else if (Intent.ACTION_SEND.equals(action)) {
        	String type = intent.getType();
            if ("text/plain".equals(type)) {
            	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            	speak(sharedText);
            }
        } else if (SpeakService.STOP.equals(action)) {
            stopSpeaking();
        }

    	if (finishAfterIntent) {
    		finish();
    	}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (!finishAfterIntent) {
			// We need an Editor object to make preference changes.
			// All objects are from android.context.Context
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString(SpeakService.TEXT, speechText.getText().toString());

			// Commit the edits!
			editor.commit();
		}
	}

	private void speak(String text) {
		speak(text, false);
	}

	private static LinkedHashMap<Integer, String> lhs = new LinkedHashMap<Integer, String>();

	private static Pattern dq = Pattern.compile("[.?]");

	private void speak(String text, boolean highlightSpokenSentence) {
		if (highlightSpokenSentence) {
			if (text.trim().length() == 0) {
				return;
			}

			int from = 0;
			int indexOfDot ;

			lhs.clear() ;

			SpeakService.setUtteranceProgressListener(new UtteranceProgressListener() {
				@Override
				public void onStart(String utteranceId) {
					Integer from = Integer.valueOf(utteranceId);
					String sentence = lhs.remove(from);
					int to = from + sentence.length();
					setSelection(from, to);
				}

				@Override
				public void onDone(String utteranceId) {
					if (lhs.isEmpty()) {
						clear();
					} else {
						speakNext(lhs);
					}
				}

				@Override
				public void onError(String utteranceId) {
					clear();
				}

				@Override
				public void onError(String utteranceId, int statusCode) {
					clear();
				}

			});
			Matcher matcher = dq.matcher(text);
			while (matcher.find(from)) {
				indexOfDot = matcher.start();
				String sentence = text.substring(from, indexOfDot+1);
				lhs.put(from, sentence);
				from = indexOfDot + 1;
			}

			if (from < text.length()) {
				lhs.put(from, text.substring(from));
			}

			speakNext(lhs);
		} else {
			Intent speakIntent = new Intent(this, SpeakService.class);
			speakIntent.setAction(SpeakService.SPEAK);
			speakIntent.putExtra(SpeakService.TEXT, text);
			this.startService(speakIntent);
		}
	}

	private void speakNext(LinkedHashMap<Integer, String> lhs) {
		Entry<Integer, String> entry = lhs.entrySet().iterator().next();
		if (entry != null) {
			Intent speakIntent = new Intent(this, SpeakService.class);
			speakIntent.setAction(SpeakService.SPEAK);
			speakIntent.putExtra(SpeakService.TEXT, entry.getValue());
			speakIntent.putExtra(Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(entry.getKey()));
			this.startService(speakIntent);
		}
	}

	private void clear() {
		SpeakService.setUtteranceProgressListener(null);
		setSelection(0, 0);
	}

	private void setSelection(final int start, final int stop) {
		speechText.post(new Runnable() {

			@Override
			public void run() {
				speechText.setSelection(start, stop);
			}

		});
	}

	private void stopSpeaking() {
		lhs.clear();
		clear();
		SpeakService.stopIt = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.speak_service_launch_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_speak) {
			speak(speechText.getText().toString(), true);
			return true;
		} else if (id == R.id.action_stop) {
			stopSpeaking();
			return true;
		} else if (id == R.id.action_settings) {
			Intent intent = new Intent();
			intent.setAction("com.android.settings.TTS_SETTINGS");
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			this.startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
