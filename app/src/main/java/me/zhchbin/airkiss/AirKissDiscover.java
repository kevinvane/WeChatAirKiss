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
    private static final int PORT = 12477;
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

        private DatagramSocket ms;

        @Override
        public void run() {
            super.run();

            try {
                ms = new DatagramSocket();
                ms.setBroadcast(true);

                String data = "ping";
                //ms = new MulticastSocket();
                InetAddress ip = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(data.getBytes(),
                        data.length(), ip, PORT);
                while(!isCancelled){

                    Log.d(TAG, "发送广播sendBroadcast:"+PORT+"->"+data);
                    sendPacketAndSleep(packet);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                if(ms!=null)ms.close();
            }
            Log.i(TAG,"SendThread 线程结束");
            interrupt();
        }
        private void sendPacketAndSleep(DatagramPacket packet) {

            try {
                ms.send(packet);
                Thread.sleep(3000);
            } catch (Exception e1) {
                e1.printStackTrace();
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
                //udpServerSocket.setSoTimeout(60*1000);
                Log.d(TAG, "ReceiveThread: 开始监听"+PORT);

                while (!isCancelled) {

                    try {
                        udpServerSocket.receive(packet);
                        byte receivedData[] = packet.getData();
                        //如果是自己发的，忽略；设备发的解析数据包，得到设备id和设备类型
                        String msg =  new String(receivedData,0,packet.getLength()).trim();
                        if("ping".equals(msg)){
                            Log.d(TAG, "这是自己发的[" + msg +"]");
                        }else{
                            Log.i(TAG, "receivedData---packet ====[" + msg +"]");
                        }
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
