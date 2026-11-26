# Lesson: Defining Intake Mechanism States with `IntakeState`

## Overview

In this lesson, you will define the operating states of the Intake mechanism using a Java enumeration.

The `IntakeState` enum describes the different ways the Intake can operate. Each state contains two values:

- The requested roller speed.
- The requested pivot position.

Rather than passing unrelated speed and position values throughout the robot code, we can group them into named states.

For example:

- `IDLE` represents a stopped Intake.
- `FORWARD` runs the roller forward while positioning the pivot.
- `REVERSE` runs the roller backward while positioning the pivot.
- `FORWARDDUTYCYCLE` applies open-loop forward output.
- `REVERSEDUTYCYCLE` applies open-loop reverse output.

This approach makes the mechanism easier to control, read, and maintain.

## Learning Objectives

By the end of this lesson, you should be able to:

1. Explain what a Java enum is and why it is useful.
2. Define an enum that implements an interface.
3. Use enum constructors to initialize state-specific values.
4. Understand constructor overloading.
5. Work with WPILib units such as `RPM`, `Degrees`, `Angle`, and `AngularVelocity`.
6. Distinguish between closed-loop velocity control and open-loop duty-cycle control.
7. Explain how mechanism states provide configuration to a subsystem.
8. Identify potential problems with unit conversions and signed measurements.

## Prerequisites

Before starting, you should understand:

- Java classes, fields, constructors, and methods.
- Java enumerations.
- Java interfaces.
- Basic object-oriented programming.
- WPILib units and measurements.
- The purpose of the `Intake` mechanism and its `IntakeIO` interface.
- The difference between position, velocity, and duty-cycle control.

## 1. Understanding Java Enums

An enum is a special Java type used to define a fixed collection of named constants.

For the Intake, we define the available operating states:

```java
public enum IntakeState implements SubsystemState {
    IDLE,
    FORWARD,
    REVERSE
}
```

Each constant represents a state that the Intake mechanism can enter.

Using an enum is better than passing arbitrary strings or numbers around.

For example, consider these two approaches:

```java
intake.setState("FORWARD");
```

and:

```java
intake.setState(IntakeState.FORWARD);
```

The second approach is safer because Java knows that `IntakeState.FORWARD` is a valid enum constant.

It also makes the available states easier to discover in an IDE.

## 2. Implementing the SubsystemState Interface

The enum declaration includes:

```java
public enum IntakeState implements SubsystemState {
```

This means `IntakeState` implements the `SubsystemState` interface.

The interface provides a shared type for states used by different robot mechanisms.

For example, the robot may have:

- `IntakeState`
- `ShooterState`
- `ClimberState`
- `ElevatorState`

Each mechanism can define its own enum while still implementing the common `SubsystemState` interface.

This makes it possible for shared utilities, such as a subsystem state machine, to work with different mechanism state types.

**Student checkpoint:** Find the `SubsystemState` interface in the project. Identify whether it defines any required methods and explain how `IntakeState` satisfies those requirements.

## 3. Defining the Intake States

The enum defines five states:

```java
public enum IntakeState implements SubsystemState {
    IDLE(0.0, 0),
    FORWARDDUTYCYCLE(0.3, 0.0),
    FORWARD(RPM.of(2330.0), Degrees.of(45.0)),
    REVERSEDUTYCYCLE(-0.3, 0),
    REVERSE(RPM.of(-2330), Degrees.of(0));
}
```

Each state is initialized with two values:

1. Roller speed.
2. Pivot position.

The values describe what the Intake should do when that state is active.

| State | Roller setting | Pivot position |
|---|---:|---:|
| `IDLE` | 0 | 0 |
| `FORWARDDUTYCYCLE` | 0.3 | 0 |
| `FORWARD` | 2330 RPM | 45 degrees |
| `REVERSEDUTYCYCLE` | -0.3 | 0 |
| `REVERSE` | -2330 RPM | 0 degrees |

The exact physical meaning of each position depends on the robot's pivot coordinate system and calibration.

### Why use named states?

Without an enum, code might contain repeated values:

```java
intake.setVelocity(2330.0);
intake.setPosition(45.0);
```

With an enum, the values are grouped together:

```java
intake.setState(IntakeState.FORWARD);
```

This makes the code easier to understand and reduces duplicated configuration.

## 4. Creating the State Fields

The enum stores two values:

```java
private final double rollerSpeed;
private final double position;
```

These fields hold the values associated with each state.

### Why are the fields private?

The `private` modifier prevents other classes from directly changing the fields.

For example, this is not allowed:

```java
IntakeState.FORWARD.rollerSpeed = 1000.0;
```

This protects the state configuration.

### Why are the fields final?

The `final` modifier means each field can only be assigned once.

The values are assigned when the enum constants are initialized.

After initialization, the state configuration cannot be changed.

This is useful because enum constants should represent stable, predefined configurations.

### Why use double instead of WPILib measurement types?

This implementation stores the values as primitive `double` fields.

That can be convenient when passing values into existing code that expects doubles.

However, it means the unit is no longer represented by the field's type.

For example:

```java
private final double rollerSpeed;
```

Does this represent RPM, rotations per second, or radians per second?

The answer depends on how the value is initialized and how it is later used.

For this reason, units must be documented and handled consistently.

## 5. Understanding the First Constructor

The first constructor accepts WPILib measurement types:

```java
IntakeState(AngularVelocity rollerSpeed, Angle position) {
    this.rollerSpeed = rollerSpeed.magnitude();
    this.position = position.magnitude();
}
```

This constructor is used by enum constants such as:

```java
FORWARD(RPM.of(2330.0), Degrees.of(45.0)),
REVERSE(RPM.of(-2330), Degrees.of(0));
```

The constructor receives:

- An `AngularVelocity` measurement.
- An `Angle` measurement.

These types communicate what each value represents.

For example:

```java
RPM.of(2330.0)
```

Creates an angular velocity measurement representing 2330 revolutions per minute.

And:

```java
Degrees.of(45.0)
```

Creates an angle measurement representing 45 degrees.

### Understanding `.magnitude()`

The code uses:

```java
rollerSpeed.magnitude()
```

and:

```java
position.magnitude()
```

The `magnitude()` method returns the numeric magnitude of a measurement.

However, there is an important issue: a magnitude operation may discard the sign of a measurement.

That matters for the reverse state.

The reverse state is initialized with:

```java
RPM.of(-2330)
```

If `.magnitude()` returns an absolute value, the stored roller speed becomes positive rather than negative.

The `REVERSE` state would then request forward roller movement instead of reverse movement.

### Recommended improvement

Convert the measurement to the specific unit expected by the rest of the mechanism code.

If `getRollerSpeed()` is expected to return RPM:

```java
IntakeState(AngularVelocity rollerSpeed, Angle position) {
    this.rollerSpeed = rollerSpeed.in(RPM);
    this.position = position.in(Degrees);
}
```

This preserves the sign of the measurement while making the numeric units explicit.

If the pivot uses radians internally, store the position in radians instead:

```java
this.position = position.in(Radians);
```

The important rule is that the enum's stored units must match the units expected by the code that consumes them.

## 6. Understanding Constructor Overloading

The enum contains three constructors:

```java
IntakeState(AngularVelocity rollerSpeed, Angle position)
```

```java
IntakeState(double rollerSpeed, Angle position)
```

```java
IntakeState(double rollerSpeed, double position)
```

This is called constructor overloading.

Constructor overloading allows a class or enum to define multiple constructors with different parameter types.

Java selects the appropriate constructor based on the arguments supplied.

### Constructor 1: Measurement types

```java
IntakeState(AngularVelocity rollerSpeed, Angle position)
```

Used when both values are WPILib measurements.

Example:

```java
FORWARD(RPM.of(2330.0), Degrees.of(45.0))
```

This constructor is useful when defining states with explicit units.

### Constructor 2: Double and Angle

```java
IntakeState(double rollerSpeed, Angle position)
```

Used when the roller speed is supplied as a primitive double but the pivot position is supplied as an `Angle`.

Example:

```java
SOME_STATE(0.3, Degrees.of(45.0))
```

The constructor stores the roller speed directly and extracts the numeric angle.

This constructor does not appear to be used by the five enum constants currently shown, but it may be intended for future states.

### Constructor 3: Two doubles

```java
IntakeState(double rollerSpeed, double position)
```

Used when both values are supplied as primitive doubles.

Examples:

```java
IDLE(0.0, 0),
FORWARDDUTYCYCLE(0.3, 0.0),
REVERSEDUTYCYCLE(-0.3, 0)
```

These values are stored directly.

Because they are primitive doubles, the compiler cannot verify their units.

The developer must know what units these values represent.

## 7. Understanding the State Values

Let's examine each state and how it is intended to operate.

### IDLE

```java
IDLE(0.0, 0),
```

The Intake should not request roller movement.

The pivot target is zero.

The mechanism's state-handling code determines whether this means the motors are stopped, the pivot is held, or another idle behavior is used.

### FORWARDDUTYCYCLE

```java
FORWARDDUTYCYCLE(0.3, 0.0),
```

The roller value is a duty-cycle-style command of 0.3.

This represents 30% output if the value is interpreted as a normalized duty cycle.

The pivot position is zero.

The `Intake` mechanism may convert this normalized value into a voltage request using the battery voltage.

For example:

\[
V_{\text{requested}} =
0.3 \times V_{\text{battery}}
\]

At 12 volts:

\[
V_{\text{requested}} = 3.6 \text{ volts}
\]

### FORWARD

```java
FORWARD(RPM.of(2330.0), Degrees.of(45.0)),
```

This state requests:

- A roller velocity of 2330 RPM.
- A pivot position of 45 degrees.

The mechanism can use these values to request closed-loop velocity and position control.

### REVERSEDUTYCYCLE

```java
REVERSEDUTYCYCLE(-0.3, 0),
```

This state requests negative open-loop output.

A negative value reverses the direction of motor output, assuming the motor's inversion and configuration are set appropriately.

### REVERSE

```java
REVERSE(RPM.of(-2330), Degrees.of(0));
```

This state requests:

- A roller velocity of -2330 RPM.
- A pivot position of zero degrees.

**Important:** The current constructor's use of `.magnitude()` may remove the negative sign. This state should be tested after correcting the unit conversion.

## 8. Providing Access to State Values

The enum provides two getter methods:

```java
public double getPosition() {
    return position;
}
```

and:

```java
public double getRollerSpeed() {
    return rollerSpeed;
}
```

These methods allow other classes to read the values associated with a state.

For example, the Intake mechanism can retrieve the roller speed:

```java
double requestedSpeed =
    IntakeState.FORWARD.getRollerSpeed();
```

It can also retrieve the pivot position:

```java
double requestedPosition =
    IntakeState.FORWARD.getPosition();
```

The getters do not change the state.

They simply return the values stored when the enum constant was initialized.

### Why not make the fields public?

Keeping the fields private protects the implementation and gives the enum control over how its values are accessed.

It also allows the internal representation to change later without requiring every class that uses `IntakeState` to be rewritten.

## 9. How IntakeState Works with the Intake Mechanism

The enum is designed to work with the `Intake` mechanism's state machine.

For example, the mechanism may receive a state:

```java
IntakeState state = IntakeState.FORWARD;
```

It can then retrieve the state configuration:

```java
double rollerSpeed = state.getRollerSpeed();
double pivotPosition = state.getPosition();
```

The mechanism uses those values to decide which IO operations to call.

Conceptually:

```text
        IntakeState.FORWARD
                 |
          +------+------+
          |             |
          v             v
      Roller Speed   Pivot Position
        2330 RPM        45 Degrees
          |             |
          v             v
     setVelocity()  setPosition()
          |             |
          +------+------+
                 |
                 v
             IntakeIO
                 |
                 v
             Hardware
```

The enum defines the desired configuration.

The Intake mechanism decides how to apply that configuration.

The IO implementation communicates with the actual hardware.

This separation is important because the enum should not need to know whether the robot is running real hardware or simulation.

## 10. Important Design Improvements

The current implementation is a useful starting point, but there are several ways to make it safer and easier to maintain.

### A. Use consistent units

The most important improvement is to define exactly what the primitive values represent.

For example:

- `rollerSpeed` could always represent RPM.
- `position` could always represent radians.

Then every constructor must convert its inputs into those units.

A consistent convention prevents bugs when the Intake mechanism reads the values.

### B. Preserve negative values

Do not use an absolute-magnitude conversion when the sign determines motor direction.

For example:

```java
this.rollerSpeed = rollerSpeed.in(RPM);
```

preserves negative velocity.

Likewise, use a signed angle conversion if negative pivot angles are valid:

```java
this.position = position.in(Radians);
```

### C. Make control modes explicit

The enum currently stores a roller value, but the value has different meanings depending on the state:

- In `FORWARD` and `REVERSE`, it represents a velocity target.
- In `FORWARDDUTYCYCLE` and `REVERSEDUTYCYCLE`, it represents a normalized open-loop output.

This is a potential source of confusion.

A future improvement would be to define an explicit control mode, such as:

```java
public enum RollerControlMode {
    STOPPED,
    VELOCITY,
    DUTY_CYCLE
}
```

Each Intake state could then identify which control mode it requires.

This avoids relying on the name of the state to infer how a numeric value should be interpreted.

### D. Consider using WPILib measurement types in the fields

An alternative is to store the measurements themselves:

```java
private final AngularVelocity rollerSpeed;
private final Angle position;
```

This preserves unit information inside the enum.

The getters could then return measurement types:

```java
public AngularVelocity getRollerSpeed() {
    return rollerSpeed;
}

public Angle getPosition() {
    return position;
}
```

This is more type-safe, but it requires the Intake mechanism to accept measurement types rather than primitive doubles.

Choose one approach and use it consistently across the mechanism.

## 11. Student Exercises

Complete the following exercises to demonstrate your understanding.

### Exercise 1: Add a new Intake state

Create a state named `SLOWFORWARD`.

It should:

- Run the roller at 1000 RPM.
- Position the pivot at 20 degrees.

Use WPILib measurement types.

### Exercise 2: Add a reverse duty-cycle state

Create a state named `SLOWREVERSE`.

It should:

- Run the roller at -0.15 duty cycle.
- Keep the pivot at zero degrees.

Explain why the negative sign matters.

### Exercise 3: Trace constructor selection

For each of the following constants, identify which constructor Java selects:

```java
TEST_A(0.0, 0.0)
TEST_B(RPM.of(1000), Degrees.of(30))
TEST_C(0.5, Degrees.of(45))
```

Explain why Java selects each constructor.

### Exercise 4: Fix the sign problem

Modify the measurement constructor so that a negative velocity remains negative.

Then explain why `.magnitude()` may be inappropriate for a signed velocity measurement.

### Exercise 5: Standardize the units

Modify the enum so that:

- Roller velocity is always stored in RPM.
- Pivot position is always stored in radians.

Update all constructors to convert their inputs into those units.

Document the units in the field Javadocs.

### Exercise 6: Add an explicit control mode

Create a `RollerControlMode` enum with these values:

- `STOPPED`
- `VELOCITY`
- `DUTY_CYCLE`

Update the Intake states so each one identifies its intended roller control mode.

Explain how this improves readability and prevents unit confusion.

## 12. Knowledge Check

Answer these questions before moving on.

1. What is a Java enum?
2. Why is `IntakeState` an enum instead of a collection of unrelated constants?
3. What does `implements SubsystemState` mean?
4. Why are the state fields declared `private final`?
5. What is constructor overloading?
6. Which constructor is selected for `FORWARD`?
7. Why are WPILib measurement types useful?
8. What is the difference between RPM and rotations per second?
9. Why can `.magnitude()` cause problems with reverse motor commands?
10. Why should open-loop duty-cycle values not be confused with velocity targets?
11. What is the responsibility of the Intake state enum compared with the Intake mechanism?
12. Why should the enum avoid directly controlling TalonFX hardware?

## 13. What Comes Next?

After completing this lesson, you should understand how the Intake's operating states are represented in Java.

The next step is to connect these states to the Intake mechanism's state machine and commands.

You will see how a command selects an `IntakeState`, how the mechanism reads the state's configuration, and how the `periodic()` method translates that configuration into calls to `IntakeIO`.

The overall design separates three responsibilities:

1. `IntakeState` defines what the mechanism should do.
2. `Intake` manages the mechanism's behavior and state transitions.
3. `IntakeIO` and its implementations communicate with hardware or simulation.