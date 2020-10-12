package com.rizzo.mediame;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.recyclerview.widget.RecyclerView.*;

/**
 * Activity che ci permette di leggere all'interno di una recycler view i media condivisi dal dispositivo a cui siamo connessi.
 */

public class LoadingMedia extends AppCompatActivity implements MediaClickListner {

    private AtomicBoolean connessione = new AtomicBoolean(true);
    private RecyclerView recyView;
    LayoutManager layoutManager;
    RecyclerViewAdapter recycleviewAdapter;
    static final int MESSAGE_READ=1;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_media);
        recyView = (RecyclerView)findViewById(R.id.recycleView);
        layoutManager = new GridLayoutManager(this,3);
        recyView.setLayoutManager(layoutManager);
        notificationManager = NotificationManagerCompat.from(this);
        recycleviewAdapter = new RecyclerViewAdapter(this,this);
        recyView.setAdapter(recycleviewAdapter);
        recyView.setHasFixedSize(true);
        recyView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (!recyclerView.canScrollVertically(1)) {
                    SendReceive.getInstance().write("send_media","send_media".getBytes());
                }
            }
        });
        Bundle b = getIntent().getExtras();
        if(b!=null && b.containsKey("ip")) {
            InetAddress address = (InetAddress) b.get("ip");
            ClientClass client = new ClientClass(true,address,handler,connessione,getCacheDir().toString(),recycleviewAdapter,getContentResolver(),notificationManager,this);
            client.start();
        }
        else {
            ServerClass server = new ServerClass(true, handler, connessione, getCacheDir().toString(), recycleviewAdapter, getContentResolver(), notificationManager,this);
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

    @Override
    public void onMediaClick(String path,String id) {
        Intent i = new Intent(this,ShowMedia.class);
        i.putExtra("media",path);
        i.putExtra("id",id);
        startActivity(i);
    }
}
