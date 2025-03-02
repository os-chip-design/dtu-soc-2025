# The SoC for the chip design course 2025

We will build a RISC-V SoC targeting the ChipIgnite project from efabless.

This README shall be the starting point for the documentation.

## Task distribution

The main tasks are the task you are responisble for and that you are expected to lead and complete. The secondary tasks are the tasks for which you are expected to monitor the progress and give help. Every group needs to deal with verification and the chip design toolchain.

#### Group 3
- **GitHub users:** *ADD* 
- **Main tasks:** Serial IO (UART) + SPI (helping group 4)
- **Secondary tasks:** Text-based VGA

#### Group 7
- **GitHub users:** [@cjavad](https://github.com/cjavad) [@MagnusAH](https://github.com/MagnusAH)
- **Main tasks:** Wildcat CPU integration + System integration (e.i. maintaing top level) + Look into Caravel
- **Secondary tasks:** Serial IO (UART), Keyboard, GitHub CI

#### Group 4
- **GitHub users:** [@sadafayubb](https://github.com/sadafayubb), [@sofusham](https://github.com/sofusham)
- **Main tasks:** SPI controller, CPU (helping group 7)
- **Secondary tasks:** GPIO controller, PWM, & Timers

#### Group 20 
- **GitHub users:** Andreas - [@AndreasSvarta](https://github.com/AndreasSvarta), Rasmus - [@McQueen24](https://github.com/McQueen24) 
- **Main tasks:** Intercooncation fabric (i.e., memory arbitrer, bus, memory mapping, etc).
- **Secondary tasks:** Verification

#### Group 1
- **GitHub users:** Arnór - [@Arn0r](https://github.com/Arn0r), Matthias - [@mattm4r](https://github.com/mattm4r), Stephan - [@StephanAAu](https://github.com/StephanAAU/).
- **Main tasks:** GPIO controller, PWM, & Timers
- **Secondary tasks:** Wildcat CPU integration, SPI controller

#### Group 8
- **GitHub users:** *ADD* 
- **Main tasks:** Keyboard, Text-based VGA
- **Secondary tasks:** GitHub CI

#### Group 2
- **GitHub users:** *ADD* 
- **Main tasks:** Off-chip SPI memory, Verification learder, GitHub CI
- **Secondary tasks:** Wildcat CPU integration, SPI controller

#### Group 13
- **GitHub users:** Jacob - [@jjdehl](https://github.com/jjdehl), Asger - [@AsgerWenneb](https://github.com/AsgerWenneb), Carl - [@CarlVinten](https://github.com/CarlVinten)
- **Main tasks:** Memories (custom + exisisting), Chip toolchain leader
- **Secondary tasks:** GPIO controller, PWM, & Timers

#### Teachers
- **GitHub users:** [@schoeberl](https://www.github.com/schoeberl), [@lucapezza](https://www.github.com/lucapezza)
- **Tasks**: Specifications, coordination, repository structure, project planning

## Task log

#### Serial IO (UART) development
- [ ] Research UART communication requirements
- [ ] Use import @shoeberl core UART
- [ ] Define UART mapping and wrapper functionality
- [ ] Implement the module
- [ ] Integrate with system interconnect
- [ ] Test and debug UART communication
- [ ] Document usage and API

#### SPI controller development
- [ ] Activity 1
- [ ] Activity 2

#### Wildcat CPU integration
- [ ] Activity 1
- [ ] Activity 2

#### System integration & top-level maintenance
- [ ] Activity 1
- [ ] Activity 2

#### Caravel exploration
- [ ] Activity 1
- [ ] Activity 2

#### Interconnection fabric development (memory arbiter, bus, memory mapping, etc.)
- [ ] Activity 1
- [ ] Activity 2

#### Off-chip SPI memory development
- [ ] Activity 1
- [ ] Activity 2

#### Verification leadership
- [ ] Activity 1
- [ ] Activity 2

#### Memory system development (custom + existing)
- [ ] Existing memory solution
  - [ ] Selection of memory
  - [ ] Simulation model
  - [ ] Macro injection in openlane
- [ ] Custom memory
  - [ ] Analog design
    - [ ] Bitcell
    - [ ] Write driver
    - [ ] Sense amplifier
    - [ ] (Precharger)
  - [ ] Digital design
    - [ ] Wordline decoder
    - [ ] Write masks
    - [ ] Two ports?
  - [ ] Generator script
  - [ ] Macro injection in openlane
  - [ ] Power gating
  - [ ] Verilog simulation model
  - [ ] Analog tests

#### Chip toolchain leadership
- [ ] Align groups on interfacing the toolchain
- [ ] Align with CI group
- [ ] Maintain config.json

#### Keyboard controller development
- [ ] Activity 1
- [ ] Activity 2

#### Text-based VGA development
- [ ] Activity 1
- [ ] Activity 2

#### GPIO controller development
- [ ] Investigate possibility of pullup and pulldown resistors
- [ ] Investigate how muxing between in/out should be handled
- [ ] Investigate possibility output driver configuration (pushpull, open drain)
- [ ] Design specification
- [ ] Design block diagram
- [ ] Design register/memory layout
- [ ] Digital Hardware design (Verilog)

#### PWM & timers implementation
- [ ] Investigate possibility of interrupts for CPU
- [ ] Investigate how muxing between timers, pwm and output
- [ ] Investigate capability needs, i.e. counting up/down
- [ ] Design specification
- [ ] Design block diagram
- [ ] Design register/memory layout
- [ ] Digital Hardware design (Verilog)

#### Support for SPI controller
- [ ] Understand Specifications of SPI protocol
- [ ] How does the controller interact with the rest of the system (interrupts, registers)?
- [ ] Writing Testbench

#### Support for CPU development
- [ ] Activity 1
- [ ] Activity 2

#### Support for Wildcat CPU integration
- [ ] Activity 1
- [ ] Activity 2

#### Keyboard development
- [ ] Activity 1
- [ ] Activity 2

#### Verification tasks
- [ ] Activity 1
- [ ] Activity 2

#### GitHub CI maintenance & automation
- [ ] Activity 1
- [ ] Activity 2


## System architecture and specifications

To be added.


## Setup

### Checking out the repository

- Be sure to initialize submodules with `git submodule update --init --recursive` or by cloning with `git clone --recurse-submodules`

## Repository structure

- [Wildcat CPU](https://github.com/schoeberl/wildcat) in ./wildcat
