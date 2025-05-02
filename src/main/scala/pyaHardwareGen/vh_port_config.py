import csv
import sys


power_def_string = """`ifdef USE_POWER_PINS
    inout VPWR,
    inout VGND,
`endif
"""


def main():
    if len(sys.argv) < 2:
        print("Wrong number of args")
        return 1

    port_config = sys.argv[1]
    modulename = port_config.split("_")[0]

    output = []
    output.append("module " + modulename + " (\n")
    output.append(power_def_string)
    output.append("\n")

    with open(port_config, "r") as csvfile:
        reader = csv.reader(csvfile, delimiter=',')

        for row in reader:
            if row[0] in ["VPWR", "VGND"]:
                continue
            else:
                output.append("   " + row[1].lower() + " logic " + row[0] + ",\n")
        # delete last comma
        output[-1] = output[-1].rstrip(",\n") + "\n"
    
    output.append(");\n")
    output.append("\n")
    output.append("endmodule\n")

    vh_out = modulename + ".vh"
    with open(vh_out, "w") as lef:
        lef.writelines(output)


if __name__ == "__main__":
    main()
