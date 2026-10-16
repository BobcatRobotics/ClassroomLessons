# Exercise 3: Defining the Shooter I/O Interface

## Objective

Understand how a Java interface separates mechanism logic from hardware-specific implementations.

By completing this exercise, you will learn how to:

- Define and use Java interfaces.
- Understand default methods.
- Represent sensor measurements in a data class.
- Use `Supplier<Double>` to represent a requested motor output.
- Understand how AdvantageKit's `@AutoLog` annotation supports telemetry.
- Explain how real hardware, simulation, and unit tests can share the same interface.

## Background

The `ShooterIO` interface defines the operations available to the shooter mechanism without requiring it to know which hardware implementation provides them.

For example, the mechanism can request an idle command without needing to know how a particular motor controller implements that behavior.

Before modifying the file, inspect `Shooter.java` and identify where it calls each method in `ShooterIO`.

## Task 1: Understand the interface contract

Examine the declaration:

`public interface ShooterIO`

**Your tasks:**

1. Explain why this type is an interface rather than a concrete class.
2. Identify the methods a shooter I/O implementation can provide.
3. Find the real hardware implementation and determine how it implements this interface.
4. Explain how another implementation could simulate the shooter without using physical motor controllers.

Do not add hardware-specific dependencies to this interface.

## Task 2: Understand the input structure

The nested `ShooterIOInputs` class stores measurements from the shooter.

It currently contains two fields:

- `velocityOfPrimaryRotPerSecond`
- `velocityOfSecondaryRotPerSecond`

Both are represented as `double` values, with rotational velocity expressed in rotations per second.

**Your tasks:**

1. Explain what each field represents.
2. Identify which implementation is responsible for populating these fields.
3. Explain why the fields have initial values of `0.0`.
4. Describe how the mechanism uses the input structure during periodic processing.

Do not rename the fields unless you also update all dependent code and generated logging references.

## Task 3: Understand `@AutoLog`

The `@AutoLog` annotation is used by AdvantageKit to generate an automatically logged input class.

The mechanism uses a generated type named `ShooterIOInputsAutoLogged`.

**Your tasks:**

1. Explain the purpose of the annotation.
2. Find where the generated class is used.
3. Explain why the generated class should not be manually implemented as a replacement for the annotation.
4. Verify that the project's annotation processing and build configuration generate the expected class.

## Task 4: Understand default methods

The interface defines three default methods:

- `updateInputs(...)`
- `idle()`
- `runFlywheelDutyOutput(...)`

A default method provides an implementation that a class can inherit or override.

**Your tasks:**

1. Explain the difference between an abstract interface method and a default method.
2. Determine why `updateInputs(...)` can have an empty default implementation.
3. Explain the consequences of `idle()` returning `null`.
4. Explain the consequences of `runFlywheelDutyOutput(...)` returning `null`.
5. Inspect the concrete implementation and determine which methods it overrides.

**Important:** These defaults are placeholders for implementations that do not provide hardware behavior. A mechanism must not attempt to schedule or await a `null` command. If you choose to change the defaults, explain your design decision and update the relevant tests.

## Task 5: Understand the duty-cycle supplier

The method `runFlywheelDutyOutput(...)` accepts a `Supplier<Double>`.

The supplied value represents a requested motor output, normally between `-1.0` and `1.0`.

**Your tasks:**

1. Explain what `Supplier<Double>` represents.
2. Describe how an implementation can obtain the current requested output.
3. Explain why a supplier can be useful when a command runs repeatedly.
4. Identify where the mechanism creates and passes this supplier.

Do not confuse duty-cycle output with rotational velocity. A duty-cycle request is a normalized motor command, not a target measured in rotations per second.

## Task 6: Verify the interface

Review the interface and its implementations.

- [ ] The input structure contains both flywheel measurements.
- [ ] The units of the velocity fields are documented.
- [ ] The interface has no unnecessary hardware-specific dependencies.
- [ ] The real hardware implementation implements the expected methods.
- [ ] Simulation and test implementations can use the same interface.
- [ ] No command that is required at runtime is unexpectedly `null`.
- [ ] The project compiles with the expected AdvantageKit-generated input class.

## Reflection questions

1. What problem does hardware abstraction solve?
2. Why can a real implementation and a fake test implementation expose the same interface?
3. Why does `updateInputs(...)` receive an object instead of returning two separate measurements?
4. What is the difference between sensor feedback and a motor command?
5. What could happen if an implementation never updates its input fields?
6. Why is it important to make missing command implementations obvious during development?