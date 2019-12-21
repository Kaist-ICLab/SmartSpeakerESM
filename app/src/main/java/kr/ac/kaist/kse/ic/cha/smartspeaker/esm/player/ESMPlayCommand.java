package kr.ac.kaist.kse.ic.cha.smartspeaker.esm.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.location.Location;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

import kr.ac.kaist.kse.ic.cha.smartspeaker.R;
import kr.ac.kaist.kse.ic.cha.smartspeaker.command.ActionCommand;


public class ESMPlayCommand extends ActionCommand implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private final static String TAG = ESMPlayCommand.class.getSimpleName();

    private enum State {IDLE, PREPARED, PLAYING}

    private MediaPlayer mPlayer;
    private State mState;

    //Result
    private long mStartTime;
    private long mEndTime;

    private Location mStartLocation;
    private Location mEndLocation;

    private void i(String msg){
        Log.i(TAG, msg);
    }

    private void e(String msg){
        Log.e(TAG, msg);
    }

    public ESMPlayCommand() {
        i("ESMPromptPlayCommand()");

        mPlayer = new MediaPlayer();
        initResult();

        mState = State.IDLE;
    }

    private void initResult() {
        mStartTime = -1;
        mEndTime = -1;
        mStartLocation = null;
        mEndLocation = null;
    }

    public long getStartPlayTime() {
        return mStartTime;
    }

    public long getEndPlayTime() {
        return mEndTime;
    }

    public Location getStartPlayLocation(){
        return mStartLocation;
    }

    public Location getEndPlayLocation(){
        return mEndLocation;
    }

    public synchronized void setResource(Context context) {
        if(mState == State.IDLE) {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(R.raw.esmvoicefinal);
            try {
                mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                afd.close();
            } catch (IOException e) {
                e(e.getMessage());
            }
        } else
            i("setResource() failed: State != IDLE");
    }

    public synchronized boolean reset() {
        if(mState == State.IDLE || mState == State.PREPARED) {
            i("reset()");
            mPlayer.reset();
            initResult();
            setState(State.IDLE);
            return true;
        }
        i("reset() failed: State != IDLE && State != PREPARED");
        return false;
    }

    public synchronized boolean prepare() {
        if(mState == State.IDLE) {
            i("prepare()");
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnPreparedListener(this);

            setState(State.PREPARED);
            return true;
        }
        i("prepare() failed: State != IDLE");
        return false;
    }

    public synchronized boolean execute() {
        if(mState == State.PREPARED) {
            i("execute()");
            mPlayer.prepareAsync();
            return true;
        }
        i("execute() failed: State != PREPARED");
        return false;
    }

    public synchronized boolean stop() {
        if(mState == State.PLAYING) {
            i("stop()");
            mPlayer.stop();
            setState(State.IDLE);
            return true;
        }
        i("stop() failed: State != PLAYING");
        return false;
    }

    public synchronized void release() {
        if(mState == State.IDLE) {
            i("release()");
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
            super.mListener = null;
            return;
        }
        i("release() failed: State != IDLE");
        return;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        i("onPrepared(MediaPlayer)");
        setState(State.PLAYING);
        mediaPlayer.start();
        mStartTime = System.currentTimeMillis();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        i("onCompletion(MediaPlayer)");
        setState(State.IDLE);

        mEndTime = System.currentTimeMillis();

        if(super.mListener != null)
            super.mListener.onCompletion(this);
    }

    private void setState(State state) {
        i("setState():"  + mState.name() + " -> " + state.name());
        mState = state;
    }
}
