import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GPIOPeripheralTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "GPIOPeripheral test"

    it should "pass" in {
        test(
            new GPIOPeripheral(
                    addrWidth = 32
                )
            ) {
            dut => {
                
                // Read register address space and check the rd values
                for (i <- 0 until 7) {
                    dut.io.mem_ifc.address.poke((i*8).U)

                    dut.io.mem_ifc.rdData.expect((i+1).U)

                    dut.clock.step()
                }

            }
        }
    }
}