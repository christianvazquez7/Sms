package com.tagalong.sms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import com.parse.Parse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class SmsReciever extends BroadcastReceiver {

    final SmsManager sms = SmsManager.getDefault();


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("HERE","GOT A TEXT");
        // Get the object of SmsManager
        Parse.initialize(context, "gtpR5QrRLjQE4NMnBM3oWJ69TY3hNnbiNbQz90Xd", "NHh3oaQZHKaHhY1Fqq3sCedy6dd82JD1DRZYW6rO");


        // Retrieves a map of extended data from the intent.
            final Bundle bundle = intent.getExtras();
            if(intent.getAction().equals("com.tagalong.sms.GET_CARD") || intent.getAction().equals("com.tagalong.sms.STATUS")){
                try {
                    JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
                    String notificationText = json.getString("alert");
                    String[] splitMessage = notificationText.split(" ");
                    Log.d("ID", "JSON ID: " + splitMessage[splitMessage.length - 1]);
                    Intent bluetoothServiceIntent = new Intent(context,BluetoothBroadcastService.class);
                    bluetoothServiceIntent.putExtra("MESSAGE",splitMessage[splitMessage.length - 1]);
                    context.startService(bluetoothServiceIntent);
                } catch (JSONException e) {
                    Log.d("SMSRECIEVER", "JSONException: " + e.getMessage());
                }
            } else if(intent.getAction().equals("com.tagalong.sms.WEARER_IMAGE")){
                try {
                    JSONObject json = new JSONObject(intent.getExtras().getString("com.parse.Data"));
                    String notificationText = json.getString("alert");
                    String[] splitMessage = notificationText.split(" ");
                    Log.d("ID", "JSON ID: " + splitMessage[splitMessage.length - 1]);
                    Intent customIntent = new Intent();
                    customIntent.setAction("com.tagalong.sms.ADD_IMAGE");
                    customIntent.putExtra("timestamp",splitMessage[splitMessage.length - 1]);
                    context.sendBroadcast(customIntent);
                } catch (JSONException e) {
                    Log.d("SMSRECIEVER", "JSONException: " + e.getMessage());
                }
        }

//            try {
//
//                if (bundle != null) {
//
//                    final Object[] pdusObj = (Object[]) bundle.get("pdus");
//
//                    for (int i = 0; i < pdusObj.length; i++) {
//
//                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
//                        String phoneNumber = currentMessage.getDisplayOriginatingAddress();
//
//                        String senderNum = phoneNumber;
//                        String message = currentMessage.getDisplayMessageBody();
//
//                        if(message.contains("#tagalong")){
//                            String[] splitMessage = message.split(" ");
//                            Log.d("ID", "JSON ID: " + splitMessage[splitMessage.length - 1]);
//                            Intent bluetoothServiceIntent = new Intent(context,BluetoothBroadcastService.class);
//                            bluetoothServiceIntent.putExtra("MESSAGE",splitMessage[splitMessage.length - 1]);
//                            context.startService(bluetoothServiceIntent);
//                            break;
//                        } else {
//                            Log.d("ID","NOT RELEVANT");
//
//                        }
//
//
//                    } // end for loop
//                } // bundle is null
//
//            } catch (Exception e) {
//                Log.e("SmsReceiver", "Exception smsReceiver" +e);
//
//            }




    }


}
