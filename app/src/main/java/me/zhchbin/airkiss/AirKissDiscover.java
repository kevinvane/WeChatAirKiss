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

public class AirKissDiscover {


    private final String TAG = getClass().getSimpleName();
    private boolean isReadThreadRunning = false;
    private static final int PORT = 12476;
    private boolean isCancelled = false;


    public void execute(){
        if(!isReadThreadRunning){
            isReadThreadRunning = true;
            new DiscoverDevThread().start();
            new SendThread().start();
        }
    }
    public void cancel(){

        isCancelled = true;
    }
    private class SendThread extends Thread{

        private DatagramSocket mSocket;

        @Override
        public void run() {
            super.run();

            try {
                mSocket = new DatagramSocket();
                mSocket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            while(!isCancelled){
                sendPacketAndSleep(0);
            }
            Log.i(TAG,"SendThread 线程结束");
            interrupt();
        }
        private void sendPacketAndSleep(int length) {

            try {
                String data = "ping";
                DatagramPacket pkg = new DatagramPacket(data.getBytes(),
                        length,
                        InetAddress.getByName("255.255.255.255"),
                        PORT);
                mSocket.send(pkg);
                //睡眠时间必须是毫秒级别的，如果是4s，配置将会很慢
                Thread.sleep(1000);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
    }

    private class DiscoverDevThread extends Thread{

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            DatagramSocket udpServerSocket = null;
            try {
                udpServerSocket = new DatagramSocket(PORT);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                int replyByteCounter = 0;
                udpServerSocket.setSoTimeout(60*1000);
                Log.d(TAG, "ReceiveThread: 开始监听"+PORT);

                while (!isCancelled) {

                    try {
                        udpServerSocket.receive(packet);
                        //Log.d("run: ","端口"+PORT+"收到消息");
                        byte receivedData[] = packet.getData();
                        Log.d(" receivedData[]", "receivedData---replyByteCounter ====" + replyByteCounter);
                        Log.d(" receivedData[]", "receivedData---packet.getLength() ====" + packet.getLength());
                        Log.d(" receivedData[]", "receivedData---packet ====" + new String(receivedData));
                        //如果是自己发的，忽略；设备发的解析数据包，得到设备id和设备类型
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                        interrupt();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Log.i(TAG,"ReceiveThread线程结束 udpServerSocket.close");
            } catch (SocketException e) {
                e.printStackTrace();
            }finally {
                if(udpServerSocket!=null)udpServerSocket.close();
                interrupt();
            }
        }
    }


}
