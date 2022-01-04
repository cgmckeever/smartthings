#!/bin/bash

set -e

backup_dir="/tmp/backup"
backup_tar=$(date +"%Y%m%dT%H%M").tar.gz

rm -rf $backup_dir
mkdir -p $backup_dir/systemd

cp /etc/systemd/system/arlo.service $backup_dir/systemd/
cp -R /home/pi/arlo $backup_dir/

cp /etc/systemd/system/homebridge.service $backup_dir/systemd/
cp /home/pi/homebridge.sh $backup_dir/
cp -R /home/pi/homebridge $backup_dir/

cp /etc/systemd/system/nodered.service $backup_dir/systemd/
cp /home/pi/nodered.sh $backup_dir/
cp -R /home/pi/node-red $backup_dir/

tar --warning=no-file-changed -czf /tmp/$backup_tar -C $backup_dir .
ls -la $backup_dir
mv /tmp/$backup_tar $backup_dir/