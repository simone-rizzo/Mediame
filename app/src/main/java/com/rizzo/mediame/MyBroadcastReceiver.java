package com.rizzo.mediame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Classe che estende Broadcastreceiver che permette di ascoltare in broadcast le informazioni del WIFI e del WIFI DIRECT.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private Generaqr generaActivity;
    private Scannerizzaqr scannerizzaActivity;

    public MyBroadcastReceiver(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel, Generaqr activity1, Scannerizzaqr activity2) {
        this.mManager = wifiP2pManager;
        this.mChannel = channel;
        this.generaActivity = activity1;
        this.scannerizzaActivity=activity2;
    }
    public AppCompatActivity getCorrectActivity()
    {
        if(generaActivity!=null)
            return (AppCompatActivity)generaActivity;
        else
            return (AppCompatActivity)scannerizzaActivity;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                getCorrectActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getCorrectActivity(), "Wifi is ON", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                getCorrectActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getCorrectActivity(), "Wifi is OFF", Toast.LENGTH_SHORT).show();
                        if(generaActivity!=null)
                            generaActivity.setWifiOn();
                        else
                            scannerizzaActivity.setWifiOn();
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.v("Connessione", "Peer cambiati");
            WifiP2pDeviceList list = intent.getParcelableExtra(WifiP2pManager.EXTRA_P2P_DEVICE_LIST);
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                mManager.requestConnectionInfo(mChannel,(generaActivity!=null)?generaActivity.connectionInfoListener:scannerizzaActivity.connectionInfoListener);
            } else {
                getCorrectActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getCorrectActivity(), "Device disconnected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            if(generaActivity!=null) {
                WifiP2pDevice device = (WifiP2pDevice) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                final String myMac = device.deviceAddress;
                getCorrectActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        generaActivity.generaQR(myMac);
                    }
                });
            }
        }
    }
}
