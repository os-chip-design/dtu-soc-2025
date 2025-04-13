#  SPI Controller – Register Map

This document uses a clean field table format for each SPI register.

---

##  TX Register

| Bit Range | Field Name | Width | Description                    |
|-----------|-------------|--------|--------------------------------|
| 31:0      | `tx_data`   | 32     | Data to transmit via SPI       |

---

##  RX Register

| Bit Range | Field Name | Width | Description                    |
|-----------|-------------|--------|--------------------------------|
| 31:0      | `rx_data`   | 32     | Data received from SPI         |

---

## ⚙ Control Register

| Bit Range | Field Name           | Width | Description                                |
|-----------|----------------------|--------|--------------------------------------------|
| 31:26     | Reserved             | 6      | Reserved (must write 0)                    |
| 25:23     | `spi_cs_select`      | 3      | Chip select index (0–7)                    |
| 22:18     | `spi_receive_length` | 5      | Number of bits to receive                  |
| 17:13     | `spi_delay_cycles`   | 5      | Delay cycles between transfers             |
| 12:8      | `spi_send_length`    | 5      | Number of bits to transmit                 |
| 7         | `spi_enable`         | 1      | Enable SPI controller                      |
| 6:2       | `spi_prescale`       | 5      | SPI clock divider                          |
| 1:0       | `spi_mode`           | 2      | SPI mode (CPOL/CPHA)                       |

---

##  Flag Register

| Bit Range | Field Name          | Width | Description                                         |
|-----------|---------------------|--------|-----------------------------------------------------|
| 31:3      | Reserved            | 29     | Reserved                                            |
| 2         | `start_transaction` | 1      | Start SPI transfer                                  |
| 1         | `rx_data_ready`     | 1      | Set when RX data is ready (clear after reading)     |
| 0         | `tx_data_ready`     | 1      | Set when ready for TX data (clear after writing)    |
