package ir.we.diagram;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public class IntroActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText phoneInput = null;

    private TextView errorText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        phoneInput = findViewById(R.id.phone);
        findViewById(R.id.next).setOnClickListener(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        phoneInput.setText(savedInstanceState.getString("phone"));
        if (savedInstanceState.containsKey("error"))
            errorText.setText(savedInstanceState.getString("error"));
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString("phone", phoneInput.getText().toString());
        if (errorText.getAlpha() == 1)
            outState.putString("error", errorText.getText().toString());
    }

    @Override
    public void onClick(View v) {
        onPhoneEntered(phoneInput.getText().toString());
    }

    void showError(String error) {
        errorText.setText(error);
        errorText.animate().alpha(1).start();
    }

    void clearError() {
        errorText.animate().alpha(0).start();
    }

    void goToEnterCodePage() {
        // TODO
        /*
        Intent intent = new Intent(String.format("diag://entercode?hash=%s", hash))
        startActivity(intent)
         */
    }

    TLRPC.TL_auth_checkPhone req = new TLRPC.TL_auth_checkPhone();

    private void onPhoneEntered(String s) {
        if(s.equals("") &&  s.length()==13){

            ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
                @Override
                public void run(TLObject response, TLRPC.TL_error error){
                    // System.out.println("simplyyyyyy");
                    Log.d("myTag", "simplyyyyy");

                    if(error!=null){
                        showError("input number doesn't exists on Telegram");
                    }

                    else{
                        //call sendCode
                    }
                }
            },ConnectionsManager.RequestFlagWithoutLogin);
        }
        showError("invalid input");
    }

/*void showError(String error) {
        errorText.setText(error);
        errorText.animate().alpha(1).start();
    }

    */

    //boolean checkNumber telegram
    //sendCode telegram
    //giveInsertedCodeFromUser
    //send code to telegram
    //give telegram response (check if the code is correct or wrong)
    //show the error wrong code if it is wrong
    //add to account telegram

/*    void submitPhoneNumber (String phoneNumber) {
        if(phoneNumber!=null){
            //shomare ro midi telegram javab mide

            if(javab==1){
                next page
            }
            else
            //print number doesn't exist
        }
        //print wrong format/number

    }
*/

}
