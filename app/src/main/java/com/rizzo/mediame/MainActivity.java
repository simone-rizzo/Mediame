package com.rizzo.mediame;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    LinearLayout btnscannerizza,btngenera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnscannerizza=(LinearLayout)findViewById(R.id.btnscannerizzaqr);
        btngenera=(LinearLayout)findViewById(R.id.btngeneraqr);
        btnscannerizza.setOnClickListener(this);
        btngenera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v==btnscannerizza)
        {
            /**
             * Lancia l'intent di aprire la fotocamera per scannerizzare
             */
            Intent i = new Intent(this, Scannerizzaqr.class);
            startActivity(i);

        }
        else if(v==btngenera)
        {
            /**
             * Lancia il generatore di qr che prima chiede il wifi poi prende le specifiche del device
             * e genera il qr
             */
            Intent i = new Intent(this, Generaqr.class);
            startActivity(i);
        }
    }
}
