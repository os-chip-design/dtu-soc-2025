import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GPIOPeripheralTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "GPIOPeripheral test"

    it should "pass" in {
        test(
            new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                )
            ) {
            dut => {
                
                // Read register address space and check the rd values
                // for (i <- 0 until 7) {
                //     dut.io.mem_ifc.address.poke((i*8).U)

                //     dut.io.mem_ifc.rdData.expect((i+1).U)

                //     dut.clock.step()
                // }

                // Read some registers
                dut.io.mem_ifc.address.poke(0x0000.U(32.W))
                dut.io.mem_ifc.rdData.expect(0.U) // Check GPIO direction register
                dut.clock.step()
                dut.io.mem_ifc.address.poke(0x0008.U(32.W))
                dut.io.mem_ifc.rdData.expect(0xF.U) // Check GPIO output register

                //dut.gpio_module(0).io.gpio_output.expect(0.U) // Check GPIO output

                dut.clock.step()

            }
        }
    }
}