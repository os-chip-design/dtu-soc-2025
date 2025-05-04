import chisel3._
import chisel3.util._

// This module is the top level GPIO and PWM peripheral
// Instantiation of the modules are done here and
// the PipeCon interface is used to allow the CPU to
// configure and write/read from the GPIO and PWM peripherals

class GPIOPeripheral(addrWidth: Int, nofGPIO: Int, testMode: Boolean = false) extends Module {

    // Instantiate the Pipecon interface
    val io = IO(new Bundle {
        val mem_ifc = (new PipeCon(addrWidth)) // From CPU
        // add test ports for the top-level module
        val test_ports = if (testMode) Some(new Bundle {
            val test_OE_N = Vec(nofGPIO, Output(Bool()))
            val test_OUT = Vec(nofGPIO, Output(Bool()))
            val test_PAD_IN = Vec(nofGPIO, Input(Bool()))
        }) else None
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
    io.mem_ifc.ack                  := false.B
    io.mem_ifc.rdData               := 0.U

    // Instantiation of the GPIO registers
    val gpio_output         = RegInit(0xF.U(nofGPIO.W))
    val gpio_input          = Reg(Vec(nofGPIO, UInt(nofGPIO.W)))

    val gpio_direction      = Module(new GPIOShiftRegister(8))
    val gpio_pullup         = Module(new GPIOShiftRegister(8))
    val gpio_pulldown       = Module(new GPIOShiftRegister(8))
    val gpio_opendrain      = Module(new GPIOShiftRegister(8))
    val gpio_drivestrength  = Module(new GPIOShiftRegister(8))
    val pwm_polarity        = Module(new GPIOShiftRegister(8))

    val pwm_enable          = Module(new GPIOShiftRegister(8))
    val pwm_duty_cycle      = Module(new GPIOShiftRegister(8))
    val pwm_prescaler       = Module(new GPIOShiftRegister(8))
    val pwm_div             = Module(new GPIOShiftRegister(8))
    val pwm_period          = Module(new GPIOShiftRegister(8))

    // Instantiation of GPIO modules
    val gpio_module = Seq.tabulate(nofGPIO) { i =>
        val m = Module(new GPIOModule(testMode))
        m.io.gpio_output       := gpio_output(i)
        gpio_input(i)          := m.io.gpio_input
        m.io.gpio_direction    := gpio_direction.io.conf_output(i)
        m.io.drivestrength     := gpio_drivestrength.io.conf_output(i)
        m.io.pullup_en         := gpio_pullup.io.conf_output(i)
        m.io.pulldown_en       := gpio_pulldown.io.conf_output(i)
        m.io.opendrain_en      := gpio_opendrain.io.conf_output(i)
        m.io.pwm_en            := pwm_enable.io.conf_output(i)
        m.io.pwm_div           := pwm_div.io.conf_output(i)
        m.io.duty_cycle        := pwm_duty_cycle.io.conf_output(i)
        m.io.pwm_period        := pwm_period.io.conf_output(i)
        m.io.pwm_polarity      := pwm_polarity.io.conf_output(i)
        
        // connect test ports to the top-level test_ports
        if (testMode) {
            m.io.test_PAD_IN.get := io.test_ports.get.test_PAD_IN(i)
            io.test_ports.get.test_OUT(i) := m.io.test_OUT.get
            io.test_ports.get.test_OE_N(i) := m.io.test_OE_N.get
        }
        
        m // return module explicitly so it can be interfaced through gpio_module(x)
    }

    gpio_direction.io.rd                := false.B
    gpio_direction.io.wr                := false.B
    gpio_direction.io.write_data        := 0.U

    gpio_pullup.io.rd                   := false.B
    gpio_pullup.io.wr                   := false.B
    gpio_pullup.io.write_data           := 0.U

    gpio_pulldown.io.rd                 := false.B
    gpio_pulldown.io.wr                 := false.B
    gpio_pulldown.io.write_data         := 0.U

    gpio_opendrain.io.rd                := false.B
    gpio_opendrain.io.wr                := false.B
    gpio_opendrain.io.write_data        := 0.U

    gpio_drivestrength.io.rd            := false.B
    gpio_drivestrength.io.wr            := false.B
    gpio_drivestrength.io.write_data    := 0.U

    pwm_enable.io.rd                    := false.B
    pwm_enable.io.wr                    := false.B
    pwm_enable.io.write_data            := 0.U

    pwm_duty_cycle.io.rd                := false.B
    pwm_duty_cycle.io.wr                := false.B
    pwm_duty_cycle.io.write_data        := 0.U

    pwm_prescaler.io.rd                 := false.B
    pwm_prescaler.io.wr                 := false.B
    pwm_prescaler.io.write_data         := 0.U

    pwm_div.io.rd                       := false.B
    pwm_div.io.wr                       := false.B
    pwm_div.io.write_data               := 0.U

    pwm_polarity.io.rd                  := false.B
    pwm_polarity.io.wr                  := false.B
    pwm_polarity.io.write_data          := 0.U

    pwm_period.io.rd                    := false.B
    pwm_period.io.wr                    := false.B
    pwm_period.io.write_data            := 0.U

    // Piping read address to statemachine/registers
    val rdAckReg                        = RegInit(false.B)
    val wrAckReg                        = RegInit(false.B)

    object State extends ChiselEnum {
        val idle, conf_or_io, shift_data, read_reg_out, read_parallel_reg = Value
    }

    // Reading state machine
    val para_read_en    = RegInit(true.B)
    val readReg         = RegInit(0.U(addrWidth.W))
    val parallel_read   = Wire(UInt(addrWidth.W))
    val serial_read     = Wire(UInt(1.W))

    parallel_read       := 0.U
    serial_read         := 0.U
    when (para_read_en) {
        readReg         := parallel_read
    }.otherwise {
        readReg         := serial_read ## readReg(nofGPIO-1, 1)
    }

    val read_state_reg  = RegInit(State.idle)
    val n_shift_read    = RegInit(0.U(4.W))

    switch (read_state_reg) {
        is(State.idle) {
            rdAckReg        := false.B
            n_shift_read    := 0.U

            when(io.mem_ifc.rd) {
                when(io.mem_ifc.address === GPIO_INPUT_OFFSET.U(addrWidth.W) ||
                     io.mem_ifc.address === GPIO_OUTPUT_OFFSET.U(addrWidth.W))
                {
                    para_read_en        := true.B
                    read_state_reg      := State.read_parallel_reg
                }.otherwise {
                    para_read_en        := false.B
                    read_state_reg      := State.shift_data
                }
            }
        }
        is(State.read_parallel_reg) {
            when(io.mem_ifc.address === GPIO_INPUT_OFFSET.U(addrWidth.W))
            {
                parallel_read       := Cat(0.U(24.W), gpio_input.asUInt)
            }.otherwise {
                parallel_read       := gpio_output
            }
            rdAckReg                := true.B
            read_state_reg          := State.idle
        }
        is(State.shift_data) {
            n_shift_read            := n_shift_read + 1.U
            switch(io.mem_ifc.address) {
                is(GPIO_DIRECTION_OFFSET.U(addrWidth.W)) {
                    serial_read                 := gpio_direction.io.read_data
                    gpio_direction.io.rd        := true.B
                }
                is(GPIO_PULLUP_OFFSET.U(addrWidth.W)) {
                    serial_read                 := gpio_pullup.io.read_data
                    gpio_pullup.io.rd           := true.B
                }
                is(GPIO_PULLDOWN_OFFSET.U(addrWidth.W)) {
                    serial_read                 := gpio_pulldown.io.read_data
                    gpio_pulldown.io.rd         := true.B
                }
                is(GPIO_OPENDRAIN_OFFSET.U(addrWidth.W)) {
                    serial_read                 := gpio_opendrain.io.read_data
                    gpio_opendrain.io.rd        := true.B
                }
                is(GPIO_DRIVESTRENGTH_OFFSET.U(addrWidth.W)) {
                    serial_read                 := gpio_drivestrength.io.read_data
                    gpio_drivestrength.io.rd    := true.B
                }
                is(PWM_ENABLE_OFFSET.U(addrWidth.W)) {
                    serial_read                 := pwm_enable.io.read_data
                    pwm_enable.io.rd            := true.B
                }
                is(PWM_DUTY_CYCLE_OFFSET.U(addrWidth.W)) {
                    serial_read                 := pwm_duty_cycle.io.read_data
                    pwm_duty_cycle.io.rd        := true.B
                }
                is(PWM_PRESCALER_OFFSET.U(addrWidth.W)) {
                    serial_read                 := pwm_prescaler.io.read_data
                    pwm_prescaler.io.rd         := true.B
                }
            }

            when(n_shift_read === (nofGPIO-1).U) {
                n_shift_read                := 0.U
                read_state_reg              := State.read_reg_out
            }.otherwise {
                read_state_reg              := State.shift_data
            }
        }
        is(State.read_reg_out) {
            gpio_direction.io.rd    := false.B
            read_state_reg          := State.idle
            rdAckReg                := true.B
        }
    }

    // Writing state machine
    val para_write_en       = RegInit(true.B)
    val writeReg            = RegInit(0.U(addrWidth.W))
    val writeAddrReg        = RegInit(0.U(addrWidth.W))

    when (para_write_en) {
        writeReg            := io.mem_ifc.wrData
    }.otherwise {
        writeReg            := writeReg(0) ## writeReg(nofGPIO-1, 1)
    }
    val write_reg_out       = writeReg(0)
    val write_state_reg     = RegInit(State.idle)
    val n_shift_write       = RegInit(0.U(4.W))

    switch (write_state_reg) {
        is(State.idle) {
            wrAckReg                := false.B
            n_shift_write           := 0.U
            para_write_en           := true.B

            writeReg                := io.mem_ifc.wrData
            writeAddrReg            := io.mem_ifc.address

            when(io.mem_ifc.wr) {
                when(io.mem_ifc.address === GPIO_OUTPUT_OFFSET.U(addrWidth.W) ||
                    io.mem_ifc.address === GPIO_INPUT_OFFSET.U(addrWidth.W)) 
                {
                    write_state_reg       := State.read_parallel_reg
                }.otherwise {
                    para_write_en         := false.B
                    write_state_reg       := State.shift_data
                }
            }
        }
        is (State.read_parallel_reg) {
            when(writeAddrReg === GPIO_OUTPUT_OFFSET.U(addrWidth.W))
            {
                gpio_output         := writeReg
            }.otherwise {
                gpio_input          := VecInit(writeReg(7, 0).asBools)
            }
            wrAckReg                := true.B
            write_state_reg         := State.idle
        }
        is(State.shift_data) {
            para_write_en           := false.B
            n_shift_write           := n_shift_write + 1.U

            switch(writeAddrReg) {
                is(GPIO_DIRECTION_OFFSET.U(addrWidth.W)) {
                    gpio_direction.io.wr                := true.B
                    gpio_direction.io.write_data        := write_reg_out
                }
                is(GPIO_PULLUP_OFFSET.U(addrWidth.W)) {
                    gpio_pullup.io.wr                   := true.B
                    gpio_pullup.io.write_data           := write_reg_out
                }
                is(GPIO_PULLDOWN_OFFSET.U(addrWidth.W)) {
                    gpio_pulldown.io.wr                 := true.B
                    gpio_pulldown.io.write_data         := write_reg_out
                }
                is(GPIO_OPENDRAIN_OFFSET.U(addrWidth.W)) {
                    gpio_opendrain.io.wr                := true.B
                    gpio_opendrain.io.write_data        := write_reg_out
                }
                is(GPIO_DRIVESTRENGTH_OFFSET.U(addrWidth.W)) {
                    gpio_drivestrength.io.wr            := true.B
                    gpio_drivestrength.io.write_data    := write_reg_out
                }
                is(PWM_ENABLE_OFFSET.U(addrWidth.W)) {
                    pwm_enable.io.wr                    := true.B
                    pwm_enable.io.write_data            := write_reg_out
                }
                is(PWM_DUTY_CYCLE_OFFSET.U(addrWidth.W)) {
                    pwm_duty_cycle.io.wr                := true.B
                    pwm_duty_cycle.io.write_data        := write_reg_out
                }
                is(PWM_PRESCALER_OFFSET.U(addrWidth.W)) {
                    pwm_prescaler.io.wr                 := true.B
                    pwm_prescaler.io.write_data         := write_reg_out
                }
            }

            when(n_shift_write === (nofGPIO-1).U) {
                gpio_direction.io.wr        := false.B
                n_shift_write               := 0.U
                write_state_reg             := State.read_reg_out
            }.otherwise {
                write_state_reg             := State.shift_data
            }

        }
        is(State.read_reg_out) {
            gpio_direction.io.wr            := false.B
            wrAckReg                        := true.B
            write_state_reg                 := State.idle
        }
    }

    // Piping ack signal to the output
    io.mem_ifc.ack          := rdAckReg || wrAckReg
    io.mem_ifc.rdData       := readReg
}

object GPIOPeripheral extends App {
  emitVerilog(new GPIOPeripheral(
    addrWidth = 32,
    nofGPIO = 8),
    Array("--target-dir", "generated")
    )
}
