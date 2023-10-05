import type { Plugin, PluginListenerHandle } from '@capacitor/core';

export interface BluetoothSerialPlugin extends Plugin {
  isEnabled(): Promise<BluetoothEnabledResult>;

  enable(): Promise<BluetoothEnabledResult>;

  scan(): Promise<BluetoothScanResult>;

  getPairedDevices(): Promise<PairedDevicesResult>;

  connect(options: BluetoothConnectOptions): Promise<void>;

  connectInsecure(options: BluetoothConnectOptions): Promise<void>;

  disconnect(options: BluetoothConnectOptions): Promise<void>;

  isConnected(
    options: BluetoothConnectOptions,
  ): Promise<BluetoothConnectResult>;

  read(options: BluetoothReadOptions): Promise<BluetoothDataResult>;

  readUntil(options: BluetoothReadUntilOptions): Promise<BluetoothDataResult>;

  write(options: BluetoothWriteOptions): Promise<void>;

  enableNotifications(
    options: BluetoothEnableNotificationsOptions,
  ): Promise<BluetoothEnableNotificationsResult>;

  disableNotifications(
    options: BluetoothDisableNotificationsOptions,
  ): Promise<void>;

  addListener(
    eventName: 'onConnectionChange',
    listenerFunc: (state: BluetoothConnectionChangeEvent) => void,
  ): Promise<PluginListenerHandle>;

  addListener(
    eventName: string,
    listenerFunc: (...args: any[]) => any,
  ): Promise<PluginListenerHandle>;
}

export interface BluetoothEnabledResult {
  enabled: boolean;
}

export interface BluetoothScanResult {
  devices: BluetoothDevice[];
}

export interface PairedDevicesResult {
  devices: BluetoothDevice[];
}

export interface BluetoothConnectResult {
  connected: boolean;
}

export interface BluetoothDataResult {
  data: string;
}

export interface BluetoothEnableNotificationsResult {
  eventName: string;
}

export interface BluetoothDevice {
  name: string;
  id: string;
  address: string;
  class: number;
  uuid: string;
  rssi: number;
}

export interface BluetoothConnectOptions {
  address: string;
}

export interface BluetoothReadOptions {
  address: string;
}

export interface BluetoothReadUntilOptions {
  address: string;
  delimiter: string;
}

export interface BluetoothWriteOptions {
  address: string;
  value: string;
  charset?: string;
}

export interface BluetoothEnableNotificationsOptions {
  address: string;
  delimiter: string;
}

export interface BluetoothDisableNotificationsOptions {
  address: string;
}

export enum BluetoothConnectionState {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  PAIRING_REQUEST = 'PAIRING_REQUEST',
  UNKNOWN = 'UNKNOWN',
  CONNECTION_FAILED = 'CONNECTION_FAILED',
  // other relevant states here
}

export interface BluetoothConnectionChangeEvent {
  device?: BluetoothDevice;
  state: BluetoothConnectionState;
}
