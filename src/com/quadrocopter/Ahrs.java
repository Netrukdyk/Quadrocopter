package com.quadrocopter;



public class Ahrs {
	final double beta = 0.06;
	
	void reset() {
		state.w = 1.0;
		state.x = 0;
		state.y = 0;
		state.z = 0;				
	}
	
	void update(Vector3 gyro, Vector3 accel, Vector3 mag, double dt) {

		double recipNorm;
		double s0, s1, s2, s3;
		double qDot1, qDot2, qDot3, qDot4;
		double hx, hy;
		double _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

		double q0 = state.w;
		double q1 = state.x;
		double q2 = state.y;
		double q3 = state.z;
		
		// Use IMU algorithm if magnetometer measurement invalid (avoids NaN in magnetometer normalisation)
		if((mag.x == 0.0f) && (mag.y == 0.0f) && (mag.z == 0.0f)) {
			updateIMU(gyro, accel, dt);
			return;
		}

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-q1 * gyro.x - q2 * gyro.y - q3 * gyro.z);
		qDot2 = 0.5f * (q0 * gyro.x + q2 * gyro.z - q3 * gyro.y);
		qDot3 = 0.5f * (q0 * gyro.y - q1 * gyro.z + q3 * gyro.x);
		qDot4 = 0.5f * (q0 * gyro.z + q1 * gyro.y - q2 * gyro.x);

		// Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
		if(!((accel.x == 0.0f) && (accel.y == 0.0f) && (accel.z == 0.0f))) {

			// Normalise accelerometer measurement
//			recipNorm = invSqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z);
//			accel.x *= recipNorm;
//			accel.y *= recipNorm;
//			accel.z *= recipNorm;
			accel.normalize();

			// Normalise magnetometer measurement
//			recipNorm = invSqrt(mag.x * mag.x + mag.y * mag.y + mag.z * mag.z);
//			mag.x *= recipNorm;
//			mag.y *= recipNorm;
//			mag.z *= recipNorm;
			mag.normalize();

			// Auxiliary variables to avoid repeated arithmetic
			_2q0mx = 2.0f * q0 * mag.x;
			_2q0my = 2.0f * q0 * mag.y;
			_2q0mz = 2.0f * q0 * mag.z;
			_2q1mx = 2.0f * q1 * mag.x;
			_2q0 = 2.0f * q0;
			_2q1 = 2.0f * q1;
			_2q2 = 2.0f * q2;
			_2q3 = 2.0f * q3;
			_2q0q2 = 2.0f * q0 * q2;
			_2q2q3 = 2.0f * q2 * q3;
			q0q0 = q0 * q0;
			q0q1 = q0 * q1;
			q0q2 = q0 * q2;
			q0q3 = q0 * q3;
			q1q1 = q1 * q1;
			q1q2 = q1 * q2;
			q1q3 = q1 * q3;
			q2q2 = q2 * q2;
			q2q3 = q2 * q3;
			q3q3 = q3 * q3;

			// Reference direction of Earth's magnetic field
			hx = mag.x * q0q0 - _2q0my * q3 + _2q0mz * q2 + mag.x * q1q1 + _2q1 * mag.y * q2 + _2q1 * mag.z * q3 - mag.x * q2q2 - mag.x * q3q3;
			hy = _2q0mx * q3 + mag.y * q0q0 - _2q0mz * q1 + _2q1mx * q2 - mag.y * q1q1 + mag.y * q2q2 + _2q2 * mag.z * q3 - mag.y * q3q3;
			_2bx = Math.sqrt(hx * hx + hy * hy);
			_2bz = -_2q0mx * q2 + _2q0my * q1 + mag.z * q0q0 + _2q1mx * q3 - mag.z * q1q1 + _2q2 * mag.y * q3 - mag.z * q2q2 + mag.z * q3q3;
			_4bx = 2.0f * _2bx;
			_4bz = 2.0f * _2bz;

			// Gradient decent algorithm corrective step
			s0 = -_2q2 * (2.0f * q1q3 - _2q0q2 - accel.x) + _2q1 * (2.0f * q0q1 + _2q2q3 - accel.y) - _2bz * q2 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mag.x) + (-_2bx * q3 + _2bz * q1) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - mag.y) + _2bx * q2 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mag.z);
			s1 = _2q3 * (2.0f * q1q3 - _2q0q2 - accel.x) + _2q0 * (2.0f * q0q1 + _2q2q3 - accel.y) - 4.0f * q1 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - accel.z) + _2bz * q3 * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mag.x) + (_2bx * q2 + _2bz * q0) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - mag.y) + (_2bx * q3 - _4bz * q1) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mag.z);
			s2 = -_2q0 * (2.0f * q1q3 - _2q0q2 - accel.x) + _2q3 * (2.0f * q0q1 + _2q2q3 - accel.y) - 4.0f * q2 * (1 - 2.0f * q1q1 - 2.0f * q2q2 - accel.z) + (-_4bx * q2 - _2bz * q0) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mag.x) + (_2bx * q1 + _2bz * q3) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - mag.y) + (_2bx * q0 - _4bz * q2) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mag.z);
			s3 = _2q1 * (2.0f * q1q3 - _2q0q2 - accel.x) + _2q2 * (2.0f * q0q1 + _2q2q3 - accel.y) + (-_4bx * q3 + _2bz * q1) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mag.x) + (-_2bx * q0 + _2bz * q2) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - mag.y) + _2bx * q1 * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mag.z);
			recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
			s0 *= recipNorm;
			s1 *= recipNorm;
			s2 *= recipNorm;
			s3 *= recipNorm;

			// Apply feedback step
			qDot1 -= beta * s0;
			qDot2 -= beta * s1;
			qDot3 -= beta * s2;
			qDot4 -= beta * s3;
		}

		// Integrate rate of change of quaternion to yield quaternion
		q0 += qDot1 * dt;
		q1 += qDot2 * dt;
		q2 += qDot3 * dt;
		q3 += qDot4 * dt;

		// Normalise quaternion
		recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
		q0 *= recipNorm;
		q1 *= recipNorm;
		q2 *= recipNorm;
		q3 *= recipNorm;
		
		state.w = q0;
		state.x = q1;
		state.y = q2;
		state.z = q3;
	}

	//---------------------------------------------------------------------------------------------------
	// IMU algorithm update

	void updateIMU(Vector3 gyro, Vector3 accel, double dt) {
		double recipNorm;
		double s0, s1, s2, s3;
		double qDot1, qDot2, qDot3, qDot4;
		double _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

		double q0 = state.w;
		double q1 = state.x;
		double q2 = state.y;
		double q3 = state.z;

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-q1 * gyro.x - q2 * gyro.y - q3 * gyro.z);
		qDot2 = 0.5f * (q0 * gyro.x + q2 * gyro.z - q3 * gyro.y);
		qDot3 = 0.5f * (q0 * gyro.y - q1 * gyro.z + q3 * gyro.x);
		qDot4 = 0.5f * (q0 * gyro.z + q1 * gyro.y - q2 * gyro.x);

		// Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
		if(!((accel.x == 0.0f) && (accel.y == 0.0f) && (accel.z == 0.0f))) {

			// Normalise accelerometer measurement
			recipNorm = invSqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z);
			accel.x *= recipNorm;
			accel.y *= recipNorm;
			accel.z *= recipNorm;
			//accel.normalize();

			// Auxiliary variables to avoid repeated arithmetic
			_2q0 = 2.0f * q0;
			_2q1 = 2.0f * q1;
			_2q2 = 2.0f * q2;
			_2q3 = 2.0f * q3;
			_4q0 = 4.0f * q0;
			_4q1 = 4.0f * q1;
			_4q2 = 4.0f * q2;
			_8q1 = 8.0f * q1;
			_8q2 = 8.0f * q2;
			q0q0 = q0 * q0;
			q1q1 = q1 * q1;
			q2q2 = q2 * q2;
			q3q3 = q3 * q3;

			// Gradient decent algorithm corrective step
			s0 = _4q0 * q2q2 + _2q2 * accel.x + _4q0 * q1q1 - _2q1 * accel.y;
			s1 = _4q1 * q3q3 - _2q3 * accel.x + 4.0f * q0q0 * q1 - _2q0 * accel.y - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * accel.z;
			s2 = 4.0f * q0q0 * q2 + _2q0 * accel.x + _4q2 * q3q3 - _2q3 * accel.y - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * accel.z;
			s3 = 4.0f * q1q1 * q3 - _2q1 * accel.x + 4.0f * q2q2 * q3 - _2q2 * accel.y;

			recipNorm = invSqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
			s0 *= recipNorm;
			s1 *= recipNorm;
			s2 *= recipNorm;
			s3 *= recipNorm;

			// Apply feedback step
			qDot1 -= beta * s0;
			qDot2 -= beta * s1;
			qDot3 -= beta * s2;
			qDot4 -= beta * s3;
		}

		// Integrate rate of change of quaternion to yield quaternion
		q0 += qDot1 * dt;
		q1 += qDot2 * dt;
		q2 += qDot3 * dt;
		q3 += qDot4 * dt;

		// Normalise quaternion
		recipNorm = invSqrt(q0 * q0 + q1 * q1 + q2 * q2 + q3 * q3);
		q0 *= recipNorm;
		q1 *= recipNorm;
		q2 *= recipNorm;
		q3 *= recipNorm;

		state.w = q0;
		state.x = q1;
		state.y = q2;
		state.z = q3;

	}
	
	Quaternion getQuaternion() {
		return state;
	}
	
	private Quaternion state = new Quaternion(1.0, 0, 0, 0);

	double invSqrt(double x) {
		return 1 / Math.sqrt(x);
	}

	
}
