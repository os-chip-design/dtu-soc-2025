# Existing assembly flow
all: hello.bin helloGPIO.bin hello_c.bin poll.bin poll_c.bin resources

# Assembly: hello
hello.o: hello.s
	riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 hello.s -o hello.o

hello.elf: hello.o
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 hello.o -o hello.elf

hello.bin: hello.elf
	riscv64-unknown-elf-objcopy -O binary hello.elf hello.bin
	riscv64-unknown-elf-objdump -d -S hello.elf > hello.disasm

# Assembly: helloGPIO
helloGPIO.o: helloGPIO.s
	riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 helloGPIO.s -o helloGPIO.o

helloGPIO.elf: helloGPIO.o
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 helloGPIO.o -o helloGPIO.elf

helloGPIO.bin: helloGPIO.elf
	riscv64-unknown-elf-objcopy -O binary helloGPIO.elf helloGPIO.bin
	riscv64-unknown-elf-objdump -d -S helloGPIO.elf > helloGPIO.disasm


# C: hello
hello_c.o: hello.c
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 -nostdlib -nostartfiles -ffreestanding -c hello.c -o hello_c.o

hello_c.elf: hello_c.o
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 hello_c.o -o hello_c.elf

hello_c.bin: hello_c.elf
	riscv64-unknown-elf-objcopy -O binary hello_c.elf hello_c.bin
	riscv64-unknown-elf-objdump -d -S hello_c.elf > hello_c.disasm

# Assembly: poll
poll.o: poll.s
	riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 poll.s -o poll.o

poll.elf: poll.o
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 poll.o -o poll.elf

poll.bin: poll.elf
	riscv64-unknown-elf-objcopy -O binary poll.elf poll.bin
	riscv64-unknown-elf-objdump -d -S poll.elf > poll.disasm

# C: poll
poll_c.o: poll.c
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 -nostdlib -nostartfiles -ffreestanding -c poll.c -o poll_c.o

poll_c.elf: poll_c.o
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 poll_c.o -o poll_c.elf

poll_c.bin: poll_c.elf
	riscv64-unknown-elf-objcopy -O binary poll_c.elf poll_c.bin
	riscv64-unknown-elf-objdump -d -S poll_c.elf > poll_c.disasm

# Copies .bin files to test resources
resources: hello.bin hello_c.bin poll.bin poll_c.bin
	mkdir -p ../../../test/resources/
	cp *.bin ../../../test/resources/

clean:
	rm -f *.o *.elf *.bin *.disasm