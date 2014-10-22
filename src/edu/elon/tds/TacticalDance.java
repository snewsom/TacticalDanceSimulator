
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
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

import java.util.ArrayList;


public class TacticalDance extends Activity implements Callback {

    public static final String TAG = "TacticalDance";

    private static final int SERVER_LIST_RESULT_CODE = 42;

    public static final int UP = 3;

    public static final int DOWN = 4;

    public static final int FLIPTOP = 5;

    private TacticalDance self;

    private int mType; // 0 = server, 1 = client

    private SurfaceView mSurface;

    private SurfaceHolder mHolder;

    private Paint bgPaint;

    private Paint goalPaint;

    private Paint ballPaint;

    private Paint paddlePaint;

    private ArrayList<Point> mPaddlePoints;

    private ArrayList<Long> mPaddleTimes;

    private int mPaddlePointWindowSize = 5;

    private int mPaddleRadius = 55;

    private Bitmap mPaddleBmp;


    private int mBallRadius = 40;

    private Connection mConnection;
    
    private String hostDevice = "";

    private ArrayList<String> rivalDevices = new ArrayList<String>();

    private SoundPool mSoundPool;

    private int tockSound = 0;

    private MediaPlayer mPlayer;

    private int hostScore = 0;

    private int clientScore = 0;

    private OnMessageReceivedListener dataReceivedListener = new OnMessageReceivedListener() {
        public void OnMessageReceived(String device, String message) {
            if (message.indexOf("SCORE") == 0) {
                String[] scoreMessageSplit = message.split(":");
                hostScore = Integer.parseInt(scoreMessageSplit[1]);
                clientScore = Integer.parseInt(scoreMessageSplit[2]);
            } else {
               // mBall.restoreState(message);
            }
        }
    };

    private OnMaxConnectionsReachedListener maxConnectionsListener = new OnMaxConnectionsReachedListener() {
        public void OnMaxConnectionsReached() {

        }
    };

    private OnIncomingConnectionListener connectedListener = new OnIncomingConnectionListener() {
        public void OnIncomingConnection(String device) {
        	if(!rivalDevices.contains(device)){
        		rivalDevices.add(device);
        	}
        	for(String s : rivalDevices){
        		System.out.println(s);
        	}
            WindowManager w = getWindowManager();
            Display d = w.getDefaultDisplay();
            int width = d.getWidth();
            int height = d.getHeight();
           // mBall = new Demo_Ball(true, width, height - 60);
           // mBall.putOnScreen(width / 2, (height / 2 + (int) (height * .05)), 0, 0, 0, 0, 0);
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

                    connectionLostAlert.setPositiveButton("Ok", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    connectionLostAlert.setCancelable(false);
                    try {
                    connectionLostAlert.show();
                    } catch (BadTokenException e){
                        // Something really bad happened here; 
                        // seems like the Activity itself went away before
                        // the runnable finished.
                        // Bail out gracefully here and do nothing.
                    }
                }
            }
            self.runOnUiThread(new displayConnectionLostAlert());
        }
    };

    private OnConnectionServiceReadyListener serviceReadyListener = new OnConnectionServiceReadyListener() {
        public void OnConnectionServiceReady() {
            if (mType == 0) {
                mConnection.startServer(4, connectedListener, maxConnectionsListener,
                        dataReceivedListener, disconnectedListener);
                self.setTitle("Tactical Dance Sim 2K5: " + mConnection.getName() + "-" + mConnection.getAddress());
            } else {
                WindowManager w = getWindowManager();
                Display d = w.getDefaultDisplay();
                int width = d.getWidth();
                int height = d.getHeight();
               // mBall = new Demo_Ball(false, width, height - 60);
                Intent serverListIntent = new Intent(self, ServerListActivity.class);
                startActivityForResult(serverListIntent, SERVER_LIST_RESULT_CODE);
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        self = this;
        mPaddleBmp = BitmapFactory.decodeResource(getResources(), R.drawable.paddlelarge);

        mPaddlePoints = new ArrayList<Point>();
        mPaddleTimes = new ArrayList<Long>();

        Intent startingIntent = getIntent();
        mType = startingIntent.getIntExtra("TYPE", 0);

        setContentView(R.layout.main);
        mSurface = (SurfaceView) findViewById(R.id.surface);
        mHolder = mSurface.getHolder();

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        goalPaint = new Paint();
        goalPaint.setColor(Color.RED);

        ballPaint = new Paint();
        ballPaint.setColor(Color.GREEN);
        ballPaint.setAntiAlias(true);

        paddlePaint = new Paint();
        paddlePaint.setColor(Color.BLUE);
        paddlePaint.setAntiAlias(true);

        mPlayer = MediaPlayer.create(this, R.raw.collision);

        mConnection = new Connection(this, serviceReadyListener);
        mHolder.addCallback(self);
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

    private void doDraw(Canvas c) {
     /*   c.drawRect(0, 0, c.getWidth(), c.getHeight(), bgPaint);
        c.drawRect(0, c.getHeight() - (int) (c.getHeight() * 0.02), c.getWidth(), c.getHeight(),
                goalPaint);

        if (mPaddleTimes.size() > 0) {
            Point p = mPaddlePoints.get(mPaddlePoints.size() - 1);

            // Debug circle
            // Point debugPaddleCircle = getPaddleCenter();
            // c.drawCircle(debugPaddleCircle.x, debugPaddleCircle.y,
            // mPaddleRadius, ballPaint);
            if (p != null) {
                c.drawBitmap(mPaddleBmp, p.x - 60, p.y - 200, new Paint());
            }
        }
        if ((mBall == null) || !mBall.isOnScreen()) {
            return;
        }
        float x = mBall.getX();
        float y = mBall.getY();
        if ((x != -1) && (y != -1)) {
            float xv = mBall.getXVelocity();
            Bitmap bmp = BitmapFactory
                    .decodeResource(this.getResources(), R.drawable.android_right);
            if (xv < 0) {
                bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.android_left);
            }

            // Debug circle
            Point debugBallCircle = getBallCenter();
            // c.drawCircle(debugBallCircle.x, debugBallCircle.y, mBallRadius,
            // ballPaint);

            c.drawBitmap(bmp, x - 17, y - 23, new Paint());
            
        }*/
    }

    public void surfaceDestroyed(SurfaceHolder holder) {

    }

   

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == Activity.RESULT_OK) && (requestCode == SERVER_LIST_RESULT_CODE)) {
            String device = data.getStringExtra(ServerListActivity.EXTRA_SELECTED_ADDRESS);
            int connectionStatus = mConnection.connect(device, dataReceivedListener,
                    disconnectedListener);
            if (connectionStatus != Connection.SUCCESS) {
                Toast.makeText(self, "Unable to connect; please try again.", 1).show();
            } else {
            	//TODO i dont know if i wanna set host here
                hostDevice = device;
            }
            return;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
      /*  if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Point p = new Point((int) event.getX(), (int) event.getY());
            mPaddlePoints.add(p);
            mPaddleTimes.add(System.currentTimeMillis());
            if (mPaddleTimes.size() > mPaddlePointWindowSize) {
                mPaddleTimes.remove(0);
                mPaddlePoints.remove(0);
            }
        } else {
            mPaddleTimes = new ArrayList<Long>();
            mPaddlePoints = new ArrayList<Point>();
        }*/
        return false;
    }

    


}
