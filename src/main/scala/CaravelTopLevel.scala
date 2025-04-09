import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

/*
module user_project_wrapper #(
    parameter BITS = 32
) (
`ifdef USE_POWER_PINS
    inout vdda1,	// User area 1 3.3V supply
    inout vdda2,	// User area 2 3.3V supply
    inout vssa1,	// User area 1 analog ground
    inout vssa2,	// User area 2 analog ground
    inout vccd1,	// User area 1 1.8V supply
    inout vccd2,	// User area 2 1.8v supply
    inout vssd1,	// User area 1 digital ground
    inout vssd2,	// User area 2 digital ground
`endif

    // Wishbone Slave ports (WB MI A)
    input wb_clk_i,
    input wb_rst_i,
    input wbs_stb_i,
    input wbs_cyc_i,
    input wbs_we_i,
    input [3:0] wbs_sel_i,
    input [31:0] wbs_dat_i,
    input [31:0] wbs_adr_i,
    output wbs_ack_o,
    output [31:0] wbs_dat_o,

    // Logic Analyzer Signals
    input  [127:0] la_data_in,
    output [127:0] la_data_out,
    input  [127:0] la_oenb,

    // IOs
    input  [`MPRJ_IO_PADS-1:0] io_in,
    output [`MPRJ_IO_PADS-1:0] io_out,
    output [`MPRJ_IO_PADS-1:0] io_oeb,

    // Analog (direct connection to GPIO pad---use with caution)
    // Note that analog I/O is not available on the 7 lowest-numbered
    // GPIO pads, and so the analog_io indexing is offset from the
    // GPIO indexing by 7 (also upper 2 GPIOs do not have analog_io).
    inout [`MPRJ_IO_PADS-10:0] analog_io,

    // Independent clock (on independent integer divider)
    input   user_clock2,

    // User maskable interrupt signals
    output [2:0] user_irq
); 
*/

class CaravelIO(MPRJ_IO_PADS: Int = 38, USE_POWER_PINS: Boolean = false) extends Bundle {
  val vdda1: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vdda2: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vssa1: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vssa2: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vccd1: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vccd2: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vssd1: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  val vssd2: Option[Analog] = if (USE_POWER_PINS) Some(Analog(1.W)) else None
  // Wishbone
  val wb_clk_i: Clock = Input(Clock())
  val wb_rst_i: Bool = Input(Bool())
  val wbs_stb_i: Bool = Input(Bool())
  val wbs_cyc_i: Bool = Input(Bool())
  val wbs_we_i: Bool = Input(Bool())
  val wbs_sel_i: UInt = Input(UInt(4.W))
  val wbs_dat_i: UInt = Input(UInt(32.W))
  val wbs_adr_i: UInt = Input(UInt(32.W))
  val wbs_ack_o: Bool = Output(Bool())
  val wbs_dat_o: UInt = Output(UInt(32.W))
  // Logic Analyzer
  val la_data_in: UInt = Input(UInt(128.W))
  val la_data_out: UInt = Output(UInt(128.W))
  val la_oenb: UInt = Input(UInt(128.W))
  // GPIO
  val io_in: UInt = Input(UInt(MPRJ_IO_PADS.W))
  val io_out: UInt = Output(UInt(MPRJ_IO_PADS.W))
  val io_oeb: UInt = Output(UInt(MPRJ_IO_PADS.W))
  // Analog IO (offset by 7)
  val analog_io: Vec[Analog] = Vec(MPRJ_IO_PADS - 10, Analog(1.W))
  // Independent clock
  val user_clock2: Clock = Input(Clock())
  // IRQs
  val user_irq: UInt = Output(UInt(3.W))
}

class CaravelTopLevel extends RawModule {
  val io = IO(new CaravelIO(MPRJ_IO_PADS = 38, USE_POWER_PINS = true))

  // Enumerate all outputs as DontCare
  io.wbs_ack_o := DontCare
  io.wbs_dat_o := DontCare
  io.la_data_out := DontCare
  io.io_out := DontCare
  io.io_oeb := DontCare
  io.user_irq := DontCare

  // Analog logic here.

  // Digital logic inside this block.
  withClockAndReset(io.wb_clk_i, io.wb_rst_i) {

  }
}

object CaravelTopLevel extends App {
  emitVerilog(new CaravelTopLevel, Array("--target-dir", "generated"))
}
