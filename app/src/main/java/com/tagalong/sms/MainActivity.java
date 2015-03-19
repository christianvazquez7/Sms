package com.tagalong.sms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.parse.Parse;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.PushService;
import com.parse.SaveCallback;


import org.w3c.dom.Text;

import java.text.ParseException;
import java.util.List;


public class MainActivity extends Activity {
    private Button registerButton;
    private TextView companionPhone;
    private EditText message;
    private boolean registered = false;
    private boolean disconnected = false;
    private boolean pairing = false;
    private TextView status;
    private ProgressBar progressBar;
    private TelephonyManager telephoneManager;
    private String connectedTo="";

    private static final String address = "http://tagalong.ddns.net/";
    private static final String registerAction = "requestPair/";
    private static final String checkStatus = "requestStatus/";
    private static final String unregister = "unregister/";
    private String myNumber;
    private View messageButton;

    private Handler statusHandler;

    private static final int PICK_CONTACT = 1;

    private Runnable pollThread = new Runnable() {
        @Override
        public void run() {
              if(pairing) {
                  fetchStatus();
              }
        }
    };

    @Override
    public void onPause(){
        statusHandler.removeCallbacks(pollThread);
        super.onPause();
    }
    @Override
    public void onResume(){
        super.onResume();
        fetchStatus();
        Intent bluetoothServiceIntent = new Intent(this,BluetoothBroadcastService.class);
        bluetoothServiceIntent.putExtra("MESSAGE","+"+myNumber);
        bluetoothServiceIntent.putExtra("register",true);
        this.startService(bluetoothServiceIntent);
    }

    public void fetchStatus(){
        Ion.with(MainActivity.this).load(address + checkStatus + myNumber)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (e != null) {
                            e.printStackTrace();
                            status.setTextColor(getResources().getColor(R.color.red));
                            status.setText("Check Internet Connection");
                            disconnected = true;
                        } else {
                            if (result.equals("")) {
                                disconnected = true;
                                registered = false;
                                pairing = false;
                                status.setTextColor(getResources().getColor(R.color.red));
                                status.setText("Disconnected");
                                progressBar.setVisibility(View.GONE);

                                List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
                                if(subscribedChannels != null) {
                                    Log.d("UNSUB","unsubscribing");

                                    for (String channel : subscribedChannels) {
                                        ParsePush.unsubscribeInBackground(channel);
                                    }
                                }
                                registerButton.setText("Register");
                                registerButton.setBackgroundColor(getResources().getColor(R.color.green));
                                connectedTo = "";


                            }
                            else if (result.equals("pairing")){
                                registered = false;
                                disconnected = false;
                                pairing = true;
                                status.setTextColor(getResources().getColor(R.color.green));
                                status.setText("Awaiting companion confirmation");
                                progressBar.setVisibility(View.VISIBLE);
                                statusHandler.postDelayed(pollThread,2000);
                                registerButton.setText("Register");
                                registerButton.setBackgroundColor(getResources().getColor(R.color.green));

                            } else {
                                registered = true;
                                Log.d("CHANNEL","Subscribing to "+result);
                                connectedTo = "wearer_"+result;
                                ParsePush.subscribeInBackground("wearer_"+myNumber);
                                disconnected = false;
                                pairing = false;
                                progressBar.setVisibility(View.GONE);
                                status.setTextColor(getResources().getColor(R.color.green));
                                status.setText("Connected to: "+result);
                                companionPhone.setText(result);
                                registerButton.setText("Unregister");
                                registerButton.setBackgroundColor(getResources().getColor(R.color.red));
                            }
                        }
                    }
                });
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Parse.initialize(this, "gtpR5QrRLjQE4NMnBM3oWJ69TY3hNnbiNbQz90Xd", "NHh3oaQZHKaHhY1Fqq3sCedy6dd82JD1DRZYW6rO");


        statusHandler = new Handler();

        //fetching phone number

        telephoneManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        myNumber = telephoneManager.getLine1Number();
        if(myNumber.equals("+15126389698"))
            myNumber = "+17876131066";
        Log.d("MY NUMBBER",myNumber);
        myNumber = myNumber.substring(1);

//        Intent bluetoothServiceIntent = new Intent(this,BluetoothBroadcastService.class);
//        bluetoothServiceIntent.putExtra("MESSAGE","+"+myNumber);
//        bluetoothServiceIntent.putExtra("register",true);
//        this.startService(bluetoothServiceIntent);


        //fetching components of ui

        registerButton = (Button) this.findViewById(R.id.register);
        companionPhone = (TextView) this.findViewById(R.id.phone);
        message = (EditText) this.findViewById(R.id.message);
        status = (TextView) this.findViewById(R.id.status);
        progressBar = (ProgressBar) this.findViewById(R.id.progressBar);
        messageButton = this.findViewById(R.id.message_companion);


        status.setText("");
        fetchStatus();





        //listener for register
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!registered) {
                    ParsePush.subscribeInBackground("wearer_"+myNumber);

//                    Intent bluetoothServiceIntent = new Intent(MainActivity.this,BluetoothBroadcastService.class);
//                    bluetoothServiceIntent.putExtra("MESSAGE","+"+myNumber);
//                    MainActivity.this.startService(bluetoothServiceIntent);

                    if (!companionPhone.getText().equals("")) {
                        progressBar.setVisibility(View.VISIBLE);
                        status.setTextColor(getResources().getColor(R.color.green));
                        status.setText("Sending request");
                        Log.d("ACTION", address + registerAction + myNumber + "/" + companionPhone.getText());

                        Ion.with(MainActivity.this).load(address + registerAction + myNumber + "/" + companionPhone.getText())
                                .setBodyParameter("message", message.getText().toString())
                                .asString()
                                .setCallback(new FutureCallback<String>() {
                                    @Override
                                    public void onCompleted(Exception e, String result) {
                                        if (e != null) {
                                            e.printStackTrace();
                                            progressBar.setVisibility(View.INVISIBLE);
                                            status.setTextColor(getResources().getColor(R.color.red));
                                            status.setText("An error occured processing your request");
                                        } else {
                                            pairing = true;
                                            disconnected = false;
                                            registered = false;
                                            status.setTextColor(getResources().getColor(R.color.green));
                                            status.setText("Awaiting companion confirmation");
                                            statusHandler.postDelayed(pollThread, 2000);
                                        }
                                    }
                                });
                    } else {
                        status.setTextColor(getResources().getColor(R.color.red));
                        status.setText("Please add a phone number for the companion");
                    }

                }

                else {
                    Ion.with(MainActivity.this).load(address + unregister + myNumber)
                            .asString()
                            .setCallback(new FutureCallback<String>() {
                                @Override
                                public void onCompleted(Exception e, String result) {
                                    if (e != null) {
                                        e.printStackTrace();
                                        status.setTextColor(getResources().getColor(R.color.red));
                                        status.setText("Check Internet Connection");
                                        disconnected = true;
                                    } else {
                                      disconnected = true;
                                      pairing = false;
                                      registered = false;
                                        registerButton.setText("Register");
                                        registerButton.setBackgroundColor(getResources().getColor(R.color.green));
                                        status.setTextColor(getResources().getColor(R.color.red));
                                        status.setText("Disconnected");
                                        List<String> subscribedChannels = ParseInstallation.getCurrentInstallation().getList("channels");
                                        Log.d("UNSUB","unsubscribing");
                                        for(String channel : subscribedChannels) {
                                            ParsePush.unsubscribeInBackground(channel);
                                        }
                                        connectedTo = "";


                                    }
                                }
                            });
                }

            }
        });

        //setting pick contact

        companionPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT);
            }
        });

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent toMessage = new Intent();
                toMessage.setClass(MainActivity.this,WearerActivity.class);
                startActivity(toMessage);
            }
        });
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT) :
                if (resultCode == Activity.RESULT_OK) {

                    Uri contactData = data.getData();
                    Cursor c =  managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {


                        String id =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                        String hasPhone =c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id,
                                    null, null);
                            phones.moveToFirst();
                            String cNumber = phones.getString(phones.getColumnIndex("data1"));
                            cNumber = android.telephony.PhoneNumberUtils.stripSeparators(cNumber);
                            if(cNumber.length() != 11)
                                cNumber = "1" + cNumber;
                            companionPhone.setText(cNumber);
                        }
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));


                    }
                }
                break;
        }

    }

}
