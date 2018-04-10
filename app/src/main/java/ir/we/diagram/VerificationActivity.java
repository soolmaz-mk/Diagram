package ir.we.diagram;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class VerificationActivity extends AppCompatActivity {

    private TextView errorText = null;

    private EditText codeInput = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        errorText = findViewById(R.id.error);
        codeInput = findViewById(R.id.code);
        findViewById(R.id.go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyCode(codeInput.getText().toString());
            }
        });
    }

    void showError(String error) {
        errorText.setText(error);
        errorText.animate().alpha(1).start();
    }

    void clearError() {
        errorText.animate().alpha(0).start();
    }


    // Controllers

    private void verifyCode(String s) {
    }
}
