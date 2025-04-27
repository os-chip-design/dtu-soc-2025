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
    val GPIO_REG_OFFSET             = 0x0000                    // Start addr of GPIO registers
    val PWM_REG_OFFSET              = (GPIO_REG_OFFSET| 0x0100) // Start addr of PWM registers

    // GPIO registers offsets within GPIO address space
    val GPIO_DIRECTION_OFFSET       = (GPIO_REG_OFFSET | 0x0000)
    val GPIO_OUTPUT_OFFSET          = (GPIO_REG_OFFSET | 0x0008)
    val GPIO_INPUT_OFFSET           = (GPIO_REG_OFFSET | 0x0010)
    val GPIO_PULLUP_OFFSET          = (GPIO_REG_OFFSET | 0x0018)
    val GPIO_PULLDOWN_OFFSET        = (GPIO_REG_OFFSET | 0x0020)
    val GPIO_OPENDRAIN_OFFSET       = (GPIO_REG_OFFSET | 0x0028)
    val GPIO_DRIVESTRENGTH_OFFSET   = (GPIO_REG_OFFSET | 0x0030)

    // PWM registers offsets within PWM address space
    val PWM_ENABLE_OFFSET           = (PWM_REG_OFFSET | 0x0000)
    val PWM_PERIOD_OFFSET           = (PWM_REG_OFFSET | 0x0008)
    val PWM_DUTY_CYCLE_OFFSET       = (PWM_REG_OFFSET | 0x0010)
    val PWM_PRESCALER_OFFSET        = (PWM_REG_OFFSET | 0x0018)
    val PWM_POLARITY_OFFSET         = (PWM_REG_OFFSET | 0x0020)
    
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
    val regAddr     = RegInit(0.U(addrWidth.W))

    val rdAckReg    = RegInit(false.B)
    val wrAckReg    = RegInit(false.B)
    
    val writeReg    = RegInit(0.U(addrWidth.W))
    val readReg     = RegInit(0.U(addrWidth.W))

    io.mem_ifc.ack      := rdAckReg || wrAckReg
    io.mem_ifc.rdData   := readReg

    when(io.mem_ifc.wr) {
        writeReg := io.mem_ifc.wrData //& io.mem_ifc.wrMask
        regAddr := io.mem_ifc.address
    }

    // Reading state machine
    switch (io.mem_ifc.rd) {
        is(true.B) {
            switch (io.mem_ifc.address) {
                is(GPIO_DIRECTION_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_direction
                    rdAckReg    := true.B
                }
                is(GPIO_OUTPUT_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_output
                    rdAckReg    := true.B
                }
                is(GPIO_INPUT_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_input
                    rdAckReg    := true.B
                }
                is(GPIO_PULLUP_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_pullup
                    rdAckReg    := true.B
                }
                is(GPIO_PULLDOWN_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_pulldown
                    rdAckReg    := true.B
                }
                is(GPIO_OPENDRAIN_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_opendrain
                    rdAckReg    := true.B
                }
                is(GPIO_DRIVESTRENGTH_OFFSET.U(addrWidth.W)) {
                    readReg     := gpio_drivestrength
                    rdAckReg    := true.B
                }
                is(PWM_ENABLE_OFFSET.U(addrWidth.W)) {
                    readReg     := pwm_enable
                    rdAckReg    := true.B
                }
                is(PWM_DUTY_CYCLE_OFFSET.U(addrWidth.W)) {
                    readReg     := pwm_duty_cycle
                    rdAckReg    := true.B
                }
                is(PWM_PRESCALER_OFFSET.U(addrWidth.W)) {
                    readReg     := pwm_prescaler
                    rdAckReg    := true.B
                }
            }
        }

        is(false.B) {
            readReg := 0.U // Default value
            rdAckReg  := false.B
        }
    }

    // Writing state machine
    switch (io.mem_ifc.wr) {
        is(true.B) {
            switch (io.mem_ifc.address) {
                is(GPIO_DIRECTION_OFFSET.U(addrWidth.W)) {
                    gpio_direction := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(GPIO_OUTPUT_OFFSET.U(addrWidth.W)) {
                    gpio_output := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(GPIO_PULLUP_OFFSET.U(addrWidth.W)) {
                    gpio_pullup := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(GPIO_PULLDOWN_OFFSET.U(addrWidth.W)) {
                    gpio_pulldown := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(GPIO_OPENDRAIN_OFFSET.U(addrWidth.W)) {
                    gpio_opendrain := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(GPIO_DRIVESTRENGTH_OFFSET.U(addrWidth.W)) {
                    gpio_drivestrength := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(PWM_ENABLE_OFFSET.U(addrWidth.W)) {
                    pwm_enable := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(PWM_DUTY_CYCLE_OFFSET.U(addrWidth.W)) {
                    pwm_duty_cycle := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
                is(PWM_PRESCALER_OFFSET.U(addrWidth.W)) {
                    pwm_prescaler := io.mem_ifc.wrData
                    wrAckReg := true.B
                }
            }
        }

        is(false.B) {
            wrAckReg  := false.B
        }
    }
}