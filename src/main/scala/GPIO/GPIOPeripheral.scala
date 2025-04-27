import chisel3._
import chisel3.util._

// This module is the top level GPIO and PWM peripheral
// Instantiation of the modules are done here and
// the PipeCon interface is used to allow the CPU to
// configure and write/read from the GPIO and PWM peripherals

class GPIOPeripheral(addrWidth: Int, nofGPIO: Int) extends Module {

    // Instantiate the Pipecon interface
    val io = IO(new Bundle {
        val mem_ifc = (new PipeCon(addrWidth)) // From CPU
    })
    // Register address offsets
    val GPIO_REG_OFFSET         = 0x0000 // Start addr of GPIO registers
    val PWM_REG_OFFSET          = 0x0400 // Start addr of PWM registers

    // GPIO registers offsets within GPIO address space
    val GPIO_DIRECTION_OFFSET       = (GPIO_REG_OFFSET | 0x0000) // GPIO direction register
    val GPIO_OUTPUT_OFFSET          = (GPIO_REG_OFFSET | 0x0008) // GPIO output register
    val GPIO_INPUT_OFFSET           = (GPIO_REG_OFFSET | 0x0010) // GPIO input register
    val GPIO_PULLUP_OFFSET          = (GPIO_REG_OFFSET | 0x0018) // GPIO pullup register
    val GPIO_PULLDOWN_OFFSET        = (GPIO_REG_OFFSET | 0x0020) // GPIO pulldown register
    val GPIO_OPENDRAIN_OFFSET       = (GPIO_REG_OFFSET | 0x0028) // GPIO open drain register
    val GPIO_DRIVESTRENGTH_OFFSET   = (GPIO_REG_OFFSET | 0x0030) // GPIO drive strength register

    // PWM registers offsets within PWM address space
    val PWM_ENABLE_OFFSET          = (PWM_REG_OFFSET | 0x0000) // PWM enable register
    val PWM_PERIOD_OFFSET          = (PWM_REG_OFFSET | 0x0008) // PWM period register
    val PWM_DUTY_CYCLE_OFFSET      = (PWM_REG_OFFSET | 0x0010) // PWM duty cycle register    
    val PWM_PRESCALER_OFFSET       = (PWM_REG_OFFSET | 0x0018) // PWM prescaler register
    //val PWM_POLARITY_OFFSET        = 0x0020.U(addrWidth.W) // PWM polarity register
    
    // Default values, outputs
    io.mem_ifc.ack          := false.B
    io.mem_ifc.rdData       := 0.U

    // Instantiation of the GPIO registers
    val gpio_direction     = RegInit(0x0.U(nofGPIO.W))
    val gpio_output        = RegInit(0xF.U(nofGPIO.W))
    val gpio_input         = RegInit(0.U(nofGPIO.W))
    val gpio_pullup        = RegInit(0.U(nofGPIO.W))
    val gpio_pulldown      = RegInit(0.U(nofGPIO.W))
    val gpio_opendrain     = RegInit(0.U(nofGPIO.W))
    val gpio_drivestrength = RegInit(0.U(nofGPIO.W))

    val pwm_enable         = RegInit(0.U(nofGPIO.W))
    val pwm_duty_cycle     = RegInit(0.U((8*nofGPIO).W))
    val pwm_prescaler      = RegInit(0.U((8*nofGPIO).W))
    val pwm_div            = RegInit(0.U((8*nofGPIO).W))

    // Instantiation of GPIO modules
    val gpio_module = Seq.fill(nofGPIO)(Module(new GPIOModule))

    // Mapping the GPIO modules to the corresponding registers
    for (i <- 0 until nofGPIO) {
        gpio_module(i).io.gpio_direction     := gpio_direction(i)
        gpio_module(i).io.gpio_output        := gpio_output(i)
        gpio_module(i).io.drivestrength      := gpio_drivestrength(i)
        gpio_module(i).io.pullup_en          := gpio_pullup(i)
        gpio_module(i).io.pulldown_en        := gpio_pulldown(i)
        gpio_module(i).io.opendrain_en       := gpio_opendrain(i)
        gpio_module(i).io.pwm_en             := pwm_enable(i)
        gpio_module(i).io.pwm_div            := pwm_div(i)
        gpio_module(i).io.duty_cycle         := pwm_duty_cycle(i)
    }

    // Piping read address to statemachine/registers
    val readRegAddr = io.mem_ifc.address

    // WIP:
    switch (readRegAddr) {
        is(GPIO_DIRECTION_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_direction // Read GPIO direction register
        }
        is(GPIO_OUTPUT_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_output // Read GPIO output register
        }
        is(GPIO_INPUT_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_input // Read GPIO input register
        }
        is(GPIO_PULLUP_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_pullup // Read GPIO pullup register
        }
        is(GPIO_PULLDOWN_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_pulldown // Read GPIO pulldown register
        }
        is(GPIO_OPENDRAIN_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_opendrain // Read GPIO open drain register
        }
        is(GPIO_DRIVESTRENGTH_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := gpio_drivestrength // Read GPIO drive strength register
        }
        is(PWM_ENABLE_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := pwm_enable // Read PWM enable register
        }
        // is(PWM_PERIOD_OFFSET.U(addrWidth.W)) {
        //     io.mem_ifc.rdData := pwm_period // Read PWM period register
        // }
        is(PWM_DUTY_CYCLE_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := pwm_duty_cycle // Read PWM duty cycle register
        }
        is(PWM_PRESCALER_OFFSET.U(addrWidth.W)) {
            io.mem_ifc.rdData := pwm_prescaler // Read PWM prescaler register
        }
    }
}