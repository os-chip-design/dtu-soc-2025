.global _start

_start:
    li t1, 0x04          # UART-mapped address
    li t0, 0x00000041    # ASCII 'A'
    sw t0, 0(t1)         # send 'A' to UART
    li t1, 0x11          # SPI-mapped address
    li t0, 0x00000042    # ASCII 'B'
    sw t0, 0(t1)         # send 'B' to UART

loop:
    j loop
