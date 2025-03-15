/**
 *  Driver Name: Oppo Disc Player
 *  Platform: Hubitat Elevation
 *  https://github.com/jdc72/Hubitat/tree/main/oppo_disc_player
 *
 *  Copyright 2025 Jeffrey D. Chapman
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Change History
 *    version 1.0.0  @  2025-03-07  - jdc72  -  Initial release
 *    version 1.0.1  @  2025-03-15  - jdc72  -  Improved MusicBrainz lookup
 *
 */

import groovy.transform.CompileStatic
import groovy.transform.Field
import java.text.SimpleDateFormat
import hubitat.device.HubAction
import hubitat.device.Protocol

@Field static final String DRIVER_NAME = "Oppo Disc Player"
@Field static final String DRIVER_VERSION = "1.0.1"
@Field static final String LINK_COMM = "https://github.com/jdc72/Hubitat/tree/main/oppo_disc_player"
@Field static final String LINK_GITHUB = "https://github.com/jdc72/Hubitat/tree/main/oppo_disc_player"
@Field static final String IMPORT_URL = "https://raw.githubusercontent.com/jdc72/Hubitat/main/oppo_disc_player/src/OppoDiscPlayer.groovy"

metadata {
	definition(name: DRIVER_NAME, namespace: "jdc72", author: "Jeff Chapman (@jdc72)", importUrl: IMPORT_URL) {
		capability "Initialize"
		capability "Refresh"
		capability "Switch"
		capability "Telnet"

		attribute "audioType", "enum", ["Dolby Digital","Dolby Digital Plus","Dolby TrueHD","DTS","DTS-HD","DTS-HD MA","LPCM","CDDA","other"]
		attribute "discType", "enum", ["Blu-ray","UHD BD","DVD-A","DVD-V","SACD","CDDA","HDCD","data","no disc","other"]
		attribute "firmwareVersion", "text"
		attribute "playbackStatus", "enum", ["no disc","open","close","loading","home","screen saver","stop","pause","play","fast forward","fast reverse","other"]
		attribute "playerType", "enum", ["BDP","UDP"]
		attribute "repeatMode", "enum", ["off","one","all","shuffle","random","other"]
		attribute "verboseMode", "text"

		attribute "chapterNum", "text"
		attribute "chapterNumTotal", "text"
		attribute "chapterTimeElapsed", "text"
		attribute "chapterTimeRemaining", "text"
		attribute "chapterTimeTotal", "text"
		attribute "discTimeElapsed", "text"
		attribute "discTimeRemaining", "text"
		attribute "discTimeTotal", "text"
		attribute "titleNum", "text"
		attribute "titleNumTotal", "text"
		attribute "trackAlbum", "text"
		attribute "trackArtist", "text"
		attribute "trackName", "text"
		attribute "trackNum", "text"
		attribute "trackNumTotal", "text"
		attribute "trackTimeElapsed", "text"
		attribute "trackTimeRemaining", "text"
		attribute "trackTimeTotal", "text"

		command "closeConnection"
		command "eject"
		command "home"
		command "play"
		command "pause"
        command "refreshTrackMetadata"
		command "stop"
		command "sendCommand", [[name: "Command", type: "STRING", description: "Command for the Oppo Media Player"]]
		command "setTrackAlbum", [[name: "Album", type: "STRING", description: "Album of the playing track"]]
		command "setTrackArtist", [[name: "Artist", type: "STRING", description: "Artist of the playing track"]]
		command "setTrackName", [[name: "Name", type: "STRING", description: "Name of the playing track"]]
		command "trackNext"
		command "trackPrevious"
	}
}

preferences {
	input name: "serverIp", type: "text", title: "Server IP Address", description: "Enter the IP address of your Oppo player or TCP/serial module connected to the Oppo's serial port", required: true
	input name: "serverPort", type: "number", title: "Server Port", description: "Enter the port number of your Oppo player (usually 48360 for BDP models and 23 for UDP models) or TCP/serial module connected to the Oppo's serial port", defaultValue: 23, required: true
	input name: "serverConnectionType", type: "enum", title: "Server Connection Type", description: "Enter the connection type to the Oppo player.  Oppo BDP models have reduced real-time information over a direct TCP connection (try to set up a serial connection via a TCP/serial module)", options:[["TCP":"Direct TCP"], ["Serial":"Serial via TCP Server"]], required: true
	input name: "playbackDiscPollingInterval", type: "number", title: "Playback/Disc Polling", description: "Enter the interval (seconds) at which to poll playback and disc information while powered ON (0: no polling) to determine changes in audio type, disc type, playback status, etc.  This polling is necessary only for Oppo BDP models with direct TCP connections", range:"0..3600", defaultValue: 0, required: false
	input name: "powerOffPollingInterval", type: "number", title: "Power Status (OFF) Polling", description: "Enter the interval (seconds) at which to poll Power Status (on/off) while powered OFF (0: no polling).  This polling is  necessary only for Oppo BDP models with direct TCP connections", range:"0..3600", defaultValue: 0, required: false
	input name: "powerOnPollingInterval", type: "number", title: "Power Status (ON) Polling", description: "Enter the interval (seconds) at which to poll Power Status (on/off) while powered ON (0: no polling).  This polling is  necessary only for Oppo BDP models with direct TCP connections", range:"0..3600", defaultValue: 0, required: false
	input name: "trackPollingInterval", type: "number", title: "Track Polling", description: "Enter the interval (seconds) at which to poll track information while playing (0: no polling) to determine changes in track numbers, names, etc.  This polling is necessary for all Oppo models and connection types, if the information is desired", range:"0..3600", defaultValue: 0, required: false
	input name: "timePollingInterval", type: "number", title: "Time Polling", description: "Enter the interval (seconds) at which to poll time information while playing (0: no polling) to determine elapsed, remaining, and total times.  This polling is necessary for all Oppo models and connection types, if the information is desired", range: "0..3600", defaultValue: 0, required: false
	input name: "logDebug", type: "bool", title: "Enable debug logging", defaultValue: false
	input name: "helpInfo", type: "hidden", title: formatHelpInfo()
}

void installed() {
	logI "installed"
	initialize()
}

void updated() {
	logI "updated"
	initialize()
}

def initialize() {
	logD "initializing..."
	unschedule()
	openConnection()
	sendCommand(QUERY_FIRMWARE_VERSION)
	sendCommand(COMMAND_VERBOSE_MODE_2)
	pauseExecution(1000)
	refresh()
}

void uninstalled() {
	logW "uninstalled"
	closeConnection()
}

void refresh() {
	logI "refreshed"
	sendCommand(QUERY_VERBOSE_MODE)
	state.remove("trackArtist")
	state.remove("trackAlbum")
	state.remove("trackName")
    queryPowerStatus()
    queryPlaybackDiscStatus()
	queryTrackTimes()
	queryTrackNumbers()
	queryTrackInfo()
}
void refreshTrackMetadata() {
    queryTrackInfo()
	runIn(1, "handleTrackInfoOnline")
}


def closeConnection() {
	try {
		logD "closing existing connections..."
		if (isTcp())
			interfaces.rawSocket.close()
		else
			telnetClose()
	}
	catch (Exception e) {
		logE "error disconnecting from ${getServerIp()}:${getServerPort()} - ${e.message}"
	}
}

def openConnection() {
	closeConnection()
	try {
		logD "connecting to ${getServerIp()}:${getServerPort()}"
		if (isTcp())
			rawSocketConnect()
		else
			tcpConnect()
	} catch (Exception e) {
		logE "error connecting to ${getServerIp()}:${getServerPort()} - ${e.message}"
	}
}
def rawSocketConnect() {
	logD "connecting to ${getServerIp()}:${getServerPort()}"
	interfaces.rawSocket.connect([eol: '\r'], getServerIp(), getServerPort())
}
def tcpConnect() {
	logD "connecting to ${getServerIp()}:${getServerPort()}"
	telnetConnect([termChars: [13]], getServerIp(), getServerPort(), null, null)
}


def socketStatus(String status) {
	logD "socket status:  $status"
}
def telnetStatus(String status) {
	logW "telnetStatus: error: " + status
	if (status != "receive error: Stream is closed") {
		logE "connection was dropped."
		initialize()
	}
}


// Command implementations
def off() {
	logD "off()"
	sendCommand(COMMAND_OFF)
}
def on() {
	logD "on()"
	sendCommand(COMMAND_ON)
}
def eject() {
	logD "eject()"
	sendCommand(COMMAND_EJECT)
}
def home() {
	logD "home()"
	sendCommand(COMMAND_HOME)
}
def pause() {
	logD "pause()"
	sendCommand(COMMAND_PAUSE)
}
def play() {
	logD "play()"
	sendCommand(COMMAND_PLAY)
}
def setTrackAlbum(String album) {
	logD "setTrackAlbum() => $album"
	if (album == device.currentValue("trackAlbum")) return
	sendEvent(name: "trackAlbum", value: album)
}
def setTrackArtist(String artist) {
	logD "setTrackArtist() => $artist"
	if (artist == device.currentValue("trackArtist")) return
	sendEvent(name: "trackArtist", value: artist)
}
def setTrackName(String name) {
	logD "setTrackName() => $name"
	if (name == device.currentValue("trackName")) return
	sendEvent(name: "trackName", value: name)
}
def stop() {
	logD "stop()"
	sendCommand(COMMAND_STOP)
}
def trackNext() {
	logD "trackNext()"
	sendCommand(COMMAND_TRACK_NEXT)
}
def trackPrevious() {
	logD "trackPrevious()"
	sendCommand(COMMAND_TRACK_PREVIOUS)
}


def sendCommand(String command) {
	try {
		String msg = formatCommand(command)
		logD "sending command: $msg"
		if (isTcp())
			interfaces.rawSocket.sendMessage(msg)
		else
			sendHubCommand(new HubAction(msg, Protocol.TELNET))
		pauseExecution(100)
	} catch (Exception e) {
		logE "error sending command: $command - ${e.message}"
	}
}
String formatCommand(String command) {
	return (isUdp() || !isTcp()) ? "#${command}" + (isTcp() ? "\r" : "") : "REMOTE $command"
}


def parse(String message) {
	try {
		String msg = decodeMessage(message)
		logD "decoded message: ${msg}"

		if (msg.startsWith("@")) {
			List<String> codes = msg.replace("@", "").split("\\s+")

			if (codes[1] == "ER") {
				logD "${codes[0]} execution error"
				switch (codes[0]) {
					case QUERY_CHAPTER_TIME_ELAPSED: handleTime("chapter", "Elapsed", TIME_000000); break
					case QUERY_CHAPTER_TIME_REMAINING: handleTime ( "chapter", "Remaining", TIME_000000); break
					case QUERY_DISC_TIME_ELAPSED: handleTime("disc", "Elapsed", TIME_000000); break
					case QUERY_DISC_TIME_REMAINING: handleTime("disc", "Remaining", TIME_000000); break
					case QUERY_TRACK_TIME_ELAPSED: handleTime("track", "Elapsed", TIME_000000); break
					case QUERY_TRACK_TIME_REMAINING: handleTime("track", "Remaining", TIME_000000); break
				}
			} else if (codes[1] == "OK") {
				logD "${codes[0]} executed successfully"
				String parameters2 = (codes.size() > 2) ? codes[2..-1].join(" ") : ""
				String parameters3 = (codes.size() > 3) ? codes[3..-1].join(" ") : ""
				switch (codes[0]) {
					case COMMAND_OFF:
					case COMMAND_ON:
					case COMMAND_POWER: if (!isVerbose()) handlePower(parameters2); break
					case COMMAND_EJECT: if (!isVerbose()) handlePlaybackStatus(parameters2); break
					case COMMAND_HOME: if (!isVerbose()) handlePlaybackStatus("HOME"); break
					case COMMAND_PAUSE: if (!isVerbose()) handlePlaybackStatus("PAUSE"); break
					case COMMAND_PLAY: if (!isVerbose()) handlePlaybackStatus("PLAY"); break
					case COMMAND_REPEAT_ROTATE: handleRepeatMode(parameters2); break
					case COMMAND_REPEAT_SET: handleRepeatMode(parameters2); break
					case COMMAND_STOP: if (!isVerbose()) handlePlaybackStatus("STOP"); break
					case QUERY_AUDIO_TYPE: handleAudioType(parameters2); break
					case QUERY_CHAPTER_NUM: handleChapterNumbers(parameters2); break
					case QUERY_CHAPTER_TIME_ELAPSED: handleTime("chapter", "Elapsed", parameters2); break
					case QUERY_CHAPTER_TIME_REMAINING: handleTime("chapter", "Remaining", parameters2); break
					case QUERY_DISC_TIME_ELAPSED: handleTime("disc", "Elapsed", parameters2); break
					case QUERY_DISC_TIME_REMAINING: handleTime("disc", "Remaining", parameters2); break
					case QUERY_DISC_TYPE: handleDiscType(parameters2); break
					case QUERY_FIRMWARE_VERSION: handleFirmwareVersion(parameters2)
					case QUERY_PLAYBACK_STATUS: handlePlaybackStatus(parameters2); break
					case QUERY_POWER_STATUS: handlePower(parameters2); break
					case QUERY_REPEAT_MODE: handleRepeatMode(parameters3); break
					case QUERY_TRACK_ALBUM: handleTrackInfo("trackAlbum", parameters2); break
					case QUERY_TRACK_ARTIST: handleTrackInfo("trackArtist", parameters2); break
					case QUERY_TRACK_NAME: handleTrackInfo("trackName", parameters2); break
					case QUERY_TRACK_NUM: handleTrackNumbers(parameters2); break
					case QUERY_TRACK_TIME_ELAPSED: handleTime("track", "Elapsed", parameters2); break
					case QUERY_TRACK_TIME_REMAINING: handleTime("track", "Remaining", parameters2); break
					case QUERY_VERBOSE_MODE: handleVerboseMode(parameters2); break
				}
			} else {
				String parameters = (codes.size() > 1) ? codes[1..-1].join(" ") : ""
				switch (codes[0]) {
					case UPDATE_AUDIO_TYPE: handleAudioType(parameters); break
					case UPDATE_DISC_TYPE: handleDiscType(parameters); break
					case UPDATE_PLAYBACK_STATUS: handlePlaybackStatus(parameters); break
					case UPDATE_POWER_STATUS: handlePower(parameters); break
				}
			}
		} else if (msg == HEARTBEAT_PACKET) {
			logD "received heartbeat message"
		} else {
			logD "unexpected message format"
		}
	} catch (Exception e) {
		logE "error parsing message: ${e.message}"
	}
}
String decodeMessage(String message) {
	if (isTcp()) {
		logD "raw message: ${message}"
		return new String(message.decodeHex(), "UTF-8")
	}
	return message
}


void schedulePlaybackDiscPolling() {
	if (getPlaybackDiscPolling() > 0 && (String)device.currentValue("switch") == "off")
		runIn(getTrackNumbersPolling(), "queryPlaybackDiscStatus")
}
void queryPlaybackDiscStatus() {
	unschedule("queryPlaybackDiscStatus")
	sendCommand(QUERY_AUDIO_TYPE)
	sendCommand(QUERY_DISC_TYPE)
	sendCommand(QUERY_PLAYBACK_STATUS)
	sendCommand(QUERY_REPEAT_MODE)
	schedulePlaybackDiscPolling()
}

void schedulePowerStatusPolling() {
    int interval = ((String)device.currentValue("switch") == "off") ? getPowerOffPolling() : getPowerOnPolling()
	if (interval > 0)
		runIn(interval, "queryPowerStatus")
}
void queryPowerStatus() {
	unschedule("queryPowerStatus")
	sendCommand(QUERY_POWER_STATUS)
	schedulePowerStatusPolling()
}

void queryTrackInfo() {
	if (isUdp() && (String)device.currentValue("discType") in ["CDDA","HDCD","SACD"]) {
		sendCommand(QUERY_TRACK_ALBUM)
		sendCommand(QUERY_TRACK_ARTIST)
		sendCommand(QUERY_TRACK_NAME)
	}
}

void scheduleTrackNumbersPolling() {
	if (getTrackNumbersPolling() > 0 && (String)device.currentValue("playbackStatus") == "play")
		runIn(getTrackNumbersPolling(), "queryTrackNumbers")
}
void queryTrackNumbers() {
	unschedule("queryTrackNumbers")
	sendCommand(QUERY_TRACK_NUM)
	sendCommand(QUERY_CHAPTER_NUM)
	scheduleTrackNumbersPolling()
}

void scheduleTrackTimesPolling() {
	if (getTrackTimesPolling() > 0 && (String)device.currentValue("playbackStatus") == "play")
		runIn(getTrackTimesPolling(), "queryTrackTimes")
}
void queryTrackTimes() {
	unschedule("queryTrackTimes")
	sendCommand(QUERY_TRACK_TIME_ELAPSED)
	sendCommand(QUERY_TRACK_TIME_REMAINING)
	sendCommand(QUERY_CHAPTER_TIME_ELAPSED)
	sendCommand(QUERY_CHAPTER_TIME_REMAINING)
	sendCommand(QUERY_DISC_TIME_ELAPSED)
	sendCommand(QUERY_DISC_TIME_REMAINING)
	runIn(2, "handleChapterTimes")
	runIn(2, "handleDiscTimes")
	runIn(2, "handleTrackTimes")
	scheduleTrackTimesPolling()
}


// Response implementations
def handleAudioType(String code) {
	List<String> codes = code.toUpperCase().replace("@", "").split("\\s+")
	String type
	switch (codes[0]) {
		case "CD": type = "CDDA"; break
		case "LPCM":
		case "PC":
		case "PCM": type = "LPCM"; break
		case "DD": type = "Dolby Digital"; break
		case "DP": type = "Dolby Digital Plus"; break
		case "DSD": type = "DSD"; break
		case "DT":
		case "DTS": type = "Dolby TrueHD"; break
		case "TH":
		case "DTS-HD": type = "DTS-HD"; break
		case "TM": type = "DTS-HD MA"; break
		case "TS": type = "DTS"; break
		case "NONE": type = "no audio"; break
		default: type = "other"
	}
	sendEvent(name: "audioType", value: type)
}
def handleChapterNumbers(String code, boolean forceTotal = false) {
	List<String> numbers = code.split("/")
	sendEvent(name: "chapterNum", value: numbers[0])
	if (numbers.size() > 1 && (isNumTotalOkayToSet(numbers[1]) || forceTotal)) {
		sendEvent(name: "chapterNumTotal", value: numbers[1])
	}
}
def handleChapterTimes() {
	handleTimeTotal("chapter")
}
def handleDiscTimes() {
	int pollingInterval = getTrackTimesPolling()
	if (pollingInterval > 0) {
		String timeRemaining = (String)device.currentValue("discTimeRemaining") ?: TIME_000000
		if (timeRemaining == TIME_000000)
			return
	}
	handleTimeTotal("disc")
}
def handleDiscType(String code) {
	String type
	switch (code.toUpperCase()) {
		case "BDMV":
		case "BD-MV": type = "Blu-ray"; break
		case "CDDA": type = "CDDA"; break
		case "DATA":
		case "DATA-DISC": type = "data"; break
		case "DVDA":
		case "DVD-AUDIO": type = "DVD-A"; break
		case "DVDV":
		case "DVD-VIDEO": type = "DVD-V"; break
		case "HDCD": type = "HDCD"; break
		case "NO-DISC": type = "no disc"; break
		case "SACD": type = "SACD"; break
		case "UHBD": type = "UHD BD"; break
		default: type = "other"
	}
	sendEvent(name: "discType", value: type)

	if (type == "no disc") {
		handleAudioType("NONE")
		handleTime("disc", "Total", TIME_000000)
		handleTrackInfo("trackAlbum", "N/A")
		handleTrackInfo("trackArtist", "N/A")
		handleTrackInfo("trackName", "N/A")
		handleTrackNumbers("00/00", true)
		handleChapterNumbers("00/00", true)
	}
}
def handleFirmwareVersion(String code) {
	String type = (code.startsWith("BDP")) ? "BDP" : "UDP"
	sendEvent(name: "firmwareVersion", value: code)
	sendEvent(name: "playerType", value: type)
}
def handlePlaybackStatus(String code) {
	String status
	switch (code.toUpperCase()) {
		case "DISC": status = "no disc"; break
		case "CLOS":
		case "CLOSE": status = "close"; break
		case "FFW1":
		case "FFW2":
		case "FFW3":
		case "FFW4":
		case "FFW5":
		case "FFWD": status = "fast forward"; break
		case "FRV1":
		case "FRV2":
		case "FRV3":
		case "FRV4":
		case "FRV5":
		case "FREV": status = "fast reverse"; break
		case "HOME":
		case "HOME MENU": status = "home"; break
		case "LOAD": status = "loading"; break
		case "OFF": status = "off"; break
		case "OPEN": status = "open"; break
		case "PAUS":
		case "PAUSE": status = "pause"; break
		case "PLAY": status = "play"; break
		case "SCSV":
		case "SCREEN SAVER": status = "screen saver"; break
		case "STOP": status = "stop"; break
		default: status = "other"
	}
	sendEvent(name: "playbackStatus", value: status)

	if (status == "play") {
		queryTrackInfo()
		queryTrackNumbers()
		queryTrackTimes()
	}
	else if (status in ["no disc","open"]) {
		if (device.currentValue("discType") != "no disc")
			handleDiscType("NO-DISC")
	}
	else if (status in ["home","off"]) {
		handleTrackInfo("trackName", "N/A")
	}
}
def handlePower(String code) {
	String power = (code.toUpperCase() in ["OFF", "0"]) ? "off" : "on"
	sendEvent(name: "switch", value: power)

	if (power == "off") {
		handlePlaybackStatus("OFF")
	}
}
def handleRepeatMode(String code) {
	String mode
	switch (code.toUpperCase()) {
		case "OFF": mode = "off"; break
		case "ALL":
		case "REPEAT ALL": mode = "all"; break
		case "REPEAT ONE":
		case "REPEAT CHAPTER":
		case "REPEAT TITLE":
		case "TT": mode = "one"; break
		case "RND":
		case "RANDOM": mode = "random"; break
		case "SHF":
		case "SHUFFLE": mode = "shuffle"; break
		default: mode = "other"
	}
	sendEvent(name: "repeatMode", value: mode)
}
def handleTime(String type, String perspective, String code) {
	sendEvent(name: type + "Time" + perspective, value: code)
}
def handleTimeTotal(String timeType) {
	String timeElapsed = (String)device.currentValue(timeType + "TimeElapsed") ?: TIME_000000
	String timeRemaining = (String)device.currentValue(timeType + "TimeRemaining") ?: TIME_000000
	String timeTotal = addTimes(timeElapsed, timeRemaining)
	sendEvent(name: timeType + "TimeTotal", value: timeTotal)
}
def handleTrackInfo(String type, String code) {
	String codeC = capitalizeWords(code)
	if (!isUdp() || codeC == (String)state."$type")
		return
	if (codeC.endsWith("*"))
		runIn(1, "handleTrackInfoOnline")
	state."$type" = codeC
	sendEvent(name: type, value: codeC)
}
def handleTrackInfoOnline() {
	String artistRaw = device.currentValue("trackArtist")
	String albumRaw = device.currentValue("trackAlbum")
	String trackRaw = device.currentValue("trackName")
	Map<String,String> result = getTrackDataOnline(trackRaw, artistRaw, albumRaw)
	String artistNew = (!artistRaw.endsWith("*")) ? artistRaw : result.artist
	String albumNew = (!albumRaw.endsWith("*")) ? albumRaw : result.album
	String trackNew = (!trackRaw.endsWith("*")) ? trackRaw : result.track
	if (artistNew.endsWith("*") || trackNew.endsWith("*")) {
		result = getTrackDataOnline(trackRaw, artistRaw)
		if (artistNew.endsWith("*") && !result.artist.endsWith("*"))
			artistNew = result.artist
		if (trackNew.endsWith("*") && !result.track.endsWith("*"))
			trackNew = result.track
	}
	if (artistNew.endsWith("*") || albumNew.endsWith("*")) {
		result = getAlbumDataOnline(artistRaw, albumRaw)
		if (artistNew.endsWith("*") && !result.artist.endsWith("*"))
			artistNew = result.artist
		if (albumNew.endsWith("*") && !result.album.endsWith("*"))
			albumNew = result.album
	}
	if (artistNew != artistRaw) {
		sendEvent(name: "trackArtist", value: artistNew)
		logD "online override of trackArtist from $artistRaw to $artistNew"
	}
	if (albumNew != albumRaw) {
		sendEvent(name: "trackAlbum", value: albumNew)
		logD "online override of trackAlbum from $albumRaw to $albumNew"
	}
	if (trackNew != trackRaw) {
		sendEvent(name: "trackName", value: trackNew)
		logD "online override of trackName from $trackRaw to $trackNew"
	}
}
def handleTrackNumbers(String code, boolean forceTotal = false) {
	List<String> numbers = code.split("/")
	String trackNum = device.currentValue("trackNum")
	sendEvent(name: "trackNum", value: numbers[0])
	sendEvent(name: "titleNum", value: numbers[0])
	if (numbers.size() > 1 && (isNumTotalOkayToSet(numbers[1]) || forceTotal)) {
		sendEvent(name: "trackNumTotal", value: numbers[1])
		sendEvent(name: "titleNumTotal", value: numbers[1])
	}
	if (trackNum != numbers[0]) {
		queryTrackInfo()
		queryTrackTimes()
	}
}
def handleTrackTimes() {
	handleTimeTotal("track")
}
def handleVerboseMode(String code) {
	sendEvent(name: "verboseMode", value: code)
	if (code != "0") {
		if (getPlaybackDiscPolling() > 0)
			disablePolling("playbackDiscPollingInterval", "Playback/Disc", code)
		if (getPowerOffPolling() > 0)
			disablePolling("powerOffPollingInterval", "Power (OFF)", code)
		if (getPowerOnPolling() > 0)
			disablePolling("powerOnPollingInterval", "Power (ON)", code)
	}
}
void disablePolling(String pollingSetting, String description, String verboseMode) {
	device.updateSetting(pollingSetting, [type: "number", value: 0])
	logW "$description polling is disabled, as the Oppo automatically sends updates in this verbose mode ($verboseMode)."
}

boolean isNumTotalOkayToSet(String number) {
	if (number == null) return false
	return ((number != "00") || ((String)device.currentValue("discType") ?: "no disc") == "no disc")
}


Map<String,String> getAlbumDataOnline(String artistRaw, String albumRaw) {
	String query = "artist:${getTrackDataSearch(artistRaw)} AND release:${getTrackDataSearch(albumRaw)}"
	String url = "https://musicbrainz.org/ws/2/release/?fmt=json&limit=1&query=${URLEncoder.encode(query, 'UTF-8')}"
	Map<String,Object> data = httpGetResponse(url)

	if (data != null && data.releases != null) {
		List<Object> releases = (List<Map<String,Object>>)data.releases
		logD "MusicBrainz query = $query"
		logD "MusicBrainz releases = $releases"
		if (releases?.size() > 0) {
			Map<String,Object> release = (Map<String,Object>)releases[0]
			List<Map<String,Object>> artists = (List<Map<String,Object>>)release.get("artist-credit")
			if (artists?.size() > 0) {
				String artistOnline = capitalizeWords((String)artists[0].name)
				String albumOnline = capitalizeWords((String)release.title)
				if (isTrackDataComparable(artistRaw, artistOnline) &&
					isTrackDataComparable(albumRaw, albumOnline))
					return [
						artist: !artistRaw.endsWith("*") ? artistRaw : artistOnline,
						album : !albumRaw.endsWith("*") ? albumRaw : albumOnline,
					]
			}
		}
	}
	return [artist: artistRaw, album: albumRaw]
}

Map<String,String> getTrackDataOnline(String trackRaw, String artistRaw, String albumRaw = null) {
	String trackSearch = getTrackDataSearch(trackRaw)
	String query = "artist:${getTrackDataSearch(artistRaw)} AND " +
		(trackRaw.endsWith("*") ? "recording:${trackSearch}" : "recording:\"${trackSearch}\"^2") +
		((albumRaw != null) ? " AND release:${getTrackDataSearch(albumRaw)}" : "")
	String url = "https://musicbrainz.org/ws/2/recording/?fmt=json&limit=1&query=${URLEncoder.encode(query, 'UTF-8')}"
	Map<String,Object> data = httpGetResponse(url)

	if (data != null && data.recordings != null) {
		List<Object> recordings = (List<Object>)data.recordings
		logD "MusicBrainz query = $query"
		logD "MusicBrainz recordings = $recordings"
		if (recordings?.size() > 0) {
			Map<String,Object> recording = (Map<String,Object>)recordings[0]
			List<Map<String,Object>> artists = (List<Map<String,Object>>)recording.get("artist-credit")
			List<Map<String,Object>> releases = (List<Map<String,Object>>)recording.get("releases")
			if (artists.size() > 0 && releases.size() > 0) {
				Map<String,Object> artistOnline = (Map<String,Object>)artists[0].artist
				String artistOnlineName = capitalizeWords((String)artistOnline?.name ?: "zz")
				String artistOnlineSort = capitalizeWords((String)artistOnline?."sort-name" ?: "zz")
				String albumOnline = capitalizeWords((String)releases[0].title)
				String trackOnline = capitalizeWords((String)recording.title)
				if ((isTrackDataComparable(artistRaw, artistOnlineName) || isTrackDataComparable(artistRaw, artistOnlineSort)) &&
					isTrackDataComparable(albumRaw, albumOnline) &&
					isTrackDataComparable(trackRaw, trackOnline))
					return [
						artist: !artistRaw.endsWith("*") ? artistRaw : artistOnlineName,
						album : !albumRaw?.endsWith("*") ? albumRaw : albumOnline,
						track : !trackRaw.endsWith("*") ? trackRaw : trackOnline
					]
			}
		}
	}
	return [artist: artistRaw, album: albumRaw, track: trackRaw]
}

static boolean isTrackDataComparable(String dataRaw, String dataOnline) {
	String compareRaw = getTrackDataCompare(dataRaw)
	String compareOnline = getTrackDataCompare(dataOnline)
	return (dataRaw == null || compareOnline.contains(compareRaw))
}
static String getTrackDataCompare(String data) {
	if (data == null) return null
	String dataCompare = getTrackDataSearch(data)
	Map<String,String> termsToRemove = ["The": "", ",": " ", "'": "", "’": "", "-": " ", "*": "", "+": " "]
	termsToRemove.each { termToRemove ->
		dataCompare = dataCompare.replace(termToRemove.key, termToRemove.value)
	}
	return dataCompare.toUpperCase().replaceAll("\\s+", " ").trim()
}
static String getTrackDataSearch(String data) {
	if (data == null) return null
	String dataSearch = (new String(data)).replaceAll("\\[.*?\\]", "")
	return trimEnd(dataSearch.replaceAll("\\s+", " "), "+").trim()
}

Map<String,Object> httpGetResponse(String url) {
	Map<String,Object> result = null
	httpGet(url) { response ->
		if (response.status == 200) {
			result = response.data
		} else {
			logW "$url search failed with status: ${response.status}"
		}
	}
	return result
}


// Attribute Helpers
boolean isUdp() {
	return device.currentValue("playerType") == "UDP"
}
boolean isVerbose() {
	return (((String)device.currentValue("verboseMode") ?: "0") != "0")
}


// Input Helpers
String getServerIp() {
	return (String)settings.serverIp
}
int getServerPort() {
	return (int)settings.serverPort
}
int getPlaybackDiscPolling() {
	return (int)settings.playbackDiscPollingInterval
}
int getPowerOffPolling() {
	return (int)settings.powerOffPollingInterval
}
int getPowerOnPolling() {
	return (int)settings.powerOnPollingInterval
}
int getTrackNumbersPolling() {
	return (int)settings.trackPollingInterval
}
int getTrackTimesPolling() {
	return (int)settings.timePollingInterval
}
boolean isTcp() {
	return ((String)settings.serverConnectionType == "TCP")
}


// Utilities
@CompileStatic
static String addTimes(String time1, String time2, String format = "HH:mm:ss") {
	SimpleDateFormat dateFmt = new SimpleDateFormat(format)
	dateFmt.setTimeZone(TimeZone.getTimeZone('UTC'))
	Long timeMs1 = dateFmt.parse(time1)?.getTime() ?: 0
	Long timeMs2 = dateFmt.parse(time2)?.getTime() ?: 0
	Long timeMs = timeMs1 + timeMs2
	Date timeDt = new Date(timeMs)
	return dateFmt.format(timeDt)
}
@CompileStatic
static String capitalizeWords(String text, String joinText = " ") {
	return text.split(' ').collect { String it -> it.capitalize() }.join(joinText)
}
@CompileStatic
static String formatHelpInfo() { // credit kkossev, jtp10181
	String description = "$DRIVER_NAME v${DRIVER_VERSION}"
	String linkGitHub = "<a href='${LINK_GITHUB}' target='_blank' style='font-size:90%;'>GitHub</a>"
	String linkCommunity = "<a href='${LINK_COMM}' target='_blank' style='font-size:90%;'>Hubitat Community</a>"
	return "<div style='padding:2px 0; font-weight:bold; text-align:center;'>${description}<br>${linkGitHub}<br>${linkCommunity}</div>"
}
@CompileStatic
static String trimEnd(String text, String trimString) {
	return (text?.endsWith(trimString)) ? text.substring(0, text.length() - trimString.length()) : text
}

void log(String level, String msg) {
	log."$level"("${device.displayName} :: ${msg}")
}
void logD(String msg) {
	if (settings.logDebug == true) log "debug", msg
}
@CompileStatic
void logE(String msg) {
	log "error", msg
}
@CompileStatic
void logI(String msg) {
	log "info", msg
}
@CompileStatic
void logW(String msg) {
	log "warn", msg
}

// Constants
@Field public static final String COMMAND_EJECT = "EJT"
@Field public static final String COMMAND_HOME = "HOM"
@Field public static final String COMMAND_OFF = "POF"
@Field public static final String COMMAND_ON = "PON"
@Field public static final String COMMAND_PAUSE = "PAU"
@Field public static final String COMMAND_PLAY = "PLA"
@Field public static final String COMMAND_POWER = "POW"
@Field public static final String COMMAND_REPEAT_ROTATE = "RPT"
@Field public static final String COMMAND_REPEAT_SET = "SRP"
@Field public static final String COMMAND_STOP = "STP"
@Field public static final String COMMAND_TRACK_NEXT = "NXT"
@Field public static final String COMMAND_TRACK_PREVIOUS = "PRE"
@Field public static final String COMMAND_VERBOSE_MODE_2 = "SVM 2"
@Field public static final String HEARTBEAT_PACKET = "HEARTBEAT"
@Field public static final String QUERY_AUDIO_TYPE = "QAT"
@Field public static final String QUERY_CHAPTER_NUM = "QCH"
@Field public static final String QUERY_CHAPTER_TIME_ELAPSED = "QCE"
@Field public static final String QUERY_CHAPTER_TIME_REMAINING = "QCR"
@Field public static final String QUERY_DISC_TIME_ELAPSED = "QEL"
@Field public static final String QUERY_DISC_TIME_REMAINING = "QRE"
@Field public static final String QUERY_DISC_TYPE = "QDT"
@Field public static final String QUERY_FIRMWARE_VERSION = "QVR"
@Field public static final String QUERY_PLAYBACK_STATUS = "QPL"
@Field public static final String QUERY_POWER_STATUS = "QPW"
@Field public static final String QUERY_REPEAT_MODE = "QRP"
@Field public static final String QUERY_TRACK_ALBUM = "QTA"
@Field public static final String QUERY_TRACK_ARTIST = "QTP"
@Field public static final String QUERY_TRACK_NAME = "QTN"
@Field public static final String QUERY_TRACK_NUM = "QTK"
@Field public static final String QUERY_TRACK_TIME_ELAPSED = "QTE"
@Field public static final String QUERY_TRACK_TIME_REMAINING = "QTR"
@Field public static final String QUERY_VERBOSE_MODE = "QVM"
@Field public static final String TIME_000000 = "00:00:00"
@Field public static final String UPDATE_AUDIO_TYPE = "UAT"
@Field public static final String UPDATE_DISC_TYPE = "UDT"
@Field public static final String UPDATE_PLAYBACK_STATUS = "UPL"
@Field public static final String UPDATE_POWER_STATUS = "UPW"
