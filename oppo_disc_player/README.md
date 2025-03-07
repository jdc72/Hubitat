# Oppo Disc Player - Hubitat Driver

This repository contains a custom driver for integrating an Oppo disc players with Hubitat. The driver enables remote-like control via LAN connections (either Ethernet or Ethernet-to-serial) and displays current status information of your Oppo player.

## Compatibility
- Oppo BDP players
- Oppo UDP players

## Features
- Control of the Oppo player (e.g., playback, power, track information).
- Supports communication to the Oppo via its Ethernet and R232 ports.
- Retrieves and displays status updates (e.g., playback state, disc information).
- Enhances track information with MusicBrainz queries and manual inputs.
- Compatibility with Oppo BDP and UDP models.

## Requirements
- Oppo BDP or UDP player
- (for serial connections) Ethernet-to-Serial converter (e.g. PUSR USR-TCP232-302)
- (for serial connections) DB9 Null Modem Cable

## Installation

### Step 1: Import the Driver
- Install the driver via HPM (preferred) or 
- Install the driver from this repository: [`OppoDiscPlayer.gvy`](https://github.com/jdc72/Hubitat/blob/main/oppo_disc_player/src/OppoDiscPlayer.gvy).

### Step 2: Add a New Device
- Add a new Virtual Device.
- Select "Oppo Disc Player".

### Step 3: Configure the Ethernet-to-Serial Converter (Optional)
- Follow the converter's instructions for changing its default IP to one in your subnet.
- Set the baud rate to "9600 bps".
- Set the data bits to "8 bits".
- Set the Parity to "None".
- Set the Stop Bits to "1 bit".
- Set the Flow Control to "None" (if available).
- Set the Work Mode to "TCP Server" (if available).
- Set the Heartbeat Packet Type to "Net heartbeat".
- Set the Hearbeat Packet to your choice.
- Set the Hearbeat Time to your choice.

### Step 3: Configure the Device
- Provide the IP address and port of the Oppo or the Ethernet-to-RS232 module.
- Select the appropriate connection to the Oppo.
- Select polling intevals, if desired.
	- _Playback/Disc Polling_  
	  Only necessary for BDP models connected via Ethernet, this polling controls status updates for playback information (e.g. play, stop), disc type (e.g. CDDA), audio type (e.g. LPCM), and repeat mode (e.g. All).
	- _Power Status (OFF) Polling_  
	  Only necessary for BDP models connected via Ethernet, this polling controls status updates for the power when the player is currently in standby.
	- _Power Status (ON) Polling_  
	  Only necessary for BDP models connected via Ethernet, this polling controls status updates for the power when the player is currently powered on.
	- _Track Polling_  
	  This polling controls status updates for the track numbers and track metadata (e.g. artist).  All Oppo models require polling to obtain this information, regardless of connection.
	- _Time Polling_  
	  This polling controls status updates for elapsed, remaining, and total times of the disc, track, and title.  All Oppo models require polling to obtain this information, regardless of connection.

## Tested Models and Connections
- Oppo BDP-103 via Ethernet (no automatic feedback from the player; i.e. requires polling queries)
- Oppo BDP-103 via Ethernet-to-Serial module (PUSR USR-TCP232-302)
- Oppo UDP-203 via Ethernet
- Oppo UDP-203 via Ethernet-to-Serial module (PUSR USR-TCP232-302)

## Limitations
- The driver does not implement all available commands.  Please let me know if you want support for other controls (e.g. volume).  I am happy to add more features.
- Oppo BDP models require more polling with Ethernet connections.  Other setups leverage automatic feedback from the player to the driver.
- Oppo players support only one Ethernet-connected device. If something else connects to the Oppo via Ethernet, the driver will disconnect and subequently require another initialization.
- Oppo truncates the track information to 15 characters.  The driver leverages MusicBrainz to retrieve full titles, which works in many cases, but not all.  If necessary, use the manual settings for the artist, album, and track information to assist the MusicBrainz lookups.

## Notes
- The "Refresh Track Metadata" command utilizes the current artist, album, and track name values to perform another lookup in MusicBrainz.  If the metadata is incorrect, you can manually set one or more of the values (artist, album, track name) and then utilize this command to incorporate those new values in a MusicBrainz lookup.
- The "Send Command" command sends any of the supported Oppo commands to the player.  This allows control beyond the driver's specific commands.
- The state variables indicate the raw track metadata fields returned by the Oppo.
- Additional visuals can be found in the [`images`](https://github.com/jdc72/Hubitat/tree/main/oppo_disc_player/docs/images) folder within this repository.

## Support
If you encounter issues or have feature requests, feel free to open an [issue](https://github.com/jdc72/Hubitat/issues) on this repository.

