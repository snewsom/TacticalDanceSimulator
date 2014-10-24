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
import android.content.res.AssetFileDescriptor;
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

	private int TYPE; // 0 = server, 1 = client

	private SurfaceView mSurface;

	private SurfaceHolder mHolder;

	private Paint bgPaint;

	private Connection mConnection;

	private String hostDevice = "";

	private ArrayList<String> rivalDevices = new ArrayList<String>();

	private SoundPool mSoundPool;

	private ArrayList<AssetFileDescriptor> songList;

	private MediaPlayer songs;

	private SensorManager sensorManager;
	private SensorEventListener sensorListener;

	private final float[] THRESHOLDS = { 10, 12, 15 };
	private final int THRESHOLD_LOW = 0;
	private final int med = 1;
	private int currentThresh = THRESHOLD_LOW;

	private long oldTime = 0;
	private boolean newSong = false;

	GameLoop gLoop;

	private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
		public void OnMessageReceived(String device, String message) {
			String[] array =message.split(":");
			if(array[0].equals("CurrentThresh")) {
				currentThresh = Integer.parseInt(array[1]);
				switchSong();
			}
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
			if (TYPE == 0) {
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

	private void sendMessage(String messageType, String message) {
		for (String device : rivalDevices) {
			mConnection.sendMessage(device, messageType + ":" + message);
		}
	}

	private class SensorListener implements SensorEventListener {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onSensorChanged(SensorEvent se) {
			// algorithm for seeing if phone is jostled
			float x = Math.abs(se.values[0]);
			float y = Math.abs(se.values[1]);
			float z = Math.abs(se.values[2]);
			float sum = (x + y + z) - sensorManager.GRAVITY_EARTH;
			// System.out.println(sum);
			if (sum > THRESHOLDS[currentThresh]) {
				Toast.makeText(getApplicationContext(), "YOU SUCK",
						Toast.LENGTH_LONG);
			}

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
		TYPE = startingIntent.getIntExtra("TYPE", 0);

		setContentView(R.layout.main);
		mSurface = (SurfaceView) findViewById(R.id.surface);
		mHolder = mSurface.getHolder();

		bgPaint = new Paint();
		bgPaint.setColor(Color.BLACK);

		songList = new ArrayList<AssetFileDescriptor>();
		songList.add(this.getResources().openRawResourceFd(R.raw.level1));
		songList.add(this.getResources().openRawResourceFd(R.raw.level2));
		songList.add(this.getResources().openRawResourceFd(R.raw.level3));

		songs = MediaPlayer.create(this, R.raw.level1);

		mConnection = new Connection(this, serviceReadyListener);
		mHolder.addCallback(self);

		songs.start();
	}

	@Override
	protected void onDestroy() {
		if (mConnection != null) {
			mConnection.shutdown();
		}

		if (songs != null) {
			songs.release();

		}
		super.onDestroy();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		gLoop = new GameLoop();
		gLoop.start();
	}

	private void doDraw(Canvas c) {
		// do some drawin stuff like...
		c.drawRect(0, 0, c.getWidth(), c.getHeight(), bgPaint);
	}

	private void draw() {
		Canvas canvas = null;
		try {
			canvas = mHolder.lockCanvas();
			if (canvas != null) {
				doDraw(canvas);
			}
		} finally {
			if (canvas != null) {
				mHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		try {
			gLoop.safeStop();
		} finally {
			gLoop = null;
		}
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

	private class GameLoop extends Thread {
		private volatile boolean running = true;

		@Override
		public void run() {
			while (running) {
				try {
					Thread.sleep(5);
					draw();
					// doin some update stuff
					if (TYPE == 0) {
						if (!newSong) {
							oldTime = 0 + System.currentTimeMillis();
							newSong = true;
							System.out.println(newSong + "" + oldTime);
						} else {
							// System.out.println(System.currentTimeMillis() -
							// oldTime);
							if (System.currentTimeMillis() - oldTime >= 5000) {
								newSong = false;
								currentThresh = (int) (Math.random() * 3);
								switchSong();
								sendMessage("CurrentThresh", currentThresh + "");
								
							}
						}
					}

				} catch (InterruptedException ie) {
					running = false;
				}
			}
		}

		public void safeStop() {
			running = false;
			interrupt();
		}
	}

	private void switchSong() {
		try {
			songs.stop();
			songs.reset();
			AssetFileDescriptor song = songList.get(currentThresh);
			songs.setDataSource(song.getFileDescriptor(),
					song.getStartOffset(), song.getDeclaredLength());
			songs.prepare();
			if(TYPE == 0) {
				// trying to make the music sync better accross phones.
				// to make for delay in message being reseaved.
				Thread.sleep(350);
			}
			songs.start();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
