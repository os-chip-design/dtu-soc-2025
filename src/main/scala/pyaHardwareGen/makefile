
default:
	python3 pya_memcell.py
	magic -dnull -noconsole -rcfile ${PDK_ROOT}/sky130B/libs.tech/magic/sky130B.magicrc magic_gds2lef.tcl << quit -noprompt
	python3 lef_port_config.py memcellcustom_lef_config.csv memcellcustom.lef
	python3 vh_port_config.py memcellcustom_lef_config.csv
