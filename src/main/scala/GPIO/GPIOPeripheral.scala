import chisel3._
import chisel3.util._

// This module is the top level GPIO and PWM peripheral
// Instantiation of the modules are done here and
// the PipeCon interface is used to allow the CPU to
// configure and write/read from the GPIO and PWM peripherals

class GPIOPeripheral(addrWidth: Int) extends Module {

    // Instantiate the Pipecon interface
    val io = IO(new Bundle {
        val mem_ifc = (new PipeCon(addrWidth)) // From CPU
    })
    // Register address offsets
    val GPIO_REG_OFFSET         = 0x0000.U(addrWidth.W) // Start addr of GPIO registers
    val PWM_REG_OFFSET          = 0x0400.U(addrWidth.W) // Start addr of PWM registers

    // GPIO registers offsets within GPIO address space
    val GPIO_DIRECTION_OFFSET       = 0x0000.U(addrWidth.W) // GPIO direction register
    val GPIO_OUTPUT_OFFSET          = 0x0008.U(addrWidth.W) // GPIO output register
    val GPIO_INPUT_OFFSET           = 0x0010.U(addrWidth.W) // GPIO input register
    val GPIO_PULLUP_OFFSET          = 0x0018.U(addrWidth.W) // GPIO pullup register
    val GPIO_PULLDOWN_OFFSET        = 0x0020.U(addrWidth.W) // GPIO pulldown register
    val GPIO_OPENDRAIN_OFFSET       = 0x0028.U(addrWidth.W) // GPIO open drain register
    val GPIO_DRIVESTRENGTH_OFFSET   = 0x0030.U(addrWidth.W) // GPIO drive strength register

    // PWM registers offsets within PWM address space
    val PWM_ENABLE_OFFSET          = 0x0000.U(addrWidth.W) // PWM enable register
    val PWM_PERIOD_OFFSET          = 0x0008.U(addrWidth.W) // PWM period register
    val PWM_DUTY_CYCLE_OFFSET      = 0x0010.U(addrWidth.W) // PWM duty cycle register    
    val PWM_PRESCALER_OFFSET       = 0x0018.U(addrWidth.W) // PWM prescaler register
    val PWM_POLARITY_OFFSET        = 0x0020.U(addrWidth.W) // PWM polarity register

    // Default values, inputs 
    io.mem_ifc.address      := 0.U
    io.mem_ifc.rd           := false.B
    io.mem_ifc.wr           := false.B
    io.mem_ifc.wrData       := 0.U
    io.mem_ifc.wrMask       := "b0000".U
    
    // Default values, outputs
    io.mem_ifc.ack          := false.B
    io.mem_ifc.rdData       := 0.U

    // Mapping read address to registers
    val readRegAddr = io.mem_ifc.address - GPIO_REG_OFFSET

    switch (readRegAddr) {
        is(GPIO_DIRECTION_OFFSET) {
            io.mem_ifc.rdData := 1.U // Read GPIO direction register
        }
        is(GPIO_OUTPUT_OFFSET) {
            io.mem_ifc.rdData := 2.U // Read GPIO output register
        }
        is(GPIO_INPUT_OFFSET) {
            io.mem_ifc.rdData := 3.U // Read GPIO input register
        }
        is(GPIO_PULLUP_OFFSET) {
            io.mem_ifc.rdData := 4.U // Read GPIO pullup register
        }
        is(GPIO_PULLDOWN_OFFSET) {
            io.mem_ifc.rdData := 5.U // Read GPIO pulldown register
        }
        is(GPIO_OPENDRAIN_OFFSET) {
            io.mem_ifc.rdData := 6.U // Read GPIO open drain register
        }
        is(GPIO_DRIVESTRENGTH_OFFSET) {
            io.mem_ifc.rdData := 7.U // Read GPIO drive strength register
        }
    }




}