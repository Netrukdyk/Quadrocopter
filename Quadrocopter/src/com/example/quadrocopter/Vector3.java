package com.example.quadrocopter;

public class Vector3 {
	public double x;
	public double y;
	public double z;
	
	public Vector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void normalize() {
		double l = Math.sqrt(x*x + y*y + z*z);
		x /= l;
		y /= l;
		z /= l;
	}
	
	public String toString() {
		return String.format("V(%.3f,%.3f,%.3f)", x, y, z);
	}
}