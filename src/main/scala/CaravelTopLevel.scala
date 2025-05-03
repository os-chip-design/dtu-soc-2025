import chisel3._
import chisel3.util._
import chisel3.experimental.Analog
import wildcat.pipeline.ThreeCats

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

  // Independent clock
  val user_clock2: Clock = Input(Clock())
  // IRQs
  val user_irq: UInt = Output(UInt(3.W))
}

class CaravelTopLevel extends RawModule {
  val io = IO(new Bundle{
    val caravel = new CaravelIO(MPRJ_IO_PADS = 38, USE_POWER_PINS = true)
    val mem = new NativeMemoryInterface(DATA_WIDTH = 32, ADDR_WIDTH = 9, WMASK_WIDTH = 4)
  })

  // Enumerate all outputs as DontCare
  io.caravel.wbs_ack_o := DontCare
  io.caravel.wbs_dat_o := DontCare
  io.caravel.la_data_out := DontCare
  io.caravel.io_out := DontCare
  io.caravel.io_oeb := DontCare
  io.caravel.user_irq := DontCare

  // Analog logic here.

  // Digital logic inside this block.

  /**
   * Conventional LA selection of signals.
   *
   * assign la_data_out = {{(128-BITS){1'b0}}, count};
   * // Assuming LA probes [63:32] are for controlling the count register  
   * assign la_write = ~la_oenb[63:64-BITS] & ~{BITS{valid}};
   * // Assuming LA probes [65:64] are for controlling the count clk & reset  
   * assign clk = (~la_oenb[64]) ? la_data_in[64]: wb_clk_i;
   * assign rst = (~la_oenb[65]) ? la_data_in[65]: wb_rst_i;
   */

  private val clk = Wire(Clock())
  private val rst = Wire(Bool())

  //when(io.la_oenb(64)) {
  //  clk := io.la_data_in(64).asClock
  // }.otherwise {
  clk := io.wb_clk_i
  // }

  // when(io.la_oenb(65)) {
  //  rst := io.la_data_in(65)
  // }.otherwise {
  rst := io.wb_rst_i
  // }

  private val uartRx = Wire(Bool())
  private val uartTx = Wire(Bool())

  // ser_rx (e7)
  uartRx := io.io_in(5)
  // ser_tx (f7)
  uartTx := io.io_out(6)

  io.wbs_ack_o := 0.U
  io.wbs_dat_o := 0.U

  io.io_out := 0.U
  io.io_oeb := 0.U

  withClockAndReset(clk, rst) {
    val topLevel = Module(new TopLevel())
    
    topLevel.io.imem <> DontCare

    topLevel.io.uartRx := uartRx
    uartTx := topLevel.io.uartTx

    topLevel.io.gpio_in := io.io_in(37, 30)
    // io.io_out(37, 30) := topLevel.io.gpio_out
    io.io_out := Cat(0.U(30.W), topLevel.io.gpio_out)
    //io.io_oeb(37, 30) := topLevel.io.gpio_oeb
    io.io_oeb := Cat(0.U(30.W), topLevel.io.gpio_oeb)
    io.caravel.la_data_out := !rst.asUInt

    val mem2pipe = Module(new NativeMemory2Pipecon(DATA_WIDTH = 32, ADDR_WIDTH = 9, WMASK_WIDTH = 4))

    mem2pipe.io.mem <> io.mem
    // Temp: tie outputs to 0
    mem2pipe.io.pipe.address := 0.U
    mem2pipe.io.pipe.rd := false.B
    mem2pipe.io.pipe.wr := false.B
    mem2pipe.io.pipe.wrData := 0.U
    mem2pipe.io.pipe.wrMask := 0.U
  }
}

object CaravelTopLevel extends App {
  emitVerilog(new CaravelTopLevel, Array("--target-dir", "generated"))
}
