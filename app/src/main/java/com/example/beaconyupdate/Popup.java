package com.example.beaconyupdate;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Popup extends Activity {

    private int width;  //Width of the Popup
    private int height; //Height of the Popup
    private Button button_ok;
    private TextView txt_msg;
    private Button button_Exit;
    private Intent intent_received; //Intent ricevuto da ScanActivity
    private Bundle extra_received;  //Valori EXTRA ricevuti dall'Intent partito dalla ScanActivity
    private BluetoothDevice chosen_device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup);


        DisplayMetrics dm=new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        width=dm.widthPixels;
        height=dm.heightPixels;
        //Larghezza all'80%, Altezza al 20%
        getWindow().setLayout((int)(width*.8),(int)(height*.4));

        button_ok=(Button)findViewById(R.id.button_ok_popup);
        txt_msg=(TextView)findViewById(R.id.txt_msg_popup);
        button_Exit=(Button)findViewById(R.id.buttonExit_popup);
        create_xml_events();

        intent_received=getIntent();    //L'EXTRA DATA che ricevo mi fa capire se arrivo da go_to_scan o da go_to detector
        extra_received=intent_received.getExtras();

        if(extra_received.get("action").equals("exit")){
            txt_msg.setText("ARE YOU SURE YOU\n WANT TO EXIT THIS\n APPLICATION?");
            button_ok.setText("EXIT");
            button_Exit.setText("CANCEL");
        }
    }

    public void create_xml_events(){
        button_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                setResult(Activity.RESULT_OK,i);
                Popup.this.finish();


            }
        });

        button_Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                setResult(Activity.RESULT_CANCELED,i);
                Popup.this.finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
