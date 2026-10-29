# Lesson: Designing the Intake IO Interface

## Objective

In this lesson, you will implement `IntakeIO`, the hardware abstraction interface for a single-jointed intake arm.

This interface defines how the rest of the robot code interacts with the intake mechanism without depending on a specific motor controller, encoder, or simulation implementation.

It also defines the `IntakeIOInputs` data structure, which contains the mechanism's sensor readings, motor telemetry, and control state. AdvantageKit uses the `@AutoLog` annotation to generate a logging-friendly version of this structure.

By the end of this lesson, you should understand how to:

- Design a Java interface for hardware abstraction.
- Define a shared telemetry data structure.
- Use AdvantageKit's `@AutoLog` annotation.
- Document units and physical quantities.
- Declare default methods for optional hardware operations.
- Separate hardware behavior from mechanism logic.
- Support real hardware, simulation, and testing through one interface.

## Prerequisites

Before starting, you should be familiar with:

- Java interfaces, classes, fields, and methods.
- JavaDoc comments.
- The difference between a declaration and an implementation.
- The basics of AdvantageKit logging.
- The `IntakeIOReal` implementation.
- The concepts of position control, voltage control, and mechanism simulation.

## Step 1: Create the Interface

Create the file:

`src/main/java/first/robot/mechanisms/IntakeIO.java`

Start with the following skeleton:

```java
package first.robot.mechanisms;

/**
 * Hardware abstraction for a single-jointed intake arm.
 */
public interface IntakeIO {

}
```

### Your task

1. Declare an interface named `IntakeIO`.
2. Add JavaDoc describing its purpose.
3. Explain why this is an interface rather than a concrete class.

### Explanation

The interface defines the operations that an intake mechanism's IO implementation must make available.

Different classes can implement the same interface:

- `IntakeIOReal` communicates with physical hardware.
- A simulation implementation models the intake mechanism.
- A test implementation supplies controlled sensor values without requiring real hardware.

The higher-level intake mechanism can depend on `IntakeIO` rather than knowing which implementation is being used.

This is an example of **dependency abstraction**: the mechanism depends on a contract, not on a specific hardware implementation.

## Step 2: Define the Input Data Structure

Inside `IntakeIO`, declare a nested class named `IntakeIOInputs`.

```java
class IntakeIOInputs {

}
```

### Your task

Create a nested class to hold the intake's sensor readings, motor telemetry, and control state.

Then add the following annotation and JavaDoc:

```java
import org.littletonrobotics.junction.AutoLog;

/** Sensor values and mechanism telemetry updated by the IO implementation. */
@AutoLog
class IntakeIOInputs {

}
```

### Explanation

The input class provides a consistent structure for communicating measured and requested values from the IO layer to the rest of the robot program.

The `@AutoLog` annotation allows AdvantageKit's annotation processor to generate an associated logging-friendly class, commonly named `IntakeIOInputsAutoLogged`.

The generated class can be used by the mechanism to process inputs through AdvantageKit's logging system.

For example, the higher-level mechanism may use:

```java
IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();
```

The generated class is not something you should manually implement. It is produced during the build when the relevant annotation-processing configuration is enabled.

## Step 3: Add Position and Velocity Telemetry

Add these fields to `IntakeIOInputs`:

```java
/** Integrated arm position in radians. */
public double pivotPositionRadians = 0.0;

/** Integrated arm velocity in radians per second. */
public double pivotVelocityRadiansPerSecond = 0.0;
```

### Your task

1. Add both fields with their initial values.
2. Preserve the JavaDoc comments.
3. Explain the difference between position and velocity.

### Explanation

`pivotPositionRadians` represents the integrated arm position in radians.

`pivotVelocityRadiansPerSecond` represents how quickly that position changes, expressed in radians per second.

For angular position, radians are a useful standard because they are compatible with many mathematical and control-system calculations.

Angular velocity can be expressed as:

\[
\omega = \frac{d\theta}{dt}
\]

where \(\theta\) is angular position in radians and \(t\) is time in seconds.

Using clearly named fields makes the expected units explicit and reduces the risk of confusing rotations, degrees, and radians.

## Step 4: Add Motor Telemetry

Add the following fields:

```java
/** Applied motor voltage in volts. */
public double pivotAppliedVolts = 0.0;

/** Motor stator current in amperes. */
public double pivotCurrentAmps = 0.0;

/** Motor temperature in degrees Celsius. */
public double pivotTemperatureCelsius = 0.0;
```

### Your task

Implement all three fields and explain how each can help diagnose a mechanism.

### Explanation

These fields report the motor's operating conditions.

| Field | Unit | Purpose |
|---|---|---|
| `pivotAppliedVolts` | Volts | Indicates the voltage applied to the motor |
| `pivotCurrentAmps` | Amperes | Helps identify motor loading and current draw |
| `pivotTemperatureCelsius` | Degrees Celsius | Helps monitor motor heating |

These measurements are useful when investigating issues such as excessive load, insufficient motor output, unexpected current draw, or overheating.

The exact source and availability of these measurements depend on the IO implementation and its hardware.

## Step 5: Add Absolute Encoder Telemetry

Add the following fields:

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

## Step 6: Add Position-Control State

Add the following fields:

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

These values are not necessarily equal. For example, an arm commanded to move to 45 degrees may still be traveling toward that target.

The control-state field indicates whether the IO implementation reports position control as enabled.

Together, these fields help the team understand what the mechanism was asked to do and what it actually reported.

Note that the field is telemetry, not a complete safety mechanism. An implementation should update it consistently with its control commands.

## Step 7: Declare `updateInputs()`

Add the following method to the interface:

```java
/**
 * Refreshes all available input values.
 *
 * @param inputs object to populate
 */
default void updateInputs(IntakeIOInputs inputs) {}
```

### Your task

1. Declare the method with the correct parameter type.
2. Preserve the JavaDoc.
3. Explain why the method receives an existing input object instead of returning a new one.

### Explanation

`updateInputs()` gives each implementation an opportunity to refresh the input structure with its latest available data.

For real hardware, the method reads motor and encoder telemetry.

For simulation, it can populate inputs from the simulated mechanism state.

For a test implementation, it can provide predetermined values.

The method accepts an existing object so that implementations can update its fields repeatedly without needing to allocate a new data structure on every update cycle.

The default implementation is empty. This allows an implementation to omit the method when it has no inputs to update, although a real mechanism implementation should populate all relevant telemetry.

## Step 8: Declare Position Control

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

`setPosition()` defines the interface for requesting a desired arm angle.

The interface does not specify how that position must be achieved. A real implementation might use the TalonFX's position controller, while a simulation might use a physics model to move the arm toward the target.

Using radians as the interface unit provides a consistent convention across implementations.

The default method is intentionally empty. The real-hardware implementation must override it to issue a motor command.

## Step 9: Declare Voltage Control

Add this method:

```java
/**
 * Commands an open-loop motor voltage.
 *
 * @param volts requested voltage
 */
default void setVoltage(double volts) {}
```

### Your task

Declare the method and explain the difference between position control and voltage control.

### Explanation

Position control requests a target angle and relies on feedback to move toward that target.

Voltage control requests a motor voltage directly. It does not inherently attempt to reach or maintain a particular angle.

The interface uses volts rather than duty cycle so the requested physical quantity is explicit.

A real implementation may use a Phoenix 6 `VoltageOut` control request to carry out the operation.

Because the default method does nothing, implementations that support voltage control must override it.

## Step 10: Declare the Stop Operation

Add the following method:

```java
/** Stops the motor and disables position control. */
default void stop() {}
```

### Your task

Declare the method and explain why a common stop operation is useful across hardware implementations.

### Explanation

The interface provides a standard way for the higher-level mechanism to request that the motor stop.

The real implementation can stop the TalonFX and update its control-state telemetry. A simulation can stop applying motor output, and a test implementation can record that the stop operation was requested.

The interface itself does not enforce any particular physical stopping behavior. The implementation and mechanism design determine whether the arm coasts, brakes, or needs additional support to resist gravity.

## Step 11: Declare Simulation Support

Add the following method:

```java
/**
 * Advances the mechanism simulation, if supported.
 *
 * @param periodSeconds simulation timestep in seconds
 * @param batteryVoltage current simulated battery voltage
 */
default void updateSim(
    double periodSeconds,
    double batteryVoltage) {}
```

### Your task

1. Declare the method with both parameters.
2. Explain the purpose of the simulation timestep.
3. Explain why battery voltage is provided to the simulation.

### Explanation

The simulation timestep describes the amount of simulated time to advance during an update.

The battery voltage provides the simulation with the available supply voltage, which can affect the maximum voltage applied to the motor and therefore the mechanism's response.

A simulation implementation might use a motor and arm physics model to update position and velocity.

The real-hardware implementation usually does not need to advance a simulated mechanism, so it can inherit the empty default implementation.

This shared interface lets the higher-level code use the same mechanism API regardless of whether it is running against real hardware or simulation.

## Step 12: Understand Why the Methods Are Default Methods

Every method in this interface uses the `default` keyword and an empty body.

### Your task

Explain the difference between a default method and an abstract interface method.

### Explanation

An abstract interface method requires an implementing class to provide an implementation unless the class inherits one from another source.

A default interface method supplies an implementation directly in the interface.

Here, the empty defaults allow hardware implementations to override only the operations they support. For example, a real implementation can override hardware commands while leaving `updateSim()` empty.

This is convenient for a shared IO contract, but it has an important trade-off: calling an unsupported operation may silently do nothing.

When a method is essential for every implementation, an abstract declaration can provide stronger compile-time enforcement.

## Step 13: Verify the Interface

Before completing the lesson, check the following:

- [ ] `IntakeIO` is declared as a Java interface.
- [ ] `IntakeIOInputs` is nested inside the interface.
- [ ] The input class is annotated with `@AutoLog`.
- [ ] All nine telemetry fields are present.
- [ ] Fields have appropriate initial values and JavaDoc.
- [ ] Position values use radians.
- [ ] Angular velocity uses radians per second.
- [ ] Voltage, current, and temperature use the documented units.
- [ ] All five IO operations are declared: `updateInputs()`, `setPosition()`, `setVoltage()`, `stop()`, and `updateSim()`.
- [ ] The real-hardware and simulation implementations use the same input structure.
- [ ] AdvantageKit annotation processing is configured so the generated input class is available.

## Challenge Questions

1. Why does the robot use an IO interface instead of accessing the TalonFX directly from the intake mechanism?
2. What does `@AutoLog` do, and why should you not manually create the generated class?
3. Why should requested position and measured position be separate fields?
4. Why is the raw absolute encoder reading useful when debugging calibration?
5. What is the difference between position control and voltage control?
6. Why might an empty default method be convenient for simulation but risky for a required hardware operation?
7. How would you implement a fake `IntakeIO` for a unit test?
8. Why should all IO implementations use the same units?
9. What information could you infer from motor current, voltage, and temperature together?
10. What additional telemetry would you add to help diagnose a mechanism that fails to reach its position goal?

## Expected Outcome

After completing this lesson, you should have an `IntakeIO` interface that defines a consistent contract for real hardware, simulation, and testing.

The interface provides a shared telemetry structure for AdvantageKit and separates the higher-level mechanism's requested behavior from the details of how each implementation carries it out.

The key design principle is that **the interface defines what the intake can do and what it reports, while each implementation defines how those operations work**.
