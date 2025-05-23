## Directory Structure of this Project

```
SystaPi
├── docs                 # JavaDoc for the SystaRESTServer, best accessed via https://beep-projects.github.io/SystaPi/
├── helpers              # collection of resources that are helpful for reverse engineering the SystaComfort protocol
│                        # or setting up systapi
├── install_systapi.sh   # Script for automatically downloading, flashing and configuring 
│                        # a Micro SD card for running the SystaREST server
├── LICENSE              # License for using and editing this software
├── README.md            # This file
├── resources            # folder for images or other files linked with README.md
├── SystaPi_files        # files required for configuring a Raspberry Pi OS image to run the SystaRESTAPI server
│   ├── cmdline.txt      # file to be placed under /boot/cmdline.txt on the pi. Triggers the execution of firstrun.sh
│   │                    # on first boot (actually the second one, after resizing the image)
│   ├── firstrun.sh      # script for configuring WiFi, keyboard and timezone. You have to configure a few things in here!
│   ├── secondrun.sh     # called after a reboot. Should have network running. Does a full-upgrade of the system, 
│   │                    # installs required packages (dnsmasq, OpenJDK) and the SystaRESTAPI.service
│   └── thirdrun.sh      # called after a reboot. Cleans up after the installation and reboots into the final system
└── SystaRESTServer      # Java based server for providing a REST API for a Paradigma SystaComfort unit
    ├── bin              # precompiled .class files for running the SystaRESTAPI server
    ├── build.sh         # build file for compiling SystaRESTServer
    ├── build_test.sh    # build file for building the JUnit test run at each commit
    ├── lib              # .jar files required for running the server
    └── src              # src files of the server, for everyone who wants to improve this
```

## Adding New Functionality

Details on how to add new functionality will be provided here. In general, follow the existing code patterns, ensure your changes are well-documented, and submit contributions via pull requests as outlined in [CONTRIBUTING.md](CONTRIBUTING.md).
