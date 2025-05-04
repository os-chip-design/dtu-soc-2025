import chisel3._
import chisel3.util._

import VgaConfig._

/** Top-level IO for the synchronous VGA Controller
  */
class VgaControllerIo extends Bundle {
  // Inputs to write to character buffer, NOT ASYNCHRONOUS
  val wrAddr = Input(UInt(CHAR_BUFFER_ADDR_WIDTH.W)) // Address to write to
  val wrData = Input(UInt(16.W)) // Standard 16-bit for char+attribute
  val wrEnable = Input(Bool()) // Write enable signal (active high)

  // --- Standard VGA Outputs (Synchronous to main clock) ---
  val hSync = Output(Bool())
  val vSync = Output(Bool())
  val r = Output(UInt(4.W))
  val g = Output(UInt(4.W))
  val b = Output(UInt(4.W))
}

/** Main VGA Controller Module (Synchronous, No Divider/FIFO), should have an
  * input clock of 25MHZ, and if written from main CPU, an Async Queue is needed
  * to cross the clock boundry
  */
class VgaController extends Module {
  val io = IO(new VgaControllerIo)

  // External modules
  val timer = Module(new VgaTimer)
  val charIndexer = Module(new VgaCharacterIndexer)
  val charBuffer = Module(new CharacterBuffer())
  val fontRom = Module(
    new VgaFontRom("src/main/scala/VgaController/fonts/VGA8.F16")
  )
  val pixelRenderer = Module(new VgaPixelRenderer())

  timer.io.timerEn := true.B

  // Connect charBuffer inputs directly from module IO
  charBuffer.io.write.enable := io.wrEnable
  charBuffer.io.write.addr := io.wrAddr
  charBuffer.io.write.data := io.wrData

  // --- Internal VGA Pipeline ---
  charIndexer.io.pixelX := timer.io.pixelX
  charIndexer.io.pixelY := timer.io.pixelY
  charIndexer.io.hActive := timer.io.hActive
  charIndexer.io.vActive := timer.io.vActive

  // Connect CharIndexer to memory read ports
  charBuffer.io.read.addr := charIndexer.io.charBaseAddr
  fontRom.io.glyphY := charIndexer.io.yCharIndex

  // Connect memory read outputs (handle 1-cycle latency)
  val charBufDataOut = charBuffer.io.read.data // Data available next cycle
  fontRom.io.charCode := charBufDataOut(7, 0)
  pixelRenderer.io.attributeData := charBufDataOut(15, 8)

  pixelRenderer.io.xIndex := charIndexer.io.xCharIndex
  val fontDataOut = fontRom.io.pixelData // Data available next cycle
  pixelRenderer.io.pixelData := fontDataOut

  pixelRenderer.io.activeArea := timer.io.hActive && timer.io.vActive

  io.r := pixelRenderer.io.r
  io.g := pixelRenderer.io.g
  io.b := pixelRenderer.io.b
  io.hSync := timer.io.hSync
  io.vSync := timer.io.vSync

}

object VgaController extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new VgaController())
}
