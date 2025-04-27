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
            ).withAnnotations(Seq( WriteVcdAnnotation )) {
            dut => {
                
                // Read register address space and check the rd values
                // for (i <- 0 until 7) {
                //     dut.io.mem_ifc.address.poke((i*8).U)

                //     dut.io.mem_ifc.rdData.expect((i+1).U)

                //     dut.clock.step()
                // }

                // Read some registers
                dut.io.mem_ifc.address.poke(0x0000.U(32.W))
                dut.io.mem_ifc.rdData.expect(0.U)
                dut.clock.step()
                dut.io.mem_ifc.rdData.expect(0.U)


                // Read
                dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                dut.io.mem_ifc.rdData.expect(0.U)           // Check that return is still the previous value
                dut.clock.step()                            // Step a clock cycle
                dut.io.mem_ifc.rdData.expect(0xF.U)         // Check that return is now the new value
                dut.io.mem_ifc.rd.poke(false.B)             // Set read signal

                dut.clock.step()

                // Write
                dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
                dut.io.mem_ifc.wrData.poke(0xAA.U)           // Set write data
                dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
                dut.clock.step()
                dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
                dut.clock.step()


                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                dut.clock.step()
                dut.io.mem_ifc.rdData.expect(0xAA.U)         // Check that return is now the new value
                dut.io.mem_ifc.rd.poke(false.B)             // Set read signal

                //dut.gpio_module(0).io.gpio_output.expect(0.U) // Check GPIO output

                dut.clock.step()

            }
        }
    }
}