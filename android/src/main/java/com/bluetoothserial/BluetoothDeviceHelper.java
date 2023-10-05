package com.bluetoothserial;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceHelper implements Serializable {

    public static JSArray devicesToJSArray(Set<BluetoothDevice> devices) {
        JSArray devicesAsJson = new JSArray();

        for (BluetoothDevice device : devices) {
            devicesAsJson.put(deviceToJSObject(device));
        }

        return devicesAsJson;
    }

    @SuppressLint("MissingPermission")
    public static JSObject deviceToJSObject(BluetoothDevice device) {
        JSObject json = new JSObject();
        json.put("name", device.getName());
        json.put("address", device.getAddress());
        json.put("id", device.getAddress());
        if (device.getBluetoothClass() != null) {
            json.put("class", device.getBluetoothClass().getDeviceClass());
        }
        return json;
    }

    public static byte[] toByteArray(String value, String charsetName) {
        if (value == null) {
            return new byte[0];
        }

        return value.getBytes(Charset.forName(charsetName));
    }

}
