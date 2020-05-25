package com.example.selfdrivingcar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText Name;
    private EditText Password;
    private TextView Info;
    private Button Login;
    private int Counter = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        Name = (EditText) findViewById(R.id.etName);
        Password = (EditText) findViewById(R.id.etPassword);
        Info = (TextView) findViewById(R.id.tvInfo);
        Login = (Button) findViewById(R.id.btnLogin);

        Info.setText("Please Login To Use Application!!! 5");

        Login.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
            validate(Name.getText().toString(),Password.getText().toString());
            }
        });

    }


    private void validate(String userName, String userPassword) {
        if ((userName == "Ha Xinh Dep") && (userPassword == "1234")) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
        } else {
            Counter--;

            Info.setText("Please Login To Use Application!!!" + String.valueOf(Counter));
            if (Counter == 0) {
                Login.setEnabled(false);
            }
        }

    }
}