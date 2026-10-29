# Lesson: Implementing Real-Hardware Intake IO

## Objective

In this lesson, you will implement `IntakeIOReal`, the hardware-specific input/output layer for the robot's intake pivot mechanism.

This class uses CTRE Phoenix 6 to communicate with a TalonFX motor controller and a CANcoder absolute encoder. The TalonFX handles closed-loop position control and direct voltage output, while the class reports sensor readings and motor telemetry through the `IntakeIO` interface.

By the end of this lesson, you should understand how to:

- Implement a hardware IO interface.
- Configure a TalonFX using `TalonFXConfiguration`.
- Configure feedback ratios, PID gains, current limits, and neutral mode.
- Use a CANcoder to measure absolute mechanism position.
- Convert between rotations and radians.
- Command a mechanism using position control or voltage control.
- Record motor voltage, current, temperature, position, and velocity.
- Normalize encoder readings to calculate a mechanism angle.
- Separate hardware implementation from mechanism logic.

## Prerequisites

Before starting, you should be familiar with:

- Java classes, interfaces, constructors, and instance fields.
- The `IntakeIO` interface and its `IntakeIOInputs` data structure.
- CTRE Phoenix 6 and TalonFX motor controllers.
- CAN bus device configuration.
- PID feedback control.
- WPILib units and `MathUtil`.
- The constants defined in `IntakeConstants.Pivot`.

## Step 1: Create the Hardware IO Class

Create the file:

`src/main/java/first/robot/mechanisms/IntakeIOReal.java`

Start with the following skeleton:

```java
package first.robot.mechanisms;

import first.robot.constants.IntakeConstants;

/**
 * Real-hardware implementation of {@link IntakeIO}.
 */
public class IntakeIOReal implements IntakeIO {

  /** Creates and configures the real intake hardware. */
  public IntakeIOReal() {
    // TODO: Configure the hardware.
  }

  @Override
  public void updateInputs(IntakeIOInputs inputs) {
    // TODO: Read hardware telemetry.
  }

  @Override
  public void setPosition(double positionRadians) {
    // TODO: Command a mechanism position.
  }

  @Override
  public void setVoltage(double volts) {
    // TODO: Command motor voltage.
  }

  @Override
  public void stop() {
    // TODO: Stop the motor.
  }
}
```

### Your task

1. Declare a class named `IntakeIOReal`.
2. Implement `IntakeIO`.
3. Implement all required interface methods.
4. Use the constructor to initialize and configure the hardware.

### Explanation

`IntakeIOReal` is responsible for communicating with the physical robot hardware. It should not be responsible for controller button mappings or autonomous routines.

The `IntakeIO` interface defines the operations that the rest of the robot code can use without depending directly on a TalonFX or CANcoder.

This separation makes it possible to provide alternative implementations, such as simulation or test IO, without changing the higher-level intake mechanism.

## Step 2: Declare the Hardware Devices

The intake pivot uses two CAN devices:

- A TalonFX motor controller to actuate the mechanism.
- A CANcoder absolute encoder to measure the pivot's absolute position.

Add the required imports and fields:

```java
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;

private final TalonFX motor =
    new TalonFX(
        IntakeConstants.Pivot.CAN_ID,
        new CANBus("CANivore"));

private final CANcoder encoder =
    new CANcoder(
        IntakeConstants.Pivot.ENCODER_CAN_ID,
        new CANBus("CANivore"));
```

### Your task

- Use the CAN IDs from `IntakeConstants.Pivot`.
- Create both devices on the configured CANivore bus.
- Explain the difference between the motor controller and the encoder.

### Explanation

The TalonFX controls the motor and provides integrated feedback signals. The CANcoder measures the mechanism's absolute angular position independently of the motor's integrated sensor.

Using constants for CAN IDs avoids scattering hardware configuration throughout the codebase.

**Important:** Confirm that both devices are actually connected to the CANivore network and that their configured IDs match the hardware.

## Step 3: Create Reusable Control Requests

Phoenix 6 uses control request objects to specify how a motor should operate.

Add these fields:

```java
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VoltageOut;

private final PositionVoltage positionRequest =
    new PositionVoltage(0);

private final VoltageOut voltageRequest =
    new VoltageOut(0);
```

### Your task

1. Create one request for position control.
2. Create another request for direct voltage output.
3. Explain why the class maintains separate requests.

### Explanation

`PositionVoltage` requests a target position using the TalonFX's configured position feedback and voltage-based control.

`VoltageOut` requests a specified motor voltage directly.

The same motor can use either request, but they represent different control strategies. Position control attempts to reach a target, while direct voltage control does not inherently regulate the mechanism to a target position.

Reusing request objects also avoids repeatedly allocating new control request instances for each command.

## Step 4: Track the Control State

Add the following fields:

```java
private boolean positionControlEnabled;
private double goalRadians;
```

### Your task

Explain what each field represents and identify which methods should update it.

### Explanation

- `positionControlEnabled` records whether the IO layer most recently selected position control.
- `goalRadians` records the most recently requested position in radians.

The position command should update both fields. Direct voltage control and stopping should disable the position-control flag.

These fields also allow the higher-level logging system to distinguish the requested control state from the measured physical state.

Note that the boolean represents the selected control mode in this implementation. It does not independently verify that the motor controller successfully accepted the command.

## Step 5: Configure the TalonFX

The constructor must configure the motor controller before it is used.

Start with:

```java
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

TalonFXConfiguration config = new TalonFXConfiguration();
```

### Your task

Configure the following settings:

1. Sensor-to-mechanism ratio.
2. PID gains.
3. Arm gravity compensation type.
4. Neutral mode.
5. Stator current limit.
6. Initial integrated position.

Use the corresponding values from `IntakeConstants.Pivot`.

### Explanation

#### Sensor-to-mechanism ratio

```java
config.Feedback.withSensorToMechanismRatio(
    IntakeConstants.Pivot.GEAR_RATIO);
```

This configures the relationship between the TalonFX's rotor rotations and mechanism rotations.

The position requests sent to the controller must use the same mechanism coordinate system that the controller is configured to interpret.

#### PID gains

```java
config.Slot0
    .withKP(IntakeConstants.Pivot.KP)
    .withKI(IntakeConstants.Pivot.KI)
    .withKD(IntakeConstants.Pivot.KD);
```

The PID controller uses feedback to reduce the difference between the requested position and the measured position.

- \(K_P\) responds to present error.
- \(K_I\) responds to accumulated error.
- \(K_D\) responds to the rate of change of error.

The appropriate gains depend on the mechanism, gearing, inertia, and desired response.

#### Gravity compensation configuration

```java
config.Slot0
    .withGravityType(GravityTypeValue.Arm_Cosine)
    .withKG(0.0);

config.Slot0
    .withKS(0.0)
    .withKV(0.0)
    .withKA(0.0);
```

`Arm_Cosine` selects an arm-style gravity feedforward model. The configured \(K_G\) value is zero, so this configuration does not request a nonzero gravity feedforward contribution.

The remaining feedforward gains are also set to zero.

#### Neutral mode

```java
config.MotorOutput.withNeutralMode(
    NeutralModeValue.Brake);
```

Brake mode configures the motor controller to resist motion when output is neutral, rather than allowing the mechanism to coast freely.

The actual safety and holding behavior still depends on the mechanism design and controller configuration.

#### Current limiting

```java
config.CurrentLimits
    .withStatorCurrentLimit(
        Amps.of(
            IntakeConstants.Pivot.STATOR_CURRENT_LIMIT_AMPS))
    .withStatorCurrentLimitEnable(true);
```

This enables the configured stator current limit to help protect the motor and mechanism.

#### Apply configuration and initialize position

```java
motor.getConfigurator().apply(config);
motor.setPosition(0);
```

The configuration is applied to the TalonFX, and its integrated position is initialized to zero.

**Hardware consideration:** Zeroing the integrated sensor at startup establishes a reference; it does not automatically make that reference physically correct. Ensure the mechanism is in a known, safe position before relying on this zero. Also consider checking configuration and sensor-operation status in production code.

## Step 6: Implement `updateInputs()`

The IO layer must periodically populate the telemetry structure.

Start with:

```java
@Override
public void updateInputs(IntakeIOInputs inputs) {
  // TODO: Read motor position.
  // TODO: Read motor velocity.
  // TODO: Read applied voltage.
  // TODO: Read stator current.
  // TODO: Read motor temperature.
  // TODO: Read absolute encoder position.
  // TODO: Report control state and goal.
}
```

### Your task

Populate the following fields:

| Input field | Source | Purpose |
|---|---|---|
| `pivotPositionRadians` | TalonFX position | Integrated mechanism position |
| `pivotVelocityRadiansPerSecond` | TalonFX velocity | Mechanism velocity |
| `pivotAppliedVolts` | Motor voltage signal | Actual applied motor voltage |
| `pivotCurrentAmps` | Stator current signal | Motor current telemetry |
| `pivotTemperatureCelsius` | Device temperature signal | Motor temperature |
| `pivotAbsoluteEncoderRotations` | CANcoder | Absolute encoder reading |
| `pivotAbsoluteAngleRadians` | Converted CANcoder reading | Calculated absolute mechanism angle |
| `pivotPositionControlEnabled` | Local state | Selected control mode |
| `pivotGoalRadians` | Local state | Requested position |

### Explanation

The method reads the hardware's latest available telemetry and writes it into the shared input structure.

For example, a TalonFX position signal is expressed in rotations in this implementation. To convert rotations to radians:

\[
\theta_{\text{rad}}=\theta_{\text{rot}}\times 2\pi
\]

Use WPILib units to make the conversion explicit:

```java
motor.getPosition()
    .refresh()
    .getValue()
    .in(Rotations) * 2.0 * Math.PI;
```

For voltage and current, use `Volts` and `Amps` respectively.

The encoder reading should be converted into the mechanism's calibrated angular coordinate system, rather than assuming the raw absolute reading is already the desired angle.

**Telemetry note:** Refreshing some signals while reading others without an explicit refresh may produce readings with different update ages. For more consistent telemetry, consider refreshing the required signals together and checking their status before consuming them.

## Step 7: Implement Position Control

The intake must be able to move toward a requested position.

Complete this method:

```java
@Override
public void setPosition(double positionRadians) {
  // TODO: Save the requested goal.
  // TODO: Enable position-control state.
  // TODO: Convert radians to mechanism rotations.
  // TODO: Send the position request to the TalonFX.
}
```

### Your task

Use the following relationship:

\[
\theta_{\text{rot}}=
\frac{\theta_{\text{rad}}}{2\pi}
\]

Then send the converted target through `PositionVoltage`.

### Explanation

The IO interface accepts radians, while the TalonFX configuration in this implementation expects mechanism rotations.

The conversion allows the rest of the robot code to work in radians without needing to know the controller's configured position units.

The method should also save the requested target and mark position control as enabled.

Position control is useful when the intake needs to move to a defined angle, such as a stowed position or an extended position.

**Important:** Position control should be paired with appropriate mechanical limits, software limits, and safe startup behavior. A target position alone does not guarantee that the mechanism can reach the target safely.

## Step 8: Implement Direct Voltage Control

The intake must also support direct voltage commands.

Complete the method:

```java
@Override
public void setVoltage(double volts) {
  // TODO: Disable position-control state.
  // TODO: Send a voltage request to the TalonFX.
}
```

Use the `VoltageOut` request and its `withOutput(...)` method.

### Your task

Explain why the position-control flag should be disabled when switching to direct voltage control.

### Explanation

Direct voltage control replaces the position-control request with an explicit voltage request. The IO layer should therefore report that position control is no longer selected.

The voltage argument is expressed in volts. Unlike duty-cycle control, a voltage request specifies a voltage rather than a fraction of the available output.

This mode can be useful for controlled testing, manual mechanism movement, or other applications where direct motor output is required.

Use voltage limits and appropriate safety controls to avoid damaging the mechanism.

## Step 9: Implement `stop()`

Complete the stop method:

```java
@Override
public void stop() {
  // TODO: Disable position-control state.
  // TODO: Stop the motor.
}
```

### Your task

Use the TalonFX's `stopMotor()` method and ensure the local control-state flag is updated.

### Explanation

Stopping the motor ends the active output request. The local state should reflect that the IO layer is no longer commanding position control.

The `goalRadians` field may retain the last requested goal for logging purposes. Retaining that value can help diagnose what the mechanism was attempting to do before it stopped.

Stopping the motor does not necessarily mean the mechanism is mechanically locked. Brake mode, gravity, inertia, and the mechanism's physical design all affect what happens after output stops.

## Step 10: Convert the CANcoder Reading to an Absolute Angle

The CANcoder provides an absolute position reading in rotations. The robot needs a calibrated mechanism angle in radians.

Implement the conversion helper:

```java
private double getAbsoluteAngleRadians(
    double readingRotations) {

  // TODO: Apply the encoder offset.
  // TODO: Normalize the relative reading.
  // TODO: Convert the normalized reading
  //       into mechanism rotations.
  // TODO: Return radians.
}
```

Use these constants:

- `ENCODER_OFFSET_ROTATIONS`
- `TRAVEL_MIDDLE_ROTATIONS`
- `ENCODER_RATIO`

### Your task

Follow these steps:

1. Subtract the configured encoder offset.
2. Calculate the relative position from the mechanism's travel midpoint.
3. Normalize the relative reading into the range `-0.5` to `0.5` rotations.
4. Divide the normalized reading by the encoder ratio.
5. Add the midpoint position back.
6. Convert the result to radians.

### Explanation

An absolute encoder's raw reading may not correspond directly to the mechanism's desired zero position. The offset compensates for the mounting alignment, while the ratio relates encoder motion to mechanism motion.

The `MathUtil.inputModulus(...)` method normalizes a value into a specified interval. This is important because rotational measurements wrap around after a full revolution.

For example, a reading near one full rotation can represent an angle close to zero rather than an angle almost one full revolution away.

The helper uses the calibrated midpoint and encoder ratio to transform the normalized measurement into the mechanism's angular coordinate system.

**Calibration requirement:** Verify the encoder offset, ratio, and midpoint against the actual hardware. Incorrect values can cause the reported angle to be wrong even when the encoder itself is functioning correctly.

## Step 11: Review the Control and Telemetry Flow

The completed implementation follows this general flow:

1. The higher-level intake mechanism requests an action through `IntakeIO`.
2. `IntakeIOReal` translates that request into a Phoenix 6 control request.
3. The TalonFX applies the requested control strategy.
4. The motor and CANcoder provide feedback signals.
5. `updateInputs()` reads those signals and populates the input structure.
6. The higher-level robot code can use those inputs for logging, monitoring, and control decisions.

This design keeps the hardware-specific details in one place.

## Step 12: Verification Checklist

Before considering the implementation complete, verify the following:

- [ ] The class implements every required `IntakeIO` method.
- [ ] The TalonFX and CANcoder use the correct CAN IDs and bus.
- [ ] The sensor-to-mechanism ratio is correct.
- [ ] PID gains and current limits come from the constants class.
- [ ] The neutral mode is configured as intended.
- [ ] The integrated position reference is initialized safely.
- [ ] Position requests are converted from radians to rotations.
- [ ] Telemetry fields use the expected units.
- [ ] The absolute encoder offset and ratio are calibrated.
- [ ] Position control, voltage control, and stopping update the control state correctly.
- [ ] Hardware configuration and sensor status are checked where appropriate.
- [ ] The mechanism has suitable physical and software travel limits.

## Challenge Questions

1. Why does `IntakeIOReal` implement `IntakeIO` instead of being called directly by the OpMode?
2. What is the difference between the TalonFX's integrated position and the CANcoder's absolute position?
3. Why does the TalonFX need a sensor-to-mechanism ratio?
4. Why must the requested position be converted from radians to rotations?
5. What is the purpose of the encoder offset?
6. Why is `MathUtil.inputModulus()` useful for rotational measurements?
7. How does position control differ from direct voltage control?
8. Why might a mechanism move after `stopMotor()` is called?
9. What risks arise from initializing the integrated sensor to zero without verifying the mechanism's physical position?
10. How could you test the absolute-angle conversion without connecting real hardware?

## Expected Outcome

After completing this lesson, you should have a real-hardware IO implementation that configures the intake pivot motor, reads motor and encoder telemetry, supports position and voltage control, and converts absolute encoder readings into calibrated mechanism angles.

The key design principle is that **the IO layer translates between the robot's mechanism-level interface and the physical hardware**, keeping motor-controller details out of the higher-level robot logic.
