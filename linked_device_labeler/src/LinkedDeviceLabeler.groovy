/**
 *  App Name: Linked Device Labeler
 *  Platform: Hubitat Elevation
 *  https://github.com/jdc72/Hubitat/tree/main/linked_device_labeler
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
 *    version 1.0.0  @  2025-04-01  -  jdc72  -  Initial release
 *
 */

import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.app.*

@Field static final String APP_NAME = "Linked Device Labeler"
@Field static final String APP_VERSION = "1.0.0"
@Field static final String LINK_COMM = "https://github.com/jdc72/Hubitat/tree/main/linked_device_labeler"
@Field static final String LINK_GITHUB = "https://github.com/jdc72/Hubitat/tree/main/linked_device_labeler"


definition(
	name: APP_NAME,
	namespace: "jdc72",
	author: "Jeffrey D. Chapman",
	description: "App to label devices linked from other hubs",
	category: "Convenience",
	singleInstance: true,
	singleThreaded: true,
	installOnOpen: true,
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: "")


preferences {
	page(name: "pageMain")
	page(name: "pageDeviceSelection")
	page(name: "pageSettings")
}


//=========================================================================
// Pages
//=========================================================================

//-------------------------------------------------------------------------
// Page:  Main
//-------------------------------------------------------------------------

def pageMain() {
	dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {
		section(titleApp()) {
			String labelSuffix = (String)settings.labelSuffix
			DeviceWrapperList allDevicesWrappers = getSettingsDeviceWrapperList("devices_all")
			Map<String,DeviceWrapper> linkedDevicesWrappers = [:]
			Map<String,Map<String,String>> linkedDevicesExclude = [:]
			Map<String,Map<String,String>> linkedDevicesInclude = [:]
			Map<String,Map<String,String>> linkedDevicesSame = [:]
			Map<String,Map<String,String>> linkedDevicesAll = [:]
			Map<String,Map<String,String>> linkedDevicesNow = (Map<String,Map<String,String>>)state.linkedDevices ?: [:]
			allDevicesWrappers?.findAll { it.controllerType == "LNK" }?.each {
				Map<String,String> dvcInfo = [id: it.id, name: it.name, labelNow: it.label]
				dvcInfo["labelNew"] = getLabelNew(dvcInfo, labelSuffix)
				dvcInfo["labelSame"] = (dvcInfo["labelNew"] == dvcInfo["labelNow"]) ? "Y" : "N"
				dvcInfo["include"] = dvcInfo["labelSame"] == "Y" ? "N" : linkedDevicesNow."${it.id}"?."include" ?: "Y"
				if ((dvcInfo["labelNow"] == null || !((Boolean)settings.onlyLabelsNull ?: false)) &&
					(dvcInfo["labelSame"] == "N" || !((Boolean)settings.onlyLabelsChanging ?: false))) {
					if (dvcInfo["labelSame"] == "Y")
						linkedDevicesSame[it.id] = dvcInfo
					else if (dvcInfo["include"] == "N")
						linkedDevicesExclude[it.id] = dvcInfo
					else
						linkedDevicesInclude[it.id] = dvcInfo
				}
				linkedDevicesAll[it.id] = dvcInfo
				linkedDevicesWrappers[it.id] = it
			}
			Map<String,Map<String,String>> linkedDevices = linkedDevicesInclude + linkedDevicesExclude + linkedDevicesSame
			state.linkedDevices = linkedDevices

			paragraph formatHeader(1, "Settings")
			href(
				name: "pageSettingsManage",
				page: "pageSettings",
				title: "Settings",
				description: "Manage app name and logging"
			)
			paragraph formatHeader(1, "Devices")
			href(
				name: "pageDeviceSelectionManage",
				page: "pageDeviceSelection",
				title: "Devices",
				description: getDevicesDescription(linkedDevicesAll, allDevicesWrappers?.size() ?: 0)
			)
			paragraph formatHeader(1, "Labels")
			if (linkedDevices.size() > 0) {
				paragraph formatHeader(2, "Controls")
				inputText("labelSuffix", "Enter a string to append to each label (optional)", [EOL: true])
				inputBoolean("onlyLabelsNull", "Include only devices currently without labels", [EOL: true])
				if ((Boolean)settings.onlyLabelsNull ?: false)
					app.updateSetting("onlyLabelsChanging", [type: "bool", value: true])
				else
					inputBoolean("onlyLabelsChanging", "Include only devices whose labels would change", [EOL: true])
				if (linkedDevicesInclude.size() > 0) {
					state.deviceIdsIncluded = (List<String>)linkedDevicesInclude.collect { it.key }
					paragraph formatHeader(3, "Apply")
					paragraph "Apply <strong>Device Label [Proposed]</strong> to all devices selected below"
					inputButton("btnApplyLabels", "Apply Device Labels", [EOL:true])
				} else {
					paragraph "No label suggestions for the selected devices."
				}
				paragraph formatHeader(2, "Values")
				sectionDeviceValues(linkedDevicesInclude, linkedDevicesExclude, linkedDevicesSame)
			}
			sectionFooter()
		}
	}
}

@CompileStatic
static String getDevicesDescription(Map<String,Map<String,String>> linkedDevices, int devicesTotalNum) {
	String desc = ""
	if (devicesTotalNum > 0) {
		List<String> descriptions = [devicesTotalNum + " total devices"]
		int devicesLinkedNum = (linkedDevices.size() ?: 0)
		if (devicesLinkedNum > 0) {
			Number devicesNullLabelNum = linkedDevices?.count { it.value?.labelNow == null } ?: 0
			Map<String,String> latestDevice = linkedDevices?.values()?.max { it?."id"?.toInteger() }
			descriptions.add(devicesLinkedNum + " linked devices")
			descriptions.add(devicesNullLabelNum + " linked devices without labels")
			descriptions.add("Most recently linked device: " + latestDevice["labelNow"] ?: latestDevice["name"])
		}
		desc = getFormat("description", descriptions?.collect { bullet(3) + it }?.join("\n"))
	}
	return (desc.isEmpty() ? "" : desc + lpx(6)) + "$DESC_DEVICES\n"
}

@CompileStatic
static String getLabelNew(Map<String,String> deviceInfo, String labelSuffix) {
	List<String> nameTerms = deviceInfo["name"].split(" on ").toList()
	if (nameTerms.size() < 2) return deviceInfo["labelNow"] ?: deviceInfo["name"]
	return nameTerms[0..-2].join(" on ") + (labelSuffix ?: "")
}


def sectionDeviceValues(Map<String,Map<String,String>> linkedDevicesInclude,
	Map<String,Map<String,String>> linkedDevicesExclude, Map<String,Map<String,String>> linkedDevicesSame) {
	boolean areAllIncluded = linkedDevicesExclude.isEmpty()
	int rowNum = 0; int rowNumHeader = 15
	List<Map<String,String>> linkedDevices = getDevicesSorted(linkedDevicesInclude) +
		getDevicesSorted(linkedDevicesExclude) + getDevicesSorted(linkedDevicesSame)
	linkedDevices.each { Map<String,String> device ->
		rowNum = (rowNum == rowNumHeader) ? 1 : rowNum + 1
		if (rowNum % rowNumHeader == 1)
			sectionHeaderRow(areAllIncluded)
		sectionDeviceRow(device)
	}
}
def sectionHeaderRow(boolean areAllIncluded) {
	String checkboxButtonName = "btnInclude|ALL|" + (areAllIncluded ? "exclude" : "include")
	sectionHeaderColumn(inputButtonCheckbox(checkboxButtonName, areAllIncluded, "white"), [align: "center", width: 1])
	sectionHeaderColumn("Device ID", [align: "center", width: 1])
	sectionHeaderColumn("Device Label")
	sectionHeaderColumn("Device Label [Proposed]")
	sectionHeaderColumn("Device Name", [width: 4])
}
def sectionHeaderColumn(String name, Map params = null) {
	String columnHdrFmt = indent(gap() + fpx(12, name), 10, (String)params?.align ?: "left")
	paragraph formatHeader(3, columnHdrFmt, false), width: (Integer)params?.width ?: 3
}
def sectionDeviceRow(Map<String,String> device) {
	boolean isSame = (device["labelSame"] == "Y") ? null : "N"
	boolean isIncluded = (device["include"] == "Y")
	String color = isIncluded ? null : COLOR_IGNORED
	String checkboxButtonName = "btnInclude|${device.id}|" + (isIncluded ? "exclude" : "include")
	String checkboxValue = (isSame) ? "" : inputButtonCheckbox(checkboxButtonName, isIncluded)
	sectionDeviceColumn(checkboxValue, [color: color, align: "center", width: 1])
	sectionDeviceColumn(hrefLinkDevicePage(device["id"], device["id"]), [color: color, align: "center", width: 1])
	sectionDeviceColumn(device["labelNow"], [color: color])
	sectionDeviceColumn(device["labelNew"], [color: color, bold: isSame])
	sectionDeviceColumn(device["name"], [color: color, width: 4])
}
def sectionDeviceColumn(String value, Map params = null) {
	String valueFmt1 = (params?.bold != null) ? "<strong>$value</strong>" : value
	String valueFmt2 = (params?.color != null) ? fpx(14, valueFmt1, (String)params.color) : fpx(14, valueFmt1)
	paragraph indent(valueFmt2, 10, (String)params?.align ?: "left"), width: (Integer)params?.width ?: 3
}

@CompileStatic
static List<Map<String,String>> getDevicesSorted(Map<String,Map<String,String>> devices) {
	devices.values().sort { d1, d2 ->
		if (d1["labelNow"] == null && d2["labelNow"] != null) return -1
		if (d2["labelNow"] == null && d1["labelNow"] != null) return -1
		if (d1["labelNow"] != null && d2["labelNow"] != null)
			return d1["labelNow"].toLowerCase() <=> d2["labelNow"].toLowerCase()
		return d1["name"].toLowerCase() <=> d2["name"].toLowerCase()
	}
}


//-------------------------------------------------------------------------
// Page:  Settings
//-------------------------------------------------------------------------

def pageSettings() {
	dynamicPage(name: "pageSettings", title: "", nextPage: "pageMain") {
		section(titleApp()) {
			paragraph formatHeader(2, "Application Label")
			inputText("linkedDeviceLabelerLabel", "Application label for the Linked Device Labeler",
				[defaultValue: getAppLabel(), required: true])
			paragraph formatHeader(2, "Logging")
			inputBoolean("loggingDebug", "Enable debug logging")
			if (settings.loggingDebug == true)
				inputBoolean("loggingTrace", "Enable trace logging")
			else
				app.updateSetting("loggingTrace", [type: "bool", value: false])
			paragraph ""
			sectionFooter()
		}
	}
}
String getAppLabel() {
	String label = (String)settings.linkedDeviceLabelerLabel
	return (label != null && !label.isEmpty()) ? label : APP_NAME
}


//-------------------------------------------------------------------------
// Page:  Device Selection
//-------------------------------------------------------------------------

def pageDeviceSelection() {
	dynamicPage(name: "pageDeviceSelection", nextPage: "pageMain") {
		section(titleApp()) {
			paragraph formatHeader(1, "Device Data Migration")
			paragraph formatHeader(2, "Battery Devices")
			inputCapability("devices_all", DESC_DEVICES, CAPABILITY_ALL)
			paragraph fpx(14, "Hint: select all devices, as this app operates only on devices linked from other hubs to this hub.", "grey")
			sectionFooter()
		}
	}
}
@Field public static final String DESC_DEVICES = "Select linked devices for label adjustments"


//-------------------------------------------------------------------------
// Page:  ALL:  Formatting
//-------------------------------------------------------------------------

@CompileStatic
static String formatTitle(String text1, String text2 = null) {
	return "<div style='display:inline-block; width:100%; border-radius:0; color:#1A77C9; background-color:#FFFFFFFF; padding:15px 15px; margin:5px 0 0 0; box-shadow:0px 0px 5px 1px #1A77C9;'>${fpx(22, text1)}${text2 == null ? "" : fpx(16, "\n$text2", "#49535c")}</div>"
}
@CompileStatic
static String formatHeader(Integer headerNum, String text, boolean addPadding = true) {
	String color1 = HEADER_COLORS[headerNum].color1
	String color2 = HEADER_COLORS[headerNum].color2
	String background = " background:linear-gradient(0deg, $color1 0%, $color2 40%, $color2 60%, $color1 100%);"
	String boxShadow = (headerNum in [1,2]) ? "box-shadow:0px 0px 3px 1px rgba(0,0,0,0.3);" : ""
	String margin = (headerNum in [1,2]) ? "margin:5px 0 0 0;" : ""
	String padding = (addPadding) ? "padding:${HEADER_PADDING[headerNum]};" : ""
	String txt = (headerNum == 1) ? fpx(18, text) : text
	return "<div style='color:#FFFFFFFF; $background $padding $margin $boxShadow'>${txt}</div>"
}
@Field public static final Map<Integer,Map<String,String>> HEADER_COLORS = [
		1: [color1:"#20599E", color2:"#1A77C9"],
		2: [color1:"#197A41", color2:"#32994D"],
		3: [color1:"#616161", color2:"grey"],
		4: [color1:"#878787", color2:"#99A3A4"],
	]
@Field public static final Map<Integer,String> HEADER_PADDING = [1:"7px 15px", 2:"7px 15px", 3:"4px 15px", 4:"4px 15px"]

@CompileStatic
static String getFormat(String type, String text = "", String formatValue = "black") { // Modified from @Stephack Code / @BPTWorld
	if (type == "bold-red") return "<div style='color:firebrick;font-weight: bold'>${text}</div>"
	if (type == "font") return "<span style='font-family:${formatValue}'>${text}</span>"
	if (type == "highlight") return "<div style='color:#ffffff;background-color:${formatValue}'>${text}</div>"
	if (type == "line") return "<div style='width:100%; height:1px; border-bottom:1px solid #1A77C9;'></div>"
	if (type == "line2") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if (type == "line-grey") return "<div style='width:100%;height:2px;border-top:1px solid grey'></div>"
	if (type == "underline") return "<span style='text-decoration: underline'>${text}</span>"
	if (type == "description") return "<span style='color:#1A77C9'>${text}</span>"
	return text
}


@CompileStatic
static String getIcon(String iconClass, boolean small = false) {
	String icon = iconClass + (small ? " text-sm pr-1" : "")
	return """<i class="$icon"></i>"""
}
@CompileStatic
static def getImage(String type, Integer height = 40, Integer width = 15) {	// Modified from @Stephack Code / BPTWorld
	String loc = "<img src=https://raw.githubusercontent.com/jdc72/Hubitat/master/__shared_resources/images/"
	if (type == "Blank") return "${loc}Hubitat_blank.png height=${height} width=${width}>"

}

@CompileStatic
static String fpx(Integer size, String text = "\n", String color = (String)null) {
	String col = (color != null) ? "color:$color;" : ""
	return "<span style=\"${col}font-size:${size}px\">${text}</span>"
}
@CompileStatic
static String lpx(Integer textSize, String text = "\n") {
	BigDecimal lineSize = textSize * 1.1
	return "<div style=\"line-height:${lineSize}px\">${text}</div>"
}

@CompileStatic
static String bullet(Integer spaces) { return "&bull;" + String.format("%" + spaces + "s", " ") }
@CompileStatic
static GString gap() { return (GString)getImage("Blank", 30, 1) }
@CompileStatic
static String indent(String text, Integer indentSize = 0, String indentSide = "left") {
	return "<div style='text-align:${indentSide};margin-${indentSide}:${indentSize}px'>${text}</div>"
}

@CompileStatic
static String getTooltip(String id, String text, String tipText, String style = "bottom:140%; left:0%") {
	return getStyleTooltip(id, style) + "<span class='tooltip-${id}'>${text}<span class='tooltipText'>${tipText}</span></span>"
}
@CompileStatic
static String getStyleTooltip(String id, String style) {
	return "<style>.tooltip-${id} {display:inline-block; position:relative} .tooltip-${id} .tooltipText{display:none; position:absolute; width:max-content; $style; z-index:999; border-radius:6px; background-color:#FFFFFFFF; padding:15px 15px; box-shadow:0px 0px 10px 5px rgba(0,0,0,0.5);} .tooltip-${id}:hover .tooltipText{display:inline-block}</style>"
}


String titleApp() {
	return formatTitle(getAppLabel(), "Populate labels for devices linked from other hubs.")
}

def sectionFooter() {
	String bullet = bullet(1)
	String name = hrefLinkExternal(LINK_GITHUB, APP_NAME)
	String footer = "<div style='color:#1A77C9;text-align:center'>$name  $bullet v${APP_VERSION}  $bullet jdc72</div>"
	paragraph lpx(20)
	paragraph getFormat("line")
	paragraph footer
}


//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Buttons
//-------------------------------------------------------------------------

@CompileStatic
static String inputButtonLink(String buttonName, String linkText, String titleText, String classes, String style) {
	String cssClasses = (classes ?: "")
	String cssStyle = (style ?: "")
	String cssTitle = (titleText == null) ? "" : "title='$titleText'"
	return "<span class='form-group'><input type='hidden' name='${buttonName}.type' value='button'></span>" +
		"<span><span class='submitOnChange $cssClasses' onclick='buttonClick(this)' style='cursor:pointer;${cssStyle}' $cssTitle>$linkText</span></span><input type='hidden' name='settings[$buttonName]' value=''>"
}
@CompileStatic
static String inputButtonLink(String buttonName, String linkText, String titleText, String color = "#1A77C9", Integer szFont = 15) {
	String style = "color:$color;font-size:${szFont}px;"
	return inputButtonLink(buttonName, linkText, titleText, null, style)
}
@CompileStatic
static String inputButtonCheckbox(String buttonName, boolean isChecked, String color = "#1A77C9", Integer szFont = 15) {
	return inputButtonLink(buttonName, getIcon(isChecked ? ICON_CHECKED : ICON_UNCHECKED), null, color, szFont)
}


def inputButton(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "button",
		textColor: params?.textColor ?: "black",
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false
	)
}


def appButtonHandler(String buttonName) {
	Map<String,Map<String,String>> linkedDevices = (Map<String,Map<String,String>>)state.linkedDevices
	if (buttonName == "btnApplyLabels") {
		List<String> deviceIds = (List<String>)state.deviceIdsIncluded
		logT "appButtonHandler: $buttonName for device IDs = $deviceIds"
		DeviceWrapperList devices = getSettingsDeviceWrapperList("devices_all")
		deviceIds?.each { String deviceId ->
			String labelNew = linkedDevices?."$deviceId"?.labelNew
			if (labelNew == null) return
			devices.find { it.id == deviceId }?.setLabel(labelNew)
		}
	} else if (buttonName.startsWith("btnInclude|")) {
		List<String> btnTerms = buttonName.split("\\|")
		List<String> deviceIds = (btnTerms[1] == "ALL") ? (List<String>)state.deviceIdsIncluded : [btnTerms[1]]
		logT "appButtonHandler: $buttonName for device IDs = $deviceIds"
		deviceIds?.each { String deviceId ->
			if (linkedDevices[deviceId] == null) return
			if (btnTerms[2] == "exclude")
				linkedDevices[deviceId]["include"] = "N"
			else if (btnTerms[2] == "include")
				linkedDevices[deviceId]["include"] = "Y"
		}
		state.linkedDevices = linkedDevices
		state.remove("deviceIdsIncluded")
	}
}


//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Etc.
//-------------------------------------------------------------------------

def inputBoolean(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "bool",
		defaultValue: params?.defaultValue ?: false,
		width: params?.width ?: 12,
		required: params?.required ?: false,
		submitOnChange: true
	)
}

def inputCapability(String name, String title, String capability, Map params = null) {
	input(
		name: name,
		title: title,
		type: "capability.${capability}",
		multiple: true,
		offerAll: capability != CAPABILITY_ALL,
		showFilter: true,
		width: params?.width ?: 12,
		required: params?.required ?: false,
		submitOnChange: true
	)
}

def inputText(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "text",
		defaultValue: (String)params?.defaultValue,
		required: params?.required ?: false,
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false,
		submitOnChange: true
	)
}


//-------------------------------------------------------------------------
// Page:  ALL:  Navigation Helpers:  HREF
//-------------------------------------------------------------------------

@CompileStatic
static String hrefLinkExternal(String href, String linkText, boolean small = false) {
	return linkText + "  " + """<a href="$href" target="_blank">${getIcon(ICON_LINK_EXT, small)}</a>"""
}
@CompileStatic
static String hrefLinkDevicePage(String deviceId, String deviceName, boolean small = false) {
	return hrefLinkExternal("/device/edit/$deviceId", deviceName, small)
}


//=========================================================================
// Device:  Helpers
//=========================================================================

DeviceWrapperList getSettingsDeviceWrapperList(String key, String deviceId = null) {
	if (key == null) return null
	DeviceWrapperList devices = (DeviceWrapperList)settings."$key"
	if (deviceId == null) return devices
	DeviceWrapper device = devices?.find {
		DeviceWrapper dvc -> dvc.getId() == deviceId
	}
	return (DeviceWrapperList)[device]
}


//=========================================================================
// Application:  Management
//=========================================================================

void installed() {
	initialize()
	logI ":: installed with settings: ${settings}"
}


void updated() {
	initialize()
	logI ":: updated with settings: ${settings}"
}


void uninstalled() {
	unschedule()
	unsubscribe()
}


void initialize() {
	app.updateLabel(getAppLabel())
	state.remove("clonedName")
	state.remove("deviceIdsIncluded")
	refresh()
}


void refresh() {
	logD "[REFRESH] nothing to refresh"
}


//=========================================================================
// Application Status:  Processing
//=========================================================================

//=========================================================================
// Utilities
//=========================================================================

void log(String level, String msg) {
	log."$level"("Battery Tracker $msg")
}
void logD(String msg) {
	if (settings.loggingDebug == true) log "debug", msg
}
@CompileStatic
void logI(String msg) {
	log "info", msg
}
void logT(String msg) {
	if (settings.loggingTrace == true) log "trace", msg
}
@CompileStatic
void logW(String msg) {
	log "warn", msg
}


//=========================================================================
// Constants
//=========================================================================

@Field public static final String CAPABILITY_ALL = "*"
@Field public static final String COLOR_IGNORED = "grey"
@Field public static final String ICON_CHECKED = "he-checkbox-checked"
@Field public static final String ICON_LINK_EXT = "pi pi-external-link"
@Field public static final String ICON_UNCHECKED = "he-checkbox-unchecked"
