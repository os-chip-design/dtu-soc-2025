import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class GPIOPeripheralTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "GPIOPeripheral test"

    it should "read and write to registers" in {
        test(
            new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                )
            ).withAnnotations(Seq( WriteVcdAnnotation )) {
            dut => {

               // Read gpio output state
               dut.clock.step()
               dut.io.mem_ifc.address.poke(0x0008.U(32.W))
               dut.io.mem_ifc.rd.poke(true.B)
               dut.io.mem_ifc.rdData.expect(0.U)
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0xF.U)
               dut.io.mem_ifc.rd.poke(false.B)
               dut.clock.step()

               // Read gpio direction
               dut.io.mem_ifc.address.poke(0x0000.U(32.W))
               dut.io.mem_ifc.rd.poke(true.B)
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rd.poke(false.B)
               dut.clock.step(2)

               // Write to gpio output ("parallel" operation, i.e. instant ack)
               dut.io.mem_ifc.address.poke(0x0008.U(32.W))
               dut.io.mem_ifc.wrData.poke(0xA.U)
               dut.io.mem_ifc.wr.poke(true.B)
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)
               dut.clock.step()

               // read back the written
               dut.io.mem_ifc.rd.poke(true.B)
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0xA.U)
               dut.io.mem_ifc.rd.poke(false.B)

               // Write to GPIO direction ("seriel" operation, i.e. delayed ack)
               dut.clock.step(2)
               dut.io.mem_ifc.address.poke(0x0000.U(32.W))
               dut.io.mem_ifc.wrData.poke(0x5.U)
               dut.io.mem_ifc.wr.poke(true.B)
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)
               dut.clock.step()

               // read back
               dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0x5.U)

               dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               dut.clock.step(2)

               // Write to GPIO output again with new value
               dut.io.mem_ifc.address.poke(0x0008.U(32.W)) // Set address
               dut.io.mem_ifc.wrData.poke(0x5.U)           // Set write data
               dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)             // Set write signal

               dut.clock.step()

               // read back again
               dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0x5.U)        // Check read data
               dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               dut.clock.step(2)

               // Write to gpio direction again with new value
               dut.io.mem_ifc.address.poke(0x0000.U(32.W)) // Set address
               dut.io.mem_ifc.wrData.poke(0xA.U)           // Set write data
               dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
               dut.clock.step(2)
               // read back again
               dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0xA.U)        // Check read data
               dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               dut.clock.step(2)

               // Write to pwm serial register
               dut.io.mem_ifc.address.poke(0x0110.U(32.W)) // Set address
               dut.io.mem_ifc.wrData.poke(0xA.U)           // Set write data
               dut.io.mem_ifc.wr.poke(true.B)              // Set write signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.wr.poke(false.B)             // Set write signal
               dut.clock.step()
               // read back again
               dut.io.mem_ifc.rd.poke(true.B)              // Set read signal
               while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                    dut.clock.step()
               }
               dut.io.mem_ifc.rdData.expect(0x5.U)        // Check read data
               dut.io.mem_ifc.rd.poke(false.B)             // Set read signal
               dut.clock.step(2)
            }
        }
    }

    it should "propagate register configuration to gpios after acknowledge" in {
          test(
               new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                    )
               ).withAnnotations(Seq( WriteVcdAnnotation )) {
               dut => {
                    // Read gpio pin state
                    for (i <- 0 until 8) {
                         //println(s"GPIO $i output: " + dut.gpio_module(2).io.gpio_input.peek().litValue)
                         //println(s"GPIO $i output: " + dut.gpio_direction.io.read_data.peek().litValue)
                    }

                    // configure gpio to output
                    dut.io.mem_ifc.address.poke(0x0000.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0xF.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    // read output states to verify they are zero
                    // TBD.

                    // Write to gpio output ("parallel" operation, i.e. instant ack)
                    dut.io.mem_ifc.address.poke(0x0008.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0xA.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    // read outputs to verify pin change
                    // TBD.


               }
          }
     }
     it should "register correct input pins" in {
          test(
               new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                    )
               ).withAnnotations(Seq( WriteVcdAnnotation )) {
               dut => {

                    // read input register
                    dut.io.mem_ifc.address.poke(0x0010.U(32.W))
                    dut.io.mem_ifc.rd.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.rdData.expect(0x0.U)

                    // apply input to gpio
                    // TBD.

                    // read input register
                    dut.io.mem_ifc.rd.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.rdData.expect(0x0.U) // change value when tbd impl

               }

          }
     }
     it should "generate pwm signal based on register" in {
          test(
               new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                    )
               ).withAnnotations(Seq( WriteVcdAnnotation )) {
               dut => {
                    // configure pwm
                    dut.io.mem_ifc.address.poke(0x0110.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x9.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0108.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0118.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0100.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    // sample pwm signal at gpio output
                    // TBD.

               }

          }
     }
     it should "change pwm signal when settings changed through register" in {
          test(
               new GPIOPeripheral(
                    addrWidth = 32,
                    nofGPIO = 8,
                    )
               ).withAnnotations(Seq( WriteVcdAnnotation )) {
               dut => {
                    // configure pwm
                    dut.io.mem_ifc.address.poke(0x0110.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x9.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0108.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0118.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    dut.io.mem_ifc.address.poke(0x0100.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0x1.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    // sample pwm signal at gpio output
                    // TBD.

                    // change period
                    dut.io.mem_ifc.address.poke(0x0108.U(32.W))
                    dut.io.mem_ifc.wrData.poke(0xF.U)
                    dut.io.mem_ifc.wr.poke(true.B)
                    while (!dut.io.mem_ifc.ack.peek().litToBoolean) {
                         dut.clock.step()
                    }
                    dut.io.mem_ifc.wr.poke(false.B)
                    dut.clock.step()

                    // sample pwm signal at gpio output
                    // TBD.

               }

          }
     }
}