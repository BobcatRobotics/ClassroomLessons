
package first.robot.mechanisms;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.RPM;
import static org.wpilib.units.Units.Radians;
import static org.wpilib.units.Units.Rotation;
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
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.AngularVelocity;

import first.robot.constants.IntakeConstants;
import first.robot.utilities.SubsystemStateMachine;

/** A Intake flywheel controlled through the AdvantageKit IO layer. */
public class Intake implements Mechanism {
  private final SubsystemStateMachine<IntakeState> intakeStateMachine = new SubsystemStateMachine<>(IntakeState.IDLE);

  private static final double LOOP_PERIOD_SECONDS = 0.020;

  private final IntakeIO io;
  private final IntakeIOInputsAutoLogged inputs = new IntakeIOInputsAutoLogged();

  private AngularVelocity rollerGoalVelocity = null;
  private Angle pivotGoalAngle = null;

  private final Mechanism2d rollerMechanism2d = new Mechanism2d(1, 1);
  private final MechanismLigament2d rollerSpokeLigament = rollerMechanism2d.getRoot("Axle", 0.5, 0.5)
      .append(new MechanismLigament2d("Flywheel", 0.4, 0.0));

  /**
   * Creates a Intake with the specified IO implementation.
   *
   * @param io real or simulated Intake IO
   */
  public Intake(IntakeIO io) {
    this.io = io;
  }

  /** Returns the measured flywheel velocity. */
  public AngularVelocity getVelocity() {
    return RotationsPerSecond.of(inputs.rollerVelocityRotationsPerSecond);
  }

  /** Returns the measured flywheel position. */
  public Angle getPosition() {
    return Rotation.of(inputs.rollerPositionRotations);
  }

  /** Returns the requested velocity when velocity control is active. */
  public Optional<AngularVelocity> getGoalVelocity() {
    return Optional.ofNullable(rollerGoalVelocity);
  }

  /** Returns the active goal, if position control is enabled. */
  public Optional<Angle> getGoalPosition() {
    return Optional.ofNullable(pivotGoalAngle);
  }

  /** Returns the measured motor voltage. */
  public org.wpilib.units.measure.Voltage getVoltage() {
    return Volts.of(inputs.rollerAppliedVolts).plus(Volts.of(inputs.pivotAppliedVolts));
  }

  /** Returns the measured motor stator current. */
  public org.wpilib.units.measure.Current getCurrent() {
    return Amps.of(inputs.rollerStatorCurrentAmps).plus(Amps.of(inputs.pivotCurrentAmps));
  }

  private void setGoalAngle(Angle angle) {
    double requestedRadians = angle.in(Radians);
    double clampedRadians = Math.max(
        IntakeConstants.Pivot.MIN_ANGLE.in(Radians),
        Math.min(IntakeConstants.Pivot.MAX_ANGLE.in(Radians), requestedRadians));

    Angle clamped = Radians.of(clampedRadians);

    if (pivotGoalAngle == null || !clamped.isEquivalent(pivotGoalAngle)) {
      io.setPosition(clampedRadians);
      pivotGoalAngle = clamped;
    }
  }

  /**
   * Runs closed-loop velocity control until interrupted.
   *
   * @param velocity target flywheel velocity
   * @return command that maintains the requested velocity
   */
  public Command setVelocity(IntakeState state) {
  }

  /** Commands the arm to a target angle and holds it there. */
  public Command setAngle(IntakeState state) {
  }

  /**
   * Drives the flywheel using an open-loop duty cycle.
   *
   * @param dutyCycle output from -1.0 to 1.0
   * @return command that maintains the requested output
   */
  public Command setRollerDutyCycle(IntakeState state) {
  }

  /** Commands the arm with an open-loop duty cycle between -1 and 1. */
  public Command setPivotDutyCycle(IntakeState state) {
  }

  /**
   * Creates a trigger that becomes active near a target velocity.
   *
   * @param velocity  target velocity
   * @param tolerance acceptable velocity error
   * @return velocity tolerance trigger
   */
  public Trigger near(
      AngularVelocity velocity, AngularVelocity tolerance) {
    return new Trigger(() -> getVelocity().isNear(velocity, tolerance));
  }

  /** Holds an angle at the lowest command priority. */
  public Command hold(Angle angle) {
  }

  /**
   * Coasts or brakes at zero applied voltage at the lowest command priority.
   *
   * @return idle command
   */
  public Command idle() {
  }

  /**
   * Refreshes inputs and records telemetry.
   *
   * <p>
   * Call from the robot's regular periodic loop.
   */
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Intake", inputs);

    rollerSpokeLigament.setAngle(
        inputs.rollerPositionRotations * 360.0 % 360.0);

    // Logger.recordOutput("Intake/Mechanism", mechanism2d);
    Logger.recordOutput("Intake/Roller/VelocityRPS",
        inputs.rollerVelocityRotationsPerSecond);
    Logger.recordOutput("Intake/Roller/AppliedVolts", inputs.rollerAppliedVolts);
    Logger.recordOutput("Intake/Roller/StatorCurrentAmps",
        inputs.rollerStatorCurrentAmps);

    if (rollerGoalVelocity != null) {
      Telemetry.log("Intake/Roller/GoalVelocity", rollerGoalVelocity);
    }

    Logger.recordOutput("Intake/AngleRadians", inputs.pivotPositionRadians);
    Logger.recordOutput(
        "Intake/VelocityRadiansPerSecond",
        inputs.pivotVelocityRadiansPerSecond);
    Logger.recordOutput("Intake/AppliedVolts", inputs.pivotAppliedVolts);
    Logger.recordOutput("Intake/CurrentAmps", inputs.pivotCurrentAmps);
    Logger.recordOutput("Intake/TemperatureCelsius", inputs.pivotTemperatureCelsius);

    if (pivotGoalAngle != null) {
      Telemetry.log("Intake/Pivot/GoalAngle", pivotGoalAngle);
    }

  }

  /** Advances the IO simulation; call from the robot simulation periodic loop. */
  public void simulationPeriodic() {
  }

  /** Stops the pivot and rolleer and clears its active goal. */
  public void stop() {
  }
}
