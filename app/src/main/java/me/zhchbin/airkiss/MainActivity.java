package me.zhchbin.airkiss;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class MainActivity extends ActionBarActivity {
    private EditText mSSIDEditText;
    private EditText mPasswordEditText;
    private boolean stopConfig = false;
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

        new AirKissTask(this, new AirKissEncoder(ssid, password)).execute();
    }

    private class AirKissTask extends AsyncTask<Void, Void, Void> implements DialogInterface.OnDismissListener {
        private final byte DUMMY_DATA[] = new byte[1500];
        private static final int REPLY_BYTE_CONFIRM_TIMES = 5;
        private static final int PORT = 10000;

        private ProgressDialog mDialog;
        private Context mContext;
        private DatagramSocket mSocket;

        private char mRandomChar;
        private AirKissEncoder mAirKissEncoder;

        private volatile boolean mDone = false;

        public AirKissTask(ActionBarActivity activity, AirKissEncoder encoder) {
            mContext = activity;
            mDialog = new ProgressDialog(mContext);
            mDialog.setOnDismissListener(this);
            mRandomChar = encoder.getRandomChar();
            mAirKissEncoder = encoder;
        }

        @Override
        protected void onPreExecute() {
            this.mDialog.setMessage("Connecting :)");
            this.mDialog.show();

            new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[15000];
                    try {

                        DatagramSocket udpServerSocket = new DatagramSocket(PORT);
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        int replyByteCounter = 0;
                        udpServerSocket.setSoTimeout(60*1000);
                        while (!stopConfig) {
                            if (getStatus() == Status.FINISHED)
                                break;

                            try {
                                udpServerSocket.receive(packet);
                                //Log.d("run: ","端口"+PORT+"收到消息");
                                byte receivedData[] = packet.getData();
                                for (byte b : receivedData) {
                                    if (b == mRandomChar){
                                        replyByteCounter++;
                                        Log.w(" receivedData[]",  "packet.getData()----b == mRandomChar----"+b);
                                    }
                                }
                                Log.e(" receivedData[]", "receivedData---replyByteCounter ====" + replyByteCounter);
                                Log.e(" receivedData[]", "receivedData---packet.getLength() ====" + packet.getLength());

                                if (replyByteCounter >= REPLY_BYTE_CONFIRM_TIMES) {
                                    Log.d("onPreExecute","线程读到的包大于等于5个了");
                                    mDone = true;
                                    break;
                                }
                            } catch (SocketTimeoutException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("onPreExecute","线程结束 udpServerSocket.close");
                        udpServerSocket.close();
                    } catch (SocketException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }



        @Override
        protected Void doInBackground(Void... params) {
            try {
                mSocket = new DatagramSocket();
                mSocket.setBroadcast(true);
            } catch (Exception e) {
                e.printStackTrace();
            }

            int encoded_data[] = mAirKissEncoder.getEncodedData();
            for (int i = 0; i < encoded_data.length; ++i) {
                sendPacketAndSleep(encoded_data[i]);
                if (i % 200 == 0) {
                    if (isCancelled() || mDone){
                        Log.d("doInBackground","isCancelled()="+isCancelled());
                        return null;
                    }
                }
            }
            Log.d("doInBackground","encoded_data遍历完成");

            return null;
        }

        private void sendPacketAndSleep(int length) {


            //这个端口为随机端口
            //如果没有socket监听该端口，会有异常at libcore.io.IoBridge.maybeThrowAfterRecvfrom
            //往10000端口发的话，不会有异常
            //虽然有异常,但不影响配置功能
            int port_send = 10000;
            try {
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                        length,
                        InetAddress.getByName("255.255.255.255"),
                        port_send);
                mSocket.send(pkg);
                Thread.sleep(2);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled(Void params) {
            Toast.makeText(getApplicationContext(), "Air Kiss Cancelled.", Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void params) {
            if (mDialog.isShowing()) {
                mDialog.dismiss();
            }

            String result;
            if (mDone) {
                result = "Air Kiss Successfully Done!";
            } else {
                result = "Air Kiss Timeout.";
            }
            stopConfig = true;
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (mDone)
                return;

            this.cancel(true);
        }
    }
}
