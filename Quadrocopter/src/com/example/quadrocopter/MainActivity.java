package com.example.quadrocopter;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.mavlink.messages.ardupilotmega.msg_attitude_quaternion;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener, OnTouchListener {
	private SensorManager sensorManager;
	private TextView out1, out2, out3;
	private Sensor magnet, accel, gyro;

	final int matrix_size = 9;
	float[] Rot = new float[matrix_size];
	float[] Inc = new float[matrix_size];

	float[] mags;
	float[] accels;
	float[] gyros;

	static ByteBuffer sendBuffer;

	Ahrs ahrs = new Ahrs();
	int speed = SensorManager.SENSOR_DELAY_FASTEST;
	Bluetooth bt;
	long lastSent, lastEvent;
	Vector3 mag, acc, gyr;
	float dt;
	Quaternion q;
	msg_attitude_quaternion mavMsg;
	byte [] bluetoothBuffer;
	ImageButton bigButton;
	float active;
	Boolean simulator = true;
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_main);

		out1 = (TextView) findViewById(R.id.out1);
		out2 = (TextView) findViewById(R.id.out2);
		out3 = (TextView) findViewById(R.id.out3);
		bigButton = (ImageButton) findViewById(R.id.bigButton);
		bigButton.setOnTouchListener(this);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accel, speed);
		sensorManager.registerListener(this, magnet, speed);
		sensorManager.registerListener(this, gyro, speed);
		sendBuffer = ByteBuffer.allocate(18);
		bt = (simulator) ? new Bluetooth("Arduino") : new Bluetooth("RemoteControl");
	}
	
	int sequence = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (lastEvent == 0)
			lastEvent = event.timestamp;
		
		switch (event.sensor.getType()) {
			case Sensor.TYPE_MAGNETIC_FIELD :
				mag = new Vector3(-event.values[0], -event.values[1], -event.values[2]);
				break;
			case Sensor.TYPE_GYROSCOPE :
				gyr = new Vector3(event.values[0], event.values[1], event.values[2]);
				break;
			case Sensor.TYPE_ACCELEROMETER :
				acc = new Vector3(event.values[0], event.values[1], event.values[2]);
				break;
		}
		
		if (mag != null && acc != null && gyr != null) {
			if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
				dt = (float) ((event.timestamp - lastEvent) / 1000000000.0);
				ahrs.update(gyr, acc, mag, dt);
				//if (out1.getText() == null)
					out1.setText(String.valueOf(100 / (Math.round(dt * 100))) + " Hz Sensor");
				
				lastEvent = event.timestamp;
			}

			if (lastSent == 0)
				lastSent = event.timestamp;

			if (event.timestamp - lastSent >= 70000000) {

				q = ahrs.getQuaternion();
				out3.setText(ahrs.getQuaternion().toString());
				
				mavMsg = new msg_attitude_quaternion(0, 0);
				mavMsg.sequence = sequence++;
				mavMsg.rollspeed = active;
				mavMsg.q1 = (float) q.w;
				mavMsg.q2 = (float) q.x;
				mavMsg.q3 = (float) q.y;
				mavMsg.q4 = (float) q.z;
				try {
					bluetoothBuffer = mavMsg.encode();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				 bt.write(bluetoothBuffer);
				 bt.flush();				 

				if (out2.getText() == "")
					out2.setText(String.valueOf((1000000000 / (event.timestamp - lastSent))) + " Hz Bluetooth");

				lastSent = event.timestamp;
			}
		}
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onPause() {
		super.onPause();
		// sensorManager.unregisterListener(this);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		if(bt != null & bt.mConnectedThread != null)
			bt.mConnectedThread.cancel();
	}
	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, magnet, speed);
		sensorManager.registerListener(this, accel, speed);
		sensorManager.registerListener(this, gyro, speed);
	}
	@Override
	public boolean onTouch(View v, MotionEvent event) {

	    switch (event.getAction() & MotionEvent.ACTION_MASK) {

	    case MotionEvent.ACTION_DOWN:
	        v.setPressed(true);
	        active = 1.0f;
	        Log.v("btn","pressed");
	        break;
	    case MotionEvent.ACTION_UP:
	    case MotionEvent.ACTION_OUTSIDE:
	        v.setPressed(false);
	        active = 0.0f;
	        Log.v("btn","unpressed");
	        break;
	    case MotionEvent.ACTION_POINTER_DOWN:
	        break;
	    case MotionEvent.ACTION_POINTER_UP:
	        break;
	    case MotionEvent.ACTION_MOVE:
	        break;
	    }

	    return true;
	}

}
