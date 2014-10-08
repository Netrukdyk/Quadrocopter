/* Handleriø naudojami praneðimø kodai: 
 * 0 - Valdomo árenginio praneðimai
 * 1 - Valdymo komandos ið UI
 * 2 - Bluetooth ryðio praneðimai
 * 3 - Kiti praneðimai
 */
package com.example.quadrocopter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class BTConnection extends Thread {

	BluetoothAdapter mBluetoothAdapter;
	BluetoothSocket mmSocket;
	BluetoothDevice mmDevice;
	OutputStream mmOutputStream;
	InputStream mmInputStream;
	Thread workerThread;
	byte[] readBuffer;
	int readBufferPosition;
	int counter;
	volatile boolean stopWorker;

	private Handler uiHandler; // UI Handleris

	int server_state = 0;
	final String BTNAME = "Mindis-PC"; // !!!!!!!!!!!!!!!!!!!!!!!!!!! <<<

	// Serverio Handleris, apdoroja þinutes ið UI
	@SuppressLint("HandlerLeak")
	private Handler serverHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				Log.v("Server", msg.getData().getString("Q"));
				try {
					String Q = msg.getData().getString("Q");
					sendData(Q);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};

	private void connect() {
		server_state = 0;
		UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // 
		try {
			mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
			mmSocket.connect();
			mmOutputStream = mmSocket.getOutputStream();
			mmInputStream = mmSocket.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			return; // kai ávyksta klaida reikia nutraukti metodà, nes vëliau
					// nustatoma teigiama serverio bûsena
		}
		server_state = 1;
	}

	private void findBT() {
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			sendToUI(2, "No bluetooth adapter available");
		} else if (!mBluetoothAdapter.isEnabled()) {
			sendToUI(3, "Enable bluetooth");
		} else {
			Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
					.getBondedDevices();
			if (pairedDevices.size() > 0) {
				for (BluetoothDevice device : pairedDevices) {
					if (device.getName().equals(BTNAME)) {
						mmDevice = device;
						break;
					}
				}
			}
		}
	}

	public void run() {
		Looper.prepare();

		findBT();

		do
			connect();
		while (server_state != 1);

		final Handler handler = new Handler();
		final byte delimiter = 10; // This is the ASCII code for a newline
									// character

		stopWorker = false;
		readBufferPosition = 0;
		readBuffer = new byte[1024];
		workerThread = new Thread(new Runnable() {
			public void run() {
				while (!Thread.currentThread().isInterrupted() && !stopWorker) {
					try {
						int bytesAvailable = mmInputStream.available();
						if (bytesAvailable > 0) {
							byte[] packetBytes = new byte[bytesAvailable];
							mmInputStream.read(packetBytes);
							for (int i = 0; i < bytesAvailable; i++) {
								byte b = packetBytes[i];
								if (b == delimiter) {
									byte[] encodedBytes = new byte[readBufferPosition];
									System.arraycopy(readBuffer, 0,
											encodedBytes, 0,
											encodedBytes.length);
									final String data = new String(
											encodedBytes, "US-ASCII");
									readBufferPosition = 0;

									handler.post(new Runnable() {
										public void run() {
											Log.v("server to UI", data);
											sendToUI(0, data);
										}
									});
								} else {
									readBuffer[readBufferPosition++] = b;
								}
							}
						}
					} catch (IOException ex) {
						stopWorker = true;
					}
				}
			}
		});

		workerThread.start();
		Looper.loop();
	}

	private void sendData(final String Q) throws IOException {
		final Runnable r = new Runnable() {
			public void run() {
				String msg = Q + "\r\n";
				try {
					if (mmOutputStream != null) {
						mmOutputStream.write(msg.getBytes());
						sendToUI(2, "Data Sent");
					} else
						sendToUI(2, "Ávyko klaida");
				} catch (IOException e) {
					sendToUI(2, "Ávyko klaida");
					e.printStackTrace();
				}
			}
		};
		serverHandler.post(r);
	}

	private void closeBT() throws IOException {
		stopWorker = true;
		if (mmOutputStream != null)
			mmOutputStream.close();
		if (mmInputStream != null)
			mmInputStream.close();
		if (mmSocket != null)
			mmSocket.close();
		sendToUI(2, "Bluetooth Closed");
		server_state = 0;
	}

	// Metodas tam, kad gautume serverio handlerá
	public Handler getHandler() {
		return serverHandler;
	}

	// Suformuoja þinutæ ir iðsiunèia UI
	private void sendToUI(int what, String msgText) {
		Bundle b = new Bundle();
		b.putString("message", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		//uiHandler.sendMessage(msg);
	}

} // End of BTConnection