# Linked Device Labeler - Hubitat App

This repository contains a custom app to simplify labeling devices linked across Hubitat hubs via Hub Mesh.

## Features
- Bulk updates to the Device Label values of devices to match their labels on the remote hub (i.e. removing " on Hub").
- Optional appending of strings (e.g. icons, characters) to Device Label values.
- Precise selection of linked devices on which to submit Device Label updates.
- Visibility (with counts) into all linked devices on the hub (requires "Toggle All" selection of all devices).

## Installation

### Step 1: Import and Add the App
- Install the application via HPM (search for "Linked" or "Labeler", requires "Fast Search") 
- Install the application from this repository via the Apps Code page: [`LinkedDeviceLabeler.groovy`](https://raw.githubusercontent.com/jdc72/Hubitat/main/linked_device_labeler/src/LinkedDeviceLabeler.groovy).
- Add the application via "Add user app" on the Apps page.

### Step 2: Select Devices
- Select linked (and local) devices.
- The app automatically filters out local devices, so no harm in selecting them as well ("Toggle All" is your friend).
- All changes to Device Label values occur only on devices linked from remote hubs.

### Step 3: Configure Controls
- Optionally restrict the display/action to linked devices currently without Device Label values (i.e. newly linked devices).
- Optionally restrict the display/action to linked devices whose Device Label values would change.
- Optionally enter string (e.g. icons, characters) to append to all Device Label values.

### Step 4: Specify Devices to Change
- Use checkboxes to select and unselect the linked devices whose Device Label values will change.

### Step 5: Apply the Changes
- Apply the Device Label edits in bulk to all selected devices.

## Limitations
- The app requires users to re-select devices after adding newly linked devices.  HE apps cannot access devices without user selections.
- The app does not allow per-device text entry for the labels.

## Notes
- Additional visuals can be found in the [`images`](https://github.com/jdc72/Hubitat/tree/main/linked_device_labeler/docs/images) folder within this repository.
- The Hubitat Community topic can be found [here](https://community.hubitat.com/t/release-linked-device-labeler/151494).

## Support
If you encounter issues or have feature requests, feel free to open an [issue](https://github.com/jdc72/Hubitat/issues) on this repository.
