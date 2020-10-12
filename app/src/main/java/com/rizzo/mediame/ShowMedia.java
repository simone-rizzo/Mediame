package com.rizzo.mediame;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.concurrent.CountDownLatch;
import static com.rizzo.mediame.App.CHANNEL_2_ID;

/**
 * Activity per visualizzare una singola foto della galleria.
 */

public class ShowMedia extends AppCompatActivity {

    private ImageView img;
    private String id;
    private StringBuilder loadedPath=new StringBuilder();
    private NotificationManagerCompat notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().show();
        setContentView(R.layout.activity_show_media);
        img=(ImageView) findViewById(R.id.imgshowmedia);
        Bundle b = getIntent().getExtras();
        notificationManager = NotificationManagerCompat.from(this);
        if(b!=null && b.containsKey("media")) {
            String path = "file://"+(String) b.get("media");
            id = (String) b.get("id");
            img.setImageURI(Uri.parse(path));
            scaricaFotoHD();
        }

    }

    /**
     * Setta l'option menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mediamenu, menu);
        return true;
    }

    /**
     * Se viene premuto donwload, viene spawnato un thread che chiede di ricevere la foto in qualità alta e si mette in attesa.
     * Una volta ricevuta la foto salva quest'ultima all'interno della nostra galleria.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.download:
                Thread scarica = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String tmp = "";
                            synchronized (loadedPath)
                            {
                                while (loadedPath.length()==0)
                                {
                                    loadedPath.wait();
                                }
                                tmp=loadedPath.toString();
                                loadedPath.setLength(0);
                            }
                            String urlStored = MediaStore.Images.Media.insertImage(getContentResolver(), tmp, "media_photo", "downloaded from MediaMe app");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Image donwloaded", Toast.LENGTH_SHORT).show();
                                }
                            });
                            notificationManager.notify(1, MyNotification.getDownloadNotification(getApplicationContext(),tmp));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                });
                scarica.start();
                return true;
        }
        return true;
    }

    /**
     * Metodo che va a creare un thread con valore di ritorno che contatta il server per richiedere una foto hd
     */
    public void scaricaFotoHD()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String path = SendReceive.getInstance().writeWithReturn("id:"+id+",","getfoto".getBytes());
                synchronized (loadedPath) {
                    loadedPath.append(path);
                    loadedPath.notify();
                }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            img.setImageURI(Uri.parse("file://" + path));
                        }
                    });
            }
        }).start();
    }



}
