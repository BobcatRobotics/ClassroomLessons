// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package first.robot.opmodes.teleop;

import first.robot.Robot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Meters;
import static org.wpilib.units.Units.RPM;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

@Teleop
public class DefaultTeleop implements OpMode {
  /**
   * Creates a new default teleop mode. This constructor is called automatically
   * by the OpModeRobot
   * framework when the program starts up.
   *
   * @param robot The robot instance to control.
   */
  public DefaultTeleop(Robot robot) {
    // Spin at a slow speed while the bottom (A) face button is held, and a fast one
    // while the right (B) one is. hold left Bumper to activate these commands
    robot.controller
        .faceDown().and(robot.controller.leftBumper())
        .whileTrue(robot.shooter.setVelocity(RPM.of(2330.0)));
    robot.controller
        .faceRight().and(robot.controller.leftBumper())
        .whileTrue(robot.shooter.setVelocity(RPM.of(4650.0)));
    // Drive the flywheel with a duty cycle while the left (X) or top (Y) face
    // button is held.
    robot.controller.faceLeft().and(robot.controller.leftBumper()).whileTrue(robot.shooter.set(0.3));
    robot.controller.faceUp().and(robot.controller.leftBumper()).whileTrue(robot.shooter.set(-0.3));


    robot.controller
      .faceDown().and(robot.controller.rightBumper())
      .whileTrue(robot.intake.setAngle(Degrees.of(0.0)));
    robot.controller
      .faceRight().and(robot.controller.rightBumper())
      .whileTrue(robot.intake.setAngle(Degrees.of(45.0)));
    // Drive the arm with a duty cycle while the left (X) or top (Y) face button is held.
    robot.controller.faceLeft().and(robot.controller.rightBumper()).whileTrue(robot.intake.setPivotDutyCycle(0.3));
    robot.controller.faceUp().and(robot.controller.rightBumper()).whileTrue(robot.intake.setPivotDutyCycle(-0.3));
  }
  
}
