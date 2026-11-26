# Lesson Plan: Building an Intake Mechanism with AdvantageKit IO
## WPILib 2027 Alpha 7 — Command V3, Coroutines, State Machines, and Telemetry

---

## 1. Lesson Overview

**Topic:** Implementing an Intake Mechanism Using AdvantageKit and Command V3

**Framework:** WPILib 2027 Alpha 7

**Programming Language:** Java

**Estimated Duration:** 90–120 minutes

**Target Audience:** FRC students with experience in Java, WPILib, and basic robot mechanisms.

### Lesson Description

In this lesson, students will learn how to implement an intake mechanism using the AdvantageKit IO architecture and WPILib 2027 Command V3.

The intake mechanism consists of two independently controlled motor functions:

1. **Intake Roller:** Controls the rotation of the intake roller to acquire and transport game pieces.
2. **Intake Pivot:** Controls the angular position of the intake arm to move the intake between operating positions.

Students will explore how to organize mechanism control using an IO interface, a state machine, and coroutine-based commands.

They will also learn how to implement closed-loop velocity control, position control, and open-loop duty-cycle control.

Finally, students will examine how AdvantageKit records mechanism inputs and outputs, allowing programmers to diagnose and analyze robot behavior using AdvantageScope.

### Central Programming Concept

The mechanism should provide a clean interface for controlling the intake while hiding the underlying hardware implementation.

The OpMode or other robot control code should request an action. The Intake mechanism should manage the requested state, and the IO implementation should communicate with the hardware or simulation.

---

## 2. Learning Objectives

By the end of this lesson, students should be able to:

1. Explain the AdvantageKit IO architecture.
2. Understand the purpose of a mechanism implementing `Mechanism`.
3. Explain how constructor injection provides an IO implementation.
4. Understand the purpose of `IntakeIOInputsAutoLogged`.
5. Implement periodic input updates using `Logger.processInputs()`.
6. Explain the purpose of a subsystem state machine.
7. Understand WPILib 2027 coroutine-based commands.
8. Implement closed-loop velocity control.
9. Implement position-based control.
10. Implement open-loop duty-cycle control.
11. Understand the difference between voltage, velocity, and position control.
12. Use WPILib units to represent physical measurements.
13. Create triggers based on measured mechanism conditions.
14. Record useful telemetry using AdvantageKit.
15. Identify opportunities for improving mechanism safety and code organization.

---

## 3. Prerequisites

Students should understand:

- Java classes, constructors, and interfaces.
- Java enums.
- Java generics.
- Java `Optional`.
- Java lambda expressions.
- Basic WPILib command-based programming.
- WPILib 2027 Command V3.
- Physical units and measurements.
- Basic motor controller operation.
- AdvantageKit logging concepts.

### Required Existing Classes

This lesson assumes the following classes already exist:

| Class | Responsibility |
|---|---|
| `Intake` | Implements the intake mechanism. |
| `IntakeIO` | Defines the hardware-independent IO interface. |
| `IntakeIOInputsAutoLogged` | Automatically generated AdvantageKit input logging class. |
| `IntakeState` | Defines the available intake operating states. |
| `IntakeConstants` | Stores intake configuration and mechanical limits. |
| `SubsystemStateMachine` | Manages the currently requested mechanism state. |
| `IntakeIOReal` | Implements real hardware communication. |
| `IntakeIOSim` | Implements intake simulation. |

Students should understand that the Intake class does not directly construct motor controllers or sensors. Those responsibilities belong to the IO implementation.

---

# 4. Lesson Structure

| Section | Topic | Time |
|---|---|---:|
| 1 | Intake mechanism architecture | 10 min |
| 2 | Understanding the Intake class | 10 min |
| 3 | AdvantageKit IO and inputs | 15 min |
| 4 | State machine architecture | 15 min |
| 5 | Coroutine-based commands | 20 min |
| 6 | Position and velocity control | 15 min |
| 7 | Open-loop duty-cycle control | 10 min |
| 8 | Periodic updates and telemetry | 15 min |
| 9 | Testing and student challenges | 15 min |

---

# 5. Instructor Lesson

## Part 1: Understanding the Intake Mechanism

Before examining the code, students should understand the physical mechanism.

An intake typically consists of:

- A roller that captures game pieces.
- A pivoting arm that positions the intake.
- Motors and sensors that allow the mechanism to be controlled.

The two motor functions have different responsibilities.

### Intake Roller

The roller rotates to pull game pieces into the robot.

Typical control methods include:

- Velocity control.
- Open-loop duty-cycle control.
- Stop.

### Intake Pivot

The pivot changes the angle of the intake arm.

Typical control methods include:

- Position control.
- Open-loop duty-cycle control.
- Hold position.
- Stop.

### Discuss with Students

Ask:

1. Why might the roller and pivot require different control strategies?
2. Why would we want to control them independently?
3. When would position control be preferable to duty-cycle control?
4. Why might manual duty-cycle control be useful during testing?

**Teaching Point:** A mechanism can contain multiple actuators with different control requirements. The mechanism class provides one unified interface for controlling them.

---

## Part 2: Understanding the Intake Class

Start with the package declaration and imports.

```java
package first.robot.mechanisms;
```

This places the Intake class alongside the other robot mechanisms.

### Important Imports

```java
import org.littletonrobotics.junction.Logger;
import org.wpilib.command3.Command;
import org.wpilib.command3.Mechanism;
import org.wpilib.command3.Trigger;
```

These imports provide:

- `Logger`: AdvantageKit input and output logging.
- `Command`: Command V3 command abstraction.
- `Mechanism`: The mechanism interface.
- `Trigger`: A condition that can activate commands or other behavior.

Additional imports:

```java
import org.wpilib.smartdashboard.Mechanism2d;
import org.wpilib.smartdashboard.MechanismLigament2d;
```

These classes allow us to create a graphical representation of a mechanism.

```java
import org.wpilib.system.RobotController;
import org.wpilib.telemetry.Telemetry;
```

These provide access to robot controller information and unit-aware telemetry.

### Class Declaration

```java
public class Intake implements Mechanism {
```

The Intake class implements the WPILib 2027 `Mechanism` interface.

This establishes the class as a mechanism that can provide commands and participate in the Command V3 architecture.

### Why Implement Mechanism?

Implementing `Mechanism` allows us to organize motor control and mechanism behavior into a reusable class.

Instead of placing motor control code directly inside the OpMode, we expose methods such as:

```java
intake.setVelocity(...);
intake.setAngle(...);
intake.setRollerDutyCycle(...);
intake.setPivotDutyCycle(...);
intake.idle();
```

The OpMode can then request these actions without needing to know how the motors are controlled internally.

### Student Checkpoint

Ask students:

1. What is the purpose of the Intake class?
2. Why does it implement `Mechanism`?
3. Why should the OpMode avoid directly controlling the motor controllers?

---

# 6. State Machine and Mechanism Variables

Examine the first group of fields.

```java
private final SubsystemStateMachine<IntakeState> intakeStateMachine =
    new SubsystemStateMachine<>(IntakeState.IDLE);
```

### Understanding the State Machine

The state machine stores the currently requested intake operating state.

It is initialized with:

```java
IntakeState.IDLE
```

This means the intake begins in its idle state.

The state machine is generic:

```java
SubsystemStateMachine<IntakeState>
```

This means the state machine manages values of type `IntakeState`.

The available states are defined in the `IntakeState` enum.

For example, the enum might contain:

```java
public enum IntakeState {
    IDLE,
    FORWARD,
    REVERSE,
    FORWARDDUTYCYCLE,
    REVERSEDUTYCYCLE
}
```

The actual implementation should also define the associated roller speeds and pivot positions.

### Why Use a State Machine?

Without a state machine, mechanism control logic can become scattered throughout the code.

A state machine centralizes the requested operating mode.

For example:

```java
intakeStateMachine.setState(IntakeState.FORWARD);
```

The periodic method can then determine how the intake should operate based on that state.

### Discuss with Students

Ask:

1. What happens when the intake state changes?
2. Why should the mechanism know its current requested state?
3. What is the difference between an actual motor output and a requested mechanism state?

**Teaching Point:** The state machine represents what the mechanism should be doing, not necessarily what the mechanism is physically doing.

---

## Part 2A: Loop Period

```java
private static final double LOOP_PERIOD_SECONDS = 0.020;
```

This represents a 20-millisecond loop period.

The corresponding update frequency is:

\[
f = \frac{1}{0.020} = 50 \text{ Hz}
\]

This is a common periodic update rate for FRC robot control.

However, in the supplied implementation, this constant is not currently referenced.

Students should understand that periodic mechanism updates should use the robot's established loop timing rather than introducing unnecessary timing assumptions.

---

## Part 2B: IO and Logged Inputs

Examine:

```java
private final IntakeIO io;
private final IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();
```

These two fields are fundamental to the AdvantageKit architecture.

### The IO Interface

```java
private final IntakeIO io;
```

This stores the IO implementation used by the mechanism.

Depending on the robot configuration, this could be:

- Real hardware.
- Simulation.
- A test implementation.

The Intake class does not need to know which implementation it receives.

### Auto-Logged Inputs

```java
private final IntakeIOInputsAutoLogged inputs =
    new IntakeIOInputsAutoLogged();
```

This object contains the measured values reported by the IO implementation.

Examples include:

- Roller position.
- Roller velocity.
- Roller voltage.
- Roller current.
- Pivot position.
- Pivot velocity.
- Pivot voltage.
- Pivot current.
- Pivot temperature.

The `AutoLogged` class is generated by AdvantageKit from the input definition in `IntakeIO`.

### Important Concept

The mechanism should use measured inputs rather than assuming that requested outputs were successfully achieved.

For example:

- A requested roller speed is not necessarily the measured roller speed.
- A requested pivot angle is not necessarily the measured pivot angle.

This distinction is essential for feedback control and troubleshooting.

---

# 7. Goal Tracking

Examine:

```java
private AngularVelocity rollerGoalVelocity = null;
private Angle pivotGoalAngle = null;
```

These fields track the requested mechanism goals.

### Roller Goal

```java
private AngularVelocity rollerGoalVelocity = null;
```

Stores the requested roller velocity.

### Pivot Goal

```java
private Angle pivotGoalAngle = null;
```

Stores the requested pivot angle.

Both values are nullable.

A `null` value indicates that the corresponding goal is not currently being tracked.

For example, when operating in open-loop duty-cycle mode, a velocity goal may no longer be relevant.

### Why Track Goals Separately?

The mechanism needs to distinguish between:

- What the driver requested.
- What the mechanism measured.
- What the motor controller is currently doing.

These are three different concepts.

### Student Checkpoint

Ask:

1. Why do we store a goal velocity?
2. Why should measured velocity be stored separately?
3. What does a `null` goal represent?
4. Why might a duty-cycle command clear a previously requested velocity goal?

---

# 8. Mechanism2d Visualization

Examine:

```java
private final Mechanism2d rollerMechanism2d =
    new Mechanism2d(1, 1);

private final MechanismLigament2d rollerSpokeLigament =
    rollerMechanism2d.getRoot("Axle", 0.5, 0.5)
        .append(new MechanismLigament2d("Flywheel", 0.4, 0.0));
```

### What Is Mechanism2d?

`Mechanism2d` provides a graphical representation of a robot mechanism.

It is useful for visualizing:

- Arm movement.
- Joint positions.
- Rotating components.
- Mechanism geometry.

### Understanding the Root

```java
rollerMechanism2d.getRoot("Axle", 0.5, 0.5)
```

This creates a root point named `Axle` at the coordinates:

- X = 0.5
- Y = 0.5

### Understanding the Ligament

```java
new MechanismLigament2d("Flywheel", 0.4, 0.0)
```

This creates a graphical ligament with:

- Name: `Flywheel`
- Length: 0.4
- Initial angle: 0 degrees

The ligament can later be updated to reflect the measured roller position.

### Updating the Visualization

Inside `periodic()`:

```java
rollerSpokeLigament.setAngle(
    inputs.rollerPositionRotations * 360.0 % 360.0);
```

This converts measured rotations into degrees.

For example:

| Position | Display Angle |
|---:|---:|
| 0 rotations | 0° |
| 0.25 rotations | 90° |
| 0.5 rotations | 180° |
| 0.75 rotations | 270° |
| 1 rotation | 0° |

The modulo operation keeps the displayed angle within one revolution.

**Instructor Note:** The current code updates the visualization object but does not publish `rollerMechanism2d` to AdvantageKit because its logging statement is commented out. Students should investigate how to expose it in their dashboard or telemetry workflow.

---

# 9. Constructor Injection

Examine the constructor:

```java
public Intake(IntakeIO io) {
    this.io = io;
}
```

The constructor receives an IO implementation.

This is an example of dependency injection.

The Intake mechanism does not create its own motor controllers or simulation objects.

Instead, another part of the robot creates the appropriate IO implementation and passes it into the Intake constructor.

For example:

```java
Intake intake = new Intake(new IntakeIOReal());
```

Or:

```java
Intake intake = new Intake(new IntakeIOSim());
```

The exact constructor arguments depend on the implementation.

### Why Is This Useful?

This approach allows the same Intake mechanism to work with:

- Real hardware.
- Simulation.
- Unit testing.

The mechanism logic remains unchanged.

### Student Exercise

Ask students to explain why this is preferable to creating a TalonFX directly inside the Intake constructor.

Expected answer:

The Intake class should not be responsible for selecting or initializing hardware implementations. Separating IO allows the same mechanism logic to operate in different environments.

---

# 10. Unit-Safe Measurement Methods

The next group of methods provides access to measured mechanism values.

## Part 10A: getVelocity()

```java
public AngularVelocity getVelocity() {
    return RotationsPerSecond.of(
        inputs.rollerVelocityRotationsPerSecond);
}
```

This returns the measured roller velocity.

The IO layer stores the measured velocity as a numerical value in rotations per second.

The mechanism converts it into a WPILib `AngularVelocity`.

### Why Use Units?

Compare:

```java
double velocity = 25.0;
```

With:

```java
AngularVelocity velocity =
    RotationsPerSecond.of(25.0);
```

The second example identifies the physical unit.

This makes the code easier to understand and reduces unit-conversion mistakes.

---

## Part 10B: getPosition()

```java
public Angle getPosition() {
    return Rotation.of(inputs.rollerPositionRotations);
}
```

Returns the measured roller position.

The returned value represents an angular position in rotations.

---

## Part 10C: getGoalVelocity()

```java
public Optional<AngularVelocity> getGoalVelocity() {
    return Optional.ofNullable(rollerGoalVelocity);
}
```

This method returns the requested roller velocity if one exists.

`Optional` is useful because the goal may be unavailable.

Instead of returning `null`, the method returns:

- `Optional.of(value)` when a goal exists.
- `Optional.empty()` when no goal exists.

### Student Checkpoint

Ask:

1. Why does `getGoalVelocity()` return `Optional`?
2. What is the difference between `Optional.empty()` and a velocity of zero?
3. Why should measured velocity and requested velocity be separate?

---

## Part 10D: getGoalPosition()

```java
public Optional<Angle> getGoalPosition() {
    return Optional.ofNullable(pivotGoalAngle);
}
```

This follows the same pattern but returns the requested pivot position.

Students should notice that the method only reports a goal if the field has actually been assigned.

In the supplied implementation, `pivotGoalAngle` is not assigned by `setAngle()`. This is an important item to address during the code review portion of the lesson.

---

## Part 10E: getVoltage()

```java
public org.wpilib.units.measure.Voltage getVoltage() {
    return Volts.of(inputs.rollerAppliedVolts)
        .plus(Volts.of(inputs.pivotAppliedVolts));
}
```

This returns the sum of the roller and pivot applied voltages.

Both values are converted into unit-safe voltage measurements before being added.

**Important:** This is an aggregate value, not necessarily the voltage applied to either individual motor.

For diagnostics, separate roller and pivot voltage accessors are generally more useful.

---

## Part 10F: getCurrent()

```java
public org.wpilib.units.measure.Current getCurrent() {
    return Amps.of(inputs.rollerStatorCurrentAmps)
        .plus(Amps.of(inputs.pivotCurrentAmps));
}
```

This returns an aggregate current measurement.

Students should understand that this combines two motor current measurements.

It should not replace individual current telemetry when diagnosing motor loading.

---

# 11. Pivot Position Control and Safety Limits

Examine the private method:

```java
private void setGoalAngle(Angle angle) {
    double requestedRadians = angle.in(Radians);

    double clampedRadians = Math.max(
        IntakeConstants.Pivot.MIN_ANGLE.in(Radians),
        Math.min(
            IntakeConstants.Pivot.MAX_ANGLE.in(Radians),
            requestedRadians));

    Angle clamped = Radians.of(clampedRadians);

    if (pivotGoalAngle == null ||
        !clamped.isEquivalent(pivotGoalAngle)) {

        io.setPosition(clampedRadians);
        pivotGoalAngle = clamped;
    }
}
```

This method demonstrates an important safety concept: software limits.

### Step 1: Convert to Radians

```java
double requestedRadians = angle.in(Radians);
```

The requested angle is converted into radians.

This is the unit expected by the IO position method in this implementation.

### Step 2: Clamp the Requested Angle

```java
double clampedRadians = Math.max(
    IntakeConstants.Pivot.MIN_ANGLE.in(Radians),
    Math.min(
        IntakeConstants.Pivot.MAX_ANGLE.in(Radians),
        requestedRadians));
```

This prevents the requested angle from exceeding the configured minimum and maximum.

Mathematically:

\[
\theta_{\text{safe}} =
\max(\theta_{\min},
\min(\theta_{\max},\theta_{\text{requested}}))
\]

For example, if:

- Minimum angle = -20 degrees.
- Maximum angle = 100 degrees.
- Requested angle = 120 degrees.

The resulting target is 100 degrees.

### Step 3: Create a Unit-Safe Angle

```java
Angle clamped = Radians.of(clampedRadians);
```

This converts the clamped value back into a WPILib angle measurement.

### Step 4: Avoid Unnecessary Updates

```java
if (pivotGoalAngle == null ||
    !clamped.isEquivalent(pivotGoalAngle))
```

The position request is only sent when the requested goal changes.

This avoids repeatedly sending identical position requests.

### Step 5: Send the Position Request

```java
io.setPosition(clampedRadians);
pivotGoalAngle = clamped;
```

The IO implementation receives the requested position, and the mechanism stores the active goal.

### Instructor Demonstration

Ask students to calculate the resulting angle for:

| Requested Angle | Minimum | Maximum | Result |
|---:|---:|---:|---:|
| -30° | -20° | 100° | -20° |
| 45° | -20° | 100° | 45° |
| 120° | -20° | 100° | 100° |

**Important:** Software clamping is not a substitute for physical limit switches, mechanical stops, or other appropriate safety measures.

Also note that `setGoalAngle()` is not currently called by any of the public commands in the supplied code. Students should investigate how to integrate it into the position-control implementation.

---

# 12. Coroutine-Based Commands

This is one of the most important sections of the lesson.

WPILib 2027 Command V3 supports coroutine-based command creation.

Examine the general structure:

```java
return run(coroutine -> {
    while (true) {
        // Mechanism control logic

        coroutine.yield();
    }
}).named("Command Name");
```

### Understanding run()

```java
run(coroutine -> {
```

Creates a command that executes a coroutine.

The coroutine provides a structured way to implement behavior that continues across multiple scheduler iterations.

### Understanding while(true)

```java
while (true) {
```

This represents a continuously running command.

The loop allows the mechanism to perform its control logic repeatedly.

### Understanding yield()

```java
coroutine.yield();
```

This suspends the coroutine and allows it to resume during a subsequent scheduler execution.

It is not equivalent to blocking the robot thread with `Thread.sleep()`.

It allows the command to cooperate with the scheduler instead of preventing other robot code from executing.

### Understanding named()

```java
.named("Command Name")
```

Assigns a descriptive name to the command.

This is useful when inspecting command execution and debugging behavior.

### Teaching Point

A coroutine-based command can maintain local execution flow across scheduler iterations without blocking the robot's main execution thread.

---

# 13. Roller Velocity Control

Examine:

```java
public Command setVelocity(IntakeState state) {
    return run(coroutine -> {
        while (true) {
            if (rollerGoalVelocity == null ||
                !RPM.of(state.getRollerSpeed())
                    .isEquivalent(rollerGoalVelocity)) {

                intakeStateMachine.setState(state);
            }

            rollerGoalVelocity = RPM.of(state.getRollerSpeed());

            coroutine.yield();
        }
    }).named("Intake Set Velocity");
}
```

### Purpose

This command requests a roller velocity based on the supplied IntakeState.

For example:

```java
intake.setVelocity(IntakeState.FORWARD);
```

The requested roller speed comes from:

```java
state.getRollerSpeed()
```

### Step 1: Check Whether the Goal Changed

```java
if (rollerGoalVelocity == null ||
    !RPM.of(state.getRollerSpeed())
        .isEquivalent(rollerGoalVelocity))
```

This checks whether a velocity goal exists and whether the requested speed differs from the previous goal.

`isEquivalent()` compares unit-aware measurements.

### Step 2: Update the State Machine

```java
intakeStateMachine.setState(state);
```

The mechanism transitions to the requested state.

### Step 3: Store the Requested Velocity

```java
rollerGoalVelocity =
    RPM.of(state.getRollerSpeed());
```

This records the requested roller velocity.

### Step 4: Yield

```java
coroutine.yield();
```

The command yields control until its next execution.

### Important Design Consideration

The method uses the roller speed stored in the IntakeState rather than accepting an AngularVelocity directly.

This means the requested velocity is coupled to the state definition.

An alternative API could accept an `AngularVelocity` directly if the mechanism needs arbitrary velocity targets.

### Student Exercise

Modify the existing velocity command to support another predefined roller speed.

Students should identify:

- Where the speed is defined.
- How the state is selected.
- How the IO implementation receives the requested velocity.

---

# 14. Pivot Position Control

Examine:

```java
public Command setAngle(IntakeState state) {
    return run(coroutine -> {
        while (true) {
            intakeStateMachine.setState(state);
            coroutine.yield();
        }
    }).named("Intake Set Angle");
}
```

### Purpose

This command requests a predefined intake state.

For example:

```java
intake.setAngle(IntakeState.FORWARD);
```

The state machine receives the requested state.

During the periodic update, the mechanism uses the state's position to command the pivot.

### Important Observation

Unlike `setVelocity()`, this command does not update `pivotGoalAngle`.

The current implementation therefore does not populate the value returned by:

```java
getGoalPosition()
```

Students should investigate how to update the tracked position goal when a new angle is requested.

A more complete implementation should route position requests through the existing `setGoalAngle()` method.

### Discuss with Students

1. Why does the command accept an IntakeState?
2. Where is the actual pivot position stored?
3. Why is the requested position different from the measured position?
4. How can we prevent the pivot from moving outside its safe range?

---

# 15. Open-Loop Roller Duty-Cycle Control

Examine:

```java
public Command setRollerDutyCycle(IntakeState state) {
    return run(coroutine -> {
        rollerGoalVelocity = null;

        while (true) {
            intakeStateMachine.setState(state);
            coroutine.yield();
        }
    }).named("Intake Set Duty Cycle");
}
```

### Purpose

This command controls the intake roller using an open-loop output.

Unlike velocity control, the command does not request a specific RPM.

Instead, it relies on the duty-cycle value stored in the IntakeState.

### Why Clear the Velocity Goal?

```java
rollerGoalVelocity = null;
```

When the roller is controlled through duty cycle, there is no active closed-loop velocity target.

Clearing the goal prevents telemetry and other code from treating an old velocity request as the current goal.

### Duty-Cycle Range

A duty cycle normally ranges from:

\[
-1.0 \leq D \leq 1.0
\]

| Duty Cycle | Requested Output |
|---:|---|
| 1.0 | 100% forward |
| 0.5 | 50% forward |
| 0.0 | 0% output |
| -0.5 | 50% reverse |
| -1.0 | 100% reverse |

Actual motor speed depends on the motor, load, battery voltage, and mechanism.

---

# 16. Open-Loop Pivot Duty-Cycle Control

Examine:

```java
public Command setPivotDutyCycle(IntakeState state) {
    return run(coroutine -> {
        pivotGoalAngle = null;

        while (true) {
            intakeStateMachine.setState(state);
            coroutine.yield();
        }
    }).named("Intake Set Duty Cycle");
}
```

This command operates the intake pivot using open-loop control.

### Why Clear the Position Goal?

```java
pivotGoalAngle = null;
```

A duty-cycle command does not request a specific pivot angle.

Therefore, the previous position goal should no longer be considered active.

### Compare the Two Duty-Cycle Commands

| Method | Controls | Clears |
|---|---|---|
| `setRollerDutyCycle()` | Roller | Roller velocity goal |
| `setPivotDutyCycle()` | Pivot | Pivot position goal |

### Student Checkpoint

Ask:

1. Why are there separate duty-cycle commands?
2. Why should each command clear its corresponding goal?
3. Why is open-loop control useful for testing?
4. What safety concerns exist when manually controlling a pivot?

---

# 17. Creating a Velocity Trigger

Examine:

```java
public Trigger near(
    AngularVelocity velocity,
    AngularVelocity tolerance) {

    return new Trigger(
        () -> getVelocity().isNear(velocity, tolerance));
}
```

### Purpose

This method creates a trigger that becomes active when the measured roller velocity is sufficiently close to a target velocity.

### Understanding the Parameters

```java
AngularVelocity velocity
```

The desired velocity.

```java
AngularVelocity tolerance
```

The acceptable difference between the measured velocity and the target.

### Understanding isNear()

```java
getVelocity().isNear(velocity, tolerance)
```

Checks whether the measured roller speed is within the specified tolerance.

Mathematically:

\[
|\omega_{\text{measured}}-\omega_{\text{target}}|
\leq \omega_{\text{tolerance}}
\]

### Example Usage

```java
intake.near(
    RPM.of(2000),
    RPM.of(100)
);
```

This trigger becomes active when the roller is within 100 RPM of 2000 RPM.

### Why Is This Useful?

Velocity triggers allow other robot actions to depend on mechanism readiness.

For example:

- Wait until the roller reaches its requested speed.
- Activate another mechanism after the intake is ready.
- Log when a mechanism reaches its target.

### Student Exercise

Create a trigger that becomes active when the intake roller reaches 1500 RPM within a tolerance of 75 RPM.

---

# 18. Hold and Idle Commands

## Part 18A: Hold Command

Examine:

```java
public Command hold(Angle angle) {
    return run(coroutine -> {
        while (true) {
            intakeStateMachine.setState(IntakeState.IDLE);
            coroutine.yield();
        }
    })
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Intake Hold");
}
```

### Purpose

This command provides a low-priority background command that places the intake into its idle state.

### Understanding Command Priority

```java
.withPriority(Command.LOWEST_PRIORITY)
```

Assigns the command the lowest scheduling priority.

This is useful for background behavior that should not prevent higher-priority commands from controlling the mechanism.

### Important Observation

The `angle` parameter is currently unused.

Despite the method being named `hold`, the implementation requests `IntakeState.IDLE` rather than commanding the pivot to the supplied angle.

Students should understand the distinction between:

- Stopping a mechanism.
- Holding a mechanism at a specific position.

A true position-hold implementation should command the supplied angle using the appropriate position-control method.

---

## Part 18B: Idle Command

```java
public Command idle() {
    return run(coroutine -> {
        rollerGoalVelocity = null;

        while (true) {
            intakeStateMachine.setState(IntakeState.IDLE);
            coroutine.yield();
        }
    })
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Intake Idle");
}
```

### Purpose

This command continuously requests the IDLE state.

It also clears the roller velocity goal.

### Why Use an Idle Command?

An idle command can provide predictable default behavior when no higher-priority command is controlling the intake.

However, students must understand that IDLE behavior is defined by the mechanism's periodic implementation.

The IDLE state must explicitly define what happens to the roller and pivot.

---

# 19. Periodic Updates and Mechanism Control

This is the most important section for understanding how the mechanism actually operates.

Examine:

```java
public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Intake", inputs);
```

### Step 1: Update Inputs

```java
io.updateInputs(inputs);
```

The IO implementation updates the measured values.

Depending on the environment, these values may come from:

- Motor controller sensors.
- Encoders.
- Simulation models.
- Test implementations.

### Step 2: Process AdvantageKit Inputs

```java
Logger.processInputs("Intake", inputs);
```

This records the input data using AdvantageKit.

The inputs can then be inspected in AdvantageScope.

### Why Is This Important?

Logging measured inputs allows programmers to investigate:

- Whether the motor reached its requested velocity.
- Whether the pivot reached its requested angle.
- Whether the motor is drawing excessive current.
- Whether the mechanism is overheating.
- Whether the mechanism behaves differently in simulation and on the real robot.

---

## Part 19A: State Machine Control

Examine the switch statement:

```java
switch (intakeStateMachine.getState()) {
    case IDLE:
        io.stop();
        break;

    case REVERSEDUTYCYCLE:
    case FORWARDDUTYCYCLE:
        double pivotVoltage =
            RobotController.getMeasureBatteryVoltage()
                .in(Volts)
                * Math.max(
                    -1.0,
                    Math.min(
                        1.0,
                        intakeStateMachine.getState().getPosition()));

        double rollerVoltage =
            RobotController.getMeasureBatteryVoltage()
                .in(Volts)
                * Math.max(
                    -1.0,
                    Math.min(
                        1.0,
                        intakeStateMachine.getState().getRollerSpeed()));

        io.setVoltage(pivotVoltage, rollerVoltage);

    case REVERSE:
    case FORWARD:
        io.setPosition(
            intakeStateMachine.getState().getPosition());

        io.setVelocity(
            intakeStateMachine.getState().getRollerSpeed());
}
```

### Understanding the IDLE State

```java
case IDLE:
    io.stop();
    break;
```

When the mechanism is idle, the IO implementation is instructed to stop.

The exact behavior depends on the IO implementation and its stop configuration.

For example, it might use brake or coast mode.

### Understanding Duty-Cycle States

```java
case REVERSEDUTYCYCLE:
case FORWARDDUTYCYCLE:
```

These states use open-loop control.

The mechanism reads the requested duty-cycle values from the state.

### Understanding Battery Voltage Compensation

```java
RobotController.getMeasureBatteryVoltage().in(Volts)
```

Retrieves the current battery voltage.

The requested duty cycle is multiplied by battery voltage to calculate a requested motor voltage.

Mathematically:

\[
V_{\text{requested}} =
V_{\text{battery}} \times D
\]

For example:

- Battery voltage = 12 V.
- Duty cycle = 0.5.
- Requested voltage = 6 V.

### Understanding Math.max() and Math.min()

```java
Math.max(-1.0, Math.min(1.0, dutyCycle))
```

Clamps the duty cycle between -1 and +1.

This prevents an invalid requested duty-cycle value from producing a voltage outside the intended range.

### Understanding Voltage Control

```java
io.setVoltage(pivotVoltage, rollerVoltage);
```

This sends the requested pivot and roller voltages to the IO implementation.

The IO layer then translates these requests into hardware or simulation operations.

---

## Important Instructor Code Review: Switch Fall-Through

The supplied switch statement has an important issue.

There is no `break` after:

```java
io.setVoltage(pivotVoltage, rollerVoltage);
```

Consequently, Java continues into the following `REVERSE` and `FORWARD` cases.

That means a duty-cycle state can execute both:

1. Open-loop voltage control.
2. Position and velocity control.

These are conflicting control requests.

For the intended behavior, the duty-cycle cases should terminate after sending their voltage requests.

For example:

```java
case REVERSEDUTYCYCLE:
case FORWARDDUTYCYCLE:
    // Calculate requested voltages.
    io.setVoltage(pivotVoltage, rollerVoltage);
    break;

case REVERSE:
case FORWARD:
    io.setPosition(
        intakeStateMachine.getState().getPosition());

    io.setVelocity(
        intakeStateMachine.getState().getRollerSpeed());
    break;
```

This is an excellent teaching opportunity to explain Java switch fall-through.

**Student Question:** Why is the `break` statement necessary here?

Expected answer: Without it, Java executes the following case statements even though the mechanism is already in a duty-cycle state.

---

# 20. AdvantageKit Telemetry

The remainder of `periodic()` records mechanism measurements.

Students should understand that telemetry is not just for debugging after something goes wrong. It is also useful for evaluating mechanism performance and improving robot behavior.

## Roller Telemetry

```java
Logger.recordOutput(
    "Intake/Roller/VelocityRPS",
    inputs.rollerVelocityRotationsPerSecond);

Logger.recordOutput(
    "Intake/Roller/AppliedVolts",
    inputs.rollerAppliedVolts);

Logger.recordOutput(
    "Intake/Roller/StatorCurrentAmps",
    inputs.rollerStatorCurrentAmps);
```

These values allow programmers to monitor:

- Measured roller velocity.
- Applied motor voltage.
- Motor stator current.

### Goal Velocity Telemetry

```java
if (rollerGoalVelocity != null) {
    Telemetry.log(
        "Intake/Roller/GoalVelocity",
        rollerGoalVelocity);
}
```

The requested roller velocity is logged only when a goal exists.

This allows students to compare:

- Requested velocity.
- Measured velocity.

The difference between these values can help identify problems with tuning, mechanical loading, or motor performance.

---

## Pivot Telemetry

```java
Logger.recordOutput(
    "Intake/AngleRadians",
    inputs.pivotPositionRadians);

Logger.recordOutput(
    "Intake/VelocityRadiansPerSecond",
    inputs.pivotVelocityRadiansPerSecond);

Logger.recordOutput(
    "Intake/AppliedVolts",
    inputs.pivotAppliedVolts);

Logger.recordOutput(
    "Intake/CurrentAmps",
    inputs.pivotCurrentAmps);

Logger.recordOutput(
    "Intake/TemperatureCelsius",
    inputs.pivotTemperatureCelsius);
```

These measurements help programmers understand the pivot's behavior.

For example:

- Position indicates the current arm angle.
- Velocity indicates how quickly the arm is moving.
- Voltage indicates the applied motor output.
- Current indicates electrical loading.
- Temperature indicates motor thermal conditions.

### Goal Position Telemetry

```java
if (pivotGoalAngle != null) {
    Telemetry.log(
        "Intake/Pivot/GoalAngle",
        pivotGoalAngle);
}
```

This records the requested pivot angle when a goal exists.

Remember that the current implementation does not assign `pivotGoalAngle` in `setAngle()`. Students should correct this before expecting useful position-goal telemetry.

---

# 21. Simulation Periodic

Examine:

```java
public void simulationPeriodic() {
}
```

This method is currently empty.

It provides a place for simulation-specific updates if the mechanism needs to advance its own simulation model.

However, many AdvantageKit IO implementations update their simulation models from their own periodic input-update methods.

Students should determine where the simulation calculations belong in their architecture.

### Important Distinction

- `periodic()` updates mechanism inputs, commands outputs, and records telemetry.
- `simulationPeriodic()` can perform additional simulation-specific work.

Avoid updating the same simulation model twice.

### Student Exercise

Have students inspect `IntakeIOSim` and identify:

1. Where the roller simulation is updated.
2. Where the pivot simulation is updated.
3. Where simulated sensor values are written.
4. Whether `simulationPeriodic()` needs any additional implementation.

---

# 22. Stop Method

Examine:

```java
public void stop() {
    rollerGoalVelocity = null;
    pivotGoalAngle = null;
    io.stop();
}
```

This method provides an immediate mechanism stop operation.

### Step 1: Clear the Roller Goal

```java
rollerGoalVelocity = null;
```

Removes the active velocity goal.

### Step 2: Clear the Pivot Goal

```java
pivotGoalAngle = null;
```

Removes the active position goal.

### Step 3: Stop the IO

```java
io.stop();
```

Instructs the hardware or simulation implementation to stop the mechanism.

### Why Is This Important?

A mechanism should provide a clear way to stop its actuators and reset its goal tracking.

This is useful for:

- Emergency handling.
- Mode transitions.
- Testing.
- Recovery from unexpected behavior.

**Important:** Clearing goal variables does not automatically update the state machine. A robust implementation should ensure that the state machine and IO outputs remain synchronized after a stop request.

---

# 23. Complete Intake Architecture

The following diagram summarizes the relationship between the major components.

```text
                 Robot / OpMode
                       |
                       v
                     Intake
                       |
          +------------+------------+
          |                         |
          v                         v
     Command V3                 State Machine
          |                         |
          |                         v
          |                    IntakeState
          |                         |
          +------------+------------+
                       |
                       v
                    periodic()
                       |
             +---------+---------+
             |                   |
             v                   v
        IntakeIO              AdvantageKit
             |                   |
       +-----+-----+             |
       |           |             |
       v           v             v
     Real        Sim          Telemetry
     Hardware    Hardware     / Logs
```

### Key Architectural Principles

- The OpMode determines what action should occur.
- The Intake mechanism manages mechanism behavior.
- The state machine identifies the requested operating state.
- Command V3 schedules and manages mechanism commands.
- The IO interface separates hardware from mechanism logic.
- AdvantageKit records measured inputs and outputs.

---

# 24. Testing and Troubleshooting

Testing should be performed in stages.

## Stage 1: Code Review

Before running the mechanism, verify:

- [ ] The Intake class implements `Mechanism`.
- [ ] The IO implementation is injected through the constructor.
- [ ] Logged inputs are updated correctly.
- [ ] AdvantageKit processes the input data.
- [ ] The state machine initializes to IDLE.
- [ ] Position limits are defined.
- [ ] Velocity goals are tracked correctly.
- [ ] Position goals are tracked correctly.
- [ ] Duty-cycle commands clear the appropriate goals.
- [ ] Switch cases terminate correctly.
- [ ] Commands have appropriate mechanism requirements.
- [ ] The stop method clears goals and stops the IO.

## Stage 2: Simulation

Test each operating state independently.

| Test | Expected Behavior |
|---|---|
| IDLE | Both motors stop according to IO configuration. |
| FORWARD | Pivot and roller receive forward state targets. |
| REVERSE | Pivot and roller receive reverse state targets. |
| FORWARDDUTYCYCLE | Both motors receive their configured forward voltage requests. |
| REVERSEDUTYCYCLE | Both motors receive their configured reverse voltage requests. |

Also verify:

- Measured roller velocity changes appropriately.
- Measured roller position updates.
- Pivot position changes toward its target.
- Pivot velocity is reported correctly.
- Goal values appear in telemetry.
- Current and voltage values are recorded.
- The velocity trigger becomes active within tolerance.

## Stage 3: AdvantageScope

Use AdvantageScope to examine:

**Roller**
- `Intake/Roller/VelocityRPS`
- `Intake/Roller/GoalVelocity`
- `Intake/Roller/AppliedVolts`
- `Intake/Roller/StatorCurrentAmps`

**Pivot**
- `Intake/AngleRadians`
- `Intake/VelocityRadiansPerSecond`
- `Intake/AppliedVolts`
- `Intake/CurrentAmps`
- `Intake/TemperatureCelsius`
- `Intake/Pivot/GoalAngle`

Students should compare requested and measured values to determine whether the mechanism is behaving as expected.

## Stage 4: Real Robot

**Safety requirements:**

- Secure the robot before testing.
- Keep people clear of moving intake components.
- Verify mechanical limits.
- Begin with conservative duty-cycle values.
- Confirm motor directions before applying larger outputs.
- Verify that the pivot cannot collide with the chassis.
- Test roller and pivot independently before operating both together.
- Have an instructor supervise testing.

---

# 25. Student Programming Challenges

## Challenge 1: Add a New Intake State

Add a new state called:

`INTAKE_POSITION`

Requirements:

- Define the desired pivot position.
- Define the desired roller velocity.
- Add the state to `IntakeState`.
- Test the state in simulation.

Students must explain how the state machine uses the new state.

## Challenge 2: Implement Position Goal Tracking

The current `setAngle()` method does not update `pivotGoalAngle`.

Modify the implementation so that:

- The requested angle is converted into a unit-safe measurement.
- The angle is clamped to the configured mechanical limits.
- The pivot goal is stored.
- The IO implementation receives the correct position request.
- AdvantageKit records the goal.

Students should explain why the requested angle and measured angle must be stored separately.

## Challenge 3: Implement a True Hold Command

The current `hold(Angle angle)` method ignores its parameter.

Modify it so that:

- The supplied angle is respected.
- The angle is clamped to safe limits.
- The pivot uses position control to maintain that angle.
- Higher-priority commands can interrupt the hold command.

Students should explain why position holding is different from stopping the motor.

## Challenge 4: Add a Roller Speed Trigger

Create a trigger that becomes active when the roller reaches 3000 RPM within 100 RPM.

Requirements:

- Use the existing `near()` method.
- Use WPILib unit-safe measurements.
- Demonstrate the trigger in an OpMode or test.

## Challenge 5: Add Individual Motor Telemetry

Create methods that return:

- Roller voltage.
- Pivot voltage.
- Roller current.
- Pivot current.

Students should explain why individual measurements are more useful for diagnosing motor problems than aggregate measurements.

## Challenge 6: Improve State Machine Control

Review the `periodic()` switch statement.

Requirements:

- Eliminate unintended fall-through.
- Ensure duty-cycle states only execute open-loop control.
- Ensure position and velocity states only execute their intended control paths.
- Ensure IDLE behaves consistently.

## Challenge 7: Add Safety Interlocks

Implement a safety restriction that prevents the intake from moving beyond its configured limits.

Possible approaches:

- Software position clamping.
- Limit switches.
- Sensor validation.
- Command interruption.
- Safe-state behavior.

Students must explain which safety responsibilities belong in the Intake mechanism and which belong in the IO implementation.

## Challenge 8: Improve Mechanism Visualization

Enable the existing `Mechanism2d` visualization.

Requirements:

- Publish the mechanism representation.
- Update the graphical ligament using measured roller position.
- Verify the visualization in the dashboard.

Students should explain the difference between a graphical representation and the actual simulated mechanism.

---

# 26. Assessment

## Knowledge Assessment

Students should be able to answer:

1. What is the purpose of the AdvantageKit IO architecture?
2. Why does Intake implement `Mechanism`?
3. What is the purpose of `IntakeIOInputsAutoLogged`?
4. What does `Logger.processInputs()` accomplish?
5. Why use a state machine?
6. What is the difference between a requested state and a measured mechanism position?
7. How does `coroutine.yield()` work?
8. What is the difference between velocity control and duty-cycle control?
9. Why do we clamp pivot angles?
10. What does `Optional` provide?
11. How does the `near()` trigger work?
12. What is the purpose of command priority?
13. Why is it important to clear old mechanism goals?
14. What is Java switch fall-through?
15. Why should we record both requested and measured values?

## Practical Assessment

| Requirement | Points |
|---|---:|
| Understands and implements the Mechanism interface | 10 |
| Correctly uses constructor injection | 10 |
| Understands AdvantageKit IO inputs | 10 |
| Correctly uses the state machine | 10 |
| Implements coroutine-based commands | 15 |
| Understands position and velocity control | 10 |
| Implements duty-cycle control | 10 |
| Correctly records telemetry | 10 |
| Implements safety limits | 5 |
| Successfully tests the mechanism | 10 |
| **Total** | **100** |

### Performance Expectations

- **90–100:** Independently understands and implements the mechanism architecture.
- **80–89:** Understands the architecture and implements most functionality with minor assistance.
- **70–79:** Understands the basic control methods but needs assistance with state management and telemetry.
- **Below 70:** Needs additional practice with IO architecture, commands, and mechanism control.

---

# 27. Instructor Code Review: Improvements to Address

Before using this code as a final student reference, review these implementation details.

### 1. Switch Fall-Through

The duty-cycle cases currently fall through into the position and velocity cases.

Add a `break` after the voltage command to prevent conflicting control requests.

### 2. Pivot Goal Tracking

`setGoalAngle()` exists but is not used by `setAngle()`.

Connect position requests to the goal-tracking method so that the requested angle is properly recorded and clamped.

### 3. Hold Command

`hold(Angle angle)` currently ignores its argument.

Implement actual position holding or rename the method to reflect its idle behavior.

### 4. Simulation Periodic

`simulationPeriodic()` is empty.

Determine whether the simulation updates are already handled by `IntakeIO` or whether additional simulation logic is necessary.

### 5. Mechanism2d Logging

The mechanism visualization is updated but its logging statement is commented out.

Enable the appropriate dashboard or telemetry publishing mechanism if students are expected to visualize it.

### 6. Aggregate Measurements

`getVoltage()` and `getCurrent()` combine roller and pivot measurements.

Consider providing individual accessors for more useful diagnostics.

### 7. Command Requirements

Verify that every command controlling the intake declares the appropriate mechanism requirement.

This is especially important when using multiple commands that can control the roller and pivot.

If independent control of the two motors is desired, consider whether the mechanism should expose separate command requirements or use a more granular command architecture.

### 8. State Machine and Goal Synchronization

Ensure that commands such as `stop()` clear or update the state machine as well as the goal variables.

Otherwise, the next periodic iteration could reapply a previously requested state.

### 9. Unused Code

Review the following items:

- `LOOP_PERIOD_SECONDS` is currently unused.
- `Degrees` is imported but unused.
- The `hold()` angle parameter is unused.
- `setGoalAngle()` is currently unused.

Students should learn to identify unused code and remove or complete it.

### 10. Command Documentation

The `setVelocity()` Javadoc refers to a velocity parameter, but the actual method accepts an `IntakeState`.

Update the documentation to accurately describe the API.

---

# 28. Final Takeaways

At the end of this lesson, students should understand how to build a complete mechanism using the AdvantageKit IO architecture and WPILib 2027 Command V3.

The central concepts are:

- **Mechanism:** Provides a reusable interface for controlling the intake.
- **IO:** Separates hardware and simulation implementations from mechanism logic.
- **State Machine:** Tracks the requested operating state.
- **Commands:** Define the actions that control the mechanism.
- **Coroutines:** Allow commands to execute continuously across scheduler iterations.
- **Units:** Represent physical measurements safely and clearly.
- **Telemetry:** Provides visibility into mechanism performance.
- **Safety:** Prevents the mechanism from operating outside its intended limits.

### The Central Programming Principle

The Intake mechanism should manage the behavior of the intake, not the physical details of individual motor controllers.

The IO layer handles hardware communication, the state machine manages requested operating states, and Command V3 provides the framework for scheduling mechanism actions.

---

# 29. Suggested Follow-Up Lessons

This lesson provides the foundation for several more advanced topics.

Recommended follow-up lessons:

1. **IntakeIO:** Build the AdvantageKit IO interface and define all logged input fields.
2. **IntakeIOReal:** Implement the real Phoenix 6 motor controller and encoder interfaces.
3. **IntakeIOSim:** Simulate the roller and pivot using WPILib simulation models.
4. **IntakeState:** Design a reusable state machine with unit-safe position, velocity, and duty-cycle values.
5. **Command V3:** Explore command composition, priorities, interruption, and coroutine execution.
6. **AdvantageScope:** Analyze intake performance using recorded telemetry.
7. **Mechanism Safety:** Implement software limits, physical limits, and safe-state behavior.
8. **Unit Testing:** Test state transitions, goal tracking, command behavior, and IO implementations.

**Expected Final Outcome:** Students can explain, modify, test, and extend an AdvantageKit-based intake mechanism using WPILib 2027 Command V3, while understanding how mechanism states, commands, IO, simulation, and telemetry work together.