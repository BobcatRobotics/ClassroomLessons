# Exercise 4: Controlling the Shooter Flywheels

## Objective

Implement the real shooter hardware using CTRE Phoenix 6 and integrate it with the existing command-based architecture.

By completing this exercise, you will learn how to:

- Initialize TalonFX motor controllers.
- Configure a motor to follow another motor with opposed alignment.
- Read and refresh Phoenix 6 status signals.
- Convert strongly typed angular velocity measurements into rotations per second.
- Create continuously executing commands.
- Apply a dynamic duty-cycle output using `DutyCycleOut`.
- Understand how hardware control connects to `ShooterIO` and `Shooter`.

## Before you begin

Review these files:

- `ShooterConstants.java` — contains the configured motor CAN IDs.
- `ShooterIO.java` — defines the hardware abstraction interface.
- `Shooter.java` — uses the flywheel implementation to execute shooter commands.
- `DefaultTeleop.java` — connects operator input to the shooter command.

Consult the CTRE Phoenix 6 documentation for the motor controller and status-signal APIs used in this project.

## Task 1: Initialize the motor controllers

Open the `ShooterReal` constructor.

The shooter contains two TalonFX motors. The primary motor receives control requests, and the secondary motor follows it.

**Requirements:**

1. Construct `flywheelMotorPrimary` using the primary motor CAN ID from `ShooterConstants`.
2. Construct `flywheelMotorSecondary` using the secondary motor CAN ID from `ShooterConstants`.
3. Use the CAN bus configuration appropriate for this project.
4. Preserve the existing final fields and initialize both before using them.

**Questions to consider:**

- What is a CAN ID?
- Why should CAN IDs be defined in a constants class?
- What is the difference between constructing a motor controller and configuring its behavior?

## Task 2: Configure the follower motor

The secondary motor should follow the primary motor with opposed alignment.

**Requirements:**

1. Create a Phoenix 6 `Follower` control request.
2. Specify the primary motor's CAN ID.
3. Configure the follower alignment as `MotorAlignmentValue.Opposed`.
4. Apply the request to the secondary motor.

The secondary motor should follow the primary without requiring a separate duty-cycle command on every execution.

**Questions to consider:**

- What does follower mode accomplish?
- Why might two mechanically coupled flywheels need opposed motor output?
- How does opposed alignment differ from reversing the direction in the mechanism's lookup table?

Do not assume opposed alignment is correct for every mechanical design. It must match the actual motor mounting and gearing.

## Task 3: Initialize the velocity status signals

The class declares two `StatusSignal<AngularVelocity>` fields.

Before refreshing or reading them, determine how to obtain each motor's velocity signal through the Phoenix 6 API.

**Requirements:**

1. Obtain the primary motor's velocity status signal.
2. Obtain the secondary motor's velocity status signal.
3. Store both signals in the appropriate fields.
4. Ensure both signals are initialized before `updateInputs(...)` is called.

**Important:** Merely declaring a status-signal field does not initialize it. Inspect the original code for this issue and correct it as part of your implementation.

## Task 4: Update and convert sensor measurements

Implement `updateInputs(ShooterIOInputs inputs)`.

The method must refresh the status signals and copy the measured velocities into the supplied input structure.

**Requirements:**

1. Refresh both status signals using `BaseStatusSignal.refreshAll(...)`.
2. Read each signal's measured value.
3. Convert each value into rotations per second using the WPILib units API.
4. Store the primary measurement in `inputs.velocityOfPrimaryRotPerSecond`.
5. Store the secondary measurement in `inputs.velocityOfSecondaryRotPerSecond`.

The input fields are `double` values, but the status signals contain strongly typed `AngularVelocity` measurements.

**Questions to consider:**

- Why should you refresh a status signal before reading it?
- What does `.in(Rotations.per(Seconds))` do?
- Why does the interface use primitive values while Phoenix 6 uses a unit-aware measurement?
- What should you consider if a status signal refresh fails?

## Task 5: Implement the idle command

Implement `idle()`.

The primary motor should repeatedly receive a stop request. The secondary motor follows the primary, so it does not require its own independent stop command in this design.

**Requirements:**

1. Use `runRepeatedly(...)` to create a continuously executing command.
2. Use the primary motor's `stopMotor()` method as the repeated action.
3. Name the command `ShooterFlywheel.Idle`.
4. Return the resulting command.

**Questions to consider:**

- What is the difference between creating a command and executing a command?
- Why does this method return a `Command`?
- What happens when the command is interrupted?

## Task 6: Implement duty-cycle control

Implement `runFlywheelDutyOutput(Supplier<Double> speed)`.

This method receives a supplier of the desired motor output. The supplier may calculate its value from the shooter lookup table.

**Requirements:**

1. Evaluate `speed.get()` inside the repeated action.
2. Apply the resulting value using the reusable `DutyCycleOut` request.
3. Send the control request to the primary TalonFX.
4. Name the command `Shooter.Flywheel.RunFlywheelSpeed`.
5. Return the command.


**Questions to consider:**

- What is the expected range of a duty-cycle request?
- Why is a `double dutyCylce` useful here?
- Why is the `DutyCycleOut` request stored as a field?
- Why does this method control the primary motor instead of directly controlling both motors?

## Task 7: Verify the complete integration

Build the project and run its applicable tests.

- [ ] Both TalonFX controllers use the configured CAN IDs.
- [ ] A velocity status signals is initialized.
- [ ] Both signals are refreshed before their measurements are read.
- [ ] Velocity values are converted to rotations per second.
- [ ] The input structure receives both measurements.
- [ ] The idle command repeatedly stops the primary motor.
- [ ] The duty-cycle command evaluates the supplier repeatedly.
- [ ] The commands use the required names.
- [ ] The project compiles and passes applicable checks.

## Safety requirements

Before testing on real hardware:

- Verify that the robot is securely supported and the flywheels are clear of people and objects.
- Verify motor IDs, wiring, mechanical alignment, and motor direction.
- Test the idle behavior before applying significant output.
- Start with a conservative output and follow the team's motor-testing procedures.
- Keep an emergency stop or other approved disable mechanism readily available.

Never use a successful compilation as proof that motor direction or follower configuration is mechanically safe.

## Reflection questions

1. Why is the real hardware implementation separate from the shooter mechanism?
2. What is the difference between a sensor measurement and a control request?
3. Why must status signals be initialized before they are refreshed?
4. What is the purpose of using a supplier for motor output?
5. How does opposed follower alignment affect the secondary motor?
6. Why might a simulated or fake implementation be preferable for automated unit tests?
7. How could you test that the secondary output is opposed without risking damage to the hardware?