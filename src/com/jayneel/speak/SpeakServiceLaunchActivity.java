package com.jayneel.speak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class SpeakServiceLaunchActivity extends Activity {
	private boolean finishAfterIntent = true;
	private EditText speechText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_speak_service_launch);
		speechText = (EditText) findViewById(R.id.speechText);
		Intent intent = getIntent();
        String action = intent.getAction();
		Log.d(Thread.currentThread().getName(), "Action: " + action);
        if (Intent.ACTION_MAIN.equals(action)) {
        	finishAfterIntent = false;

        } else if (Intent.ACTION_SEND.equals(action)) {
        	String type = intent.getType();
            if ("text/plain".equals(type)) {
            	String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            	speak(sharedText);
            }
        } else if ("STOP".equals(action)) {
        	stop();
        }
        
    	if (finishAfterIntent) {
    		finish();
    	}
	}
	
	private void speak(String text) {
		Intent speakIntent = new Intent(this, SpeakService.class);
    	speakIntent.setAction("SPEAK");
    	speakIntent.putExtra("text", text);
    	this.startService(speakIntent);
	}
	
	private void stop() {
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
			speak(speechText.getText().toString());
			return true;
		} else if (id == R.id.action_stop) {
			stop();
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
