package kr.ac.kaist.kse.ic.cha.smartspeaker.esm.sound;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import kr.ac.kaist.kse.ic.cha.smartspeaker.command.ActionCommand;

public class SoundCommandHandler {
    private static final String TAG = SoundCommandHandler.class.getSimpleName();
    private static SoundCommandHandler instance = new SoundCommandHandler();

    public static SoundCommandHandler getInstance () {
        return instance;
    }

    private static Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if(msg.obj instanceof ActionCommand)
                ((ActionCommand) msg.obj).execute();
        }
    };

    private void i(String msg){
        Log.i(TAG, msg);
    }

    public void sendCommand(ActionCommand command, long delay) {
        Message msg = Message.obtain(mHandler);
        msg.obj = command;
        mHandler.sendMessageDelayed(msg, delay);
    }


    public void cancel() {
        i("cancel()");
        mHandler.removeCallbacksAndMessages(null);
    }
}
