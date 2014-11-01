package com.ryan.snapshot;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

public class Lobby extends Activity {
    private EditText etInput;
    private Button btnAdd;
    private ListView lvItem;
    private ArrayList<String> itemArrey;
    private ArrayAdapter<String> itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);
        setUpView();

        final Button buttonToLiveGame = (Button) findViewById(R.id.buttonLiveGame);
        buttonToLiveGame.setOnClickListener(liveGameListener);
    }

    private void setUpView() {
        // TODO Auto-generated method stub
        etInput = (EditText)this.findViewById(R.id.editText_input);
        btnAdd = (Button)this.findViewById(R.id.button_add);
        lvItem = (ListView)this.findViewById(R.id.listView_items);


        itemArrey = new ArrayList<String>();
        itemArrey.clear();

        itemAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,itemArrey);
        lvItem.setAdapter(itemAdapter);


        btnAdd.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                addItemList();
            }
        });

        etInput.setOnKeyListener(new View.OnKeyListener() {

            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // TODO Auto-generated method stub

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    addItemList();
                }
                return true;
            }
        });


    }

    protected void addItemList() {
        // TODO Auto-generated method stub

        // TODO Auto-generated method stub
        if (isInputValid(etInput)) {
            itemArrey.add(0,etInput.getText().toString());
            etInput.setText("");

            itemAdapter.notifyDataSetChanged();

        }

    }

    protected boolean isInputValid(EditText etInput2) {
        // TODO Auto-generatd method stub
        if (etInput2.getText().toString().trim().length()<1) {
            etInput2.setError("Please Enter Item");
            return false;
        } else {
            return true;
        }

    }


    private final View.OnClickListener liveGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final Intent toLiveGame = new Intent(Lobby.this, LiveGame.class);
            startActivity(toLiveGame);
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.game_set_up, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
