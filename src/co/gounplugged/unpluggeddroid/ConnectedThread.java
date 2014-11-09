package co.gounplugged.unpluggeddroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class ConnectedThread extends Thread {
	
	// Constants
	private String TAG = "ConnectedThread";
	
	// State
	public static final int DISCONNECTED = 0;
	public static final int STREAMING = 1;
	private int state;
	
	// Bluetooth SDK
	private final BluetoothSocket mBluetoothSocket;
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
 
    public ConnectedThread(BluetoothSocket socket) {
    	mBluetoothSocket = socket;
        InputStream tInputStream = null;
        OutputStream tOutputStream = null;
 
        // Get the input and output streams, using temp objects because
        // member streams are final
        try {
        	tInputStream = socket.getInputStream();
        	tOutputStream = socket.getOutputStream();
        } catch (IOException e) { }
 
        mInputStream = tInputStream;
        mOutputStream = tOutputStream;
    }
 
    public void run() {
        byte[] buffer = new byte[1024];  // buffer store for the stream
        int bytes; // bytes returned from read()
 
        // Keep listening to the InputStream until an exception occurs
        while (true) {
            try {
                // Read from the InputStream
                bytes = mInputStream.read(buffer);
                // Send the obtained bytes to the UI activity
//                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
//                        .sendToTarget();
                String str = new String(buffer, "UTF-8");
            	Log.d(TAG, "reveived chat" + str);
            } catch (IOException e) {
                break;
            }
        }
    }
 
    /* Call this from the main activity to send data to the remote device */
    public void write(byte[] bytes) {
    	Log.d(TAG, "writing chat");
        try {
        	mOutputStream.write(bytes);
        	Log.d(TAG, "chat wrote");
        } catch (IOException e) { }
    }
}