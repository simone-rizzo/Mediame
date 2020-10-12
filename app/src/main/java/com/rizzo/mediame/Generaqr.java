package com.rizzo.mediame;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Generaqr extends AppCompatActivity implements MultiplePermissionsListener {

    private ImageView qrImage;
    //-------------------------------
    WifiP2pManager mManager;
    WifiManager wifiManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    //-------------------------------
    AtomicBoolean connesso;
    //-------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generaqr);
        qrImage = (ImageView) findViewById(R.id.imageqr);
        inizializza();
    }

    /**
     * Richiede i permessi di Lettura e scrittura in memoria esterna.
     */
    public void inizializza() {
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(this).check();
    }

    /**
     * Registra il receiver
     */
    @Override
    protected void onResume() {
        super.onResume();
    }


    /**
     * In pausa scollego il broadcast receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }

    /**
     * Metodo che data una stringa genera un Bitmap contenete il codice qr
     *
     * @param testo
     */
    public void generaQR(String testo) {
        QRGEncoder qrgEncoder = new QRGEncoder(testo
                , null, QRGContents.Type.TEXT, 1000);
        qrgEncoder.setColorBlack(Color.BLACK);
        qrgEncoder.setColorWhite(Color.WHITE);
        // Getting QR-Code as Bitmap
        Bitmap bitmap = qrgEncoder.getBitmap();
        // Setting Bitmap to ImageView
        qrImage.setImageBitmap(bitmap);
    }

    /**
     * Setta il wifi ad ON
     */
    public void setWifiOn() {
        wifiManager.setWifiEnabled(true);
    }

    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListner() {
        return connectionInfoListener;
    }

    /**
     * Listner che ci da informazioni sulla connessione stabiita tra i due dispositivi, se la connessione è stabilita
     * lancia la nuova activity.
     */
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            //indirizzo di chi tiene il server
            final InetAddress groudpOwnerAddress = info.groupOwnerAddress;
            if (!connesso.get()) {
                //Scollego il discovery dei peers
                mManager.stopPeerDiscovery(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(int reason) {

                    }
                });

                if (info.groupFormed && info.isGroupOwner) {
                    //Ci siamo connessi come HOST
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(getApplicationContext(),SharingMedia.class);
                            startActivity(i);
                        }
                    });
                } else if (info.groupFormed) {
                    //Siamo Connessi come CLIENT
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(getApplicationContext(),SharingMedia.class);
                            i.putExtra("ip",groudpOwnerAddress);
                            startActivity(i);
                        }
                    });
                }
            }
        }
    };

    /**
     * Se tutti i permessi sono stati inseriti allora instanzio il tutto.
     * @param report
     */
    @Override
    public void onPermissionsChecked(MultiplePermissionsReport report) {
        if(report.areAllPermissionsGranted()) {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
            mChannel = mManager.initialize(this, getMainLooper(), null);
            mReceiver = new MyBroadcastReceiver(mManager, mChannel, this, null);
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
            mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
            mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onFailure(int reason) {
                }
            });
            connesso = new AtomicBoolean();
            connesso.set(false);
        }
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {

    }
}
