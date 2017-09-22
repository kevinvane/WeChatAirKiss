package me.zhchbin.airkiss;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity implements AirKissCallBack{

    private final String TAG = getClass().getSimpleName();
    private EditText mSSIDEditText;
    private EditText mPasswordEditText;
    private AirKissConfig airKissConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSSIDEditText = (EditText)findViewById(R.id.ssidEditText);
        mPasswordEditText = (EditText)findViewById(R.id.passwordEditText);

        Context context = getApplicationContext();
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                String ssid = connectionInfo.getSSID();
                if (Build.VERSION.SDK_INT >= 17 && ssid.startsWith("\"") && ssid.endsWith("\""))
                    ssid = ssid.replaceAll("^\"|\"$", "");
                mSSIDEditText.setText(ssid);
                mSSIDEditText.setEnabled(false);
            }
        }
    }

    public void onConnectBtnClick(View view) {

        String ssid = mSSIDEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        if (ssid.isEmpty() || password.isEmpty()) {
            Context context = getApplicationContext();
            CharSequence text = "Please input ssid and password.";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }
        airKissConfig = new AirKissConfig(this);
        airKissConfig.execute(ssid, password);
        //new AirKissDiscover().execute();
    }

    @Override
    public void airKissConfigSuccess() {
        Log.i(TAG, "airKissConfigSuccess: ");
    }

    @Override
    public void airKissConfigFail() {
        Log.e(TAG, "airKissConfigFail: ");

    }

    @Override
    public void airKissConfigTimeOut() {
        Log.e(TAG, "airKissConfigTimeOut: ");

    }


//    private class AirKissTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnDismissListener {
//
//        private ProgressDialog mDialog;
//        private Context mContext;
//
//
//        public AirKissTask(ActionBarActivity activity, AirKissEncoder encoder) {
//            mContext = activity;
//            mDialog = new ProgressDialog(mContext);
//            mDialog.setOnDismissListener(this);
//
//        }
//
//        @Override
//        protected void onPreExecute() {
//            this.mDialog.setMessage("Connecting :)");
//            this.mDialog.show();
//
//
//        }
//
//
//
//        @Override
//        protected Void doInBackground(Void... params) {
//
//
//            return null;
//        }
//
//
//
//        @Override
//        protected void onCancelled(Void params) {
//            Toast.makeText(getApplicationContext(), "Air Kiss Cancelled.", Toast.LENGTH_LONG).show();
//        }
//
//        @Override
//        protected void onPostExecute(Void params) {
//            if (mDialog.isShowing()) {
//                mDialog.dismiss();
//            }
//
//            String result;
//            if (mDone) {
//                result = "Air Kiss Successfully Done!";
//            } else {
//                result = "Air Kiss Timeout.";
//            }
//            stopConfig = true;
//            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
//        }
//
//        @Override
//        public void onDismiss(DialogInterface dialog) {
//            if (mDone)
//                return;
//
//            this.cancel(true);
//        }
//    }
}
