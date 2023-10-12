package net.flamgop.packet;

public record SetColorPacket(byte platform, byte r, byte g, byte b) implements Packet {
    @Override
    public byte command() {
        return Packets.SET_COLOR.command();
    }

    @Override
    public byte length() {
        return 0x06;
    }

    @Override
    public byte[] otherData() {
        return new byte[]{
                platform(),
                r(),
                g(),
                b()
        };
    }
}
