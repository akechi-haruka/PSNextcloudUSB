{
source /opt/psnextcloudusb/vars.sh

cd /sys/kernel/config/usb_gadget/$GADGETDIR || exit 1

echo > functions/mass_storage.usb0/lun.0/file

umount /usbdisk.d
mkdir -p ${FILE/img/d}
mount -o loop,ro -t vfat $FILE ${FILE/img/d}

echo $FILE > functions/mass_storage.usb0/lun.0/file

} 2>&1