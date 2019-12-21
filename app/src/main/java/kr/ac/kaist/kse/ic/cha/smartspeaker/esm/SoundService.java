package kr.ac.kaist.kse.ic.cha.smartspeaker.esm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity;
import kr.ac.kaist.kse.ic.cha.smartspeaker.R;
import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.sound.RecordSound;

public class SoundService extends Service implements RecordSound.OnCompletionListener{

    public final int ID_NOTIFICATION = 0x1;
    private final String TAG = SoundService.class.getSimpleName();
    private NotificationManager mNotiManager;

    private RecordSound mSound;

    private boolean isServiceClosable;


    private void i(String format) {
        Log.i(TAG, format);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        i("onCreate(): isServiceClosable == " + isServiceClosable);

        mNotiManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        showNotification();

        mSound = new RecordSound(this, this);
        if (!isServiceClosable)
            unregisterAlarm();

    }

    @Override
    public void onCompletion() {
        if(mSound == null)
            return;

        mSound.RepeatRecording();
        Log.i(TAG,"사운드녹음 시작함");

    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        i("onStartCommand()");
        mSound.RepeatRecording();
        return START_REDELIVER_INTENT;
    }


    private void stopESMService() {
        i("stopSoundService()");
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
;

        if(mSound != null) {
            mSound.release();
            mSound = null;
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
                .setContentTitle("Sound Service")
                .setContentText("Sound Service is running...")
                .setTicker("Sound Service is running...")
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





}
