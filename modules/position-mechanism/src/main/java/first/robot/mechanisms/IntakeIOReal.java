
package first.robot.mechanisms;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.Volts;

import org.wpilib.math.util.MathUtil;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import first.robot.constants.IntakeConstants;


/**
 * Real-hardware implementation of {@link IntakeIO}.
 *
 * <p>Position feedback and goals use mechanism rotations, with the TalonFX
 * applying the configured sensor-to-mechanism ratio.
 */
public class IntakeIOReal implements IntakeIO {



  private final TalonFX motor =
      new TalonFX(IntakeConstants.Pivot.CAN_ID, new CANBus("CANivore"));
  private final CANcoder encoder =
      new CANcoder(IntakeConstants.Pivot.ENCODER_CAN_ID, new CANBus("CANivore"));

  private final PositionVoltage positionRequest = new PositionVoltage(0);
  private final VoltageOut voltageRequest = new VoltageOut(0);

  private boolean positionControlEnabled;
  private double goalRadians;

  /** Creates and configures the real intake hardware. */
  public IntakeIOReal() {
  }

  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    inputs.pivotPositionRadians =
        motor.getPosition().refresh().getValue().in(Rotations)
            * 2.0 * Math.PI;

    inputs.pivotVelocityRadiansPerSecond =
        motor.getVelocity().getValueAsDouble();

    inputs.pivotAppliedVolts =
        motor.getMotorVoltage().refresh().getValue().in(Volts);

    inputs.pivotCurrentAmps =
        motor.getStatorCurrent().refresh().getValue().in(Amps);

        
    inputs.pivotTemperatureCelsius = motor.getDeviceTemp().getValueAsDouble();

    double encoderRotations =
        encoder.getAbsolutePosition().refresh().getValue().in(Rotations);

    inputs.pivotAbsoluteEncoderRotations = encoderRotations;
    inputs.pivotAbsoluteAngleRadians = getAbsoluteAngleRadians(encoderRotations);
    inputs.pivotPositionControlEnabled = positionControlEnabled;
    inputs.pivotGoalRadians = goalRadians;
  }

  @Override
  public void setPosition(double positionRadians) {
  }

  @Override
  public void setVoltage(double volts) {
  }

  @Override
  public void stop() {
    positionControlEnabled = false;
    motor.stopMotor();
  }

  private double getAbsoluteAngleRadians(double readingRotations) {
    double fromZero = readingRotations - IntakeConstants.Pivot.ENCODER_OFFSET_ROTATIONS;
    double fromMiddle = MathUtil.inputModulus(
        fromZero - IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS * IntakeConstants.Pivot.ENCODER_RATIO,
        -0.5,
        0.5);

    double angleRotations =
        IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS + fromMiddle / IntakeConstants.Pivot.ENCODER_RATIO;

    return angleRotations * 2.0 * Math.PI;
  }
}
