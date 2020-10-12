package com.rizzo.mediame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity che permette la condivisione dei propi medias.
 */

public class SharingMedia extends AppCompatActivity implements View.OnClickListener {

    private AtomicBoolean connessione = new AtomicBoolean(true);
    private ImageView immagine;
    private NotificationManagerCompat notificationManager;
    private LinearLayout btndisconnect;
    private TextView statusTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing_media);
        immagine=(ImageView)findViewById(R.id.imgshare);
        btndisconnect=(LinearLayout)findViewById(R.id.btndisconnect);
        btndisconnect.setOnClickListener(this);
        notificationManager = NotificationManagerCompat.from(this);
        statusTextView=(TextView) findViewById(R.id.statustext);
        Bundle b = getIntent().getExtras();
        if(b!=null && b.containsKey("ip")) {
            InetAddress address = (InetAddress) b.get("ip");
            ClientClass client = new ClientClass(false,address,handler,connessione,getCacheDir().toString(),null,getContentResolver(),notificationManager,this);
            client.start();
        }
        else {
            ServerClass server = new ServerClass(false, handler, connessione, getCacheDir().toString(), null, getContentResolver(), notificationManager,this);
            server.start();
        }
    }
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            if(msg.what==SendReceive.DISCONNECT)
            {
                Toast.makeText(getApplicationContext(),"Disconnected",Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        }
    } ;

    /**
     * Disconnessione del service.
     * @param v
     */
    @Override
    public void onClick(View v) {
        if(v==btndisconnect)
        {
            SendReceive.getInstance().disconnetti();
        }
    }
}
