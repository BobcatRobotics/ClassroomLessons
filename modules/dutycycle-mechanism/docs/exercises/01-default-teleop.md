# Exercise 1: Creating a Default Teleop Binding

## Objective

Configure a controller button to activate the robot's shooter during teleoperated operation.

By completing this exercise, you will learn how to:

- Read and understand an existing command-based Java implementation.
- Access a controller through the robot container.
- Bind a controller button to a command.
- Pass a target distance to a subsystem using a Java lambda expression.
- Use WPILib unit types to represent physical quantities.

## Background

Open `DefaultTeleop.java` and examine the `DefaultTeleop` class.

### 1. Understand the `@Teleop` annotation

The class is annotated with `@Teleop`. This identifies it as a teleoperated operation mode to the project's OpMode framework.

**Your task:** Keep the annotation and implement the constructor without changing the class's role.

### 2. Understand the `Robot` parameter

The constructor receives a `Robot` instance. This gives the operation mode access to the robot's configured components.

Inspect `Robot.java` to determine:

- How the controller is accessed.
- How the shooter subsystem is accessed.
- What types of objects those fields expose.

**Your task:** Use the existing robot instance instead of creating another controller or shooter.

### 3. Select the controller button

The controller API provides methods for selecting buttons, including `faceUp()`.

**Your task:** Select the face-up button on the configured controller.

Consider what object the button-selection method returns and which methods are available on that object.

### 4. Configure the button binding

The command-based API provides `onTrue(...)` for scheduling a command when its associated trigger changes to the active state.

**Your task:** Configure the selected button to schedule the shooter command when pressed.

Consult the project's command framework documentation if you are unsure about the expected argument type.

### 5. Create the shooter command

The shooter subsystem exposes a `set(...)` method.

**Your task:** Call this method on the existing shooter subsystem and provide the requested target distance.

Read the method declaration in the shooter implementation to determine what type of argument it expects.

### 6. Specify the dutyCycle output

The project uses WPILib's strongly typed units API. The static import of `Meters` allows you to construct a distance without repeatedly qualifying the class name.

For example, `Meters.of(1)` represents a distance of one meter.

**Your task:** Supply that dutyCycle expected by `set(...)`. Pay attention to whether the method expects a value directly or a `double` that provides the value.

## Completion checklist

- [ ] The class still implements `OpMode`.
- [ ] The `@Teleop` annotation is preserved.
- [ ] The existing `Robot` instance is used.
- [ ] The face-up controller button is selected.
- [ ] The button schedules a command when pressed.
- [ ] The shooter command receives a target distance of one meter.
- [ ] The code compiles and passes the project's applicable checks.

## Reflection questions

1. Why does the constructor receive a `Robot` instance?
2. What is the difference between selecting a button and binding a command to it?
3. What does `onTrue(...)` do?
4. Why might a subsystem method accept a `double dutyCycle` ?
