package com.example.androidmdp2022;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothService {

    private static final String TAG = "BluetoothService";

    private static final String appName = "Group 18";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static boolean btConnectStatus = false;
    private static ConnectedThread connectedThread;
    private final BluetoothAdapter btAdapter;
    Context context1;
    ProgressDialog progressDialog;
    private UUID deviceUUID;
    Intent connectStatus;
    private AcceptThread insecureAcceptThread;
    private ConnectThread connectThread;
    private BluetoothDevice device;

    public BluetoothService(Context context) {
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context1 = context;
        startAcceptThread();
    }

    public static void write(byte[] out) {
        ConnectedThread tmp;

        Log.d(TAG, "write: Write is called.");
        connectedThread.write(out);
    }

    public synchronized void startAcceptThread() {
        Log.d(TAG, "start");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (insecureAcceptThread == null) {
            insecureAcceptThread = new AcceptThread();
            insecureAcceptThread.start();
        }
    }

    public void startClientThread(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        try {
            progressDialog = ProgressDialog.show(context1, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "StartClientThread Dialog show failure");
        }


        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");
        this.device = device;
        if (insecureAcceptThread != null) {
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }

        connectedThread = new ConnectedThread(mSocket);
        connectedThread.start();
    }

    // disconnect all devices and reset all thread
    public synchronized void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (insecureAcceptThread != null) {
            insecureAcceptThread.cancel();
            insecureAcceptThread = null;
        }
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = btAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
                Log.d(TAG, "Accept Thread: Setting up Server using: " + myUUID);
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread: IOException: " + e.getMessage());
            }
            ServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG, "run: AcceptThread Running. ");
            BluetoothSocket socket =null;
            try {
                Log.d(TAG, "run: RFCOM server socket start here...");

                socket = ServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            }catch (IOException e){
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }
            if(socket!=null){
                connected(socket, socket.getRemoteDevice());
            }
            Log.i(TAG, "END AcceptThread");
        }
        public void cancel(){
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try{
                ServerSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close AcceptThread ServerSocket " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID u){
            Log.d(TAG, "ConnectThread: started.");
            BluetoothService.this.device = device;
            deviceUUID = u;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "RUN: connectThread");

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + myUUID);
                tmp = device.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }
            mSocket = tmp;
            btAdapter.cancelDiscovery();

            try {
                mSocket.connect();

                Log.d(TAG, "RUN: ConnectThread connected.");

                connected(mSocket, device);

            } catch (IOException e) {
                try {
                    mSocket.close();
                    Log.d(TAG, "RUN: ConnectThread socket closed.");
                } catch (IOException e1) {
                    Log.e(TAG, "RUN: ConnectThread: Unable to close connection in socket."+ e1.getMessage());
                }
                Log.d(TAG, "RUN: ConnectThread: could not connect to UUID."+ myUUID);
                try {
                    MainActivity mBluetoothActivityActivity = (MainActivity) context1;
                    mBluetoothActivityActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context1, "Failed to connect to the Device.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception z) {
                    z.printStackTrace();
                }

            }
            try {
                progressDialog.dismiss();
            } catch(NullPointerException e){
                e.printStackTrace();
            }
        }

        public void cancel() {
            Log.d(TAG, "cancel: Closing Client Socket");
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close ConnectThread socket " + e.getMessage());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            connectStatus = new Intent("ConnectionStatus");
            connectStatus.putExtra("Status", "connected");
            connectStatus.putExtra("Device", device);
            LocalBroadcastManager.getInstance(context1).sendBroadcast(connectStatus);
            btConnectStatus = true;

            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = this.socket.getInputStream();
                tmpOut = this.socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    String incomingmessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: "+ incomingmessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingmessage);

                    LocalBroadcastManager.getInstance(context1).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. " + e.getMessage());

                    connectStatus = new Intent("ConnectionStatus");
                    connectStatus.putExtra("Status", "disconnected");
                    connectStatus.putExtra("Device", device);
                    LocalBroadcastManager.getInstance(context1).sendBroadcast(connectStatus);
                    btConnectStatus = false;

                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close ConnectThread socket " + e.getMessage());
            }
        }
    }
}
