import chisel3._
import breeze.numerics.pow
class SPIOffChipMemoryControllerWrapper( 
    val addrWidth: Int = 24,
    val dataWidth: Int = 32,
    val spiFreq: Int = 1000000,
    val freq: Int = 50000000,
    val configuredIntoQSPI: Boolean = false
)extends Module {
    val interconnectPort = IO(new PipeCon(addrWidth))
    val qspiPort = IO(new qspiIO)
    val m = Module(new SPIOffChipMemoryController(addrWidth, dataWidth, spiFreq, freq, configuredIntoQSPI))

    val MAXAddr = pow(2, addrWidth).toInt.U
    val MINAddr = 0.U //TODO: Figure out the correct restraints
    interconnectPort <> m.interconnectPort
    qspiPort <> m.qspiPort
    assert(!(interconnectPort.rd == 1 && interconnectPort.wr == 1))
    assert(m.addrWidth == interconnectPort.address.getWidth)
    assert(m.dataWidth == interconnectPort.wrData.getWidth)
    assert(m.dataWidth == interconnectPort.rdData.getWidth)

    when(interconnectPort.rd === 1.U){
        assert(interconnectPort.address <= MAXAddr)
        assert(interconnectPort.address >= MINAddr)
        assert(interconnectPort.wrData === 0.U)
    }
    when(interconnectPort.wr === 1.U){
        assert(interconnectPort.address <= MAXAddr)
        assert(interconnectPort.address >= MINAddr)
        assert(interconnectPort.rdData === 0.U)
    }
}