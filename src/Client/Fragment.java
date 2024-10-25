package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class Fragment {
    void fragmentUDP(byte[] screenBytes, int frameId, InetAddress address, int serverPort, DatagramSocket socket) throws InterruptedException, IOException {

        // Chia nhỏ dữ liệu thành các gói tin nhỏ
        int maxPacketSize = 1400; // Tổng kích thước gói tin UDP
        int headerSize = 8; // Kích thước header (4 + 2 + 2)
        int dataSizePerPacket = maxPacketSize - headerSize;

        int totalPackets = (int) Math.ceil((double) screenBytes.length / dataSizePerPacket);

        for (int packetNum = 0; packetNum < totalPackets; packetNum++) {
            int start = packetNum * dataSizePerPacket;
            int end = Math.min(start + dataSizePerPacket, screenBytes.length);
            int length = end - start;

            byte[] packetData = new byte[headerSize + length];

            // Thêm Frame ID vào đầu gói tin (4 bytes)
            ByteBuffer.wrap(packetData, 0, 4).putInt(frameId);

            // Thêm Total Packets vào gói tin (2 bytes)
            ByteBuffer.wrap(packetData, 4, 2).putShort((short) totalPackets);

            // Thêm Packet Number vào gói tin (2 bytes)
            ByteBuffer.wrap(packetData, 6, 2).putShort((short) packetNum);

            // Thêm dữ liệu hình ảnh vào gói tin
            System.arraycopy(screenBytes, start, packetData, headerSize, length);

            // Gửi gói tin
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, serverPort);
            socket.send(packet);
        }

        System.out.println("Đã gửi Frame ID: " + frameId + " với " + totalPackets + " gói tin!");

        // Điều chỉnh tần suất gửi
        Thread.sleep(100); // 10 fps
    }
}
