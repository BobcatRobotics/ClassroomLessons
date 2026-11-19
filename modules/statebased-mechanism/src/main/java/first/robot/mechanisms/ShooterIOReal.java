package first.robot.mechanisms;

import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Volts;

import org.wpilib.hardware.bus.CANPort;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;

import first.robot.constants.ShooterConstants;

/** Real TalonFX implementation of the shooter IO interface. */
public class ShooterIOReal implements ShooterIO {
  private final TalonFX motor =
      new TalonFX(ShooterConstants.CAN_ID, new CANBus(CANPort.CAN_S0));

  private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);
  private final VoltageOut voltageRequest = new VoltageOut(0.0);

  private boolean velocityControlEnabled = false;
  private double goalVelocityRotationsPerSecond = 0.0;

  /** Creates and configures the real shooter motor. */
  public ShooterIOReal() {
    motor.getConfigurator().apply(
        ShooterConstants.createMotorConfiguration());
    motor.setPosition(Rotations.of(0.0));
  }

  @Override
  public void updateInputs(ShooterIOInputs inputs) {
    inputs.positionRotations = motor.getPosition().getValueAsDouble();
    inputs.velocityRotationsPerSecond =
        motor.getVelocity().getValueAsDouble();
    inputs.appliedVolts = motor.getMotorVoltage().getValueAsDouble();
    inputs.statorCurrentAmps = motor.getStatorCurrent().getValueAsDouble();
    inputs.temperatureCelsius = motor.getDeviceTemp().getValueAsDouble();

    inputs.velocityControlEnabled = velocityControlEnabled;
    inputs.goalVelocityRotationsPerSecond =
        goalVelocityRotationsPerSecond;
  }

  @Override
  public void setVelocity(double rotationsPerSecond) {
    velocityControlEnabled = true;
    goalVelocityRotationsPerSecond = rotationsPerSecond;

    motor.setControl(
        velocityRequest.withVelocity(
            RotationsPerSecond.of(rotationsPerSecond)));
  }

  @Override
  public void setVoltage(double volts) {
    velocityControlEnabled = false;
    motor.setControl(voltageRequest.withOutput(Volts.of(volts)));
  }

  @Override
  public void stop() {
    velocityControlEnabled = false;
    motor.setControl(voltageRequest.withOutput(Volts.of(0.0)));
  }
}
