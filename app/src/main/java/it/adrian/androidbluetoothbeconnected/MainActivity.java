package it.adrian.androidbluetoothbeconnected;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = "MainActivity";

    BluetoothAdapter bluetoothAdapter;

    private UUID myUUID;
    private String myName;

    TextView textInfo, textStatus;
    Button startButton;
    Button stopButton;
    EditText errorCode;

    ThreadBeConnected myThreadBeConnected;
    DataOutputStream os;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textInfo = (TextView)findViewById(R.id.info);
        textStatus = (TextView)findViewById(R.id.status);
        startButton = (Button) findViewById(R.id.start_button);
        errorCode = (EditText) findViewById(R.id.error_code);
        stopButton = (Button) findViewById(R.id.stop_button);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)){
            Toast.makeText(this,
                    "FEATURE_BLUETOOTH NOT support",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //generate UUID on web: http://www.famkruithof.net/uuid/uuidgen
        //have to match the UUID on the another device of the BT connection
        myUUID = UUID.fromString("ec79da00-853f-11e4-b4a9-0800200c9a66");
        myName = myUUID.toString();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String stInfo = bluetoothAdapter.getName() + "\n" +
                bluetoothAdapter.getAddress();
        textInfo.setText(stInfo);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Turn ON BlueTooth if it is OFF
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }

                setup();
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // STOP bluetooth connection
                if(myThreadBeConnected!=null){
                    myThreadBeConnected.cancel();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void setup() {
        textStatus.setText("setup()");
        myThreadBeConnected = new ThreadBeConnected();
        myThreadBeConnected.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadBeConnected!=null){
            myThreadBeConnected.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                setup();
            }else{
                Toast.makeText(this, "BlueTooth NOT enabled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    private class ThreadBeConnected extends Thread {

        private BluetoothServerSocket bluetoothServerSocket = null;
        private InputStream mmInStream;
        private OutputStream mmOutStream;

        public ThreadBeConnected() {
            try {
                bluetoothServerSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(myName, myUUID);

                textStatus.setText("Waiting\n"
                        + "bluetoothServerSocket :\n"
                        + bluetoothServerSocket);

            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);

            }
        }

        @Override
        public void run() {
            BluetoothSocket bluetoothSocket = null;

            if(bluetoothServerSocket!=null){
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();

                    BluetoothDevice remoteDevice = bluetoothSocket.getRemoteDevice();

                    final String strConnected = "Connected:\n" +
                            remoteDevice.getName() + "\n" +
                            remoteDevice.getAddress();

                    InputStream tmpIn = null;
                    OutputStream tmpOut = null;

                    // Get the BluetoothSocket input and output streams
                    try {
                        tmpIn = bluetoothSocket.getInputStream();
                        tmpOut = bluetoothSocket.getOutputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    mmInStream = tmpIn;
                    mmOutStream = tmpOut;

                    try {
                        mmOutStream.write(errorCode.getText().toString().getBytes());
                        //send what is already in buffer
                        mmOutStream.flush();
                        Log.d(TAG, "message written");
                    }catch (IOException e){
                        Log.e(TAG, "Exception during write", e);
                    }



                    //connected
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText(strConnected);
                        }});

                } catch (Exception e) {
                    Log.e(TAG, "Socket's accept() method failed", e);


                    final String eMessage = e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            textStatus.setText("something wrong: \n" + eMessage);
                        }});
                }
            }else{
                runOnUiThread(new Runnable(){

                    @Override
                    public void run() {
                        textStatus.setText("bluetoothServerSocket == null");
                    }});
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "close bluetoothServerSocket",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                //send what is already in buffer
                mmOutStream.flush();

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
    }
}