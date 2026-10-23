
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.mechanisms;

import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.Seconds;

import java.util.function.Supplier;

import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.epilogue.Logged;
import org.wpilib.units.measure.AngularVelocity;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.controls.DutyCycleOut;
import com.ctre.phoenix6.controls.Follower;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.MotorAlignmentValue;

import first.robot.constants.ShooterConstants;

/**
 * Controls the physical shooter flywheel motors using CTRE Phoenix 6.
 *
 * <p>Implement the hardware initialization, sensor updates, and flywheel commands
 * using the project requirements.
 */
@Logged
public class ShooterIOReal implements Mechanism, ShooterIO {

  /** TalonFX controller responsible for receiving shooter output requests. */
  private final TalonFX flywheelMotorPrimary;

  /** TalonFX controller configured to follow the primary motor in the opposite direction. */
  private final TalonFX flywheelMotorSecondary;

  /** Status signal containing the primary flywheel's measured angular velocity. */
  private StatusSignal<AngularVelocity> velocityOfPrimaryRotPerSecond;

  /** Status signal containing the secondary flywheel's measured angular velocity. */
  private StatusSignal<AngularVelocity> velocityOfSecondaryRotPerSecond;

  /** Reusable duty-cycle control request for the primary flywheel motor. */
  private final DutyCycleOut request = new DutyCycleOut(0);

  /**
   * Creates the real shooter flywheel controller.
   */
  public ShooterIOReal() {
    // TODO 1: Construct the primary TalonFX using ShooterConstants.
    // TODO 2: Construct the secondary TalonFX using ShooterConstants.
    // TODO 3: Configure the secondary motor to follow the primary in the
    //         opposed direction.
  }

  /**
   * Refreshes and copies the flywheel velocity measurements into the input structure.
   *
   * @param inputs input structure to populate with the latest flywheel measurements
   */
  @Override
  public void updateInputs(ShooterIOInputs inputs) {
    // TODO 4: Refresh both motor velocity status signals.
    // TODO 5: Convert both measured velocities to rotations per second.
    // TODO 6: Store both measurements in the input structure.
  }

  /**
   * Creates a command that repeatedly stops the primary flywheel motor.
   *
   * @return a command that repeatedly requests the primary motor to stop
   */
  @Override
  public void stop() {
    // TODO 7: Create a repeatedly executing command that stops the primary motor.
    // TODO 8: Name the command "ShooterFlywheel.Idle".
    return null;
  }

  /**
   * Creates a command that continuously applies a duty-cycle output to the flywheel.
   *
   * @param speed supplier for the desired duty-cycle output, typically in the
   *     range {@code -1.0} to {@code 1.0}
   * @return a command that repeatedly applies the supplied duty-cycle output
   */
  @Override
  public void set(double volts) {
    // TODO 9: Create a repeatedly executing command.
    // TODO 10: Read the current output from the supplier on each execution.
    // TODO 11: Apply the output using the reusable DutyCycleOut request.
    // TODO 12: Name the command "Shooter.Flywheel.RunFlywheelSpeed".
    return null;
  }
}
