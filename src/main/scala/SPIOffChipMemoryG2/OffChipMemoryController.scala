import chisel3._
import chisel3.util._

// Main module for the off-chip memory controller
class OffChipMemoryController(clockWidth: Int, addrWidth: Int) extends Module {
  
  val spiPort = IO(new spiIO)
  val pipeCon = IO(new PipeCon(addrWidth))

  val bridge = Module(new Bridge(clockWidth, addrWidth))
  

  val config = Module(new OffChipMemoryConfig(clockWidth, addrWidth))
  
  // Default values for the configuration
  config.pipeCon.address := 0.U
  config.pipeCon.rd      := 0.U
  config.pipeCon.wr      := 0.U
  config.pipeCon.wrData  := 0.U
  config.pipeCon.wrMask  := 0.U

  // Default values for the bridge
  bridge.pipeCon.address := 0.U
  bridge.pipeCon.rd      := 0.U
  bridge.pipeCon.wr      := 0.U
  bridge.pipeCon.wrData  := 0.U
  bridge.pipeCon.wrMask  := 0.U
  
  val configAddr = "h00000000".U(24.W)
  spiPort <> bridge.spiPort
  bridge.config <> config.config


  when (pipeCon.address === configAddr){
    pipeCon <> config.pipeCon
  }.otherwise {
    pipeCon <> bridge.pipeCon
  }
}
