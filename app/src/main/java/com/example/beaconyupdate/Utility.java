package com.example.beaconyupdate;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Array;


public class Utility {

    public static void makeToast(Context context, String msg,int duration) {
        LinearLayout toastLayout;
        final Toast custom_toast;
        TextView toast_style;


        switch(duration){
            case 0:
                custom_toast=Toast.makeText(context, msg, Toast.LENGTH_SHORT);
                toastLayout=(LinearLayout)custom_toast.getView();
                toast_style=(TextView)toastLayout.getChildAt(0);
                toast_style.setTextSize(20);
                //toast_style.setTextColor(Color.RED);
                break;
            case 1:
                custom_toast=Toast.makeText(context, msg, Toast.LENGTH_LONG);
                toastLayout=(LinearLayout)custom_toast.getView();
                toast_style=(TextView)toastLayout.getChildAt(0);
                toast_style.setTextSize(20);
                //toast_style.setTextColor(Color.RED);
                break;
            default:
                custom_toast=Toast.makeText(context,"",Toast.LENGTH_SHORT);
                break;
        }

        custom_toast.show();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                custom_toast.cancel();
            }
        }, 800);
    }

    public static void whichThreadAmIIN(){
        if(Looper.myLooper()==Looper.getMainLooper()){
            System.out.println("Main Thread!");
        }
        else    System.out.println("Not Main Thread!");
    }

    public static String[] arrayIntegerGenerator(int cont){
        String[] res = new String[cont];
        for(int i = 0; i < cont; i++){
            res[i] = String.valueOf(i);
        }
        return res;
    }

    public static boolean areDrawablesIdentical(Drawable drawableA, Drawable drawableB) {
        Drawable.ConstantState stateA = drawableA.getConstantState();
        Drawable.ConstantState stateB = drawableB.getConstantState();
        // If the constant state is identical, they are using the same drawable resource.
        // However, the opposite is not necessarily true.
        return (stateA != null && stateB != null && stateA.equals(stateB))
                ||  getBitmap(drawableA).sameAs(getBitmap(drawableB));
    }

    public static String convertHexToASCII(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            if(output.equals("00")){    //Gli 00 non vanno salvati

            }
            else{
                //convert hex to decimal
                int decimal = Integer.parseInt(output, 16);
                //convert the decimal to character
                sb.append((char)decimal);

                temp.append(decimal);
            }

        }

        return sb.toString();
    }

    public static Bitmap getBitmap(Drawable drawable) {
        Bitmap result;
        if (drawable instanceof BitmapDrawable) {
            result = ((BitmapDrawable) drawable).getBitmap();
        } else {
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            // Some drawables have no intrinsic width - e.g. solid colours.
            if (width <= 0) {
                width = 1;
            }
            if (height <= 0) {
                height = 1;
            }

            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return result;
    }

    public static String convertHexBytes(byte[] array){ //Converte un array di Byte (Hex) in una String
        StringBuilder sb = new StringBuilder(array.length * 2);
        for(byte b: array)
            sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len=0;
        if(s.length() %2 !=0){
            StringBuilder sb = new StringBuilder();
            sb.insert(0,'0');
            sb.insert(sb.length(),s);
            s = sb.toString();
        }
        len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        //System.out.println(Utility.convertHexBytes(data));
        return data;
    }

    public static String switchBites(String msb)  //da little end a big end,e viceversa
    {
        StringBuilder word2for2 = new StringBuilder();
        int j = 0;
        for(int i = msb.length() - 2; i >= 0; i-=2)
        {
            word2for2.insert(j, msb.charAt(i));
            word2for2.insert(j + 1, msb.charAt(i+1));
            j+=2;
        }
        return word2for2.toString();
    }

    public static String fillWith0(String s,String type)
    {
        //System.out.println("FULL -> "+s);
        StringBuilder sWith0 = new StringBuilder();
        if (type.equals("broadcast"))
        {
            for (int i = 0; i < 8 - s.length(); i++)
            {
                sWith0.insert(i, '0');
            }
            sWith0.insert(sWith0.length(),s);

        }
        else if(type.equals("uri")){
            sWith0.insert(sWith0.length(),s);
            for (int i = sWith0.length(); i < 34 - sWith0.length(); i++)
            {
                sWith0.insert(i, '0');
            }
        }
        return sWith0.toString();
    }

    public static String removeZeros(String s){
        StringBuilder sWitch0 = new StringBuilder();
        for(int i=0;i<=s.length()-1;i++){
            if(s.charAt(i) == '0'){}
            else if(s.charAt(i) != '0'){
                sWitch0.insert(0,s.substring(i,s.length()));    //La fine non è compresa!
                break;
            }

        }
        System.out.println("Hex prima di essere convertito: " + sWitch0.toString());
        return sWitch0.toString();
    }

    public static boolean isHexString(String value) {
        for (int i = 1; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) == -1) return false;
        }
        return true;
    }
    public static boolean isInt(String value){
        try{
            Long.parseLong(value,16);
            return true;
        }
        catch(Exception e){
            return false;
        }
    }

    public static String fromASCIItoHEX(String ascii){
        char[] ch = ascii.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char c : ch) {
            int i = (int) c;
            sb.append(Integer.toHexString(i).toUpperCase());
        }
        return sb.toString();
    }

    public static void hideKeyboard(Context context, View view ) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static String createMac(String s){
        StringBuilder builder = new StringBuilder(s);
        int j = 2;
        for(int i = 2; i < s.length();i+=2){
            builder.insert(j,':');
            j+=3;
        }
        return builder.toString()
                ;    }


}
