package kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity;
import kr.ac.kaist.kse.ic.cha.smartspeaker.command.NullCommand;
import kr.ac.kaist.kse.ic.cha.smartspeaker.command.ActionCommand;

import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.endTime;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.startTime;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.MainActivity.isSaved;

public class ESMPlayer implements ActionCommand.OnCompletionListener {
    private final String TAG = getClass().getSimpleName();

    private final Context mContext;
    private final long MIN_INTERVAL_ESM_MILLIS;

    private final OnCompletionListener onCompletionListener ;

    private long MAX_INTERVAL_ESM_MILLIS;


    private static boolean isReserved  = false;
    private static boolean isPlaying = false;
    private static boolean isForced = false;

    public static long ESMshouldStartAfter = 0L;
    public static long Deletefrom = 0L;
    public static long Deleteto = 0L;

    private NullCommand mStartCommand;
//    private NullCommand mRecordingCommand;
    private ESMPlayCommand mAskPromptCommand;
    private NullCommand mEndCommand;
//    private ESMRecorder mRecorder;
    private NullCommand mPassCommand;

    private static final long SECOND = 1000;

    private static final long MINUTE = 60*SECOND;

    public static long ESMTime = 0;
    public static long ESMTimeFrom = 0;
    public static long ESMTimeTo = 0;

    public final static ArrayList<Long> timeFiles = new ArrayList<Long>();

    public interface OnCompletionListener {
        void onCompletion();
    }

    private void i(String msg){
        Log.i(TAG, msg);
    }

    private void _playESM(long delay) {
        //when it is playing time (=operation time), ESM is reserved. If not, it does not prompt any ESM sound.
        Log.i(TAG,"_playESM에 들어갔다.");
        isReserved = true;
        if(isPlayingTime()) {
            ESMCommandHandler.getInstance().sendCommand(mStartCommand, delay);
        }else{
            Log.i(TAG,"mPassCommand 보냈다.");
            ESMCommandHandler.getInstance().sendCommand(mPassCommand, 10 * MINUTE);

        }

    }

    public static boolean isPlayingTime(){
        //ESM should prompt only at working time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int min = calendar.get(Calendar.MINUTE);
        return (startTime <=hour && hour <= (endTime-1));
    }

    private static Calendar lastESMCalendar  = Calendar.getInstance();


    public static boolean isAfter15Mins(){
        //To maintain at least 15 minutes interval between ESMs
        lastESMCalendar = Calendar.getInstance();
        long currentTime=lastESMCalendar.getTimeInMillis();
        if (currentTime >= ESMshouldStartAfter){
            return true;
        }else{
            return false;
        }
    }


    public void playForcedESM() {
        //when movement detected triggered ESM, it immediately prompted ESM
        if(isPlaying)
            return;

        if(!isPlayingTime() && !isAfter15Mins())
            return;

        isPlaying = true;
        isForced = true;
        isSaved = false;
        Log.i(TAG,"forcedESM시작됨");
        ESMCommandHandler.getInstance().cancel();
        _playESM(0);
    }

    public static boolean isPlaying(){
        return isPlaying;
    }

    public void playESM() {
        if(isReserved)
            return;
        if(isPlaying)
            return;

        i("playESM()");

        long delay = getRandomInterval();

        _playESM(delay);
    }


    public ESMPlayer(Context context, long minInterval, long maxInterval, OnCompletionListener onCompletionListener) {
        Validate.notNull(context);

        mContext = context;
        this.onCompletionListener = onCompletionListener;
        this.MIN_INTERVAL_ESM_MILLIS = minInterval;
        this.MAX_INTERVAL_ESM_MILLIS = maxInterval;

        mStartCommand = new NullCommand();
        mStartCommand.setOnCompletionListener(this);

//        mRecordingCommand = new NullCommand();
//        mRecordingCommand.setOnCompletionListener(this);

        mAskPromptCommand = new ESMPlayCommand();
        mAskPromptCommand.setOnCompletionListener(this);

        mEndCommand = new NullCommand();
        mEndCommand.setOnCompletionListener(this);

        mPassCommand = new NullCommand();
        mPassCommand.setOnCompletionListener(this);

//        mRecorder = new ESMRecorder();
    }

    public void release() {
        i("release()");
        ESMCommandHandler.getInstance().cancel();

        mStartCommand.release();

        mAskPromptCommand.stop();
//        mRecorder.stop();

        mStartCommand.release();
        mAskPromptCommand.release();
//        mRecorder.release();

        mStartCommand = null;

        isPlaying = false;


        mAskPromptCommand = null;
//        mRecorder = null;
    }




    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath + "/ESM");
        if (!file.exists())
            file.mkdirs();

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String filename;

        if(!isForced) {
            filename = (file.getAbsolutePath() + "/" + timeStamp +"R"+ ".3gp");
        }else{
            filename = (file.getAbsolutePath() + "/" + timeStamp +"D"+ ".3gp");
        }

        isForced = false;

        return filename;
    }

    public void Update(String s){
        MainActivity.recording.setText(s);
    }


    @Override
    public void onCompletion(ActionCommand command) {
        if (command.equals(mStartCommand)) {
            Log.i(TAG,"ESM이 시작되었다");
            isPlaying = true;

            mAskPromptCommand.reset();
            mAskPromptCommand.setResource(mContext);
            mAskPromptCommand.prepare();
            ESMCommandHandler.getInstance().sendCommand(mAskPromptCommand, 100);

        }
        else if (command.equals(mAskPromptCommand)) {
            // As you know, it is repeatedly and continuously recording, regardless ESM (duration : 30 seconds) during operation time.
            // When ESM question prompted, 2 minutes before ESM and 2 minutes after ESM should remained as data. (from ESMTimeFrom to ESMTimeTo)
            // Data out of the time range should be eliminated
            lastESMCalendar = Calendar.getInstance();
            ESMTime=lastESMCalendar.getTimeInMillis();
            lastESMCalendar.add(Calendar.MINUTE, -2);
            ESMTimeFrom=lastESMCalendar.getTimeInMillis();
            lastESMCalendar.add(Calendar.MINUTE, 4);
            ESMTimeTo=lastESMCalendar.getTimeInMillis();
            ESMCommandHandler.getInstance().sendCommand(mEndCommand, SECOND);

        } else if (command.equals(mEndCommand)) {

            isReserved = false;
            isPlaying = false;
            Update("Collecting data...");
            //To maintain at least 15 minutes interval between ESMs
            lastESMCalendar = Calendar.getInstance();
            lastESMCalendar.add(Calendar.MINUTE, 15);
            ESMshouldStartAfter=lastESMCalendar.getTimeInMillis();
            // To eliminate recording files out of the time range
            lastESMCalendar.add(Calendar.MINUTE, -13);
            Deletefrom=lastESMCalendar.getTimeInMillis();

            lastESMCalendar.add(Calendar.MINUTE, 11);
            Deleteto=lastESMCalendar.getTimeInMillis();

            if (onCompletionListener != null)
                onCompletionListener.onCompletion();

        } if (command.equals(mPassCommand)) {
            Log.i("TEST", "mPassCommand received");
            isReserved = false;
            isPlaying = false;
            isForced = false;
            if (onCompletionListener != null)
                onCompletionListener.onCompletion();

        }
    }

    private long getRandomInterval() {
        long between = MAX_INTERVAL_ESM_MILLIS - MIN_INTERVAL_ESM_MILLIS;
        long rand = (long) (Math.random() * between);
        return MIN_INTERVAL_ESM_MILLIS + rand;
    }

}
