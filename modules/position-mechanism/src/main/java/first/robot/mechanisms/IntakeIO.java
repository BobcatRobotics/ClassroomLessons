
package first.robot.mechanisms;

import org.littletonrobotics.junction.AutoLog;

/**
 * Hardware abstraction for a single-jointed intake arm.
 *
 * <p>All position values are in radians, angular velocities are in radians per second,
 * and voltage, current, and temperature are expressed in SI units.
 */
public interface IntakeIO {

  /** Sensor values and mechanism telemetry updated by the IO implementation. */
  @AutoLog
  class IntakeIOInputs {
    /** Integrated arm position in radians. */
    public double pivotPositionRadians = 0.0;

    /** Integrated arm velocity in radians per second. */
    public double pivotVelocityRadiansPerSecond = 0.0;

    /** Applied motor voltage in volts. */
    public double pivotAppliedVolts = 0.0;

    /** Motor stator current in amperes. */
    public double pivotCurrentAmps = 0.0;

    /** Motor temperature in degrees Celsius. */
    public double pivotTemperatureCelsius = 0.0;

    /** Absolute encoder reading in rotations, before offset correction. */
    public double pivotAbsoluteEncoderRotations = 0.0;

    /** Absolute encoder angle in radians, after offset correction. */
    public double pivotAbsoluteAngleRadians = 0.0;

    /** Whether the arm is currently using position control. */
    public boolean pivotPositionControlEnabled = false;

    /** Active position goal in radians. */
    public double pivotGoalRadians = 0.0;
  }

  /**
   * Refreshes all available input values.
   *
   * @param inputs object to populate
   */
  default void updateInputs(IntakeIOInputs inputs) {}

  /**
   * Commands closed-loop position control.
   *
   * @param positionRadians desired arm angle in radians
   */
  default void setPosition(double positionRadians) {}

  /**
   * Commands an open-loop motor voltage.
   *
   * @param volts requested voltage
   */
  default void setVoltage(double volts) {}

  /** Stops the motor and disables position control. */
  default void stop() {}

  /**
   * Advances the mechanism simulation, if supported.
   *
   * @param periodSeconds simulation timestep in seconds
   * @param batteryVoltage current simulated battery voltage
   */
  default void updateSim(double periodSeconds, double batteryVoltage) {}
}
