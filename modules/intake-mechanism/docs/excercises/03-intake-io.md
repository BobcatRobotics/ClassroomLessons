# Lesson: Designing the Intake IO Interface

## Objective

In this lesson, you will implement `IntakeIO`, the hardware abstraction interface for the robot's intake mechanism.

The interface defines how the rest of the robot code interacts with the intake without depending on a specific motor controller, encoder, or simulation implementation.

It also defines the `IntakeIOInputs` data structure. This structure contains telemetry for both the intake roller and pivot, including measured values, absolute encoder data, and requested control state. AdvantageKit uses the `@AutoLog` annotation to generate a logging-friendly version of this structure.

By the end of this lesson, you should understand how to:

- Design a Java interface for hardware abstraction.
- Define a shared telemetry data structure for multiple mechanism components.
- Use AdvantageKit's `@AutoLog` annotation.
- Document units and physical quantities.
- Declare default methods for optional hardware operations.
- Represent both closed-loop and open-loop control requests.
- Separate hardware behavior from mechanism logic.
- Support real hardware, simulation, and testing through a shared interface.

## Prerequisites

Before starting, you should be familiar with:

- Java interfaces, classes, fields, and methods.
- JavaDoc comments.
- The difference between a declaration and an implementation.
- The basics of AdvantageKit logging.
- The `IntakeIOReal` implementation.
- The concepts of velocity control, position control, and voltage control.
- Angular position and velocity units, including rotations, radians, and rotations per second.

## Step 1: Create the Interface

Create the file:

`src/main/java/first/robot/mechanisms/IntakeIO.java`

Start with the following skeleton:

```java
package first.robot.mechanisms;

/**
 * Hardware abstraction for the Intake flywheel.
 *
 * <p>Implementations provide real hardware or simulated motor behavior.
 */
public interface IntakeIO {

}
```

### Your task

1. Declare an interface named `IntakeIO`.
2. Add JavaDoc describing its purpose.
3. Explain why this is an interface rather than a concrete class.

### Explanation

The interface defines the operations and telemetry structure that an intake IO implementation makes available.

Different classes can implement the same interface:

- `IntakeIOReal` communicates with physical hardware.
- A simulation implementation models the intake mechanism.
- A test implementation supplies controlled sensor values without requiring real hardware.

The higher-level intake mechanism can depend on `IntakeIO` rather than knowing which implementation is being used.

This is an example of **dependency abstraction**: the mechanism depends on a contract, not on a specific hardware implementation.

## Step 2: Define the Input Data Structure

Inside `IntakeIO`, declare a nested class named `IntakeIOInputs`.

```java
@AutoLog
class IntakeIOInputs {

}
```

Add the import and JavaDoc:

```java
import org.littletonrobotics.junction.AutoLog;

/**
 * Inputs measured from the Intake.
 */
@AutoLog
class IntakeIOInputs {

}
```

### Your task

Create a nested class to hold the intake's sensor readings, motor telemetry, and control state.

### Explanation

The input class provides a consistent structure for communicating measured and requested values from the IO layer to the rest of the robot program.

The `@AutoLog` annotation allows AdvantageKit's annotation processor to generate an associated logging-friendly class, commonly named `IntakeIOInputsAutoLogged`.

The generated class can be used by the higher-level mechanism to process inputs through AdvantageKit's logging system:

```java
IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();
```

The generated class is not something you manually implement. It is produced during the build when the relevant annotation-processing configuration is enabled.

## Step 3: Add Roller Position and Velocity Telemetry

The roller is a rotating intake component. Add fields to represent its integrated position and measured velocity:

```java
/** Flywheel position in rotations. */
public double rollerPositionRotations = 0.0;

/** Flywheel velocity in rotations per second. */
public double rollerVelocityRotationsPerSecond = 0.0;
```

### Your task

1. Add both fields with their initial values.
2. Preserve the JavaDoc comments.
3. Explain the difference between position and velocity.
4. Explain why the roller uses rotations and rotations per second in this interface.

### Explanation

`rollerPositionRotations` represents the integrated roller position, measured in complete rotations.

`rollerVelocityRotationsPerSecond` represents how quickly the roller rotates, measured in rotations per second.

Position describes how far the roller has rotated. Velocity describes its rate of rotation.

Angular velocity can be expressed as:

\[
\omega = \frac{d\theta}{dt}
\]

where \(\theta\) is angular position and \(t\) is time in seconds.

The interface uses rotations for the roller because its velocity-control request is expressed in rotations per second. Keeping the units explicit in field names helps prevent confusion with radians, degrees, or other units.

## Step 4: Add Roller Motor Telemetry

Add the roller's electrical and thermal measurements:

```java
/** Applied motor voltage in volts. */
public double rollerAppliedVolts = 0.0;

/** Motor stator current in amps. */
public double rollerStatorCurrentAmps = 0.0;

/** Motor temperature in degrees Celsius. */
public double rollerTemperatureCelsius = 0.0;
```

### Your task

Implement all three fields and explain how each can help diagnose the roller.

### Explanation

These fields report the roller motor's operating conditions.

| Field | Unit | Purpose |
|---|---|---|
| `rollerAppliedVolts` | Volts | Indicates the voltage applied to the roller motor |
| `rollerStatorCurrentAmps` | Amps | Helps identify motor loading and current draw |
| `rollerTemperatureCelsius` | Degrees Celsius | Helps monitor motor heating |

These measurements are useful when investigating excessive load, insufficient motor output, unexpected current draw, or overheating.

The exact source and availability of these measurements depend on the IO implementation and its hardware.

## Step 5: Add Roller Velocity-Control State

The interface records whether roller velocity control is selected and the requested velocity:

```java
/** Whether closed-loop velocity control is active. */
public boolean rollerVelocityControlEnabled = false;

/** Requested flywheel velocity in rotations per second. */
public double rollerGoalVelocityRotationsPerSecond = 0.0;
```

### Your task

1. Add both fields.
2. Explain why the requested velocity is stored separately from the measured velocity.
3. Explain what the control-enabled flag tells the logging system.

### Explanation

`rollerVelocityControlEnabled` indicates whether the IO implementation reports that closed-loop velocity control is active.

`rollerGoalVelocityRotationsPerSecond` stores the most recently requested roller velocity.

The requested and measured values are not necessarily equal. For example, after requesting 50 rotations per second, the roller may still be accelerating toward that target.

Together, these fields make it possible to compare what the robot requested with what the motor actually reported.

The boolean is telemetry about the selected control mode. It is not proof that the controller accepted the request or that the roller reached its goal.

## Step 6: Add Pivot Position and Velocity Telemetry

The pivot is an angular mechanism. Add fields for its integrated position and velocity:

```java
/** Integrated arm position in radians. */
public double pivotPositionRadians = 0.0;

/** Integrated arm velocity in radians per second. */
public double pivotVelocityRadiansPerSecond = 0.0;
```

### Your task

1. Add both fields with their initial values.
2. Preserve the JavaDoc comments.
3. Explain the difference between pivot position and pivot velocity.
4. Explain why the pivot uses radians while the roller uses rotations.

### Explanation

`pivotPositionRadians` represents the integrated pivot position in radians.

`pivotVelocityRadiansPerSecond` represents how quickly the pivot angle changes, expressed in radians per second.

Radians are commonly used for mechanism angles because they work naturally with mathematical and control-system calculations.

One full rotation corresponds to \(2\pi\) radians:

\[
\theta_{\text{rad}} = \theta_{\text{rot}} \times 2\pi
\]

The interface presents mechanism-level units. Each IO implementation is responsible for converting the hardware's native units into the units documented by the input fields.

## Step 7: Add Pivot Motor Telemetry

Add the pivot motor's electrical and thermal measurements:

```java
/** Applied motor voltage in volts. */
public double pivotAppliedVolts = 0.0;

/** Motor stator current in amperes. */
public double pivotCurrentAmps = 0.0;

/** Motor temperature in degrees Celsius. */
public double pivotTemperatureCelsius = 0.0;
```

### Your task

Implement all three fields and explain how each can help diagnose the pivot.

### Explanation

| Field | Unit | Purpose |
|---|---|---|
| `pivotAppliedVolts` | Volts | Indicates the voltage applied to the pivot motor |
| `pivotCurrentAmps` | Amperes | Helps identify motor loading and current draw |
| `pivotTemperatureCelsius` | Degrees Celsius | Helps monitor motor heating |

These measurements can help identify excessive mechanical load, insufficient motor output, unexpected current draw, or overheating.

The exact source and availability of these measurements depend on the IO implementation and its hardware.

## Step 8: Add Absolute Encoder Telemetry

The pivot also has an absolute encoder. Store both its raw reading and its calibrated mechanism angle:

```java
/** Absolute encoder reading in rotations, before offset correction. */
public double pivotAbsoluteEncoderRotations = 0.0;

/** Absolute encoder angle in radians, after offset correction. */
public double pivotAbsoluteAngleRadians = 0.0;
```

### Your task

1. Add both fields.
2. Explain why the interface stores both the raw encoder reading and the calibrated angle.
3. Explain the difference between rotations and radians.

### Explanation

The raw encoder reading is useful for checking the sensor and diagnosing calibration issues.

The calibrated angle represents the encoder measurement after applying the offset and mechanism-specific conversion.

One full rotation corresponds to \(2\pi\) radians:

\[
\theta_{\text{rad}} = \theta_{\text{rot}} \times 2\pi
\]

The raw and calibrated values serve different purposes. Keeping both makes it easier to distinguish an encoder reading problem from an offset or conversion problem.

## Step 9: Add Pivot Position-Control State

Add the pivot's control-state and requested-position fields:

```java
/** Whether the arm is currently using position control. */
public boolean pivotPositionControlEnabled = false;

/** Active position goal in radians. */
public double pivotGoalRadians = 0.0;
```

### Your task

1. Add the boolean control-state field.
2. Add the requested position field.
3. Explain why measured position and requested position should be tracked separately.

### Explanation

The measured position describes where the mechanism is reported to be. The goal describes where the mechanism has been asked to move.

These values are not necessarily equal. For example, a pivot commanded to move to 45 degrees may still be traveling toward that target.

The control-state field indicates whether the IO implementation reports position control as enabled.

Together, these fields help the team understand what the mechanism was asked to do and what it actually reported.

Note that the field is telemetry, not a complete safety mechanism. An implementation should update it consistently with its control commands.

## Step 10: Declare `updateInputs()`

Add the following method to the interface:

```java
/**
 * Updates the supplied inputs with the latest measurements.
 *
 * @param inputs input object to update
 */
default void updateInputs(IntakeIOInputs inputs) {}
```

### Your task

1. Declare the method with the correct parameter type.
2. Preserve the JavaDoc.
3. Explain why the method receives an existing input object instead of returning a new one.

### Explanation

`updateInputs()` gives each implementation an opportunity to refresh the input structure with its latest available data.

For real hardware, the method reads the roller motor, pivot motor, and encoder telemetry.

For simulation, it can populate inputs from the simulated mechanism state.

For a test implementation, it can provide predetermined values.

The method accepts an existing object so implementations can update its fields repeatedly without allocating a new data structure on every update cycle.

The default implementation is empty. This allows an implementation to omit the method when it has no inputs to update, although a real mechanism implementation should populate all relevant telemetry.

## Step 11: Declare Roller Velocity Control

Add the following method:

```java
/**
 * Requests closed-loop velocity control.
 *
 * @param rotationsPerSecond target velocity in rotations per second
 */
default void setVelocity(double rotationsPerSecond) {}
```

### Your task

1. Declare the method.
2. Explain why the parameter is expressed in rotations per second.
3. Explain how a real implementation might carry out this request.

### Explanation

`setVelocity()` defines the interface for requesting a target roller speed.

The interface does not specify how that speed must be achieved. A real implementation may use a Phoenix 6 `VelocityVoltage` request, while a simulation may use a motor model to calculate the roller's response.

The parameter is expressed in rotations per second to match the roller velocity telemetry and make the expected unit explicit.

The default method is intentionally empty. An implementation that supports roller velocity control must override it to issue a motor command.

## Step 12: Declare Pivot Position Control

Add this method:

```java
/**
 * Commands closed-loop position control.
 *
 * @param positionRadians desired arm angle in radians
 */
default void setPosition(double positionRadians) {}
```

### Your task

Declare the method and explain why the parameter is expressed in radians.

### Explanation

`setPosition()` defines the interface for requesting a desired pivot angle.

The interface does not specify how that position must be achieved. A real implementation might use the TalonFX's position controller, while a simulation might use a physics model to move the arm toward the target.

Using radians as the interface unit provides a consistent convention across implementations.

The default method is intentionally empty. An implementation must override it to issue a motor command.

## Step 13: Declare Voltage Control for Both Motors

Add this method:

```java
/**
 * Requests open-loop voltage control.
 *
 * @param pivotVolts requested motor voltage for the pivot
 * @param rollerVolts requested motor voltage for the roller
 */
default void setVoltage(double pivotVolts, double rollerVolts) {}
```

### Your task

1. Declare the method with both voltage parameters.
2. Explain why the pivot and roller have separate voltage arguments.
3. Explain the difference between closed-loop control and direct voltage control.

### Explanation

The intake has two independently controlled motors, so the interface provides a separate voltage request for each one.

- `pivotVolts` is the requested voltage for the pivot motor.
- `rollerVolts` is the requested voltage for the roller motor.

Position and velocity control request a target and rely on feedback to regulate the mechanism toward that target.

Voltage control requests a motor voltage directly. It does not inherently attempt to reach or maintain a particular position or velocity.

The interface uses volts rather than duty cycle so the requested physical quantity is explicit.

A real implementation may use a Phoenix 6 `VoltageOut` request for each motor. Because the default method does nothing, implementations that support voltage control must override it.

## Step 14: Declare the Stop Operation

Add the following method:

```java
/** Stops applying motor voltage. */
default void stop() {}
```

### Your task

Declare the method and explain why a common stop operation is useful across hardware implementations.

### Explanation

The interface provides a standard way for the higher-level mechanism to request that the intake stop.

The real implementation can stop both TalonFX motors and update its control-state telemetry. A simulation can stop applying motor output, and a test implementation can record that the stop operation was requested.

The interface itself does not enforce any particular physical stopping behavior. The implementation and mechanism design determine whether the roller coasts or brakes and whether the pivot needs additional support to resist gravity.

## Step 15: Understand Why the Methods Are Default Methods

Every operation in this interface uses the `default` keyword and an empty body.

### Your task

Explain the difference between a default method and an abstract interface method.

### Explanation

An abstract interface method requires an implementing class to provide an implementation unless the class inherits one from another source.

A default interface method supplies an implementation directly in the interface.

Here, the empty defaults allow hardware implementations to override the operations they support. For example, a real implementation can override `setVelocity()`, `setPosition()`, `setVoltage()`, and `stop()`.

This is convenient for a shared IO contract, but it has an important trade-off: calling an unsupported operation may silently do nothing.

When an operation is essential for every implementation, an abstract declaration can provide stronger compile-time enforcement.

**Implementation note:** The supplied `IntakeIO` interface does not declare an `updateSim()` method. Simulation-specific stepping must therefore be handled by the simulation implementation or another simulation-facing API unless the interface is intentionally extended.

## Step 16: Verify the Interface

Before completing the lesson, check the following:

- [ ] `IntakeIO` is declared as a Java interface.
- [ ] `IntakeIOInputs` is nested inside the interface.
- [ ] The input class is annotated with `@AutoLog`.
- [ ] All roller telemetry fields are present.
- [ ] All pivot telemetry fields are present.
- [ ] Fields have appropriate initial values and JavaDoc.
- [ ] Roller position uses rotations.
- [ ] Roller velocity and its goal use rotations per second.
- [ ] Pivot position and its goal use radians.
- [ ] Pivot velocity uses radians per second.
- [ ] Absolute encoder telemetry includes raw rotations and calibrated radians.
- [ ] Voltage, current, and temperature use the documented units.
- [ ] Both control-state flags are present.
- [ ] All five IO operations are declared: `updateInputs()`, `setVelocity()`, `setPosition()`, `setVoltage()`, and `stop()`.
- [ ] `setVoltage()` accepts independent pivot and roller voltage requests.
- [ ] Real-hardware and simulation implementations use the same input structure.
- [ ] AdvantageKit annotation processing is configured so the generated input class is available.

## Challenge Questions

1. Why does the robot use an IO interface instead of accessing the TalonFX directly from the intake mechanism?
2. What does `@AutoLog` do, and why should you not manually create the generated class?
3. Why are roller position and velocity expressed in rotations and rotations per second?
4. Why are pivot position and velocity expressed in radians and radians per second?
5. Why should requested roller velocity and measured roller velocity be separate fields?
6. Why should requested pivot position and measured pivot position be separate fields?
7. Why is the raw absolute encoder reading useful when debugging calibration?
8. What is the difference between `VelocityVoltage`, `PositionVoltage`, and `VoltageOut`?
9. Why does `setVoltage()` accept two separate voltage arguments?
10. Why might an empty default method be convenient for some implementations but risky for a required hardware operation?
11. How would you implement a fake `IntakeIO` for a unit test?
12. Why should all IO implementations use the same units?
13. What information could you infer from motor current, voltage, and temperature together?
14. What additional telemetry would you add to help diagnose a mechanism that fails to reach its velocity or position goal?

## Expected Outcome

After completing this lesson, you should have an `IntakeIO` interface that defines a consistent contract for the intake's roller and pivot across real hardware, simulation, and testing.

The interface provides a shared telemetry structure for AdvantageKit and separates the higher-level mechanism's requested behavior from the details of how each implementation carries it out.

The key design principle is that **the interface defines what the intake can do and what it reports, while each implementation defines how those operations work**.
