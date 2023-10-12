package net.flamgop.packet;

public interface Packet {
    byte command();
    byte length();
    byte[] otherData();
}
