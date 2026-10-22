# Exercise 2: Implementing the Shooter Mechanism

## Objective

Implement a shooter mechanism that selects a flywheel duty-cycle output based on the distance to a target and runs the flywheel using the project's Command V3 framework.

By completing this exercise, you will learn how to:

- Use an interpolating lookup table.
- Work with Java `Supplier` functional interfaces.
- Create commands using the Command V3 coroutine API.
- Configure a mechanism's default command.
- Record subsystem inputs using AdvantageKit.
- Separate mechanism behavior from hardware I/O implementations.

## Before you begin

Inspect these files before modifying `Shooter.java`:

- `ShooterIO.java` — understand how shooter input data is represented and updated.
- `ShooterReal.java` — understand the flywheel operations available to the mechanism.
- `LookupTables.java` — understand the interpolators provided by the project.
- `DefaultTeleop.java` — see how the shooter command is invoked by a controller binding.

Do not implement new hardware-control methods unless the exercise explicitly requires them. Reuse the existing APIs.

## Task 1: Initialize the mechanism

Open the `Shooter` constructor.

The constructor receives a `ShooterIO` implementation. The class also needs a default command so the shooter has a defined idle behavior when no other command is scheduled.

**Requirements:**

1. Store the supplied I/O implementation in the appropriate field.
2. Register `idle()` as the default command using the mechanism API.
3. Preserve the constructor's existing parameter and visibility.

**Questions to consider:**

- Why is the I/O implementation passed into the constructor?
- What is the purpose of a default command?
- When should the default command run?

## Task 2: Process and log inputs

Implement `periodic()`.

The framework calls this method periodically. The shooter input structure is already created as an automatically logged input object.

**Requirements:**

1. Ask the I/O implementation to update the input structure.
2. Process the updated structure using AdvantageKit's `Logger`.
3. Use the log key `Shooter/inputs`.

**Questions to consider:**

- Why should the inputs be updated before they are logged?
- What information does AdvantageKit record?
- Why does the mechanism use an input structure instead of logging every field individually?

## Task 3: Understand the lookup table

The `FLYWHEEL_LUT` field maps a target distance to a `Double` representing flywheel duty-cycle output.

The table is already populated with these values:

| Target distance | Duty-cycle output |
|---:|---:|
| 0 m | 1.0 |
| 0.5 m | 0.8 |
| 1.0 m | 0.6 |
| 1.5 m | 0.4 |
| 2.0 m | 0.2 |
| 2.5 m | 0.0 |

The configured interpolators determine how values between the entries are calculated.

**Your tasks:**

1. Identify the key and value types used by the table.
2. Explain the purpose of each interpolator.
3. Determine the expected output at a distance of 1.25 meters.
4. Determine the expected output at a distance of 2.25 meters.
5. Explain what happens when a requested distance falls outside the configured range, based on the project's `InterpolatingTreeMap` implementation.

Do not replace the table with a series of `if` statements. The goal is to learn how to use the supplied interpolation API.

## Task 4: Implement the idle command

Complete `idle()`.

The `ShooterReal` implementation provides the flywheel's idle behavior. The command should delegate to that implementation through the Command V3 coroutine API.

**Requirements:**

1. Create a command using the mechanism's `run(...)` API.
2. Use the coroutine to await completion of the flywheel's idle operation.
3. Name the resulting command `Shooter.Idle`.
4. Return the command.

**Questions to consider:**

- What does `awaitAll(...)` accomplish?
- Why does `idle()` return a `Command` instead of directly controlling the motor?
- How does naming a command help with debugging and telemetry?

## Task 5: Calculate the requested flywheel output

Complete the beginning of `shoot(Supplier<Distance> targetDistance)`.

The method receives a supplier rather than a distance directly. This means the distance can be obtained by calling `targetDistance.get()`.

**Requirements:**

1. Create a `Supplier<Double>` named `flywheelDutyOutput`.
2. When evaluated, it must obtain the current target distance.
3. Use that distance to query `FLYWHEEL_LUT`.
4. Keep the lookup logic in the supplier instead of duplicating the interpolation calculation.

**Questions to consider:**

- What is the difference between `Distance` and `Supplier<Distance>`?
- What does `targetDistance.get()` return?
- Why does the lookup table return a `Double` rather than a `Distance`?

## Task 6: Create the shooting command

Finish the `shoot(...)` method.

The flywheel implementation exposes an operation for running at a duty-cycle output supplied by the caller.

**Requirements:**

1. Create a command using the mechanism's `run(...)` API.
2. Await the flywheel operation using the `flywheelDutyOutput` supplier.
3. Name the resulting command `Shooter.Shoot`.
4. Return the command.

The method should use the lookup table to select the output. It should not directly set a motor output in `Shooter`.

## Task 7: Verify the implementation

Build the project and run the applicable unit tests.

Check the following scenarios:

- [ ] The constructor stores the provided I/O implementation.
- [ ] The idle command is registered as the default command.
- [ ] Periodic input updates occur before logging.
- [ ] The lookup table returns the configured outputs at its exact keys.
- [ ] Intermediate distances produce interpolated values.
- [ ] The idle command delegates to the flywheel idle operation.
- [ ] The shooting command uses the supplied target distance.
- [ ] Both commands have the required names.
- [ ] The project passes compilation and applicable code-quality checks.

## Reflection questions

1. Why is interpolation useful for a shooter?
2. What advantage does `Supplier<Double>` provide over calculating the output once?
3. Why should the mechanism delegate hardware behavior to `ShooterReal`?
4. What is the relationship between a command and a mechanism?
5. How does AdvantageKit help diagnose problems when the robot is running?
6. What would need to change if the lookup table were calibrated for a different target or shooting configuration?

## Optional extension

Add unit tests for the lookup-table behavior. Verify exact key values and intermediate values, and document the expected behavior outside the configured distance range.

If you change the table, explain why the new calibration points are appropriate for the robot's actual shooting behavior.