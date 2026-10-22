
package first.robot.mechanisms;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Volts;

import java.util.Optional;

import org.littletonrobotics.junction.Logger;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Trigger;
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
import org.wpilib.system.RobotController;
import org.wpilib.telemetry.Telemetry;
import org.wpilib.units.measure.AngularVelocity;

/** A shooter flywheel controlled through the AdvantageKit IO layer. */
public class Shooter implements Mechanism {
  private final ShooterIO io;
  private final ShooterIOInputsAutoLogged inputs =
      new ShooterIOInputsAutoLogged();

  private AngularVelocity goalVelocity = null;

  private final Mechanism2d mechanism2d = new Mechanism2d(1, 1);
  private final MechanismLigament2d spokeLigament =
      mechanism2d.getRoot("Axle", 0.5, 0.5)
          .append(new MechanismLigament2d("Flywheel", 0.4, 0.0));

  /**
   * Creates a shooter with the specified IO implementation.
   *
   * @param io real or simulated shooter IO
   */
  public Shooter(ShooterIO io) {
    this.io = io;
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
   * Runs closed-loop velocity control until interrupted.
   *
   * @param velocity target flywheel velocity
   * @return command that maintains the requested velocity
   */
  public Command setVelocity(AngularVelocity velocity) {
    return run(coroutine -> {
      while (true) {
        if (goalVelocity == null || !velocity.isEquivalent(goalVelocity)) {
          io.setVelocity(velocity.in(RotationsPerSecond));
        }

        goalVelocity = velocity;
        coroutine.yield();
      }
    }).named("Shooter Set Velocity");
  }

  /**
   * Drives the flywheel using an open-loop duty cycle.
   *
   * @param dutyCycle output from -1.0 to 1.0
   * @return command that maintains the requested output
   */
  public Command set(double dutyCycle) {
    return run(coroutine -> {
      goalVelocity = null;

      while (true) {
        io.setVoltage(
            RobotController.getMeasureBatteryVoltage()
                .in(Volts) * Math.max(-1.0, Math.min(1.0, dutyCycle)));

        coroutine.yield();
      }
    }).named("Shooter Set Duty Cycle");
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
    return run(coroutine -> {
      goalVelocity = null;

      while (true) {
        io.stop();
        coroutine.yield();
      }
    })
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Shooter Idle");
  }

  /**
   * Refreshes inputs and records telemetry.
   *
   * <p>Call from the robot's regular periodic loop.
   */
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Shooter", inputs);

    spokeLigament.setAngle(
        inputs.positionRotations * 360.0 % 360.0);

    //Logger.recordOutput("Shooter/Mechanism", mechanism2d);
    Logger.recordOutput("Shooter/VelocityRPS",
        inputs.velocityRotationsPerSecond);
    Logger.recordOutput("Shooter/AppliedVolts", inputs.appliedVolts);
    Logger.recordOutput("Shooter/StatorCurrentAmps",
        inputs.statorCurrentAmps);

    if (goalVelocity != null) {
      Telemetry.log("Shooter/GoalVelocity", goalVelocity);
    }
  }

  /** Call this from Robot.simulationPeriodic(). */
  public void simulationPeriodic() {
  }
}
