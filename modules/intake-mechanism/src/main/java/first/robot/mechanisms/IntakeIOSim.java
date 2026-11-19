
package first.robot.mechanisms;

import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.RadiansPerSecond;
import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Seconds;
import static org.wpilib.units.Units.Volts;

import org.wpilib.hardware.bus.CANPort;
import org.wpilib.math.system.DCMotor;
import org.wpilib.math.system.Models;
import org.wpilib.simulation.BatterySim;
import org.wpilib.simulation.FlywheelSim;
import org.wpilib.simulation.SingleJointedArmSim;
import org.wpilib.system.RobotController;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.sim.TalonFXSimState.MotorType;

import first.robot.constants.IntakeConstants;

/** Simulated TalonFX and Kraken X60 Intake implementation. */
public class IntakeIOSim implements IntakeIO {
    private static final double LOOP_PERIOD_SECONDS = 0.020;

    private static final DCMotor DC_MOTOR = DCMotor.getKrakenX60(1);

    private final TalonFX rollerMotor = new TalonFX(IntakeConstants.Roller.CAN_ROLLER_ID, new CANBus(CANPort.CAN_S0));
    private final TalonFX pivotMotor = new TalonFX(IntakeConstants.Pivot.CAN_ID, new CANBus(CANPort.CAN_S0));

    private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0);
    private final PositionVoltage positionRequest = new PositionVoltage(0);
    private final VoltageOut rollerVoltageRequest = new VoltageOut(0.0);
    private final VoltageOut pivotVoltageRequest = new VoltageOut(0.0);

    private final FlywheelSim flywheelSim = new FlywheelSim(
            Models.flywheelFromPhysicalConstants(
                    DC_MOTOR,
                    IntakeConstants.Roller.MOMENT_OF_INERTIA_KG_M2,
                    IntakeConstants.Roller.GEAR_RATIO),
            DC_MOTOR);
    private final SingleJointedArmSim armSim = new SingleJointedArmSim(
            DCMotor.getKrakenX60(1),
            IntakeConstants.Pivot.GEAR_RATIO,
            SingleJointedArmSim.estimateMOI(
                    IntakeConstants.Pivot.ARM_LENGTH_METERS, IntakeConstants.Pivot.ARM_MASS_KG),
            IntakeConstants.Pivot.ARM_LENGTH_METERS,
            IntakeConstants.Pivot.MIN_ANGLE_RADIANS,
            IntakeConstants.Pivot.MAX_ANGLE_RADIANS,
            true,
            IntakeConstants.Pivot.STARTING_ANGLE_RADIANS);
    private boolean rollerVelocityControlEnabled = false;
    private boolean pivotPositionControlEnabled = false;
    private double goalVelocityRotationsPerSecond = 0.0;
    private double pivotGoalAppliedVolts;
    private double pivotGoalRadians;
    private double simulatedPositionRotations = 0.0;
    private double lastVelocityRadiansPerSecond = 0.0;

    /** Creates and configures the simulated Intake motor. */
    public IntakeIOSim() {
        rollerMotor.getConfigurator().apply(
                IntakeConstants.Roller.createMotorConfiguration());

        rollerMotor.getSimState().setMotorType(MotorType.KrakenX60);
        rollerMotor.getSimState().setRawRotorPosition(
                0.0 * IntakeConstants.Roller.GEAR_RATIO);

        rollerMotor.setPosition(Rotations.of(0.0));
    }

    @Override
    public void updateInputs(IntakeIOInputs inputs) {
        double batteryVoltage = RobotController.getBatteryVoltage();

        double loadedVoltage =
        BatterySim.calculateDefaultBatteryLoadedVoltage(rollerMotor.getStatorCurrent().getValue().in(Amps),
        pivotMotor.getStatorCurrent().getValue().in(Amps));


        var simState = rollerMotor.getSimState();

        // Keep the simulated TalonFX supply voltage synchronized with the robot.
        simState.setSupplyVoltage(
                RobotController.getMeasureBatteryVoltage());

        // Apply the Talon's requested voltage to the physical flywheel model.
        flywheelSim.setInputVoltage(
                simState.getMotorVoltageMeasure().in(Volts));

        flywheelSim.update(LOOP_PERIOD_SECONDS);

        double velocityRadiansPerSecond = flywheelSim.getAngularVelocity();

        double accelerationRadiansPerSecondSquared = (velocityRadiansPerSecond - lastVelocityRadiansPerSecond)
                / LOOP_PERIOD_SECONDS;

        lastVelocityRadiansPerSecond = velocityRadiansPerSecond;

        // Integrate angular velocity to obtain the flywheel's position.
        simulatedPositionRotations += RadiansPerSecond.of(velocityRadiansPerSecond)
                .times(Seconds.of(LOOP_PERIOD_SECONDS))
                .in(Rotations);

        // Feed the simulated mechanism state back into Phoenix.
        simState.setRawRotorPosition(
                simulatedPositionRotations * IntakeConstants.Roller.GEAR_RATIO);

        simState.setRotorVelocity(
                RotationsPerSecond.of(
                        velocityRadiansPerSecond / (2.0 * Math.PI))
                        .times(IntakeConstants.Roller.GEAR_RATIO));

        simState.setRotorAcceleration(
                RadiansPerSecond.of(accelerationRadiansPerSecondSquared)
                        .times(IntakeConstants.Roller.GEAR_RATIO).baseUnitMagnitude());

        if (pivotPositionControlEnabled) {
                double error = pivotGoalRadians - armSim.getAngle();
                double outputVolts = 150.0 * error;
                pivotGoalAppliedVolts = Math.max(
                -batteryVoltage,
                Math.min(batteryVoltage, outputVolts));
        }

        pivotGoalAppliedVolts = Math.max(
            -batteryVoltage,
            Math.min(batteryVoltage, pivotGoalAppliedVolts));

        armSim.setInputVoltage(pivotGoalAppliedVolts);
        armSim.update(LOOP_PERIOD_SECONDS);


        inputs.rollerPositionRotations = simulatedPositionRotations;
        inputs.rollerVelocityRotationsPerSecond = velocityRadiansPerSecond / (2.0 * Math.PI);
        inputs.rollerAppliedVolts = simState.getMotorVoltageMeasure().in(Volts);
        inputs.rollerStatorCurrentAmps = rollerMotor.getStatorCurrent().getValueAsDouble();
        inputs.rollerTemperatureCelsius = rollerMotor.getDeviceTemp().getValueAsDouble();

        inputs.rollerVelocityControlEnabled = rollerVelocityControlEnabled;
        inputs.rollerGoalVelocityRotationsPerSecond = goalVelocityRotationsPerSecond;

        inputs.pivotPositionRadians = armSim.getAngle();
        inputs.pivotVelocityRadiansPerSecond = armSim.getVelocity();
        inputs.pivotAppliedVolts = pivotGoalAppliedVolts;
        inputs.pivotCurrentAmps = armSim.getCurrentDraw();
        inputs.pivotTemperatureCelsius = 0.0;
        inputs.pivotAbsoluteEncoderRotations = armSim.getAngle() / (2.0 * Math.PI);
        inputs.pivotAbsoluteAngleRadians = armSim.getAngle();
        inputs.pivotPositionControlEnabled = pivotPositionControlEnabled;
        inputs.pivotGoalRadians = pivotGoalRadians;
    }

    @Override
    public void setVelocity(double rotationsPerSecond) {
        rollerVelocityControlEnabled = true;
        goalVelocityRotationsPerSecond = rotationsPerSecond;

        rollerMotor.setControl(
                velocityRequest.withVelocity(
                        RotationsPerSecond.of(rotationsPerSecond)));
    }

    @Override
    public void setVoltage(double pivotVolts, double rollerVolts) {
        rollerVelocityControlEnabled = false;
        rollerMotor.setControl(rollerVoltageRequest.withOutput(Volts.of(rollerVolts)));
        pivotPositionControlEnabled = false;
        pivotMotor.setControl(pivotVoltageRequest.withOutput(Volts.of(rollerVolts)));
    }

    @Override
    public void stop() {
        rollerVelocityControlEnabled = false;
        pivotPositionControlEnabled = false;
        rollerMotor.setControl(rollerVoltageRequest.withOutput(Volts.of(0.0)));
        pivotMotor.setControl(pivotVoltageRequest.withOutput(Volts.of(0)));
        rollerMotor.stopMotor();
        pivotMotor.stopMotor();
    }
}
