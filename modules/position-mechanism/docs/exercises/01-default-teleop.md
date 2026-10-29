# Lesson: Creating a Default Teleop OpMode

## Objective

In this lesson, you will implement the robot's default teleoperated control mode using WPILib 2027's OpMode framework and Command V3.

By the end of this lesson, you should understand how to:

- Register a teleoperated OpMode using `@Teleop`.
- Implement the `OpMode` interface.
- Access robot mechanisms and controller inputs through the `Robot` instance.
- Combine controller triggers to create modifier-button controls.
- Bind commands to buttons using `whileTrue()`.
- Control a shooter using velocity and duty-cycle commands.
- Control an intake mechanism using position and duty-cycle commands.
- Use WPILib units to express physical quantities safely.

## Prerequisites

Before starting, you should be familiar with:

- Java classes, constructors, and interfaces.
- Lambda expressions and method calls.
- The Command-based programming model.
- WPILib units, including `Degrees`, `RPM`, and `Meters`.
- The robot's `Robot`, controller, shooter, and intake classes.
- The difference between closed-loop velocity control and open-loop duty-cycle control.

## Step 1: Create the OpMode Class

Create the file:

`src/main/java/first/robot/opmodes/teleop/DefaultTeleop.java`

Start with the following skeleton:

```java
package first.robot.opmodes.teleop;

import first.robot.Robot;
import org.wpilib.opmode.OpMode;
import org.wpilib.opmode.Teleop;

@Teleop
public class DefaultTeleop implements OpMode {

  public DefaultTeleop(Robot robot) {
    // Configure controller bindings here.
  }
}
```

### Your task

1. Place the class in the correct package.
2. Add the `@Teleop` annotation.
3. Implement the `OpMode` interface.
4. Add a constructor that accepts a `Robot` instance.

### Explanation

- `@Teleop` identifies this class as a teleoperated OpMode for the OpMode framework to discover.
- `implements OpMode` declares that the class follows the framework's OpMode contract.
- `DefaultTeleop(Robot robot)` receives access to the robot's mechanisms and controller.
- The framework constructs the OpMode when it initializes the available modes, according to the framework's lifecycle.

The constructor is where you configure the controller bindings. You do not need to create another `Robot` instance.

## Step 2: Import WPILib Units

Add the following imports:

```java
import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.RPM;
```

### Your task

Use the unit factories to represent:

1. A shooter velocity of 2330 RPM.
2. A shooter velocity of 4650 RPM.
3. An intake angle of 0 degrees.
4. An intake angle of 45 degrees.

### Explanation

WPILib units make the intended physical quantity explicit.

For example:

```java
RPM.of(2330.0)
Degrees.of(45.0)
```

These expressions create unit-aware values rather than passing unqualified numeric literals throughout your mechanism code.

Use the unit expected by each mechanism method. A velocity command should receive a velocity, while an angle command should receive an angle.

## Step 3: Configure Shooter Velocity Control

The shooter should support two preset speeds.

| Controller input | Shooter behavior |
|---|---|
| Hold left bumper + bottom face button (A) | Set velocity to 2330 RPM |
| Hold left bumper + right face button (B) | Set velocity to 4650 RPM |

Complete the following bindings:

```java
robot.controller
    .faceDown()
    .and(robot.controller.leftBumper())
    .whileTrue(
        robot.shooter.setVelocity(
            /* TODO: 2330 RPM */
        )
    );

robot.controller
    .faceRight()
    .and(robot.controller.leftBumper())
    .whileTrue(
        robot.shooter.setVelocity(
            /* TODO: 4650 RPM */
        )
    );
```

### Your task

- Replace both TODOs with unit-aware velocity values.
- Explain why each binding combines two controller triggers.
- Explain what happens when the combined trigger becomes active.

### Explanation

The `and()` method combines two trigger conditions. The associated command is eligible to run while both the face button and the left bumper are held.

The `whileTrue()` method binds a command to the combined trigger's active state. The command's behavior when the trigger becomes inactive depends on its implementation and the command framework's lifecycle rules.

The `setVelocity()` method represents velocity control. Depending on the implementation of `Shooter`, the mechanism may use feedback control to reach and maintain the requested flywheel speed.

## Step 4: Configure Shooter Duty-Cycle Control

The shooter should also support direct duty-cycle commands.

| Controller input | Shooter behavior |
|---|---|
| Hold left bumper + left face button (X) | Apply a duty-cycle request of `0.3` |
| Hold left bumper + top face button (Y) | Apply a duty-cycle request of `-0.3` |

Complete the following bindings:

```java
robot.controller
    .faceLeft()
    .and(robot.controller.leftBumper())
    .whileTrue(
        robot.shooter.set(/* TODO: 0.3 */)
    );

robot.controller
    .faceUp()
    .and(robot.controller.leftBumper())
    .whileTrue(
        robot.shooter.set(/* TODO: -0.3 */)
    );
```

### Your task

1. Complete the two duty-cycle values.
2. Explain the difference between positive and negative duty-cycle requests.
3. Explain why duty-cycle control differs from velocity control.

### Explanation

A duty-cycle request typically ranges from `-1.0` to `1.0`:

- `0.0` represents zero commanded output.
- `0.3` requests a positive 30% output.
- `-0.3` requests a negative 30% output.

The actual motor direction depends on motor configuration, inversion, and mechanism wiring.

Unlike velocity control, a duty-cycle command does not inherently request a particular RPM. The resulting speed depends on the motor, load, battery voltage, and other conditions.

## Step 5: Configure Intake Position Control

The intake should support two preset angles.

| Controller input | Intake behavior |
|---|---|
| Hold right bumper + bottom face button (A) | Set the intake angle to 0 degrees |
| Hold right bumper + right face button (B) | Set the intake angle to 45 degrees |

Complete the bindings:

```java
robot.controller
    .faceDown()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.setAngle(
            /* TODO: 0 degrees */
        )
    );

robot.controller
    .faceRight()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.setAngle(
            /* TODO: 45 degrees */
        )
    );
```

### Your task

- Use the `Degrees` unit factory for both angles.
- Explain why the intake uses `setAngle()` rather than the shooter's `setVelocity()` method.
- Identify which class is responsible for implementing the actual motor and sensor behavior.

### Explanation

The intake is being controlled by a requested angular position, while the shooter is being controlled by rotational velocity.

The OpMode specifies the desired behavior. The intake mechanism implementation is responsible for translating that request into the appropriate motor or position-control behavior.

Whether `setAngle()` repeatedly commands a position, finishes after reaching a target, or requires a particular trigger lifecycle depends on how the intake command is implemented.

## Step 6: Configure Intake Duty-Cycle Control

The intake should also support direct motor output.

| Controller input | Intake behavior |
|---|---|
| Hold right bumper + left face button (X) | Apply a duty-cycle request of `0.3` |
| Hold right bumper + top face button (Y) | Apply a duty-cycle request of `-0.3` |

Complete the following bindings:

```java
robot.controller
    .faceLeft()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.set(/* TODO: 0.3 */)
    );

robot.controller
    .faceUp()
    .and(robot.controller.rightBumper())
    .whileTrue(
        robot.intake.set(/* TODO: -0.3 */)
    );
```

### Your task

Complete the two commands and explain when direct duty-cycle control is useful during mechanism testing.

### Explanation

Direct output control can help verify motor direction, inspect mechanism movement, and perform basic hardware tests.

Use it cautiously. A duty-cycle command can continue requesting output while its trigger remains active. Ensure that the mechanism has appropriate limits, safety interlocks, and stopping behavior.

## Step 7: Understand the Controller Layout

The completed OpMode uses the two bumpers as modifiers.

| Modifier | Face button | Mechanism | Operation |
|---|---|---|---|
| Left bumper | A | Shooter | 2330 RPM |
| Left bumper | B | Shooter | 4650 RPM |
| Left bumper | X | Shooter | +0.3 duty cycle |
| Left bumper | Y | Shooter | -0.3 duty cycle |
| Right bumper | A | Intake | 0-degree target |
| Right bumper | B | Intake | 45-degree target |
| Right bumper | X | Intake | +0.3 duty cycle |
| Right bumper | Y | Intake | -0.3 duty cycle |

This layout lets the same four face buttons control two different mechanisms without assigning each operation a separate button.

## Step 8: Verify the Completed Implementation

Before running the robot, check the following:

- [ ] The class is annotated with `@Teleop`.
- [ ] The class implements `OpMode`.
- [ ] The constructor accepts the existing `Robot` instance.
- [ ] All eight controller bindings are present.
- [ ] Each binding combines the correct face button and bumper.
- [ ] Shooter velocity requests use `RPM.of(...)`.
- [ ] Intake angle requests use `Degrees.of(...)`.
- [ ] Duty-cycle requests stay within `-1.0` and `1.0`.
- [ ] The shooter and intake command methods have the expected behavior.
- [ ] Commands stop or transition safely when their triggers become inactive.
- [ ] The controls are tested in simulation or under appropriate supervised testing conditions.

## Challenge Questions

1. Why does `DefaultTeleop` receive a `Robot` object instead of constructing one?
2. What is the purpose of the `@Teleop` annotation?
3. What does `triggerA.and(triggerB)` mean?
4. How does `whileTrue()` differ conceptually from a one-time button-press binding?
5. Why might a flywheel use velocity control while an intake arm uses position control?
6. What would happen if you changed the shooter's duty-cycle request from `0.3` to `0.8`?
7. Why should motor direction and stopping behavior be verified before operating a mechanism on the actual robot?
8. What changes would you make if you wanted the shooter to keep running at its last requested speed after releasing the bumper?

## Expected Outcome

After completing this lesson, you should have a working `DefaultTeleop` OpMode that registers with the framework and maps controller inputs to shooter and intake commands.

The important design principle is that the OpMode defines **operator intent**, while the mechanism classes define **how the robot achieves that intent**.
