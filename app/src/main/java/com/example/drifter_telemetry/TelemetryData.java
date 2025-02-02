package com.example.drifter_telemetry;

public class TelemetryData {
    public String arm_enabled;
    public String steering_mode;
    public String drive_mode;
    public String battery_status;
    public int[] motor_rpm = new int[4]; // Corrected array declaration
    public float[] motor_throttle = new float[4]; // Corrected array declaration
    public float battery_voltage;
    public int[] steering_values = new int[2]; // Corrected array declaration
    public float g_force_x;
    public float g_force_y;
    public float rotational_rate;
}