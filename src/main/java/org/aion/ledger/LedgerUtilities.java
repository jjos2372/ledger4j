package org.aion.ledger;

import org.apache.commons.lang3.SystemUtils;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.hid4java.HidServicesSpecification;
import org.hid4java.ScanMode;
import org.hid4java.jna.HidApi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

import static org.aion.ledger.Constants.INTERFACE_NUMBER;
import static org.aion.ledger.Constants.LIB_NATIVE;
import static org.aion.ledger.Constants.USAGE_PAGE_LEDGER;

public class LedgerUtilities {

    static {
        // https://github.com/gary-rowe/hid4java/pull/43
        HidApi.dropReportIdZero = false;
    }

    @Nonnull
    private static void entry(StringBuilder builder, String entry, String value) {
        builder.append(entry);
        builder.append(" = ");
        builder.append(value);
        builder.append(", ");
    }

    @Nullable
    private static LedgerDevice findLedgerDeviceHIDAPI() {
        HidServicesSpecification specs = new HidServicesSpecification();
        HidServices services = HidManager.getHidServices(specs);

        for (org.hid4java.HidDevice device : services.getAttachedHidDevices()) {
            if (isLedger(device.getVendorId(), device.getProduct(),
                    device.getUsagePage(), device.getInterfaceNumber())) {

                if (!device.isOpen()) {
                    device.open();
                }
                return new LedgerHIDAPI(device);
            }
        }

        // nothing found
        return null;
    }

    @Nullable
    public static LedgerDevice findLedgerDevice() throws IOException {
        if (LIB_NATIVE) {
            return findLedgerDeviceHIDAPI();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static boolean isLedger(final int vendorId,
                                    @Nonnull final String productString,
                                    final int usagePage,
                                    final int interfaceNumber) {
        // TODO: this area needs work, original implement not picking up Nano S
        // TODO: the second condition specified below (for fallback on Mac OS and Windows currently not included)
        // see: https://github.com/LedgerHQ/ledgerjs/blob/master/packages/hw-transport-node-hid/src/getDevices.js
        // written this way more for clarity
        if (vendorId != Constants.VENDOR_LEDGER)
            return false;

        if (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
            if (usagePage != USAGE_PAGE_LEDGER) {
                return false;
            }
        } else {
            if (interfaceNumber != INTERFACE_NUMBER) {
                return false;
            }
        }

        return true;
    }

    @Nonnull
    public static String intToHex(int amount) {
        // TODO: this can be optimised
        return "0x" + bytesToHex(toByteArray(amount));
    }

    @Nonnull
    public static String shortToHex(short amount) {
        return "0x" + bytesToHex(toByteArray(amount));
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    @Nonnull
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // from: https://stackoverflow.com/questions/2648242/is-this-the-best-way-to-convert-string-hex-to-bytes
    // with some minor tweaks
    @Nonnull
    public static byte[] hexToBytes(@Nonnull String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of characters.");
        }

        if (hex.substring(0, 2).equals("0x")) {
            hex = hex.substring(2);
        }

        return hexToBytes(hex.toCharArray());
    }

    @Nonnull
    private static byte[] hexToBytes(@Nonnull char[] hex) {
        if (hex.length % 2 != 0) {
            throw new IllegalArgumentException("Must pass an even number of characters.");
        }

        int length = hex.length >> 1;
        byte[] raw = new byte[length];
        for (int o = 0, i = 0; o < length; o++) {
            raw[o] = (byte) ((getHexCharValue(hex[i++]) << 4)
                    | getHexCharValue(hex[i++]));
        }
        return raw;
    }

    private static byte getHexCharValue(char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        }

        if (c >= 'A' && c <= 'F') {
            return (byte) (10 + c - 'A');
        }

        if (c >= 'a' && c <= 'f') {
            return (byte) (10 + c - 'a');
        }
        throw new IllegalArgumentException("Invalid hex character");
    }

    /**
     * Returns a byte array given an int, function is guaranteed to return
     * a nonnull value that is of length 4
     *
     * @param i input integer
     * @return big-endian encoded byte array of length 4
     */
    @Nonnull
    private static byte[] toByteArray(final int i) {
        final byte[] val = new byte[4];
        val[3] = (byte) (i & 0xFF);
        val[2] = (byte) ((i >> 8) & 0xFF);
        val[1] = (byte) ((i >> 16) & 0xFF);
        val[0] = (byte) ((i >> 24) & 0xFF);
        return val;
    }

    @Nonnull
    private static byte[] toByteArray(final short i) {
        final byte[] val = new byte[2];
        val[1] = (byte) (i & 0xFF);
        val[0] = (byte) ((i >> 8) & 0xFF);
        return val;
    }

    @Nonnull
    public static byte[] toHardenedOffset(final int i) {
        byte[] offset = toByteArray(i);
        offset[0] = (byte) (offset[0] | (byte) 0x80);
        return offset;
    }
}
