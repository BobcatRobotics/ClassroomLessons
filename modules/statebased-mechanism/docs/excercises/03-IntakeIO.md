# Lesson Plan: Creating the Intake IO Interface
## WPILib 2027 Alpha 7 — AdvantageKit, Hardware Abstraction, and AutoLog

---

## 1. Lesson Overview

**Topic:** Designing a Hardware Abstraction Interface Using AdvantageKit

**Framework:** WPILib 2027 Alpha 7

**Programming Language:** Java

**Estimated Duration:** 60–90 minutes

**Target Audience:** FRC students with basic Java, WPILib, and AdvantageKit experience.

### Lesson Description

In this lesson, students will learn how to create an IO interface for the robot's intake mechanism using AdvantageKit.

The IO interface defines the contract between the mechanism's control logic and the underlying hardware implementation.

Rather than directly interacting with motor controllers and sensors, the Intake mechanism communicates through a common interface named `IntakeIO`.

Students will learn how to define measured inputs, annotate input classes with `@AutoLog`, and create methods for controlling the intake roller and pivot.

The lesson also introduces the importance of separating hardware-specific implementation details from mechanism behavior.

By the end of the lesson, students should understand how the same IO interface can support real hardware, simulation, and unit testing without changing the Intake mechanism itself.

### Central Programming Concept

The IO interface defines what a mechanism needs from its hardware, not how the hardware accomplishes it.

The real and simulated implementations must fulfill the same contract.

---

## 2. Learning Objectives

By the end of this lesson, students should be able to:

1. Explain the purpose of an IO interface.
2. Understand Java interfaces and default methods.
3. Explain the AdvantageKit IO architecture.
4. Understand the purpose of the `@AutoLog` annotation.
5. Define hardware input measurements.
6. Distinguish between measured values and requested values.
7. Understand the difference between open-loop and closed-loop control.
8. Define methods for controlling motors through an IO interface.
9. Explain why hardware-specific code should not exist in the mechanism class.
10. Understand how real and simulated IO implementations share the same interface.
11. Identify appropriate telemetry measurements for a mechanism.
12. Understand the importance of consistent measurement units.

---

## 3. Prerequisites

Students should understand:

- Java classes and interfaces.
- Java default methods.
- Java primitive data types.
- Java Javadocs.
- Basic motor controller operation.
- Encoders and sensor measurements.
- Basic WPILib programming.
- AdvantageKit logging concepts.
- The difference between position, velocity, voltage, and current.

### Required Existing Classes

This lesson is part of the Intake mechanism development sequence.

| Class | Responsibility |
|---|---|
| `Intake` | Implements the intake mechanism behavior. |
| `IntakeIO` | Defines the hardware abstraction interface. |
| `IntakeIOInputsAutoLogged` | Generated AdvantageKit input logging class. |
| `IntakeState` | Defines intake operating states. |
| `IntakeConstants` | Stores mechanical and hardware configuration. |
| `IntakeIOReal` | Implements real hardware communication. |
| `IntakeIOSim` | Implements simulated hardware behavior. |

---

# 4. Lesson Structure

| Section | Topic | Time |
|---|---|---:|
| 1 | Introduction to IO architecture | 10 min |
| 2 | Understanding Java interfaces | 10 min |
| 3 | Understanding the IntakeIO interface | 10 min |
| 4 | AdvantageKit AutoLog inputs | 15 min |
| 5 | Understanding roller measurements | 10 min |
| 6 | Understanding pivot measurements | 10 min |
| 7 | Defining motor control methods | 10 min |
| 8 | Real and simulated implementations | 10 min |
| 9 | Student exercises and review | 15 min |

---

# 5. Instructor Lesson

## Part 1: Introduction to Hardware Abstraction

Before examining the code, students need to understand why hardware abstraction is important.

Consider a robot with an intake mechanism containing:

- A roller motor.
- A pivot motor.
- A roller encoder.
- A pivot encoder.
- An absolute encoder.

A traditional implementation might directly create motor controllers and sensors inside the Intake class.

For example:

```java
private final TalonFX rollerMotor = new TalonFX(10);
private final TalonFX pivotMotor = new TalonFX(11);
private final CANcoder pivotEncoder = new CANcoder(12);
```

Although this can work, it creates a strong dependency between the mechanism and the specific hardware.

This becomes problematic when:

- Testing the mechanism without real hardware.
- Simulating the robot.
- Changing motor controllers.
- Replacing sensors.
- Writing unit tests.

### The AdvantageKit Solution

AdvantageKit encourages separating mechanism logic from hardware communication.

Instead of creating hardware objects directly, the Intake class receives an IO implementation.

```java
public class Intake implements Mechanism {
    private final IntakeIO io;

    public Intake(IntakeIO io) {
        this.io = io;
    }
}
```

The IO interface defines the methods that hardware implementations must provide.

For example:

```java
io.setVelocity(25.0);
io.setPosition(1.2);
io.stop();
```

The Intake mechanism does not need to know which motor controller receives these requests.

### Discuss with Students

Ask:

1. Why would we want to simulate an intake without connecting real motors?
2. Why should mechanism logic not depend directly on Phoenix 6?
3. What happens if the team changes its motor controller?
4. How does an interface make unit testing easier?

**Teaching Point:** Hardware abstraction allows the same mechanism implementation to work with multiple hardware implementations.

---

# 6. Understanding Java Interfaces

Examine the declaration:

```java
public interface IntakeIO {
```

An interface defines a contract that other classes can implement.

In this example, `IntakeIO` defines the operations required by the Intake mechanism.

An implementation might look like:

```java
public class IntakeIOReal implements IntakeIO {
}
```

Or:

```java
public class IntakeIOSim implements IntakeIO {
}
```

Both classes implement the same interface.

### Why Use an Interface?

The Intake mechanism can call:

```java
io.setPosition(1.2);
```

Without knowing whether the implementation uses:

- A TalonFX.
- A Spark MAX.
- A simulated motor.
- A test implementation.

### Understanding Default Methods

Notice that the methods in this interface use the `default` keyword.

For example:

```java
default void stop() {}
```

A default method provides an implementation directly inside the interface.

This means implementing classes do not have to override every method.

This can be useful when a hardware implementation does not need to support a particular operation.

However, students should understand that an empty default implementation does not actually perform the requested action.

### Instructor Discussion

Ask:

1. What is the difference between an abstract interface method and a default method?
2. Why might an IO interface use default methods?
3. What happens if an implementation forgets to override `setVoltage()`?

**Teaching Point:** Default methods make implementations flexible, but they can also hide missing functionality. Important hardware operations should be implemented and tested explicitly.

---

# 7. Understanding the IntakeIOInputs Class

Examine:

```java
@AutoLog
class IntakeIOInputs {
```

This nested class defines all the measurements and status information that the Intake mechanism receives from its hardware.

The `@AutoLog` annotation tells AdvantageKit to generate an automatically logged version of the input class.

The generated class is:

```java
IntakeIOInputsAutoLogged
```

This is the class used by the Intake mechanism.

### Why Group Inputs Together?

Instead of creating separate methods for every sensor measurement, the IO implementation updates one input object.

For example:

```java
io.updateInputs(inputs);
```

The IO implementation populates all the relevant measurements.

The mechanism can then access:

```java
inputs.rollerVelocityRotationsPerSecond
inputs.pivotPositionRadians
inputs.pivotAppliedVolts
```

### Advantages

- Groups related measurements.
- Simplifies periodic input updates.
- Provides consistent logging.
- Supports simulation.
- Supports replay and testing.
- Makes mechanism code independent of specific hardware.

### Important Concept: Measured vs. Requested Values

The input class contains both measured values and requested goals.

Students must understand the distinction.

| Type | Example | Meaning |
|---|---|---|
| Measurement | `rollerVelocityRotationsPerSecond` | Actual measured roller speed |
| Goal | `rollerGoalVelocityRotationsPerSecond` | Requested roller speed |
| Measurement | `pivotPositionRadians` | Actual measured pivot angle |
| Goal | `pivotGoalRadians` | Requested pivot angle |
| Status | `pivotPositionControlEnabled` | Whether position control is active |

The mechanism should never assume that a requested goal has actually been achieved.

---

# 8. Roller Input Measurements

The first group of fields describes the intake roller.

## Part 8A: Roller Position

```java
/** Flywheel position in rotations. */
public double rollerPositionRotations = 0.0;
```

This field represents the measured roller position.

The value is expressed in rotations.

For example:

| Value | Meaning |
|---:|---|
| 0.0 | Zero rotations |
| 0.25 | Quarter rotation |
| 0.5 | Half rotation |
| 1.0 | One full rotation |
| 2.0 | Two full rotations |

### Why Track Position?

Roller position can be useful for:

- Tracking roller movement.
- Visualizing rotation.
- Diagnosing encoder behavior.
- Measuring game-piece movement indirectly.

It is important to distinguish between motor rotations and mechanism rotations when gearing is involved.

---

## Part 8B: Roller Velocity

```java
/** Flywheel velocity in rotations per second. */
public double rollerVelocityRotationsPerSecond = 0.0;
```

This represents the measured roller velocity.

The unit is rotations per second.

For example:

- 10 RPS = 600 RPM.
- 25 RPS = 1500 RPM.
- 50 RPS = 3000 RPM.

Conversion:

\[
RPM = RPS \times 60
\]

### Why Use Rotations Per Second?

RPS provides a convenient measurement for many WPILib motor simulations and control calculations.

The mechanism can convert the value into a unit-safe AngularVelocity.

---

## Part 8C: Applied Voltage

```java
/** Applied motor voltage in volts. */
public double rollerAppliedVolts = 0.0;
```

This field represents the voltage currently applied to the roller motor.

Voltage is useful for understanding motor behavior and diagnosing electrical problems.

For example, if the roller is not reaching its target velocity, programmers can inspect:

- Requested velocity.
- Measured velocity.
- Applied voltage.
- Motor current.

---

## Part 8D: Stator Current

```java
/** Motor stator current in amps. */
public double rollerStatorCurrentAmps = 0.0;
```

This measures the motor's stator current.

Current provides insight into motor loading.

High current may indicate:

- Increased mechanical resistance.
- A jammed intake.
- Excessive loading.
- A mechanical obstruction.

Students should understand that current alone does not identify the cause of a problem.

---

## Part 8E: Temperature

```java
/** Motor temperature in degrees Celsius. */
public double rollerTemperatureCelsius = 0.0;
```

This records the roller motor temperature.

Temperature telemetry is useful for identifying overheating.

Students should understand why temperature monitoring matters during long matches and repeated mechanism operation.

---

## Part 8F: Velocity Control Status

```java
/** Whether closed-loop velocity control is active. */
public boolean rollerVelocityControlEnabled = false;
```

This indicates whether the roller is currently operating in closed-loop velocity control.

A boolean field represents a true/false condition.

For example:

| Value | Meaning |
|---|---|
| `true` | Velocity control is active |
| `false` | Velocity control is inactive |

This can help distinguish between closed-loop and open-loop operation.

---

## Part 8G: Roller Velocity Goal

```java
/** Requested flywheel velocity in rotations per second. */
public double rollerGoalVelocityRotationsPerSecond = 0.0;
```

This records the requested roller velocity.

It should be compared with:

```java
rollerVelocityRotationsPerSecond
```

The difference between the two values represents velocity error.

\[
e = \omega_{\text{goal}} - \omega_{\text{measured}}
\]

For example:

- Goal = 30 RPS.
- Measured = 27 RPS.
- Error = 3 RPS.

### Student Checkpoint

Ask:

1. What is the difference between roller position and velocity?
2. Why do we monitor motor current?
3. Why should requested velocity and measured velocity be separate?
4. How could motor temperature telemetry help during competition?

---

# 9. Pivot Input Measurements

The second group of fields describes the intake pivot.

## Part 9A: Pivot Position

```java
/** Integrated arm position in radians. */
public double pivotPositionRadians = 0.0;
```

This represents the measured pivot angle.

The value is expressed in radians.

Examples:

| Radians | Degrees |
|---:|---:|
| 0 | 0° |
| 0.7854 | 45° |
| 1.5708 | 90° |
| 3.1416 | 180° |

Conversion:

\[
Degrees = Radians \times \frac{180}{\pi}
\]

### Why Use Radians?

Radians are commonly used in mathematical and control calculations.

WPILib units allow students to represent angles without relying on untyped numeric values throughout their mechanism code.

---

## Part 9B: Pivot Velocity

```java
/** Integrated arm velocity in radians per second. */
public double pivotVelocityRadiansPerSecond = 0.0;
```

This represents the angular velocity of the intake arm.

It helps determine how quickly the pivot is moving.

This measurement can be useful when:

- Tuning position control.
- Identifying mechanical oscillation.
- Detecting unexpected movement.
- Evaluating acceleration and deceleration.

---

## Part 9C: Pivot Applied Voltage

```java
/** Applied motor voltage in volts. */
public double pivotAppliedVolts = 0.0;
```

This represents the voltage applied to the pivot motor.

It is useful for analyzing motor behavior during movement and position holding.

---

## Part 9D: Pivot Current

```java
/** Motor stator current in amperes. */
public double pivotCurrentAmps = 0.0;
```

This records the pivot motor current.

The pivot may experience high current when:

- Lifting the intake.
- Holding a heavy intake assembly.
- Accelerating the arm.
- Encountering mechanical resistance.

---

## Part 9E: Pivot Temperature

```java
/** Motor temperature in degrees Celsius. */
public double pivotTemperatureCelsius = 0.0;
```

This records the pivot motor temperature.

Students should understand that pivot motors can experience significant loading when holding an arm against gravity.

Temperature monitoring is particularly useful for mechanisms that remain energized for long periods.

---

# 10. Understanding Absolute Encoders

The interface includes two absolute encoder measurements.

## Part 10A: Absolute Encoder Rotations

```java
/** Absolute encoder reading in rotations, before offset correction. */
public double pivotAbsoluteEncoderRotations = 0.0;
```

This stores the raw absolute encoder position.

An absolute encoder reports its position within a defined range, even after the robot has been powered off.

For example, an encoder might report:

- 0.0 rotations.
- 0.25 rotations.
- 0.5 rotations.
- 0.75 rotations.

The raw reading may not correspond directly to the physical intake angle.

This is because the encoder may have been mounted with a mechanical offset.

---

## Part 10B: Corrected Absolute Angle

```java
/** Absolute encoder angle in radians, after offset correction. */
public double pivotAbsoluteAngleRadians = 0.0;
```

This represents the absolute encoder angle after applying the configured offset.

The corrected angle should represent the actual mechanism reference frame.

### Why Do We Need an Offset?

Consider an intake arm that points horizontally when its angle is zero.

The encoder might report 0.35 rotations when the arm is physically horizontal.

The software must compensate for that offset.

A simplified correction is:

\[
\theta_{\text{corrected}} =
\theta_{\text{raw}}-\theta_{\text{offset}}
\]

The implementation must also handle encoder wraparound and the desired angular range.

### Integrated vs. Absolute Encoders

| Integrated Encoder | Absolute Encoder |
|---|---|
| Tracks relative movement | Reports position within its absolute range |
| May require initialization or synchronization | Retains its position reference across power cycles |
| Useful for continuous movement tracking | Useful for establishing mechanism position |
| Commonly associated with motor controllers | Often implemented as a separate sensor |

### Student Discussion

Ask:

1. Why might an absolute encoder be useful for an intake pivot?
2. What happens if the encoder offset is incorrect?
3. Why should raw and corrected encoder values both be logged?
4. How could we synchronize an integrated sensor with an absolute encoder?

---

# 11. Pivot Control Status and Goal

## Part 11A: Position Control Enabled

```java
/** Whether the arm is currently using position control. */
public boolean pivotPositionControlEnabled = false;
```

This indicates whether the pivot is currently operating in closed-loop position control.

This is useful for diagnosing the mechanism's operating mode.

For example:

- `true`: Position control is active.
- `false`: Position control is inactive.

---

## Part 11B: Pivot Position Goal

```java
/** Active position goal in radians. */
public double pivotGoalRadians = 0.0;
```

This represents the requested pivot angle.

It should be compared against:

```java
pivotPositionRadians
```

The position error is:

\[
e_{\theta} =
\theta_{\text{goal}}-\theta_{\text{measured}}
\]

For example:

- Goal = 1.57 radians.
- Measured = 1.40 radians.
- Error = 0.17 radians.

### Why Is This Useful?

Logging the goal and measured position together allows programmers to investigate:

- Position-control accuracy.
- Mechanical resistance.
- Controller tuning.
- Encoder calibration.
- Whether the mechanism reaches its target.

---

# 12. Updating Hardware Inputs

Examine the first IO method:

```java
default void updateInputs(IntakeIOInputs inputs) {}
```

### Purpose

This method allows the hardware implementation to update all measured values.

For example, a real implementation might retrieve:

- Roller encoder position.
- Roller velocity.
- Roller applied voltage.
- Roller current.
- Pivot position.
- Pivot velocity.
- Absolute encoder position.

The simulated implementation would populate the same fields using simulation data.

### Example Usage in the Mechanism

The Intake class calls:

```java
io.updateInputs(inputs);
```

Then processes the values:

```java
Logger.processInputs("Intake", inputs);
```

This creates a consistent input-update workflow.

### Why Use One Input Method?

Instead of having separate methods such as:

```java
getRollerVelocity();
getRollerCurrent();
getPivotPosition();
getPivotVoltage();
```

The IO interface groups all measurements into one object.

This reduces the number of individual hardware calls and makes telemetry easier to organize.

### Student Exercise

Ask students to identify which fields should be updated by `updateInputs()`.

Expected answer: All measured sensor values and hardware status values, including measured position, velocity, voltage, current, temperature, and control status.

Goal fields should be populated according to the chosen logging design and kept consistent with the active hardware requests.

---

# 13. Closed-Loop Roller Velocity Control

Examine:

```java
default void setVelocity(double rotationsPerSecond) {}
```

### Purpose

This method requests closed-loop velocity control of the intake roller.

The parameter is expressed in rotations per second.

For example:

```java
io.setVelocity(25.0);
```

Requests a roller velocity of 25 rotations per second.

### Understanding Closed-Loop Control

Closed-loop control uses feedback from a sensor to regulate motor output.

The motor controller compares:

- Requested velocity.
- Measured velocity.

It then adjusts motor output to reduce the difference.

Conceptually:

```text
Requested Velocity
        |
        v
   Motor Controller
        |
        v
      Motor
        |
        v
      Encoder
        |
        v
  Measured Velocity
        |
        +----------> Feedback
```

### Why Use Velocity Control?

Velocity control is useful when the roller needs a predictable operating speed.

For example:

- Maintaining intake speed while acquiring a game piece.
- Maintaining consistent roller performance under changing loads.
- Controlling the speed of a conveyor or flywheel.

### Student Checkpoint

Ask:

1. What is the difference between open-loop and closed-loop control?
2. Why does velocity control need feedback?
3. What happens if the roller encounters additional mechanical resistance?

---

# 14. Closed-Loop Pivot Position Control

Examine:

```java
default void setPosition(double positionRadians) {}
```

### Purpose

This method requests closed-loop position control of the intake pivot.

The parameter is expressed in radians.

For example:

```java
io.setPosition(1.5708);
```

Requests approximately 90 degrees.

### Understanding Position Control

The motor controller attempts to move the pivot to the requested angle.

It uses encoder feedback to determine the current position.

The difference between requested and measured position is the position error.

\[
e = \theta_{\text{goal}}-\theta_{\text{measured}}
\]

The controller adjusts motor output to reduce this error.

### Why Use Position Control?

Position control is useful when the intake needs to move between known positions.

For example:

- Deployed position.
- Retracted position.
- Ground intake position.
- Transport position.

### Important Design Consideration

The IO interface does not define how position control is implemented.

A Phoenix 6 implementation might use a position request, while a simulated implementation might use a WPILib physics model and feedback controller.

The Intake mechanism does not need to know those details.

---

# 15. Open-Loop Voltage Control

Examine:

```java
default void setVoltage(
    double pivotVolts,
    double rollerVolts) {}
```

### Purpose

This method requests open-loop voltage control for both intake motors.

It accepts two independent values:

- `pivotVolts`: Voltage requested for the pivot motor.
- `rollerVolts`: Voltage requested for the roller motor.

For example:

```java
io.setVoltage(3.0, 6.0);
```

Requests:

- 3 volts for the pivot.
- 6 volts for the roller.

### Understanding Voltage Control

Unlike closed-loop control, open-loop voltage control does not directly request a particular position or velocity.

Instead, it requests a motor voltage.

The resulting motor behavior depends on:

- Battery voltage.
- Motor characteristics.
- Mechanical loading.
- Friction.
- Gear reduction.

### Why Use Voltage Instead of Duty Cycle?

Voltage control allows the programmer to request an output in volts.

Duty cycle represents a fraction of available output.

The relationship is approximately:

\[
V_{\text{requested}} =
D \times V_{\text{battery}}
\]

For example:

- Battery voltage = 12 V.
- Duty cycle = 0.5.
- Requested voltage = 6 V.

Using voltage requests can make control behavior easier to reason about when battery voltage changes.

### Student Exercise

Calculate the requested voltage for:

| Battery Voltage | Duty Cycle | Requested Voltage |
|---:|---:|---:|
| 12 V | 0.5 | 6 V |
| 12 V | 0.25 | 3 V |
| 10 V | 0.5 | 5 V |
| 9 V | -0.4 | -3.6 V |

---

# 16. Stopping the Mechanism

Examine:

```java
default void stop() {}
```

### Purpose

This method requests that the hardware implementation stop the intake motors.

The exact behavior depends on the implementation.

For example, the real implementation might:

- Set motor voltage to zero.
- Disable active control requests.
- Apply brake mode.
- Apply coast mode.

The simulated implementation should produce corresponding simulated behavior.

### Important Distinction

Stopping a motor and holding a mechanism are not necessarily the same thing.

For example:

- Stopping a roller generally means removing its motor output.
- Stopping a pivot may allow it to fall if gravity is acting on the arm.
- Holding a pivot may require active position control.

Students should understand why the IO implementation must define stop behavior carefully.

---

# 17. Understanding the Complete IO Contract

The IntakeIO interface provides the following operations:

| Method | Purpose | Control Type |
|---|---|---|
| `updateInputs()` | Retrieve measurements | Sensor feedback |
| `setVelocity()` | Control roller velocity | Closed-loop |
| `setPosition()` | Control pivot position | Closed-loop |
| `setVoltage()` | Control both motor voltages | Open-loop |
| `stop()` | Stop motor outputs | Stop |

The interface deliberately avoids hardware-specific classes.

For example, there are no:

- `TalonFX` objects.
- `CANcoder` objects.
- Phoenix 6 control requests.
- Simulation motor models.

Those details belong in the implementation classes.

---

# 18. Real and Simulated Implementations

The next step is implementing the interface.

Both implementations use the same contract.

## Real Hardware

```java
public class IntakeIOReal implements IntakeIO {
    // Phoenix 6 motor controllers
    // CANcoder configuration
    // Sensor updates
    // Motor control requests
}
```

The real implementation communicates with the robot's actual hardware.

It should:

- Read sensor measurements.
- Update logged inputs.
- Send velocity requests.
- Send position requests.
- Apply voltage requests.
- Stop motor outputs.

## Simulation

```java
public class IntakeIOSim implements IntakeIO {
    // Simulated motors
    // Simulated encoders
    // Physics models
    // Sensor updates
}
```

The simulated implementation models the behavior of the physical intake.

It should update the same input fields as the real implementation.

### Comparison

| Responsibility | Real IO | Simulation IO |
|---|---|---|
| Read roller position | Hardware encoder | Simulated encoder |
| Read roller velocity | Hardware encoder | Simulated encoder |
| Read pivot position | Integrated sensor | Simulated sensor |
| Read absolute position | CANcoder | Simulated absolute encoder |
| Set roller velocity | Motor controller | Simulation controller |
| Set pivot position | Motor controller | Simulation controller |
| Set motor voltage | Hardware output | Simulation input |
| Stop motors | Hardware stop | Simulation stop |

### Key Teaching Point

The Intake mechanism should behave the same way regardless of whether it receives real or simulated IO.

---

# 19. Testing and Troubleshooting

## Stage 1: Code Review

Before implementing real or simulated IO, verify:

- [ ] The interface is named `IntakeIO`.
- [ ] The input class is annotated with `@AutoLog`.
- [ ] All roller measurements are defined.
- [ ] All pivot measurements are defined.
- [ ] Goal values are represented separately from measured values.
- [ ] Control status fields are defined.
- [ ] The interface methods have accurate Javadocs.
- [ ] All measurements use consistent units.
- [ ] No hardware-specific classes appear in the interface.

## Stage 2: Real IO Testing

Verify that the real implementation:

- Reads the correct motor positions.
- Reads the correct motor velocities.
- Reports applied voltage.
- Reports current and temperature.
- Reads the absolute encoder.
- Applies the correct encoder offset.
- Sends the requested velocity and position.
- Responds correctly to voltage requests.
- Stops both motors safely.

## Stage 3: Simulation Testing

Verify that the simulated implementation:

- Updates all roller inputs.
- Updates all pivot inputs.
- Produces realistic sensor values.
- Responds to velocity requests.
- Responds to position requests.
- Responds to voltage requests.
- Stops correctly.
- Reports control status accurately.

## Stage 4: AdvantageScope

Inspect the logged inputs.

**Roller:**
- Position.
- Velocity.
- Applied voltage.
- Current.
- Temperature.
- Velocity control status.
- Goal velocity.

**Pivot:**
- Integrated position.
- Integrated velocity.
- Applied voltage.
- Current.
- Temperature.
- Absolute encoder position.
- Corrected absolute angle.
- Position control status.
- Goal angle.

Students should verify that requested and measured values behave as expected.

---

# 20. Student Programming Challenges

## Challenge 1: Add a Roller Acceleration Measurement

Add a new input field:

`rollerAccelerationRotationsPerSecondSquared`

Requirements:

- Document the measurement.
- Populate it in the real IO implementation.
- Populate it in simulation.
- Log it through AdvantageKit.

Students should explain how acceleration can help diagnose mechanism behavior.

## Challenge 2: Add Pivot Absolute Encoder Connectivity

Add a boolean field:

`pivotAbsoluteEncoderConnected`

Requirements:

- Update the field from the real encoder status.
- Update the field appropriately in simulation.
- Use the field to identify encoder disconnection.

Students should explain why sensor connectivity is important for safe position control.

## Challenge 3: Add Roller Motor Connectivity

Add:

`rollerMotorConnected`

Requirements:

- Record motor connectivity.
- Update the field in real IO.
- Simulate a connected motor.
- Log the result.

## Challenge 4: Add Individual Stop Methods

Extend the interface with:

- `stopRoller()`
- `stopPivot()`

Students should explain when independent motor stopping is useful.

## Challenge 5: Add Pivot Voltage Limits

Design a mechanism safety strategy that limits pivot voltage near its mechanical boundaries.

Students should explain why safety restrictions should be enforced in the mechanism and, where appropriate, in the hardware implementation.

## Challenge 6: Add a Roller Duty-Cycle Method

Extend the interface with:

```java
default void setRollerDutyCycle(double dutyCycle) {}
```

Requirements:

- Document the valid range.
- Implement the method in real IO.
- Implement the method in simulation.
- Clamp or validate the requested value.
- Test positive, negative, and zero values.

Students should explain the difference between duty-cycle and voltage control.

## Challenge 7: Add a Pivot Position Error Measurement

Add a logged field:

`pivotPositionErrorRadians`

Students should calculate:

\[
e = \theta_{\text{goal}}-\theta_{\text{measured}}
\]

They should explain how this value could help evaluate position-control performance.

---

# 21. Assessment

## Knowledge Assessment

Students should be able to answer:

1. What is an IO interface?
2. Why does IntakeIO use a Java interface?
3. What is the purpose of `@AutoLog`?
4. What class does AdvantageKit generate?
5. What is the difference between measured values and goal values?
6. Why are the inputs grouped into one class?
7. What is the difference between RPS and RPM?
8. Why are radians used for pivot measurements?
9. What is an absolute encoder?
10. Why do we need encoder offsets?
11. What is the difference between velocity and position control?
12. What is the difference between voltage and duty-cycle control?
13. Why should hardware-specific code remain outside the mechanism?
14. How can one IO interface support both simulation and real hardware?
15. Why is it important to report sensor connectivity?

## Practical Assessment

| Requirement | Points |
|---|---:|
| Correctly defines the IO interface | 10 |
| Correctly defines the AutoLog input class | 15 |
| Defines roller measurements | 10 |
| Defines pivot measurements | 10 |
| Understands absolute encoder measurements | 10 |
| Defines closed-loop control methods | 10 |
| Defines open-loop voltage control | 10 |
| Documents methods with Javadocs | 5 |
| Understands real vs. simulated IO | 10 |
| Successfully tests the implementation | 10 |
| **Total** | **100** |

### Performance Expectations

- **90–100:** Independently understands and designs the IO interface.
- **80–89:** Correctly implements most fields and methods with minor assistance.
- **70–79:** Understands hardware abstraction but needs help with AdvantageKit inputs.
- **Below 70:** Needs additional practice with interfaces, measurements, and IO architecture.

---

# 22. Instructor Code Review: Important Design Considerations

Before using this interface as the final student reference, review the following items.

### 1. Default Methods

All hardware methods currently have empty default implementations.

This allows an implementation to omit a method without generating a compiler error.

However, it also means a missing implementation may silently do nothing.

Consider making important methods abstract so that every implementation must provide them.

For example:

```java
void setVelocity(double rotationsPerSecond);
void setPosition(double positionRadians);
void setVoltage(double pivotVolts, double rollerVolts);
void stop();
```

This makes the hardware contract stricter.

### 2. Units and Naming

The input fields use explicit unit suffixes:

- `Rotations`
- `RotationsPerSecond`
- `Radians`
- `RadiansPerSecond`
- `Volts`
- `Amps`
- `Celsius`

This is an excellent convention.

Students should maintain it throughout the real and simulated implementations.

### 3. Goal Values

The interface includes goal fields:

```java
rollerGoalVelocityRotationsPerSecond
pivotGoalRadians
```

Decide whether these represent:

- The most recently requested hardware goal.
- The currently active hardware goal.

Keep their meanings consistent across real and simulated IO.

### 4. Control Status Fields

The interface contains:

```java
rollerVelocityControlEnabled
pivotPositionControlEnabled
```

These should accurately reflect the active control mode.

Do not assume that sending a request means the controller successfully entered that mode.

### 5. Encoder Data

The interface provides both raw and corrected absolute encoder values.

This is useful for diagnosing offset configuration.

Make sure the real and simulated implementations use the same coordinate convention.

### 6. Default Input Values

All numeric values initialize to zero.

This is appropriate for creating a predictable initial input object, but zero should not automatically be interpreted as a valid sensor measurement.

Consider adding sensor connectivity fields so the mechanism can distinguish between:

- A valid measurement of zero.
- A disconnected sensor returning a default value.

### 7. Roller and Pivot Independence

The interface provides separate velocity and position methods, but the voltage method controls both motors together.

If independent motor control becomes necessary, consider introducing:

```java
void setRollerVoltage(double volts);
void setPivotVoltage(double volts);
```

This gives the mechanism more granular control.

### 8. Javadoc Accuracy

The class currently describes the interface as an Intake flywheel abstraction.

Since it controls both the roller and pivot, update the description to reflect the complete mechanism.

---

# 23. Final Takeaways

At the end of this lesson, students should understand that the IO interface is the foundation of the AdvantageKit architecture.

It defines the communication contract between mechanism logic and hardware implementations.

Students should leave understanding:

- **Interfaces:** Define the required hardware operations.
- **AutoLog:** Generates logging support for mechanism inputs.
- **Measurements:** Represent the actual state of the mechanism.
- **Goals:** Represent requested mechanism behavior.
- **Closed-loop control:** Uses feedback to achieve a requested velocity or position.
- **Open-loop control:** Applies requested motor voltage without directly targeting a position or velocity.
- **Abstraction:** Allows real hardware and simulation to share the same mechanism logic.
- **Telemetry:** Makes mechanism behavior observable and diagnosable.

### The Central Programming Principle

The Intake mechanism should not need to know which motor controller or encoder is installed.

It should only need to know that the IO implementation can provide measurements and accept control requests.

This separation makes robot code easier to test, maintain, simulate, and extend.

---

# 24. Suggested Follow-Up Lessons

This lesson establishes the foundation for the remaining Intake implementation.

Recommended lesson order:

1. **IntakeIO:** Define the hardware abstraction interface and logged measurements.
2. **IntakeIOReal:** Implement the Phoenix 6 motor controllers and CANcoder.
3. **IntakeIOSim:** Implement roller and pivot physics simulation.
4. **IntakeState:** Define mechanism operating states and associated control values.
5. **Intake:** Integrate IO, state management, commands, and telemetry.
6. **Teleop Integration:** Bind controller inputs to Intake commands.
7. **Simulation Testing:** Validate mechanism behavior without physical hardware.
8. **AdvantageScope:** Analyze recorded intake telemetry.

**Expected Final Outcome:** Students can design an AdvantageKit IO interface that cleanly separates mechanism logic from hardware implementation and supports real hardware, simulation, and unit testing using a consistent set of inputs and control methods.