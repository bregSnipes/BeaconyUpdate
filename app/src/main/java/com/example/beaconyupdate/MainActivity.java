package com.example.beaconyupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer {

    private final DfuProgressListener dfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(final String deviceAddress) {
            //progressBar.setIndeterminate(true);
            //textPercentage.setText(R.string.dfu_status_connecting);
        }

        @Override
        public void onDfuProcessStarting(final String deviceAddress) {
            //progressBar.setIndeterminate(true);
            //textPercentage.setText(R.string.dfu_status_starting);
        }
        ///...
    };
    private BluetoothAdapter bAdapter;
    private BluetoothManager bManager;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;
    private BeUtility be;
    private final int SDK_REQUEST = 33;
    private final int BLUETOOTH_ON_OFF = 88;
    private Button button_fw_home_layout,button_upload_home_layout;
    private EditText edit_name_home_layout;
    private ConstraintLayout home_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setGui();
        create_xml_events();

        bManager = (BluetoothManager) getSystemService(MainActivity.this.BLUETOOTH_SERVICE);
        bAdapter= bManager.getAdapter();

        be = new BeUtility(this);
        be.AddObserver();

        if (Build.VERSION.SDK_INT > 23) {
            //SE LA VERSIONE DI ANDROID E' MARSHMELLOW O SUPERIORE, BISOGNA CHIEDERE I PERMESSI PER FAR PARTIRE LA SCAN
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, SDK_REQUEST);
        }
        else{
            bluetooth_ble_state();
        }



        //startDfuService();  --> Per far partire il DFU


        //DfuServiceInitiator.createDfuNotificationChannel(context);   --> Android Oreo o superiore, per visualizzare progresso upload


    }

    private void setGui(){
        //Layouts
        home_layout = findViewById(R.id.home_layout);


        //Buttons
        button_fw_home_layout = findViewById(R.id.button_fw_home_layout);
        button_upload_home_layout = findViewById(R.id.button_upload_home_layout);


        //EditTexts
        edit_name_home_layout = findViewById(R.id.edit_name_home_layout);
    }

    private void create_xml_events(){
        button_upload_home_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!be.IsScanning()){
                    be.StartScan(0);
                }
            }
        });
    }

    private void startDfuService(BluetoothDevice mSelectedDevice){
       /* final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDevice.getAddress())
                .setDeviceName(mSelectedDevice.getName())
                .setKeepBond(keepBond);
        // If you want to have experimental buttonless DFU feature (DFU from SDK 12.x only!) supported call
        // additionally:
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        // but be aware of this: https://devzone.nordicsemi.com/question/100609/sdk-12-bootloader-erased-after-programming/
        // and other issues related to this experimental service.

        // For DFU bootloaders from SDK 15 and 16 it may be required to add a delay before sending each
        // data packet. This delay gives the DFU target more time to perpare flash memory, causing less
        // packets being dropped and more reliable transfer. Detection of packets being lost would cause
        // automatic switch to PRN = 1, making the DFU very slow (but reliable).
        stater.setPrepareDataObjectDelay(300L);

        // Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
        // In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
        if (mFileType == DfuService.TYPE_AUTO)
            starter.setZip(mFileStreamUri, mFilePath);
        else {
            starter.setBinOrHex(mFileType, mFileStreamUri, mFilePath).setInitFile(mInitFileStreamUri, mInitFilePath);
        }
        final DfuServiceController controller = starter.start(this, DfuService.class);
        // You may use the controller to pause, resume or abort the DFU process.

        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DfuServiceListenerHelper.unregisterProgressListener(this, dfuProgressListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(android.os.Build.VERSION.SDK_INT >= 21)  //Dall'API 21 in poi è possibile rimuovere l'App appena chiusa dalla lista delle app recenti.
        {
            finishAndRemoveTask();
        }
        else
        {
            finish();   //Se API<21, allora rimarrà nella lista delle App recenti, ma il processo è completamente finito.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == BLUETOOTH_ON_OFF && resultCode == Activity.RESULT_OK){
            Utility.makeToast(MainActivity.this,"BLUETOOTH ACTIVATED!",0);
            bluetooth_ble_state();
        }
        else if(requestCode == BLUETOOTH_ON_OFF && resultCode == Activity.RESULT_CANCELED){
            Utility.makeToast(MainActivity.this,"BLUETOOTH NEEDS\nTO BE ON TO WORK!",0);
            MainActivity.this.finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == SDK_REQUEST){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                bluetooth_ble_state();
            }
            else if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED){
                Utility.makeToast(MainActivity.this,"BLUETOOTH NEEDS\nTO BE ON TO WORK!",0);
                MainActivity.this.finish();
                return;
            }

        }
    }

    protected void bluetooth_ble_state(){

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Utility.makeToast(this,"BLUETOOTH NOT SUPPORTED ON THIS DEVICE",0);
            finish();
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Utility.makeToast(this,"BLE NOT SUPPORTED ON THIS DEVICE",0);
            finish();
            return;
        }

        if (bAdapter== null || !bAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth,BLUETOOTH_ON_OFF);
        }
        else{
            //Do nothing
        }

    }


    @Override
    public void update(Observable observable, Object o) {
        //Scan Results

        if(String.valueOf(o).equals("stop")){
            System.out.println("STOP!");
            return;
        }

        ScanResult result = (ScanResult)o;
        BluetoothDevice device = result.getDevice();

        try {
            if (device.getName().equalsIgnoreCase(edit_name_home_layout.getText().toString())) {
                //Device Name scelto
                if (be.IsScanning()) be.StopScan();
            }
        }
        catch(Exception e){
            //Do nothing
        }



    }
}
