package first.robot.mechanisms;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Volts;

import org.wpilib.hardware.bus.CANPort;
import org.wpilib.math.util.MathUtil;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;

import first.robot.constants.IntakeConstants;

/** Real TalonFX implementation of the Intake IO interface. */
public class IntakeIOReal implements IntakeIO {
  private final TalonFX rollerMotor = new TalonFX(IntakeConstants.Roller.CAN_ROLLER_ID, new CANBus(CANPort.CAN_S0));
  private final TalonFX pivotMotor = new TalonFX(IntakeConstants.Pivot.CAN_ID, new CANBus(CANPort.CAN_S0));
  private final CANcoder encoder = new CANcoder(IntakeConstants.Pivot.ENCODER_CAN_ID, new CANBus("CANivore"));

  private final VelocityVoltage rollerVelocityRequest = new VelocityVoltage(0.0);
  private final PositionVoltage pivotPositionRequest = new PositionVoltage(0);
  private final VoltageOut pivotVoltageRequest = new VoltageOut(0);
  private final VoltageOut rollerVoltageRequest = new VoltageOut(0.0);

  private boolean rollerVelocityControlEnabled = false;
  private double goalrollerVelocityRotationsPerSecond = 0.0;

  private boolean pivotPositionControlEnabled;
  private double pivotGoalRadians;

  /** Creates and configures the real Intake motor. */
  public IntakeIOReal() {
    rollerMotor.getConfigurator().apply(
        IntakeConstants.Roller.createMotorConfiguration());
    rollerMotor.setPosition(Rotations.of(0.0));
  }

  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    inputs.rollerPositionRotations = rollerMotor.getPosition().getValueAsDouble();
    inputs.rollerVelocityRotationsPerSecond = rollerMotor.getVelocity().getValueAsDouble();
    inputs.rollerAppliedVolts = rollerMotor.getMotorVoltage().getValueAsDouble();
    inputs.rollerStatorCurrentAmps = rollerMotor.getStatorCurrent().getValueAsDouble();
    inputs.rollerTemperatureCelsius = rollerMotor.getDeviceTemp().getValueAsDouble();
    inputs.rollerVelocityControlEnabled = rollerVelocityControlEnabled;
    inputs.rollerGoalVelocityRotationsPerSecond = goalrollerVelocityRotationsPerSecond;

    inputs.pivotPositionRadians = pivotMotor.getPosition().refresh().getValue().in(Rotations)
        * 2.0 * Math.PI;

    inputs.pivotVelocityRadiansPerSecond = pivotMotor.getVelocity().getValueAsDouble();

    inputs.pivotAppliedVolts = pivotMotor.getMotorVoltage().refresh().getValue().in(Volts);

    inputs.pivotCurrentAmps = pivotMotor.getStatorCurrent().refresh().getValue().in(Amps);

    inputs.pivotTemperatureCelsius = pivotMotor.getDeviceTemp().getValueAsDouble();

    double encoderRotations = encoder.getAbsolutePosition().refresh().getValue().in(Rotations);

    inputs.pivotAbsoluteEncoderRotations = encoderRotations;
    inputs.pivotAbsoluteAngleRadians = getAbsoluteAngleRadians(encoderRotations);
    inputs.pivotPositionControlEnabled = pivotPositionControlEnabled;
    inputs.pivotGoalRadians = pivotGoalRadians;
  }

  private double getAbsoluteAngleRadians(double readingRotations) {
    double fromZero = readingRotations - IntakeConstants.Pivot.ENCODER_OFFSET_ROTATIONS;
    double fromMiddle = MathUtil.inputModulus(
        fromZero - IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS * IntakeConstants.Pivot.ENCODER_RATIO,
        -0.5,
        0.5);

    double angleRotations = IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS
        + fromMiddle / IntakeConstants.Pivot.ENCODER_RATIO;

    return angleRotations * 2.0 * Math.PI;
  }

  @Override
  public void setPosition(double positionRadians) {
  }

  @Override
  public void setVelocity(double rotationsPerSecond) {
  }

  @Override
  public void setVoltage(double pivotVolts, double rollerVolts) {
  }

  @Override
  public void stop() {
  }
}
