package net.flamgop.packet;

public enum Packets {
    SET_COLOR((byte)0x90),
    TAG_UID((byte)0x09),
    PLATFORM_CHANGE((byte)0x04),
    ECHO((byte)0x01),
    BOOT((byte)0x15),
    DISC_LIST((byte)0x03),
    ANY((byte)-1)

    ;

    private final byte command;

    Packets(byte command) {
        this.command = command;
    }

    public byte command() {
        return command;
    }
}
