package kr.ac.kaist.kse.ic.cha.smartspeaker.esm.sound;

import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class SoundRecorder {
    private final String TAG = getClass().getSimpleName();
    private enum State {PREPARED, RECORDING, IDLE}

    private State mState;
    private MediaRecorder mRecorder;
    private MediaRecorder mRrecorder;

    private File mFile;

    private void i(String format){
        Log.i( TAG, format);
    }
    private void e(String format){
        Log.e( TAG, format);
    }

    public SoundRecorder() {
        i("SoundRecorder()");
        mRecorder = new MediaRecorder();
        mRrecorder = new MediaRecorder();

        mState = State.IDLE;
    }

    public synchronized void setRecordFile (File f){
        if(mState == State.IDLE) {
            i("setRecordFile(f = " + f.getAbsolutePath() + ")");
            mFile = f;
        }
    }

    public synchronized boolean reset() {
        if(mState == State.PREPARED || mState == State.IDLE) {
            i("reset()");
            mState = State.IDLE;
            mRecorder.reset();
            mRrecorder.reset();

            mFile = null;
            return true;
        }
        return false;
    }

    public synchronized boolean prepare() {
        if(mState != State.IDLE || mFile == null)
            return false;

        i("prepare()");

        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

        if (Build.VERSION.SDK_INT >= 10) {
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setAudioEncodingBitRate(96000);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        } else {
            mRecorder.setAudioSamplingRate(8000);
            mRecorder.setAudioEncodingBitRate(12200);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }

        mRecorder.setOutputFile(mFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mState = State.PREPARED;
            return true;
        } catch (IOException e) {
            e(e.getMessage());
        }
        return false;
    }

    public synchronized boolean start() {
        if(mState == State.PREPARED) {
            i("execute()");
            mRecorder.start();
            mState = State.RECORDING;
            return true;
        }
        return false;
    }

    public synchronized boolean stop() {
        if(mState == State.RECORDING) {
            i("stop()");
            mRecorder.stop();
            mState = State.IDLE;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        i("release()");
        mRecorder.release();
        mRecorder = null;
        mFile = null;
    }
}
