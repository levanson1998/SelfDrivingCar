package com.example.selfdrivingcar;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

//import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    TextView viewSpeed, viewStatus, viewTime;
    Button btnStart, btnStop, btnPic;
    Switch swSpeed;
    ImageView viewImg;
    ToggleButton setSpeed;
    String url_heroku = "https://seft-drivingcar.herokuapp.com/";

    private Socket mSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnhXa();

        /* init */
        // bắt đầu chỗ listen envent đi setOnClickListener
        /*----WHEN PUSH BUTTON START/STOP ----*/
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Start Tracking !", Toast.LENGTH_SHORT).show();

                Context context=view.getContext();
                if (isConnectedToNetwork(context))
                {
                    Connect2Server();
                    mSocket.emit("android-on",true);
                    viewStatus.setText("Status: Starting !");
                    viewStatus.setBackgroundColor(Color.rgb(0,200,0));
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Stop Tracking !", Toast.LENGTH_SHORT).show();

                // socketio send to server
                Context context=view.getContext();
                if (isConnectedToNetwork(context)) {
                    Connect2Server();
                    mSocket.emit("android-on", false);
                    viewStatus.setText("Status: Stopping !");
                    viewStatus.setBackgroundColor(Color.rgb(200, 0, 0));
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                }
                }

        });

        /*----WHEN PUSH ToggleButton SLOW-FAST----*/
        setSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context=view.getContext();
                if (isConnectedToNetwork(context)) {
                    if(setSpeed.isChecked()) {
                        Connect2Server();
                        mSocket.emit("android-on", "speed_fast");
                        Toast.makeText(MainActivity.this, "SPEED IS FAST", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Connect2Server();
                        mSocket.emit("android-on", "speed_slow");
                        Toast.makeText(MainActivity.this, "SPEED IS SLOW", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void AnhXa()
    {
        btnStart = findViewById(R.id.btnstart);
        btnStop = findViewById(R.id.btnstop);
        viewSpeed = findViewById(R.id.txtspeed);
        btnPic = findViewById(R.id.btnpicture);
        viewStatus = findViewById(R.id.txtviewstatus);
        viewImg = findViewById(R.id.imgview);
        viewTime = findViewById(R.id.txttime);
        setSpeed = findViewById(R.id.slowfast);
    }

    private void Connect2Server(){
        try {
            mSocket = IO.socket(url_heroku);
            mSocket.connect();
            Toast.makeText(this, "Connected to Server!", Toast.LENGTH_SHORT).show();
        } catch (URISyntaxException e) {
            Toast.makeText(this, "Server fails to start...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /*----CHECK NETWORK CONNECTION----*/
    public static boolean isConnectedToNetwork(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isConnected = false;
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            isConnected = (activeNetwork != null) && (activeNetwork.isConnectedOrConnecting());
        }

        return isConnected;
    }

    /*---SET SPEED SLOW-FAST*/

}


