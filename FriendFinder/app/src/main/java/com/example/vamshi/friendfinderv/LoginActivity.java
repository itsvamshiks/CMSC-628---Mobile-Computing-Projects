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


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    EditText login_emailtxt,login_passwordtxt;
    Button loginConfirm;
    String BASE_URL = "";
    private Context currContext;
    private static final String LOGIN_URL = "login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        currContext = this;

        login_emailtxt = findViewById(R.id.login_emailtext);
        login_passwordtxt = findViewById(R.id.login_passwordtxt);
        loginConfirm = findViewById(R.id.LoginConfirmButton);

        BASE_URL = getString(R.string.baseurl);
        loginConfirm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        final String login_email = login_emailtxt.getText().toString().trim().toLowerCase();
        final String login_password = login_passwordtxt.getText().toString().trim().toLowerCase();
        if (login_email.equalsIgnoreCase("")){
            login_emailtxt.setError("Enter Valid Email");
        }
        if (login_password.equalsIgnoreCase("")){
            login_passwordtxt.setError("Enter Password");
        }
        if (!login_email.equalsIgnoreCase("") && !login_password.equalsIgnoreCase("")) {
            Log.d("DEBUG", "login successful");
            String[] userDetails = new String[2];
            userDetails[0] = login_email;
            userDetails[1] = login_password;
            LoginUser loginTask = new LoginUser();
            loginTask.execute(userDetails);
        }else
        {
            Log.w("DEBUG", "login failed");
            Toast.makeText(LoginActivity.this, "Login Failed",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private class LoginUser extends AsyncTask<String, Integer, String> {


        @Override
        protected String doInBackground(String... strings) {
            URL url;
            String response = "";

            try{
                url = new URL(BASE_URL+LOGIN_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                String requestJsonString = new JSONObject().put("email", strings[0]).put("password", strings[1]).toString();
                Log.d("REQUEST : ", requestJsonString);
                writer.write(requestJsonString);

                writer.flush();
                writer.close();
                os.close();
                int responseCode = conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    line = br.readLine();
                    while (line != null) {
                        response += line;
                        line = br.readLine();
                    }

                    br.close();
                }
                Log.d("RESPONSE: ", response);
                conn.disconnect();


            } catch (IOException | JSONException e){

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
                    case "1":
                        Log.d("LOGIN","SUCCESSFUL LOGIN");
                        SharedPreferences details = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        SharedPreferences.Editor e = details.edit();

                        e.putString("email", String.valueOf(login_emailtxt.getText().toString()));
                        e.apply();

                        //redirect to maps activity
                        Intent i = new Intent(LoginActivity.this, MapsActivity.class);
                        i.putExtra("email", String.valueOf(login_emailtxt.getText()));
                        startActivity(i);
                        break;

                    case "0":
                        Toast.makeText(currContext,"INVALID USER DETAILS",Toast.LENGTH_LONG).show();
                        break;

                    default:
                        Toast.makeText(currContext,"UNKNOWN SERVER RESPONSE",Toast.LENGTH_LONG).show();
                        break;
                }


            }


        }
    }

}

