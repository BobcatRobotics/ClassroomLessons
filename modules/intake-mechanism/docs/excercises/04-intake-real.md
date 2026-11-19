# Lesson: Implementing Real-Hardware Intake IO

## Objective

In this lesson, you will implement `IntakeIOReal`, the hardware-specific input/output layer for the robot's intake mechanism.

The intake has two independently controlled TalonFX motors:

- A **roller motor**, which supports closed-loop velocity control and direct voltage control.
- A **pivot motor**, which supports closed-loop position control and direct voltage control.

A CANcoder measures the pivot's absolute position. The class reads telemetry from all three devices and reports it through the `IntakeIO` input structure.

By the end of this lesson, you should understand how to:

- Implement a hardware IO interface for a mechanism with multiple motors.
- Construct Phoenix 6 devices on their respective CAN buses.
- Reuse Phoenix 6 control request objects.
- Control a roller with `VelocityVoltage` or `VoltageOut`.
- Control a pivot with `PositionVoltage` or `VoltageOut`.
- Track requested setpoints and selected control modes.
- Read motor position, velocity, voltage, current, and temperature.
- Read and calibrate an absolute CANcoder measurement.
- Convert between mechanism rotations, radians, and rotations per second.
- Keep hardware-specific details separate from mechanism logic.

## Prerequisites

Before starting, you should be familiar with:

- Java classes, interfaces, constructors, and instance fields.
- The `IntakeIO` interface and its `IntakeIOInputs` data structure.
- CTRE Phoenix 6 TalonFX and CANcoder devices.
- CAN bus device configuration.
- Closed-loop velocity and position control.
- WPILib units and `MathUtil`.
- The constants defined in `IntakeConstants.Roller` and `IntakeConstants.Pivot`.

## Step 1: Create the Hardware IO Class

Create the file:

`src/main/java/first/robot/mechanisms/IntakeIOReal.java`

Start with the following structure:

```java
package first.robot.mechanisms;

import first.robot.constants.IntakeConstants;

/** Real TalonFX implementation of the Intake IO interface. */
public class IntakeIOReal implements IntakeIO {

  /** Creates and configures the real Intake hardware. */
  public IntakeIOReal() {
    // TODO: Configure the hardware.
  }

  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    // TODO: Read hardware telemetry.
  }

  @Override
  public void setPosition(double positionRadians) {
    // TODO: Command a pivot position.
  }

  @Override
  public void setVelocity(double rotationsPerSecond) {
    // TODO: Command a roller velocity.
  }

  @Override
  public void setVoltage(double pivotVolts, double rollerVolts) {
    // TODO: Command both motors using voltage control.
  }

  @Override
  public void stop() {
    // TODO: Stop both motors.
  }
}
```

### Your task

1. Declare a class named `IntakeIOReal`.
2. Implement `IntakeIO`.
3. Implement every method required by the interface.
4. Use the constructor to initialize and configure the hardware.
5. Keep controller mappings and command composition out of this class.

### Explanation

`IntakeIOReal` communicates with physical robot hardware. The `IntakeIO` interface defines the operations the rest of the robot code can use without depending directly on TalonFX or CANcoder classes.

This separation allows the robot to use other implementations, such as simulation or test IO, without changing the higher-level intake mechanism.

## Step 2: Declare the Hardware Devices

The intake uses three CAN devices:

| Device | Purpose | CAN bus in this implementation |
|---|---|---|
| Roller TalonFX | Drives the intake roller | `CAN_S0` |
| Pivot TalonFX | Moves the intake pivot | `CAN_S0` |
| Pivot CANcoder | Measures absolute pivot position | `CANivore` |

Add the Phoenix 6 and WPILib bus imports:

```java
import org.wpilib.hardware.bus.CANPort;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
```

Declare the devices:

```java
private final TalonFX rollerMotor =
    new TalonFX(
        IntakeConstants.Roller.CAN_ROLLER_ID,
        new CANBus(CANPort.CAN_S0));

private final TalonFX pivotMotor =
    new TalonFX(
        IntakeConstants.Pivot.CAN_ID,
        new CANBus(CANPort.CAN_S0));

private final CANcoder encoder =
    new CANcoder(
        IntakeConstants.Pivot.ENCODER_CAN_ID,
        new CANBus("CANivore"));
```

### Your task

- Use the CAN IDs from the appropriate `IntakeConstants` groups.
- Create the roller and pivot TalonFX devices on `CAN_S0`.
- Create the CANcoder on the `CANivore` bus.
- Confirm that the physical wiring and device configuration match these bus assignments.

### Explanation

The roller and pivot are separate motors and need separate TalonFX instances because they receive different control requests.

The CANcoder is a separate feedback device. It measures the pivot's absolute angular position independently of the pivot TalonFX's integrated rotor position.

**Important:** A device's CAN ID and bus must match the actual robot wiring. A correct ID on the wrong bus will not communicate with the intended device.

## Step 3: Create Reusable Control Requests

Phoenix 6 control request objects describe how each motor should operate. This implementation uses four reusable requests:

```java
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;

private final VelocityVoltage rollerVelocityRequest =
    new VelocityVoltage(0.0);

private final PositionVoltage pivotPositionRequest =
    new PositionVoltage(0.0);

private final VoltageOut pivotVoltageRequest =
    new VoltageOut(0.0);

private final VoltageOut rollerVoltageRequest =
    new VoltageOut(0.0);
```

### Your task

1. Create a `VelocityVoltage` request for the roller.
2. Create a `PositionVoltage` request for the pivot.
3. Create a separate `VoltageOut` request for each motor.
4. Explain why the roller and pivot need different closed-loop request types.

### Explanation

- `VelocityVoltage` asks the roller TalonFX to regulate its velocity to a requested setpoint.
- `PositionVoltage` asks the pivot TalonFX to regulate its position to a requested setpoint.
- `VoltageOut` requests a direct motor voltage rather than regulating to a velocity or position target.

The same motor can be operated using different control strategies. The request type determines which strategy is selected.

Separate request objects make the intended control path explicit and avoid creating a new request object each time a command is sent.

## Step 4: Track the Control State

The implementation stores the most recently selected control mode and requested setpoint for each motor:

```java
private boolean rollerVelocityControlEnabled = false;
private double goalrollerVelocityRotationsPerSecond = 0.0;

private boolean pivotPositionControlEnabled;
private double pivotGoalRadians;
```

### Your task

Explain what each field represents and identify which methods update it.

### Explanation

| Field | Meaning |
|---|---|
| `rollerVelocityControlEnabled` | Whether roller velocity control was most recently selected |
| `goalrollerVelocityRotationsPerSecond` | Most recently requested roller velocity |
| `pivotPositionControlEnabled` | Whether pivot position control was most recently selected |
| `pivotGoalRadians` | Most recently requested pivot angle |

`setVelocity()` enables roller velocity control and saves its requested velocity.

`setPosition()` enables pivot position control and saves its requested angle.

`setVoltage()` disables both closed-loop mode flags because it selects direct voltage control for both motors.

`stop()` also disables both flags.

These fields report the selected control mode and requested goal. They do not independently prove that the motor controller accepted the request or reached the setpoint.

## Step 5: Configure the Roller and Initialize the Pivot

The constructor in the supplied implementation applies a configuration to the roller motor and initializes the roller's integrated position:

```java
public IntakeIOReal() {
  rollerMotor.getConfigurator().apply(
      IntakeConstants.Roller.createMotorConfiguration());

  rollerMotor.setPosition(Rotations.of(0.0));
}
```

### Your task

1. Apply the roller configuration returned by `IntakeConstants.Roller.createMotorConfiguration()`.
2. Initialize the roller's integrated position to zero rotations.
3. Review how the pivot motor is configured elsewhere in the project.

### Explanation

The roller configuration is centralized in `IntakeConstants.Roller`, keeping hardware settings out of the IO implementation.

The call to `setPosition()` establishes the TalonFX integrated sensor's position reference. It does not physically move the roller or independently verify its physical position.

**Implementation detail:** The Java file supplied for this lesson does not apply a configuration to `pivotMotor` in this constructor. Do not assume that the pivot's feedback ratio, PID gains, current limits, or neutral mode are configured here. If those settings are required, confirm where they are applied or add the appropriate configuration as a separate implementation change.

## Step 6: Implement `updateInputs()`

The IO layer periodically copies hardware measurements and local control state into `IntakeIOInputs`.

The implementation reads the following roller signals:

```java
inputs.rollerPositionRotations =
    rollerMotor.getPosition().getValueAsDouble();

inputs.rollerVelocityRotationsPerSecond =
    rollerMotor.getVelocity().getValueAsDouble();

inputs.rollerAppliedVolts =
    rollerMotor.getMotorVoltage().getValueAsDouble();

inputs.rollerStatorCurrentAmps =
    rollerMotor.getStatorCurrent().getValueAsDouble();

inputs.rollerTemperatureCelsius =
    rollerMotor.getDeviceTemp().getValueAsDouble();

inputs.rollerVelocityControlEnabled =
    rollerVelocityControlEnabled;

inputs.rollerGoalVelocityRotationsPerSecond =
    goalrollerVelocityRotationsPerSecond;
```

The pivot signals are read separately:

```java
inputs.pivotPositionRadians =
    pivotMotor.getPosition().refresh().getValue().in(Rotations)
        * 2.0 * Math.PI;

inputs.pivotVelocityRadiansPerSecond =
    pivotMotor.getVelocity().getValueAsDouble();

inputs.pivotAppliedVolts =
    pivotMotor.getMotorVoltage().refresh().getValue().in(Volts);

inputs.pivotCurrentAmps =
    pivotMotor.getStatorCurrent().refresh().getValue().in(Amps);

inputs.pivotTemperatureCelsius =
    pivotMotor.getDeviceTemp().getValueAsDouble();
```

Finally, the CANcoder and local pivot control state are recorded:

```java
double encoderRotations =
    encoder.getAbsolutePosition().refresh().getValue().in(Rotations);

inputs.pivotAbsoluteEncoderRotations = encoderRotations;
inputs.pivotAbsoluteAngleRadians =
    getAbsoluteAngleRadians(encoderRotations);

inputs.pivotPositionControlEnabled = pivotPositionControlEnabled;
inputs.pivotGoalRadians = pivotGoalRadians;
```

### Your task

Understand the purpose and source of each input:

| Input field | Hardware or source | Purpose |
|---|---|---|
| `rollerPositionRotations` | Roller TalonFX | Integrated roller position |
| `rollerVelocityRotationsPerSecond` | Roller TalonFX | Roller velocity |
| `rollerAppliedVolts` | Roller TalonFX | Applied motor voltage |
| `rollerStatorCurrentAmps` | Roller TalonFX | Roller stator current |
| `rollerTemperatureCelsius` | Roller TalonFX | Device temperature |
| `rollerVelocityControlEnabled` | Local state | Selected roller control mode |
| `rollerGoalVelocityRotationsPerSecond` | Local state | Requested roller velocity |
| `pivotPositionRadians` | Pivot TalonFX | Integrated pivot position converted to radians |
| `pivotVelocityRadiansPerSecond` | Pivot TalonFX | Pivot velocity field |
| `pivotAppliedVolts` | Pivot TalonFX | Applied motor voltage |
| `pivotCurrentAmps` | Pivot TalonFX | Pivot stator current |
| `pivotTemperatureCelsius` | Pivot TalonFX | Device temperature |
| `pivotAbsoluteEncoderRotations` | CANcoder | Raw absolute encoder position |
| `pivotAbsoluteAngleRadians` | Calibrated CANcoder reading | Absolute mechanism angle |
| `pivotPositionControlEnabled` | Local state | Selected pivot control mode |
| `pivotGoalRadians` | Local state | Requested pivot angle |

### Explanation: unit conversions

A rotation is one complete revolution, or \(2\pi\) radians:

\[
\theta_{\text{rad}} = \theta_{\text{rot}} \times 2\pi
\]

The pivot position is explicitly converted from rotations to radians in the supplied implementation.

**Important review item:** The supplied implementation assigns `pivotMotor.getVelocity().getValueAsDouble()` directly to `pivotVelocityRadiansPerSecond`. Confirm the unit returned by the Phoenix 6 signal and the pivot TalonFX feedback configuration. If the signal is in rotations per second, it must be multiplied by \(2\pi\) to represent radians per second. The current Java line does not perform that conversion.

The roller telemetry is expressed in rotations and rotations per second. Voltage, current, and temperature fields should retain the units indicated by their names.

### Telemetry freshness

Some signals in this implementation are explicitly refreshed with `.refresh()`, while others are read using `.getValueAsDouble()`. Those readings may have different update ages. For production telemetry, consider refreshing related signals together and checking their status before using them.

## Step 7: Implement Pivot Position Control

The pivot moves toward a requested angle using `PositionVoltage`:

```java
@Override
public void setPosition(double positionRadians) {
  pivotGoalRadians = positionRadians;
  pivotPositionControlEnabled = true;

  // The Talon is configured to interpret requests in mechanism rotations.
  double mechanismRotations = positionRadians / (2.0 * Math.PI);

  pivotMotor.setControl(
      pivotPositionRequest.withPosition(mechanismRotations));
}
```

### Your task

1. Save the requested angle in `pivotGoalRadians`.
2. Set `pivotPositionControlEnabled` to `true`.
3. Convert radians to mechanism rotations.
4. Send the converted setpoint through the pivot TalonFX's `PositionVoltage` request.

Use:

\[
\theta_{\text{rot}} = \frac{\theta_{\text{rad}}}{2\pi}
\]

### Explanation

The IO interface accepts the pivot goal in radians, while the supplied implementation converts the request to mechanism rotations before sending it to the TalonFX.

This keeps the mechanism-facing API independent of the motor controller's configured position units.

The comment in the Java implementation states that the TalonFX is configured to interpret requests in mechanism rotations. Verify that the pivot TalonFX's actual feedback configuration supports that assumption.

Position control is useful for moving the intake to defined angles, such as a stowed or deployed position. A position request should be paired with appropriate mechanical and software limits.

## Step 8: Implement Roller Velocity Control

The roller uses `VelocityVoltage` to regulate its rotational speed:

```java
@Override
public void setVelocity(double rotationsPerSecond) {
  rollerVelocityControlEnabled = true;
  goalrollerVelocityRotationsPerSecond = rotationsPerSecond;

  rollerMotor.setControl(
      rollerVelocityRequest.withVelocity(
          RotationsPerSecond.of(rotationsPerSecond)));
}
```

### Your task

1. Enable the roller velocity-control state.
2. Store the requested velocity.
3. Convert the numeric value to a WPILib `RotationsPerSecond` unit.
4. Send the velocity request to the roller TalonFX.

### Explanation

The method accepts a velocity in rotations per second and uses `RotationsPerSecond.of(...)` to make the unit explicit when constructing the Phoenix 6 request.

Velocity control is appropriate when the roller should maintain a target speed despite changes in loading, within the limits of the motor, controller, and configured gains.

The requested velocity is also saved for telemetry. It represents the target, not the measured roller speed.

## Step 9: Implement Direct Voltage Control for Both Motors

The intake supports direct voltage control for the pivot and roller:

```java
@Override
public void setVoltage(double pivotVolts, double rollerVolts) {
  rollerVelocityControlEnabled = false;
  rollerMotor.setControl(
      rollerVoltageRequest.withOutput(Volts.of(rollerVolts)));

  pivotPositionControlEnabled = false;
  pivotMotor.setControl(
      pivotVoltageRequest.withOutput(Volts.of(pivotVolts)));
}
```

### Your task

1. Disable roller velocity control.
2. Send the requested roller voltage to the roller TalonFX.
3. Disable pivot position control.
4. Send the requested pivot voltage to the pivot TalonFX.
5. Explain why the method accepts two voltage arguments.

### Explanation

The two motors have independent voltage requests, so the method can command each motor separately.

- `pivotVolts` is the direct voltage requested for the pivot.
- `rollerVolts` is the direct voltage requested for the roller.

Direct voltage control replaces the corresponding closed-loop request. The local control flags are therefore cleared to report that neither velocity nor position control is selected.

A voltage request specifies volts rather than a duty-cycle fraction. Use appropriate voltage limits and safety controls, especially when manually moving a pivot mechanism.

## Step 10: Implement `stop()`

The supplied implementation stops both motors and clears the closed-loop control flags:

```java
@Override
public void stop() {
  rollerVelocityControlEnabled = false;
  rollerMotor.setControl(
      rollerVoltageRequest.withOutput(Volts.of(0.0)));
  rollerMotor.stopMotor();

  pivotPositionControlEnabled = false;
  pivotMotor.setControl(
      rollerVoltageRequest.withOutput(Volts.of(0.0)));
  pivotMotor.stopMotor();
}
```

### Your task

1. Clear the roller velocity-control flag.
2. Send a zero-voltage request to the roller and stop it.
3. Clear the pivot position-control flag.
4. Send a zero-voltage request to the pivot and stop it.
5. Explain why stopping both motors is necessary.

### Explanation

The intake contains two independently actuated motors. Stopping only one would leave the other motor's previous request active.

The method clears the local closed-loop state flags, sends zero-voltage requests, and calls `stopMotor()` for both TalonFX devices.

**Code review note:** The supplied method uses `rollerVoltageRequest` for the pivot's zero-voltage request. Both request fields are `VoltageOut`, so the request type is compatible, but using `pivotVoltageRequest` would make the pivot control path consistent with the rest of the class.

The goal fields are not reset by `stop()`. They therefore retain the last requested setpoints, which can be useful when reviewing logs.

Stopping a motor does not necessarily mechanically lock the mechanism. Brake configuration, gravity, inertia, and the mechanism's physical design all affect what happens after output stops.

## Step 11: Convert the CANcoder Reading to an Absolute Angle

The CANcoder returns an absolute position in rotations. The intake needs a calibrated mechanism angle in radians.

The supplied helper is:

```java
private double getAbsoluteAngleRadians(double readingRotations) {
  double fromZero =
      readingRotations - IntakeConstants.Pivot.ENCODER_OFFSET_ROTATIONS;

  double fromMiddle =
      MathUtil.inputModulus(
          fromZero
              - IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS
                  * IntakeConstants.Pivot.ENCODER_RATIO,
          -0.5,
          0.5);

  double angleRotations =
      IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS
          + fromMiddle / IntakeConstants.Pivot.ENCODER_RATIO;

  return angleRotations * 2.0 * Math.PI;
}
```

### Your task

Follow the conversion:

1. Subtract `ENCODER_OFFSET_ROTATIONS`.
2. Calculate the encoder reading relative to the mechanism's travel midpoint.
3. Normalize that relative reading into the interval from `-0.5` to `0.5` rotations.
4. Divide by `ENCODER_RATIO` to convert the relative encoder movement to mechanism movement.
5. Add `TRAVEL_MIDDLE_ROTATIONS` to restore the mechanism midpoint.
6. Convert the resulting mechanism rotations to radians.

### Explanation

An absolute encoder's raw reading does not necessarily correspond to the mechanism's desired zero position.

- `ENCODER_OFFSET_ROTATIONS` compensates for the encoder's mounting alignment.
- `TRAVEL_MIDDLE_ROTATIONS` defines the mechanism's travel midpoint in the calibrated coordinate system.
- `ENCODER_RATIO` relates encoder rotations to mechanism rotations.

`MathUtil.inputModulus()` handles rotational wraparound. It maps the relative reading into the requested interval so that values near one full revolution can be treated as values near zero when appropriate.

The final multiplication by \(2\pi\) converts mechanism rotations to radians.

**Calibration requirement:** Verify the offset, midpoint, and ratio against the actual hardware. Incorrect calibration constants can produce an incorrect angle even when the CANcoder is communicating correctly.

## Step 12: Review the Complete Control and Telemetry Flow

The implementation follows this flow:

1. Higher-level intake code requests an operation through `IntakeIO`.
2. `IntakeIOReal` translates the request into a Phoenix 6 control request.
3. The appropriate TalonFX receives the request.
4. The roller TalonFX, pivot TalonFX, and CANcoder provide feedback.
5. `updateInputs()` copies measurements and local control state into `IntakeIOInputs`.
6. The higher-level mechanism code can use those inputs for logging, monitoring, and control decisions.

The IO layer contains the hardware-specific details. The mechanism layer can work with the interface and its input data rather than directly controlling TalonFX and CANcoder objects.

## Step 13: Verification Checklist

Before considering the implementation complete, verify the following:

- [ ] The class implements every required `IntakeIO` method.
- [ ] The roller TalonFX uses `IntakeConstants.Roller.CAN_ROLLER_ID`.
- [ ] The pivot TalonFX uses `IntakeConstants.Pivot.CAN_ID`.
- [ ] The CANcoder uses `IntakeConstants.Pivot.ENCODER_CAN_ID`.
- [ ] The roller and pivot TalonFX devices are on `CAN_S0`.
- [ ] The CANcoder is on the `CANivore` bus.
- [ ] The roller configuration is applied successfully.
- [ ] The roller integrated position is initialized as intended.
- [ ] The pivot TalonFX configuration is applied somewhere in the project.
- [ ] The pivot position request uses the units expected by its TalonFX configuration.
- [ ] Roller velocity requests use rotations per second.
- [ ] Pivot position requests convert radians to mechanism rotations.
- [ ] Pivot velocity telemetry is converted to radians per second if the source signal is rotations per second.
- [ ] Roller and pivot telemetry fields use the expected units.
- [ ] The CANcoder offset, midpoint, and ratio are calibrated.
- [ ] Velocity and position control flags are updated when changing control modes.
- [ ] Direct voltage control commands both motors independently.
- [ ] `stop()` stops both motors.
- [ ] The mechanism has suitable physical and software travel limits.
- [ ] Device communication and configuration status are checked where appropriate.

## Challenge Questions

1. Why does `IntakeIOReal` implement `IntakeIO` instead of being called directly by the OpMode?
2. Why are there separate TalonFX instances for the roller and pivot?
3. Why are the roller and pivot on `CAN_S0`, while the CANcoder is on `CANivore`?
4. What is the difference between a TalonFX's integrated position and the CANcoder's absolute position?
5. What does `VelocityVoltage` do that `VoltageOut` does not?
6. Why must the pivot position request be converted from radians to mechanism rotations?
7. Why should the roller's requested velocity be stored separately from its measured velocity?
8. What is the purpose of the CANcoder offset?
9. Why is `MathUtil.inputModulus()` useful for rotational measurements?
10. Why should `setVoltage()` disable both closed-loop control flags?
11. Why must `stop()` stop both motors?
12. What could happen if the pivot velocity signal is recorded in rotations per second but labeled as radians per second?
13. How could you unit-test the absolute-angle conversion without connecting real hardware?

## Expected Outcome

After completing this lesson, you should understand how the real intake IO implementation:

- Controls the roller using velocity or voltage requests.
- Controls the pivot using position or voltage requests.
- Reads telemetry from two TalonFX motors and one CANcoder.
- Tracks requested setpoints and selected control modes.
- Converts the pivot's absolute encoder reading into a calibrated mechanism angle.

The key design principle is that **the IO layer translates between the robot's mechanism-level interface and the physical hardware**, keeping motor-controller details out of higher-level robot logic.
