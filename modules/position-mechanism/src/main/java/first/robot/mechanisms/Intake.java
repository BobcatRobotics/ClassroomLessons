
package first.robot.mechanisms;

import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.Volts;

import java.util.Optional;

import org.littletonrobotics.junction.Logger;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Trigger;
import org.wpilib.system.RobotController;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Voltage;

import first.robot.constants.IntakeConstants;

/**
 * Intake arm mechanism backed by an AdvantageKit IO implementation.
 *
 * <p>Angles are measured from horizontal. Zero points straight out, and positive
 * angles raise the arm.
 */
public class Intake implements Mechanism {


  private static final double LOOP_PERIOD_SECONDS = 0.020;

  private final IntakeIO io;
  private final IntakeIOInputsAutoLogged inputs =
      new IntakeIOInputsAutoLogged();

  private Angle goalAngle;

  /**
   * Creates an intake mechanism.
   *
   * @param io real, simulated, or test IO implementation
   */
  public Intake(IntakeIO io) {
    this.io = io;
  }

  /** Updates and records the latest hardware inputs. */
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Intake", inputs);

    if (goalAngle != null) {
      Logger.recordOutput("Intake/GoalRadians", goalAngle.in(Radians));
    }

    Logger.recordOutput("Intake/AngleRadians", inputs.pivotPositionRadians);
    Logger.recordOutput(
        "Intake/VelocityRadiansPerSecond",
        inputs.pivotVelocityRadiansPerSecond);
    Logger.recordOutput("Intake/AppliedVolts", inputs.pivotAppliedVolts);
    Logger.recordOutput("Intake/CurrentAmps", inputs.pivotCurrentAmps);
    Logger.recordOutput("Intake/TemperatureCelsius", inputs.pivotTemperatureCelsius);
  }

  /** Advances the IO simulation; call from the robot simulation periodic loop. */
  public void simulationPeriodic() {
    io.updateSim(
        LOOP_PERIOD_SECONDS,
        RobotController.getBatteryVoltage());
  }

  /** Returns the measured arm angle. */
  public Angle getAngle() {
    return Radians.of(inputs.pivotPositionRadians);
  }

  /** Returns the measured angular velocity in radians per second. */
  public double getVelocityRadiansPerSecond() {
    return inputs.pivotVelocityRadiansPerSecond;
  }

  /** Returns the motor's applied voltage. */
  public Voltage getVoltage() {
    return Volts.of(inputs.pivotAppliedVolts);
  }

  /** Returns the active goal, if position control is enabled. */
  public Optional<Angle> getGoal() {
    return Optional.ofNullable(goalAngle);
  }

  private void setGoalAngle(Angle angle) {
    double requestedRadians = angle.in(Radians);
    double clampedRadians = Math.max(
        IntakeConstants.Pivot.MIN_ANGLE.in(Radians),
        Math.min(IntakeConstants.Pivot.MAX_ANGLE.in(Radians), requestedRadians));

    Angle clamped = Radians.of(clampedRadians);

    if (goalAngle == null || !clamped.isEquivalent(goalAngle)) {
      io.setPosition(clampedRadians);
      goalAngle = clamped;
    }
  }

  /** Commands the arm to a target angle and holds it there. */
  public Command setAngle(Angle angle) {
  }

  /** Commands the arm with an open-loop duty cycle between -1 and 1. */
  public Command set(double dutyCycle) {
  }

  /** Creates a trigger that is true when the arm is near the requested angle. */
  public Trigger near(Angle angle, Angle tolerance) {
    return new Trigger(() -> getAngle().isNear(angle, tolerance));
  }

  /** Holds an angle at the lowest command priority. */
  public Command hold(Angle angle) {
  }

  /** Stops the arm and clears its active goal. */
  public void stop() {
    goalAngle = null;
    io.stop();
  }
}
