package com.rizzo.mediame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Broadcast receiver custom per gestire l'evento di disconnessione effettuato attraverso le notifiche, tramite pending intent.
 */

public class Notificationreceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("disconnessione");
        if(message.equals("si"))
        {
            SendReceive.getInstance().disconnetti();
        }
    }
}
