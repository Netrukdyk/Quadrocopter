package com.example.quadrocopter;

import android.annotation.SuppressLint;

@SuppressLint("DefaultLocale")
public class Quaternion {
	public double w;
	public double x;
	public double y;
	public double z;
	
	public Quaternion(double w, double x, double y, double z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public String toString() {
		return String.format("Q(%.3f,%.3f,%.3f,%.3f)", w, x, y, z);
	}
	
	public Vector3 getGravity() {
		Quaternion q = this;
	    Vector3 v = new Vector3(0, 0, 0);
		v.x = 2 * (q.x*q.z - q.w*q.y);
	    v.y = 2 * (q.w*q.x + q.y*q.z);
	    v.z = q.w*q.w - q.x*q.x - q.y*q.y + q.z*q.z;
	    return v;
	}
	

	public Vector3 getYawPitchRoll() {
		Vector3 data = new Vector3(0, 0, 0);
		Vector3 gravity = getGravity();
		Quaternion q = this;
		
		// yaw: (about Z axis)
		data.x = Math.atan2(2*q.x*q.y - 2*q.w*q.z, 2*q.w*q.w + 2*q.x*q.x - 1);

		//pitch
		data.y = Math.atan(gravity.y / gravity.z);
		if (gravity.z < 0.0)
		{
			if(gravity.y > 0.0) {
				data.y += Math.PI;
			} else {
				data.y -= Math.PI;
			}
		}

		//roll
		data.z = -Math.atan(gravity.x / gravity.z);
		if (gravity.z < 0.0)
		{
			if(gravity.x > 0.0) {
				data.z -= Math.PI;
			} else {
				data.z += Math.PI;
			}
		}

		return data;
	}
}