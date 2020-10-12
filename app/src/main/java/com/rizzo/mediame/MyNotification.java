package com.rizzo.mediame;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

import static com.rizzo.mediame.App.CHANNEL_2_ID;
import static com.rizzo.mediame.App.CHANNEL_1_ID;

/**
 * Classe Factory che genera Pending Intent e Notification per i due canali.
 */

public class MyNotification {
    /**
     * Crea un pending intent per aprire la foto nella galleria
     */
    public static PendingIntent galleryIntent(Context context, String tmp)
    {
        Uri u = Uri.parse("content://"+tmp);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_QUICK_VIEW);
        intent.setData(u);
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        return pendingIntent;
    }

    /**
     * Crea una Notifica per segnalare che il media è stato scaricato correttamente, ed è visualizzabile con click sulla galleria
     */
    public static Notification getDownloadNotification(Context context,String path)
    {
        //PendingIntent p = galleryIntent(context,path);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_2_ID)
                .setSmallIcon(R.drawable.icon_download)
                .setContentTitle("Your photo has been downloaded")
                .setContentText("Your photo has been successfully saved in your gallery")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                //.setContentIntent(p)
                .setAutoCancel(true)
                .build();
        return notification;
    }

    /**
     * Create un pending intetn da inserire nella notifica del servizio per disattivarlo
     */
    public static PendingIntent ServiceIntet(Context context)
    {
        Intent broadcastIntent = new Intent(context,Notificationreceiver.class);
        broadcastIntent.putExtra("disconnessione","si");
        PendingIntent p = PendingIntent.getBroadcast(context,0,broadcastIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        return p;
    }


    /**
     * Crea una notifica per il Servizio
     * @return
     */
    public static Notification getServiceNotification(Context context)
    {
        PendingIntent p = ServiceIntet(context);
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_code_scanner_flash_on)
                .setContentTitle("MediaMe service")
                .setContentText("The service is running...")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(Color.RED)
                .addAction(R.drawable.ic_code_scanner_flash_on,"Disconnect",p)
                .build();
        return notification;
    }

}
