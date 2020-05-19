package com.example.selfdrivingcar;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    TextView viewSpeed, viewStatus, viewTime;
    Button btnStart, btnStop, btnPic;
    Switch swSpeed;
    ImageView viewImg;
    ToggleButton setSpeed;
    String url_heroku = "https://ha-drivingcar.herokuapp.com/";

    private Socket mSocket;
    private Handler customHandler = new Handler();
    NotificationCompat.Builder builder;
    private static final int ID_NOTIFICATION_BROADCAST = 607;
    private final String channelID = "ChannelID_01";
    private boolean onetime = true;

    /*--- NOTIFICATION --*/
    private static final String CHANNEL_WHATEVER="channel_whatever";
    private static final int NOTIFY_ID=1337;
    private static final String GROUP_SAMPLE="sampleGroup";
    private NotificationManagerCompat mgrCompat=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* INIT */
        AnhXa();
        Connect2Server();

        /*START NOTIFY INIT*/
        NotificationManager mgr=
                (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O &&
                mgr.getNotificationChannel(CHANNEL_WHATEVER)==null) {
            mgr.createNotificationChannel(new NotificationChannel(CHANNEL_WHATEVER,
                    "Whatever", NotificationManager.IMPORTANCE_DEFAULT));
        }

        mgrCompat=NotificationManagerCompat.from(this);
        /*END NOTIFY */

        /*--- setVisibility ---*/
        viewStatus.setText("Status: ______");
        viewStatus.setTextColor(Color.rgb(255,255,255));
        viewImg.setVisibility(View.INVISIBLE);
        viewTime.setVisibility(View.INVISIBLE);

        /* --- THREAD --- */
    Thread thread = new Thread(){
        public void run(){
            mSocket.on("car-status",statusData);
            mSocket.on("car-disconnect", disconnectData);

            customHandler.postDelayed(this, 5000);
        }
    };
    thread.start();

        /*----WHEN PUSH BUTTON START ----*/
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Start Tracking !", Toast.LENGTH_SHORT).show();
                Context context=view.getContext();
                if (isConnectedToNetwork(context))
                {
                    Log.d("test", "startClick");

                    Connect2Server();

                    mSocket.emit("from-android","start");
                    viewStatus.setText("Status: Start");
                    viewStatus.setBackgroundColor(Color.rgb(0,200,0));
                    Log.d("test", "btnStart");
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Not connect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
                }
            }
        });

        /*----WHEN PUSH BUTTON STOP ----*/
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context=view.getContext();
                if (isConnectedToNetwork(context)) {
                    mSocket.emit("from-android", "stop");
                    viewStatus.setText("Status: Stop");
                    viewStatus.setBackgroundColor(Color.rgb(200, 0, 0));
                    viewImg.setVisibility(View.INVISIBLE);
                    viewTime.setVisibility(View.INVISIBLE);
                    mSocket.disconnect();
                    onetime=true;
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Not connect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
                }
                }
        });

        /*----WHEN PUSH ToggleButton SLOW/FAST----*/
        setSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context=view.getContext();
                if (isConnectedToNetwork(context)) {
                    if(setSpeed.isChecked()) {
                        mSocket.emit("from-android", "speed_fast");
                        Toast.makeText(MainActivity.this, "SPEED IS FAST", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        mSocket.emit("from-android", "speed_slow");
                        Toast.makeText(MainActivity.this, "SPEED IS SLOW", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Not connect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
                }
            }
        });

        /*--- WHEN PUSH GET PIC BUTTON ---*/
        btnPic.setOnClickListener(new View.OnClickListener
                () {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
                Context context= view.getContext();
                if (isConnectedToNetwork((context))){
                    mSocket.emit("from-android", "getpic");
                    mSocket.on("send-img", imgData);
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Not connect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
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

// onetime:true: not connect, false: connected
    private void Connect2Server(){
        try {
            if(onetime==true) {
                onetime=false;
                mSocket = IO.socket(url_heroku);
                mSocket.connect();
                mSocket.emit("android-connect", true);
            }
            else{
                Log.d("test", " don't connect2server");
            }
//            Toast.makeText(this, "Connected to Server!", Toast.LENGTH_SHORT).show();
        } catch (URISyntaxException e) {
            Toast.makeText(this, "Server fails to start...", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /*---- CHECK NETWORK CONNECTION ----*/
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

    private Emitter.Listener statusData = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    String statusCar;
                    Integer ReStop;
                    String speed;
                    String img_text, captime;
                    try{
                        // {"status":"lost", "speed":"123"}
                        statusCar =object.getString("status");
                        ReStop = object.getInt("ReStop");
                        speed = object.getString("speed");
                        img_text = object.getString("Image");
                        captime = object.getString("CapTime");



                        switch (statusCar){
                            case "Lost":
                                viewStatus.setText("Status: Lost !");
                                viewStatus.setBackgroundColor(Color.rgb(241, 191, 41));
                                viewTime.setText(captime);
                                String encodedString=img_text.substring(img_text.indexOf(",")+1,img_text.length());
                                byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                viewImg.setImageBitmap(decodedByte);

                                viewImg.setVisibility(View.VISIBLE);
                                viewTime.setVisibility(View.VISIBLE);

                                showNotificationLost();
                                break;
                            case "Run":
                                viewStatus.setText("Status: Running !");
                                viewStatus.setBackgroundColor(Color.rgb(0, 200, 0));
//                                viewImg.setVisibility(View.INVISIBLE);
//                                viewTime.setVisibility(View.INVISIBLE);
                                break;
                            case "Stop":
                                Log.d("ReS", "value: "+Integer.toString(ReStop));
                                // vat can
                                if(ReStop==1){
                                    viewStatus.setText("Have Obstacle!");
                                }
                                // bien bao
                                else if(ReStop==2){
                                    viewStatus.setText("Signal Stop!");
                                }
                                else{
                                    viewStatus.setText("Status: Stopping !");
                                }
                                // viewStatus.setText("Status: Stopping !");
                                viewStatus.setBackgroundColor(Color.rgb(200, 0, 0));
                                viewTime.setText(captime);
                                String encodedString1=img_text.substring(img_text.indexOf(",")+1,img_text.length());
                                byte[] decodedString1 = Base64.decode(encodedString1, Base64.DEFAULT);
                                Bitmap decodedByte1 = BitmapFactory.decodeByteArray(decodedString1, 0, decodedString1.length);
                                viewImg.setImageBitmap(decodedByte1);
                                viewImg.setVisibility(View.VISIBLE);
                                viewTime.setVisibility(View.VISIBLE);

                                break;
                        }
                        viewSpeed.setText("Speed: "+speed);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            });
        }
    };

    private Emitter.Listener imgData = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject) args[0];
                    String img_text, captime;
                    try {
                        img_text = object.getString("Image");
                        captime=object.getString("CapTime");
                        viewTime.setText(captime);
                        String encodedString=img_text.substring(img_text.indexOf(",")+1,img_text.length());
                        byte[] decodedString = Base64.decode(encodedString, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        viewImg.setImageBitmap(decodedByte);

                        viewImg.setVisibility(View.VISIBLE);
                        viewTime.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    private  Emitter.Listener disconnectData = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    viewStatus.setText("Car disconnect !");
                }
            });
        }
    };
/*  NOTIFICATION  */
    private NotificationCompat.Builder buildNormal() {
        NotificationCompat.Builder b=
                new NotificationCompat.Builder(this, CHANNEL_WHATEVER);

        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle("download completed !")
                .setContentText("ok download !")
                .setContentIntent(buildPendingIntent())
                .setSmallIcon(R.drawable.ic_warning_black_24dp)
                .setGroup(GROUP_SAMPLE)
                .setGroupSummary(true);

        return(b);
    }

    private void showNotificationLost() {
        NotificationCompat.Builder b=
                new NotificationCompat.Builder(this, CHANNEL_WHATEVER);
        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentTitle("CAR LOST")
                .setContentText("I'm lost !!!")
                .setContentIntent(buildPendingIntent())
                .setSmallIcon(R.drawable.ic_warning_black_24dp)
                .setGroup(GROUP_SAMPLE);
        mgrCompat.notify(NOTIFY_ID, b.build());
    }

    private PendingIntent buildPendingIntent() {
        Intent i=new Intent(this, MainActivity.class);
        return(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
    }
}





