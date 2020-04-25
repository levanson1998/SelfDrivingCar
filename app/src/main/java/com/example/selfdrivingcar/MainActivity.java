package com.example.selfdrivingcar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

//import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    TextView viewSpeed, viewStatus, viewTime;
    Button btnStart, btnStop, btnPic;
    Switch swSpeed;
    ImageView viewImg;
    ToggleButton setSpeed;
    String url_heroku = "https://seft-drivingcar.herokuapp.com/";

    private Socket mSocket;
    private Handler customHandler = new Handler();
    NotificationCompat.Builder notification;
    private  static  final int uniqueID=12345;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /* init */
        AnhXa();

        viewStatus.setText("Status: ______");
        viewStatus.setTextColor(Color.rgb(255,255,255));
        viewImg.setVisibility(View.INVISIBLE);
        viewTime.setVisibility(View.INVISIBLE);
        notification=new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);

        /*----WHEN PUSH BUTTON START/STOP ----*/
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Toast.makeText(MainActivity.this, "Start Tracking !", Toast.LENGTH_SHORT).show();

                Context context=view.getContext();
                if (isConnectedToNetwork(context))
                {
                    Connect2Server();
                    mSocket.emit("from-android","start");
                    viewStatus.setText("Status: Starting !");
                    viewStatus.setBackgroundColor(Color.rgb(0,200,0));
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Disconnect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
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
                    mSocket.emit("from-android", "stop");
                    viewStatus.setText("Status: Stopping !");
                    viewStatus.setBackgroundColor(Color.rgb(200, 0, 0));
                    viewImg.setVisibility(View.INVISIBLE);
                    viewTime.setVisibility(View.INVISIBLE);
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Disconnect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
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
                        mSocket.emit("from-android", "speed_fast");
                        Toast.makeText(MainActivity.this, "SPEED IS FAST", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Connect2Server();
                        mSocket.emit("from-android", "speed_slow");
                        Toast.makeText(MainActivity.this, "SPEED IS SLOW", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Disconnect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
                }
            }
        });

        /*WHEN PUSH GET PIC BUTTON*/
        btnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context= view.getContext();
                if (isConnectedToNetwork((context))){
                    Connect2Server();
                    mSocket.emit("from-android", "getpic");
                    mSocket.on("send-img", imgData);
                }
                else {
                    Toast.makeText(MainActivity.this, "Please check network connection !", Toast.LENGTH_SHORT).show();
                    viewStatus.setText("Status: Disconnect !");
                    viewStatus.setBackgroundColor(Color.rgb(255, 193, 7));
                    notifier();
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

    /*----HANDLER FOR UPDATING----*/
    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            // statusData: [Lost, Stop, Running] - [speed]
            mSocket.on("car-status",statusData);
            mSocket.emit("from-android", "request-speed");
//            mSocket.on("get-speed", speedData);
            customHandler.postDelayed(this, 1000);
        }
    };

    private void notifier(){
        Intent intent=new Intent(this, MainActivity.class);
        PendingIntent pendingIntent=PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setSmallIcon(R.drawable.ic_warning_black_24dp);
        notification.setWhen(System.currentTimeMillis());
        notification.setContentTitle("Self Driving Car Lost");
        notification.setContentText("OMG I'm lost. Please find me!!!");
        notification.setCategory(NotificationCompat.CATEGORY_MESSAGE);
        notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        notification.setContentIntent(pendingIntent);
        notification.setDefaults(Notification.DEFAULT_ALL);
        NotificationManager nm= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(uniqueID,notification.build());
    }
/*

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
*/

    private void Connect2Server(){
        try {
            mSocket = IO.socket(url_heroku);
            mSocket.connect();
//            Toast.makeText(this, "Connected to Server!", Toast.LENGTH_SHORT).show();
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

    private Emitter.Listener statusData = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject object = (JSONObject)args[0];
                    String statusCar;
                    String speed;
                    try{
                        statusCar =object.getString("status");
                        speed = object.getString("speed");

                        switch (statusCar){
                            case "Lost":
                                viewStatus.setText("Status: Lost");
                                viewStatus.setBackgroundColor(Color.rgb(241, 191, 41));
                                notifier();
                                break;
                            case "Run":
                                viewStatus.setText("Status: Running");
                                viewStatus.setBackgroundColor(Color.rgb(0, 200, 0));
                                break;
                            case"Stop":
                                viewStatus.setText("Status: Stopping");
                                viewStatus.setBackgroundColor(Color.rgb(200, 0, 0));
                                viewImg.setVisibility(View.INVISIBLE);
                                viewTime.setVisibility(View.INVISIBLE);
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
                        viewTime.setText("Captured Time: " +captime);
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

    }



