package com.example.quadrocopter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {
	private SensorManager sensorManager;
	private TextView out1, out2, out3;
	private Sensor magnet, accel, gyro;

	final int matrix_size = 9;
	float[] Rot = new float[matrix_size];
	float[] Inc = new float[matrix_size];

	float[] mags;
	float[] accels;
	float[] gyros;

	Ahrs ahrs = new Ahrs();
	BTConnection blueTooth;
	Handler serverHandler, uiHandler;

	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		out1 = (TextView) findViewById(R.id.out1);
		out2 = (TextView) findViewById(R.id.out2);
		out3 = (TextView) findViewById(R.id.out3);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);

		blueTooth = new BTConnection(uihHandler);
		blueTooth.connect();
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onSensorChanged(SensorEvent event) {

		switch (event.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD :
				mags = event.values.clone();
				break;
			case Sensor.TYPE_GYROSCOPE :
				gyros = event.values.clone();
				break;
			case Sensor.TYPE_ACCELEROMETER :
				accels = event.values.clone();
				break;
		}

		if (mags != null && accels != null && gyros != null) {

			@SuppressWarnings("unused")
			Vector3 mag = new Vector3(mags[0], mags[1], mags[2]);
			Vector3 acc = new Vector3(accels[0], accels[1], accels[2]);
			Vector3 gyr = new Vector3(gyros[0], gyros[1], gyros[2]);

			ahrs.updateIMU(gyr, acc, 0.01);
			String q = ahrs.getQuaternion().toString();
			out3.setText(q);
			blueTooth.send(q);

		}

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler uihHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case BTConnection.MSG_TOAST :
					Toast.makeText(getApplicationContext(), msg.getData().getString("MSG"), Toast.LENGTH_SHORT).show();
					break;
			}
		}
	};
}
