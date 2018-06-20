package fog.server;


import com.yuhao.packet.DataPacket;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * FogServer
 *
 * @author CYH
 * @date 2018/5/23
 */
public class FogServer {

    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private ServerSocket serverSocket;
    private static int PORT;
    private static int BUFFER_SIZE;
    private ByteBuffer buffer;

    private List<SocketChannel> acqSystem;

    private List<SocketChannel> client;

    private static File data;



    public FogServer(int port,int bufferSize){

        PORT=port;
        BUFFER_SIZE=bufferSize;
        this.buffer=ByteBuffer.allocate(BUFFER_SIZE);
        acqSystem=new ArrayList<>();
        client=new ArrayList<>();

        data=new File("data.txt");
        if (!data.exists()){
            try {
                data.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * 启动监听服务
     */
    public void startListen() throws Exception{


        /**
         * 打开选择器
         */
        selector=Selector.open();

        /**
         * 打开服务通道
         */
        serverSocketChannel=ServerSocketChannel.open();

        /**
         * 设置通道为非阻塞
         */
        serverSocketChannel.configureBlocking(false);

        /**
         * 创建服务端socket
         */
        serverSocket=serverSocketChannel.socket();

        /**
         * 绑定监听端口
         */
        serverSocket.bind(new InetSocketAddress(PORT));

        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        System.out.println("端口绑定完毕!!");


        Iterator<SelectionKey> keyIterator=null;
        SelectionKey key=null;

        while (true){
            selector.select();
            keyIterator=selector.selectedKeys().iterator();

            while (keyIterator.hasNext()){

                key=keyIterator.next();

                /**
                 * 处理具体逻辑
                 */
                this.handleKey(key);

                keyIterator.remove();

            }

        }

    }

    private void handleKey(SelectionKey key) throws Exception{

        /**
         * 连接事件
         */
        if (key.isAcceptable()){
            SocketChannel socketChannel=this.serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector,SelectionKey.OP_READ);
            System.out.println("新的连接!!");

        }
        /**
         * 读取信息事件
         */
        else if (key.isReadable()){

            SocketChannel socketChannel= (SocketChannel) key.channel();

            try {
                buffer.clear();

                long count=socketChannel.read(buffer);

                if (count>0){
                    buffer.flip();
                    byte[] b=new byte[buffer.limit()];
                    buffer.get(b,buffer.position(),buffer.limit());

                    ByteArrayInputStream byteIn=new ByteArrayInputStream(b);
                    ObjectInputStream objIn=new ObjectInputStream(byteIn);
                    try{
                        DataPacket dataPacket= (DataPacket) objIn.readObject();
                        handlCode(socketChannel,dataPacket);

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    objIn.close();
                    byteIn.close();

                }else {
                    System.out.println("连接关闭");
                    socketChannel.close();
                }

            }catch (Exception e){
                e.printStackTrace();
                socketChannel.close();
            }

        }
        /**
         * 写信息事件
         */
        else if (key.isWritable()){

            System.out.println("写数据");


        }

    }

    private void handlCode(SocketChannel socketChannel,DataPacket dataPacket){

        switch (dataPacket.getCode()){
            case 1:
                System.out.println("家庭血氧仪数据");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("家庭-血氧浓度",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);


                break;

            case 2:

                System.out.println("社区血氧仪数据");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("社区-血氧浓度",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);

                break;

            case 3:

                System.out.println("社区-PM2.5");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("社区-PM2.5",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);

                break;

            case 4:

                System.out.println("医院-血氧浓度");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("医院-血氧浓度",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);

                break;

            case 5:

                System.out.println("医院-PM2.5");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("医院-PM2.5",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);

                break;

            case 6:

                System.out.println("医院-温度");
                /*
                写入文件保存记录
                 */
                try {
                    saveData("医院-温度",dataPacket,socketChannel.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /**
                 * 发送给客户端
                 */
                sendClient(dataPacket);

                break;



            case 7:
                /**
                 * 采集设备连接
                 */
                acqSystem.add(socketChannel);
                DataPacket data1=new DataPacket();
                data1.setCode(200);
                data1.setContent("ok");
                data1.setSendTime(new Date());
                sendMsg(socketChannel,data1);
                System.out.println("采集设备连接");
                break;

            case 8:
                /**
                 * 采集设备断开连接
                 */
                for (SocketChannel socket:acqSystem) {
                    try {
                        if (socket.getRemoteAddress().equals(socketChannel.getRemoteAddress())){
                            acqSystem.remove(socketChannel);
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("采集断开连接");
                break;

            case 9:
                /**
                 * 客户端设备连接
                 */
                client.add(socketChannel);
                DataPacket data2=new DataPacket();
                data2.setCode(200);
                data2.setContent("ok");
                data2.setSendTime(new Date());
                sendMsg(socketChannel,data2);
                System.out.println("客户APP设备连接");


                break;

            case 10:
                /**
                 * 客户端断开连接
                 */
                for (SocketChannel channel:client) {
                    try {
                        if (channel.getRemoteAddress().equals(socketChannel.getRemoteAddress())){
                            client.remove(socketChannel);
                            break;
                        }
                    } catch (IOException e) {
                        System.out.println();
                        e.printStackTrace();
                    }
                }
                System.out.println("客户APP设备断开连接");

                break;

            case 11:
                System.out.println("控制家庭血氧仪数据");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);


                break;

            case 12:

                System.out.println("控制-社区血氧仪数据");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);

                break;

            case 13:

                System.out.println("控制-社区-PM2.5");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);

                break;

            case 14:

                System.out.println("控制-医院-血氧浓度");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);

                break;

            case 15:

                System.out.println("控制-医院-PM2.5");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);

                break;

            case 16:

                System.out.println("控制-医院-温度");

                /**
                 * 发送给采集设备
                 */
                sendAcq(dataPacket);

                break;




            default:
                DataPacket error=new DataPacket();
                error.setCode(404);
                error.setContent("error code");
                error.setSendTime(new Date());
                sendMsg(socketChannel,error);
                System.out.println("错误信息");
                break;
        }

    }




    private void sendClient(DataPacket dataPacket){
        for (SocketChannel socket:client) {
            sendMsg(socket,dataPacket);
        }
    }

    private void sendAcq(DataPacket dataPacket){
        for (SocketChannel socket:acqSystem) {
            sendMsg(socket,dataPacket);
        }
    }




    private void sendMsg(SocketChannel socket,DataPacket dataPacket){
            try {
                ByteArrayOutputStream bytesOut=new ByteArrayOutputStream();
                ByteBuffer buffer=ByteBuffer.allocate(BUFFER_SIZE);
                ObjectOutputStream ojbOut = new ObjectOutputStream(bytesOut);
                ojbOut.writeObject(dataPacket);
                ojbOut.close();
                buffer.put(bytesOut.toByteArray());
                bytesOut.close();
                buffer.flip();
                if (socket.isConnected()){
                    socket.write(buffer);
                }
                buffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }



    private void saveData(String title,DataPacket dataPacket,String remoteAddress){
        try {
            String str = "设备:"+remoteAddress+ "-"+title+": "+dataPacket.getContent()+"   时间:"+dataPacket.getSendTime()+"\r\n";
            FileUtils.write(data,str,"utf-8",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args){


        FogServer server=new FogServer(8888,1024);

        try {
            server.startListen();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}
