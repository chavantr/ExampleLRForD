package ve.com.abicelis.remindy.app.activities;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import ve.com.abicelis.remindy.R;
import ve.com.abicelis.remindy.database.RemindyDAO;
import ve.com.abicelis.remindy.model.User;

public class RegistrationActivity extends AppCompatActivity {


    private EditText txtName;
    private EditText txtEmail;
    private EditText txtPhone;
    private EditText txtPassword;
    private RemindyDAO dao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        txtName = (EditText) findViewById(R.id.txtName);
        txtEmail = (EditText) findViewById(R.id.txtEmail);
        txtPhone = (EditText) findViewById(R.id.txtPhone);
        txtPassword = (EditText) findViewById(R.id.txtPassword);
        dao = new RemindyDAO(getApplicationContext());

        findViewById(R.id.btnSignUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {

                    User user = new User();

                    user.setName(txtName.getText().toString());
                    user.setEmail(txtEmail.getText().toString());
                    user.setPhone(txtPhone.getText().toString());
                    user.setPassword(txtPassword.getText().toString());
                    if (dao.insertUser(user) > 0) {
                        finish();
                    }
                } else {
                    show();
                }
            }
        });
    }

    private boolean validate() {

        if (!TextUtils.isEmpty(getText(txtName)) && !TextUtils.isEmpty(getText(txtEmail)) && !TextUtils.isEmpty(getText(txtPhone)) && !TextUtils.isEmpty(getText(txtPassword))) {
            return true;
        }

        return false;
    }

    private String getText(EditText txtView) {
        return txtView.getText().toString();
    }

    private void show() {
        Snackbar.make(txtName, "All fields(*) required.", Snackbar.LENGTH_LONG).show();
    }


}
