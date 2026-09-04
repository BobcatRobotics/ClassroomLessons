package frc.robot.subsystems.Hopper;

import org.littletonrobotics.junction.AutoLog;

/**
 * Hardware abstraction interface for the hopper subsystem.
 *
 * <p>This interface separates the hopper subsystem logic from the underlying
 * hardware implementation. Implementations can provide real hardware,
 * simulation, or other implementations without requiring changes to the
 * {@link Hopper} subsystem.
 */
public interface HopperIO {

  /**
   * Container for sensor and status information reported by the hopper.
   *
   * <p>The {@link AutoLog} annotation generates the
   * {@code HopperIOInputsAutoLogged} class used by AdvantageKit.
   */
  @AutoLog
  class HopperIOInputs {
  }

  /**
   * Updates the input values from the hopper hardware.
   *
   * @param inputs object to populate with the latest hardware measurements
   */
  default void updateInputs(HopperIOInputs inputs) {
  }

  /**
   * Sets the hopper motor duty-cycle output.
   *
   * <p>A value of {@code 1.0} represents full forward output,
   * {@code -1.0} represents full reverse output, and {@code 0.0}
   * represents no motor output.
   *
   * @param dutyCycle desired motor output from -1.0 to +1.0
   */
  default void setDutyCycle(double dutyCycle) {
  }

  /**
   * Stops the hopper motor.
   */
  default void stop() {
  }

  /**
   * Performs periodic processing for the hopper hardware.
   */
  default void periodic() {
  }

  /**
   * Performs periodic processing for the hopper simulation.
   */
  default void simulationPeriodic() {
  }
}

