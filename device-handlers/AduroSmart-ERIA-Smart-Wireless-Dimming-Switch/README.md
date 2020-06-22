# Device Handler for [AduroSmart ERIA Smart Wireless Dimming Switch Remote](https://amzn.to/2Nli8Ub)

There are at least 3 device handlers for this, they are:
1. The default one that Smartthings installs
2. The one on [Adurosmart GitHub](https://github.com/adurosmart/SmartThingsPublic/blob/master/devicetypes/smartthings/zigbee-multi-button.src/zigbee-multi-button.groovy)
3. And a random version on a [WIP Branch](https://github.com/adurosmart/SmartThingsPublic/blob/0b5bcaaa0bcec6bafd966dd2486c4f24bc50c7fc/devicetypes/smartthings/zigbee-multi-button.src/zigbee-multi-button.groovy)

This one attempts to elimate the duplicate events that were at least seem in numbers 2 and 3.

## Known Issues

- The battery level does not work. This seems to be an issue across all Device Handler versions.

## Button Definitions

- Buttons 1 [top] and 4 [bottom] are on/off single press
- Buttons 2 and 3 allow for long hold

## Webcore Integration

### Buttons (defined in handler)

- Each button can be directly referenced in WebCore by number, and has an even `PUSHED`
- Since the buttons 2 and 3 will send multiple events when pressed, the Piston will executre repeatedly. You
will get semaphore timeouts with this. There is really no way (I know) to avoid flooding Webcore like this.

