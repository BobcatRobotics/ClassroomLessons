
package first.robot.mechanisms;

import org.littletonrobotics.junction.AutoLog;

/**
 * Hardware abstraction for the Intake flywheel.
 *
 * <p>Implementations provide real hardware or simulated motor behavior.
 */
public interface IntakeIO {
  /** Inputs measured from the Intake. */
  @AutoLog
  class IntakeIOInputs {
    /** Flywheel position in rotations. */
    public double rollerPositionRotations = 0.0;

    /** Flywheel velocity in rotations per second. */
    public double rollerVelocityRotationsPerSecond = 0.0;

    /** Applied motor voltage in volts. */
    public double rollerAppliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double rollerStatorCurrentAmps = 0.0;

    /** Motor temperature in degrees Celsius. */
    public double rollerTemperatureCelsius = 0.0;

    /** Whether closed-loop velocity control is active. */
    public boolean rollerVelocityControlEnabled = false;

    /** Requested flywheel velocity in rotations per second. */
    public double rollerGoalVelocityRotationsPerSecond = 0.0;

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
   * Updates the supplied inputs with the latest measurements.
   *
   * @param inputs input object to update
   */
  default void updateInputs(IntakeIOInputs inputs) {}

  /**
   * Requests closed-loop velocity control.
   *
   * @param rotationsPerSecond target velocity in rotations per second
   */
  default void setVelocity(double rotationsPerSecond) {}


  /**
   * Commands closed-loop position control.
   *
   * @param positionRadians desired arm angle in radians
   */
  default void setPosition(double positionRadians) {}


  /**
   * Requests open-loop voltage control.
   *
   * @param pivotVolts requested motor voltage for the pivot
   * @param rollerVolts requested motor voltage for the roller
   */
  default void setVoltage(double pivotVolts, double rollerVolts) {}

  /** Stops applying motor voltage. */
  default void stop() {}
}
