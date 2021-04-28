package com.example.beaconyupdate;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import androidx.core.app.ActivityCompat;


public class BeUtility extends Observable{

    BluetoothManager manager;
    BluetoothAdapter adapter;
    private final int REQUEST_ENABLE_BT = 1;
    private final int SDK_REQUEST = 2;
    Activity calling;
    BluetoothLeScanner scanner;
    ScanCallback callback;
    Handler handler;
    boolean SCAN_STATE;
    ScanSettings scanSettings;
    ScanFilter scanFilter;
    List<ScanFilter> filters = new ArrayList<ScanFilter>();

    public BeUtility(final Activity calling){
        this.calling = calling;
        SCAN_STATE = false;
        manager = (BluetoothManager) calling.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager.getAdapter();
        scanner = adapter.getBluetoothLeScanner();




        callback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, final ScanResult result) {
                //super.onScanResult(callbackType, result);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        setChanged();
                        notifyObservers(result);
                    }
                });

            }
        };
    }

    public void StartScan(int interval,String deviceToFilter){
        SCAN_STATE = true;

        /*scanSettings = new ScanSettings.Builder()
                .setScanMode( ScanSettings.SCAN_MODE_LOW_LATENCY )
                .build();

        scanFilter = new ScanFilter.Builder()
                //.setServiceUuid( new ParcelUuid(UUID.fromString(UUID_TO_FILTER) ) )
                .setDeviceName(deviceToFilter)
                .build();
        filters.add(scanFilter);*/

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.startScan(callback);
            }
        });

        if(interval == 0)   return;
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                StopScan();
            }
        },interval);
    }

    public void StopScan(){

        //if(!MainActivity.RED_LIGHT) MainActivity.RED_LIGHT = true;
        scanner.stopScan(callback);
        SCAN_STATE = false;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                setChanged();
                notifyObservers("stop");
            }
        });
    }

    public boolean IsScanning(){
        return SCAN_STATE;
    }

    public void AddObserver(){
        addObserver((Observer) calling);
    }

    }
