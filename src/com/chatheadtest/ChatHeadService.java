package com.chatheadtest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ChatHeadService extends Service implements OnClickListener,
		OnTouchListener {

	private URL feedURL = null;
	private Button refresh;
	private TextView processingStatus;
	
	private WindowManager windowManager;
	private WindowManager.LayoutParams params;
	private boolean isDragEvent;
	private RelativeLayout blockOverlay;

	private int initialX;
	private int initialY;
	private float initialTouchX;
	private float initialTouchY;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		try{
			feedURL = new URL("https://graph.facebook.com/me/statuses?access_token=CAACEdEose0cBANZAWUisjQNuG1plogPmZBWmvNvEKIIsDsgOG3rhcNLO1t2KbJxXZAWZBoV5aGttkk2Hw4NyvygZBh6QiTQXXgQTf6P8CiZCcCxYJHw1nZAyBkfYcT6sN1ZALUTSvFf7xubW7vI3ZAZBja9dM0wmeF0qGZAldOqUnGpjXJEXW5wj2prZC9GO91M5kOZBc4qzUcgy1vpSAwsOFGRFetZC59sc1xZBaYZD");
		}catch(Exception e){
			Toast.makeText(this, "Cannot parse URL",Toast.LENGTH_SHORT).show();
		}
		windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

		// load the block overlay here
		LayoutInflater overlayInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		blockOverlay = (RelativeLayout) overlayInflater.inflate(
				R.layout.block_overlay, null);

		refresh = (Button)blockOverlay.findViewById(R.id.refresh_button);
		processingStatus = (TextView)blockOverlay.findViewById(R.id.processing_view);
		setUpListeners();

		params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.TYPE_PHONE,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_DIM_BEHIND,
				PixelFormat.TRANSLUCENT);

		params.gravity = Gravity.CENTER;
		params.dimAmount = 0.1f;
		//for initializing the alert to particular coordinates
		// params.x = 0;
		// params.y = 100;

		windowManager.addView(blockOverlay, params);
		
		
		new FetchFeedTask().execute(feedURL);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (blockOverlay != null)
			windowManager.removeView(blockOverlay);
	}

	private void setUpListeners() {
		blockOverlay.setOnTouchListener(this);
		blockOverlay.setOnClickListener(this);
		((Button) blockOverlay.findViewById(R.id.dismiss_button))
				.setOnClickListener(this);
		((TextView)blockOverlay.findViewById(R.id.qf_title)).setOnClickListener(this);
		refresh.setOnClickListener(this);
		//((ImageView) blockOverlay.findViewById(R.id.close_button)).setOnClickListener(this);

	}

	private void killService() {
		stopSelf();
	}

	@Override
	public void onClick(View v) {
		//The user was dragging the window; don't react
		if (isDragEvent) {
			isDragEvent = false;
			return;
		}
		
		switch (v.getId()) {
		case R.id.dismiss_button:
			killService();
			break;

		case R.id.qf_title:
			Toast.makeText(getApplicationContext(), "Quick Feed Title",
					Toast.LENGTH_SHORT).show();
			break;

		case R.id.refresh_button:
			Toast.makeText(getApplicationContext(), "Hit refresh button",
					Toast.LENGTH_SHORT).show();
			new FetchFeedTask().execute(feedURL);
			break;
		default:
			// do nothing
			break;
		}

	}
	
	private void enableRefresh(){
		refresh.setEnabled(true);
		refresh.setTextColor(Color.WHITE);
	}
	
	private void disableRefresh(){
		refresh.setEnabled(false);
		refresh.setTextColor(Color.GRAY);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			initialX = params.x;
			initialY = params.y;
			initialTouchX = event.getRawX();
			initialTouchY = event.getRawY();
			return false;
		case MotionEvent.ACTION_UP:
			isDragEvent = false;
			return false;
		case MotionEvent.ACTION_MOVE:
			isDragEvent = true;
			params.x = initialX + (int) (event.getRawX() - initialTouchX);
			params.y = initialY + (int) (event.getRawY() - initialTouchY);
			windowManager.updateViewLayout(blockOverlay, params);
			return true;
		}

		return false;
	};
	
	
	private class FetchFeedTask extends AsyncTask<URL, Void, String> {
	    protected void onPreExecute(){
	    	processingStatus.setText("Loading...");
	    	disableRefresh();
	    }
		
		protected String doInBackground(URL... urls) {
	         int count = urls.length;
	         HttpClient httpclient = new DefaultHttpClient();
             HttpResponse response;
             String responseString = null;
	  
	         for (int i = 0; i < count; i++) {
	        	 
	             try {
	                 response = httpclient.execute(new HttpGet(urls[i].toURI()));
	                 StatusLine statusLine = response.getStatusLine();
	                 if(statusLine.getStatusCode() == HttpStatus.SC_OK){
	                     ByteArrayOutputStream out = new ByteArrayOutputStream();
	                     response.getEntity().writeTo(out);
	                     out.close();
	                     responseString = out.toString();
	                 } else{
	                     //Closes the connection.
	                     response.getEntity().getContent().close();
	                     throw new IOException(statusLine.getReasonPhrase());
	                 }
	             } catch (ClientProtocolException e) {
	                 responseString = "Client Protocol Exception";
	             } catch (IOException e) {
	                 //responseString = "IO exception";
	                 responseString = e.toString();
	             } catch(Exception e){
	            	 //responseString = "General Exception";
	            	 responseString = e.toString();
	             }
	             if (isCancelled()) break;
	             
	             // Escape early if cancel() is called
	             
	         }
	         
	         try{
	        	 JSONObject jsonObj = new JSONObject(responseString);
	         }catch(JSONException je){
	        	 //TODO Handle this exception
	         }
	         
	         return responseString;
	     }

//	     protected void onProgressUpdate(Integer... progress) {
//	         setProgressPercent(progress[0]);
//	     }

	     protected void onPostExecute(String result) {
	    	 enableRefresh();
	         //Toast.makeText(getBaseContext(), "Hit completed", Toast.LENGTH_SHORT).show();
	    	 if(result == null)
	    		 processingStatus.setText("Error in retrieving text");
	    	 else
	    		 processingStatus.setText(result.substring(0, result.length()>300?300:result.length()));
	     }
	 }

}