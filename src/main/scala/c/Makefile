alls: hello.bin	

hello.o: hello.s	
	riscv64-unknown-elf-as -march=rv32i -mabi=ilp32 hello.s -o hello.o	

hello.elf: hello.o	
	riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x0 hello.o -o hello.elf	

hello.bin: hello.elf	
	riscv64-unknown-elf-objcopy -O binary hello.elf hello.bin	
	riscv64-unknown-elf-objdump -d -S hello.elf > hello.disasm	

allc:
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 crt0.c -c -o crt0.o
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 hello.c -c -o hello.o
	riscv64-unknown-elf-gcc -march=rv32i -mabi=ilp32 hello.c -c -o hello.bin
	riscv64-unknown-elf-ld -melf32lriscv -T linker.ld crt0.o hello.o
#	riscv64-unknown-elf-ld -melf32lriscv crt0.o hello.o
	riscv64-unknown-elf-objdump -d -f -h a.out
	riscv64-unknown-elf-objcopy -O binary -j .text a.out text.bin
	riscv64-unknown-elf-objcopy -O binary -j .data a.out data.bin
	cat text.bin data.bin > ../a.bin
	xxd -p -c 4 ../a.bin


clean:	
	rm -f *.o *.elf *.bin *.disasm	
