package com.tagalong.sms;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.parse.ParsePush;


public class WearerActivity extends Activity {
    private WebView webInterface;
    private String myNumber;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String timestamp = intent.getStringExtra("timestamp");
            final String senderPhone = intent.getStringExtra("sender"); //change
            Log.d("HERE",timestamp);
            webInterface.post(new Runnable() {
                @Override
                public void run() {
                    webInterface.loadUrl("javascript:sendMessage("+timestamp+",\'"+senderPhone+"\');");
                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_wearer);
        webInterface = (WebView) findViewById(R.id.webInterface);
        WebSettings webSettings = webInterface.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webInterface.setWebChromeClient(new WebChromeClient());
        WebSettings settings = webInterface.getSettings();
        settings.setDomStorageEnabled(true);

        final Intent intent = getIntent();
        final String action = intent.getAction();
        ParsePush.subscribeInBackground("companion");


        if (Intent.ACTION_VIEW.equals(action)) {
            String url = intent.getDataString();
            webInterface.loadUrl(url);
            String[] params = url.split("_");
            myNumber = params[params.length-1];
            Log.d("HERE",url);
            Log.d("HERE",myNumber);
            Intent customIntent = new Intent();
            customIntent.setAction("com.tagalong.sms.ADD_IMAGE");

            String[] params2 = params[1].split("=");
            customIntent.putExtra("timestamp",params2[params2.length-1]);
            Log.d("HERE",params2[params2.length-1]);
            this.sendBroadcast(customIntent);

        } else {
            webInterface.loadUrl("http://tagalong.ddns.net/wearer");
            myNumber = getIntent().getStringExtra("phone");
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wearer, menu);
        return true;
    }

    @Override
    public void onPause(){
        super.onPause();
        this.unregisterReceiver(mReceiver);
        ParsePush.unsubscribeInBackground("companion");

    }

    @Override
    public void onResume(){
        super.onResume();
        ParsePush.subscribeInBackground("companion");
        IntentFilter filter = new IntentFilter("com.tagalong.sms.ADD_IMAGE");
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
