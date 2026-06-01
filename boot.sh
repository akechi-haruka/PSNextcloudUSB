#!/usr/bin/env bash

source /opt/psnextcloudusb/vars.sh

umount /usbdisk.d
mkdosfs /usbdisk.img

modprobe libcomposite
mkdir -p /sys/kernel/config/usb_gadget/$GADGETDIR
cd /sys/kernel/config/usb_gadget/$GADGETDIR || exit 1
echo 0x1d6b > idVendor # Linux Foundation
echo 0x0104 > idProduct # Multifunction Composite Gadget
echo 0x0100 > bcdDevice # v1.0.0
echo 0x0200 > bcdUSB # USB2
mkdir -p strings/0x409
echo $SERIAL > strings/0x409/serialnumber
echo $MANUFACTURER > strings/0x409/manufacturer
echo $PRODUCT > strings/0x409/product
mkdir -p configs/c.1/strings/0x409
echo "Config 1: ECM network" > configs/c.1/strings/0x409/configuration
echo 250 > configs/c.1/MaxPower

mkdir -p ${FILE/img/d}
mount -o loop,ro -t vfat $FILE ${FILE/img/d}
mkdir -p functions/mass_storage.usb0
echo 1 > functions/mass_storage.usb0/stall
echo 0 > functions/mass_storage.usb0/lun.0/cdrom
echo 0 > functions/mass_storage.usb0/lun.0/ro
echo 0 > functions/mass_storage.usb0/lun.0/nofua

echo $FILE > functions/mass_storage.usb0/lun.0/file

ln -s /sys/kernel/config/usb_gadget/$GADGETDIR/functions/mass_storage.usb0 /sys/kernel/config/usb_gadget/$GADGETDIR/configs/c.1/

ls /sys/class/udc > UDC