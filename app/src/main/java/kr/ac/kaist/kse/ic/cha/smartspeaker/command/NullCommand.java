package kr.ac.kaist.kse.ic.cha.smartspeaker.command;

import android.util.Log;

public class NullCommand extends ActionCommand {
    private final static String TAG = ActionCommand.class.getSimpleName();

    public NullCommand(){
    }

    private void i(String msg){
        Log.i(TAG, msg);
    }

    @Override
    public boolean execute() {
        i("execute()");
        if(super.mListener != null)
            super.mListener.onCompletion(this);
        return true;
    }

    public void release() {
        i("release()");
        super.mListener = null;
    }
}
