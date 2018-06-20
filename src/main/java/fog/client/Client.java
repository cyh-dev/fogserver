package fog.client;



import com.yuhao.packet.DataPacket;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * Client
 *
 * @author CYH
 * @date 2018/5/23
 */
public class Client {

    public static void main(String[] args) throws Exception {

        SocketAddress address=new InetSocketAddress("127.0.0.1",8888);

        SocketChannel client=SocketChannel.open(address);

        client.configureBlocking(false);

        ByteBuffer buffer=ByteBuffer.allocate(1024);

        while (true){

            buffer.clear();

            System.out.println("发送数据包:");

            String data=new BufferedReader(new InputStreamReader(System.in)).readLine();
            if (data.equals("null")){
                break;
            }

            DataPacket dataPacket=new DataPacket();
            dataPacket.setContent(data);
            dataPacket.setCode(1);
            dataPacket.setSendTime(new Date());

            ByteArrayOutputStream bytesOut=new ByteArrayOutputStream();
            ObjectOutputStream ojbOut=new ObjectOutputStream(bytesOut);
            ojbOut.writeObject(dataPacket);
            ojbOut.close();

            buffer.put(bytesOut.toByteArray());
            bytesOut.close();
            buffer.flip();
            client.write(buffer);
            System.out.println("客户端---发送数据:"+data);
            buffer.clear();

            while (true) {

                long count = client.read(buffer);

                if (count > 0) {
                    buffer.flip();
                    byte[] b = new byte[buffer.limit()];
                    buffer.get(b, buffer.position(), buffer.limit());

                    ByteArrayInputStream byteIn = new ByteArrayInputStream(b);
                    ObjectInputStream objIn = new ObjectInputStream(byteIn);
                    DataPacket result = null;
                    try {
                        result = (DataPacket) objIn.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (result != null) {
                        System.out.println(result.toString());
                    }

                    objIn.close();
                    byteIn.close();

                    break;
                }
            }


        }
        client.close();
        System.out.println("连接关闭");

    }


}
