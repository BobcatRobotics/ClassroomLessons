package frc.robot.subsystems.Hopper;

import org.littletonrobotics.junction.Logger;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * Subsystem responsible for controlling and monitoring the hopper mechanism.
 *
 * <p>The hopper is controlled using a duty-cycle output ranging from
 * {@code -1.0} to {@code +1.0}. Motor velocity, position, current, voltage,
 * and other measurements are obtained from the {@link HopperIO} interface.
 *
 * <p>The hardware implementation can be swapped between real hardware and
 * simulation without changing the subsystem API.
 */
public class Hopper extends SubsystemBase {

  /** Hardware abstraction used to control and monitor the hopper. */
  private final HopperIO io;

  /** Logged hopper inputs. */
  private final HopperIOInputsAutoLogged inputs =
      new HopperIOInputsAutoLogged();

  /**
   * Creates a new Hopper subsystem.
   *
   * @param io hardware implementation used by the hopper
   */
  public Hopper(HopperIO io) {
    this.io = io;
  }

  /**
   * Updates and logs hopper sensor inputs.
   */
  @Override
  public void periodic() {
    io.periodic();
    io.updateInputs(inputs);

    Logger.processInputs("Hopper/inputs", inputs);
  }

  /**
   * Sets the hopper motor duty cycle.
   *
   * <p>The requested value is automatically constrained to the valid
   * duty-cycle range of {@code -1.0} to {@code +1.0}.
   *
   * @param dutyCycle desired motor output from -1.0 to +1.0
   */
  public void setDutyCycle(double dutyCycle) {
    io.setDutyCycle(
        MathUtil.clamp(dutyCycle, -1.0, 1.0));
  }

  /**
   * Returns the current hopper motor duty cycle.
   *
   * @return current motor duty-cycle output from -1.0 to +1.0
   */
  public double getDutyCycle() {
    return inputs.dutyCycleOutput;
  }

  /**
   * Returns the current hopper motor velocity.
   *
   * @return hopper motor velocity in rotations per second
   */
  public double getVelocity() {
    return inputs.velocityOfHopperMotorRPS;
  }

  /**
   * Returns the current hopper motor position.
   *
   * @return hopper motor position in rotations
   */
  public double getPosition() {
    return inputs.positionOfHopperMotorRotations;
  }

  /**
   * Returns whether the hopper motor controller is connected.
   *
   * @return {@code true} if the motor is connected
   */
  public boolean getIsMotorConnected() {
    return inputs.hopperConnected;
  }

  /**
   * Stops the hopper motor.
   */
  public void stop() {
    io.stop();
  }

  /**
   * Updates the hopper simulation.
   */
  @Override
  public void simulationPeriodic() {
    io.simulationPeriodic();
  }
}
