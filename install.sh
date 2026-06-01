#!/usr/bin/env bash

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

set -e

echo Installing dependencies...
apt update
apt install openjdk-25-jdk-headless maven git

echo Configuring drivers...
echo "dtoverlay=dwc2" | sudo tee -a /boot/firmware/config.txt
echo "dwc2" | sudo tee -a /etc/modules
echo "libcomposite" | sudo tee -a /etc/modules

echo Downloading PSNextcloudUSB...
mkdir /opt/psnextcloudusb
cd /opt/psnextcloudusb
git clone https://github.com/akechi-haruka/PSNextcloudUSB .

mvn install
cp target/PSNextcloudUSB-build.jar PSNextcloudUSB.jar

echo Installing services...
cp massstorage.service /etc/systemd/system/massstorage.service
cp psnextcloudusb.service /etc/systemd/system/psnextcloudusb.service

systemctl enable massstorage.service

echo Creating config directory...

mkdir /boot/psnextcloudusb
echo "2000" > /boot/psnextcloudusb/disksize.txt
cp config.properties /boot/psnextcloudusb/config.properties

select-editor
sensible-editor /boot/psnextcloudusb/config.properties

echo Creating storage... This may take a long time...
dd if=/dev/zero of=/usbdisk.img bs=1048576 count="$(cat /boot/psnextcloudusb/disksize.txt)"

echo Finished!
read -n 1 -r -s -p "Press any key to continue..."

reboot
