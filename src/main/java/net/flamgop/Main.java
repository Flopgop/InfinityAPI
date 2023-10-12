package net.flamgop;

import net.flamgop.crypto.KeyCalculation;
import net.flamgop.packet.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.usb4java.*;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class Main {
    private static final ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();

    public static void main(String[] args) throws IOException {
        Context context = new Context();
        if (LibUsb.init(context) != 0) return;
        Thread shutdown = new Thread(() -> LibUsb.exit(context), "LibUSB Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdown);



        DeviceList list = new DeviceList();
        int deviceCount;
        if ((deviceCount = LibUsb.getDeviceList(context, list)) <= 0) throw new AssertionError("Couldn't get devices.");

        int portalCount = 0;
        int[] portalIndices = new int[0xff];

        System.out.println("Found " + deviceCount + " devices");
        for (int i = 0; i < deviceCount; i++) {
            Device device = list.get(i);
            DeviceHandle portal = new DeviceHandle();
            if (LibUsb.open(device, portal) < 0) continue;
            DeviceDescriptor descriptor = new DeviceDescriptor();
            LibUsb.getDeviceDescriptor(device, descriptor);

            if (descriptor.idVendor() == 0x0e6f && descriptor.idProduct() == 0x0129) {
                System.out.println("Found a portal");

                portalIndices[portalCount++] = i;
            }
        }

        final List<String> testKeys = List.of(
                "0456263a873a80",
                "049c0bb2a03784",
                "04a0f02a3d2d80",
                "04b40c12a13780",
                "04d9fb8a763b80"
        );
        final List<String> expectedKeys = List.of(
                "29564af75805",
                "c0b423c8e4c2",
                "1e0615823120",
                "2737629f2ebe",
                "edb56de8a9fe"
        );
        if (!testKeys.stream().map(s -> {
            try {
                return KeyCalculation.calculateKeyA(s,0);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // ideally this wouldn't happen, but in the event it does we don't want to continue because it will break the key decryption.
            }
        }).allMatch(s -> expectedKeys.stream().anyMatch(s::equalsIgnoreCase))) {
            System.out.println("Failed to verify all key checks!");
            return;
        }
        System.out.println("Successfully verified all key checks.");

        final Deque<Byte> last4 = new LinkedList<>();

        PacketHandler platformChangeHandler = new PacketHandler(Packets.PLATFORM_CHANGE) {
            @Override
            public void handle(Portal recipient, ByteBuffer packet) {
                byte placedOrRemoved = packet.get(0x05);
                byte platform = packet.get(0x02);
                byte r = (byte) (placedOrRemoved == 0 ? platform % 3 == 0 ? 0xff : 0x00 : 0x00);
                byte g = (byte) (placedOrRemoved == 0 ? platform % 3 == 1 ? 0xff : 0x00 : 0x00);
                byte b = (byte) (placedOrRemoved == 0 ? platform % 3 == 2 ? 0xff : 0x00 : 0x00);
                var colorPacket = new SetColorPacket(platform, r,g,b);
                if (last4.size() >= 4) last4.pollLast();
                if (placedOrRemoved == 0) last4.push(platform);
                recipient.sendPacket(colorPacket);
            }
        };

        PacketHandler tagUIDHandler = new PacketHandler(Packets.TAG_UID) {
            @Override
            public void handle(Portal recipient, ByteBuffer packet) {
                packet.rewind();
                byte[] uid = new byte[7];
                packet.get(4, uid);
                try {
                    System.out.println("Key A for " + KeyCalculation.hexlify(uid) + ": " + KeyCalculation.calculateKeyA(KeyCalculation.hexlify(uid), 0));
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        PacketHandler bootHandler = new PacketHandler(Packets.BOOT) {
            @Override
            public void handle(Portal recipient, ByteBuffer packet) {
                SetColorPacket p1 = new SetColorPacket((byte) 1, (byte) 0xff, (byte) 0x00, (byte) 0x00);
                SetColorPacket p2 = new SetColorPacket((byte) 2, (byte) 0x00, (byte) 0xff, (byte) 0x00);
                SetColorPacket p3 = new SetColorPacket((byte) 3, (byte) 0x00, (byte) 0x00, (byte) 0xff);
                recipient.sendPacket(p1);
                recipient.sendPacket(p2);
                recipient.sendPacket(p3);
                threadPool.submit(() -> {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    SetColorPacket p1o = new SetColorPacket((byte) 1, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                    SetColorPacket p2o = new SetColorPacket((byte) 2, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                    SetColorPacket p3o = new SetColorPacket((byte) 3, (byte) 0x00, (byte) 0x00, (byte) 0x00);
                    recipient.sendPacket(p1o);
                    recipient.sendPacket(p2o);
                    recipient.sendPacket(p3o);
                });
            }
        };

        AtomicInteger packetCount = new AtomicInteger(0);

        PacketHandler echoHandler = new PacketHandler(Packets.ECHO) {
            @Override
            public void handle(Portal recipient, ByteBuffer packet) {
                packetCount.incrementAndGet();
            }
        };

        Thread thread = new Thread(() -> System.out.println(packetCount.get() + " packets sent during this session."));
        Runtime.getRuntime().addShutdownHook(thread);

        final long[] lastPacketTime = {System.currentTimeMillis()};

        PacketHandler anyHandler = new PacketHandler(Packets.ANY) {
            @Override
            public void handle(Portal recipient, ByteBuffer packet) {
                if (!((packet.get() == (byte)0xaa) && (packet.get() == 0x01)))
                    lastPacketTime[0] = System.currentTimeMillis();
            }
        };

        List<Portal> portals = new ArrayList<>();
        IntStream.range(0,portalCount).forEach(i -> portals.add(new Portal(context, portalIndices[i])));
        portals.forEach(p -> p.addPacketHandler(platformChangeHandler));
        portals.forEach(p -> p.addPacketHandler(tagUIDHandler));
        portals.forEach(p -> p.addPacketHandler(bootHandler));
        portals.forEach(p -> p.addPacketHandler(echoHandler));
        portals.forEach(p -> p.addPacketHandler(anyHandler));
        portals.forEach(Portal::activate);

        double animationTime = 0;

        double delta = 0;

        boolean once = true;

        Terminal terminal = TerminalBuilder.builder()
                .jna(true)
                .system(true)
                .build();

        terminal.enterRawMode();
        var reader = terminal.reader();

        AtomicBoolean shouldClose = new AtomicBoolean(false);
        threadPool.submit(() -> {
            while (!shouldClose.get()) {
                try {
                    shouldClose.set(reader.read() != -2);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        while (!shouldClose.get()) {
            long start = System.currentTimeMillis();
            portals.forEach(Portal::receivePackets);
            AtomicInteger matching = new AtomicInteger();
            AtomicInteger index = new AtomicInteger();
            last4.forEach(b -> {
                if (b == index.byteValue()+1) {
                    matching.incrementAndGet();
                }
                index.incrementAndGet();
            });
            if (matching.get() >= 3) {
                RequestTagDataPacket packet = new RequestTagDataPacket();
                portals.forEach(p -> p.sendPacket(packet));
                last4.clear();
            }

            if (System.currentTimeMillis()-lastPacketTime[0] > 5000L) {
                once = false;
                animationTime += delta/5000.0;
                Color col = new Color(Color.HSBtoRGB((float) (animationTime*360f), 1f, 1f));
                byte r = (byte) col.getRed();
                byte g = (byte) col.getGreen();
                byte b = (byte) col.getBlue();

                portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 1, r, g, b)));
                portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 2, r, g, b)));
                portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 3, r, g, b)));

            } else {
                animationTime = 0;
                if (!once) {
                    once = true;
                    portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 1, (byte) 0, (byte) 0, (byte) 0)));
                    portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 2, (byte) 0, (byte) 0, (byte) 0)));
                    portals.forEach(p -> p.sendPacket(new SetColorPacket((byte) 3, (byte) 0, (byte) 0, (byte) 0)));
                }
            }
            long end = System.currentTimeMillis();
            delta = (end-start)/1000.0;
        }
        reader.close();
        terminal.close();
    }
}