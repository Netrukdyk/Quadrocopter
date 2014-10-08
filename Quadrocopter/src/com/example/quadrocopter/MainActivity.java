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

public class MainActivity extends Activity implements SensorEventListener {
	private SensorManager sensorManager;
	private TextView accelOut, magOut, gyroOut;
	private Sensor magnet, accel, gyro;
	final int matrix_size = 9;
	float[] Rot = new float[matrix_size];
	float[] Inc = new float[matrix_size];

	float[] mags;
	float[] accels;
	float[] gyros;
	
	Ahrs ahrs = new Ahrs();
	BTConnection bt = new BTConnection();
    Handler serverHandler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		accelOut = (TextView) findViewById(R.id.accelOut);
		magOut = (TextView) findViewById(R.id.magOut);
		gyroOut = (TextView) findViewById(R.id.gyroOut);

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magnet,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro,
				SensorManager.SENSOR_DELAY_FASTEST);
//		
//		bt = new BTConnection(); // sukuria bluetooth serverá
//        bt.start();
//        serverHandler = bt.getHandler(); // gaunam serverio handlerá, per kurá siøsim þinutes serveriui
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onSensorChanged(SensorEvent event) {

		switch (event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			mags = event.values.clone();
			break;
		case Sensor.TYPE_GYROSCOPE:
			gyros = event.values.clone();
			break;
		case Sensor.TYPE_ACCELEROMETER:
			accels = event.values.clone();
			break;
		}

		if (mags != null && accels != null && gyros != null) {
			
			Vector3 mag = new Vector3(mags[0], mags[1], mags[2]);
			Vector3 acc = new Vector3(accels[0], accels[1], accels[2]);
			Vector3 gyr = new Vector3(gyros[0], gyros[1], gyros[2]);
			
			accelOut.setText(acc.toString());
			magOut.setText(mag.toString());
			gyroOut.setText(gyr.toString());
			
			ahrs.updateIMU(gyr, acc, 0.01);
			
			gyroOut.setText(ahrs.getQuaternion().toString());
			
			sendToServer(1,"Labas");
			 
		}

	}
	
	// suformuoja praneðimà ir iðsiunèia serveriui
	private void sendToServer(int what, String msgText){
		Bundle b = new Bundle();
		b.putString("Q", msgText);
		Message msg = new Message();
		msg.what = what;
		msg.setData(b);
		if(serverHandler!=null){
			serverHandler.sendMessage(msg);
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
		sensorManager.registerListener(this, magnet,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro,
				SensorManager.SENSOR_DELAY_FASTEST);	}
}
