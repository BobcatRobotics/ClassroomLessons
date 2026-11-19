
package first.robot.mechanisms;

import org.littletonrobotics.junction.AutoLog;

/**
 * Hardware abstraction for the shooter flywheel.
 *
 * <p>Implementations provide real hardware or simulated motor behavior.
 */
public interface ShooterIO {
  /** Inputs measured from the shooter. */
  @AutoLog
  class ShooterIOInputs {
    /** Flywheel position in rotations. */
    public double positionRotations = 0.0;

    /** Flywheel velocity in rotations per second. */
    public double velocityRotationsPerSecond = 0.0;

    /** Applied motor voltage in volts. */
    public double appliedVolts = 0.0;

    /** Motor stator current in amps. */
    public double statorCurrentAmps = 0.0;

    /** Motor temperature in degrees Celsius. */
    public double temperatureCelsius = 0.0;

    /** Whether closed-loop velocity control is active. */
    public boolean velocityControlEnabled = false;

    /** Requested flywheel velocity in rotations per second. */
    public double goalVelocityRotationsPerSecond = 0.0;
  }

  /**
   * Updates the supplied inputs with the latest measurements.
   *
   * @param inputs input object to update
   */
  default void updateInputs(ShooterIOInputs inputs) {}

  /**
   * Requests closed-loop velocity control.
   *
   * @param rotationsPerSecond target velocity in rotations per second
   */
  default void setVelocity(double rotationsPerSecond) {}

  /**
   * Requests open-loop voltage control.
   *
   * @param volts requested motor voltage
   */
  default void setVoltage(double volts) {}

  /** Stops applying motor voltage. */
  default void stop() {}
}
