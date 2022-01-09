# Device Handler for [Sylvania Smart Home 73743 Lightify Smart Dimming Switch](https://www.amazon.com/gp/product/B0196M620Y/ref=ppx_yo_dt_b_asin_title_o01_s00?ie=UTF8&psc=1)

There are at least 3 device handlers for this, they are [referenced here](https://community.smartthings.com/t/faq-sylvania-battery-power-dimming-switch-model-73743-why-there-are-three-different-dths-to-choose-from/107714). This one attempts to implement the holdable-dim option when paired with a Webcore Piston.

## Known Issues

- Installation of a new device may not register/find this DH properly. Setting to this handler manually does sync everything and work as intended.
- Since Dimming triggers a Piston every second, Webcore seems to get out of order of the requests. This of course makes your dim go in weird directions. Unclear of resolution. *Possibly Resolved*

## Button Definitions

- Push Button 1 or 2
- Double Tap (ie. when lights are on hitting ON/Top): sets to min/max ranges respectively
- Hold Button 1 or 2: Continual brightness adjustment
- TODO (Probably never): Push both at same time

## Webcore Integration

### Buttons (defined in handler)
- Button 1: Top
- Button 2: Bottom

### Dimmable 
If you want to set up a dimmable function while a button is held, you can set up a Webcore listener on the **level** _changes_ trigger

![Piston](https://user-images.githubusercontent.com/513738/67286662-69972000-f49f-11e9-9d0a-c363c5582956.png)
