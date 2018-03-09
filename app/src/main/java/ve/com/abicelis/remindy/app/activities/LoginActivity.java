package ve.com.abicelis.remindy.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.database.RemindyDAO;

public class LoginActivity extends AppCompatActivity {

    private EditText txtUserName;
    private EditText txtPassword;
    private RemindyDAO dao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        txtUserName = (EditText) findViewById(R.id.txtUserName);
        txtPassword = (EditText) findViewById(R.id.txtPassword);

        dao = new RemindyDAO(getApplicationContext());

        findViewById(R.id.btnSignUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnSignIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!TextUtils.isEmpty(txtUserName.getText().toString()) && !TextUtils.isEmpty(txtPassword.getText().toString())) {
                    if (dao.loginUser(txtUserName.getText().toString(), txtPassword.getText().toString())) {
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        startActivity(intent);
                    } else {
                        show("Email not match");
                    }
                } else {
                    show("Enter username & password");
                }

            }
        });
    }

    private void show(String message) {
        Snackbar.make(txtUserName, message, Snackbar.LENGTH_LONG).show();
    }


}
