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

            // Retrieves a map of extended data from the intent.
            final Bundle bundle = intent.getExtras();

            try {

                if (bundle != null) {

                    final Object[] pdusObj = (Object[]) bundle.get("pdus");

                    for (int i = 0; i < pdusObj.length; i++) {

                        SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                        String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                        String senderNum = phoneNumber;
                        String message = currentMessage.getDisplayMessageBody();

                        if(message.contains("#tagalong")){
                            String[] splitMessage = message.split(" ");
                            Log.d("ID", "JSON ID: " + splitMessage[splitMessage.length - 1]);
                            Intent bluetoothServiceIntent = new Intent(context,BluetoothBroadcastService.class);
                            bluetoothServiceIntent.putExtra("MESSAGE",splitMessage[splitMessage.length - 1]);
                            context.startService(bluetoothServiceIntent);
                            break;
                        } else {
                            Log.d("ID","NOT RELEVANT");

                        }


                    } // end for loop
                } // bundle is null

            } catch (Exception e) {
                Log.e("SmsReceiver", "Exception smsReceiver" +e);

            }




    }


}
