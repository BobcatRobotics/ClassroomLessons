
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.mechanisms;

import static org.wpilib.units.Units.Meters;

import java.util.Optional;
import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Trigger;
import org.wpilib.math.interpolation.InterpolatingTreeMap;
import org.wpilib.system.RobotController;
import org.wpilib.units.measure.AngularVelocity;
import org.wpilib.units.measure.Distance;

import first.robot.lookup.LookupTables;

/**
 * Coordinates the shooter's flywheel and shooting behavior.
 *
 * TODO: Complete the implementation using the exercise instructions.
 */
public class Shooter implements Mechanism {

  /** Hardware abstraction used to update shooter input data. */
  private final ShooterIO io;

  /** Automatically logged representation of the shooter's input data. */
  private final ShooterIOInputsAutoLogged inputs = new ShooterIOInputsAutoLogged();

  /**
   * Maps target distance in meters to the corresponding flywheel duty-cycle output.
   */
  private static final InterpolatingTreeMap<Distance, Double> FLYWHEEL_LUT =
      new InterpolatingTreeMap<>(
          LookupTables.inverseUnitInterpolator(), LookupTables.doubleInterpolator());

  static {
    FLYWHEEL_LUT.put(Meters.of(0), 1.0);
    FLYWHEEL_LUT.put(Meters.of(0.5), 0.8);
    FLYWHEEL_LUT.put(Meters.of(1), 0.6);
    FLYWHEEL_LUT.put(Meters.of(1.5), 0.4);
    FLYWHEEL_LUT.put(Meters.of(2), 0.2);
    FLYWHEEL_LUT.put(Meters.of(2.5), 0.0);
  }

  /**
   * Creates a shooter mechanism using the specified I/O implementation.
   *
   * @param io the shooter I/O implementation used to retrieve input data
   */
  public Shooter(ShooterIO io) {
    // TODO 1: Store the supplied I/O implementation.
    // TODO 2: Register the idle command as the default command.
  }


  /** Returns the measured flywheel velocity. */
  public AngularVelocity getVelocity() {
    return RotationsPerSecond.of(inputs.velocityRotationsPerSecond);
  }

  /** Returns the requested velocity when velocity control is active. */
  public Optional<AngularVelocity> getGoalVelocity() {
    return Optional.ofNullable(goalVelocity);
  }

  /** Returns the measured motor voltage. */
  public org.wpilib.units.measure.Voltage getVoltage() {
    return Volts.of(inputs.appliedVolts);
  }

  /** Returns the measured motor stator current. */
  public org.wpilib.units.measure.Current getCurrent() {
    return org.wpilib.units.Units.Amps.of(inputs.statorCurrentAmps);
  }
  

  /**
   * Updates shooter input data and records it using AdvantageKit.
   */
  public void periodic() {
    // TODO 3: Update the input structure using the I/O implementation.
    // TODO 4: Record the inputs with AdvantageKit.
  }


  /**
   * Drives the flywheel using an open-loop duty cycle.
   *
   * @param dutyCycle output from -1.0 to 1.0
   * @return command that maintains the requested output
   */
  public Command set(double dutyCycle) {
    return null;
  }

  /**
   * Creates a trigger that becomes active near a target velocity.
   *
   * @param velocity target velocity
   * @param tolerance acceptable velocity error
   * @return velocity tolerance trigger
   */
  public Trigger near(
      AngularVelocity velocity, AngularVelocity tolerance) {
    return new Trigger(() -> getVelocity().isNear(velocity, tolerance));
  }

  /**
   * Coasts or brakes at zero applied voltage at the lowest command priority.
   *
   * @return idle command
   */
  public Command idle() {
    return null;
  }
  /** Call this from Robot.simulationPeriodic(). */
  public void simulationPeriodic() {
  }
}

