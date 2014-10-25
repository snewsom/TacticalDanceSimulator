package edu.elon.tds;

import net.clc.bt.Connection;
import net.clc.bt.R;
import net.clc.bt.ServerListActivity;
import net.clc.bt.Connection.OnConnectionLostListener;
import net.clc.bt.Connection.OnConnectionServiceReadyListener;
import net.clc.bt.Connection.OnIncomingConnectionListener;
import net.clc.bt.Connection.OnMaxConnectionsReachedListener;
import net.clc.bt.Connection.OnMessageReceivedListener;
import net.clc.bt.R.drawable;
import net.clc.bt.R.id;
import net.clc.bt.R.layout;
import net.clc.bt.R.raw;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;

public class TacticalDance extends Activity implements Callback {

	public static final String TAG = "TacticalDance";

	private static final int SERVER_LIST_RESULT_CODE = 42;

	private TacticalDance self;

	private int mType; // 0 = server, 1 = client

	private SurfaceView mSurface;

	private SurfaceHolder mHolder;

	private Paint bgPaint;

	private Connection mConnection;

	private String hostDevice = "";

	private ArrayList<String> rivalDevices = new ArrayList<String>();

	private SoundPool mSoundPool;

	private MediaPlayer mPlayer;

	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	private final float[] THRESHOLDS = { 10, 12, 15 };
	private final int low = 0;
	private final int med = 1;
	private final int high = 2;
	private int currentThresh = low;

	private long oldTime = System.currentTimeMillis();
	private boolean isRunning = false;
	private float sum = 0;
	private int count = 0;

	private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
		public void OnMessageReceived(String device, String message) {

		}
	};

	private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
		public void OnMaxConnectionsReached() {

		}
	};

	private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
		public void OnIncomingConnection(String device) {
			if (!rivalDevices.contains(device)) {
				rivalDevices.add(device);
			}
			for (String s : rivalDevices) {
				System.out.println(s);
			}
		}
	};

	private OnConnectionLostListener disconnectedListener = new OnConnectionLostListener() {
		public void OnConnectionLost(String device) {
			class displayConnectionLostAlert implements Runnable {
				public void run() {
					Builder connectionLostAlert = new Builder(self);

					connectionLostAlert.setTitle("Connection lost");
					connectionLostAlert
							.setMessage("Your connection with the other player has been lost.");

					connectionLostAlert.setPositiveButton("Ok",
							new OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							});
					connectionLostAlert.setCancelable(false);
					try {
						connectionLostAlert.show();
					} catch (BadTokenException e) {
					}
				}
			}
			self.runOnUiThread(new displayConnectionLostAlert());
		}
	};

	private OnConnectionServiceReadyListener serviceReadyListener = new OnConnectionServiceReadyListener() {
		public void OnConnectionServiceReady() {
			if (mType == 0) {
				mConnection.startServer(4, connectedListener,
						maxConnectionsListener, dataReceivedListener,
						disconnectedListener);
				self.setTitle("Tactical Dance Sim 2K5: "
						+ mConnection.getName() + "-"
						+ mConnection.getAddress());
			} else {
				Intent serverListIntent = new Intent(self,
						ServerListActivity.class);
				startActivityForResult(serverListIntent,
						SERVER_LIST_RESULT_CODE);
			}
		}
	};

	private class SensorListener implements SensorEventListener {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent se) {
			// (rough) algorithm for seeing if phone is jostled
			// takes 20 measurements. if the average of those 
			// 20 measurements is > threshold, you lose
			float x = Math.abs(se.values[0]);
			float y = Math.abs(se.values[1]);
			float z = Math.abs(se.values[2]);
			sum += (x + y + z) - sensorManager.GRAVITY_EARTH;
			count+=1;
			// System.out.println(sum);
			
			if(count>=20){
				if (sum/20 > THRESHOLDS[currentThresh]) {
					Toast.makeText(getApplicationContext(), "YOU SUCK",
							Toast.LENGTH_LONG).show();

					mSurface.setBackgroundColor(Color.BLACK);
				}
				count = 0;
				sum = 0;
			}
			
			if (!isRunning) {
				oldTime = System.currentTimeMillis();
				isRunning = true;
				System.out.println(isRunning + "" + oldTime);
			}
			else {
				//System.out.println(System.currentTimeMillis() - oldTime);
				//set to 5s for testing purposes. CHANGE BACK
				if (System.currentTimeMillis() - oldTime >= 5000) {
					changePace();
				}
			}

		}

	}
	
	/**
	 * Called every 20s to update the screen, music, and thresholds.
	 * is currently called by onSensorChanged(). probably needs to be
	 * called in update() method of nonexistent game loop.
	 */
	public void changePace(){
		isRunning = false;
		currentThresh = (int) (Math.random() * 3);
		mPlayer.stop();
		System.out.println("OKAY M EFFER U CAN STOP NOW");
		System.out.println(currentThresh);
		
		//change song and background depending on threshold
		if(currentThresh==0){
			//mPlayer = MediaPlayer.create(this, R.raw.level1);
			mSurface.setBackgroundColor(Color.RED);
		}else if(currentThresh==1){
			//mPlayer = MediaPlayer.create(this, R.raw.level2);
			mSurface.setBackgroundColor(Color.YELLOW);
		}else{
			mPlayer = MediaPlayer.create(this, R.raw.level3);
			bgPaint.setColor(Color.GREEN);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorListener = new SensorListener();
		sensorManager.registerListener(sensorListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		self = this;

		Intent startingIntent = getIntent();
		mType = startingIntent.getIntExtra("TYPE", 0);

		setContentView(R.layout.main);
		mSurface = (SurfaceView) findViewById(R.id.surface);
		mHolder = mSurface.getHolder();

		bgPaint = new Paint();
		bgPaint.setColor(Color.GREEN);
		
		mSurface.setBackgroundColor(Color.GREEN);
		
		mPlayer = MediaPlayer.create(this, R.raw.level3);

		mConnection = new Connection(this, serviceReadyListener);
		mHolder.addCallback(self);

		mPlayer.start();
	}

	@Override
	protected void onDestroy() {
		if (mConnection != null) {
			mConnection.shutdown();
		}
		if (mPlayer != null) {
			mPlayer.release();
		}
		super.onDestroy();
	}

	public void surfaceCreated(SurfaceHolder holder) {

	}

	private void doDraw(Canvas c) {

	}

	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if ((resultCode == Activity.RESULT_OK)
				&& (requestCode == SERVER_LIST_RESULT_CODE)) {
			String device = data
					.getStringExtra(ServerListActivity.EXTRA_SELECTED_ADDRESS);
			int connectionStatus = mConnection.connect(device,
					dataReceivedListener, disconnectedListener);
			if (connectionStatus != Connection.SUCCESS) {
				Toast.makeText(self, "Unable to connect; please try again.", 1)
						.show();
			} else {
				// TODO i dont know if i wanna set host here
				hostDevice = device;
			}
			return;
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}

}
