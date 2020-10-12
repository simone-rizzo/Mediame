package com.rizzo.mediame;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.Handler;

import androidx.core.app.NotificationManagerCompat;

/**
 * Thread che realizza effettivamente il servizio in background di invio e ricezione dei messaggi sul socket TCP.
 * Questo thread viene instanziato ugualmente su client e server, per predisporre l'applicazione in futuro per implementare
 * la condivisione delle gallerie in tempo reale su tutti e due i device.
 */

public class SendReceive extends Thread {

    public Socket socket;
    private DataInputStream inputStream;
    public DataOutputStream outputStream;
    private int type;
    private AtomicBoolean connesso;
    ArrayList<String> immaginiPath=null;
    String chacheDir;
    RecyclerViewAdapter strz;
    ContentResolver contentResolver;
    private static SendReceive Instance;
    public static int HEADER_SIZE=512;
    private int fotoLette=0;
    //---------------------
    private StringBuilder fotoDownloaded=new StringBuilder();
    private NotificationManagerCompat notificationManager;
    private Handler handler;
    //---------------------
    public static int DISCONNECT=-1;

    public SendReceive(Socket skt, int ty, AtomicBoolean b, String chache, RecyclerViewAdapter strnz, ContentResolver cont, NotificationManagerCompat nom, Handler hand) {
        socket=skt;
        type=ty;
        connesso=b;
        chacheDir=chache;
        strz=strnz;
        contentResolver=cont;
        notificationManager=nom;
        handler=hand;
        try {
            inputStream=new DataInputStream(socket.getInputStream());
            outputStream=new DataOutputStream(socket.getOutputStream());
            Instance=this;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Singleton pattern
     * @return
     */
    public static SendReceive getInstance()
    {
        if(Instance!=null)
        {
            return Instance;
        }
        else
        {
            return null;
        }
    }

    /**
     * Cliclo lettura decodifica del messaggio risposta tipica del server.
     * HEADERS dei messaggi:
     * "id:numero," indica la rischiesta dal client di voler vedere l'immagine con quello specifico id
     * "path:nomefile," indica la ricezione di un immagine ad alta qualità da scaricare
     * "send_media" indica il voler ricevere altre foto dal content provider
     * "size1:id1,size2:id2.....sizen.idn" indica la ricezione di n foto con ognuna possiede size[n] e id[n].
     */

    @Override
    public void run() {
        //ExecutorService executor = Executors.newCachedThreadPool();
        while (socket!=null && !socket.isClosed())
        {
            try {
                Log.v("Connessione",socket.toString());
                if (!socket.isConnected() && !connesso.get()) {
                    disconnetti();
                }
                else {
                    int lenght = inputStream.readInt();
                    if(lenght>0) {
                        //Richiesta di decodifica delle immagini ricevute
                        byte[] hd = new byte[HEADER_SIZE];
                        inputStream.readFully(hd, 0, HEADER_SIZE);
                        String header=new String(hd,0,hd.length);

                        if(header.contains(",") && !header.contains("id") && !header.contains("path")) {
                            //Riceve le foto e le decodifica
                            String[] split = header.split(",");
                            int n_foto=split.length-1;
                            final int[] sizes = new int[n_foto];
                            int len = 0;
                            for (int i = 0; i < n_foto; i++) {
                                sizes[i] = Integer.parseInt(split[i].split(":")[0]);
                                len += sizes[i];
                            }
                            final byte[] message = new byte[len];
                            inputStream.readFully(message, 0, len);
                            immaginiPath = new ArrayList<>();
                            final ArrayList<String> ids = new ArrayList<>();
                            int letti = 0;
                            for (int i = 0; i < n_foto; i++) {
                                try {
                                    ids.add(split[i].split(":")[1]);
                                    File downloadingMediaFile = new File(chacheDir, split[i].split(":")[1] + ".jpg");
                                    FileOutputStream out = new FileOutputStream(downloadingMediaFile);
                                    out.write(message, letti, sizes[i]);
                                    try {
                                        out.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    String path =/*"file://"+*/chacheDir + "/" + split[i].split(":")[1] + ".jpg";
                                    immaginiPath.add(path);
                                    letti += sizes[i];
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            Log.v("Connessione","ho caricato:"+n_foto+" foto");

                            //Lettura delle foto completata invio path al thread UI
                            if(immaginiPath!=null && immaginiPath.size()>0 && strz!=null) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        strz.addFoto(immaginiPath,ids);
                                    }
                                });

                            }
                        }
                        //Richiesta di generare le foto ed inviarle
                        else if(header.contains("send_media"))
                        {
                            int n_foto=12;
                            MediaIdsPaths queryresult = VediFotoMedia(fotoLette,n_foto);
                            fotoLette+=queryresult.paths.length;
                            byte[] total;
                            ArrayList<Integer> sizes=new ArrayList<Integer>();
                            int tot=0;
                            String new_header="";
                            ArrayList<byte[]> elem = new ArrayList<byte[]>();
                            for(int i=0;i<queryresult.paths.length;i++){
                                try {
                                    byte[] bytesImg = BitmapUtils.getBytesImageCompressedFromPath(queryresult.paths[i],80,40,40);
                                    tot+=bytesImg.length;
                                    sizes.add(bytesImg.length);
                                    elem.add(bytesImg);
                                    new_header+=bytesImg.length+":"+queryresult.getIds()[i]+","; //aggiungo gli id delle foto con la dim
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            total=new byte[tot];
                            int scritti=0;
                            for (int i=0;i<sizes.size();i++)
                            {
                                try {
                                    System.arraycopy(elem.get(i), 0, total, scritti, sizes.get(i));
                                    scritti += sizes.get(i);
                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                }
                            }
                            write(new_header,total);
                        }
                        else if(header.contains("id"))
                        {
                            String id = header.split(",")[0].split(":")[1];
                            String path = GetMediaFromID(id);
                            File f = new File(path);
                            byte[] message = new byte[(int)f.length()];
                            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(f));
                            buf.read(message, 0, message.length);
                            buf.close();
                            write("path:hd_"+id+",",message);
                        }
                        else if(header.contains("path")) {
                            byte[] message = new byte[lenght-HEADER_SIZE];
                            String name = header.split(",")[0].split(":")[1];
                            inputStream.readFully(message, 0, lenght-HEADER_SIZE);
                            File downloadingMediaFile = new File(chacheDir, name + ".jpg");
                            FileOutputStream out = new FileOutputStream(downloadingMediaFile);
                            out.write(message, 0, message.length);
                            out.close();
                            synchronized (fotoDownloaded) {
                                fotoDownloaded.append(downloadingMediaFile.getAbsolutePath());
                                fotoDownloaded.notify();
                            }
                            Log.v("Connessione","Foto arrivata:"+fotoDownloaded);
                        }
                    }
                }
            } catch (IOException e) {

                disconnetti();
                e.printStackTrace();
            }
        }
    }

    /**
     * Metodo per chiudere il servizio: Disconnettione
     */
    public void disconnetti()
    {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
            connesso.set(false);
            notificationManager.cancel(0);
            Message msg = handler.obtainMessage();
            msg.what = DISCONNECT;
            handler.sendMessage(msg);
            /*handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myActivity,"Disconnected",Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(myActivity,MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    myActivity.startActivity(intent);
                }
            });*/
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }



    /**
     * Spawna un thread che scrive i bytes
     * @param header stringa che rappresenta quello che è contenuto nei bytes
     * @param bytes array di bites contenenti le foto
     */
    public void write(String header,byte[] bytes)
    {
        Send invia=new Send(outputStream);
        invia.setBytes(header,bytes);
        invia.start();
    }

    /**
     * Lancio un thread asyncrono che scrive una richiesta al server e restituisce il path in cui ha salvato il media
     * @param header
     * @param bytes
     * @return
     */
    public String writeWithReturn(String header,byte[] bytes){
        ExecutorService pool = Executors.newSingleThreadExecutor();
        MediaRequest req = new MediaRequest(bytes,outputStream,header);
        Future<String> res = pool.submit(req);
        try {
            String path = res.get(20000L, TimeUnit.MILLISECONDS);
            return path;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } catch (TimeoutException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Contatta il content provider chiedendo le immagini
     * @return array di stringhe contententi i path delle foto
     */
    public MediaIdsPaths VediFotoMedia(int letti,int n) {
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        /**
         * Modo per ottenere n foto alla volta.
         */
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + "  DESC \n LIMIT " + letti + "," + n;

        //Stores all the images from the gallery in Cursor
        Cursor cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                null,
                null,
                orderBy);
        String[] paths = new String[cursor.getCount()];
        String[] ids = new String[cursor.getCount()];
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            int idcolumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            String path = cursor.getString(dataColumnIndex);
            paths[i] = path;
            ids[i] = cursor.getString(idcolumn);
        }
        return new MediaIdsPaths(ids, paths);
    }

    /**
     * Dato un id di un media ritorna il path al suo percorso contattando il content provider
     * @param id id del media
     * @return path del percorso del media
     */
    public String GetMediaFromID(String id) {
        Uri path = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Long.parseLong(id));
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
        Cursor cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                columns,
                MediaStore.Images.Media._ID + " = " + id,
                null,
                null);
        cursor.moveToPosition(0);
        int idcolumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
        String res = cursor.getString(idcolumn);
        return res.toString();

    }

    /**
     * Classe contenente i path e gli id delle foto contenute dalla query fatta sul content provider.
     */
    public class MediaIdsPaths
    {
        private String[] ids,paths;

        public MediaIdsPaths(String[] ids, String[] paths) {
            this.ids = ids;
            this.paths = paths;
        }

        public String[] getIds() {
            return ids;
        }

        public String[] getPaths() {
            return paths;
        }
    }

    /**
     * Thread che scrive sul socket e attende la risposta del percorso del media scaricato, bloccandosi in attesa della notifica.
     */
    public class MediaRequest implements Callable<String> {

        private byte[] bytes;
        private DataOutputStream outputStream;
        String header;

        public MediaRequest(byte[] bytes, DataOutputStream outputStream, String header) {
            this.bytes = bytes;
            this.outputStream = outputStream;
            this.header = header;
        }

        @Override
        public String call() throws Exception {
            try {
                byte[] headerBytes = header.getBytes();
                byte[] total = new byte[bytes.length+HEADER_SIZE];
                System.arraycopy(headerBytes,0,total,0,headerBytes.length);
                System.arraycopy(bytes,0,total,HEADER_SIZE,bytes.length);
                outputStream.writeInt(total.length);
                outputStream.write(total);
                outputStream.flush();
                String res="";
                /**
                 * Il thread attende finchè la foto non è stata scaricata e settata all'interno di fotoDownloaded.
                 */
                synchronized (fotoDownloaded)
                {
                    while (fotoDownloaded.length()==0)
                        fotoDownloaded.wait();
                    res=fotoDownloaded.toString();
                    fotoDownloaded.setLength(0);
                }
                return res;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    /**
     * Thread per inviare il messaggio.
     */
    public class Send extends Thread{

        private byte[] bytes;
        private DataOutputStream outputStream;
        String header;

        public Send(DataOutputStream o)
        {
            outputStream=o;
        }
        public void setBytes(String hd,byte[] b)
        {
            bytes=b;
            header=hd;
        }
        @Override
        public void run() {
            try {
                byte[] headerBytes = header.getBytes();
                byte[] total = new byte[bytes.length+HEADER_SIZE];
                System.arraycopy(headerBytes,0,total,0,headerBytes.length);
                System.arraycopy(bytes,0,total,HEADER_SIZE,bytes.length);
                outputStream.writeInt(total.length);
                outputStream.write(total);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
