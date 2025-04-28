import chisel3._
import chisel3.util._

// Main module for the off-chip memory controller
class OffChipMemoryController() extends Module {
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(24))

  val bridge = Module(new Bridge())
  spiPort <> bridge.spiPort

  val config = Module(new OffChipMemoryConfig())

  val configAddr = "h00000000".U(24.W)
  bridge.config <> config.config

  when (pipeCon.address === configAddr){
    pipeCon <> config.pipeCon
  }.otherwise {
    pipeCon <> bridge.pipeCon
  }
}
