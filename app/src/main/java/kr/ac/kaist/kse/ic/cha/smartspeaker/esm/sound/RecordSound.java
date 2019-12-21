package kr.ac.kaist.kse.ic.cha.smartspeaker.esm.sound;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.lang3.Validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import kr.ac.kaist.kse.ic.cha.smartspeaker.command.ActionCommand;
import kr.ac.kaist.kse.ic.cha.smartspeaker.command.NullCommand;
import kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer;

import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.Deletefrom;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.Deleteto;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.ESMTime;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.ESMTimeFrom;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.ESMTimeTo;
import static kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player.ESMPlayer.isPlayingTime;


public class RecordSound implements ActionCommand.OnCompletionListener {
    private final String TAG = getClass().getSimpleName();

    private final Context mContext;
    private final RecordSound.OnCompletionListener onCompletionListener ;
    private NullCommand mStartCommand;
    private NullCommand mCheckCommand;
    private NullCommand mEndCommand;
    private NullCommand mPassCommand;
    private SoundRecorder mRecorder;

    public final static ArrayList<Long> recordFiles = new ArrayList<Long>();



    private static final long SECOND = 1000;

    private static final long MINUTE = 60*SECOND;
    private static boolean isRecording = false;

    public interface OnCompletionListener {
        void onCompletion();
    }

    private void i(String msg){
        Log.i(TAG, msg);
    }

    public void RepeatRecording() {
        if(isRecording)
            return;
        isRecording=true;
        if(isPlayingTime()) {
            if(isTimeToRecord()) {
                Log.i(TAG,"녹음중");
                SoundCommandHandler.getInstance().sendCommand(mStartCommand, 0);
            }else{
                Log.i(TAG,"녹음안함");
                SoundCommandHandler.getInstance().sendCommand(mCheckCommand, 0);
            }
        }
        else{
            SoundCommandHandler.getInstance().sendCommand(mPassCommand, 10 * MINUTE);
        }

    }


    public RecordSound(Context context, RecordSound.OnCompletionListener onCompletionListener) {
        Validate.notNull(context);
        mContext = context;
        this.onCompletionListener = onCompletionListener;

        mStartCommand = new NullCommand();
        mStartCommand.setOnCompletionListener(this);

        mCheckCommand = new NullCommand();
        mCheckCommand.setOnCompletionListener(this);

        mEndCommand = new NullCommand();
        mEndCommand.setOnCompletionListener(this);

        mPassCommand = new NullCommand();
        mPassCommand.setOnCompletionListener(this);

        mRecorder = new SoundRecorder();
    }

    public void release() {
        i("release()");
        SoundCommandHandler.getInstance().cancel();

        mStartCommand.release();
        mRecorder.stop();

        mCheckCommand.release();
        mRecorder.release();

        mStartCommand = null;;
        mCheckCommand=null;
        mRecorder = null;
    }

    private static Calendar lastESMCalendar  = Calendar.getInstance();

    public static boolean isTimeToRecord(){
        lastESMCalendar = Calendar.getInstance();
        long currentTime=lastESMCalendar.getTimeInMillis();
        if (currentTime >= Deletefrom && currentTime <=Deleteto){
            return false;
        }else{
            return true;
        }
    }


    private String getFilename(String timeStamp) {
        //filepaths where recording sound data are saved
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath + "/SoundData");
        if (!file.exists())
            file.mkdirs();
//        String timeStamp = new SimpleDateFormat("HHmm").format(new Date());
        return file.getAbsolutePath() + "/" + timeStamp +".3gp";
    }

    @Override
    public void onCompletion(ActionCommand command) {
        if (command.equals(mStartCommand)) {
            mRecorder.reset();
            lastESMCalendar = Calendar.getInstance();
            long recordingTime=lastESMCalendar.getTimeInMillis();
            String timeStamp = String.valueOf(recordingTime);
            mRecorder.setRecordFile(new File(getFilename(timeStamp)));
            recordFiles.add(recordingTime);
            Log.i(TAG,"sound data 녹음이 시작되었다"+recordingTime);
            mRecorder.prepare();
            mRecorder.start();
            SoundCommandHandler.getInstance().sendCommand(mEndCommand, 30*SECOND);
            Log.i(TAG,"여기");
        } else if (command.equals(mEndCommand)) {
            Log.i(TAG,"여기22");
            mRecorder.stop();
            isRecording=false;
            if (onCompletionListener != null)
                onCompletionListener.onCompletion();

        } else if (command.equals(mCheckCommand)) {
            //여기서 뭔가를 한다. 저장을 한다. ESM타임이 있지 그때를 기준으로 더 이상녹음 없을거란 마리야..
            if(recordFiles!=null && !recordFiles.isEmpty()) {
                Log.i(TAG,"처리중");
                String filepath = Environment.getExternalStorageDirectory().getPath();
                String inputpath = filepath + "/SoundData";
                String outputpath = filepath + "/ESM";
                for (int i = 0; i < recordFiles.size(); i += 1) {
                    if (recordFiles.get(i) > ESMTimeFrom && recordFiles.get(i) < ESMTimeTo) {
                        //Target sound files should be moved to the ESM folder
                        String inputFile = "/" + recordFiles.get(i).toString() + ".3gp";
                        moveFile(inputpath, inputFile, outputpath);
                    } else {
                        //Non-target sound files should be deleted
                        String inputFile = "/" + recordFiles.get(i).toString() + ".3gp";
                        deleteFile(inputpath, inputFile);
                    }

                }
                recordFiles.clear();
            }
            SoundCommandHandler.getInstance().sendCommand(mPassCommand, SECOND);
        } if (command.equals(mPassCommand)) {
            isRecording=false;
            Log.i("TEST", "mPassCommand received");
            if (onCompletionListener != null)
                onCompletionListener.onCompletion();

        }
    }
    private void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File (outputPath);
            if (!dir.exists())
            {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + inputFile);
            out = new FileOutputStream(outputPath + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + inputFile).delete();


        }

        catch (FileNotFoundException fnfe1) {
            Log.e("tag", fnfe1.getMessage());
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

    }
    private void deleteFile(String inputPath, String inputFile) {
        try {
            // delete the original file
            new File(inputPath + inputFile).delete();
        }
        catch (Exception e) {
            Log.e("tag", e.getMessage());
        }
    }
}



