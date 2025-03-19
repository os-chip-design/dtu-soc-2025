# The SoC for the chip design course 2025
[![CI](https://github.com/os-chip-design/dtu-soc-2025/actions/workflows/scala.yml/badge.svg)](https://github.com/os-chip-design/dtu-soc-2025/actions/workflows/scala.yml)

We will build a RISC-V SoC targeting the ChipIgnite project from efabless.

This README shall be the starting point for the documentation.



## Task distribution

The main tasks are the task you are responisble for and that you are expected to lead and complete. The secondary tasks are the tasks for which you are expected to monitor the progress and give help. Every group needs to deal with verification and the chip design toolchain.

#### Group 3
- **GitHub users:** [@UlrikTJ](https://github.com/UlrikTJ), [@Emwm](https://github.com/Emwm)
- **Main tasks:** Serial IO (UART) + SPI (helping group 4)
- **Secondary tasks:** Text-based VGA

#### Group 7
- **GitHub users:** [@cjavad](https://github.com/cjavad) [@MagnusAH](https://github.com/MagnusAH)
- **Main tasks:** Wildcat CPU integration + System integration (e.i. maintaing top level) + Look into Caravel
- **Secondary tasks:** Serial IO (UART), Keyboard, GitHub CI

#### Group 4
- **GitHub users:** [@sadafayubb](https://github.com/sadafayubb), [@sofusham](https://github.com/sofusham), [@DavidBayPedersen](https://github.com/DavidBayPedersen)
- **Main tasks:** SPI controller, CPU (helping group 7)
- **Secondary tasks:** GPIO controller, PWM, & Timers

#### Group 20 
- **GitHub users:** Andreas - [@AndreasSvarta](https://github.com/AndreasSvarta), Rasmus - [@McQueen24](https://github.com/McQueen24) 
- **Main tasks:** Interconnection fabric (i.e., memory arbiter, bus, memory mapping, etc.).
- **Secondary tasks:** Verification

#### Group 1
- **GitHub users:** Arnór - [@Arn0r](https://github.com/Arn0r), Matthias - [@mattm4r](https://github.com/mattm4r), Stephan - [@StephanAAu](https://github.com/StephanAAU/).
- **Main tasks:** GPIO controller, PWM, & Timers
- **Secondary tasks:** Wildcat CPU integration, SPI controller

#### Group 8
- **GitHub users:** [@Willdew](https://github.com/Willdew), [@komv123](https://github.com/komv123)
- **Main tasks:** Keyboard, Text-based VGA
- **Secondary tasks:** GitHub CI

#### Group 2
- **GitHub users:** Tobias - [@Collinn](https://github.com/Collinnn), Mariana - [@immarianaas](https://github.com/immarianaas), Andreas - [@DreasL02](https://github.com/DreasL02).
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
- [ ] Investigate SPI:
  - [ ] Study the SPI protocol (eg. clock polarity, phase)
  - [ ] Check data width (normal/squad switch?) and speed
  - [ ] Decide on Master or Slave Configuration
- [ ] Design SPI specifications:
  - [ ] List input and output signals, as well as control signals
  - [ ] Define register for storing receiver data (and maybe temporary registers for data transmission?)
  - [ ] Decide on Error Handling
  - [ ] Specify performance metrics (clock frequency, latency, eg.)
- [ ] Design Block Diagram (subsystems, datapaths, clk and control paths):
- [ ] Design Register/Memory layout:
- [ ] Chisel Implementation:
- [ ] Verification:
  - [ ] Testbenches (?)
  - [ ] Analog tests

#### Wildcat CPU integration
- [ ] Integrate into top level system with "template" as specified under system integration
- [ ] Create tooling to run programs on hardware in simulations
- [ ] Integrate and coordinate with vital hardware components such as serial etc.
- [ ] Test hardware based instruction loading and program execution, including the ability to use memory

#### System integration & top-level maintenance
- [ ] Design "template" for every sub-implementation to follow
  - [ ] For a top level module interface
  - [ ] For a unified API specification
- [ ] Ensure every project uses this format so everything can integrate with each other, potentially mock implementations at the beginning
- [ ] Allow test suites at different levels (hardware/software) and integrate with CI
- [ ] Tie together top level module with sub-implementation interfaces
- [ ] Tie together top level module and caravel interface
- [ ] Ensure continiued development does not deviate from preestablished conventions and norms

#### Caravel exploration
- [ ] Find out how we want to invoke soc-dtu-2025 repo to actually build stuff e.i. do we keep caravel configuration in this repo and just use local ci invocation to use it with some monkey patching? Or is it even a good idea to use it as a submodule. 
- [ ] Outline top level specifications and translate to caravel pin-out / pin config
- [ ] Implement build step that is compatible with rest of build process

#### Interconnection fabric development (memory arbiter, bus, memory mapping, etc.)
- [ ] Define interconnection protocol(AXI4-Lite)
  - [ ] Define amount of components and address space in collaboration with the groups
  - [ ] Define IO interface
- [ ] Bridge CPU and AXI4-Lite interfaces
- [ ] Write bus logic in verilog
- [ ] Create a testbench for read and write with different components

#### Serial Peripheral Interface (SPI) off memory development
- [ ] Research SPI communication requirements
- [ ] Determine off-chip memory type (USB,SD-card,...)
- [ ] Define SPI mapping and wrapper functionality
- [ ] Work with group 4 for a standardized protocol to talk to external memory
- [ ] Implement the external memory according to protocol
- [ ] Integrate with github continuous integration
- [ ] Test and debug SPI communication with the memory
- [ ] Document API and how to use

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

#### Keyboard development
- [ ] Implementation details
  - [ ] Figure out what standard to use (USB or PS-2?)
  - [ ] Bus connection (Should it be interrupt or polling based?)
  - [ ] Parallel or serial connection?
  - [ ] Do we have a character set and where is it stored?
    - [ ] Do we have danish and english (Multible character sets or only one?)
  - [ ] Own clock divider or from main clock (Do we have PLL?)
- [ ] Research PS-2 
  - [ ] Clock domain
  - [ ] Data signal structure
  - [ ] Existing implentations
  - [ ] Is debouncing handled in the keyboard? Is shift and other modifiers handled in keyboard?
- [ ] Design block diagram
- [ ] Write testbench
- [ ] Implement
- [ ] Test on basys fpga?

#### Text-based VGA development
- [ ] Research text-based vga protocol
  - [ ] Colors?
  - [ ] Character set?
    - [ ] Rom or flash?
    - [ ] RAM or VRAM?
    - [ ] Character buffer
    - [ ] Is fps fixed, and if so at what framerate?
    - [ ] Resolution?
  - [ ] Is writing to character buffer handled by the cpu or keyboard controller?
  - [ ] General memory layout and implementation
- [ ] Block diagram
- [ ] Testbench
- [ ] Implementation
- [ ] Test on basys fpga?

#### GPIO controller development
- [x] Investigate possibility of pullup and pulldown resistors
- [x] Investigate how muxing between in/out should be handled
- [x] Investigate possibility output driver configuration (pushpull, open drain)
- [x] Investigate ESD protection necessity
- [ ] Design specification
- [ ] Design block diagram
- [ ] Design register/memory layout
- [ ] Design the wrapper for analog front-end
- [ ] Digital Hardware design

#### PWM & timers implementation
- [X] Investigate possibility of interrupts for CPU
- [ ] Investigate how muxing between timers, pwm and output
- [ ] Investigate capability needs, i.e. counting up/down
- [ ] Investigate clocking and clock division
- [ ] Design specification
- [ ] Design block diagram
- [ ] Design register/memory layout
- [ ] Digital Hardware design

#### Support for SPI controller
- [ ] Understand Specifications of SPI protocol
- [ ] How does the controller interact with the rest of the system (interrupts, registers)?
- [ ] Writing Testbench

#### Support for CPU development
- [ ] Understand the Wildcat CPU Design
- [ ] See how CPU connects to peripherals (SPI, GPIO, PWM, Timers)
- [ ] Testbenches and Simulations to confirm they work as expected

#### Support for Wildcat CPU integration
- [ ] Activity 1
- [ ] Activity 2

#### Verification tasks and CI
- [ ] Come up with verification plan
- [ ] Come up with a rough draft of a timeline of tests needed to be done
- [ ] Figure out success and failure criteria for each component
- [ ] Integrate with github actions to automate testing framework
- [ ] Integrate into CI on github


## System architecture and specifications

To be added.


## Setup

### Checking out the repository

- Be sure to initialize submodules with `git submodule update --init --recursive` or by cloning with `git clone --recurse-submodules`

## Repository structure

- [Wildcat CPU](https://github.com/schoeberl/wildcat) in ./wildcat
