// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot;
import org.wpilib.command3.Scheduler;
import org.wpilib.command3.button.CommandGamepad;
import org.wpilib.epilogue.Epilogue;
import org.wpilib.epilogue.Logged;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.framework.RobotBase;

import first.robot.mechanisms.Shooter;
import first.robot.mechanisms.ShooterIOReal;
import first.robot.mechanisms.ShooterIOSim;

@Logged
public class Robot extends OpModeRobot {
  public final Shooter shooter;
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
    } else {
      shooter = new Shooter(new ShooterIOSim());
    }

    shooter.setDefaultCommand(shooter.idle());
  }
 
  @Override
  public void robotPeriodic() {
    // Update Subsystem Periodics
    shooter.periodic();
    // 1. Run the scheduler to poll triggers and execute our commands.
    scheduler.run();

    // 2. Update telemetry.
    Epilogue.update(this);
  }

  @Override
  public void simulationPeriodic() {
  }
}
