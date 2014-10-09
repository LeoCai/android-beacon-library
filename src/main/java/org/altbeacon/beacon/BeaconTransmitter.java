package org.altbeacon.beacon;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.altbeacon.beacon.Beacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Provides a mechanism for transmitting as a beacon.   Requires Android L
 */
@TargetApi(Build.VERSION_CODES.L)
public class BeaconTransmitter {
    private static final String TAG = "BeaconTransmitter";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private int mAdvertiseMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
    private int mAdvertiseTxPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;
    private Beacon mBeacon;
    private BeaconParser mBeaconParser;

    /**
     * Creates a new beacon transmitter capable of transmitting beacons with the format
     * specified in the BeaconParser and with the data fields specified in the Beacon object
     * @param context
     * @param parser specifies the format of the beacon transmission
     */
    public BeaconTransmitter(Context context, BeaconParser parser) {
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        mBeaconParser = parser;
    }

    /**
     * Sets the beacon whose fields will be transmitted
     * @param beacon
     */
    public void setBeacon(Beacon beacon) {
        mBeacon = beacon;
    }

    /**
     * @see #setAdvertiseMode(int)
     * @return advertiseMode
     */
    public int getAdvertiseMode() {
        return mAdvertiseMode;
    }

    /**
     * AdvertiseSettings.ADVERTISE_MODE_BALANCED 3 Hz
     * AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY 1 Hz
     * AdvertiseSettings.ADVERTISE_MODE_LOW_POWER 10 Hz
     * @param mAdvertiseMode
     */
    public void setAdvertiseMode(int mAdvertiseMode) {
        this.mAdvertiseMode = mAdvertiseMode;
    }

    /**
     * @see #setAdvertiseTxPowerLevel(int mAdvertiseTxPowerLevel)
     * @return txPowerLevel
     */
    public int getAdvertiseTxPowerLevel() {
        return mAdvertiseTxPowerLevel;
    }

    /**
     * AdvertiseSettings.ADVERTISE_TX_POWER_HIGH -56 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_LOW -75 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -66 dBm @ 1 meter with Nexus 5
     * AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW not detected with Nexus 5
     * @param mAdvertiseTxPowerLevel
     */
    public void setAdvertiseTxPowerLevel(int mAdvertiseTxPowerLevel) {
        this.mAdvertiseTxPowerLevel = mAdvertiseTxPowerLevel;
    }

    /**
     * Starts advertising with fields from the passed beacon
     * @param beacon
     */
    public void startAdvertising(Beacon beacon) {
        mBeacon = beacon;
        startAdvertising();
    }

    /**
     * Starts this beacon advertising
     */
    public void startAdvertising() {
        if (mBeacon == null) {
            throw new NullPointerException("Beacon cannot be null.  Set beacon before starting advertising");
        }
        String id1 = mBeacon.getIdentifiers().get(0).toString();
        int id2 = Integer.parseInt(mBeacon.getIdentifiers().get(1).toString());
        int id3 = Integer.parseInt(mBeacon.getIdentifiers().get(2).toString());
        int manufacturerCode = mBeacon.getManufacturer();

        byte[] advertisingBytes = mBeaconParser.getBeaconAdvertisementData(mBeacon);
        Log.d(TAG, "Starting advertising with ID1: "+id1+" ID2: "+id2+" ID3: "+id3);
        try{
            AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
            dataBuilder.setManufacturerData(manufacturerCode, advertisingBytes);

            AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();

            settingsBuilder.setAdvertiseMode(mAdvertiseMode);

            settingsBuilder.setTxPowerLevel(mAdvertiseTxPowerLevel);

            settingsBuilder.setType(AdvertiseSettings.ADVERTISE_TYPE_NON_CONNECTABLE);

            mBluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
            String byteString = "";
            for (int i= 0; i < advertisingBytes.length; i++) {
                byteString += String.format("%02X", advertisingBytes[i]);
                byteString += " ";
            }
            Log.e(TAG, "Started advertising with data: "+byteString);

        } catch (Exception e){
            Log.e(TAG, "Cannot start advetising due to excepton: ",e);
        }
    }

    /**
     * Stops this beacon from advertising
     */
    public void stopAdvertising() {
        Log.d(TAG, "Stopping advertising");
        mBluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onFailure(int errorCode) {
            Log.e(TAG,"Advertisement failed.");

        }

        @Override
        public void onSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG,"Advertisement succeeded.");
        }

    };

}