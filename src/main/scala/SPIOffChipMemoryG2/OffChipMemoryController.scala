import chisel3._
import chisel3.util._

// Main module for the off-chip memory controller
// This module is responsible for managing the communication with off-chip memories (1x 16MB flash and 2x 8B RAMs)

// pipeConFlash, pipeConRam1 and pipeConRam2 are the interfaces to the off-chip memories
// pipeConConfig is the interface to the configuration register (see OffChipMemoryConfig.scala for more details on fields)

// mcSpiPort is the SPI port that connects to the off-chip memories
// The chip select lines are used to select the target memory chip for the current operation

// The flash control can be reconfigured to target the RAMs instead of the flash memory (using the same chip select line)

// NB: Back to Back requests when switching between chips are currently not supported and result in undefined behavior
//    (e.g. if you want to read from flash and then write to RAM1, you need to wait for the cycle after the ack of the read to send the next request)
//    Back to back requests are supported for the same chip (e.g. if you want to read from RAM1 and write to RAM1, you can start the write request right after the ack of the read request)

class OffChipMemoryController() extends Module {
  val mcSpiPort = IO(new spiMultiChipIO(3))     // 3 chips in order: flash, RAM1, RAM2
  val pipeConFlash = IO(new PipeCon(24)) // 24 bits address for the flash
  val pipeConRam1 = IO(new PipeCon(23))  // 23 bits address for RAM1
  val pipeConRam2 = IO(new PipeCon(23))  // 23 bits address for RAM2 
  val pipeConConfig = IO(new PipeCon(1))  // 1 bit address for the configuration register
  
  val bridge = Module(new Bridge(10, 24))

  val config = Module(new OffChipMemoryConfig)
  
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
    
  mcSpiPort.chipSelect(0) := true.B  // Flash chip select
  mcSpiPort.chipSelect(1) := true.B  // RAM1 chip select
  mcSpiPort.chipSelect(2) := true.B  // RAM2 chip select
  mcSpiPort.spiClk := bridge.spiPort.spiClk
  bridge.spiPort.dataIn := mcSpiPort.dataIn
  mcSpiPort.dataOut := bridge.spiPort.dataOut

  pipeConFlash.rdData := 0.U
  pipeConRam1.rdData := 0.U
  pipeConRam2.rdData := 0.U
  pipeConConfig.rdData := 0.U

  pipeConFlash.ack := false.B
  pipeConRam1.ack := false.B
  pipeConRam2.ack := false.B
  pipeConConfig.ack := false.B

  config.config <> bridge.config

  val requestFlash  = pipeConFlash.rd  || pipeConFlash.wr
  val requestRam1   = pipeConRam1.rd   || pipeConRam1.wr
  val requestRam2   = pipeConRam2.rd   || pipeConRam2.wr
  val requestConfig = pipeConConfig.rd || pipeConConfig.wr

  val requestFlashReg = RegInit(false.B)
  val requestRam1Reg = RegInit(false.B)
  val requestRam2Reg = RegInit(false.B)
  val requestConfigReg = RegInit(false.B)

  // Looking at the requests on the pipeConFlash, pipeConRam1, pipeConRam2 and pipeConConfig
  // and forwarding them to the bridge / config   
  when(requestFlashReg || requestFlash) {
    pipeConFlash <> bridge.pipeCon
    mcSpiPort.chipSelect(0) := bridge.spiPort.chipSelect
  }.elsewhen(requestRam1Reg || requestRam1) {
    pipeConRam1 <> bridge.pipeCon
    mcSpiPort.chipSelect(1) := bridge.spiPort.chipSelect
    bridge.config.targetFlash := false.B // Always disable targetting flash for RAM1 and RAM2
  }.elsewhen(requestRam2Reg || requestRam2) {
    pipeConRam2 <> bridge.pipeCon
    mcSpiPort.chipSelect(2) := bridge.spiPort.chipSelect
    bridge.config.targetFlash := false.B // Always disable targetting flash for RAM1 and RAM2
  }.elsewhen(requestConfigReg || requestConfig) {
    pipeConConfig <> config.pipeCon
  } 

  when (requestFlashReg && pipeConFlash.ack){
    requestFlashReg := false.B
  }
  when (requestRam1Reg && pipeConRam1.ack){
    requestRam1Reg := false.B
  }
  when (requestRam2Reg && pipeConRam2.ack){
    requestRam2Reg := false.B
  }
  when (requestConfigReg && pipeConConfig.ack){
    requestConfigReg := false.B
  }
  when (requestFlash) {
    requestFlashReg := true.B
  }
  when (requestRam1) {
    requestRam1Reg := true.B
  }
  when (requestRam2) {
    requestRam2Reg := true.B
  }
  when (requestConfig) {
    requestConfigReg := true.B
  }
}

object OffChipMemoryControllerSynth extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new OffChipMemoryController(), Array("--target-dir", "generated"))
}