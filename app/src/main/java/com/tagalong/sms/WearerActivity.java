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
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;


public class WearerActivity extends Activity {
    private WebView webInterface;
    private String myNumber;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String timestamp = intent.getStringExtra("timestamp");
            Log.d("HERE",timestamp);
            webInterface.post(new Runnable() {
                @Override
                public void run() {
                    webInterface.loadUrl("javascript:sendMessage("+timestamp+",\'"+myNumber+"\');");
                }
            });
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wearer);
        webInterface = (WebView) findViewById(R.id.webInterface);
        WebSettings webSettings = webInterface.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webInterface.loadUrl("http://tagalong.ddns.net/wearer");
        myNumber = getIntent().getStringExtra("phone");
        webInterface.setWebChromeClient(new WebChromeClient());
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
    }

    @Override
    public void onResume(){
        super.onResume();
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
