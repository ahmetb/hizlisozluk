package com.avior.hizlisozluk;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class ResultsActivity extends Activity{
	//private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/534.25 (KHTML, like Gecko) Chrome/12.0.706.0 Safari/534.25"; // if spoofing needed.
	
	private static final String TAG = "HizliSozluk";
	private static final String WORD_PROPNAME = "word";
	
	private WebView web;

	protected void showErrorToast(String message){
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		getWindow().requestFeature(Window.FEATURE_PROGRESS); 
        getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

		final Activity act = this; // to be accessed inside onProgressChanged
		
		Bundle extras = getIntent().getExtras();
		
		if(extras != null){
			final String word = extras.getString(WORD_PROPNAME);
			
			if (word == null || "".equals(word) || word.length() < 1){
				Log.e(TAG, "Could not find extract 'word' in ResultsActivity extras bundle.");
				showErrorToast(getString(R.string.err_recv_word));
				finish();
			} else {
				String url = String.format(getString(R.string.search_url), word);
				Log.i(TAG, String.format("Loading: %s", url));
				
				setTitle(String.format(getString(R.string.loading_title), word));
				
				web = new WebView(this);
				//web.getSettings().setUserAgentString(USER_AGENT);
				web.getSettings().setJavaScriptEnabled(true);
				
				// enable progress bar
				web.setWebChromeClient(new WebChromeClient() {
					public void onProgressChanged(WebView view, int progress) {
						act.setProgress(progress * 100);
						
						if(progress == 100) // finished loading
							setTitle(String.format(getString(R.string.results_title), word));
					}
				});
				
				// detect errors
				web.setWebViewClient(new WebViewClient(){
					@Override
					public void onReceivedError(WebView view, int errorCode,
							String description, String failingUrl) {
						showErrorToast(description);
						Log.e(TAG, String.format(getString(R.string.err_webclient), description));
					}
				});
				
				// key listeners
				web.setOnKeyListener(new OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						// provide back functionality
						if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()){
							web.goBack();
							return true;
						}
						
						// enable search key.
						if (keyCode == KeyEvent.KEYCODE_SEARCH){
							//Intent i = new Intent(getApplicationContext(), MainActivity.class);
							finish();
						}
						return false;
					}
				});
				
				web.loadUrl(url);
				setContentView(web);
			}
		} else {
			Log.e(TAG, getString(R.string.err_xtras_bundle));
			showErrorToast(getString(R.string.err_xtras_bundle));
			finish();
		}
	}
}
