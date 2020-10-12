package com.rizzo.mediame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.zxing.Result;
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

/**
 * Activity che permette di scannerizzare il codice qr necessario per effettuare il pair di connessione WIFI DIRECT.
 */

public class Scannerizzaqr extends AppCompatActivity implements DecodeCallback, MultiplePermissionsListener {

    CodeScanner codeScanner;
    CodeScannerView scannView;
    TextView resultData;
    //--------------------------------
    WifiP2pManager mManager;
    WifiManager wifiManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    //--------------------------------
    AtomicBoolean connesso;
    //-------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scannerizzaqr);

        scannView = findViewById(R.id.scannerView);
        codeScanner = new CodeScanner(this,scannView);
        resultData = (TextView) findViewById(R.id.resultQR);
        codeScanner.setDecodeCallback(this);
        inizializza();
    }
    public void inizializza()
    {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager = (WifiP2pManager)getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this,getMainLooper(),null);
        mReceiver = new MyBroadcastReceiver(mManager,mChannel,null,this);
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
        connesso=new AtomicBoolean();
        connesso.set(false);
    }

    /**
     * Metodo che viene chiamato quando si legge un codice QR con successo
     * @param result
     */
    @Override
    public void onDecoded(@NonNull final Result result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultData.setText(result.getText());
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress=result.getText();
                config.groupOwnerIntent=15; //Con 15 è il group owner con 0 è il client.
                mManager.connect(mChannel, config,new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to "+result.getText(),Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Not connected pls retry later"+reason,Toast.LENGTH_SHORT).show();
                        codeScanner.startPreview();
                    }
                });
            }
        });
    }

    /**
     * Nel resume dell'app chiedo subito i permessi della fotocamera
     */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        requestPermission();
    }

    /**
     * Richiesta dell'accesso alla camera
     */
    public void requestPermission()
    {
        Dexter.withActivity(this).withPermissions(Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE).withListener(this).check();

    }
    @Override
    public void onPermissionsChecked(MultiplePermissionsReport report) {
        codeScanner.startPreview();
    }

    @Override
    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
        Toast.makeText(this,"Impossibile utilizzare la camera permessi non disponibili",Toast.LENGTH_SHORT).show();
        token.continuePermissionRequest();
    }

    /**
     * Ritorna il nostro connection info listner
     * @return
     */
    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListner() {
        return null;
    }
    /**
     * Setta il nostro wifi ad on
     */

    public void setWifiOn() {
        wifiManager.setWifiEnabled(true);
    }
    /**
     * Registra il receiver
     */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
    }


    /**
     * In pausa scollego il broadcast receiver
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    /**
     * Listner che ci da informazioni sulla connessione stabiita tra i due dispositivi
     */
    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            //indirizzo di chi tiene il server
            final InetAddress groudpOwnerAddress = info.groupOwnerAddress;
            if (!connesso.get()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Device connected", Toast.LENGTH_SHORT).show();
                    }
                });
                if (info.groupFormed && info.isGroupOwner) {
                    //Ci siamo connessi come HOST
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(getApplicationContext(),LoadingMedia.class);
                            startActivity(i);
                        }
                    });
                } else if (info.groupFormed) {
                    //Siamo Connessi come CLIENT
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent i = new Intent(getApplicationContext(),LoadingMedia.class);
                            i.putExtra("ip",groudpOwnerAddress);
                            startActivity(i);
                        }
                    });
                }
            }
        }
    };
}
