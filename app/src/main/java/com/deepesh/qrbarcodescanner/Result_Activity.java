package com.deepesh.qrbarcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdOptions;

import java.util.Objects;

public class Result_Activity extends AppCompatActivity {

    Toolbar toolbar;
    TextView textView;
    AdLoader adLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_activity);

        textView = findViewById(R.id.result_textview);
        // url color
        textView.setLinkTextColor(Color.BLUE);

        toolbar = findViewById(R.id.result_toolbar);
        setSupportActionBar(toolbar);
        // setup back button
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);


        Intent intent = getIntent();
        String data = intent.getStringExtra("data");
        textView.setText(data);


        // Loading Native Ad
        //==========================================================

        adLoader = new AdLoader.Builder(getApplicationContext(), "ca-app-pub-3940256099942544/2247696110")
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd NativeAd) {
                        // Show the ad.
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError adError) {
                        // Handle the failure by logging, altering the UI, and so on.
                    }
                })
                .withNativeAdOptions(new NativeAdOptions.Builder()
                        // Methods in the NativeAdOptions.Builder class can be
                        // used here to specify individual options settings.
                        .build())
                .build();


    }


    //============================================================


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // setting options menu
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.options_menu_result, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId()==R.id.share){

            String data = textView.getText().toString();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, data);
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, "Select an app...");
            startActivity(shareIntent);
        }

        return super.onOptionsItemSelected(item);
    }

}















