package com.avior.hizlisozluk;

import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

public class MainActivity extends Activity implements OnClickListener {
	// constants
	private static final int VOICE_RECOGNITION_REQUEST_CODE = 0x1337c0de;
	private static final int TR_EN_MODE = 0x7;
	private static final int EN_TR_MODE = 0x3;
	
	private static final String TAG = "HizliSozluk";
	private static final String WORD_PROPNAME = "word";
	
	private static final HashMap<Integer, String> languageTag = new HashMap<Integer, String>(){{
		put(TR_EN_MODE, "tr-TR");
		put(EN_TR_MODE, "en-US");
	}};
	
	// non-static variables.
	private View[] buttons = { null, null };
	private static int mode = 0;
	AlertDialog choiceDialog;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.main);

		View btn = findViewById(R.id.entr_btn);
		buttons[0] = btn;

		btn = findViewById(R.id.tren_btn);
		buttons[1] = btn;

		checkRecognition();
	}

	public void checkRecognition() {
		Log.v(TAG, "Checking for RecognizerIntent from package manager.");
		
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(
				RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		
		if (activities.size() != 0) {
			// recognizer is available.			
			for (View b : buttons)
				b.setOnClickListener(this);
			
		} else {
			// recognizer intent not found show error promopt.
			Log.w(TAG, "Recognizer Intent not found!");
			for (View b : buttons)
				b.setEnabled(false);
			
			showVoiceSearchDownloadRequest();
		}
	}

	private void showVoiceSearchDownloadRequest() {
		// request user to download Voice Search app.
		
		AlertDialog dialog;
		final Activity thisActivity = this; 
		
		AlertDialog.Builder builder = new AlertDialog.Builder(thisActivity);
		builder	.setMessage(getString(R.string.err_voice_search))
				.setCancelable(false)
				.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// launch market
						try {
							// start market intent
							Intent goToMarket = new Intent(Intent.ACTION_VIEW,Uri.parse(getString(R.string.market_voicesearch_url)));
							startActivity(goToMarket);
						} catch (Throwable e) {
							// market not detected on the phone.
							new AlertDialog.Builder(thisActivity)
							.setMessage(R.string.err_no_market)
							.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener(){
								public void onClick(DialogInterface dialog, int which) {
									// do nothing.
								}
							}).create().show();
						}
					}
				})
				.setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// dismiss this window.
						if (dialog != null) dialog.dismiss();
					}
				});

		dialog = builder.create();
		dialog.show();
		Log.d(TAG, "Showing voice search download request dialog.");
	}

	@Override
	public void onClick(View v) {
		// handle two buttons 
		
		if (v.getId() == R.id.entr_btn) {
			mode = EN_TR_MODE;
			startVoiceRecognitionActivity();
		}
		if (v.getId() == R.id.tren_btn) {
			mode = TR_EN_MODE;
			startVoiceRecognitionActivity();
		}
	}

	private void startVoiceRecognitionActivity() {
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
				RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
				getString(R.string.prompt));
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag.get(mode));
		startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNITION_REQUEST_CODE
				&& resultCode == RESULT_OK) {
			List<String> matches = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			
			if (matches == null || matches.size() < 1){
				Log.w(TAG, "Matches list is 'null' or size is 0.");
				return;
			} else if (matches.size() == 1){
				Log.i(TAG, "Single result redirection.");
				processResult(matches.get(0));
			} else {
				Log.d(TAG, "Showing results dialog.");
				showResultsDialog(matches);
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void showResultsDialog(List<String> results) {
		final CharSequence[] items = new CharSequence[results.size()];
		int offset = 0;
		for(String s : results)
			items[offset++] = s;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.pick));
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int item) {
		    	// process chosen item
		        processResult(items[item]);
		    }
		});
		
		// show results dialog
		choiceDialog = builder.create();
		choiceDialog.show();
	}

	protected void processResult(CharSequence result) {
		Log.i(TAG, String.format("Requested processing for `%s`", result));
		
		if (choiceDialog != null) choiceDialog.hide(); // it may be left visible mistakenly. i don't know.
		
		Intent i = new Intent(getApplicationContext(), ResultsActivity.class);
		i.putExtra(WORD_PROPNAME, result);
		startActivity(i);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// handle menu key
		if (keyCode == KeyEvent.KEYCODE_MENU){
			showAbout(getApplicationContext());
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void showAbout(Context context) {
		Dialog d = new Dialog(this);
		d.setContentView(R.layout.about);
		d.setTitle(getString(R.string.about_title));
		d.setCancelable(true);
		d.show();
	}
}