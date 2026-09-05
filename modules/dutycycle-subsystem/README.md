# FRC Hopper Subsystem — Complete Lesson Plan

## Lesson Overview

In this lesson series, students will build a complete FRC hopper subsystem using an **IO-layer architecture**.

Students will start with a basic command-based robot and progressively add:

1. `RobotContainer`
2. `Hopper`
3. `HopperIO`
4. `HopperReal`
5. `HopperSim`

By the end of the lesson, students will have a hopper that can:

- Run on a real TalonFX.
- Run in simulation.
- Be controlled through commands.
- Report motor telemetry.
- Log data with AdvantageKit.
- Switch automatically between real hardware and simulation.

---

# Learning Objectives

By the end of the lesson series, students should be able to:

### Command-Based Programming

- Understand the role of `RobotContainer`.
- Bind controller buttons to commands.
- Understand how commands interact with subsystems.

### Subsystems

- Create a command-based subsystem.
- Separate mechanism logic from hardware implementation.
- Create a clean subsystem API.

### IO Architecture

- Explain the purpose of an IO interface.
- Understand dependency injection.
- Separate real hardware from simulation.
- Pass sensor data through an IO input structure.

### CTRE Phoenix

- Create a `TalonFX`.
- Configure motor inversion.
- Configure brake/coast behavior.
- Configure current limits.
- Read TalonFX status signals.
- Control a TalonFX using duty cycle.

### Simulation

- Create a `DCMotorSim`.
- Model a Kraken X60.
- Simulate motor voltage.
- Simulate position, velocity, acceleration, and current.
- Use `TalonFXSimState`.

### AdvantageKit

- Use `@AutoLog`.
- Populate logged input structures.
- Record commanded outputs.
- View telemetry in AdvantageScope.

---

# Prerequisites

Students should already understand:

- Basic Java syntax.
- Classes and objects.
- Methods.
- Constructors.
- Interfaces.
- Variables.
- `if` statements.
- Basic enums.
- Java packages/imports.
- Basic WPILib command-based programming.
- Basic Git/GitHub workflow.

Students should also have:

- WPILib installed.
- A working robot project.
- CTRE Phoenix installed through the project's dependencies.
- AdvantageKit configured.
- AdvantageScope available for log inspection.

---

# Overall Architecture

Before writing code, show students the architecture they are going to build.

```text
                       RobotContainer
                             |
                             v
                          Hopper
                             |
                             v
                         HopperIO
                        /         \
                       /           \
                      v             v
                HopperReal      HopperSim
                    |                |
                    v                v
                 TalonFX         DCMotorSim
                    |                |
                    v                v
              Real Hardware     Simulated Motor
                       \          /
                        \        /
                         v      v
                      HopperIOInputs
                             |
                             v
                        AdvantageKit
                             |
                             v
                        AdvantageScope
```

Explain that the goal is **not just to make the hopper move**.

The goal is to create an architecture that allows the same robot code to work with both real hardware and simulation.

---

# Understanding the Robot Structure

## Instructor Guide

Students should understand where the hopper belongs in the command-based robot architecture.

## Student Activity

Have students inspect the existing `Robot.java` and `RobotContainer.java`.

Ask:

> Where should the code that controls the hopper live?

Students should eventually identify that the mechanism belongs in a subsystem rather than directly inside `Robot.java`.

## Homework

Explain:

```text
Robot
  ↓
RobotContainer
  ↓
Subsystems
  ↓
Commands
```

`RobotContainer` is where the robot's subsystems and controls are assembled.

---


## Stage 1 — Give Students the Starting File

Students receive a partially completed file.

For example:

```java
public HopperSim() {

}
```

Do not immediately show the completed solution.

---

## Stage 2 — Add One Piece at a Time

Students add one logical change.

For example:

```java
motorSim = new DCMotorSim(...);
```

Then explain:

- What the line does.
- Why it is needed.
- What would happen without it.

---

## Stage 3 — Test

After each major section, have students compile or run simulation.

This helps students associate:

```text
Code
 ↓
Robot behavior
```

rather than simply copying a completed file.

---

# The Big Picture

The ultimate lesson students should take away is not how to write a hopper.

It is how to design robot software.

The hopper demonstrates a reusable architecture:

```text
             SUBSYSTEM
                 |
                 v
               IO API
              /      \
             /        \
          REAL        SIM
           |           |
        Hardware     Model
             \       /
              \     /
               INPUTS
                  |
                  v
              LOGGING
```

The subsystem describes **what the mechanism does**.

The IO interface describes **what hardware information and controls are available**.

The real implementation describes **how the physical hardware works**.

The simulation implementation describes **how to model that hardware without a robot**.

AdvantageKit provides the telemetry that lets students observe what is happening.

This architecture gives the team a foundation that can be reused for:

- Intake
- Shooter
- Arm
- Elevator
- Climber
- Drivetrain
- Other mechanisms

Once students understand the hopper, they should be able to apply the same architecture to almost any mechanism on the robot.