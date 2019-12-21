package kr.ac.kaist.kse.ic.cha.smartspeaker.esm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.badoo.mobile.util.WeakHandler;


import kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity;
import kr.ac.kaist.kse.ic.cha.smartspeaker.R;
import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer;

public class ESMService extends Service implements ESMPlayer.OnCompletionListener{

    public static final long SECOND = 1000;
    public static final long MINUTE = 60*SECOND;


    //Random interval from INTERVAL_MIN_MILLIS to INTERVAL_MAX_MILLIS
    //You can change random interval here
    public static final long INTERVAL_MIN_MILLIS = 15 * MINUTE;
    public static final long INTERVAL_MAX_MILLIS = 25 * MINUTE;

    public static final String EXTRA_INTERVAL_MIN = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.EXTRA_NUM";
    public static final String EXTRA_INTERVAL_MAX = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.EXTRA_MAX";

    public static final String ACTION_RESTART_SERVICE = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ACTION_RESTART_SERVICE";
    public static final String ACTION_CLOSE_SERVICE = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ACTION_CLOSE_SERVICE";

    public static final String ACTION_FORCE_TO_PLAY_ESM = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService.ACTION_PLAY_FORCE_NBACK";

    public static final String ACTION_ESM_START = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService.ACTION_ESM_START";
    public static final String ACTION_ESM_STOP = "kr.ac.kaist.kse.ic.cha.smartspeaker.esm.ESMService.ACTION_ESM_STOP";

    public final int ID_NOTIFICATION = 0x1;
    private final String TAG = ESMService.class.getSimpleName();
    private NotificationManager mNotiManager;

    private ESMPlayer mPlayer;

    private final BroadcastReceiver playForcedESMkListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(ACTION_FORCE_TO_PLAY_ESM) && mPlayer != null) {
                mPlayer.playForcedESM();
                Log.i(TAG,"Received FORCE FLAG");
            }
        }
    };

    private boolean isServiceClosable;

    private ESMServiceCloseBroadcastReceiver mBroadcastReceiver;

    private void i(String format) {
        Log.i(TAG, format);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        i("onCreate(): isServiceClosable == " + isServiceClosable);

        registerReceiver(playForcedESMkListener, new IntentFilter(ACTION_FORCE_TO_PLAY_ESM));

        mNotiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();

        mBroadcastReceiver = new ESMServiceCloseBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RESTART_SERVICE);
        filter.addAction(ACTION_CLOSE_SERVICE);
        registerReceiver(mBroadcastReceiver, filter);

        if (!isServiceClosable)
            unregisterAlarm();
    }

    @Override
    public void onCompletion() {
        if(mPlayer == null)
            return;
        mPlayer.playESM();
        Log.i(TAG,"랜덤ESM을 예약함");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        i("onStartCommand()");
        long min = intent.getLongExtra(EXTRA_INTERVAL_MIN, INTERVAL_MIN_MILLIS);
        long max = intent.getLongExtra(EXTRA_INTERVAL_MAX, INTERVAL_MAX_MILLIS);

        startESMService(min, max);
        return START_REDELIVER_INTENT;
    }

    private void startESMService(long min, long max) {
        i("startESMService()");
        if(mPlayer != null)
            return;

        mPlayer = new ESMPlayer(this, min, max,  this);
        mPlayer.playESM();
        WeakHandler h = new WeakHandler(Looper.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                if (mPlayer != null)
                    mPlayer.playESM();
            }
        });
    }

    private void stopESMService() {
        i("stopESMService()");
        isServiceClosable = true;
        stopSelf();
    }

    @Override
    public void onDestroy() {
        i("onDestroy(): isServiceClosable == " + isServiceClosable);
        super.onDestroy();

        if (!isServiceClosable)
            registerAlarm();

        mNotiManager.cancel(ID_NOTIFICATION);

        unregisterReceiver(mBroadcastReceiver);
        mBroadcastReceiver = null;

        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void showNotification() {
        i("showNotification()");

        PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Notification noti = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ESM Service")
                .setContentText("ESM Service is running...")
                .setTicker("ESM Service is running...")
                .setWhen(System.currentTimeMillis())
                .setContentIntent(intent)
                .setOngoing(true)
                .build();
        mNotiManager.notify(ID_NOTIFICATION, noti);
    }

    private void registerAlarm() {
        //TODO:
        /*
        i("registerAlarm()");
        Intent intent = new Intent(ESMService.this, ESMServiceRestartBroadcastReceiver.class);
        intent.setAction(ACTION_RESTART_SERVICE);

        intent.putExtra(EXTRA_SERVICE_TYPE, serviceType);

        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 10 * 1000, 60 * 1000, sender);
        */
    }

    private void unregisterAlarm() {
        //TODO:
        /*
        i("unregisterAlarm()");
        Intent intent = new Intent(ESMService.this, ESMServiceRestartBroadcastReceiver.class);
        intent.setAction(ACTION_RESTART_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(sender);
        */
    }

    public class ESMServiceCloseBroadcastReceiver extends BroadcastReceiver {
        public final String TAG = getClass().getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            i("onReceive(intent = " + intent.getAction() + ")");

            if (intent.getAction().equals(ACTION_CLOSE_SERVICE)) {
                stopESMService();
            }
        }
    }




}
