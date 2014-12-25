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
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.Engine;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SpeakService extends IntentService {
	static final String STOP = "STOP";
	static final String SPEAK = "SPEAK";
	static final String TEXT = "text";

	private TextToSpeech textToSpeech;
	private CountDownLatch countDownLatch;
	static boolean stopIt = false;

	static UtteranceProgressListener utteranceProgressListener;

	static void setUtteranceProgressListener(UtteranceProgressListener utteranceProgressListener) {
		SpeakService.utteranceProgressListener = utteranceProgressListener;
	}

	/**
	 * Creates an IntentService. Invoked by your subclass's constructor.
	 *
	 */
	public SpeakService() {
		super(SPEAK);
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
							textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

								@Override
								public void onStart(String utteranceId) {
									if (utteranceProgressListener != null) {
										utteranceProgressListener.onStart(utteranceId);
									}
								}

								@SuppressWarnings("deprecation")
								@Override
								public void onError(String utteranceId) {
									if (utteranceProgressListener != null) {
										utteranceProgressListener.onError(utteranceId);
									}
								}

								@Override
								public void onError(String utteranceId, int errorCode) {
									if (utteranceProgressListener != null) {
										utteranceProgressListener.onError(utteranceId, errorCode);
									}
								}

								@Override
								public void onDone(String utteranceId) {
									if (utteranceProgressListener != null) {
										utteranceProgressListener.onDone(utteranceId);
									}
								}
							});
						}
					}
				});
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		Log.d(Thread.currentThread().getName(), "Action: " + action);
		if (SPEAK.equals(action)) {
			String text = intent.getExtras().getString(TEXT);
			String utteranceId = intent.getExtras().getString(Engine.KEY_PARAM_UTTERANCE_ID);
			if (text != null) {
				try {
					countDownLatch.await();
					Thread.sleep(100);
					say(text, utteranceId);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void say(String text, String utteranceId) {
		HashMap<String, String> params = new HashMap<String, String>();
		if (utteranceId != null) {
			params.put(Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
		}
		textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		Intent stopIntent = new Intent(this, SpeakServiceLaunchActivity.class);
		stopIntent.setAction(STOP);

		PendingIntent stopPendingIntent =
				PendingIntent.getActivity(
						this,
						10001,
						stopIntent,
						PendingIntent.FLAG_CANCEL_CURRENT);

		NotificationCompat.Builder notficationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.notification_title))
				.setContentText(getString(R.string.notification_text))
				.setPriority(Notification.PRIORITY_MAX);

		notficationBuilder.setContentIntent(stopPendingIntent);
		notficationBuilder.addAction(R.drawable.ic_av_stop, STOP, stopPendingIntent);

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