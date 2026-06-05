#!/usr/bin/env bash

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

set -e

echo Installing dependencies...
apt update
apt install -y openjdk-17-jdk-headless maven git tgt open-iscsi vim unzip

if [ "$#" -eq 1 ]
  then
  echo "Extracting PSNextcloudUSB from $1..."
  unzip $1 -d /opt/psnextcloudusb
  cd /opt/psnextcloudusb
else
  echo Downloading PSNextcloudUSB...
  rm -r /opt/psnextcloudusb || true
  mkdir /opt/psnextcloudusb
  cd /opt/psnextcloudusb
  git clone https://github.com/akechi-haruka/PSNextcloudUSB .
fi

echo "Building (this may take a while on a Pi Zero 1)..."
MAVEN_OPTS="-Dhttps.protocols=TLSv1.1,TLSv1.2" mvn install -Dmaven.test.skip=true
cp target/PSNextcloudUSB-build.jar PSNextcloudUSB.jar

echo Configuring drivers...
echo "dtoverlay=dwc2" | sudo tee -a /boot/firmware/config.txt
echo "dwc2" | sudo tee -a /etc/modules
echo "libcomposite" | sudo tee -a /etc/modules

echo Installing services...
cp massstorage.service /etc/systemd/system/massstorage.service
cp psnextcloudusb.service /etc/systemd/system/psnextcloudusb.service

systemctl enable massstorage.service
systemctl enable psnextcloudusb.service

echo Creating config directory...

mkdir /boot/firmware/psnextcloudusb
cp config.properties /boot/firmware/psnextcloudusb/config.properties

read -ep 'Please enter the desired size of the temporary storage in MB: ' storage_size
[[ $storage_size =~ ^[[:digit:]]+$ ]] || exit 1
(( ( (storage_size=(10#$storage_size)) <= 9999 ) && storage_size >= 0 )) || exit 1

echo "$($storage_size)M" > /boot/firmware/psnextcloudusb/disksize.txt

echo "Now opening an editor to edit the configuration file for PSNextcloudUSB"
read -n 1 -r -s -p "Press any key to continue..."

select-editor
sensible-editor /boot/firmware/psnextcloudusb/config.properties

echo Creating storage...
dd if=/dev/zero of=/usbdisk.img count=0 obs=1 seek="$(cat /boot/firmware/psnextcloudusb/disksize.txt)"

echo "Linking storage (step 1)..."
set +e
tgtadm --lld iscsi --op new --mode target --tid 1 -T iqn.2026-06.local:psnextcloudusb
tgtadm --lld iscsi --op new --mode logicalunit --tid 1 --lun 1 -b /usbdisk.img
tgtadm --lld iscsi --op bind --mode target --tid 1 -I ALL
set -e
tgt-admin --dump | tee /etc/tgt/conf.d/psnextcloudusb.conf

echo "Linking storage (step 2)..."
sed -i "s/\(node.startup *= *\).*/\1automatic/" /etc/iscsi/iscsid.conf
service open-iscsi restart
iscsiadm -m discovery -t sendtargets -p 127.0.0.1
set +e
iscsiadm -m node -l
set -e
DISK=/dev/disk/by-path/ip-127.0.0.1:3260-iscsi-iqn.2026-06.local:psnextcloudusb-lun-1

echo Formatting...
mkdosfs -n $PRODUCT $DISK

echo Verifying...
mkdir -p /usbdisk.d
mount $DISK /usbdisk.d
echo "Successfully installed! Enjoy!" > /usbdisk.d/test.txt
sync
umount /usbdisk.d


echo Finished!
echo After the device has shut down, you can now plug it into your PS.
read -n 1 -r -s -p "Press any key to shutdown..."

shutdown now
