// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;
import static org.wpilib.units.Units.Degrees;

import org.wpilib.command3.Scheduler;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.epilogue.Epilogue;
import org.wpilib.epilogue.Logged;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.framework.RobotBase;

import first.robot.mechanisms.Intake;
import first.robot.mechanisms.IntakeIOReal;
import first.robot.mechanisms.IntakeIOSim;
import first.robot.mechanisms.Shooter;
import first.robot.mechanisms.ShooterIOReal;
import first.robot.mechanisms.ShooterIOSim;

@Logged
public class Robot extends OpModeRobot {
  public final Shooter shooter;
  public final Intake intake;
  public final CommandGamepad controller = new CommandGamepad(
      0);
  // Store the scheduler in a field for Epilogue to pick up and log for us
  private final Scheduler scheduler = Scheduler.getDefault();

  /**
   * Initializes the robot class and sets safe default commands for all its
   * mechanisms.
   */
  public Robot() {
    if (RobotBase.isSimulation()) {
      shooter = new Shooter(new ShooterIOReal());
      intake = new Intake(new IntakeIOReal());
    } else {
      shooter = new Shooter(new ShooterIOSim());
      intake = new Intake(new IntakeIOSim());
    }

    shooter.setDefaultCommand(shooter.idle());
    intake.setDefaultCommand(intake.hold(Degrees.of(0.0)));
  }
 
  @Override
  public void robotPeriodic() {
    // Update Subsystem Periodics
    shooter.periodic();
    intake.periodic();
    // 1. Run the scheduler to poll triggers and execute our commands.
    scheduler.run();

    // 2. Update telemetry.
    Epilogue.update(this);
  }

  @Override
  public void simulationPeriodic() {
    shooter.simulationPeriodic();
    intake.simulationPeriodic();
  }
}
