# Lesson: Implementing Real Intake Hardware with `IntakeIOReal`

## Overview

In this lesson, you will implement the `IntakeIO` interface using real hardware from CTRE Phoenix 6.

The `IntakeIOReal` class connects the robot's software to three physical devices:

- A TalonFX motor controlling the intake roller.
- A TalonFX motor controlling the intake pivot.
- A CANcoder measuring the pivot's absolute position.

You will learn how to configure motor controllers, read sensor measurements, convert encoder values into meaningful mechanism measurements, and send motor control requests.

This lesson builds on the previous lesson, **Creating the Intake IO Interface**. That interface defined what information the Intake mechanism needs and which operations its hardware implementation must support. This lesson implements those operations for the real robot.

## Learning Objectives

By the end of this lesson, you should be able to:

1. Explain the purpose of a real hardware IO implementation.
2. Create and manage CTRE Phoenix 6 motor controllers and encoders.
3. Understand CAN buses and CAN device IDs.
4. Read motor position, velocity, voltage, current, and temperature.
5. Convert motor and encoder measurements into mechanism units.
6. Understand absolute encoder offsets and angle wrapping.
7. Send velocity, position, and voltage control requests.
8. Track active control modes and their requested goals.
9. Identify common hardware integration and unit-conversion mistakes.

## Prerequisites

Before starting, you should understand:

- Java classes, interfaces, fields, and methods.
- The `IntakeIO` interface and its `IntakeIOInputs` data class.
- AdvantageKit's `@AutoLog` and `Logger.processInputs()` workflow.
- Basic motor controller concepts.
- Encoder position and velocity.
- The difference between closed-loop and open-loop control.
- WPILib units and CTRE Phoenix 6 control requests.

## 1. Understanding the IO Architecture

The robot's Intake mechanism should not need to know which motor controller or encoder is physically installed.

Instead, the mechanism interacts with the `IntakeIO` interface.

The real implementation is responsible for translating between the mechanism's software interface and the physical hardware.

The architecture looks like this:

```text
                 Intake
                   |
                   v
                IntakeIO
                   |
                   v
               IntakeIOReal
                   |
          +--------+--------+
          |        |        |
          v        v        v
       Roller    Pivot   CANcoder
       TalonFX   TalonFX  Encoder
```

The `Intake` class requests operations such as:

- Set the roller velocity.
- Set the pivot position.
- Apply voltage to both motors.
- Stop the motors.
- Update the latest sensor measurements.

The `IntakeIOReal` class translates those requests into Phoenix 6 commands.

**Important:** The mechanism class should not need to know about TalonFX objects, CANcoder objects, or Phoenix 6 control request types.

## 2. Creating the Real IO Implementation

Start by declaring the class:

```java
public class IntakeIOReal implements IntakeIO {
```

The `implements IntakeIO` declaration means this class provides the hardware implementation of the Intake IO interface.

Because the interface defines the operations expected by the mechanism, the real implementation must provide the corresponding hardware behavior.

This separation makes it possible to use different implementations for:

- Real robot hardware.
- Simulation.
- Unit testing.

For example, `IntakeIOReal` can operate the physical robot while a simulated implementation models the same mechanism without requiring physical motors.

## 3. Importing Units and Hardware Libraries

The class imports WPILib units and CTRE Phoenix 6 hardware classes.

```java
import static org.wpilib.units.Units.Amps;
import static org.wpilib.units.Units.Rotations;
import static org.wpilib.units.Units.RotationsPerSecond;
import static org.wpilib.units.Units.Volts;
```

Static imports allow us to use unit names directly instead of qualifying each one with `Units`.

For example:

```java
Volts.of(6.0);
Rotations.of(2.0);
RotationsPerSecond.of(10.0);
Amps.of(20.0);
```

These expressions create values with explicit units.

The class also imports Phoenix 6 hardware and control request types:

```java
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;

import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.hardware.TalonFX;
```

Each control request has a different purpose:

| Request | Purpose |
|---|---|
| `PositionVoltage` | Closed-loop position control |
| `VelocityVoltage` | Closed-loop velocity control |
| `VoltageOut` | Open-loop voltage control |

The distinction between these requests is important. A position request asks the motor controller to reach a target position. A voltage request directly applies a requested motor voltage.

## 4. Creating the Hardware Objects

The implementation creates three hardware objects:

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

### TalonFX

A `TalonFX` represents a CTRE motor controller.

Each controller is identified by:

1. Its CAN device ID.
2. The CAN bus to which it is connected.

The CAN ID comes from `IntakeConstants`, rather than being hardcoded in this class.

This makes hardware configuration easier to maintain and reduces the chance of accidentally using the wrong device ID.

### CANcoder

The `CANcoder` provides an absolute encoder measurement.

Unlike an integrated motor encoder, an absolute encoder can report its angular position after the robot is powered on without requiring the mechanism to first move to a known reference position.

This is especially useful for an arm or pivot mechanism.

### Understanding CAN buses

Notice that the two TalonFX controllers use:

```java
new CANBus(CANPort.CAN_S0)
```

The CANcoder uses:

```java
new CANBus("CANivore")
```

These are different CAN bus configurations.

Make sure the bus assigned to each device matches the physical wiring and the CAN network configuration used on the robot.

A device configured for the wrong CAN bus will not communicate as expected.

**Student checkpoint:** Find the CAN IDs and bus assignments for all three devices. Verify that they match the robot's wiring diagram.

## 5. Creating Reusable Control Requests

The class creates four reusable control request objects:

```java
private final VelocityVoltage rollerVelocityRequest =
    new VelocityVoltage(0.0);

private final PositionVoltage pivotPositionRequest =
    new PositionVoltage(0);

private final VoltageOut pivotVoltageRequest =
    new VoltageOut(0);

private final VoltageOut rollerVoltageRequest =
    new VoltageOut(0.0);
```

These objects describe how the TalonFX should control its motor.

Instead of creating a new request every time a command is sent, the implementation stores request objects as fields and updates their values when needed.

For example:

```java
rollerMotor.setControl(
    rollerVelocityRequest.withVelocity(
        RotationsPerSecond.of(rotationsPerSecond)));
```

The `withVelocity()` method updates the requested velocity.

Similarly:

```java
pivotMotor.setControl(
    pivotPositionRequest.withPosition(mechanismRotations));
```

The `withPosition()` method updates the requested position.

Reusing request objects keeps the implementation organized and avoids unnecessary object creation in frequently executed control code.

## 6. Tracking the Active Control Modes

The class stores several fields that describe the current control requests:

```java
private boolean rollerVelocityControlEnabled = false;
private double goalrollerVelocityRotationsPerSecond = 0.0;

private boolean pivotPositionControlEnabled;
private double pivotGoalRadians;
```

These fields track the software's requested control modes and goals.

For the roller:

- `rollerVelocityControlEnabled` indicates whether velocity control is active.
- `goalrollerVelocityRotationsPerSecond` stores the requested roller speed.

For the pivot:

- `pivotPositionControlEnabled` indicates whether position control is active.
- `pivotGoalRadians` stores the requested pivot angle.

These values are later copied into `IntakeIOInputs` so AdvantageKit can record them.

This is useful when diagnosing a mechanism that is not behaving as expected. You can compare the requested goal with the measured position or velocity.

**Important:** These fields represent the control mode requested by this software implementation. They do not independently prove that the motor controller accepted the request or successfully reached the goal.

## 7. Configuring the Hardware

The constructor initializes the roller motor:

```java
public IntakeIOReal() {
  rollerMotor.getConfigurator().apply(
      IntakeConstants.Roller.createMotorConfiguration());

  rollerMotor.setPosition(Rotations.of(0.0));
}
```

### Applying a motor configuration

The configuration is created in `IntakeConstants.Roller`.

This is where motor-specific settings should be defined, such as:

- Current limits.
- Neutral mode.
- Inversion.
- Motor output settings.
- Feedback configuration.
- Sensor-to-mechanism ratio.
- Closed-loop gains.

Keeping configuration outside the IO implementation makes the hardware setup easier to maintain.

### Zeroing the roller position

The following line sets the roller's reported position to zero:

```java
rollerMotor.setPosition(Rotations.of(0.0));
```

This establishes a software reference for the roller's integrated position sensor.

It does not physically move the roller.

### Student checkpoint: Configuration completeness

The constructor shown configures the roller motor, but does not explicitly configure the pivot motor or CANcoder.

Before deploying this implementation, verify that:

1. The pivot motor receives its intended configuration.
2. The pivot motor's feedback source and sensor-to-mechanism ratio are configured.
3. The CANcoder's direction, magnet offset, and other required settings are configured.
4. Configuration calls are checked for errors or timeouts.

Do not assume that configuring the roller also configures the pivot.

## 8. Updating Sensor Inputs

The `updateInputs()` method reads the hardware and copies the measurements into the AdvantageKit input object.

```java
@Override
public void updateInputs(IntakeIOInputs inputs) {
```

This method is called by the Intake mechanism during its periodic update.

The method should read the latest hardware measurements and populate the input fields.

### Reading roller measurements

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
```

These readings provide information about the roller.

| Input | Meaning |
|---|---|
| Position | Roller position in rotations |
| Velocity | Roller speed in rotations per second |
| Applied volts | Voltage currently applied to the motor |
| Stator current | Current measured at the motor stator |
| Temperature | Motor device temperature |

The values are copied into the `IntakeIOInputs` object so the mechanism and AdvantageKit can use them.

### Recording the roller control request

```java
inputs.rollerVelocityControlEnabled =
    rollerVelocityControlEnabled;

inputs.rollerGoalVelocityRotationsPerSecond =
    goalrollerVelocityRotationsPerSecond;
```

These values describe the software's requested control state.

They are not sensor measurements. They are useful because logs can show both what the mechanism requested and what the hardware measured.

## 9. Reading Pivot Measurements

The pivot has several measurements that need to be converted into mechanism units.

### Pivot position

```java
inputs.pivotPositionRadians =
    pivotMotor.getPosition().refresh().getValue().in(Rotations)
        * 2.0 * Math.PI;
```

The motor position is read in rotations and converted to radians.

One full rotation is equal to \(2\pi\) radians.

\[
\text{Radians} = \text{Rotations} \times 2\pi
\]

For example:

\[
0.25 \text{ rotations} \times 2\pi
= \frac{\pi}{2} \text{ radians}
\]

That is a quarter turn, or 90 degrees.

This conversion is correct only if the TalonFX position signal is already configured to represent mechanism rotations rather than raw motor rotations.

The sensor-to-mechanism ratio must therefore be configured correctly.

### Pivot velocity

```java
inputs.pivotVelocityRadiansPerSecond =
    pivotMotor.getVelocity().getValueAsDouble();
```

This line deserves special attention.

The input field is named `pivotVelocityRadiansPerSecond`, but the TalonFX velocity signal is generally expressed in rotations per second when using the default rotational units.

If that is the signal's configured unit, it must be converted:

```java
inputs.pivotVelocityRadiansPerSecond =
    pivotMotor.getVelocity().getValueAsDouble()
        * 2.0 * Math.PI;
```

The unit stored in an input field must match the unit in its name and documentation.

A mismatch can cause incorrect telemetry, control calculations, and simulation behavior.

### Pivot electrical measurements

```java
inputs.pivotAppliedVolts =
    pivotMotor.getMotorVoltage().refresh().getValue().in(Volts);

inputs.pivotCurrentAmps =
    pivotMotor.getStatorCurrent().refresh().getValue().in(Amps);

inputs.pivotTemperatureCelsius =
    pivotMotor.getDeviceTemp().getValueAsDouble();
```

These measurements help us understand the electrical and thermal behavior of the pivot motor.

For example:

- Applied voltage helps diagnose motor output.
- Stator current can help identify high mechanical loads or a stalled mechanism.
- Temperature can help identify overheating.

## 10. Reading the Absolute Encoder

The CANcoder measures the pivot's absolute position.

```java
double encoderRotations =
    encoder.getAbsolutePosition()
        .refresh()
        .getValue()
        .in(Rotations);
```

The raw encoder reading is then copied into the input object:

```java
inputs.pivotAbsoluteEncoderRotations = encoderRotations;
```

The raw reading is also converted into a corrected angle:

```java
inputs.pivotAbsoluteAngleRadians =
    getAbsoluteAngleRadians(encoderRotations);
```

Why do we need two values?

The raw encoder reading represents the sensor's reported position. The corrected angle represents the position of the actual mechanism after applying the robot's calibration and geometry.

This distinction is important when debugging an absolute encoder.

## 11. Converting the Absolute Encoder Reading

The helper method converts the encoder reading into the mechanism's corrected angle:

```java
private double getAbsoluteAngleRadians(double readingRotations) {
  double fromZero =
      readingRotations
          - IntakeConstants.Pivot.ENCODER_OFFSET_ROTATIONS;

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

This method performs three important operations.

### Step 1: Apply the encoder offset

```java
double fromZero =
    readingRotations
        - IntakeConstants.Pivot.ENCODER_OFFSET_ROTATIONS;
```

The encoder may not report zero when the intake is at its defined zero angle.

The offset corrects for the difference between the sensor's zero and the mechanism's zero.

### Step 2: Wrap the encoder reading

```java
double fromMiddle =
    MathUtil.inputModulus(
        fromZero
            - IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS
                * IntakeConstants.Pivot.ENCODER_RATIO,
        -0.5,
        0.5);
```

An absolute encoder reports a position that wraps around after one rotation.

For example, a reading close to zero and a reading close to one rotation can represent nearly the same physical angle.

`MathUtil.inputModulus()` places the value into the requested interval.

Here, the interval is:

\[
[-0.5,\ 0.5)
\]

This creates a signed representation around the selected middle point.

### Step 3: Convert to mechanism rotations

```java
double angleRotations =
    IntakeConstants.Pivot.TRAVEL_MIDDLE_ROTATIONS
        + fromMiddle / IntakeConstants.Pivot.ENCODER_RATIO;
```

The encoder ratio converts the wrapped encoder measurement into mechanism rotations.

The result is then converted to radians:

```java
return angleRotations * 2.0 * Math.PI;
```

### Why this matters

The absolute encoder's position is not necessarily the same as the pivot's mechanism angle.

The conversion accounts for:

- Sensor mounting position.
- Encoder offset.
- The mechanism's selected center point.
- The relationship between encoder rotations and mechanism rotations.

**Student checkpoint:** Draw a diagram showing the pivot's zero angle, the encoder's zero position, and the direction of positive rotation. Explain why an offset is necessary.

## 12. Setting the Pivot Position

The `setPosition()` method requests closed-loop position control:

```java
@Override
public void setPosition(double positionRadians) {
  pivotGoalRadians = positionRadians;
  pivotPositionControlEnabled = true;

  double mechanismRotations =
      positionRadians / (2.0 * Math.PI);

  pivotMotor.setControl(
      pivotPositionRequest.withPosition(mechanismRotations));
}
```

The method receives an angle in radians.

The TalonFX position request is configured to use mechanism rotations, so the requested angle must be converted.

\[
\text{Rotations} =
\frac{\text{Radians}}{2\pi}
\]

For example, a request of \(\pi\) radians becomes:

\[
\frac{\pi}{2\pi} = 0.5 \text{ rotations}
\]

The method also updates the goal and control-mode fields so AdvantageKit can record the requested state.

### Important configuration requirement

This conversion assumes the TalonFX position signal and control request are configured in mechanism rotations.

If the TalonFX is configured to report motor rotations instead, the request must also account for the gearing between the motor and pivot.

Never assume that a motor rotation is the same as a mechanism rotation.

## 13. Setting the Roller Velocity

The `setVelocity()` method requests closed-loop velocity control:

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

The method receives a target velocity in rotations per second.

It then:

1. Marks roller velocity control as enabled.
2. Stores the requested velocity for logging.
3. Updates the Phoenix 6 velocity request.
4. Sends the request to the roller motor.

The TalonFX uses its configured feedback and closed-loop gains to try to achieve the requested speed.

This is different from applying a fixed percentage of motor output.

For example:

- A velocity request asks the motor to maintain a target speed.
- A voltage request asks the motor controller to apply a specified voltage.

The motor may need to apply different voltages as the mechanical load changes to maintain a requested velocity.

## 14. Applying Open-Loop Voltage

The `setVoltage()` method controls both motors using direct voltage requests:

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

Unlike velocity and position control, this method does not request a particular speed or angle.

Instead, it applies the requested voltage to each motor.

The method also disables the corresponding software control-mode flags.

This is useful for:

- Manual mechanism testing.
- Duty-cycle style control.
- Characterization.
- Open-loop movement.
- Simple testing before closed-loop gains are tuned.

### Voltage versus duty cycle

A duty cycle is a normalized motor output, often between -1.0 and +1.0.

Voltage is measured in volts.

A common conversion is:

\[
V_{\text{requested}} =
\text{Duty Cycle} \times V_{\text{battery}}
\]

For example, a duty cycle of 0.5 with a 12-volt battery corresponds to a requested voltage of 6 volts.

The `setVoltage()` method accepts volts, not a duty-cycle percentage.

## 15. Stopping the Motors

The `stop()` method stops both motors:

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

The method clears the control-mode flags and sends zero-output requests.

It then calls `stopMotor()` on both TalonFX controllers.

### Code review checkpoint

The pivot motor is currently sent the `rollerVoltageRequest` object:

```java
pivotMotor.setControl(
    rollerVoltageRequest.withOutput(Volts.of(0.0)));
```

Both fields use the `VoltageOut` type, so this may work, but it is clearer and safer to use the pivot's own request object:

```java
pivotMotor.setControl(
    pivotVoltageRequest.withOutput(Volts.of(0.0)));
```

This keeps each motor's control request separate and makes the code easier to understand.

Also verify the intended neutral behavior when stopping. `stopMotor()` does not automatically mean that a pivot will mechanically hold its position. The motor's configured neutral mode and the mechanism's physical design determine what happens after output is removed.

## 16. How AdvantageKit Uses This Implementation

The `IntakeIOReal` class does not directly call `Logger.processInputs()`.

Instead, it fills the `IntakeIOInputs` object provided by the Intake mechanism.

The mechanism is responsible for logging the inputs:

```java
io.updateInputs(inputs);
Logger.processInputs("Intake", inputs);
```

The overall data flow is:

```text
Physical Hardware
       |
       v
   IntakeIOReal
       |
       v
  IntakeIOInputs
       |
       v
Logger.processInputs()
       |
       v
 AdvantageKit Logs
```

This design keeps hardware communication separate from logging and mechanism behavior.

It also allows the same mechanism code to run with real hardware, simulation, or a test implementation.

## 17. Important Code Review Findings

Before deploying this implementation, review the following items.

### A. Configure every hardware device

The constructor configures only the roller motor.

Verify that the pivot motor and CANcoder receive their required configurations, including the feedback ratio, encoder offset, and direction.

### B. Correct the pivot velocity unit

The input field is named `pivotVelocityRadiansPerSecond`, but the code currently assigns the TalonFX signal's raw `getValueAsDouble()` result.

Verify the configured signal unit. If it is rotations per second, convert it to radians per second:

```java
inputs.pivotVelocityRadiansPerSecond =
    pivotMotor.getVelocity().getValueAsDouble()
        * 2.0 * Math.PI;
```

### C. Use the correct request object in `stop()`

Use `pivotVoltageRequest` for the pivot motor rather than reusing `rollerVoltageRequest`.

### D. Check hardware configuration and request status

Phoenix 6 operations can report status information.

For production robot code, check important configuration and sensor operations so that a failed configuration or disconnected device does not silently go unnoticed.

Also consider whether signals should be refreshed individually or managed through a configured signal update frequency. Avoid unnecessary synchronous refreshes in a frequently executed periodic loop.

### E. Validate encoder conversion

Test the absolute encoder conversion at several known pivot positions:

- Mechanism zero.
- Middle of the travel range.
- Minimum travel.
- Maximum travel.
- Positions immediately on either side of the encoder wrap boundary.

The corrected angle should remain continuous across the positions where the raw absolute encoder reading wraps.

### F. Keep the control state accurate

The boolean fields track requested control modes. They should be updated consistently whenever control changes, including when a motor is stopped or switched to a different control mode.

Consider whether a failed motor request should leave the software reporting that a control mode is active.

## 18. Student Exercises

Complete the following exercises to demonstrate your understanding.

### Exercise 1: Hardware configuration

1. Find the CAN ID for the roller motor.
2. Find the CAN ID for the pivot motor.
3. Find the CAN ID for the CANcoder.
4. Identify which CAN bus each device uses.
5. Explain why the pivot motor needs its own configuration.

### Exercise 2: Unit conversion

Convert the following values:

| Measurement | Expected conversion |
|---|---|
| 0.25 rotations | Radians |
| 0.5 rotations | Radians |
| 1.0 rotations | Radians |
| 1.0 rotations per second | Radians per second |
| 90 degrees | Rotations |

Show your calculations.

### Exercise 3: Absolute encoder

Explain the purpose of each of these constants:

- `ENCODER_OFFSET_ROTATIONS`
- `TRAVEL_MIDDLE_ROTATIONS`
- `ENCODER_RATIO`

Then explain what could happen if the encoder offset is incorrect.

### Exercise 4: Add a pivot voltage request

Modify `setVoltage()` to clamp the requested pivot voltage to the currently available battery voltage.

Explain why a requested voltage should not exceed the available supply voltage.

### Exercise 5: Improve stop behavior

Modify `stop()` so that:

- Both control-mode flags are cleared.
- Both goal values are reset to a clearly defined state.
- Each motor uses its own voltage request object.
- The intended motor neutral behavior is documented.

### Exercise 6: Add error reporting

Investigate how Phoenix 6 reports configuration and control request failures.

Add an appropriate way to detect and report a motor configuration failure.

Do not silently assume that a configuration request succeeded.

### Exercise 7: Validate encoder calibration

Create a small test or diagnostic routine that records:

- Raw encoder rotations.
- Corrected pivot angle in radians.
- Integrated pivot position in radians.

Move the pivot through its travel range and compare the measurements.

Explain what you would investigate if the absolute and integrated positions disagree.

## 19. Knowledge Check

Answer these questions before moving on.

1. Why does `IntakeIOReal` implement `IntakeIO`?
2. What is the difference between `VelocityVoltage` and `VoltageOut`?
3. Why are control request objects stored as fields?
4. What does `refresh()` do when reading a Phoenix 6 signal?
5. Why does the pivot position need to be converted from rotations to radians?
6. What is the purpose of an absolute encoder offset?
7. Why does an absolute encoder reading need to account for wraparound?
8. Why should the pivot and roller have separate control requests?
9. What is the difference between a requested goal and a measured sensor value?
10. Why should hardware configuration errors be checked?

## 20. What Comes Next?

After completing this lesson, you should understand how the Intake mechanism communicates with real hardware.

The next step is to implement or review the simulation version of the IO interface.

A simulation implementation should provide the same measurements and control operations as the real implementation, but use simulated motor and mechanism models instead of physical TalonFX controllers.

The goal is for the `Intake` mechanism to operate consistently regardless of whether it is running on the real robot, in simulation, or in a unit test.