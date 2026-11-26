# Lesson Plan: Creating a Default Teleop OpMode
## WPILib 2027 Alpha 7 — Command V3 and Controller Bindings

---

## 1. Lesson Overview

**Topic:** Teleoperated Robot Control Using OpMode and Command V3

**Framework:** WPILib 2027 Alpha 7

**Programming Language:** Java

**Estimated Duration:** 60–90 minutes

**Target Audience:** FRC students with basic Java and WPILib experience.

### Lesson Description

In this lesson, students will learn how to implement a teleoperated (Teleop) operating mode using the WPILib 2027 OpMode framework.

Students will work with a `DefaultTeleop` class that implements the `OpMode` interface. They will learn how to use the `@Teleop` annotation to register an operating mode, receive the robot instance through constructor injection, and configure controller bindings that operate the robot's shooter and intake mechanisms.

The lesson focuses on combining controller inputs using `and()`, scheduling commands with `whileTrue()`, and understanding the difference between velocity control and duty-cycle control.

By the end of the lesson, students should be able to independently create a Teleop OpMode and map controller inputs to robot mechanisms.

---

## 2. Learning Objectives

By the end of this lesson, students should be able to:

1. Explain the purpose of an OpMode in WPILib 2027.
2. Understand how the `@Teleop` annotation registers an operating mode.
3. Explain how constructor injection provides access to the robot.
4. Identify and use controller inputs through the robot's controller interface.
5. Combine multiple controller inputs using `and()`.
6. Understand how `whileTrue()` schedules and cancels commands.
7. Distinguish between velocity control and duty-cycle control.
8. Map multiple controller combinations to different robot mechanisms.
9. Explain how Command V3 separates operator input from mechanism implementation.
10. Test and troubleshoot controller bindings in simulation and on the robot.

---

## 3. Prerequisites

Students should already understand:

- Basic Java classes, constructors, and interfaces.
- Java method chaining.
- Java enums.
- Lambda expressions and functional interfaces.
- Basic WPILib command-based programming.
- The purpose of robot subsystems/mechanisms.
- How the robot's controller interface exposes button inputs.
- The difference between a motor's velocity and duty cycle.

### Required Existing Robot Components

This lesson assumes the following components already exist:

| Component | Responsibility |
|---|---|
| `Robot` | Provides access to robot mechanisms and controller. |
| `Shooter` | Controls the shooter flywheel. |
| `Intake` | Controls the intake pivot. |
| `IntakeState` | Defines intake positions and duty-cycle values. |
| `Controller` | Provides Xbox-style controller inputs. |

Students should understand that this lesson does not implement the shooter or intake hardware. It connects operator inputs to commands that those mechanisms already provide.

---

# 4. Lesson Structure

| Section | Topic | Time |
|---|---|---:|
| 1 | Introduction to OpModes | 10 min |
| 2 | Understanding the DefaultTeleop class | 10 min |
| 3 | Controller inputs and trigger combinations | 15 min |
| 4 | Shooter control bindings | 15 min |
| 5 | Intake control bindings | 15 min |
| 6 | Testing and troubleshooting | 10 min |
| 7 | Student challenges and review | 15 min |

---

# 5. Instructor Lesson

## Part 1: Introduction to OpModes

### What is an OpMode?

An OpMode defines a particular operating mode for the robot.

Common examples include:

- Autonomous: The robot operates using a predefined routine.
- Teleop: Drivers control the robot using a controller.
- Test: Engineers and programmers test individual robot systems.

WPILib 2027 introduces an OpMode-oriented programming model that allows operating modes to be defined using annotations.

Instead of placing all operator control bindings directly inside `RobotContainer`, we can create dedicated classes for different operating modes.

### Discuss with Students

Ask students:

1. What is the difference between Autonomous and Teleop?
2. Why might we want multiple Teleop modes?
3. Why is it useful to separate driver controls from mechanism implementations?

**Teaching Point:** Separating OpModes from mechanisms allows programmers to change how the robot is controlled without rewriting the underlying mechanism code.

---

## Part 2: Understanding the DefaultTeleop Class

Start by examining the class declaration.

```java
package first.robot.opmodes.teleop;

import first.robot.Robot;
import first.robot.mechanisms.IntakeState;

import static org.wpilib.units.Units.RPM;

import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

@Teleop
public class DefaultTeleop implements OpMode {
```

### Explanation

**Package**

```java
package first.robot.opmodes.teleop;
```

The package organizes this class with other Teleop operating modes.

**Imports**

```java
import first.robot.Robot;
import first.robot.mechanisms.IntakeState;
```

These imports allow the class to access the robot instance and intake state definitions.

```java
import static org.wpilib.units.Units.RPM;
```

This static import allows us to construct velocity values using `RPM.of()` without referencing the entire Units class.

```java
import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;
```

These imports provide the OpMode interface and Teleop annotation.

**Annotation**

```java
@Teleop
```

This annotation identifies the class as a Teleop operating mode for the WPILib OpMode framework.

**Interface**

```java
implements OpMode
```

The class implements the OpMode interface, allowing it to participate in the OpMode framework.

### Student Checkpoint

Have students answer:

1. What does `@Teleop` tell WPILib?
2. Why does this class implement `OpMode`?
3. Why is this class located in the `opmodes.teleop` package?

---

## Part 3: Constructor Injection

Examine the constructor:

```java
public DefaultTeleop(Robot robot) {
```

The OpMode framework creates this operating mode and provides the robot instance through its constructor.

The constructor receives a reference to the existing `Robot` object.

This allows the OpMode to access the robot's mechanisms and controller without creating new instances of them.

For example:

```java
robot.controller
robot.shooter
robot.intake
```

These references provide access to the controller, shooter, and intake.

### Important Concept: Dependency Injection

Constructor injection allows a class to receive the objects it needs instead of constructing those objects itself.

This is important because:

- The robot owns its mechanisms.
- The OpMode uses those existing mechanisms.
- Multiple OpModes can use the same robot mechanisms.
- Hardware initialization remains separate from operator controls.

### Student Exercise

Ask students to explain why the following approach is undesirable:

```java
public DefaultTeleop() {
    Shooter shooter = new Shooter();
}
```

Expected answer:

The OpMode should use the robot's existing shooter instance rather than creating another shooter. Creating another instance could result in duplicate hardware configuration, inconsistent state, or conflicting control.

---

## Part 4: Understanding Controller Inputs

Before implementing the mechanism controls, students need to understand the controller interface.

The robot exposes controller buttons through:

```java
robot.controller
```

The following methods represent the Xbox-style face buttons:

| Method | Xbox Button | Physical Position |
|---|---|---|
| `faceDown()` | A | Bottom |
| `faceRight()` | B | Right |
| `faceLeft()` | X | Left |
| `faceUp()` | Y | Top |
| `leftBumper()` | LB | Left shoulder |
| `rightBumper()` | RB | Right shoulder |

### Understanding Trigger Combinations

Consider:

```java
robot.controller.faceDown().and(robot.controller.leftBumper())
```

This combines two controller inputs.

The resulting trigger becomes active when both conditions are true:

- The A button is pressed.
- The left bumper is held.

The `and()` method creates a logical AND relationship.

| A Button | Left Bumper | Combined Trigger |
|---|---|---|
| Released | Released | False |
| Pressed | Released | False |
| Released | Pressed | False |
| Pressed | Pressed | True |

### Student Checkpoint

Ask students:

1. What does `and()` accomplish?
2. Why might we use bumpers as modifier buttons?
3. How could we prevent a mechanism from activating accidentally?

**Teaching Point:** Modifier buttons allow us to assign multiple functions to the same face buttons without requiring additional controller buttons.

---

# 6. Shooter Control Bindings

The first mechanism students will control is the shooter.

The shooter supports two different types of motor control:

1. Velocity control.
2. Duty-cycle control.

## Part 5A: Velocity Control

Examine the following code:

```java
robot.controller
    .faceDown().and(robot.controller.leftBumper())
    .whileTrue(robot.shooter.setVelocity(RPM.of(2330.0)));

robot.controller
    .faceRight().and(robot.controller.leftBumper())
    .whileTrue(robot.shooter.setVelocity(RPM.of(4650.0)));
```

### Explanation

The first binding activates the shooter at 2330 RPM when the driver holds:

- A button.
- Left bumper.

The second binding activates the shooter at 4650 RPM when the driver holds:

- B button.
- Left bumper.

### Understanding RPM.of()

```java
RPM.of(2330.0)
```

This creates a unit-safe rotational velocity value.

Instead of passing an untyped number, we explicitly identify the value as revolutions per minute.

This helps prevent mistakes involving different measurement units.

### Velocity Control vs. Duty-Cycle Control

Velocity control attempts to maintain a requested motor speed.

For example:

```java
robot.shooter.setVelocity(RPM.of(2330.0))
```

The motor controller uses feedback to reach and maintain the requested speed.

This is useful when the shooter needs consistent wheel speed regardless of changes in battery voltage or mechanical load.

### Student Exercise

Have students change the shooter presets:

| Button Combination | Original Speed | New Speed |
|---|---:|---:|
| A + LB | 2330 RPM | 2500 RPM |
| B + LB | 4650 RPM | 5000 RPM |

Students should identify the exact locations where the changes must be made.

---

## Part 5B: Duty-Cycle Control

Examine the following code:

```java
robot.controller.faceLeft()
    .and(robot.controller.leftBumper())
    .whileTrue(robot.shooter.set(0.3));

robot.controller.faceUp()
    .and(robot.controller.leftBumper())
    .whileTrue(robot.shooter.set(-0.3));
```

### Explanation

These bindings control the shooter using a duty cycle rather than a velocity target.

The first binding applies:

```java
robot.shooter.set(0.3)
```

The second binding applies:

```java
robot.shooter.set(-0.3)
```

A duty cycle represents the requested motor output as a fraction of its available output range.

| Duty Cycle | Meaning |
|---:|---|
| `1.0` | 100% positive output |
| `0.5` | 50% positive output |
| `0.3` | 30% positive output |
| `0.0` | Zero output |
| `-0.3` | 30% reverse output |
| `-1.0` | 100% reverse output |

**Important:** Duty cycle does not represent a specific RPM. Actual motor speed depends on battery voltage, mechanical load, motor characteristics, and other conditions.

### Student Checkpoint

Ask students:

1. What is the difference between `setVelocity()` and `set()`?
2. Why might we want direct duty-cycle control?
3. What happens when the duty cycle is negative?
4. Does a duty cycle of 0.5 guarantee 50% of the motor's maximum RPM?

---

# 7. Intake Control Bindings

The next mechanism is the intake pivot.

The intake has two types of control:

1. Position-based control.
2. Duty-cycle control.

## Part 6A: Position-Based Control

Examine:

```java
robot.controller
    .faceDown().and(robot.controller.rightBumper())
    .whileTrue(robot.intake.setAngle(IntakeState.REVERSE));

robot.controller
    .faceRight().and(robot.controller.rightBumper())
    .whileTrue(robot.intake.setAngle(IntakeState.FORWARD));
```

### Explanation

The intake mechanism is controlled using predefined states.

`IntakeState.REVERSE` and `IntakeState.FORWARD` represent positions defined by the intake mechanism's state configuration.

When the driver holds:

- A + RB: The intake moves toward its reverse position.
- B + RB: The intake moves toward its forward position.

### Why Use an Enum?

Using an enum provides meaningful names for predefined mechanism states.

Compare:

```java
robot.intake.setAngle(IntakeState.FORWARD);
```

With:

```java
robot.intake.setAngle(0.75);
```

The enum communicates the intended behavior without requiring the programmer to remember what a numeric value represents.

It also centralizes mechanism-specific position definitions.

### Student Exercise

Ask students to locate the `IntakeState` enum and identify:

- Where the forward position is defined.
- Where the reverse position is defined.
- How the intake mechanism translates these states into physical movement.

---

## Part 6B: Intake Duty-Cycle Control

Examine:

```java
robot.controller.faceLeft()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.setPivotDutyCycle(
            IntakeState.FORWARDDUTYCYCLE));

robot.controller.faceUp()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.setPivotDutyCycle(
            IntakeState.REVERSEDUTYCYCLE));
```

### Explanation

These bindings allow the driver to manually operate the intake pivot.

Instead of requesting a predefined position, the driver directly controls motor output.

The two values are defined by the `IntakeState` enum:

- `FORWARDDUTYCYCLE`: Drives the pivot in the forward direction.
- `REVERSEDUTYCYCLE`: Drives the pivot in the reverse direction.

This is particularly useful for:

- Manually positioning the intake.
- Testing motor direction.
- Recovering from unexpected mechanism positions.
- Diagnosing mechanical or sensor issues.

### Student Checkpoint

Ask students:

1. Why might manual duty-cycle control be useful?
2. What is the difference between a position command and a duty-cycle command?
3. Where should the forward and reverse duty-cycle values be defined?
4. Why should duty-cycle values be limited to safe operating ranges?

---

# 8. Understanding whileTrue()

All of the controller bindings use:

```java
.whileTrue(command)
```

This is an important part of Command V3.

`whileTrue()` associates a command with a trigger.

The command is scheduled when the trigger becomes active and canceled when the trigger becomes inactive.

Conceptually:

```text
Button pressed
      |
      v
Trigger becomes TRUE
      |
      v
Command is scheduled
      |
      v
Button remains pressed
      |
      v
Command continues running
      |
      v
Button released
      |
      v
Trigger becomes FALSE
      |
      v
Command is canceled
```

### Important Command Behavior

Students should understand that `whileTrue()` is not simply a Java while loop.

It does not continuously create new commands every iteration.

It uses the command scheduler and trigger lifecycle to manage command execution.

Also, a command that finishes naturally while its trigger remains active is not necessarily rescheduled until the trigger becomes inactive and active again. Students should verify the exact Command V3 behavior for the commands they implement.

### Mechanism Requirements

Commands that control a mechanism should declare the appropriate mechanism requirement.

For example:

- Shooter commands should require the shooter.
- Intake commands should require the intake.

This prevents conflicting commands from simultaneously controlling the same mechanism.

If two commands requiring the same mechanism are scheduled together, the command scheduler resolves the conflict according to its scheduling rules.

### Instructor Demonstration

Demonstrate the following:

1. Hold A + LB.
2. Observe the shooter targeting 2330 RPM.
3. Release A.
4. Observe the command cancellation behavior.
5. Hold B + LB.
6. Observe the shooter targeting 4650 RPM.
7. Repeat the process with the intake.

---

# 9. Complete Controller Mapping

Have students construct the following mapping table.

| Modifier | Face Button | Mechanism | Action |
|---|---|---|---|
| LB | A | Shooter | 2330 RPM |
| LB | B | Shooter | 4650 RPM |
| LB | X | Shooter | 30% forward duty cycle |
| LB | Y | Shooter | 30% reverse duty cycle |
| RB | A | Intake | Reverse position |
| RB | B | Intake | Forward position |
| RB | X | Intake | Forward duty cycle |
| RB | Y | Intake | Reverse duty cycle |

### Discuss the Design

This controller layout uses the bumpers as mechanism selectors.

- Left bumper selects shooter controls.
- Right bumper selects intake controls.
- Face buttons select the action.

This creates a consistent control scheme and allows eight separate actions using only six physical buttons.

---

# 10. Testing and Troubleshooting

Testing should be performed in stages.

## Stage 1: Code Review

Before enabling the robot, verify:

- [ ] The class implements `OpMode`.
- [ ] The class has the `@Teleop` annotation.
- [ ] The constructor receives the `Robot` instance.
- [ ] All controller bindings reference the correct buttons.
- [ ] Shooter commands use the correct units.
- [ ] Intake commands reference the correct states.
- [ ] Commands have appropriate mechanism requirements.
- [ ] Mechanisms stop or enter a safe state when commands are canceled.

## Stage 2: Simulation

Test the bindings in simulation.

Verify:

- The correct commands are scheduled.
- The shooter receives the expected velocity targets.
- The shooter responds correctly to duty-cycle commands.
- The intake receives the correct position requests.
- The intake responds correctly to manual duty-cycle commands.
- Releasing the buttons produces the expected command cancellation behavior.

Use AdvantageKit logging and AdvantageScope to investigate:

- Shooter target velocity.
- Shooter measured velocity.
- Shooter applied voltage.
- Intake target angle.
- Intake measured angle.
- Intake applied voltage.
- Command scheduling and cancellation.

## Stage 3: Real Robot

**Safety requirements:**

- Keep the robot securely supported when testing mechanisms that could cause movement.
- Ensure the shooter is clear of people and objects.
- Keep hands away from moving mechanisms.
- Verify mechanical limits before enabling pivot movement.
- Begin with conservative motor outputs.
- Have an instructor supervise testing.

Test one mechanism at a time before testing simultaneous operation.

---

# 11. Student Programming Challenges

## Challenge 1: Add a New Shooter Preset

Add a third shooter velocity preset.

Requirements:

- Use the left bumper.
- Use the Y button.
- Set the target velocity to 3500 RPM.

Questions:

1. Which existing binding would you modify or extend?
2. Does this conflict with an existing binding?
3. What happens if two shooter commands are requested simultaneously?

## Challenge 2: Add a New Intake Control

Create a new controller binding that moves the intake to an additional predefined state.

Requirements:

- Use the right bumper.
- Select an available face button.
- Use `setAngle()`.

Students must explain how the new state is defined in `IntakeState`.

## Challenge 3: Change Duty-Cycle Values

Modify the shooter manual controls:

- Forward: 50%.
- Reverse: 20%.

Students must identify how positive and negative values affect motor direction.

## Challenge 4: Create a Different Teleop Mode

Create another class named:

`PracticeTeleop`

Requirements:

- Implement `OpMode`.
- Use the `@Teleop` annotation.
- Receive the `Robot` instance through the constructor.
- Configure at least two controller bindings.
- Use both shooter and intake mechanisms.

Students should explain why a separate OpMode is useful for testing different driver control layouts.

## Challenge 5: Add a Safety Improvement

Design an improvement to prevent unsafe mechanism operation.

Possible approaches:

- Add software limits to intake movement.
- Prevent simultaneous conflicting intake commands.
- Add a reduced-power manual mode.
- Add a driver override.
- Ensure motors stop when the operating mode is disabled.

Students must identify whether the safety behavior belongs in the OpMode, mechanism, or IO implementation and justify their decision.

---

# 12. Assessment

## Knowledge Assessment

Students should be able to answer:

1. What is an OpMode?
2. What does the `@Teleop` annotation do?
3. Why does the constructor receive a `Robot` object?
4. What does `and()` do?
5. What does `whileTrue()` do?
6. What is the difference between velocity control and duty-cycle control?
7. Why do we use units such as `RPM.of()`?
8. Why are enums useful for mechanism states?
9. What happens when two commands require the same mechanism?
10. Why should mechanism safety logic not exist exclusively in the Teleop class?

## Practical Assessment

| Requirement | Points |
|---|---:|
| Correctly implements the OpMode interface | 10 |
| Correctly registers using `@Teleop` | 10 |
| Correctly receives the Robot instance | 10 |
| Creates controller trigger combinations | 15 |
| Implements shooter velocity controls | 15 |
| Implements shooter duty-cycle controls | 10 |
| Implements intake position controls | 10 |
| Implements intake duty-cycle controls | 10 |
| Successfully tests the bindings | 10 |
| **Total** | **100** |

### Performance Expectations

- **90–100:** Independently implements and explains all bindings.
- **80–89:** Implements the majority of bindings with minor assistance.
- **70–79:** Understands the controller mappings but requires assistance with command configuration.
- **Below 70:** Needs additional practice with triggers, commands, and mechanism interfaces.

---

# 13. Instructor Review: Important Code Considerations

Before using this example as a final student exercise, review the following items.

### 1. Conflicting Commands

Multiple buttons can be pressed simultaneously.

For example:

- A + LB requests shooter velocity control.
- X + LB requests shooter duty-cycle control.

Both commands require the shooter, so the command scheduler must resolve the conflict.

Students should understand this behavior and should not assume that multiple commands can control the same mechanism simultaneously.

### 2. Command Cancellation and Motor Output

Canceling a command does not automatically guarantee that a motor stops.

The mechanism's command implementation must define the appropriate behavior when interrupted.

For example, a command may need to explicitly stop the motor or hold its current position when canceled.

### 3. Mechanism Safety

Safety constraints should be implemented as close to the mechanism as practical.

The OpMode should determine what the driver wants to do. The mechanism should enforce safe operating limits.

### 4. Constructor Lifecycle

The OpMode framework manages the creation and lifecycle of the operating mode.

Students should not manually instantiate `DefaultTeleop` or create additional robot instances.

### 5. Controller Layout

The example uses the left bumper for shooter control and the right bumper for intake control.

This is a design choice rather than a WPILib requirement. Teams can establish their own controller layouts as long as they remain consistent and are documented.

---

# 14. Final Takeaways

At the end of this lesson, students should understand the relationship between the robot, its operating modes, controller inputs, and mechanisms.

The architecture can be summarized as:

```text
             OpModeRobot
                  |
                  v
                Robot
                  |
                  v
             DefaultTeleop
                  |
          +-------+-------+
          |               |
          v               v
       Controller      Mechanisms
          |               |
          |          +----+----+
          |          |         |
          v          v         v
       Triggers    Shooter   Intake
          |          |         |
          v          v         v
       Commands   Shooter   Intake
                  Commands  Commands
```

Students should leave understanding that:

- OpModes define how the robot operates.
- Controller triggers define when actions should occur.
- Commands define what actions should happen.
- Mechanisms define how robot systems behave.
- IO implementations handle the interaction with hardware and simulation.

**The central programming principle:** The Teleop OpMode should describe driver intent, not contain the underlying hardware control logic.

---

## 15. Suggested Follow-Up Lessons

This lesson provides the foundation for more advanced operator control.

Recommended follow-up topics:

1. **Advanced Controller Mapping:** Create reusable controller binding methods and organize controls by mechanism.
2. **Command V3:** Explore command composition, sequential commands, parallel commands, and command interruption.
3. **Mechanism Safety:** Implement limits, interlocks, and safe-state behavior.
4. **Multiple OpModes:** Create competition, practice, and testing operating modes.
5. **AdvantageKit Telemetry:** Record controller inputs, command states, and mechanism targets.
6. **Simulation:** Verify complete Teleop behavior without requiring physical robot hardware.

**Expected Final Outcome:** Students can create a WPILib 2027 Teleop OpMode, configure controller bindings, and operate multiple robot mechanisms using Command V3 while maintaining a clean separation between operator controls and mechanism implementations.