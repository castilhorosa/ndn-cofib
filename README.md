# CoFIB: A Data Structure for NDN FIB in Programmable Switches

The source code in this repository includes the main components of the NDN CoFIB data structure. It includes the elements for the data plane and control plane.

Here is the sintax to run our code:

```
ndn-cofib -f <[-c] | [-l] | [-e] | [-t] | [-r] | [-p] [-P]> [--input <dataset_file>] [<--output> <file>] [-n] [--pps] [--duration]
        Options:
        -c: Verify if the prefix dataset is canonical.
        -h: Verify if the prefix dataset has collisions.
        -l: Load the prefixes from the dataset file and provides a simple and interactive CLI.
        -e: Generate P4Runtime table entries for the DFIB, HCT, and DPST for CoFIB switch.
        -t: Generate the binary traffic file to be injected into the CoFIB switch.
        -r: Generate a random syntetic dataset containing all combinations of prefixes of a given size.
        -n: Number of components for each prefix to generate the syntetic dataset if [-r] is used.
        -p: Generate the packets file containing a given amount of packets for each possible number of components, sorted by the number of components.
        -P: Generate the packets file containing a given amount of packets for each possible number of components, in a random order in the number of components.
        --pps:       Packets per second if [-p] is used.
        --duration:  Duration of the traffic in seconds if [-p] is used.
```

Please, contact me for any questions or suggestions.
