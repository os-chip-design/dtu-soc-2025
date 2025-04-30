import chisel3._
import breeze.numerics.pow

class BridgeWrapper() extends Module{
    val addrWidth = 24
    val dataWidth = 32
    val maskWidth = 4
    val clockWidth = 10

    val spiPort = IO(new spiIO)
    val pipeCon = IO(new PipeCon(addrWidth))
    val config  = IO(new configIO(clockWidth))
    val m = Module(new Bridge(clockWidth,addrWidth))

    val MAXAddr = pow(2, addrWidth).toInt.U
    val MINAddr = 0.U 
    pipeCon <> m.pipeCon
    spiPort <> m.spiPort
    config <> m.config
    assert(!(config.clockDivision === 0.U))
    assert(!(pipeCon.rd === 1.U && pipeCon.wr === 1.U))
    assert(addrWidth == pipeCon.address.getWidth)
    assert(dataWidth == pipeCon.wrData.getWidth)
    assert(dataWidth == pipeCon.rdData.getWidth)
    assert(maskWidth == pipeCon.wrMask.getWidth)

    when(pipeCon.ack === 1.U){
        // TODO check validity with protocol
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
