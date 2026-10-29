# Intake Mechanism: Student Lessons

## Overview

This lesson series walks through the implementation of an intake arm for an FRC robot using **WPILib 2027 Command V3, CTRE Phoenix 6, and AdvantageKit**.

You will learn how to separate hardware access from mechanism behavior, collect telemetry, implement position and voltage control, and connect the mechanism to the operator's controller.

The lessons build on one another. Complete them in order to understand how each layer contributes to the finished intake system.

## Learning Objectives

By the end of this series, you should be able to:

- Design a hardware abstraction using Java interfaces.
- Implement real hardware IO with a TalonFX and CANcoder.
- Create a reusable mechanism using Command V3.
- Log sensor measurements and mechanism state with AdvantageKit.
- Use WPILib units for angles, velocities, and voltages.
- Create commands and triggers for mechanism control.
- Connect controller inputs to mechanism commands in a teleoperated OpMode.

## Lesson Sequence

### 1. [IntakeIO — Hardware Abstraction](IntakeIO.md)

**Start here.** Define the contract that all intake IO implementations must follow.

Topics covered:

- The `IntakeIO` interface and `IntakeIOInputs` data structure.
- AdvantageKit's `@AutoLog` annotation.
- Position, velocity, voltage, current, and temperature telemetry.
- Position control, voltage control, stopping, and simulation methods.
- Default interface methods and hardware abstraction.

**Outcome:** A common interface that real hardware, simulation, and test implementations can share.

### 2. [IntakeIOReal — Real-Hardware Implementation](IntakeIOReal.md)

Implement the IO interface using actual robot hardware.

Topics covered:

- TalonFX and CANcoder initialization.
- CTRE Phoenix 6 configuration.
- Feedback ratios, PID gains, neutral mode, and current limits.
- Reading motor and encoder telemetry.
- Converting between rotations and radians.
- Position and voltage requests.
- Absolute encoder calibration and angle normalization.

**Outcome:** A hardware IO implementation that communicates with the intake motor and encoder.

### 3. [Intake — Mechanism Logic](Intake.md)

Build the mechanism layer that connects the IO implementation to the rest of the robot program.

Topics covered:

- Dependency injection and the `Mechanism` interface.
- AdvantageKit input processing and output logging.
- Unit-aware getters and optional position goals.
- Clamping position and duty-cycle requests.
- Command V3 coroutines and `coroutine.yield()`.
- Position commands, voltage commands, proximity triggers, and hold behavior.
- Simulation updates.

**Outcome:** A reusable intake mechanism that exposes commands and measurements without depending directly on the hardware implementation.

### 4. [DefaultTeleop — Operator Controls](DefaultTeleop.md)

Connect the intake mechanism to the driver's controller.

Topics covered:

- Registering an OpMode with `@Teleop`.
- Implementing the `OpMode` interface.
- Combining controller triggers with `and()`.
- Binding commands using `whileTrue()`.
- Using bumpers as modifier buttons.
- Controlling the intake angle and applying direct duty-cycle output.
- Integrating intake controls with shooter controls.

**Outcome:** A teleoperated control mode that lets the driver operate the intake using controller inputs.

## Architecture Overview

The lessons follow this structure:

```text
DefaultTeleop
     |
     v
  Intake
 (Mechanism)
     |
     v
   IntakeIO
  (Interface)
     |
     +-------------------+
     |                   |
     v                   v
 IntakeIOReal       IntakeIOSim
 (Real Hardware)   (Simulation)
     |
     v
 TalonFX + CANcoder
```

`IntakeIOReal` and `IntakeIOSim` are alternative implementations of the same interface. The diagram illustrates the intended architecture; the simulation implementation may be introduced separately.

AdvantageKit collects and records the telemetry exposed through the IO layer and the mechanism.

## Recommended Workflow

1. Read the lesson and understand its responsibilities.
2. Complete the TODO sections before looking at a finished implementation.
3. Review the JavaDoc and explain the purpose of each method.
4. Build the project and resolve compilation errors.
5. Test the behavior in simulation when available.
6. Verify controller mappings and mechanism limits.
7. Test real hardware under appropriate supervision.

## Safety and Testing

Before operating the physical robot:

- Verify motor direction and encoder calibration.
- Confirm the intake's minimum and maximum angles.
- Check current limits and controller configuration.
- Test low-output commands before using higher outputs.
- Confirm commands stop or transition safely when interrupted.
- Keep the mechanism clear of people and obstructions during testing.

Software limits help constrain requests but do not replace mechanical stops, appropriate motor configuration, or physical safety procedures.

## Prerequisites

You should have a working robot project with:

- Java and the project's configured Gradle toolchain.
- WPILib 2027 Alpha and Command V3 dependencies.
- CTRE Phoenix 6 dependencies.
- AdvantageKit configured for input logging and generated `AutoLog` classes.
- An `IntakeConstants` class containing the required pivot configuration.
- A `Robot` class that provides access to the intake mechanism and controller.

## Completion Criteria

You have completed this series when:

- [ ] The IO interface compiles and defines all required operations.
- [ ] The real-hardware implementation configures and reads the devices correctly.
- [ ] The mechanism exposes working position and voltage commands.
- [ ] Telemetry appears in AdvantageKit logs.
- [ ] Simulation works when a simulation IO implementation is provided.
- [ ] The teleop controls invoke the intended intake commands.
- [ ] The mechanism operates within its configured limits and has verified stopping behavior.

## Next Steps

Once the intake is working, consider extending the project with:

- Unit tests for angle clamping and command behavior.
- A dedicated simulation implementation with a physics model.
- Tests for absolute encoder calibration.
- Position and velocity plots in AdvantageScope.
- Autonomous commands that coordinate intake movement with the shooter.
- Additional fault handling for disconnected sensors or failed motor-controller configuration.

**Remember:** The goal is not just to make the intake move. It is to build a mechanism that is testable, observable, reusable, and safe to operate.
