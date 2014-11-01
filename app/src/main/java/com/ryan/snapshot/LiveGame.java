package com.ryan.snapshot;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import com.ryan.snapshot.camera.IntentIntegrator;
import android.widget.Button;
import android.widget.Toast;
import com.ryan.snapshot.camera.IntentResult;

public class LiveGame extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_game);

        final Button scanCode = (Button) findViewById(R.id.buttonScanQRCode);
        scanCode.setOnClickListener(new android.view.View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                IntentIntegrator integrator = new IntentIntegrator(LiveGame.this);
                integrator.initiateScan();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (scanResult != null) {

            // handle scan result
            final String contantsString = scanResult.getContents()==null?"0":scanResult.getContents();
            if (contantsString.equalsIgnoreCase("0")) {
                Toast.makeText(this, "Problem to get the  contant Number", Toast.LENGTH_LONG).show();

            }else {
                Toast.makeText(this, contantsString, Toast.LENGTH_LONG).show();

            }
        }
        else{
            Toast.makeText(this, "Problem to secan the barcode.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.live_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
