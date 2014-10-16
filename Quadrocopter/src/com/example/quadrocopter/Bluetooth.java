package com.example.quadrocopter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Bluetooth {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice;
	private ConnectThread mConnectThread;
	public ConnectedThread mConnectedThread;
	String name;

	public Bluetooth(String name) {
		this.name = name;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		getDevice();
		mConnectThread = new ConnectThread(mDevice);
		mConnectThread.start();
	}

	public void write(byte[] data) {
		if (mConnectedThread != null)
			mConnectedThread.write(data);
	}
	public void flush() {
		if (mConnectedThread != null)
			mConnectedThread.flush();
	}
	public void getDevice() {
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		if (pairedDevices.size() > 0)
			for (BluetoothDevice device : pairedDevices)
				if (device.getName().equals(name)){
					mDevice = device;
					break;
				}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private BluetoothSocket mSocket;
		private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
		public ConnectThread(BluetoothDevice device) {
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
			}
			mmSocket = tmp;
		}
		public void run() {
			mBluetoothAdapter.cancelDiscovery();
			try {
				mmSocket.connect();
			} catch (IOException connectException) {
				try {
					if(mSocket != null) mSocket.close();
				} catch (IOException closeException) {
				}
				return;
			}
			Log.v("bt", "new");
			mConnectedThread = new ConnectedThread(mmSocket);
			mConnectedThread.start();
		}
		public void cancel() {
			try {
				if(mmSocket != null) mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	class ConnectedThread extends Thread {
		Boolean running = true;
		private final BluetoothSocket mmSocket;
		private final BufferedOutputStream mmOutStream;
		ArrayBlockingQueue<Byte> data;
		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			BufferedOutputStream tmpOut = null;
			try {

				tmpOut = new BufferedOutputStream(mmSocket.getOutputStream());
			} catch (IOException e) {
			}
			mmOutStream = tmpOut;
			data = new ArrayBlockingQueue<Byte>(1024);

		}
		public void run() {
			while (running) {
				try {
					byte b = data.poll(1, TimeUnit.DAYS);
					mmOutStream.write(b);
					if (b == '\n')
						mmOutStream.flush();
				} catch (Exception e) {
				}
			}
		}
		public void write(byte[] bytes) {
			for (byte b : bytes)
				try {
					data.put(b);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		public void flush() {
			try {
				mmOutStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		public void cancel() {
			running = false;
			try {
				if(mmSocket != null) mmSocket.close();
			} catch (IOException e) {
			}
		}

	}

}
