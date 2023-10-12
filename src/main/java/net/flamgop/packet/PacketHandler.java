package net.flamgop.packet;

import net.flamgop.Portal;
import net.flamgop.packet.Packets;

import java.nio.ByteBuffer;

public abstract class PacketHandler {
    private final Packets packet;

    public PacketHandler(Packets packet) {
        this.packet = packet;
    }
    public Packets getPacket() {
        return packet;
    }
    public abstract void handle(Portal recipient, ByteBuffer packet);
}
