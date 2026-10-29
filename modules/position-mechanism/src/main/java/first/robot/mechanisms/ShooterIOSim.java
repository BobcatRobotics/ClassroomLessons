
package first.robot.mechanisms;

import static org.wpilib.units.Units.KilogramSquareMeters;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Seconds;
import static org.wpilib.units.Units.Volts;

import org.wpilib.hardware.bus.CANPort;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.simulation.FlywheelSim;
import org.wpilib.system.RobotController;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState.MotorType;

import first.robot.constants.ShooterConstants;

/** Simulated TalonFX and Kraken X60 shooter implementation. */
public class ShooterIOSim implements ShooterIO {
  private static final double LOOP_PERIOD_SECONDS = 0.020;

  private static final DCMotor DC_MOTOR = DCMotor.getKrakenX60(1);

  private final TalonFX motor =
      new TalonFX(ShooterConstants.CAN_ID, new CANBus(CANPort.CAN_S0));

  private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);
  private final VoltageOut voltageRequest = new VoltageOut(0.0);

  private final FlywheelSim flywheelSim = new FlywheelSim(
      Models.flywheelFromPhysicalConstants(
          DC_MOTOR,
          ShooterConstants.MOMENT_OF_INERTIA_KG_M2,
          ShooterConstants.GEAR_RATIO),
      DC_MOTOR);

  private boolean velocityControlEnabled = false;
  private double goalVelocityRotationsPerSecond = 0.0;

  private double simulatedPositionRotations = 0.0;
  private double lastVelocityRadiansPerSecond = 0.0;

  /** Creates and configures the simulated shooter motor. */
  public ShooterIOSim() {
    motor.getConfigurator().apply(
        ShooterConstants.createMotorConfiguration());

    motor.getSimState().setMotorType(MotorType.KrakenX60);
    motor.getSimState().setRawRotorPosition(
        0.0 * ShooterConstants.GEAR_RATIO);

    motor.setPosition(Rotations.of(0.0));
  }

  @Override
  public void updateInputs(ShooterIOInputs inputs) {
    var simState = motor.getSimState();

    // Keep the simulated TalonFX supply voltage synchronized with the robot.
    simState.setSupplyVoltage(
        RobotController.getMeasureBatteryVoltage());

    // Apply the Talon's requested voltage to the physical flywheel model.
    flywheelSim.setInputVoltage(
        simState.getMotorVoltageMeasure().in(Volts));

    flywheelSim.update(LOOP_PERIOD_SECONDS);

    double velocityRadiansPerSecond =
        flywheelSim.getAngularVelocity();

    double accelerationRadiansPerSecondSquared =
        (velocityRadiansPerSecond - lastVelocityRadiansPerSecond)
            / LOOP_PERIOD_SECONDS;

    lastVelocityRadiansPerSecond = velocityRadiansPerSecond;

    // Integrate angular velocity to obtain the flywheel's position.
    simulatedPositionRotations +=
        RadiansPerSecond.of(velocityRadiansPerSecond)
            .times(Seconds.of(LOOP_PERIOD_SECONDS))
            .in(Rotations);

    // Feed the simulated mechanism state back into Phoenix.
    simState.setRawRotorPosition(
        simulatedPositionRotations * ShooterConstants.GEAR_RATIO);

    simState.setRotorVelocity(
        RotationsPerSecond.of(
            velocityRadiansPerSecond / (2.0 * Math.PI))
            .times(ShooterConstants.GEAR_RATIO));

    simState.setRotorAcceleration(
        RadiansPerSecond.of(accelerationRadiansPerSecondSquared)
            .times(ShooterConstants.GEAR_RATIO).baseUnitMagnitude());

    inputs.positionRotations = simulatedPositionRotations;
    inputs.velocityRotationsPerSecond =
        velocityRadiansPerSecond / (2.0 * Math.PI);
    inputs.appliedVolts = simState.getMotorVoltageMeasure().in(Volts);
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
