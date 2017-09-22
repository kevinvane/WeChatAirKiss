package me.zhchbin.airkiss;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by led on 2017/9/22.
 */

public class AirKissConfig {

    private final String TAG = getClass().getSimpleName();

    //这个是空的数据，数据载体是lenght
    private final byte DUMMY_DATA[] = new byte[1500];
    //收到多少个回复包才认为是配置成功
    private static final int REPLY_BYTE_CONFIRM_TIMES = 20;
    //接收回复包的端口
    private static final int PORT = 10000;

    private char mRandomChar;
    private AirKissEncoder mAirKissEncoder;
    private volatile boolean mDone = false;
    private boolean isCancelled = false;
    private boolean stopConfig;
    private boolean isReadThreadRunning = false;

    private AirKissCallBack airKissCallBack;

    public AirKissConfig(AirKissCallBack airKissCallBack) {

        this.airKissCallBack = airKissCallBack;
    }

    public void execute(String ssid, String password){

        mAirKissEncoder = new AirKissEncoder(ssid, password);
        mRandomChar = mAirKissEncoder.getRandomChar();
        if(!isReadThreadRunning){
            isReadThreadRunning = true;
            new ReceiveThread().start();
        }
        new SendThread().start();
    }
    public void cancel(){

        isCancelled = true;
    }
    public void release(){

        stopConfig = true;
    }



    private class ReceiveThread extends Thread{

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[15000];
            DatagramSocket udpServerSocket = null;
            try {
                udpServerSocket = new DatagramSocket(PORT);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                int replyByteCounter = 0;
                udpServerSocket.setSoTimeout(60*1000);
                Log.d(TAG, "ReceiveThread: 开始监听"+PORT);
                Log.d(TAG, "ReceiveThread: mRandomChar="+Integer.valueOf(mRandomChar));

                while (!stopConfig) {

                    try {
                        udpServerSocket.receive(packet);
                        //Log.d("run: ","端口"+PORT+"收到消息");
                        byte receivedData[] = packet.getData();
                        for (byte b : receivedData) {
                            if (b == mRandomChar){
                                replyByteCounter++;
                                Log.i(" receivedData[]",  "packet.getData()----b == mRandomChar----"+b);
                            }
                        }
                        //Log.d(" receivedData[]", "receivedData---replyByteCounter ====" + replyByteCounter);
                        //Log.d(" receivedData[]", "receivedData---packet.getLength() ====" + packet.getLength());

                        if (replyByteCounter >= REPLY_BYTE_CONFIRM_TIMES) {
                            Log.d("onPreExecute","线程读到的包大于等于5个了");
                            mDone = true;
                            airKissCallBack.airKissConfigSuccess();
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        interrupt();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                isReadThreadRunning = false;
                Log.i(TAG,"ReceiveThread线程结束 udpServerSocket.close");

            } catch (SocketException e) {
                e.printStackTrace();
            }finally {
                if(udpServerSocket!=null)udpServerSocket.close();
                interrupt();
            }
        }
    }

    private class SendThread extends Thread{

        private DatagramSocket mSocket;

        @Override
        public void run() {
            super.run();

            for (int i = 0; i < DUMMY_DATA.length; i++) {

                DUMMY_DATA[i] = (byte)0x88;
            }
            sendSSIDAndPasswd();
            Log.i(TAG,"SendThread 线程结束");
            interrupt();
        }
        private void sendSSIDAndPasswd(){
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
                    if (isCancelled || mDone){
                        Log.d(TAG,"SendThread isCancelled="+isCancelled);
                        Log.d(TAG,"SendThread mDone="+mDone);
                        return;
                    }
                }
            }
            Log.d(TAG,"SendThread encoded_data遍历完成,发送数据包结束.");
            airKissCallBack.airKissConfigTimeOut();
            release();
        }
        private void sendPacketAndSleep(int length) {

            //这个端口为随机端口
            //如果没有socket监听该端口，会有异常at libcore.io.IoBridge.maybeThrowAfterRecvfrom
            //往10000端口发的话，不会有异常
            //虽然有异常,但不影响配置功能
            //这里设置为10000是让ReceiveThread能读到数据，这样就不会阻塞，可以及时发现取消配置
            int port_send = 10000;
            try {
                DatagramPacket pkg = new DatagramPacket(DUMMY_DATA,
                        length,
                        InetAddress.getByName("255.255.255.255"),
                        port_send);
                mSocket.send(pkg);
                //睡眠时间必须是毫秒级别的，如果是4s，配置将会很慢
                Thread.sleep(2);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }
}
