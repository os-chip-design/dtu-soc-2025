import chisel3._
import breeze.numerics.pow
class OffChipMemoryControllerWrapper()extends Module {
    val addrWidth = 24

    val mcSpiPort = IO(new spiMultiChipIO(3))     // 3 chips in order: flash, RAM1, RAM2
    val pipeConFlash = IO(new PipeCon(addrWidth)) // 24 bits address for the flash
    val pipeConRam1 = IO(new PipeCon(addrWidth-1))  // 23 bits address for RAM1
    val pipeConRam2 = IO(new PipeCon(addrWidth-1))  // 23 bits address for RAM2
    val pipeConConfig = IO(new PipeCon(1))  // 1 bit address for the configuration register

    val controller = Module(new OffChipMemoryController())

    mcSpiPort <> controller.mcSpiPort
    pipeConFlash <> controller.pipeConFlash
    pipeConRam1 <> controller.pipeConRam1
    pipeConRam2 <> controller.pipeConRam2
    pipeConConfig <> controller.pipeConConfig

    assert(!(pipeConFlash.rd == 1 && pipeConFlash.wr == 1))
    assert(!(pipeConRam1.rd == 1 && pipeConRam1.wr == 1))
    assert(!(pipeConRam2.rd == 1 && pipeConRam2.wr == 1))
    assert(!(pipeConConfig.rd == 1 && pipeConConfig.wr == 1))
    assert(!(mcSpiPort.chipSelect(0) == 0 && mcSpiPort.chipSelect(1) == 0 && mcSpiPort.chipSelect(2) == 0))
}
