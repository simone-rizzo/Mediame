package com.rizzo.mediame;

import android.app.Activity;
import android.app.Notification;
import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import android.os.Handler;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import static com.rizzo.mediame.App.CHANNEL_1_ID;

/**
 * Classe per avviare una connessione con il server per avviare il servizio di share dei media.
 */

public class ClientClass extends Thread{

    Socket socket;
    String hostAdd;
    private Handler handler;
    private AtomicBoolean connesso;
    private String chache;
    private RecyclerViewAdapter stron;
    private ContentResolver contentResolver;
    private NotificationManagerCompat notificationManager;
    private boolean client=false;
    private int tentativi=20;
    private Context context;


    public ClientClass(boolean isclient,InetAddress hostAddress, Handler handl, AtomicBoolean b, String cha, RecyclerViewAdapter str, ContentResolver cont,NotificationManagerCompat nom,Context contx )
    {
        handler=handl;
        connesso=b;
        socket=new Socket();
        hostAdd=hostAddress.getHostAddress();
        contentResolver=cont;
        stron=str;
        chache=cha;
        notificationManager=nom;
        client=isclient;
        context=contx;
    }

    @Override
    public void run() {
        while (tentativi>0)
        {
            try {
                tryConnect();
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * Metodo per effettuare una prova di connessione, in caso di eccezione
     * si aspetta 1s e si decrementa il numero di tentativi.
     * @throws InterruptedException
     */
    public void tryConnect() throws InterruptedException {
        try {
            connect();
        }
        catch (SocketTimeoutException e)
        {
            e.printStackTrace();
            Thread.sleep(1000);
            tentativi--;
        }catch (IOException e) {
            e.printStackTrace();
            Thread.sleep(1000);
            tentativi--;
        }
    }

    /**
     * Metodo che effettua la connessione al server e avvia il thread principale di lettura e scrittura.
     * @throws IOException
     */
    public void connect() throws  IOException {
        Socket socket=new Socket();
        socket.connect(new InetSocketAddress(hostAdd,8888),10000);
        connesso.set(true);
        tentativi=0;
        notificationManager.notify(0,MyNotification.getServiceNotification(context));
        Log.v("Connessione","Connesso al socket da client");
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,"Service ready",Toast.LENGTH_LONG).show();
            }
        });
        SendReceive sendReceive=new SendReceive(socket,0,connesso,chache,stron,contentResolver,notificationManager,handler);
        sendReceive.start();
        if(client)
            sendReceive.write("send_media","send_media".getBytes());
    }
}
