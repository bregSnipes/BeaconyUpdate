package com.example.beaconyupdate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import android.app.LoaderManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements Observer, LoaderManager.LoaderCallbacks<Cursor> {


    private static final String EXTRA_URI = "uri";
    private static final int EXIT_REQUEST = 154;
    public static boolean RED_LIGHT;

    private final DfuProgressListener dfuProgressListener = new DfuProgressListenerAdapter() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            Log.d("CHECK22","On Device Connecting");
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            Log.d("CHECK22","On Device Connected");
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            Log.d("CHECK22","On Dfu Process Starting");
            txt_state_upload_layout.setText("STARTING OTA...");
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            Log.d("CHECK22","On Dfu Process Started");
            txt_state_upload_layout.setText("OTA STARTED!");
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            Log.d("CHECK22","On Enabling Dfu Mode");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            Log.d("CHECK22", "Uploading...");
            if(!txt_state_upload_layout.getText().equals("UPLOADING..."))  txt_state_upload_layout.setText("UPLOADING...");
            txt_progress_upload_layout.setText(percent + " %");
            if(progressBar.getVisibility() != View.VISIBLE) progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            Log.d("CHECK22","On Firmware Validating");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            Log.d("CHECK22","On Device Disconnecting");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            Log.d("CHECK22","On Device Disconnected");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            Log.d("CHECK22","On Dfu Completed");
            if(OTA_ENABLED) OTA_ENABLED = false;
            if(!be.IsScanning())    be.StartScan(0);
            txt_state_upload_layout.setText("SEARCHING BEACONS...");
            txt_progress_upload_layout.setText("");
            if(progressBar.getVisibility() != View.GONE) progressBar.setVisibility(View.GONE);
            timer.start();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            Log.d("CHECK22","On Dfu Aborted");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            Log.d("CHECK22","On Error");
            if(OTA_ENABLED) OTA_ENABLED = false;
            if(!be.IsScanning())    be.StartScan(0);
            txt_state_upload_layout.setText("SEARCHING BEACONS...");
            txt_progress_upload_layout.setText("");
            if(progressBar.getVisibility() != View.GONE) progressBar.setVisibility(View.GONE);
            timer.start();
        }
    };

    private  DfuServiceController controller;
    private BluetoothAdapter bAdapter;
    private BluetoothManager bManager;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;
    private BluetoothGattCharacteristic ff8b,ff8a, ffaa;
    private BeUtility be;
    private int progressSeekBar = 0;
    private int RSSI_UNIT = 0;
    private int RSSI_VALUE;
    private final int SDK_REQUEST = 33;
    private final int BLUETOOTH_ON_OFF = 88;
    private Button button_fw_home_layout,button_upload_home_layout;
    private EditText edit_name_home_layout;
    private TextView txt_fw_home_layout,txt_state_upload_layout,txt_progress_upload_layout,txt_rssi_home_layout;
    private SeekBar seekbar_rssi_home_layout;
    private ConstraintLayout home_layout,upload_layout;
    private ArrayList<String> mac_listened;
    private boolean CONNECTION_STATE;
    private final int SELECT_FILE_REQ = 1;
    private static final int SELECT_INIT_FILE_REQ = 2;
    private Uri fileStreamUri;
    private String filePath,mPath;
    private String initFilePath;
    private Uri initFileStreamUri;
    BluetoothDevice device;
    private int fileType;
    private int fileTypeTmp; // This value is being used when user is selecting a file not to overwrite the old value (in case he/she will cancel selecting file)
    private boolean statusOk;
    private boolean OTA_ENABLED;
    private Integer scope;
    private ProgressBar progressBar;
    private CountDownTimer timer;
    ToneGenerator tone;

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle args) {
        final Uri uri = args.getParcelable(EXTRA_URI);
        /*
         * Some apps, f.e. Google Drive allow to select file that is not on the device. There is no "_data" column handled by that provider. Let's try to obtain
         * all columns and than check which columns are present.
         */
        // final String[] projection = new String[] { MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA };
        return new CursorLoader(this, uri, null /* all columns, instead of projection */, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToNext()) {
            /*
             * Here we have to check the column indexes by name as we have requested for all. The order may be different.
             */
            final String fileName = data.getString(data.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)/* 0 DISPLAY_NAME */);
            final int fileSize = data.getInt(data.getColumnIndex(MediaStore.MediaColumns.SIZE) /* 1 SIZE */);
            String filePath = null;
            final int dataIndex = data.getColumnIndex(MediaStore.MediaColumns.DATA);
            if (dataIndex != -1)
                filePath = data.getString(dataIndex /* 2 DATA */);
            if (!TextUtils.isEmpty(filePath))
                this.filePath = filePath;
            updateFileInfo(fileName,fileSize,fileType);
        } else {
            Utility.makeToast(MainActivity.this,"You have to choose a .zip file first!",0);
            filePath = null;
            fileStreamUri = null;
            txt_fw_home_layout.setText("NO FILE");
        }
    }

    private void updateFileInfo(final String fileName, final long fileSize, final int fileType) {
        txt_fw_home_layout.setText(fileName);
        final String extension = this.fileType == DfuService.TYPE_AUTO ? "(?i)ZIP" : "(?i)HEX|BIN"; // (?i) =  case insensitive
        final boolean statusOk = this.statusOk = MimeTypeMap.getFileExtensionFromUrl(fileName).matches(extension);

        // Ask the user for the Init packet file if HEX or BIN files are selected. In case of a ZIP file the Init packets should be included in the ZIP.
        if (statusOk) {
            if (fileType != DfuService.TYPE_AUTO) {
                scope = null;
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dfu_file_init_title)
                        .setMessage(R.string.dfu_file_init_message)
                        .setNegativeButton(R.string.no, (dialog, which) -> {
                            initFilePath = null;
                            initFileStreamUri = null;
                        })
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType(DfuService.MIME_TYPE_OCTET_STREAM);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, SELECT_INIT_FILE_REQ);
                        })
                        .show();
            } else {
                /*new AlertDialog.Builder(this).setTitle(R.string.dfu_file_scope_title).setCancelable(false)
                        .setSingleChoiceItems(R.array.dfu_file_scope, 0, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    scope = null;
                                    break;
                                case 1:
                                    scope = DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS;
                                    break;
                                case 2:
                                    scope = DfuServiceInitiator.SCOPE_APPLICATION;
                                    break;
                            }
                        }).setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                    int index;
                    if (scope == null) {
                        index = 0;
                    } else if (scope == DfuServiceInitiator.SCOPE_SYSTEM_COMPONENTS) {
                        index = 1;
                    } else {
                        index = 2;
                    }
                }).show();*/
            }
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        filePath = null;
        fileStreamUri = null;
        statusOk = false;
    }

    private enum ACTIVE_LAYOUT{
        HOME,
        UPLOAD
    }
    private ACTIVE_LAYOUT active_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        active_layout = ACTIVE_LAYOUT.HOME;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //DfuServiceListenerHelper.registerProgressListener(this, dfuProgressListener);


        setGui();
        create_xml_events();

        setGattCallback();

        bManager = (BluetoothManager) getSystemService(MainActivity.this.BLUETOOTH_SERVICE);
        bAdapter= bManager.getAdapter();

        tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        mac_listened = new ArrayList<String>();
        CONNECTION_STATE = false;

        be = new BeUtility(this);
        be.AddObserver();

        if (Build.VERSION.SDK_INT > 23) {
            //SE LA VERSIONE DI ANDROID E' MARSHMELLOW O SUPERIORE, BISOGNA CHIEDERE I PERMESSI PER FAR PARTIRE LA SCAN
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, SDK_REQUEST);
        }
        else{
            bluetooth_ble_state();
        }

    }

    private void setGui(){
        //Layouts
        home_layout = findViewById(R.id.home_layout);
        upload_layout = findViewById(R.id.upload_layout);


        //Buttons
        button_fw_home_layout = findViewById(R.id.button_fw_home_layout);
        button_upload_home_layout = findViewById(R.id.button_upload_home_layout);


        //EditTexts
        edit_name_home_layout = findViewById(R.id.edit_name_home_layout);

        //TextViews
        txt_fw_home_layout = findViewById(R.id.txt_fw_home_layout);
        txt_state_upload_layout = findViewById(R.id.txt_state_upload_activity);
        txt_progress_upload_layout = findViewById(R.id.txt_progress_upload_layout);
        txt_rssi_home_layout = findViewById(R.id.txt_rssi_home_layout);

        //ProgressBar
        progressBar = findViewById(R.id.progressbar_upload_layout);
        Drawable draw = getResources().getDrawable(R.drawable.custom_progressbar);
        progressBar.setProgressDrawable(draw);

        //Seekbar
        seekbar_rssi_home_layout = findViewById(R.id.seekbar_rssi_home_layout);

        //CountDownTimer
        timer = new CountDownTimer(30000,30000) {
            @Override
            public void onTick(long l) {
                //Do nothing
            }

            @Override
            public void onFinish() {

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while(!CONNECTION_STATE && !OTA_ENABLED && active_layout == ACTIVE_LAYOUT.UPLOAD){
                            tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,150);
                            try {
                                Thread.sleep(1000);
                            }catch(Exception e){}
                        }
                    }
                }).start();
            }
        };
}

    private void create_xml_events(){

        seekbar_rssi_home_layout.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                progressSeekBar = i;
                txt_rssi_home_layout.setText("RSSI LIMIT: " + String.valueOf(-(progressSeekBar+RSSI_UNIT)) + " dBm");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                RSSI_VALUE = -(progressSeekBar+RSSI_UNIT);
            }
        });

        button_upload_home_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!statusOk) {
                    Utility.makeToast(MainActivity.this,"Please, select valid HEX file",0);
                    return;
                }
                if(edit_name_home_layout.getText().length() > 0){
                    be.StartScan(0);
                    active_layout = ACTIVE_LAYOUT.UPLOAD;
                    home_layout.setVisibility(View.GONE);
                    upload_layout.setVisibility(View.VISIBLE);
                    active_layout=ACTIVE_LAYOUT.UPLOAD;
                    txt_state_upload_layout.setText("SEARCHING BEACONS...");
                    timer.start();
                }
                else{
                    Utility.makeToast(MainActivity.this,"Device Name needs to be at least 1 character!",0);
                }
            }
        });
        button_fw_home_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fileTypeTmp = fileType;
                int index = 0;
                switch (fileType) {
                    case DfuService.TYPE_AUTO:
                        index = 0;
                        break;
                    case DfuService.TYPE_SOFT_DEVICE:
                        index = 1;
                        break;
                    case DfuService.TYPE_BOOTLOADER:
                        index = 2;
                        break;
                    case DfuService.TYPE_APPLICATION:
                        index = 3;
                        break;
                }
                // Show a dialog with file types
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.dfu_file_type_title)
                        .setSingleChoiceItems(R.array.dfu_file_type, index, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    fileTypeTmp = DfuService.TYPE_AUTO;
                                    break;
                                case 1:
                                    fileTypeTmp = DfuService.TYPE_SOFT_DEVICE;
                                    break;
                                case 2:
                                    fileTypeTmp = DfuService.TYPE_BOOTLOADER;
                                    break;
                                case 3:
                                    fileTypeTmp = DfuService.TYPE_APPLICATION;
                                    break;
                            }
                        })
                        .setPositiveButton(R.string.ok, (dialog, which) -> openFileChooser())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
    });
    }


    private void openFileChooser(){
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(fileTypeTmp == DfuService.TYPE_AUTO ? DfuService.MIME_TYPE_ZIP : DfuService.MIME_TYPE_OCTET_STREAM);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, SELECT_FILE_REQ);
    }

    private void startDfuService(BluetoothDevice mSelectedDevice){
        //System.out.println("Start DFU Service!" + "\n" + "Device uploading: " + mSelectedDevice.getAddress());
        final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDevice.getAddress())
                .setDeviceName(mSelectedDevice.getName())
                .setKeepBond(false);

        //starter.createDfuNotificationChannel(this);
        // If you want to have experimental buttonless DFU feature (DFU from SDK 12.x only!) supported call
        // additionally:
        starter.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(true);
        // but be aware of this: https://devzone.nordicsemi.com/question/100609/sdk-12-bootloader-erased-after-programming/
        // and other issues related to this experimental service.

        // For DFU bootloaders from SDK 15 and 16 it may be required to add a delay before sending each
        // data packet. This delay gives the DFU target more time to perpare flash memory, causing less
        // packets being dropped and more reliable transfer. Detection of packets being lost would cause
        // automatic switch to PRN = 1, making the DFU very slow (but reliable).
        starter.setPrepareDataObjectDelay(300L);

        // Init packet is required by Bootloader/DFU from SDK 7.0+ if HEX or BIN file is given above.
        // In case of a ZIP file, the init packet (a DAT file) must be included inside the ZIP file.
        if (fileType == DfuService.TYPE_AUTO) {
            //System.out.println("Uri: " + fileStreamUri + "\n" + "Path: " + filePath);
            starter.setZip(fileStreamUri, filePath);
        }
        else {
            starter.setBinOrHex(fileType, fileStreamUri, filePath).setInitFile(initFileStreamUri, initFilePath);
        }
         controller = starter.start(this, DfuService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            starter.createDfuNotificationChannel(this);
        }
        //starter.start(this, DfuService.class);
        // You may use the controller to pause, resume or abort the DFU process.

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
        try {
            if (!controller.isAborted()) controller.abort();
        }catch(Exception e){}
        timer.cancel();
        if(CONNECTION_STATE)    bluetoothGatt.disconnect();
        if(be.IsScanning()) be.StopScan();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        switch(active_layout){
            case HOME:
                Intent i = new Intent(MainActivity.this,Popup.class);
                i.putExtra("action","exit");    //PRIMA ERA RETURN TO SCAN
                startActivityForResult(i,EXIT_REQUEST);
                break;
            case UPLOAD:
                if(OTA_ENABLED) OTA_ENABLED = false;
                try {
                    if (!controller.isAborted()) controller.abort();
                }catch(Exception e){}
                timer.cancel();
                upload_layout.setVisibility(View.GONE);
                home_layout.setVisibility(View.VISIBLE);
                active_layout = ACTIVE_LAYOUT.HOME;
                txt_progress_upload_layout.setText("");
                txt_state_upload_layout.setText("");
                if(progressBar.getVisibility() != View.GONE) progressBar.setVisibility(View.GONE);
                if(!mac_listened.isEmpty()) mac_listened.clear();
                if(CONNECTION_STATE)    bluetoothGatt.disconnect();
                if(be.IsScanning()) be.StopScan();
                break;
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
        else if(requestCode == SELECT_FILE_REQ && resultCode == Activity.RESULT_OK){

            // clear previous data
            fileType = fileTypeTmp;
            filePath = null;
            fileStreamUri = null;

            // and read new one
            final Uri uri = data.getData();
            mPath = uri.getPath();
            /*
             * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
             * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
             */
            if(uri.getScheme().equals("content")) {
                // an Uri has been returned
                fileStreamUri = uri;
                // if application returned Uri for streaming, let's us it. Does it works?
                // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                final Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                    fileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);

                // file name and size must be obtained from Content Provider
                final Bundle bundle = new Bundle();
                bundle.putParcelable(EXTRA_URI, uri);
                getLoaderManager().restartLoader(SELECT_FILE_REQ, bundle, this);
            }
        }
        else if(requestCode == SELECT_FILE_REQ && resultCode == Activity.RESULT_CANCELED){
            //Do nothing
        }
        else if(requestCode == SELECT_INIT_FILE_REQ && resultCode == Activity.RESULT_OK){
            initFilePath = null;
            initFileStreamUri = null;

            // and read new one
            final Uri uri = data.getData();
            /*
             * The URI returned from application may be in 'file' or 'content' schema. 'File' schema allows us to create a File object and read details from if
             * directly. Data from 'Content' schema must be read by Content Provider. To do that we are using a Loader.
             */
            if (uri.getScheme().equals("file")) {
                // the direct path to the file has been returned
                initFilePath = uri.getPath();
            } else if (uri.getScheme().equals("content")) {
                // an Uri has been returned
                initFileStreamUri = uri;
                // if application returned Uri for streaming, let's us it. Does it works?
                // FIXME both Uris works with Google Drive app. Why both? What's the difference? How about other apps like DropBox?
                final Bundle extras = data.getExtras();
                if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                    initFileStreamUri = extras.getParcelable(Intent.EXTRA_STREAM);
            }
        }
        else if(requestCode == EXIT_REQUEST && resultCode == Activity.RESULT_OK){

            //Close Application
            if(android.os.Build.VERSION.SDK_INT >= 21)  //Dall'API 21 in poi è possibile rimuovere l'App appena chiusa dalla lista delle app recenti.
            {
                finishAndRemoveTask();
            }
            else
            {
                finish();   //Se API<21, allora rimarrà nella lista delle App recenti, ma il processo è completamente finito.
            }
        }
        else if(requestCode == EXIT_REQUEST && resultCode == Activity.RESULT_CANCELED){
            //Do nothing
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
            Log.d("CHECK22", "Scan Stopped!");
            return;
        }

        Log.d("CHECK22", "Scanning...");

        ScanResult result = (ScanResult) o;
        device = result.getDevice();

        if(!OTA_ENABLED) {

            try {
                if (device.getName().toLowerCase().contains(edit_name_home_layout.getText().toString().toLowerCase()) && result.getRssi() >= RSSI_VALUE) {
                    //Device Name scelto
                    if (mac_listened.size() > 0) {
                        boolean found = false;
                        for (int i = 0; i < mac_listened.size(); i++) {
                            if (mac_listened.get(i).equals(device.getAddress())) {
                                found = true;
                                break;
                            }
                        }
                        if (!found && !RED_LIGHT) {
                            timer.cancel();
                            if (be.IsScanning()) be.StopScan();
                            bluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
                            mac_listened.add(device.getAddress());
                        } else {
                            //Do nothing
                        }
                    } else {
                        if(!RED_LIGHT) {
                            timer.cancel();
                            if (be.IsScanning()) be.StopScan();
                            bluetoothGatt = device.connectGatt(MainActivity.this, false, gattCallback);
                            mac_listened.add(device.getAddress());
                        }
                        else{
                            //Do Nothing
                        }
                    }
                }
            } catch (Exception e) {
                //Do nothing
            }


        }
        else{
            //Scan per dispositivo OTA
            try{
                if (device.getName().equalsIgnoreCase("DfuTarg") && result.getRssi() >= RSSI_VALUE) {
                    //Device Name scelto
                    if (be.IsScanning()) be.StopScan();
                    startDfuService(device);
                }
            }
            catch(Exception e){
                //Do nothing
            }
        }

    }

    private void setGattCallback(){
        gattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);

                if(newState == BluetoothGatt.STATE_CONNECTED){

                    Log.d("CHECK22", "CONNECTED TO DEVICE: " + gatt.getDevice().getAddress().toUpperCase());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_state_upload_layout.setText(gatt.getDevice().getAddress().toUpperCase() + " CONNECTED!\nPAIRING...");
                        }
                    });
                    CONNECTION_STATE = true;
                    ff8a = null;
                    ff8b = null;
                    ffaa = null;
                    bluetoothGatt.discoverServices();
                }
                else if(newState == BluetoothGatt.STATE_DISCONNECTED){
                    Log.d("CHECK22", "DISCONNECTED FROM DEVICE: " + gatt.getDevice().getAddress().toUpperCase());
                    CONNECTION_STATE = false;
                    if(OTA_ENABLED){
                        if(!be.IsScanning())    be.StartScan(0);
                    }
                    else if(!OTA_ENABLED && active_layout == ACTIVE_LAYOUT.UPLOAD){
                        if(!be.IsScanning())    be.StartScan(0);
                        txt_state_upload_layout.setText("SEARCHING BEACONS...");
                        txt_progress_upload_layout.setText("");
                        if(progressBar.getVisibility() != View.GONE) progressBar.setVisibility(View.GONE);
                        timer.start();
                    }
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                ArrayList<BluetoothGattService> servizi = new ArrayList<>(gatt.getServices());
                ArrayList<BluetoothGattCharacteristic> caratteristiche = new ArrayList<>();

                for (int i = 0; i < servizi.size(); i++) {
                    if (servizi.get(i).getUuid().toString().contains("ff80")) {
                        caratteristiche.addAll(servizi.get(i).getCharacteristics());
                        for (int j = 0; j < caratteristiche.size(); j++) {
                            if (caratteristiche.get(j).getUuid().toString().contains("ff8b")) {
                                ff8b = caratteristiche.get(j);
                                ff8b.setValue(Utility.hexStringToByteArray("ff"));
                            } else if (caratteristiche.get(j).getUuid().toString().contains("ff8a")) {
                                ff8a = caratteristiche.get(j);
                                ff8a.setValue(Utility.hexStringToByteArray("383838383838"));
                            }
                        }
                    } else if (servizi.get(i).getUuid().toString().contains("ff82")) {
                        caratteristiche.addAll(servizi.get(i).getCharacteristics());
                        for (int j = 0; j < caratteristiche.size(); j++) {
                            if (caratteristiche.get(j).getUuid().toString().contains("ffaa")) {
                                ffaa = caratteristiche.get(j);
                                ffaa.setValue(Utility.hexStringToByteArray("01"));
                            }
                        }
                    }
                }

                if(ff8a != null && ff8b != null && ffaa != null){
                    Log.d("CHECH22", "BEACON SUPPORTS OTA! ");
                    gatt.writeCharacteristic(ff8a);
                }
                else{
                    Log.d("CHECK22", "BEACON DOESN'T SUPPORT OTA");
                    gatt.disconnect();
                }

            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if(characteristic == ff8a){
                    gatt.writeCharacteristic(ff8b);
                }
                else if(characteristic == ff8b){
                    Log.d("CHECK22", "BEACON PAIRED!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_state_upload_layout.setText("BEACONY PAIRED!");
                        }
                    });
                    gatt.writeCharacteristic(ffaa);
                }
                else if(characteristic == ffaa){
                    Log.d("CHECK22", "OTA ACTIVATED!");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_state_upload_layout.setText("OTA ACTIVATED!");
                        }
                    });
                    OTA_ENABLED = true;
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
            }
        };
    }

}
