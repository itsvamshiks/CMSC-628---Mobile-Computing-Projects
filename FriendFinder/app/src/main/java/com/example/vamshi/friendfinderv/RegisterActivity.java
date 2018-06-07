package com.example.vamshi.friendfinderv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;


public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    EditText fullNameTextBox, reg_emailTextBox, passwordTextBox;
    Button registerButton;
    private static final String TAG = "RegisterActivity";
    String BASE_URL = "";
    private static final String REGISTER_URL = "register.php";
    private Context currContext;


    @Override
    protected void
    onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        currContext = this;
        BASE_URL = getString(R.string.baseurl);
        Log.d("REQUEST",BASE_URL);
        fullNameTextBox = (EditText) findViewById(R.id.fullnametext);
        reg_emailTextBox = (EditText) findViewById(R.id.register_emailtext);
        passwordTextBox = (EditText) findViewById(R.id.passwordtxt);
        registerButton = (Button) findViewById(R.id.RegisterButton);
        registerButton.setOnClickListener(this);


    }

    @Override
    public void onClick(View v) {

        final String fullName = fullNameTextBox.getText().toString().trim().toLowerCase();
        final String password = passwordTextBox.getText().toString().trim().toLowerCase();
        final String reg_email = reg_emailTextBox.getText().toString().trim().toLowerCase();
        if (reg_email.equalsIgnoreCase("")){
            reg_emailTextBox.setError("Invalid Email");
        }
        if (password.equalsIgnoreCase("")){
            passwordTextBox.setError("Invalid Password");
        }
        if (fullName.equalsIgnoreCase("")){
            fullNameTextBox.setError("Invalid Name");
        }

        if (!reg_email.equalsIgnoreCase("") && !password.equalsIgnoreCase("") && !fullName.equalsIgnoreCase("")) {
            Log.d(TAG, "registration successful");
            String[] details = new String[3];
            details[0] = reg_email;
            details[1] = fullName;
            details[2] = password;
            Log.d("REQUEST",details[0]+details[1]+details[2]);
            RegisterUser registerTask = new RegisterUser();
            registerTask.execute(details);
        }else
        {
            Log.w(TAG, "registration failed");
            Toast.makeText(RegisterActivity.this, "Authentication failed.",
                    Toast.LENGTH_SHORT).show();
        }


    }

    private class RegisterUser extends AsyncTask<String, Integer, String>{

        @Override
        protected String doInBackground(String... strings) {

            URL url;
            String response = "";
            try {

                url = new URL(BASE_URL + REGISTER_URL);
                Log.d("REQUEST",url.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                String requestJsonString = new JSONObject()
                        .put("email", strings[0])
                        .put("full_name", strings[1])
                        .put("password", strings[2])
                        .toString();
                Log.d("REQUEST : ", requestJsonString);
                writer.write(requestJsonString);

                writer.flush();
                writer.close();
                os.close();
                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    line = br.readLine();
                    while(line != null){
                        response += line;
                        line = br.readLine();
                    }

                    br.close();
                }
                Log.d("REQUEST", response);
                conn.disconnect();

            }catch (IOException | JSONException e)
            {
                e.printStackTrace();
            }
            Log.d("RESPONSE:", response);


                return response;


         }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s!=null && !s.isEmpty()){
                switch (s){
                    case "0":
                        Toast.makeText(currContext,"COULD NOT REGISTER USER",Toast.LENGTH_LONG).show();
                        break;

                    case "1":
                        Toast.makeText(currContext,"USER REGISTERED",Toast.LENGTH_SHORT).show();
                        SharedPreferences preferenceDetails = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor e = preferenceDetails.edit();

                        e.putString("email", String.valueOf(reg_emailTextBox.getText()));
                        e.apply();

                        //redirect to login activity
                        Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                        i.putExtra("email", String.valueOf(reg_emailTextBox.getText()));
                        startActivity(i);
                        break;

                    case "2":
                        Toast.makeText(currContext,"USER ALREADY EXISTS",Toast.LENGTH_LONG).show();
                        break;

                }
            }


        }
    }
}


