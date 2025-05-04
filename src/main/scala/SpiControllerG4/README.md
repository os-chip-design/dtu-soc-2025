#  SPI Controller – Register Map

---

##  `txReg0` and `txReg1`

| Register | Bit Range | Field Name        | Width | Description                         |
|----------|-----------|-------------------|--------|-------------------------------------|
| `txReg0` | 31:0      | `tx_data[31:0]`   | 32     | Lower 32 bits of SPI transmit data  |
| `txReg1` | 31:0      | `tx_data[63:32]`  | 32     | Upper 32 bits of SPI transmit data  |

---

##  `rxReg0` and `rxReg1`

| Register | Bit Range | Field Name        | Width | Description                         |
|----------|-----------|-------------------|--------|-------------------------------------|
| `rxReg0` | 31:0      | `rx_data[31:0]`   | 32     | Lower 32 bits of SPI received data  |
| `rxReg1` | 31:0      | `rx_data[63:32]`  | 32     | Upper 32 bits of SPI received data  |

---

##  `controlReg`

| Bit Range | Field Name           | Width | Description                                |
|-----------|----------------------|--------|--------------------------------------------|
| 31        | `spi_enable`         | 1      | Write 1 to start SPI transaction           |
| 30:27     | Reserved             | 4      | Reserved (must write 0)                    |
| 26:20     | `spi_receive_length` | 7      | Number of bits to receive                  |
| 19:13     | `spi_delay_cycles`   | 7      | Delay cycles between send and receive      |
| 12:6      | `spi_send_length`    | 7      | Number of bits to transmit                 |
| 5:2       | `spi_prescale`       | 4      | SPI clock divider                          |
| 1:0       | `spi_mode`           | 2      | SPI mode (CPOL/CPHA)                       |

---

##  `flagReg`

| Bit Range | Field Name | Width | Description                                              |
|-----------|------------|--------|----------------------------------------------------------|
| 31:1      | Reserved   | 30     | Reserved (reads as 0)                                    |
| 0         | `ready`    | 1      | Set when SPI controller is idle and ready for new input  |
