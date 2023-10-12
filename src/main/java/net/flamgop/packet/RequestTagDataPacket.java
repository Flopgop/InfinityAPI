package net.flamgop.packet;

public class RequestTagDataPacket implements Packet {
    @Override
    public byte command() {
        return (byte)0xb4;
    }

    @Override
    public byte length() {
        return 0x03;
    }

    @Override
    public byte[] otherData() {
        return new byte[0];
    }
}
