package com.example.quadrocopter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BTConnection {

	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Handler uiHandler, serverHandler;
	boolean connected = false;
	String deviceName = "Arduino";

	// Message types
	public static final int MSG_STATE_CHANGE = 1;
	public static final int MSG_TOAST = 2;

	// --- Konstruktoriai -------------------------------------------------

	public BTConnection(Handler handler) {
		this.uiHandler = handler;
	}

	public BTConnection(Handler handler, String deviceName) {
		this.uiHandler = handler;
		this.deviceName = deviceName;
	}

	// --- Public Metordai----------------------------------------------------

	public void connect() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
			sendToUI(MSG_TOAST, "No bluetooth adapter available");
			return;
		}
		if (!mBluetoothAdapter.isEnabled()) {
			sendToUI(MSG_TOAST, "Bluetooth disabled");
			return;
		}

		if (mmDevice == null)
			findBtDevice();

		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

		// Thread
		try {
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
			Log.v("BT","Ok");
			sendToUI(MSG_TOAST, "Connected");
			connected = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(String data) {
		if (connected) {
			// Thread
			String msg = data + "\r\n";
			try {
				if (mmOutputStream != null) {
					mmOutputStream.write(msg.getBytes());
				} else
					sendToUI(MSG_TOAST, "Ávyko klaida");
			} catch (IOException e) {
				sendToUI(MSG_TOAST, "Ávyko klaida");
				e.printStackTrace();
			}
		}

	}

	public void disconnect() {
		try {
			if (mmOutputStream != null)
				mmOutputStream.close();
			if (mmInputStream != null)
				mmInputStream.close();
			if (mmDevice != null)
				mmDevice = null;
			if (mmSocket != null)
				mmSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		connected = false;
		sendToUI(MSG_TOAST, "Bluetooth disconnected");
	}

	// --- Private Metodai--------------------------------------------------

	private void findBtDevice() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

		if (pairedDevices.size() == 0) {
			sendToUI(MSG_TOAST, "No paired devices");
			return;
		}

		for (BluetoothDevice device : pairedDevices) {
			if (device.getName().equals(deviceName)) {
				mmDevice = device;
				break;
			}
		}
	}

	private void sendToUI(int what, String msgText) {
		Bundle b = new Bundle();
		b.putString("MSG", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		uiHandler.sendMessage(msg);
	}
}