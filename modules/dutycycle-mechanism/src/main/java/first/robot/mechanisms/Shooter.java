
// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.mechanisms;

import static org.wpilib.units.Units.Meters;

import java.util.function.Supplier;

import org.littletonrobotics.junction.Logger;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.math.interpolation.InterpolatingTreeMap;
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

  /** Implementation responsible for controlling the flywheel hardware. */
  private final ShooterReal flywheel = new ShooterReal();

  /**
   * Creates a shooter mechanism using the specified I/O implementation.
   *
   * @param io the shooter I/O implementation used to retrieve input data
   */
  public Shooter(ShooterIO io) {
    // TODO 1: Store the supplied I/O implementation.
    // TODO 2: Register the idle command as the default command.
  }

  /**
   * Updates shooter input data and records it using AdvantageKit.
   */
  public void periodic() {
    // TODO 3: Update the input structure using the I/O implementation.
    // TODO 4: Record the inputs with AdvantageKit.
  }

  /**
   * Creates the shooter's idle command.
   *
   * @return a command that places the flywheel in its idle state
   */
  @Override
  public Command idle() {
    // TODO 5: Create a command that runs the flywheel's idle behavior.
    // TODO 6: Name the command "Shooter.Idle".
    return null;
  }

  /**
   * Creates a command that shoots toward the hub using the supplied target distance.
   *
   * @param targetDistance supplier for the distance from the center of the robot
   *     to the center of the target
   * @return a command that runs the flywheel at the selected duty-cycle output
   */
  public Command shoot(Supplier<Distance> targetDistance) {
    // TODO 7: Create a Supplier<Double> that looks up the flywheel output
    //         for the supplied target distance.
    // TODO 8: Create a command that runs the flywheel using that supplier.
    // TODO 9: Name the command "Shooter.Shoot".
    return null;
  }
}
