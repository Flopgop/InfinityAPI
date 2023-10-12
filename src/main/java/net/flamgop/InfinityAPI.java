package net.flamgop;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.usb4java.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class InfinityAPI {
    private static volatile InfinityAPI instance = null;

    private final List<Portal> portals = new ArrayList<>();

    private InfinityAPI() {
        Context context = new Context();
        if (LibUsb.init(context) != 0) return;
        Thread shutdown = new Thread(() -> LibUsb.exit(context), "LibUSB Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdown);

        refreshPortals(context);
    }

    @ApiStatus.Internal
    public void refreshPortals(@NotNull Context context) {
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
        IntStream.range(0,portalCount).forEach(i -> portals.add(new Portal(context, portalIndices[i])));
    }

    public @Unmodifiable @NotNull List<Portal> getPortals() {
        return Collections.unmodifiableList(portals);
    }

    public static InfinityAPI init() {
        if (instance != null) return instance;
        return instance = new InfinityAPI();
    }
}
