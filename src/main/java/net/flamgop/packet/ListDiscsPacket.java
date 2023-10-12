package net.flamgop.packet;

public class ListDiscsPacket implements Packet {
    @Override
    public byte command() {
        return Packets.DISC_LIST.command();
    }

    @Override
    public byte length() {
        return 0x02;
    }

    @Override
    public byte[] otherData() {
        return new byte[0];
    }
}
