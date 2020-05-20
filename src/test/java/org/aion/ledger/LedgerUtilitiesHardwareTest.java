package org.aion.ledger;

import org.aion.ledger.exceptions.CommsException;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.google.common.truth.Truth.*;
import static org.junit.Assert.assertEquals;

public class LedgerUtilitiesHardwareTest {

    @Test
    public void printHardwareDevices() throws IOException {
        HidServices services = HidManager.getHidServices();
        for (org.hid4java.HidDevice device : services.getAttachedHidDevices()) {
            System.out.println(device);
        }
    }

    // this is tested against the ledger device, basically means that we need
    // a ledger device for these tests to pass
    @Test
    public void testLedgerConnected() throws IOException {
        LedgerDevice device = LedgerUtilities.findLedgerDevice();
        assertThat(device).isNotNull();
        device.close();
    }

    
    @Test
    public void testLedgerGetRandom() throws Exception {
        LedgerDevice device = LedgerUtilities.findLedgerDevice();
        assertThat(device).isNotNull();
        
        // This command returns random bytes from the dongle hardware random number generator
        ByteBuffer buff = ByteBuffer.allocate(5);
		buff.put((byte)0xE0);
		buff.put((byte)0xC0);
		buff.put((byte)0x02); // P1
		buff.put((byte)0x08); // P2
		buff.put((byte)0x00); // LEN
		buff.clear();
		device.exchange(buff.array());
        device.close();
    }
    
    // this is tested against the ledger device, basically means that we need
    // a ledger device for these tests to pass
    @Test
    public void testLedgerExchange() throws IOException {
        LedgerDevice device = LedgerUtilities.findLedgerDevice();
        assertThat(device).isNotNull();
        
        ByteBuffer buff = ByteBuffer.allocate(5);
		buff.put((byte)0x80);
		buff.put((byte)0x01);
		buff.put((byte)0x00); // P1
		buff.put((byte)0x00); // P2
		buff.put((byte)0x00); // LEN
		buff.clear();
		try {
			device.exchange(buff.array());
		}
		catch (CommsException e) {
			assertEquals(true, e.getResponseCode()==CommsException.RESP_INCORRECT_APP || e.getResponseCode() == CommsException.RESP_SECURITY_STATUS_LOCKED);
		}
        device.close();
    }
}
