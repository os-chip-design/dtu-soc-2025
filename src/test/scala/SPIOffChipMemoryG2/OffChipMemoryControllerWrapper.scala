import chisel3._
import breeze.numerics.pow
class OffChipMemoryControllerWrapper()extends Module {
    val addrWidth = 24
    val dataWidth = 32
    val maskWidth = 4
    val clockWidth = 8

    val spiPort = IO(new spiIO)
    val pipeCon = IO(new PipeCon(addrWidth))
    val m = Module(new OffChipMemoryController(clockWidth, addrWidth))

    val MAXAddr = pow(2, addrWidth).toInt.U
    val MINAddr = 0.U //TODO: Figure out the correct restraints
    pipeCon <> m.pipeCon
    spiPort <> m.spiPort


    assert(!(pipeCon.rd == 1 && pipeCon.wr == 1))
    assert(addrWidth == pipeCon.address.getWidth)
    assert(dataWidth == pipeCon.wrData.getWidth)
    assert(dataWidth == pipeCon.rdData.getWidth)
    assert(maskWidth == pipeCon.wrMask.getWidth)
    
    when(pipeCon.ack === 1.U){
        when(pipeCon.rd === 1.U){
            assert(pipeCon.address <= MAXAddr)
            assert(pipeCon.address >= MINAddr)
            assert(pipeCon.wrData === 0.U)
        }
        when(pipeCon.wr === 1.U){
            assert(pipeCon.address <= MAXAddr)
            assert(pipeCon.address >= MINAddr)
            assert(pipeCon.rdData === 0.U)
        }
    }

}
