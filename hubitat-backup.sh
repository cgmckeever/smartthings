#!/bin/bash

set -e

he_login="username"
he_passwd="password"
he_ipaddr="IP.address"
cookiefile="/tmp/hubitat.cookie"
backupdir="/path/to/backup"
backupfile=Hubitat_$(date +%Y-%m-%d-%H%M).lzf

find $backupdir/*.lzf -mtime +5 -exec rm {} \;
curl -k -c $cookiefile -d username=$he_login -d password=$he_passwd https://$he_ipaddr/login
curl -k -sb $cookiefile https://$he_ipaddr/hub/backup | awk -F'["=]' '/class=.download/ { file=$9 } END { print file }' | xargs -I@ curl -k -sb $cookiefile https://$he_ipaddr/hub//backupDB?fileName=@ -o $backupdir/$backupfile

rm $cookiefile
