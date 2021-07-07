#!/usr/bin/bash

# bash install_systapi.sh ;
# or pass the path of the sdcard
# bash install_systapi.sh /dev/mmcblk0 ;
 
echo 
echo "=============================================================="
echo " WARNING  WARNING  WARNING  WARNING  WARNING  WARNING  WARNING"
echo "=============================================================="
echo 
echo "This script will make use of dd to flash a SD card. This has the potential to break your system."
echo "By continuing, you confirm that you know what you are doing and that you will double check every step of this script"
read -p "press Y to continue " -n 1 -s
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi 

echo 
echo "=============================================================="
echo " initializing script"
echo "=============================================================="
echo 

# SD card path
if [ $1 ]; then
  SD_CARD_PATH=$1
else
  SD_CARD_PATH=/dev/mmcblk0
fi

#get HOSTNAME for raspberry pi from firstrun.sh
RPI_HOST_NAME=$( grep "^HOSTNAME=" SystaPi_files/firstrun.sh | cut -d "=" -f 2 )

echo "SD_CARD_PATH = ${SD_CARD_PATH}"
echo "RPI_HOST_NAME = ${RPI_HOST_NAME}"

echo 
echo "=============================================================="
echo " check SD card path"
echo "=============================================================="
echo 

echo "please make sure that your SD card is inserted to this computer"
echo "press any key to continue ..."
read -n 1 -s
echo

DISKNAME=$( echo $SD_CARD_PATH | rev | cut -d "/" -f 1 | rev )

if [ -z "$( lsblk | grep $DISKNAME )" ]; then
  echo "FAIL: Disk with name $DISKNAME not found, exiting"
  exit
fi

echo "CHECK OK: Disk with name $DISKNAME exists"

if [ -b $SD_CARD_PATH ]; then
  echo "CHECK OK: Path $SD_CARD_PATH exists"
else
  echo "FAIL: SD card at ${SD_CARD_PATH} not found, exiting"
  exit;
fi

#show available disks for final check
echo "=============================================================="
echo " available disks"
echo "=============================================================="
echo "lsblk | grep disk"
lsblk | grep disk
echo

echo "This script will dd an image of Raspberry Pi OS over whatever is located at ${SD_CARD_PATH}"
echo "Please check the above listing and confirm that you want to write the image on $DISKNAME"
read -p "Press Y to continue" -n 1 -s
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 1
fi 

echo 
echo "=============================================================="
echo " get latest raspbian 32bit os image"
echo "=============================================================="
echo 

echo "get rasbian os information data from server";
rm operating-systems-categories.json
wget https://downloads.raspberrypi.org/operating-systems-categories.json

RPI_IMAGE_URL=$( cat operating-systems-categories.json | grep lite | grep urlHttp | sed -e 's/.*\: \"\(.*\)\".*/\1/' )
RPI_IMAGE_HASH_URL="${RPI_IMAGE_URL}.sha256"

echo 
echo "=============================================================="
echo " check downloded file"
echo "=============================================================="
echo 

RPI_IMAGE_ZIP=$(basename $RPI_IMAGE_URL)
RPI_IMAGE_HASH=$(basename $RPI_IMAGE_HASH_URL)
RPI_IMAGE=$( echo $RPI_IMAGE_ZIP | sed 's/.zip/.img/' )

if [ -f "$RPI_IMAGE_ZIP" ]; then
  echo "${RPI_IMAGE_ZIP} file found. Skipping download."
else

  echo "downloading Raspberry Pi OS image from server. please wait ..."
  rm   $RPI_IMAGE_ZIP.downloading
  wget $RPI_IMAGE_URL -O $RPI_IMAGE_ZIP.downloading
  mv   $RPI_IMAGE_ZIP.downloading $RPI_IMAGE_ZIP

  echo "downloading hash file for image file from server. please wait ..."
  rm ${RPI_IMAGE_HASH}
  wget $RPI_IMAGE_HASH_URL

  echo "checking hash value of image file"
  HASH_OK=$( sha256sum -c $RPI_IMAGE_HASH | grep "$RPI_IMAGE_ZIP: OK" )
  if [ -z "$HASH_OK" ]; then
    echo "hash does not match, aborting"
    exit
  else
    echo "hash is ok"
  fi

fi

echo 
echo "=============================================================="
echo " unzip image file"
echo "=============================================================="
echo 

echo "unzip raspberry pi zip image file"
if [ -f $RPI_IMAGE ]; then
  echo "file found, skip the zip"
else
  echo "unzipping file. please wait a few minutes ..."
  echo "unzip -o $RPI_IMAGE_ZIP $RPI_IMAGE"
  unzip -o $RPI_IMAGE_ZIP $RPI_IMAGE
fi

echo 
echo "=============================================================="
echo " write image to SD card"
echo "=============================================================="
echo 

echo "unmount SD card $DISKNAME"
echo "press any key to continue..."
read -n 1 -s

mount | grep $DISKNAME | cut -d " " -f 3 | while read -r line ; do
    echo "sudo umount $line"
    sudo umount $line
done
echo

echo "SD card used for the following operations is located at: $SD_CARD_PATH"
echo "press any key to continue..."
read -n 1 -s
echo

echo "wipe SD card: sudo wipefs -a $SD_CARD_PATH"
echo "press any key to continue..."
read -n 1 -s
sudo wipefs -a $SD_CARD_PATH
echo

echo "write image file to SD card: sudo dd bs=8M if=$RPI_IMAGE of=$SD_CARD_PATH status=progress"
echo "press any key to continue..."
read -n 1 -s
echo "writing image to SD card ..."
sudo dd bs=8M if=$RPI_IMAGE of=$SD_CARD_PATH status=progress
echo

echo 
echo "=============================================================="
echo " mount SD card"
echo "=============================================================="
echo 
sleep 3 #give the system some time to detect the new formatted disks

echo "mount SD card"
UUID=$( lsblk -f | grep $DISKNAME | grep boot | rev | xargs | cut -d " " -f1 | rev )
echo "udisksctl mount -b \"/dev/disk/by-uuid/$UUID\""
udisksctl mount -b "/dev/disk/by-uuid/$UUID"
RPI_PATH=$( mount | grep $DISKNAME | cut -d " " -f 3 )

echo "press any key to continue..."
read -n 1 -s
echo

echo 
echo "=============================================================="
echo " setting root=PARTUUID in cmdline.txt"
echo "=============================================================="
echo 

echo "the UUID of the root partition might have changed for the downloaded image. Updating the entry in cmdline.txt"
PARTUUID=$( sudo blkid | grep rootfs | grep -oP '(?<=PARTUUID=\").*(?=\")' )
echo "set PARTUUID=$PARTUUID for rootfs in cmdline.txt"
sed -i "s/\(.*PARTUUID=\)[^ ]*\( .*\)/\1$PARTUUID\2/" SystaPi_files/cmdline.txt

echo 
echo "=============================================================="
echo " copy files to SD card"
echo "=============================================================="
echo 

echo "copy files to $RPI_PATH"
echo cp SystaPi_files/cmdline.txt $RPI_PATH
cp SystaPi_files/cmdline.txt $RPI_PATH
echo cp SystaPi_files/firstrun.sh $RPI_PATH
cp SystaPi_files/firstrun.sh $RPI_PATH
echo cp SystaPi_files/secondrun.sh $RPI_PATH
cp SystaPi_files/secondrun.sh $RPI_PATH
echo cp SystaPi_files/thirdrun.sh $RPI_PATH
cp SystaPi_files/thirdrun.sh $RPI_PATH
echo
echo "copy SystaRESTServer to $RPI_PATH"
echo cp -r SystaRESTServer $RPI_PATH
cp -r SystaRESTServer $RPI_PATH

echo "press any key to continue..."
read -n 1 -s
echo


echo 
echo "=============================================================="
echo " unmount SD card"
echo "=============================================================="
echo 

echo "unmount SD card"
sudo umount $RPI_PATH
echo "all work is done. Please insert the SD card into your raspberry pi"
echo "NOTE: when starting up, your raspberry pi should reboot 4 times until all setup work is finished and the SystaRESTServer is up and running. Be patient!"

echo
echo "///////////////////////////////////////////////////////////////"
echo
echo "               ssh -x pi@${RPI_HOST_NAME}.local"
echo
echo "///////////////////////////////////////////////////////////////"
echo
exit;
