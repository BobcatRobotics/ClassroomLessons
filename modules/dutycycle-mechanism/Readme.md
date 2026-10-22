# Shooter Mechanism Implementation Lab

Welcome to the Shooter Mechanism Implementation Lab! In this exercise, you will complete a partially implemented Java shooter system using WPILib-style command-based programming, Command V3, AdvantageKit, and CTRE Phoenix 6.

The project is divided into four exercises. Each exercise focuses on a different layer of the robot software architecture, allowing you to build an understanding of how operator controls, commands, mechanisms, and hardware work together.

## Learning Objectives

By completing this lab, you will learn how to:

- Bind controller inputs to commands.
- Implement a mechanism using the command-based framework.
- Use interpolation to select motor outputs based on target distance.
- Work with Java interfaces, default methods, and functional interfaces.
- Separate hardware-independent logic from hardware-specific implementations.
- Control TalonFX motors using CTRE Phoenix 6.
- Read sensor measurements and record telemetry using AdvantageKit.
- Build, test, and debug a multi-class Java implementation.

## Exercise Sequence

Complete the exercises in the order listed below. Each exercise builds on concepts introduced in the preceding exercises.

### Exercise 1: Default Teleop

**File:** `DefaultTeleop.java`

**Focus:** Controller bindings and command scheduling.

Start by implementing the teleoperated control that connects the operator's face-up button to the shooter command.

You will learn how to:

- Access the controller and shooter through the `Robot` instance.
- Bind a button using `onTrue(...)`.
- Create a shooting command with a target distance of one meter.
- Pass a value using a Java lambda expression.

**Instructions:** [Exercise 1 — Default Teleop](docs/exercises/01-default-teleop.md)

### Exercise 2: Shooter Mechanism

**File:** `Shooter.java`

**Focus:** Mechanism behavior, lookup tables, commands, and telemetry.

Implement the higher-level shooter mechanism that translates a target distance into a flywheel duty-cycle output.

You will learn how to:

- Initialize a mechanism and register its default command.
- Update and log inputs with AdvantageKit.
- Use an interpolating lookup table.
- Work with `Supplier<Distance>` and `Supplier<Double>`.
- Compose flywheel operations into commands.
- Name commands for debugging and telemetry.

**Instructions:** [Exercise 2 — Shooter Mechanism](docs/exercises/02-shooter-mechanism.md)

### Exercise 3: Shooter I/O Interface

**File:** `ShooterIO.java`

**Focus:** Interfaces and hardware abstraction.

Explore how the shooter mechanism interacts with hardware without depending on a particular hardware implementation.

You will learn how to:

- Define and use Java interfaces.
- Understand default methods and method overriding.
- Represent sensor measurements in an input structure.
- Use `@AutoLog` with AdvantageKit.
- Understand how real, simulated, and fake implementations can share the same interface.

**Instructions:** [Exercise 3 — Shooter I/O Interface](docs/exercises/03-shooter-io.md)

### Exercise 4: Real Shooter Hardware

**File:** `ShooterReal.java`

**Focus:** CTRE Phoenix 6 motor control and sensor feedback.

Implement the real hardware layer that controls the flywheel motors and supplies measurements to the shooter mechanism.

You will learn how to:

- Initialize TalonFX motor controllers.
- Configure a follower motor with opposed alignment.
- Read and refresh Phoenix 6 status signals.
- Convert angular velocity measurements into rotations per second.
- Create continuously executing motor-control commands.
- Apply duty-cycle output requests.
- Verify the integration between hardware and mechanism layers.

**Instructions:** [Exercise 4 — Real Shooter Hardware](docs/exercises/04-shooter-real.md)

---

## How the Pieces Fit Together

The following diagram illustrates the relationship between the four files.

```text
             Operator Input
                   |
                   v
          +------------------+
          |   DefaultTeleop  |
          | Controller input |
          +------------------+
                   |
                   | Requests a shooting command
                   v
          +------------------+
          |      Shooter     |
          | Lookup table     |
          | Command logic    |
          | AdvantageKit logs |
          +------------------+
                   |
                   | Uses hardware abstraction
                   v
          +------------------+
          |    ShooterIO     |
          | Interface and    |
          | input structure  |
          +------------------+
                   ^
                   |
                   | Implements the interface
                   |
          +------------------+
          |    ShooterReal   |
          | TalonFX control  |
          | Sensor feedback  |
          +------------------+
                   |
                   v
             Physical Motors
```

### Execution Flow

When the operator presses the face-up button:

1. `DefaultTeleop` schedules the shooting command.
2. `Shooter` obtains the target distance and looks up the corresponding duty-cycle output.
3. `ShooterReal` applies that output to the primary flywheel motor.
4. The secondary motor follows the primary with the configured opposed alignment.
5. The shooter periodically updates its inputs and records telemetry through AdvantageKit.

The interface separates the mechanism's behavior from the implementation that communicates with the motors.

## Getting Started

Before beginning, make sure you have:

- The project cloned and opened in your CodeRunner environment.
- The project's dependencies resolved successfully.
- Access to the supporting classes, including `Robot`, `ShooterConstants`, `LookupTables`, and the generated AdvantageKit input class.
- Familiarity with basic Java classes, interfaces, lambdas, and methods.

### Recommended Workflow

1. Read the instructions for the current exercise before modifying code.
2. Inspect the referenced supporting classes and method signatures.
3. Complete the TODOs for that exercise.
4. Review your changes for correctness and consistency with the existing code style.
5. Build the project and run applicable tests.
6. Commit your completed exercise before moving on to the next one.

Do not begin by changing all four files at once. Working incrementally makes it easier to identify errors and understand the purpose of each component.

## Important Implementation Notes

### Lookup table and calibration

The shooter uses a distance-to-duty-cycle lookup table. The supplied values are examples for this exercise; they should not be assumed to be a validated real-world shooter calibration.

Understand how interpolation works and verify the expected output before testing on hardware.

### Commands and suppliers

A command describes an operation that the command framework executes. A `Supplier<T>` provides a value when its `get()` method is called.

Pay attention to when suppliers are evaluated. A supplier evaluated during repeated command execution can provide changing values, while a value calculated once may remain fixed.

### Hardware safety

The real shooter implementation can operate physical motors. Before testing on hardware:

- Verify the CAN IDs and wiring.
- Check motor direction and follower alignment.
- Secure the robot and clear the flywheels.
- Begin with conservative output and follow team testing procedures.
- Ensure the approved disable or emergency-stop procedure is available.

Compile and test the software before enabling physical outputs whenever possible.

## Testing and Verification

After completing each exercise, build the project and run the relevant tests.

At the end of the lab, verify that:

- [ ] The face-up button schedules the shooting command.
- [ ] The shooting command obtains its output from the lookup table.
- [ ] The shooter registers an idle default command.
- [ ] The input structure is updated and logged periodically.
- [ ] The I/O interface is implemented by the real hardware class.
- [ ] Both flywheel velocity signals are initialized and refreshed.
- [ ] The primary motor receives the requested duty-cycle output.
- [ ] The secondary motor follows the primary with the intended alignment.
- [ ] The project compiles and passes applicable tests and code-quality checks.


## Completion Criteria

You have completed the lab when all four exercises are implemented, the project builds successfully, the applicable tests pass, and you can explain how a controller input becomes a hardware command.

The goal is not just to make the shooter work. It is to understand how each software layer contributes to a maintainable, testable robot program.