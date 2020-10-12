package com.rizzo.mediame;

/**
 * Interfaccia per gestire il click degli item della recycler view
 */
public interface MediaClickListner {
    /**
     * Metodo per gestire la chiamata dell' onClickListner della recycler view
     * @param path
     */
    void onMediaClick(String path,String id);
}
