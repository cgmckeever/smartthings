<?php

require_once 'config.php';
$debug = false;

$macs_to_block = explode(',', $argv[1]);

$unifi_connection = new UniFi_API\Client($controlleruser, $controllerpassword, $controllerurl, $site_id, $controllerversion);
$set_debug_mode   = $unifi_connection->set_debug($debug);
$loginresults     = $unifi_connection->login(); // always true regardless of site id

foreach ($macs_to_block as $mac) {
    $block_result = $unifi_connection->block_sta($mac);

    /**
     * NOTE:
     * during testing I had some strange behavior where clients were not reconnecting to the network correctly,
     * they appeared unblocked and received a valid IP address but could not actually get any data.
     * the clients did not come to life until I disabled the SSID and then re enabled it.
     * I guessed maybe these commands were occurring too quickly for the controller so I have slowed them down;
     * since introducing the sleep I have not seen the above behavior so it might be fixed
     */
    sleep(1);

    $getid_result = $unifi_connection->stat_client($mac);

    if (property_exists($getid_result[0], "oui")) {
        // this field(manufacturer) seems to exist on valid mac addresses
        if (property_exists($getid_result[0], "name")) {
            // this is the alias field if it has been defined
            $name = $getid_result[0]->name;
        } else {
            $name = $getid_result[0]->hostname;
        }
        print 'blocked ' . $name . PHP_EOL;
    } else {
        print 'ERROR: could not block ' . $mac . PHP_EOL;
        print '       check mac address is valid and part of your network' . PHP_EOL;
    }
}