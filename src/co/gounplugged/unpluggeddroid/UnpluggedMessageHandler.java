package co.gounplugged.unpluggeddroid;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import co.gounplugged.unpluggeddroid.adapter.MessageAdapter;

public class UnpluggedMessageHandler extends Handler {
	private static final String TAG = "UnpluggedMessageHandler";
	
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_WRITE = 2;
    public static final int STATE_CHANGED = 3;
    
    private MessageAdapter mMessageAdapter;
    private MenuItem mItemConnectionStatus;

	public UnpluggedMessageHandler(MessageAdapter messageAdapter, MenuItem itemConnectionStatus) {
    	this.mMessageAdapter = messageAdapter;
        mItemConnectionStatus = itemConnectionStatus;
    }
    
	@Override
    public void handleMessage(Message msg) {
	    switch (msg.what) {
		    case MESSAGE_WRITE:
			    byte[] writeBuf = (byte[]) msg.obj;
			    // construct a string from the buffer
			    String writeMessage = new String(writeBuf);
			    mMessageAdapter.addMessage("Me: " + writeMessage);
			    break;
		    
		    case MESSAGE_READ:
			    byte[] readBuf = (byte[]) msg.obj;
			    // construct a string from the valid bytes in the buffer
			    String readMessage = new String(readBuf);
			    mMessageAdapter.addMessage("SOMEONE: " + readMessage);
			    break;
		    
		    case STATE_CHANGED:
		    	 switch (msg.arg1) {
			    	 case UnpluggedMesh.STATE_DISCONNECTED:
                         if (mItemConnectionStatus != null)
                            mItemConnectionStatus.setTitle("Disconnected");
				    	 break;
			    	 case UnpluggedMesh.STATE_CONNECTED:
                         if (mItemConnectionStatus != null)
                             mItemConnectionStatus.setTitle("Connected");
                         break;
		    	 }
		    	 Log.d(TAG, msg.what + " " + msg.arg1);
		    	 break;
	    }
    }
}
