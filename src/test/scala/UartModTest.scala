import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3._

import scala.util.Random

class UartModTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "UartModule"

  "UartModule" should "transmit when valid and ready" in {
    val baudRate = 9600
    val clockFreq = 10000
    test(new UartModule(clockFreq, baudRate)) { dut =>
      val clocksPerBit = clockFreq / baudRate

      val testData = "b10101010".U

      //verify initial conditions
      dut.io.tx.expect(true.B) //default high

      //start test
      dut.io.tx_data.poke(testData) //put data in buffer
      dut.io.flowControl.poke(false.B) //not testing flow control
      dut.io.tx_valid.poke(true.B) //indicate time to send

      //waiting for ready signal low when transmitting
      while (dut.io.tx_ready.peek().litToBoolean) {
        dut.clock.step(1)
      }

      dut.io.tx.expect(false.B) // start bit
      dut.clock.step(clocksPerBit)

      var bitCount = 0
      while (bitCount < 8) {
        //println(s"At cycle $bitCount, tx is: ${dut.io.tx.peek().litValue}") //debug code
        dut.clock.step(clocksPerBit)
        bitCount = bitCount + 1
      }

    }
  }
  "UartModule" should "receive on baud rate cycles" in {
    //globals for convenience
    val baudRate = 10
    val clockFreq = 1000
    val testData = "b10101010".U
    val clockPerBit = (clockFreq/baudRate)
    val outBits = (0 until 8).map(i => ((testData.litValue >> i) & 1) == 1)
    test(new UartModule(clockFreq, baudRate)) { dut =>

      dut.io.flowControl.poke(false.B) //not testing flow control
      dut.io.rx.poke(false.B) //pull low for start bit
      dut.clock.step(clockPerBit)
      for (outBit <- outBits) { // start transmitting data points
          dut.io.rx.poke(outBit.B)
          dut.clock.step(clockPerBit)
      }
      dut.io.rx.poke(true.B) //stop bit
      dut.clock.step(clockPerBit*2) //wait for data to be stored
      dut.io.rx_data.expect(testData) // check data register for frame!
    }
  }

  "UartModule" should "transmit and receive" in {
    val baudRate = 10
    val clockFreq = 100
    test(new UartModule(clockFreq, baudRate)) { dut =>
      timescope {
        fork {
          while (true) {
            dut.io.rx.poke(dut.io.tx.peek()) // connect uart tx to rx
            dut.clock.step()
          }
        }
      }
      dut.io.flowControl.poke(false.B) //not testing flow control
      val testData = "b0110010".U
      //verify initial conditions
      dut.io.tx.expect(true.B) //default high

      //start test
      dut.io.tx_data.poke(testData) //put data in buffer
      dut.io.tx_valid.poke(true.B) //indicate time to send

      //data should be traveling through to rx
      dut.clock.step(20*(clockFreq/baudRate)) //wait for transmission to finish

      //println(s"rx_data is: ${dut.io.rx_data.peek().litValue}") //debug code
      dut.io.rx_data.expect(testData)

    }
  }
  "UartModule" should "respect flow control" in {
    val baudRate = 10
    val clockFreq = 100
    test(new UartModule(clockFreq, baudRate)) { dut =>
      timescope {
        fork {
          while (true) {
            dut.io.rx.poke(dut.io.tx.peek()) // connect uart tx to rx
            dut.clock.step()
          }
        }
      }
      dut.io.flowControl.poke(true.B) // testing flow control
      val testData = "b0110010".U
      //verify initial conditions
      dut.io.tx.expect(true.B) //default high

      //start test - CTS off
      dut.io.rx_ready.poke(true.B)
      dut.io.tx_data.poke(testData) //put data in buffer
      dut.io.cts.poke(true.B) //shouldn't transmit yet!
      dut.io.tx_valid.poke(true.B) //indicate time to send

      //data should be traveling through to rx
      dut.clock.step(20*(clockFreq/baudRate)) //wait for transmission to finish

      //println(s"rx_data is: ${dut.io.rx_data.peek().litValue}") //debug code
      dut.io.rx_data.expect(0x00) //no data should've been sent

      //test - CTS on
      dut.io.cts.poke(false.B) //should transmit now!
      dut.io.tx_valid.poke(true.B) //indicate time to send
      dut.clock.step(20*(clockFreq/baudRate))
      dut.io.rx_data.expect(testData)

      //test - RTS respects rx_read
      dut.io.rx_ready.poke(true) //ready to receieve
      dut.clock.step()
      dut.io.rts.expect(false)
      dut.io.rx_ready.poke(false) //buffer full
      dut.clock.step()
      dut.io.rts.expect(true)
    }
  }
}