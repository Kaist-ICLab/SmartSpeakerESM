package kr.ac.kaist.kse.ic.cha.smartspeaker.command;

public abstract class ActionCommand {
    protected OnCompletionListener mListener;
    public abstract boolean execute();


    public ActionCommand(){

    }

    public interface OnCompletionListener {
        void onCompletion(ActionCommand command);
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mListener = listener;
    }
}
