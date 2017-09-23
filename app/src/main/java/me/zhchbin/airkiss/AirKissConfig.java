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
    private static final int REPLY_BYTE_CONFIRM_TIMES = 2;
    //接收回复包的端口
    private static final int PORT = 10000;

    private char mRandomChar;
    private AirKissEncoder mAirKissEncoder;
    private volatile boolean mDone = false;
    private boolean isCancelled;
    private boolean isReadThreadRunning = false;

    private AirKissCallBack airKissCallBack;

    public AirKissConfig(AirKissCallBack airKissCallBack) {

        this.airKissCallBack = airKissCallBack;
    }

    public void execute(String ssid, String password){

        if(isReadThreadRunning){

            Log.e(TAG, "execute: read thread is running ,so return." );
            return;
        }
        resetFalg();
        mAirKissEncoder = new AirKissEncoder(ssid, password);
        mRandomChar = mAirKissEncoder.getRandomChar();
        new ReceiveThread().start();
        new SendThread().start();
    }
    private void resetFalg(){

        mDone = false;
        isCancelled = false;
    }
    public void cancel(){

        isCancelled = true;
    }
    private void finishConfig(){

        mDone = true;
    }



    private class ReceiveThread extends Thread{

        @Override
        public void run() {
            super.run();

            isReadThreadRunning = true;
            byte[] buffer = new byte[1024];
            DatagramSocket udpServerSocket = null;
            try {
                udpServerSocket = new DatagramSocket(PORT);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                int replyByteCounter = 0;

                //发送数据的线程结束，读线程收不到数据，就会导致超时，释放资源
                udpServerSocket.setSoTimeout(2*1000);
                Log.d(TAG, "ReceiveThread: 开始监听"+PORT);
                Log.d(TAG, "ReceiveThread: mRandomChar="+Integer.valueOf(mRandomChar));

                while (!mDone && !isCancelled) {

                    try {
                        Log.d("run: ","阻塞读...");
                        udpServerSocket.receive(packet);
                        byte receivedData[] = packet.getData();
                        for (byte b : receivedData) {
                            if (b == mRandomChar){
                                replyByteCounter++;
                                Log.i(" receivedData[]",  "packet.getData()----b == mRandomChar----"+b);
                            }
                        }
                        //Log.d(" receivedData[]", "receivedData---replyByteCounter ====" + replyByteCounter);
                        Log.d(" receivedData[]", "receivedData---packet.getLength() ====" + packet.getLength());
                        //Log.d(" receivedData[]", "receivedData---packet.getLength() ====" + new String(receivedData));

                        if (replyByteCounter >= REPLY_BYTE_CONFIRM_TIMES) {
                            Log.d("onPreExecute","线程读到的包等于"+REPLY_BYTE_CONFIRM_TIMES+"个了");
                            //mDone = true;
                            finishConfig();
                            airKissCallBack.airKissConfigSuccess();
                            break;
                        }
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        interrupt();
                    } catch (IOException e) {
                        e.printStackTrace();
                        interrupt();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }finally {
                isReadThreadRunning = false;
                if(udpServerSocket!=null){
                    Log.i(TAG,"ReceiveThread线程结束 udpServerSocket.close");
                    udpServerSocket.close();
                }
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
                    //如果取消或者已经完成
                    if (isCancelled || mDone){
                        Log.d(TAG,"SendThread isCancelled="+isCancelled);
                        Log.d(TAG,"SendThread mDone="+mDone);
                        return;
                    }
                }
            }
            Log.d(TAG,"SendThread encoded_data遍历完成,发送数据包结束.");
            airKissCallBack.airKissConfigTimeOut();
            //改变标志位，让读线程也退出。
            cancel();
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
