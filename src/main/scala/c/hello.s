.global _start

_start:
    li t1, 0x04          # UART-mapped address
    li t0, 0x00000041    # ASCII 'A'
    sw t0, 0(t1)         # send 'A' to UART

loop:
    j loop
