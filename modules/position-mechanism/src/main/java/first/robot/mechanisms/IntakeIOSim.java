
package first.robot.mechanisms;

import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.Volts;

import org.wpilib.hardware.bus.CANPort;
import org.wpilib.math.system.DCMotor;
import org.wpilib.simulation.SingleJointedArmSim;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState.MotorType;

import first.robot.constants.ShooterConstants;


/**
 * Physics simulation for the intake arm.
 *
 * <p>The simulation models a Kraken X60, arm inertia, gearing, and hard stops.
 */
public class IntakeIOSim implements IntakeIO {

  private static final double GEAR_RATIO = 60.0;
  private static final double ARM_LENGTH_METERS = 1.0;
  private static final double ARM_MASS_KG = 3.0;
  private static final double MIN_ANGLE_RADIANS = 0.0;
  private static final double MAX_ANGLE_RADIANS = Math.PI / 2.0;
  private static final double STARTING_ANGLE_RADIANS = 0.0;
    private final VoltageOut voltageRequest = new VoltageOut(0.0);

  private final TalonFX motor =
      new TalonFX(ShooterConstants.CAN_ID, new CANBus(CANPort.CAN_S0));
  private final SingleJointedArmSim armSim =
      new SingleJointedArmSim(
          DCMotor.getKrakenX60(1),
          GEAR_RATIO,
          SingleJointedArmSim.estimateMOI(
              ARM_LENGTH_METERS, ARM_MASS_KG),
          ARM_LENGTH_METERS,
          MIN_ANGLE_RADIANS,
          MAX_ANGLE_RADIANS,
          true,
          STARTING_ANGLE_RADIANS);

  private double appliedVolts;
  private double goalRadians;
  private boolean positionControlEnabled;

  public IntakeIOSim() {
    motor.getConfigurator().apply(
        ShooterConstants.createMotorConfiguration());

    motor.getSimState().setMotorType(MotorType.KrakenX60);
    motor.getSimState().setRawRotorPosition(
        0.0 * ShooterConstants.GEAR_RATIO);

    motor.setPosition(Rotations.of(0.0));
  }

  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    inputs.pivotPositionRadians = armSim.getAngle();
    inputs.pivotVelocityRadiansPerSecond = armSim.getVelocity();
    inputs.pivotAppliedVolts = appliedVolts;
    inputs.pivotCurrentAmps = armSim.getCurrentDraw();
    inputs.pivotTemperatureCelsius = 0.0;
    inputs.pivotAbsoluteEncoderRotations =
        armSim.getAngle() / (2.0 * Math.PI);
    inputs.pivotAbsoluteAngleRadians = armSim.getAngle();
    inputs.pivotPositionControlEnabled = positionControlEnabled;
    inputs.pivotGoalRadians = goalRadians;
  }

  @Override
  public void setPosition(double positionRadians) {
    // Simulate the position request with a proportional controller.
    goalRadians = Math.max(
        MIN_ANGLE_RADIANS,
        Math.min(MAX_ANGLE_RADIANS, positionRadians));
    positionControlEnabled = true;
  }

  @Override
  public void setVoltage(double volts) {
    appliedVolts = volts;
    positionControlEnabled = false;


    motor.setControl(voltageRequest.withOutput(volts));
  }

  @Override
  public void stop() {
    appliedVolts = 0.0;
    positionControlEnabled = false;
    motor.setControl(voltageRequest.withOutput(Volts.of(0.0)));
  }

  @Override
  public void updateSim(double periodSeconds, double batteryVoltage) {
    if (positionControlEnabled) {
      double error = goalRadians - armSim.getAngle();
      double outputVolts = 150.0 * error;
      appliedVolts = Math.max(
          -batteryVoltage,
          Math.min(batteryVoltage, outputVolts));
    }

    appliedVolts = Math.max(
        -batteryVoltage,
        Math.min(batteryVoltage, appliedVolts));

    armSim.setInputVoltage(appliedVolts);
    armSim.update(periodSeconds);
  }
}
