import type { PluginListenerHandle } from '@capacitor/core';
import { WebPlugin } from '@capacitor/core';

import type {
  BluetoothConnectOptions,
  BluetoothConnectResult,
  BluetoothDataResult,
  BluetoothDisableNotificationsOptions,
  BluetoothEnabledResult,
  BluetoothEnableNotificationsOptions,
  BluetoothEnableNotificationsResult,
  BluetoothReadOptions,
  BluetoothReadUntilOptions,
  BluetoothScanResult,
  PairedDevicesResult,
  BluetoothSerialPlugin,
  BluetoothWriteOptions,
  BluetoothConnectionChangeEvent,
} from './definitions';
import { BluetoothConnectionState } from './definitions';
import { OptionsRequiredError } from './utils/errors';

export class BluetoothSerialWeb
  extends WebPlugin
  implements BluetoothSerialPlugin {
  async isEnabled(): Promise<BluetoothEnabledResult> {
    throw new Error('Method not implemented.');
  }

  async enable(): Promise<BluetoothEnabledResult> {
    throw new Error('Method not implemented.');
  }

  async scan(): Promise<BluetoothScanResult> {
    throw new Error('Method not implemented.');
  }

  async getPairedDevices(): Promise<PairedDevicesResult> {
    throw new Error('Method not implemented.');
  }

  async connect(options: BluetoothConnectOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async connectInsecure(options: BluetoothConnectOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async disconnect(options: BluetoothConnectOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async isConnected(
    options: BluetoothConnectOptions,
  ): Promise<BluetoothConnectResult> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async read(options: BluetoothReadOptions): Promise<BluetoothDataResult> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async readUntil(
    options: BluetoothReadUntilOptions,
  ): Promise<BluetoothDataResult> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async write(options: BluetoothWriteOptions): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async enableNotifications(
    options: BluetoothEnableNotificationsOptions,
  ): Promise<BluetoothEnableNotificationsResult> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async disableNotifications(
    options: BluetoothDisableNotificationsOptions,
  ): Promise<void> {
    if (!options) {
      return Promise.reject(new OptionsRequiredError());
    }
    throw new Error('Method not implemented.');
  }

  async subscribeToBluetoothStateChanges(): Promise<void> {
    throw new Error('Method not implemented.');
  }
  async unsubscribeFromBluetoothStateChanges(): Promise<void> {
    throw new Error('Method not implemented.');
  }

  addListener(
    eventName: 'onConnectionChange',
    listenerFunc: (state: BluetoothConnectionChangeEvent) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle {
    listenerFunc({
      device: undefined,
      state: BluetoothConnectionState.UNKNOWN,
    });
    throw new Error(`${eventName} Method not implemented.`);
  }
}
