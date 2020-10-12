package com.rizzo.mediame;

import android.app.Activity;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.rizzo.mediame.App.CHANNEL_1_ID;

/**
 * Classe che serve per aprire la connessione accettando la richiesta dal client.
 */
public class ServerClass extends Thread {

    Socket socket;
    ServerSocket serverSocket;
    private Handler handler;
    private AtomicBoolean connesso;
    private String chache;
    private RecyclerViewAdapter stron;
    private ContentResolver contentResolver;
    private NotificationManagerCompat notificationManager;
    private boolean client=false;
    private Context context;

    public ServerClass(boolean isclient,Handler hand, AtomicBoolean b, String cha, RecyclerViewAdapter str, ContentResolver cont,NotificationManagerCompat not,Context contx)
    {
        handler=hand;
        connesso=b;
        contentResolver=cont;
        stron=str;
        chache=cha;
        notificationManager=not;
        client=isclient;
        context=contx;
    }


    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8888);
            socket = serverSocket.accept();
            connesso.set(true);
            notificationManager.notify(0,MyNotification.getServiceNotification(context));
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, "Service ready", Toast.LENGTH_LONG).show();
                }
            });
            SendReceive sendReceive = new SendReceive(socket,15,connesso,chache,stron,contentResolver,notificationManager,handler);
            sendReceive.start();
            if(client)
            sendReceive.write("send_media","send_media".getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
