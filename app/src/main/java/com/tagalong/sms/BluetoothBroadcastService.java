package com.tagalong.sms;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BluetoothBroadcastService extends Service {

    private BluetoothAdapter bluetoothAdapter;
    private static final String uuid = "07f2934c-1e81-4554-bb08-44aa761afbfb";
    private ConnectedThread handleThread;
    private ConnectThread connectThread;
    private ArrayList<BluetoothDevice> deviceList;
    private String msg="";
    private boolean found;
    private Object lock = new Object();
    private boolean isRegistration = false;
    private boolean tryHarder = true;
    private boolean tryingHarder = false;

    public BluetoothBroadcastService() {
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //checking for non cached uuid if server uuid not chached
            synchronized (lock) {
                if(!found) {
                    if (BluetoothDevice.ACTION_UUID.equals(intent.getAction())) {
                        BluetoothDevice deviceExtra = intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                        Parcelable[] uuidExtra = intent.getParcelableArrayExtra("android.bluetooth.device.extra.UUID");
                        if (uuidExtra != null) {
                            for (Parcelable e : uuidExtra) {
                                if (e.toString().equals("07f2934c-1e81-4554-bb08-44aa761afbfb")) {
                                    Log.d("REAL","Found thee uuid from sdp");
                                    found = true;
                                    connectThread = new ConnectThread(deviceExtra,isRegistration);
                                    connectThread.start();
                                    break;
                                }
                            }

                            if(!found){
                                Log.d("HARDER","TRYING HARDER");
                                    if (tryHarder) {
                                        tryHarder();
                                        tryHarder = false;
                                        tryingHarder = true;
                                    }

                            }

                        }
                    }
                }
            }
        }
    };

    private void tryHarder(){
        for (BluetoothDevice b : bluetoothAdapter.getBondedDevices()){
            if(b.getName().toLowerCase().contains("glass")) {
                connectThread = new ConnectThread(b, isRegistration);
                connectThread.start();
                break;
            }
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // start asynctask to get off this thread
        if(intent != null) {
            deviceList = new ArrayList<BluetoothDevice>();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_UUID);


            this.registerReceiver(mReceiver, filter);
            msg = intent.getStringExtra("MESSAGE");
            isRegistration = intent.getBooleanExtra("register",false);
            Log.d("HERE", "GOT MESSAGE FROM RECEIVER: " + msg);

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice glass = null;
            found = false;

            //check for cached uuid (Faster)

            for (BluetoothDevice b : bluetoothAdapter.getBondedDevices()) {
                ParcelUuid[] uuids = b.getUuids();

                Log.d("DEVICE", b.getName());

                if(uuids != null) {
                    for (ParcelUuid p : uuids) {
                        Log.d("UUID", p.getUuid().toString());
                        if (p.getUuid().toString().equals("07f2934c-1e81-4554-bb08-44aa761afbfb")) {
                            glass = b;
                            found = true;
                            break;
                        }
                    }
                }
                if (found)
                    break;
            }

            if (found) {
                Log.d("PAIR", "PAIRED WITH: " + glass.getName());
                connectThread = new ConnectThread(glass,isRegistration);
                connectThread.start();
            } else {
                for (BluetoothDevice b : bluetoothAdapter.getBondedDevices()) {
                    b.fetchUuidsWithSdp();
//                    if(b.getName().equalsIgnoreCase("Christian D Vazquez Machado's Glass")){
//                        found = true;
//                        connectThread = new ConnectThread(b,isRegistration);
//                        connectThread.start();
//                    }
                }
            }
            return Service.START_STICKY;
        } else {
            return Service.START_NOT_STICKY;
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private static final String TAG = "CONNECTTHREAD";
        private boolean register;

        public ConnectThread(BluetoothDevice device,boolean register) {
            Log.e(TAG, "ConnectThread start....");
            this.register = register;
            BluetoothSocket tmp = null;
            mmDevice = device;
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
            } catch (Exception e) {
                Log.e(TAG,"Danger Will Robinson");
                e.printStackTrace();
            }
            mmSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            Log.e(TAG,"stopping discovery");
            boolean retry = register;

                try {
                    Log.e(TAG, "connecting to : "+mmDevice.getName());
                    mmSocket.connect();
                    retry = false;
                } catch (IOException connectException) {

                    connectException.printStackTrace();

                    Log.e(TAG, "failed to connect");

                    try {
                        Log.e(TAG, "close-ah-da-socket");
                        mmSocket.close();
                    } catch (IOException closeException) {
                        Log.e(TAG, "failed to close hte socket");
                    }
                    Log.e(TAG, "returning..");
                    if(!retry || tryingHarder)
                        return;
                    else {
                        try{
                            Thread.sleep(1500);
                            connectThread = new ConnectThread(mmDevice,isRegistration);
                            connectThread.start();
                        } catch (InterruptedException e){

                        }
                    }
                }


            Log.e(TAG,"we can now manage our connection!");
            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }



    public void manageConnectedSocket(BluetoothSocket mmSocket) {
        handleThread = new ConnectedThread(mmSocket);
        handleThread.start();
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private static final String TAG = "CONNECTEDTHREAD";

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            // Keep listening to the InputStream while connected

            try {

                String string = msg;
                mmOutStream.write(string.getBytes(Charset.forName("UTF-8")));


            } catch (Exception e) {
                Log.e(TAG, "disconnected", e);
                connectionLost();
                //                  break;
            }

        }
        public void connectionLost() {
            cancel();
        }
        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            Log.d("CLOSE","CLOSING SOCKET");
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

}
