<div align="center">
<img src="resources/systapi/banner.png" alt="SystaPi" style="width:100%;"/>
</div>

<sup>Code:</sup> [![GitHub license](https://img.shields.io/github/license/beep-projects/SystaPi)](https://github.com/beep-projects/SystaPi/blob/main/LICENSE) [![Scc Count Badge](https://sloc.xyz/github/beep-projects/SystaPi/?category=code)](https://github.com/beep-projects/SystaPi/) [![Scc Count Badge](https://sloc.xyz/github/beep-projects/SystaPi/?category=blanks)](https://github.com/beep-projects/SystaPi/) [![Scc Count Badge](https://sloc.xyz/github/beep-projects/SystaPi/?category=lines)](https://github.com/beep-projects/SystaPi/) [![Scc Count Badge](https://sloc.xyz/github/beep-projects/SystaPi/?category=comments)](https://github.com/beep-projects/SystaPi/) [![Scc Count Badge](https://sloc.xyz/github/beep-projects/SystaPi/?category=cocomo)](https://github.com/beep-projects/SystaPi/)  
<sup>Checks:</sup> [![JUnit](https://github.com/beep-projects/SystaPi/actions/workflows/junit.yml/badge.svg)](https://github.com/beep-projects/SystaPi/actions/workflows/junit.yml) [![shellcheck](https://github.com/beep-projects/SystaPi/actions/workflows/shellcheck.yml/badge.svg)](https://github.com/beep-projects/SystaPi/actions/workflows/shellcheck.yml) [![Pylint](https://github.com/beep-projects/SystaPi/actions/workflows/pylint.yml/badge.svg)](https://github.com/beep-projects/SystaPi/actions/workflows/pylint.yml)  
<sup>Repo:</sup> [![GitHub issues](https://img.shields.io/github/issues/beep-projects/SystaPi)](https://github.com/beep-projects/SystaPi/issues) [![GitHub forks](https://img.shields.io/github/forks/beep-projects/SystaPi)](https://github.com/beep-projects/SystaPi/network) [![GitHub stars](https://img.shields.io/github/stars/beep-projects/SystaPi)](https://github.com/beep-projects/SystaPi/stargazers) ![GitHub repo size](https://img.shields.io/github/repo-size/beep-projects/SystaPi) [![Visitors](https://api.visitorbadge.io/api/visitors?path=beep-projects%2FSystaPi&label=Visitors&labelColor=%235a5a5a&countColor=%234cc71e&style=flat&labelStyle=none)](https://visitorbadge.io/status?path=beep-projects%2FSystaPi)

# SystaPi, SystaREST and STouchREST

**SystaPi** provides a REST API for communication with [Paradigma SystaComfort](https://www.paradigma.de/produkte/regelungen/systacomfortll/) units. The goal of this project is to make the Paradigma system compatible with every home automation system that supports REST APIs.
The project contains an installation script to setup a Raspberry Pi as SystaPi for running the SystaREST server. The server is running two services, SystaRESTAPI for reading values from the Sytsa Comfort unit and STouchRESTAPI for writing to the Systa Comfort, e.g. selecting the operation mode.  
**Important Note:** The communication protocols use by the Systa Comfort unit are not publicly available! Everything here is based on [reverse engineering](resources/protocols.md) and will only work for systems that are used by contributors. **Please [contribute](#contribute) information from your system!**  

This project is inspired by this post on the VDR portal [Heizungssteuerung: Daten auslesen](https://www.vdr-portal.de/forum/index.php?thread/119690-heizungssteuerung-daten-auslesen/) and I also used some information from the [SystaComfortPrometheusExporter](https://github.com/xgcssch/SystaComfortPrometheusExporter).  

Build with a Raspberry Pi Zero WH and ENC28J60 Ethernet HAT, the SystaPi fits easily into the housing of the Paradigma SystaComfort.  
<img src="resources/SystaPi.jpg" alt="SystaPi" width="30%"></img> <img src="resources/SystaComfort_and_Pi_open.png" alt="SystaComfort_and_Pi_open" width="30%"></img> <img src="resources/SystaComfort_and_Pi_closed.png" alt="SystaComfort_and_Pi_closed" width="30%"></img> 

## Content

- [Code Structure and Extending Functionality](CodeStructure.md)
- [Parts List](#parts-list)
- [Installation](#installation)
  - [SystaComfort](#systacomfort)
  - [Linux](#linux)
  - [Windows / manual installation](#windows--manual-installation)
  - [Troubleshooting the installation](#troubleshooting-the-installation)
- [The SystaREST API](SystaREST.md)
  - [findsystacomfort](#findsystacomfort)
  - [start](#start)
  - [stop](#stop)
  - [servicestatus](#servicestatus)
  - [rawdata](#rawdata)
  - [dashboard](#dashboard)
  - [monitorrawdata](#monitorrawdata)
  - [waterheater](#waterheater)
  - [status](#status)
  - [enablelogging](#enablelogging)
  - [disablelogging](#disablelogging)
- [The STouchREST API](STouchRest.md)
  - [connect](#connect)
  - [disconnect](#disconnect)
  - [touch](#touch)
  - [screen](#screen)
  - [debugscreen](#debugscreen)
  - [objecttree](#objecttree)
  - [touchbutton](#touchbutton)
  - [touchtext](#touchtext)
  - [automation](#automation)
- [Contribute](#contribute)
- [Known Issues](#known-issues)
- [Links](#links)

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

This is what I am using for this project, but any Raspberry Pi with at least one Ethernet interface and a second WiFi or Ethernet interface should do the job. The required size of the Micro SD card depends on the amount of data you want to log. Logging data of one day requires ~100 MB.

* Raspberry Pi Zero WH
* ENC28J60 Ethernet HAT
* Micro SD card >256MB
* Micro USB Powersupply 5V / 1A

Of course you also need a Paradigma SystaComfort or Paradigmy SystaComfort II. The following are the paradigma software versions that I succesfully used with **SystaPi** (#1, #2) or that where reported to work (#3)

|                  |   |        #1        |   |       #2          |   |       #3          |
|------------------|---|------------------|---|-------------------|---|-------------------|
| **SystaComfort** |   | `V1.14  8.08.14` |   | `V1.26  10.02.20` |   | `V1.12  20.05.14` |
| **System**       |   | `V2.09.2`        |   | `V2.16.1`         |   | `V2.09.1`         |
| **Basis**        |   | `V0.23`          |   | `V0.34`           |   | `V0.23`           |

## Installation

For easy installation I have created some scripts that configure the Raspberry Pi OS automatically on a Micro SD card. These scripts are not actively maintained, so they might stop working at some time. If auto configuration fails, step through the files `firstrun.sh` and `secondrun.sh` and run the commands manually on your `systapi`. Your are also welcome to fix the scripts and create a pull request to this repository.

Once the Micro SD card is prepared as described in the next sections, the scripts should do the following on first boot ups of the Raspberry Pi: 
* resize the Raspberry Pi OS partition to use the full size of the Micro SD card
* configure WiFi on interface `wlan0`
* `apt full-upgrade` the system
* install `dnsmasq`
* configure `dnsmasq` and `dhcpd` for IP spoofing on interface `eth0`\
  (this will make the Paradigma SystaComfort to communicate with `systapi` instead of [SystaWeb](https://paradigma.remoteportal.de/)
* install OpenJDK 11 from [https://www.azul.com/downloads/?package=jdk#download-openjdk](https://www.azul.com/downloads/?package=jdk#download-openjdk)\
  (this is the most current one you can get for the ARMv6 of the Raspberry Pi Zero)
* install the `systemd` service unit `SytsaRESTServer.service` for automatically starting the SystaRESTServer

### SystaComfort
You have to make sure that your Paradigma SystaComfort unit is sending unencrypted data to [paradigma.remoteportal.de](http://paradigma.remoteportal.de/). If you are one of the unlucky ones, that got a SystaComfort installed with encryption enabled, or the remote portal being disable, you have to get hold of the SystaService software. You can ask Paradigma for that, or your system installer. See also the instructions for setting up the SystaComfortPrometheusExporter
 [english](https://github.com/xgcssch/SystaComfortPrometheusExporter#configure-systacomfort-controller)/[german](https://github.com/xgcssch/SystaComfortPrometheusExporter/blob/main/README_de.md#voraussetzungen)

### Linux

For Linux I provide a script that downloads Raspberry Pi OS and flashes it onto a Micro SD card. The script was mainly written out of curiosity to see how that could work. So it has no added sanity checks and you should use it with care. Check each step, when asked to confirm. If unsure, follow the manual installation guide.

1. Run the following commands in a shell for downloading and unzipping the project files

   ```bash
   wget https://github.com/beep-projects/SystaPi/releases/download/2.2/SystaPi-2.2.zip
   unzip SystaPi-2.2.zip
   ```

2. Open `SystaPi-2.2/SystaPi_files/firstrun.sh` with a text editor and configure everything in the marked section to your liking. 
   Most probably you want to generate your `WPA_PASSPHRASE` via `wpa_passphrase MY_WIFI passphrase` , or  use the [WPA PSK (Raw Key) Generator](https://www.wireshark.org/tools/wpa-psk.html), and add the credentials to the file.
   If you use the network `192.168.1.x` for your local network, you should change the `IP_PREFIX` to another IP range, to avoid network collisions
   
   ```bash
   #-------------------------------------------------------------------------------
   #----------------------- START OF CONFIGURATION --------------------------------
   #-------------------------------------------------------------------------------
   
   # which hostname do you want to give your raspberry pi?
   HOSTNAME=systapi
   #username: beep, password: projects
   #you can change the password if you want and generate a new password with
   #Linux: mkpasswd --method=SHA-256
   #Windows: you can use an online generator like https://www.dcode.fr/crypt-hasing-function
   USER=beep
   # shellcheck disable=SC2016
   PASSWD='$5$oLShbrSnGq$nrbeFyt99o2jOsBe1XRNqev5sWccQw8Uvyt8jK9mFR9' #keep single quote to avoid expansion of $
   # configure the wifi connection
   # the example WPA_PASSPHRASE is generated via
   #     wpa_passphrase MY_WIFI passphrase
   # but you also can enter your passphrase as plain text, if you accept the potential insecurity of that approach
   SSID=MY_WIFI
   WPA_PASSPHRASE=3755b1112a687d1d37973547f94d218e6673f99f73346967a6a11f4ce386e41e
   # define the network to use for communication between systapi and Systa Comfort
   # change if you use the same network range on your wifi network
   IP_PREFIX="192.168.1"
   # configure your timezone and key board settings
   TIMEZONE="Europe/Berlin"
   COUNTRY="DE"
   XKBMODEL="pc105"
   XKBLAYOUT=$COUNTRY
   XKBVARIANT=""
   XKBOPTIONS=""
   # if you want to use an ENC28J60 Ethernet HAT, enable it here
   ENABLE_ENC28J60=true
   
   #-------------------------------------------------------------------------------
   #------------------------ END OF CONFIGURATION ---------------------------------
   #-------------------------------------------------------------------------------
   ```
   
3. Insert the Micro SD card that you want to get prepared as SystaPi into your computing device

4. Continue in the shell

   ```bash
   cd SystaPi-2.2
   ./install_systapi.sh
   ```

5. Eject the Micro SD card and insert it into your Raspberry Pi

6. Connect the Raspberry Pi with an Ethernet cable to your Paradigma SystaComfort

7. Power up the Raspberry Pi

8. Wait a while (~20 minutes, depending on the number of system updates available) and then try to load the WADL of the server: [http://systapi:1337/application.wadl?detail=true](http://systapi:1337/application.wadl?detail=true)
    For troubleshooting, you can check the progress by checking the logs. After 5 minutes the resize of the partitions and ```firstrun.sh``` should be finished, so that you can ssh into the **systapi** and watch the installation process. Default user is `beep` with password `projects`.

   ```bash
   ssh -x beep@systapi.local
   tail -f /boot/secondrun.log
   ```

### Windows / manual installation

1. Install Raspberry Pi OS following this [guide](https://www.raspberrypi.com/documentation/computers/getting-started.html#installing-the-operating-system).
   [Raspberry Pi OS Lite](https://www.raspberrypi.org/software/operating-systems/#raspberry-pi-os-32-bit) is sufficient.

2. Download [SystaPi](https://github.com/beep-projects/SystaPi/releases/download/2.2/SystaPi-2.2.zip)

3. Extract the downloaded zip file

4. Change into the `SystaPi_files` subfolder of the extracted archive

5. Open `firstrun.sh` with a text editor and configure everything in the marked section to your liking.
   Most probably you want to use something like [WPA PSK (Raw Key) Generator](https://www.wireshark.org/tools/wpa-psk.html) and add the generated credentials to the file.
   If you use the network `192.168.1.x` for your local network, you should change the `IP_PREFIX` to another IP range, to avoid network collisions

   ```bash
   #-------------------------------------------------------------------------------
   #----------------------- START OF CONFIGURATION --------------------------------
   #-------------------------------------------------------------------------------
   
   # which hostname do you want to give your raspberry pi?
   HOSTNAME=systapi
   #username: beep, password: projects
   #you can change the password if you want and generate a new password with
   #Linux: mkpasswd --method=SHA-256
   #Windows: you can use an online generator like https://www.dcode.fr/crypt-hasing-function
   USER=beep
   # shellcheck disable=SC2016
   PASSWD='$5$oLShbrSnGq$nrbeFyt99o2jOsBe1XRNqev5sWccQw8Uvyt8jK9mFR9' #keep single quote to avoid expansion of $
   # configure the wifi connection
   # the example WPA_PASSPHRASE is generated via
   #     wpa_passphrase MY_WIFI passphrase
   # but you also can enter your passphrase as plain text, if you accept the potential insecurity of that approach
   SSID=MY_WIFI
   WPA_PASSPHRASE=3755b1112a687d1d37973547f94d218e6673f99f73346967a6a11f4ce386e41e
   # define the network to use for communication between systapi and Systa Comfort
   # change if you use the same network range on your wifi network
   IP_PREFIX="192.168.1"
   # configure your timezone and key board settings
   TIMEZONE="Europe/Berlin"
   COUNTRY="DE"
   XKBMODEL="pc105"
   XKBLAYOUT=$COUNTRY
   XKBVARIANT=""
   XKBOPTIONS=""
   # if you want to use an ENC28J60 Ethernet HAT, enable it here
   ENABLE_ENC28J60=true
   
   #-------------------------------------------------------------------------------
   #------------------------ END OF CONFIGURATION ---------------------------------
   #-------------------------------------------------------------------------------
   ```
   
6. Make sure that the `boot`-partition of the Micro SD card is accessible via file explorer

7. Open `cmdline.txt` from the Micro SD card and copy the `root=PARTUUID=`-Number over into the `cmdline.txt` in the `SystaPi_files` subfolder. If you do not do this step, your pi will not boot!

8. Copy all files from the `SystaPi_files` subfolder to `boot`-partition of the Micro SD card

9. Copy the `SystaRESTServer` folder and all of its content to the `boot`-partition.

10. Eject the Micro SD card and insert it into your Raspberry Pi

11. Connect the Raspberry Pi with an Ethernet cable to your Paradigma SystaComfort

12. Power up the Raspberry Pi

13. Wait a while (~20 minutes, depending on the number of system updates available) and then try to load the WADL of the server: [http://systapi:1337/application.wadl?detail=true](http://systapi:1337/application.wadl?detail=true)
For troubleshooting, you can check the progress by checking the logs. After 5 minutes the resize of the partitions and ```firstrun.sh``` should be finished, so that you can ssh into the **systapi** and whatch the installation process.. Default user is `beep` with password `projects`.

    ```bash
    ssh -x beep@systapi.local
    tail -f /boot/secondrun.log
    ```

### Troubleshooting the installation

1. The autoconfig of the Raspberry Pi OS worked fine when I did the commit for it. But if development of Raspberry Pi OS goes on, the scripts might break. If you connect the Raspberry Pi to a screen via HDMI, you will see if something gets wrong.
2. If the pi does not boot, check if you did step 7 in case of a manual installation.
3. If you do not know where the install script died on the Raspberry Pi, have a look into the `/boot` folder via `ls /boot/*.log`. 
Each script creates a log file, so check `firstrun.log`, `secondrun.log` and `thirdrun.log`, to see where the script failed.
4. SystaRESTServer is installed as a service on the raspberry pi. 
`systemctl status SystaRESTServer.service` will show you if the service is running or died for some reason

## Contribute
Steps to send your contribution are described in [CONTRIBUTING.md](CONTRIBUTING.md), but whatever you want to contribute to the project, the fastes way is to [open a new discussion](https://github.com/beep-projects/SystaPi/discussions/new/choose) and describe your contribution.


To support you in reverse engineering the protocol for SystaRESTAPI, the server has a rudimentary [logging](#enablelogging) functionality integrated. The [dashboard](#dashboard) gives you an overview of the known values received in the last 24h and has buttons to start/stop the logging and to download all log files as zip, for easy logfile handling.  
You can also use [monitorrawdata](#monitorrawdata) to monitor the data sent by **SystaPi** and contribute new fields that you can identify with your system (see also [monitorrawdata](#monitorrawdata)). These pages are created using [React](https://reactjs.org/), so you also can use them as starting point for creating your own dashboard.  

If you want to report new fields, simply open a new [Issue](https://github.com/beep-projects/SystaPi/issues) or [Discussion](https://github.com/beep-projects/SystaPi/discussions).  

<img src="resources/systapidashboard.jpg" alt="SystaPi Dashboard" style="width:45%;"/> <img src="resources/rawData_react_html.jpg" alt="rawData_react_html" style="width:45%;"/>  

To support you in reverse engineering the protocol for STouchRESTAPI, the server has an endpoint, that provides a clickable image of the S-touch scree, which also logs the x,y coordinates of clicks on the screen, see [debugscreen](#debugscreen).
<img src="resources/debug_screen.jpg" alt="SDebug Screen" style="width:45%;"/>  

## Known Issues

There are some ENC28J60 modules sold with wrong jumper settings. Make sure you set the jumpers as in the picture on the left (vertically connecting the PINs).

| correct                                                   | wrong                                                   |
| --------------------------------------------------------- | ------------------------------------------------------- |
| ![correct jumper settings](resources/enc28j60_right.jpg) | ![wrong jumper settings](resources/enc28j60_wrong.jpg) |

## Links

* [SystaREST Javadoc](http://beep-projects.github.io/SystaPi)
* [Paradigma Downloads](http://www.paradigma.de/software/)
* [Heizungssteuerung: Daten auslesen](https://www.vdr-portal.de/forum/index.php?thread/119690-heizungssteuerung-daten-auslesen/)
* [SystaComfortPrometheusExporter](https://github.com/xgcssch/SystaComfortPrometheusExporter)
* [ParadigmaHeatingReader](https://github.com/kayr7/ParadigmaHeatingReader) also read [protocols](https://github.com/beep-projects/SystaPi/blob/main/resources/protocols.md) if you want to buils anything on top of that (S-touch App)

