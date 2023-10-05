package com.bluetoothserial.plugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import com.bluetoothserial.BluetoothConnectionState;
import com.bluetoothserial.BluetoothDeviceHelper;
import com.bluetoothserial.BluetoothSerialService;
import com.bluetoothserial.KeyConstants;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CapacitorPlugin(permissions = {@Permission(strings = {Manifest.permission.ACCESS_COARSE_LOCATION}, alias = "ACCESS_COARSE_LOCATION"), @Permission(strings = {Manifest.permission.ACCESS_FINE_LOCATION}, alias = "ACCESS_FINE_LOCATION"), @Permission(strings = {Manifest.permission.BLUETOOTH}, alias = "BLUETOOTH"), @Permission(strings = {Manifest.permission.BLUETOOTH_ADMIN}, alias = "BLUETOOTH_ADMIN"), @Permission(strings = {"android.permission.BLUETOOTH_SCAN"}, alias = "BLUETOOTH_SCAN"), @Permission(strings = {"android.permission.BLUETOOTH_CONNECT"}, alias = "BLUETOOTH_CONNECT"),})
public class BluetoothSerial extends Plugin {
    private static final String ERROR_ADDRESS_MISSING = "Error: Address is missing.";
    private static final String ERROR_DEVICE_NOT_FOUND = "Error: Device not found.";
    private static final String ERROR_CONNECTION_FAILED = "Error: Connection failed.";
    private static final String ERROR_DISCONNECT_FAILED = "Error: Disconnect failed.";
    private static final String ERROR_WRITING = "Error: Writing.";
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                sendConnectionStateChange(device, BluetoothConnectionState.CONNECTED);
            } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                sendConnectionStateChange(device, BluetoothConnectionState.DISCONNECTED);
            } else if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                sendConnectionStateChange(device, BluetoothConnectionState.PAIRING_REQUEST);
            }
        }
    };
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSerialService service;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        private Set<BluetoothDevice> devices = new HashSet<>();

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName() == null) {
                        Log.d(getLogTag(), "ACTION_FOUND - No Device Name, trying to get it...");
                        // Initiate service discovery to fetch the name
                        device.fetchUuidsWithSdp();
                    } else {
                        Log.d(getLogTag(), "ACTION_FOUND - Adding New Device "+device.getName());
                        Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                        devices.add(device);
                    }
                    break;
                case BluetoothDevice.ACTION_UUID:
                    BluetoothDevice deviceFromUUID = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

                    // Now the device should have a name
                    if (deviceFromUUID.getName() != null) {
                        devices.add(deviceFromUUID);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    resolveDevices(devices);
                    unregisterReceiver(this);
                    break;
            }
        }
    };

    private String[] getPermissionAliases() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            return new String[]{"ACCESS_FINE_LOCATION", "BLUETOOTH_SCAN", "BLUETOOTH_CONNECT"};
        } else {
            return new String[]{"ACCESS_COARSE_LOCATION", "ACCESS_FINE_LOCATION", "BLUETOOTH", "BLUETOOTH_ADMIN"};
        }
    }

    private void unregisterReceiver(BroadcastReceiver receiver) {
        getContext().unregisterReceiver(receiver);
    }

    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        getContext().registerReceiver(receiver, filter);
    }

    private void resolveDevices(Set<BluetoothDevice> devices) {
        PluginCall call = getSavedCall();

        JSObject response = new JSObject();
        JSArray devicesAsJson = BluetoothDeviceHelper.devicesToJSArray(devices);
        response.put("devices", devicesAsJson);

        resolveCall(call, response);

        freeSavedCall();
    }

    @SuppressLint("MissingPermission")
    private void sendConnectionStateChange(BluetoothDevice device, String state) {
        JSObject ret = new JSObject();
        JSObject jsDevice = new JSObject();
        jsDevice.put("name", device.getName());
        jsDevice.put("address", device.getAddress());
        ret.put("device", jsDevice);
        ret.put("state", state);
        notifyListeners("onConnectionChange", ret);
    }

    @PluginMethod()
    public void isEnabled(PluginCall call) {
        boolean enabled = isEnabled();
        resolveEnableBluetooth(call, enabled);
    }

    @PluginMethod()
    public void enable(PluginCall call) {
        enableBluetooth(call);
    }

    @SuppressLint("MissingPermission")
    @PluginMethod()
    public void scan(PluginCall call) {
        if (rejectIfDisabled(call)) {
            return;
        }

        try {
            saveCall(call);

            IntentFilter filterFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            IntentFilter filterUUID = new IntentFilter(BluetoothDevice.ACTION_UUID);
            IntentFilter filterFinished = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

            registerReceiver(receiver, filterFound);
            registerReceiver(receiver, filterUUID);
            registerReceiver(receiver, filterFinished);

            bluetoothAdapter.startDiscovery();

            final BluetoothSerial serial = this;
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    serial.stopScan();
                }
            }, 5000);
        } catch (Exception e) {
            Log.e(getLogTag(), "Error searching devices", e);
            call.reject("Error searching devices", e);
            freeSavedCall();
        }
    }

    @SuppressLint("MissingPermission")
    @PluginMethod()
    public void getPairedDevices(PluginCall call) {
        if (rejectIfDisabled(call)) {
            return;
        }

        try {
            JSObject response = new JSObject();
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            JSArray devicesAsJson = BluetoothDeviceHelper.devicesToJSArray(devices);
            response.put("devices", devicesAsJson);
            resolveCall(call, response);
        } catch (Exception e) {
            Log.e(getLogTag(), "Error getting devices", e);
            call.reject("Error getting devices", e);
        }
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        bluetoothAdapter.cancelDiscovery();
    }

    @PluginMethod()
    public void connect(PluginCall call) {
        if (!isEnabled()) {
            enableBluetooth(call);
        }
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        if (rejectIfDisabled(call)) {
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            call.reject(ERROR_DEVICE_NOT_FOUND);
            return;
        }

        /* TODO - autoConnect
        Boolean autoConnect = call.getBoolean(keyAutoConnect);
        autoConnect = autoConnect == null ? false : autoConnect;
         */

        saveCall(call);
        getService().connect(device, this);
    }

    @SuppressLint("MissingPermission")
    public void connected() {
        PluginCall call = getSavedCall();
        if (call != null) {
            JSObject ret = new JSObject();
            String address = getAddress(call);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                call.reject(ERROR_DEVICE_NOT_FOUND);
                return;
            }
            boolean isConnected = getService().isConnected(address);
            JSObject jsDevice = new JSObject();
            jsDevice.put("name", device.getName());
            jsDevice.put("address", device.getAddress());
            ret.put("device", jsDevice);
            ret.put("state", isConnected ? BluetoothConnectionState.CONNECTED : BluetoothConnectionState.CONNECTION_FAILED);
            ret.put("debug", String.format("connected() was called, isConnected(%s) = %s", address, isConnected ? "true" : "false"));
            notifyListeners("onConnectionChange", ret);
            resolveCall(call);
            freeSavedCall();
        }
    }

    @SuppressLint("MissingPermission")
    public void connectionFailed() {
        PluginCall call = getSavedCall();
        if (call != null) {
            JSObject ret = new JSObject();
            String address = getAddress(call);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                call.reject(ERROR_DEVICE_NOT_FOUND);
                return;
            }
            JSObject jsDevice = new JSObject();
            jsDevice.put("name", device.getName());
            jsDevice.put("address", device.getAddress());
            ret.put("device", jsDevice);
            ret.put("state", BluetoothConnectionState.CONNECTION_FAILED);
            notifyListeners("onConnectionChange", ret);
            call.reject(ERROR_CONNECTION_FAILED);
            freeSavedCall();
        }
    }

    @PluginMethod()
    public void disconnect(PluginCall call) {
        String address = getAddress(call);
        boolean success;
        if (address == null) {
            success = getService().disconnectAllDevices();
        } else {
            success = getService().disconnect(address);
        }

        if (success) {
            resolveCall(call);
        } else {
            call.reject(ERROR_DISCONNECT_FAILED);
        }
    }

    @PluginMethod()
    public void isConnected(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        boolean connected = getService().isConnected(address);
        JSObject response = new JSObject();
        response.put("connected", connected);

        resolveCall(call, response);
    }

    @PluginMethod()
    public void write(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        String value = call.getString(KeyConstants.VALUE);
        //Log.i(getLogTag(), value);

        String charsetName = call.getString(KeyConstants.CHARSET);
        if (charsetName == null) {
            charsetName = "UTF-8";
        }

        boolean success = getService().write(address, BluetoothDeviceHelper.toByteArray(value, charsetName));

        if (success) {
            resolveCall(call);
        } else {
            call.reject(ERROR_WRITING);
        }
    }

    @PluginMethod()
    public void read(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        try {
            String value = getService().read(address);

            JSObject response = new JSObject();
            response.put("data", value);

            resolveCall(call, response);
        } catch (IOException e) {
            Log.e(getLogTag(), "Exception during read", e);
            call.reject("Exception during read", e);
        }
    }

    @PluginMethod()
    public void readUntil(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        String delimiter = getDelimiter(call);

        try {
            String value = getService().readUntil(address, delimiter);

            JSObject response = new JSObject();
            response.put("data", value);

            resolveCall(call, response);
        } catch (IOException e) {
            Log.e(getLogTag(), "Exception during readUntil", e);
            call.reject("Exception during readUntil", e);
        }
    }

    @PluginMethod()
    public void enableNotifications(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        String delimiter = getDelimiter(call);

        try {
            String eventName = getService().enableNotifications(address, delimiter);

            JSObject response = new JSObject();
            response.put("eventName", eventName);

            resolveCall(call, response);
        } catch (IOException e) {
            Log.e(getLogTag(), "Exception during enableNotifications", e);
            Log.e(getLogTag(), e.getMessage());
            call.reject("Exception during enableNotifications", e);
        }
    }

    @PluginMethod()
    public void disableNotifications(PluginCall call) {
        String address = getAddress(call);

        if (address == null) {
            call.reject(ERROR_ADDRESS_MISSING);
            return;
        }

        try {
            getService().disableNotifications(address);

            resolveCall(call);
        } catch (IOException e) {
            Log.e(getLogTag(), "Exception during disableNotifications", e);
            call.reject("Exception during disableNotifications", e);
        }
    }

    public void notifyClient(String eventName, JSObject response) {
        notifyListeners(eventName, response);
    }


    @Override
    protected void handleOnStart() {
        super.handleOnStart();
        initializeBluetoothAdapter();
        initializeService();
    }

    @Override
    protected void handleOnStop() {
        super.handleOnStop();
        unregisterReceiver(bluetoothStateReceiver);
        /* Disconnects Bluetooth devices when the app goes to the background
        if(service != null) {
            getService().stopAll();
        }
        */
    }

    private void enableBluetooth(PluginCall call) {
        if (!hasRequiredPermissions()) {
            requestPermissionForAliases(getPermissionAliases(), call, "checkPermission");
        } else if (isEnabled()) {
            resolveEnableBluetooth(call, true);
            return;
        }
    }

    @PermissionCallback
    private void checkPermission(PluginCall call) {
        Boolean permissionNotGranted = false;
        for (String permissionAlias : getPermissionAliases()) {
            if (getPermissionState(permissionAlias) != PermissionState.GRANTED) {
                permissionNotGranted = true;
            }
        }
        if (!permissionNotGranted) {
            resolveEnableBluetooth(call, false);
        } else {
            call.reject("Permission is required to take a picture");
        }
    }

    private void resolveEnableBluetooth(PluginCall call, boolean enabled) {
        JSObject ret = new JSObject();
        ret.put(KeyConstants.ENABLED, enabled);

        resolveCall(call, ret);
    }

    private void resolveCall(PluginCall call, JSObject ret) {
        call.resolve(ret);
        call.release(getBridge());
    }

    private void resolveCall(PluginCall call) {
        call.resolve();
        releaseBridge(call);
    }

    private void releaseBridge(PluginCall call) {
        if (call != null && !call.isReleased()) {
            call.release(getBridge());
        }
    }

    private boolean rejectIfDisabled(PluginCall call) {
        if (!hasRequiredPermissions()) {
            if (!hasRequiredPermissions()) {
                requestPermissionForAliases(getPermissionAliases(), call, "checkPermission");
            }
            Log.e(getLogTag(), "App does not have permission to access bluetooth");

            call.reject("App does not have permission to access bluetooth");
            return true;
        }

        if (isDisabled()) {
            Log.e(getLogTag(), "Bluetooth is disabled");

            call.reject("Bluetooth is disabled");
            return true;
        }

        return false;
    }

    @Override
    public boolean hasRequiredPermissions() {
        for (String alias : getPermissionAliases()) {
            if (getPermissionState(alias) != PermissionState.GRANTED) {
                Log.e(getLogTag(), "Permission not granted: " + alias);

                return false;
            }
        }
        return true;
    }

    private boolean isDisabled() {
        return !isEnabled();
    }

    private boolean isEnabled() {
        return hasRequiredPermissions() && bluetoothAdapter.isEnabled();
    }

    private void initializeBluetoothAdapter() {
        bluetoothAdapter = getBluetoothManager().getAdapter();
    }

    private void initializeService() {
        if (service == null) {
            service = new BluetoothSerialService(this, bluetoothAdapter);
        }

        IntentFilter filterAclConnected = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filterAclDisconnected = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter filterAclPairingRequest = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);

        registerReceiver(bluetoothStateReceiver, filterAclConnected);
        registerReceiver(bluetoothStateReceiver, filterAclDisconnected);
        registerReceiver(bluetoothStateReceiver, filterAclPairingRequest);
    }

    private String getAddress(PluginCall call) {
        return getString(call, KeyConstants.ADDRESS_UUID);
    }

    private String getDelimiter(PluginCall call) {
        return getString(call, KeyConstants.DELIMITER);
    }

    private String getString(PluginCall call, String key) {
        return call.getString(key);
    }

    private BluetoothManager getBluetoothManager() {
        return (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    private BluetoothSerialService getService() {
        if (service == null) {
            initializeService();
        }

        return service;
    }
}
