package co.gounplugged.unpluggeddroid;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class UnpluggedNode extends Thread {
	private static final String TAG = "UnpluggedNode";
	private final String note;
	
	// State
	public static final int DISCONNECTED = 0;
	public static final int CONNECTED = 1;
	public static final int ACCEPTING = 2;
	public static final int CONNECTING = 3;
	protected int state;
	
	// Bluetooth
	protected UUID uuid;
	protected BluetoothAdapter mBluetoothAdapter;
	protected UnpluggedConnectedThread connectedThread;
	protected BluetoothSocket mBluetoothSocket;
	protected final Handler mHandler;
	
	public UnpluggedNode(Handler handler, BluetoothAdapter bluetoothAdapter, UUID uuid_, String note_) {
		this.state = DISCONNECTED;
		this.mHandler = handler;
		this.mBluetoothAdapter = bluetoothAdapter;
		this.uuid = uuid_;
		this.note = note_;
	}

    public void chat(String str) {
    	Log.d(TAG, "writing chat");
    	if (state == CONNECTED) {
    		Log.d(TAG, "writing chat cuz connected");
    		connectedThread.write(str.getBytes());		
    	}
    }
    
    public synchronized int getConnectionState() {
    	return this.state;
    }
    
    protected synchronized void setState(int state_) {
    	Log.d(TAG, note + "setState() " + state + " -> " + state_);
    	this.state = state_;
    	// Give the new state to the Handler so the UI Activity can update
    	mHandler.obtainMessage(UnpluggedMessageHandler.STATE_CHANGED, state, -1).sendToTarget();
	}
    
    public Handler getHandler() {
    	return this.mHandler;
    }
    
    protected void cancel() {
       	
		try {
			if (connectedThread != null) {
				connectedThread.cancel();
				connectedThread = null;
			}
			if (mBluetoothSocket != null) {
				mBluetoothSocket.close();
				mBluetoothSocket = null;
			}
			setState(DISCONNECTED);
		} catch (IOException e) {} 		
		
	}

}
