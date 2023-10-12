package net.flamgop;

import net.flamgop.packet.Packet;
import net.flamgop.packet.PacketHandler;
import net.flamgop.packet.Packets;
import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Portal {
    private final int id;
    private final Context context;
    private final DeviceHandle deviceHandle;

    private final List<PacketHandler> handlers = new ArrayList<>();

    private boolean alreadyActive = false;

    public Portal(Context context, int id) {
        this.id = id;
        this.context = context;
        this.deviceHandle = connect(id);

        hijackDriver();
    }

    public void sendPacket(Packet packet) {
        var buffer = ByteBuffer.allocateDirect(32);
        buffer.rewind();
        buffer.put(0, (byte)0xff); // Prefix
        buffer.put(1, packet.length()); // length
        buffer.put(2, packet.command()); // command
        buffer.put(3, packet.command()); // response code
        buffer.put(4, packet.otherData()); // command data
        int checksum = 0;
        for (int i = 0; i < packet.length()+2; i++) {
            checksum+=buffer.get(i);
        }
        buffer.put(packet.length()+2, (byte)(checksum&0xff)); // checksum
        sendPacket(buffer);

    }

    private void hijackDriver() {
        if (LibUsb.kernelDriverActive(deviceHandle, 0) == 1) {
            if (LibUsb.detachKernelDriver(deviceHandle, 0) < 0) {
                LibUsb.close(deviceHandle);
            }
        }

        int error = LibUsb.claimInterface(deviceHandle, 0);
        if (error != 0) {
            System.out.println("Error: " + error);
            System.out.println("Error name: " + LibUsb.errorName(error));
            System.exit(1);
        }
    }

    private DeviceHandle connect(int id) {
        DeviceList list = new DeviceList();
        LibUsb.getDeviceList(context, list);

        DeviceHandle handle = new DeviceHandle();
        LibUsb.open(list.get(id), handle);

        DeviceDescriptor descriptor = new DeviceDescriptor();
        LibUsb.getDeviceDescriptor(list.get(id), descriptor);

        if (descriptor.idVendor() != 0x0e6f || descriptor.idProduct() != 0x0129) throw new AssertionError("Device was not a Disney Infinity Portal!");
        return handle;
    }

    public void activate() {
        if (alreadyActive) throw new IllegalStateException("Portal is already active!");
        alreadyActive = true;
        ByteBuffer packet = ByteBuffer.allocateDirect(32);
        packet.put(new byte[]{(byte) 0xff,0x11, (byte) 0x80,0x00,0x28,0x63,0x29,0x20,0x44,0x69,0x73,0x6e,0x65,0x79,0x20,0x32,0x30,0x31,0x33, (byte) 0xb6,0x30,0x6f, (byte) 0xcb,0x40,0x30,0x6a,0x44,0x20,0x30,0x5c,0x6f,0x00});
        sendPacket(packet);
    }

    private void sendPacket(ByteBuffer packet) {
        int ret = -1;
        IntBuffer length = IntBuffer.allocate(1);

        receivePackets();

        while (ret < 0) {
            ret = LibUsb.bulkTransfer(deviceHandle, (byte) 0x01, packet, length, 100);
            receivePackets();
        }
    }

    public void addPacketHandler(PacketHandler handler) {
        handlers.add(handler);
    }

    @SuppressWarnings("UnusedReturnValue")
    public int receivePackets() {
        int packetsReceived = 0;
        int ret = 0;

        IntBuffer length = IntBuffer.allocate(1);
        ByteBuffer packet = ByteBuffer.allocateDirect(32);
        while (ret == 0) {
            ret = LibUsb.bulkTransfer(deviceHandle, (byte) 0x81, packet, length, 10);
            if (ret == 0) {
                receivePacket(packet);
                packetsReceived++;
            }
        }

        return packetsReceived;
    }

    private void receivePacket(ByteBuffer packet) {
        byte first = packet.get();
        byte second = packet.get();
        processPacket(packet, Packets.ANY);
        switch (first) {
            case (byte) 0xaa -> {
                switch (second) {
                    case (byte)0x09 -> processPacket(packet, Packets.TAG_UID);
                    case (byte)0x01 -> processPacket(packet, Packets.ECHO);
                    case (byte)0x15 -> processPacket(packet, Packets.BOOT);
                    case (byte)0x03 -> processPacket(packet, Packets.DISC_LIST);
                    default -> System.out.println("Unknown second byte: 0x" + Integer.toHexString(second));
                }
            }
            case (byte) 0xab -> {
                switch (second) { // TODO add more
                    case (byte)0x04 -> processPacket(packet, Packets.PLATFORM_CHANGE);
                    default -> System.out.println("Unknown second byte: 0x" + Integer.toHexString(second));
                }
            }
            default -> System.out.println("Unknown first byte: 0x" + Integer.toHexString(first));
        }
    }

    private void processPacket(ByteBuffer packet, Packets packetType) {
        packet.rewind();
        var noHandlers = handlers.stream().filter(ph -> ph.getPacket() == packetType).findAny().isEmpty();
        if (noHandlers) {
            System.out.println("Couldn't find valid handler for " + packetType);
        }
        handlers.stream().filter(ph -> ph.getPacket() == packetType).forEach(ph -> {
            ph.handle(this, packet);
            packet.rewind();
        });
    }

    public int getId() {
        return id;
    }
}
