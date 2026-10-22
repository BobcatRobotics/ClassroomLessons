# Exercise 1: Default Teleop Bindings

## Objective

Understand and verify how controller buttons are bound to shooter commands during teleoperated operation.

You will learn how to:

- Read an `OpMode` class that is constructed by the framework.
- Access the controller and shooter through the `Robot` instance.
- Bind buttons with `whileTrue(...)`.
- Use WPILib unit types (`RPM`) to describe a target velocity.

## Background

Open `DefaultTeleop.java`.

```java
@Teleop
public class DefaultTeleop implements OpMode {
  public DefaultTeleop(Robot robot) {
    robot.controller.faceDown().whileTrue(robot.shooter.setVelocity(RPM.of(2330.0)));
    robot.controller.faceRight().whileTrue(robot.shooter.setVelocity(RPM.of(4650.0)));
    robot.controller.faceLeft().whileTrue(robot.shooter.set(0.3));
    robot.controller.faceUp().whileTrue(robot.shooter.set(-0.3));
  }
}
```

### 1. The `@Teleop` annotation and `OpMode`

`@Teleop` registers the class as a teleoperated mode. The framework creates the class automatically at startup and passes it the `Robot` instance. All bindings are created once, in the constructor.

**Task:** Keep both the annotation and `implements OpMode` when you edit this class.

### 2. The `Robot` parameter

The constructor does not create its own controller or shooter. It uses `robot.controller` and `robot.shooter`, so every mode shares the same hardware objects.

**Task:** Open `Robot.java` and find where `controller` and `shooter` are declared. Note which `ShooterIO` implementation (`ShooterIOReal` or `ShooterIOSim`) is passed to the shooter, and how that choice is made.

### 3. Button selection versus binding

| Method | Button |
|---|---|
| `faceDown()` | Bottom (A) |
| `faceRight()` | Right (B) |
| `faceLeft()` | Left (X) |
| `faceUp()` | Top (Y) |

Each of these returns a `Trigger`. Selecting a button gives you the trigger, and calling a binding method on that trigger attaches a command.

### 4. `whileTrue(...)`

`whileTrue(command)` schedules the command when the trigger becomes active and cancels it when the trigger becomes inactive.

This fits a shooter. Both `setVelocity(...)` and `set(...)` loop forever (`while (true)`), so they only stop when something interrupts them. Releasing the button cancels the command, and the shooter falls back to the lowest-priority `idle()` command.

> `onTrue(...)` schedules a command only when the button is first pressed. It would not cancel the command on release, which would leave the flywheel spinning.

### 5. Velocity commands

```java
robot.shooter.setVelocity(RPM.of(2330.0))
```

`setVelocity` expects an `AngularVelocity`. `RPM.of(2330.0)` builds one, and `Shooter` converts it to rotations per second before it reaches the IO layer. That is 2330 RPM, about 38.8 rotations per second.

### 6. Duty-cycle commands

```java
robot.shooter.set(0.3)    // forward
robot.shooter.set(-0.3)   // reverse
```

`set(double)` takes a plain `double` from -1.0 to 1.0. It is open loop, so the flywheel speed depends on load and battery voltage.

## Tasks

1. Trace each binding and write down the button, command, and expected flywheel behavior.
2. In `Robot.java`, find where `DefaultTeleop` can reach `robot.shooter`, and confirm it is constructed with the correct IO implementation.
3. Add a fifth binding of your choice, for example `faceLeftShoulder()` if your controller supports it, running `RPM.of(3000.0)`. Verify it in simulation.
4. Clean up: `Meters` is imported but never used. Remove it, then build to confirm nothing breaks.

## Completion checklist

- [ ] The class still implements `OpMode` and keeps `@Teleop`.
- [ ] The existing `Robot` instance is used, with no new controller or shooter created.
- [ ] A and B run closed-loop velocity commands at 2330 and 4650 RPM.
- [ ] X and Y run -/+ 30% duty-cycle commands.
- [ ] Every binding uses `whileTrue(...)`.
- [ ] Unused imports are removed.
- [ ] The code compiles and passes the project's applicable checks.

## Reflection questions

1. Why does the constructor receive a `Robot` instance?
2. What is the difference between selecting a button and binding a command to it?
3. What would happen if you used `onTrue(...)` instead of `whileTrue(...)` for `setVelocity(...)`?
4. Two buttons are held at once, for example A and B. Both commands require the shooter. Which one runs? (Hint: read about command priority and the lowest-priority `idle()` in Exercise 2.)
5. What advantage do unit types such as `RPM.of(...)` have over an unqualified `double`?
