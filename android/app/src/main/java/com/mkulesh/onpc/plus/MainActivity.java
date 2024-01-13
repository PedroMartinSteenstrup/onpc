/*
 * Enhanced Music Controller
 * Copyright (C) 2019-2023 by Mikhail Kulesh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with this program.
 */

package com.mkulesh.onpc.plus;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.mkulesh.onpc.utils.Utils;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

@SuppressWarnings({"RedundantSuppression"})
public class MainActivity extends FlutterActivity
{
    private static final String METHOD_CHANNEL = "platform_method_channel";

    // dart -> platform
    private static final String GET_NETWORK_STATE = "getNetworkState";
    private static final String VOLUME_KEYS_ENABLED = "setVolumeKeysEnabled";
    private static final String VOLUME_KEYS_DISABLED = "setVolumeKeysDisabled";
    private static final String KEEP_SCREEN_ON_ENABLED = "setKeepScreenOnEnabled";
    private static final String KEEP_SCREEN_ON_DISABLED = "setKeepScreenOnDisabled";
    private static final String GET_INTENT = "getIntent";
    private static final String REGISTER_WIDGET_CALLBACK = "registerWidgetCallback";
    private static final String WIDGET_UPDATE = "widgetUpdate";

    // platform -> dart
    private static final String VOLUME_UP = "volumeUp";
    private static final String VOLUME_DOWN = "volumeDown";
    private static final String NETWORK_STATE_CHANGE = "networkStateChange";

    static class ConnectionState
    {
        final ConnectivityManager connectivity;
        private final Context context;

        ConnectionState(Context context)
        {
            this.connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            this.context = context;
        }

        @SuppressWarnings("deprecation")
        boolean isConnected()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                // getActiveNetwork Added in API level 23
                final Network net = connectivity.getActiveNetwork();
                if (net == null)
                {
                    return false;
                }
                // getNetworkCapabilities Added in API level 21
                return connectivity.getNetworkCapabilities(net) != null;
            }
            else
            {
                // getActiveNetworkInfo, Added in API level 1, Deprecated in API level 29
                final android.net.NetworkInfo netInfo = connectivity.getActiveNetworkInfo();
                return netInfo != null && netInfo.isConnected();
            }
        }

        @SuppressWarnings("deprecation")
        boolean isWifi()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                // getActiveNetwork Added in API level 23
                final Network net = connectivity.getActiveNetwork();
                if (net == null)
                {
                    return false;
                }
                // getNetworkCapabilities Added in API level 21
                final NetworkCapabilities cap = connectivity.getNetworkCapabilities(net);
                if (cap == null)
                {
                    return false;
                }
                // hasTransport Added in API level 21
                return cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
            }
            else
            {
                // If app targets Android 10 or higher, it must have the ACCESS_FINE_LOCATION permission
                // in order to use getConnectionInfo(), see
                // https://developer.android.com/about/versions/10/privacy/changes
                final WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wifi == null)
                {
                    return false;
                }
                return wifi.isWifiEnabled() &&
                       wifi.getConnectionInfo() != null &&
                       wifi.getConnectionInfo().getNetworkId() != -1;
            }
        }
    }

    static class MyBroadcastReceiver extends BroadcastReceiver
    {
        private final MainActivity listener;
        MyBroadcastReceiver(MainActivity listener)
        {
            this.listener = listener;
        }
        @Override
        public void onReceive(Context context, Intent intent)
        {
            //Log.d("onpc", "network state change via broadcast: " + intent);
            listener.onNetworkStateChanged();
        }
    }

    @SuppressLint("NewApi")
    static class MyNetworkCallback extends ConnectivityManager.NetworkCallback
    {
        private final MainActivity listener;
        MyNetworkCallback(MainActivity listener)
        {
            this.listener = listener;
        }
        @Override
        public void onAvailable(Network network)
        {
            super.onAvailable(network);
            //Log.d("onpc", "network available via network callback: " + network);
            listener.runOnUiThread(listener::onNetworkStateChanged);
        }
        @Override
        public void onLost(Network network)
        {
            //Log.d("onpc", "network lost via network callback: " + network);
            listener.runOnUiThread(listener::onNetworkStateChanged);
        }
    }

    private ConnectionState connectionState;
    private MyBroadcastReceiver broadcastReceiver;
    private MyNetworkCallback networkCallback;
    private boolean volumeKeys = true;
    private boolean keepScreenOn = false;
    private String intentData = null;
    private MethodChannel platformChannel = null;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine)
    {
        super.configureFlutterEngine(flutterEngine);

        // read preferences stored in Flutter code
        readPreferences();
        if (keepScreenOn)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        connectionState = new ConnectionState(this);

        // Message channel to Flutter code
        platformChannel = new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), METHOD_CHANNEL);
        platformChannel.setMethodCallHandler(this::onPlatformMethodCall);
        intentData = null;
    }

    @Override
    protected void onStart()
    {
        try
        {
            // avoid NullPointerException in io.flutter.embedding.android.FlutterActivity
            super.onStart();
        }
        catch (NullPointerException ex)
        {
            // nothing to do
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        try
        {
            // avoid NullPointerException in io.flutter.embedding.android.FlutterActivity
            super.onSaveInstanceState(outState);
        }
        catch (NullPointerException ex)
        {
            // nothing to do
        }
    }

    @Override
    public void onDestroy()
    {
        try
        {
            // avoid NullPointerException in io.flutter.embedding.android.FlutterActivity
            super.onDestroy();
        }
        catch (NullPointerException ex)
        {
            // nothing to do
        }
    }

    public void onNetworkStateChanged()
    {
        if (platformChannel != null)
        {
            final boolean isConnected = connectionState.isConnected();
            final boolean isWifi = connectionState.isWifi();
            //Log.d("onpc", "network state: isConnected = " + isConnected + ", isWifi = " + isWifi);
            final int state = !isConnected ? 0 : (!isWifi ? 1 : 2);
            platformChannel.invokeMethod(NETWORK_STATE_CHANGE, String.valueOf(state));
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onResume()
    {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            builder.addTransportType(NetworkCapabilities.TRANSPORT_VPN);
            networkCallback = new MyNetworkCallback(this);
            connectionState.connectivity.registerNetworkCallback(builder.build(), networkCallback);
        }
        else
        {
            broadcastReceiver = new MyBroadcastReceiver(this);
            registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        //Log.d("onpc", "onResume: intent = " + getIntent());
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent)
    {
        super.onNewIntent(intent);
        //Log.d("onpc", "onNewIntent: intent = " + intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent)
    {
        if (intent != null && intentData == null)
        {
            if (intent.getDataString() != null)
            {
                intentData = intent.getDataString();
            }
            else
            {
                intentData = intent.getAction();
            }
            //Log.d("onpc", "handleIntent: intentData = " + intentData);
            setIntent(new Intent());
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        // Log.d("onpc", "onPause: invalidate intentData");
        intentData = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && networkCallback != null)
        {
            connectionState.connectivity.unregisterNetworkCallback(networkCallback);
        }
        else if (broadcastReceiver != null)
        {
            unregisterReceiver(broadcastReceiver);
        }
    }

    private void readPreferences()
    {
        final SharedPreferences preferences = getSharedPreferences(Utils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allPrefs = preferences.getAll();
        volumeKeys = Utils.readBooleanPreference(allPrefs, "volume_keys", volumeKeys);
        keepScreenOn = Utils.readBooleanPreference(allPrefs, "keep_screen_on", keepScreenOn);
    }

    public void restartActivity()
    {
        PackageManager pm = getPackageManager();
        Intent intent = pm.getLaunchIntentForPackage(getPackageName());
        if (intent == null)
        {
            intent = getIntent();
        }
        finish();
        startActivity(intent);
    }

    @SuppressLint("ApplySharedPref")
    void onPlatformMethodCall(MethodCall methodCall, MethodChannel.Result result)
    {
        boolean newKeepScreenOn = keepScreenOn;
        if (methodCall.method.equals(VOLUME_KEYS_ENABLED))
        {
            volumeKeys = true;
            result.success("volume keys enabled");
        }
        else if (methodCall.method.equals(VOLUME_KEYS_DISABLED))
        {
            volumeKeys = false;
            result.success("volume keys disabled");
        }
        else if (methodCall.method.equals(KEEP_SCREEN_ON_ENABLED))
        {
            newKeepScreenOn = true;
            result.success("keep screen on enabled");
        }
        else if (methodCall.method.equals(KEEP_SCREEN_ON_DISABLED))
        {
            newKeepScreenOn = false;
            result.success("keep screen on disabled");
        }
        else if (methodCall.method.equals(GET_NETWORK_STATE))
        {
            final int state = !connectionState.isConnected() ? 0 : (!connectionState.isWifi() ? 1 : 2);
            result.success(String.valueOf(state));
        }
        else if (methodCall.method.equals(GET_INTENT))
        {
            //Log.d("onpc", "onPlatformMethodCall: intent = " + intentData);
            result.success(intentData != null ? intentData : "");
        }
        else if (methodCall.method.equals(REGISTER_WIDGET_CALLBACK))
        {
            if (methodCall.arguments instanceof ArrayList && ((ArrayList<?>) methodCall.arguments).size() == 8)
            {
                final SharedPreferences preferences = getSharedPreferences(Utils.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("flutter.widget_playback_power", (long) ((ArrayList<?>) methodCall.arguments).get(0));
                editor.putLong("flutter.widget_playback_previous", (long) ((ArrayList<?>) methodCall.arguments).get(1));
                editor.putLong("flutter.widget_playback_next", (long) ((ArrayList<?>) methodCall.arguments).get(2));
                editor.putLong("flutter.widget_playback_stop", (long) ((ArrayList<?>) methodCall.arguments).get(3));
                editor.putLong("flutter.widget_playback_play", (long) ((ArrayList<?>) methodCall.arguments).get(4));
                editor.putLong("flutter.widget_playback_volume_up", (long) ((ArrayList<?>) methodCall.arguments).get(5));
                editor.putLong("flutter.widget_playback_volume_down", (long) ((ArrayList<?>) methodCall.arguments).get(6));
                editor.putLong("flutter.widget_playback_volume_off", (long) ((ArrayList<?>) methodCall.arguments).get(7));
                editor.commit();
                result.success("registered " + ((ArrayList<?>) methodCall.arguments).size() + " callbacks");
            }
            else
            {
                result.success("invalid number of callbacks");
            }
        }
        else if (methodCall.method.equals(WIDGET_UPDATE))
        {
            final PendingIntent playback = WidgetPlaybackProvider.buildIntent(
                    this, WidgetPlaybackProvider.REFRESH_ACTION);
            final PendingIntent shortcuts = WidgetShortcutsProvider.buildIntent(
                    this, WidgetShortcutsProvider.REFRESH_ACTION);
            try
            {
                playback.send();
                shortcuts.send();
                result.success("widget update triggered");
            }
            catch (PendingIntent.CanceledException e)
            {
                result.success("widget update fails");
            }
        }
        else
        {
            result.success("nothing to do");
        }

        if (newKeepScreenOn != keepScreenOn)
        {
            restartActivity();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (volumeKeys)
        {
            if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    platformChannel.invokeMethod(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ?
                            VOLUME_UP : VOLUME_DOWN, "");
                    return true;
                }
                else if (event.getAction() == KeyEvent.ACTION_UP)
                {
                    // Report to the OS that event is fully processed
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }
}
