package com.rizzo.mediame;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {

    public static final String CHANNEL_1_ID = "channel1";
    public static final String CHANNEL_2_ID = "channel2";
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    /**
     * Metodo che inizializza i due channel: chennel1 utilizzato per le notifiche del servizio in backround
     * channel2 per le notifiche relative al download dei media.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel1.setDescription("This is Service Channel");
            NotificationChannel channel2 = new NotificationChannel(
                    CHANNEL_2_ID,
                    "Download",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel2.setDescription("This is Download Channel");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
            manager.createNotificationChannel(channel2);
        }
    }
}
