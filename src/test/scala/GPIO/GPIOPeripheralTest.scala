import chisel3._
import chisel3.util._
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

                // Read
                dut.clock.step()
                dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                dut.io.mem_ifc.rdData.expect(0.U)           // Check that return is still the previous value
                //dut.clock.step()                            // Step a clock cycle
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                dut.io.mem_ifc.rdData.expect(0xF.U)         // Check that return is now the new value
                dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
                dut.clock.step()


                // Read gpio direction
                dut.io.mem_ifc.address.poke(0x0000.U(32.W)) // Set address
                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                dut.io.mem_ifc.rd.poke(false.B)
                dut.clock.step(2)

               //  // Another read
               //  dut.io.mem_ifc.address.poke(0x0028.U(32.W))
               //  dut.io.mem_ifc.rd.poke(true.B)
               //  while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
               //       dut.clock.step()
               //  }
               //  dut.io.mem_ifc.rdData.expect(0xA.U)
               //  dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               //  dut.clock.step()

                // Write to paralel gpio
                dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
                dut.io.mem_ifc.wrData.poke(0xA.U)           // Set write data
                dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
                dut.clock.step()

                // read back
                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                dut.io.mem_ifc.rdData.expect(0xA.U)         // Check that return is now the new value
                dut.io.mem_ifc.rd.poke(false.B)             // Set read signal

                // Write to GPIO direction
                dut.clock.step(2)
                dut.io.mem_ifc.address.poke(0x0000.U(32.W)) // Set address
                dut.io.mem_ifc.wrData.poke(0x55.U)           // Set write data
                dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
                dut.clock.step()

                // read back
                dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
                while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                     dut.clock.step()
                }
                //dut.io.mem_ifc.rdData.expect(0x55.U)         // Check that return is now the new value
               //  val expect_values = VecInit(0x55.U(8.W).asBools)
               //  for (i <- 0 until 8) {
               //      dut.gpio_direction.io.conf_output(i).expect(expect_values(i)) // Check that return is now the new value
               //  }
                //dut.gpio_direction.io.conf_output.expect(expect_values)         // Check that return is now the new value
                //val expect_values = 0x55.U(8.W)
                //dut.gpio_direction.io.conf_output.asUInt.expect(expect_values)

               //  for (i <- 0 until 8) {
               //      dut.gpio_direction.io.conf_output(i).expect( (i % 2) ) // Check that return is now the new value
               //  }

               //println(s"GPIO direction: ${dut.gpio_direction.io.conf_output.peek()}")

               //dut.gpio_direction.io.conf_output(0).expect(Seq(0.B, 1.B, 0.B, 1.B, 0.B, 1.B, 0.B, 1.B)) // Check that return is now the new value
               // val expected_values = Seq(0.B, 1.B, 0.B, 1.B, 0.B, 1.B, 0.B, 1.B)
               // for (i <- expected_values.indices) {
               //     dut.gpio_direction.io.conf_output(i).expect(expected_values(i)) // Check that return is now the new value
               // }
               dut.io.mem_ifc.rdData.expect(0x55.U)

               dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               dut.clock.step(2)

               // Write to GPIO output
               dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
               dut.io.mem_ifc.wrData.poke(0x55.U)           // Set write data
               dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)             // Set write signal

               dut.clock.step()

               //dut.gpio_module(0).io.gpio_output.expect(0.U) // Check GPIO output

               //  dut.clock.step()

               //  // Write to invalid address
               //  dut.io.mem_ifc.address.poke(0x0012.U(32.W)) // Set address
               //  dut.io.mem_ifc.wrData.poke(0x55.U)           // Set write data
               //  dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
               //  while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
               //       dut.clock.step()
               //  }
               //  dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
               //  dut.clock.step()
               //  dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
               //  while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
               //       dut.clock.step()
               //  }
               //  dut.io.mem_ifc.rdData.expect(0x00.U)         // Check that return is still the previous value
               //  dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               //  dut.clock.step()

            }
        }
    }
}