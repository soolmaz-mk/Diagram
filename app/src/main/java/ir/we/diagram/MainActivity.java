package ir.we.diagram;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

public class MainActivity extends AppCompatActivity {

    //private native void sayHello();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TLRPC.TL_auth_checkPhone req = new TLRPC.TL_auth_checkPhone();
        req.phone_number = "+989172213026";
        

        ConnectionsManager.getInstance().sendRequest(req, new RequestDelegate() {
            @Override
            public void run(TLObject response, TLRPC.TL_error error){
               // System.out.println("simplyyyyyy");
                Log.d("myTag", "simplyyyyy");
            }
        },1);
    }
}

        //boolean checkNumber telegram
        //sendCode telegram
        //giveInsertedCodeFromUser
        //send code to telegram
        //give telegram response (check if the code is correct or wrong)
        //show the error wrong code if it is wrong
        //add to account telegram

        void submitPhoneNumber (String phoneNumber) {
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


