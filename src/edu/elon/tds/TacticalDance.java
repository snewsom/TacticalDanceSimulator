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
import android.os.Vibrator;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager.BadTokenException;
import android.widget.Button;
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

	private Vibrator v;

	public boolean isWinning = true;
	public boolean gameStarted = false;

	private final float[] THRESHOLDS = { 12, 20, 30 };
	private final int THRESHOLD_LOW = 0;
	private final int THRESHOLD_MED = 1;
	private final int THRESHOLD_HIGH = 2;
	private int currentThresh = THRESHOLD_LOW;

	private int delayTime = 0;

	private long oldTime = 0;
	private boolean newSong = false;

	float sum = 0;
	int count = 0;
	
	int winningDevices;

	GameLoop gLoop;

	private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
		public void OnMessageReceived(String device, String message) {
			String[] array = message.split(":");
			if (array[0].equals("CurrentThresh")) {
				currentThresh = Integer.parseInt(array[1]);
				delayTime = (int) (System.currentTimeMillis() - Long
						.parseLong(array[3]));
				delayTime = Math.abs(delayTime);
				System.out.println("delay time: " + delayTime);
				if(isWinning){
					switchSong();
				}
			}
			if (array[0].equals("RestartGame")) {
				restartGame();
			}
			//received if client devices lose
			if(message.equals("Update:Lost")){
				winningDevices--;
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
			// for debug
			for (String s : rivalDevices) {
				System.out.println(s);
			}
			gameStarted = true;
			switchSong();
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
			if (isWinning && gameStarted) {
				float x = Math.abs(se.values[0]);
				float y = Math.abs(se.values[1]);
				float z = Math.abs(se.values[2]);
				sum += (x + y + z) - sensorManager.GRAVITY_EARTH;
				count++;
				// System.out.println(sum);
				if (count >= 10) {
					if (sum / count > THRESHOLDS[currentThresh]) {
						songs.stop();
						v.vibrate(500);
						bgPaint.setColor(Color.CYAN);
						isWinning = false;
						sendMessage("Update","Lost");
						

					}
					count = 0;
					sum = 0;
				}
			}
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (TYPE == 0) {
			getMenuInflater().inflate(R.menu.main, menu);
		}
		return true;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		sensorListener = new SensorListener();
		sensorManager.registerListener(sensorListener,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		self = this;

		v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		Intent startingIntent = getIntent();
		TYPE = startingIntent.getIntExtra("TYPE", 0);
		

		setContentView(R.layout.main);
		mSurface = (SurfaceView) findViewById(R.id.surface);
		mHolder = mSurface.getHolder();

		bgPaint = new Paint();
		bgPaint.setColor(Color.MAGENTA);

		songList = new ArrayList<AssetFileDescriptor>();
		songList.add(this.getResources().openRawResourceFd(R.raw.level1));
		songList.add(this.getResources().openRawResourceFd(R.raw.level2));
		songList.add(this.getResources().openRawResourceFd(R.raw.level3));

		songs = MediaPlayer.create(this, R.raw.level1);

		mConnection = new Connection(this, serviceReadyListener);
		mHolder.addCallback(self);

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

	@Override
	protected void onPause() {
		v.cancel();
		super.onPause();
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
				gameStarted = true;
				songs.start();
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
					if(winningDevices == 1){
						Toast.makeText(getApplicationContext(), "You Win!", Toast.LENGTH_LONG).show();
					}
					if (TYPE == 0 && gameStarted && isWinning && winningDevices!=1) {
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

	public boolean serverRestartGame(MenuItem menuItem) {
		sendMessage("RestartGame", "");
		restartGame();
		return true;
	}

	public void restartGame() {
		winningDevices = rivalDevices.size();
		currentThresh = 0;
		isWinning = true;
		bgPaint.setColor(Color.MAGENTA);
		switchSong();
	}

	private void switchSong() {
		try {
			songs.stop();
			songs.reset();
			AssetFileDescriptor song = songList.get(currentThresh);
			songs.setDataSource(song.getFileDescriptor(),
					song.getStartOffset(), song.getDeclaredLength());
			songs.prepare();
			if (TYPE == 1) {
				// trying to make the music sync better accross phones.
				// to make for delay in message being reseaved.
				songs.seekTo(delayTime);
			}

			v.vibrate(250);
			sendMessage("CurrentThresh",
					currentThresh + ":Systime:" + System.currentTimeMillis());
			if (currentThresh == 0) {
				bgPaint.setColor(Color.RED);
			} else if (currentThresh == 1) {
				bgPaint.setColor(Color.YELLOW);
			} else {
				bgPaint.setColor(Color.GREEN);
			}
			songs.start();

		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}