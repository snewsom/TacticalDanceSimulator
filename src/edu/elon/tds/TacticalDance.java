package edu.elon.tds;

import net.clc.bt.Connection;
import net.clc.bt.R;
import net.clc.bt.ServerListActivity;
import net.clc.bt.Connection.OnConnectionLostListener;
import net.clc.bt.Connection.OnConnectionServiceReadyListener;
import net.clc.bt.Connection.OnIncomingConnectionListener;
import net.clc.bt.Connection.OnMaxConnectionsReachedListener;
import net.clc.bt.Connection.OnMessageReceivedListener;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.res.AssetFileDescriptor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;

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
	private ArrayList<AssetFileDescriptor> songList;
	private MediaPlayer songs;
	private SensorManager sensorManager;
	private SensorEventListener sensorListener;
	private Vibrator v;
	public boolean isWinning = true;
	public boolean gameStarted = false;
	private final float[] THRESHOLDS = { 12, 20, 30 };
	private int currentThresh = 1;
	private long oldTime = 0;
	private boolean newSong = false;
	float sum = 0;
	int count = 0;
	int winningDevices;
	int loseCount;
	GameLoop gLoop;

	private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
		public void OnMessageReceived(String device, String message) {
			String[] array = message.split(":");
			System.out.println(device + " : " + message);
			if (array[0].equals("CurrentThresh")) {
				currentThresh = Integer.parseInt(array[1]);
				if (isWinning && TYPE == 1) {
					switchSong();
				}
			}
			if (array[0].equals("RestartGame")) {
				restartGame();
			}
			// received if client devices lose
			if (array[0].equals("PlayerLost")) {
				System.out.println("A player lost!");
				if (TYPE == 0) {
					System.out.println(winningDevices);
					winningDevices--;
					if (winningDevices <= 1) {
						if (!checkWinner()) {
							sendMessage("CheckWinner", "");
						}
						winningDevices = 1 + rivalDevices.size();
					}
				}
			}
			if (array[0].equals("LoseCount")) {
				loseCount++;
				// theoretically impossible, realistically improbable
				if (loseCount == rivalDevices.size()) {
					sendMessage("NoWinner", "");
					noWinner();
				}
			}
			if (array[0].equals("NoWinner")) {
				noWinner();
			}
			if (array[0].equals("CheckWinner")) {
				System.out.println("Checking winner...");
				checkWinner();
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
				winningDevices = 1 + rivalDevices.size();
			}
			for (String s : rivalDevices) {
				System.out.println(s);
			}
			gameStarted = true;
			System.out.println("Game started!");
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
		if (TYPE == 0) {
			for (String device : rivalDevices) {
				mConnection.sendMessage(device, messageType + ":" + message);
			}
		} else {
			mConnection.sendMessage(hostDevice, messageType + ":" + message);
		}
	}

	protected boolean checkWinner() {
		if (isWinning) {
			win();
			return true;
		} else if (TYPE == 1) {
			sendMessage("LoseCount", "");
		}
		return false;
	}

	protected void win() {
		System.out.println("you win");
		v.vibrate(1000);
		winSound();
		try {
			for (int i = 0; i < 10; i++) {
				bgPaint.setColor(Color.argb(255, 255, 165, 0));
				Thread.sleep(100);
				bgPaint.setColor(Color.YELLOW);
				Thread.sleep(100);
				bgPaint.setColor(Color.GREEN);
				Thread.sleep(100);
				bgPaint.setColor(Color.BLUE);
				Thread.sleep(100);
				bgPaint.setColor(Color.CYAN);
				Thread.sleep(100);
				bgPaint.setColor(Color.argb(255, 125, 38, 205));
				Thread.sleep(100);
				bgPaint.setColor(Color.RED);
			}
			if (TYPE == 0) {
				restartGame();
			} else {
				sendMessage("RestartGame", "");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean serverRestartGame(MenuItem menuItem) {
		restartGame();
		return true;
	}

	public void restartGame() {
		bgPaint.setColor(Color.MAGENTA);
		System.out.println("Game Restarted");
		isWinning = true;
		if (TYPE == 0) {
			loseCount = 0;
			winningDevices = rivalDevices.size() + 1;
			oldTime = System.currentTimeMillis();
			currentThresh = 0;
			sendMessage("RestartGame", "");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			switchSong();
		}
	}

	protected void noWinner() {
		v.vibrate(2000);
		bgPaint.setColor(Color.GRAY);
		try {
			Thread.sleep(5000);
			if (TYPE == 0) {
				restartGame();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private class SensorListener implements SensorEventListener {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {

		}

		@SuppressWarnings("static-access")
		@Override
		public void onSensorChanged(SensorEvent se) {
			// algorithm for seeing if phone is jostled
			if (isWinning && gameStarted) {
				float x = Math.abs(se.values[0]);
				float y = Math.abs(se.values[1]);
				float z = Math.abs(se.values[2]);
				sum += (x + y + z) - sensorManager.GRAVITY_EARTH;
				count++;
				if (count >= 10) {
					if (sum / count > THRESHOLDS[currentThresh]) {
						try {
							if (songs.isPlaying()) {
								songs.stop();
							}
						} catch (IllegalStateException e) {

						}
						v.vibrate(500);
						bgPaint.setColor(Color.CYAN);
						loseSound();
						isWinning = false;
						if (TYPE == 0) {
							winningDevices--;
							System.out
									.println("Number of devices left in the game: "
											+ winningDevices);
							if (winningDevices == 1) {
								sendMessage("CheckWinner", "");
							}
						} else {
							sendMessage("PlayerLost", "");
						}

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
		if (TYPE == 0) {
			winningDevices = 1;
		}

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
		songList.add(this.getResources().openRawResourceFd(R.raw.lose));
		songList.add(this.getResources().openRawResourceFd(R.raw.win));

		songs = MediaPlayer.create(this, R.raw.level2);

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
				Toast.makeText(self, "Unable to connect; please try again.",
						Toast.LENGTH_SHORT).show();
			} else {
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
					Thread.sleep(0);
					draw();
					if (TYPE == 0 && gameStarted && isWinning
							&& winningDevices != 1) {
						if (!newSong) {
							oldTime = 0 + System.currentTimeMillis();
							newSong = true;
							System.out.println(newSong + "" + oldTime);
						} else {
							if (System.currentTimeMillis() - oldTime >= 15000) {
								System.out
										.println("Time elapsed. Song Switched.");
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

	private void switchSong() {
		try {
			System.out.println("Song Switched");
			AssetFileDescriptor song = songList.get(currentThresh);
			songs.stop();
			songs.reset();
			songs.setDataSource(song.getFileDescriptor(),
					song.getStartOffset(), song.getDeclaredLength());
			songs.prepare();

			v.vibrate(250);
			if (TYPE == 0) {
				sendMessage("CurrentThresh", currentThresh + ":Systime:"
						+ System.currentTimeMillis());
			}
			if (currentThresh == 0) {
				bgPaint.setColor(Color.RED);
			} else if (currentThresh == 1) {
				bgPaint.setColor(Color.YELLOW);
			} else {
				bgPaint.setColor(Color.GREEN);
			}
			if (TYPE == 0) {
				Thread.sleep(200);
			}
			songs.start();

		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void loseSound() {
		try {
			AssetFileDescriptor song = songList.get(3);
			songs.stop();
			songs.reset();
			songs.setDataSource(song.getFileDescriptor(),
					song.getStartOffset(), song.getDeclaredLength());
			songs.prepare();
			songs.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void winSound() {
		try {
			AssetFileDescriptor song = songList.get(4);
			songs.stop();
			songs.reset();
			songs.setDataSource(song.getFileDescriptor(),
					song.getStartOffset(), song.getDeclaredLength());
			songs.prepare();
			songs.start();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}