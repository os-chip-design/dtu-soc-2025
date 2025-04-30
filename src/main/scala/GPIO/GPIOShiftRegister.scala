import chisel3._
import chisel3.util._

// This is a shift register module
// specifically designed to accomodate the GPIO peripheral
// 
// The idea is to avoid having 32 wires for each configuration
// and instead use a shift register to shift in the values
// trading registers for wires going across the chip and
// instead only have wires between the registers daisy chained


class GPIOShiftRegister(bit_width: Int) extends Module {
    val io = IO(new Bundle {
        val read_data          = Output(Bool())
        val write_data         = Input(Bool())
        val rd                 = Input(Bool())
        val wr                 = Input(Bool())
        val conf_output        = Output(Vec(bit_width, Bool()))
    })

    val en_shifting = (io.rd | io.wr)

    val read_write_mux = Mux(io.rd, io.read_data, io.write_data)

    val shift_reg = RegInit(0.U(bit_width.W))

    when (en_shifting) {
        shift_reg := shift_reg(bit_width-1, 0) ## read_write_mux
    }
    io.read_data := shift_reg(bit_width-1)

    // val out_regs = Reg(Vec(32, Bool()))
    // for (i <- 0 until bit_width) {
    //     out_regs(i) := RegEnable(shift_reg(i), ~en_shifting)
    // }
    // io.conf_output := out_regs

    for (i <- 0 until bit_width) {
        io.conf_output(i) := RegEnable(shift_reg(i), ~en_shifting)
    }

}

object GPIOShiftRegister extends App {
    emitVerilog(new GPIOShiftRegister(
        bit_width = 32),
        Array("--target-dir", "generated")
    )
}