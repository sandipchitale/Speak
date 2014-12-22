package com.jayneel.speak;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SpeakService extends IntentService {
	private TextToSpeech textToSpeech;
	private CountDownLatch countDownLatch;
	static boolean stopIt = false;

	/**
	 * Creates an IntentService. Invoked by your subclass's constructor.
	 *
	 */
	public SpeakService() {
		super("Speak");
		countDownLatch = new CountDownLatch(1);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		textToSpeech = new TextToSpeech(getApplicationContext(),
				new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						if (status != TextToSpeech.ERROR) {
							countDownLatch.countDown();
							Log.d(Thread.currentThread().getName(), "TTS Initialized.");
						}
					}
				});
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		Log.d(Thread.currentThread().getName(), "Action: " + action);
		if ("SPEAK".equals(action)) {
			String text = intent.getExtras().getString("text");
			if (text != null) {
				try {
					countDownLatch.await();
					Thread.sleep(100);
					say(text);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void say(String text) {
		textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, new HashMap<String, String>());
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
		Intent stopIntent = new Intent(this, SpeakServiceLaunchActivity.class);
		stopIntent.setAction("STOP");
		
		PendingIntent stopPendingIntent =
				PendingIntent.getActivity(
						this,
						10001,
						stopIntent,
						PendingIntent.FLAG_CANCEL_CURRENT);
		        				
		NotificationCompat.Builder notficationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle("Speak")
				.setContentText("Touch to stop.")
				.setPriority(Notification.PRIORITY_MAX);
		
		notficationBuilder.setContentIntent(stopPendingIntent);
		notficationBuilder.addAction(R.drawable.ic_av_stop, "STOP", stopPendingIntent);
		
		mNotificationManager.notify(1001, notficationBuilder.build());
		stopIt = false;
		waitToFinishSpeaking();
	}
	
	private void waitToFinishSpeaking() {
		try {
			while (textToSpeech.isSpeaking()) {
				if (stopIt) {
					stopIt = false;
					textToSpeech.stop();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		} finally {
			NotificationManager mNotificationManager =
					(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			mNotificationManager.cancel(1001);
		}
	}


	@Override
	public void onDestroy() {
		textToSpeech.shutdown();
		super.onDestroy();
	}

}