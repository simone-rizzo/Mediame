package com.rizzo.mediame;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * Classe utility che serve per comprimere, leggere, ridimensionare Bitmap.
 */

public class BitmapUtils {
    /**
     * Decodifica una foto in Bitmap riducendo la dimensione per ottimizzare la lettura su recyclerview
     * @param media_path
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap decodeSampledBitmapFromResource(String media_path, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(media_path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(media_path, options);
    }

    /**
     * Calcola la dimensione ottimale della foto
     * @param options opzione di decodifica
     * @param reqWidth larghezza
     * @param reqHeight altezza
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /**
     * Comprime l'immagine Bitmap
     * @param bitmap immagine bitmap
     * @param quality definisce la qualità della compressione 100 è massima qualità
     * @return ritorna l'array di bites dell'immagine compressa
     */
    public static byte[] getBytesFromBitmap(Bitmap bitmap,int quality)
    {
        ByteArrayOutputStream strem = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,quality,strem);
        return strem.toByteArray();
    }

    /**
     * Dato un path di un'immagine legge il bitmap dell'immagine la ridimenziona
     * in width e height applica una compressione sulla qualità e restituisce
     * un array di byte.
     * @param path
     * @param quality intero da 0 a 100 dove 100 è l'immagine senza alcuna compressione.
     * @param width
     * @param height
     * @return
     */
    public static byte[] getBytesImageCompressedFromPath(String path,int quality,int width, int height)
    {
        return getBytesFromBitmap(decodeSampledBitmapFromResource(path,width,height),quality);
    }
}
