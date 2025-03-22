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
 *    version 1.0.0  @  2025-03-16  -  jdc72  -  Initial release
 *    version 1.1.0  @  2025-03-22  -  jdc72  -  Manual edits of device labels; optional device deselection at initialization
 *    version 1.1.1  @  2025-03-22  -  jdc72  -  Additional help for manual edits of device labels
 *
 */

import groovy.transform.CompileStatic
import groovy.transform.Field
import com.hubitat.app.*

@Field static final String APP_NAME = "Linked Device Labeler"
@Field static final String APP_VERSION = "1.1.1"
@Field static final String LINK_COMM = "https://community.hubitat.com/t/release-linked-device-labeler/151494"
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
	Map<String,Map<String,String>> linkedDevicesNow = (Map<String,Map<String,String>>)state.linkedDevices ?: [:]
	String deviceIdEdit = (String)state.deviceIdEdit
	boolean isMain = (deviceIdEdit == null || linkedDevicesNow[deviceIdEdit] == null)

	dynamicPage(name: "pageMain", title: "", install: isMain, uninstall: true) {
		section(titleApp()) {
			if (isMain)
				sectionMain(linkedDevicesNow)
			else
				sectionDeviceLabelEdit(linkedDevicesNow)
			sectionFooter()
		}
	}
}


//-------------------------------------------------------------------------
// Section:  Main
//-------------------------------------------------------------------------

def sectionMain(Map<String,Map<String,String>> linkedDevicesNow) {
	String labelSuffix = (String)settings.labelSuffix
	DeviceWrapperList allDevicesWrappers = getSettingsDeviceWrapperList("devices_all")
	Map<String,String> labelsManualNow = (Map<String,String>)state.labelsManual ?: [:]
	Map<String,String> labelsManualNew = [:]
	Map<String,DeviceWrapper> linkedDevicesWrappers = [:]
	Map<String,Map<String,String>> linkedDevicesExclude = [:]
	Map<String,Map<String,String>> linkedDevicesInclude = [:]
	Map<String,Map<String,String>> linkedDevicesSame = [:]
	Map<String,Map<String,String>> linkedDevicesAll = [:]
	allDevicesWrappers?.findAll { it.controllerType == "LNK" }?.each {
		Map<String,String> dvcInfo = [id: it.id, name: it.name, labelNow: it.label]
		String labelDerive = getLabelNew(dvcInfo, labelSuffix)
		String labelManual = (linkedDevicesNow[it.id]?.labelManual == "Y") ?
			linkedDevicesNow[it.id].labelNew : labelsManualNow[it.id]
		if (labelManual == dvcInfo["labelNow"]) labelManual = null
		dvcInfo["labelNew"] = labelManual ?: labelDerive
		dvcInfo["labelSame"] = (dvcInfo["labelNew"] == dvcInfo["labelNow"]) ? "Y" : "N"
		dvcInfo["labelManual"] = (dvcInfo["labelNew"] == labelDerive) ? "N" : "Y"
		dvcInfo["include"] = (dvcInfo["labelSame"] == "Y") ? "N" : linkedDevicesNow?."${it.id}"?."include" ?: "Y"
		if ((dvcInfo["labelNow"] == null || !((Boolean)settings.onlyLabelsNull ?: false)) &&
			(dvcInfo["labelSame"] == "N" || !((Boolean)settings.onlyLabelsChanging ?: false))) {
			if (dvcInfo["labelSame"] == "Y")
				linkedDevicesSame[it.id] = dvcInfo
			else if (dvcInfo["include"] == "N")
				linkedDevicesExclude[it.id] = dvcInfo
			else
				linkedDevicesInclude[it.id] = dvcInfo
		}
		if (dvcInfo["labelSame"] == "N" && dvcInfo["labelNew"] != labelDerive) {
			labelsManualNew[it.id] = dvcInfo["labelNew"]
		}
		linkedDevicesAll[it.id] = dvcInfo
		linkedDevicesWrappers[it.id] = it
	}
	Map<String,Map<String,String>> linkedDevices = linkedDevicesInclude + linkedDevicesExclude + linkedDevicesSame
	if (!linkedDevices.isEmpty())
		state.linkedDevices = linkedDevices
	else state.remove("linkedDevices")

	if (!labelsManualNew.isEmpty())
		state.labelsManual = labelsManualNew
	else state.remove("labelsManual")

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
	paragraph fpx(14, "Note: after linking new devices in Settings > Hub Mesh, you must select those devices here as well", "grey")
	paragraph formatHeader(1, "Labels")
	String desc = "For devices linked from other hubs\n" + lpx(8)
	desc += bullet(3) + "Create a new Device Label from the Device Name (imported from the remote hub)\n"
	desc += bullet(3) + "Strip \"on [hub]\" from the end\n"
	desc += bullet(3) + "Optionally append a string (e.g. icon, characters)\n"
	desc += bullet(3) + "Only devices whose Device Label values could change are selectable\n"
	paragraph desc
	if (linkedDevicesAll.size() > 0) {
		paragraph formatHeader(2, "Controls")
		inputBoolean("onlyLabelsNull", "Include only devices currently without labels.", [EOL: true])
		if ((Boolean)settings.onlyLabelsNull ?: false)
			app.updateSetting("onlyLabelsChanging", [type: "bool", value: true])
		else
			inputBoolean("onlyLabelsChanging", "Include only devices whose labels would change.", [EOL: true])
		inputText("labelSuffix", "Enter a string to append to each label (optional) and press \"Enter\"", [EOL: true])
		if (linkedDevicesInclude.size() + linkedDevicesExclude.size() > 0) {
			state.deviceIdsExcluded = (List<String>)linkedDevicesExclude.collect { it.key }
			state.deviceIdsIncluded = (List<String>)linkedDevicesInclude.collect { it.key }
			paragraph "Apply the proposed labels to all devices selected below."
			inputButton("btnApply", "Apply Device Labels", [EOL: true])
			paragraph formatHeader(2, "Values")
			sectionDeviceValues(linkedDevicesInclude, linkedDevicesExclude, linkedDevicesSame)
		} else {
			paragraph formatHeader(2, "Values")
			paragraph "No label suggestions for the selected devices and controls."
		}
	} else {
		paragraph fpx(16, "Please select devices above.", "firebrick")
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
			descriptions.add("Most recently linked device: " +
				(latestDevice["labelNow"] != null ? latestDevice["labelNow"] : latestDevice["name"]))
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
	String checkboxHover = fpx(14, "Click to <strong>" + (areAllIncluded ? "exclude" : "include") + "</strong> all eligible devices " + (areAllIncluded ? "from" : "in") + " label changes (i.e. whose values could change)", COLOR_TEXT)
	String checkboxValue = getTooltip("1", inputButtonCheckbox(checkboxButtonName, areAllIncluded, [color:"white"]), checkboxHover)
	sectionHeaderColumn(checkboxValue, [align: "center", width: 1])
	sectionHeaderColumn("Device ID", [align: "center", width: 1])
	sectionHeaderColumn("Device Label")
	sectionHeaderColumn("Device Label  //  Proposed")
	sectionHeaderColumn("Device Name", [width: 4])
}
def sectionHeaderColumn(String name, Map params = null) {
	String columnHdrFmt = indent(gap() + fpx(12, name), 10, (String)params?.align ?: "left")
	paragraph formatHeader(3, columnHdrFmt, [padding: false]), width: (Integer)params?.width ?: 3
}
def sectionDeviceRow(Map<String,String> device) {
	boolean isSame = (device["labelSame"] == "Y")
	boolean isIncluded = (device["include"] == "Y")
	String color = isIncluded ? null : COLOR_IGNORED
	String checkboxButtonName = "btnInclude|${device.id}|" + (isIncluded ? "exclude" : "include")
	String checkboxHover = fpx(14, "Click to <strong>" + (isIncluded ? "exclude" : "include") + "</strong> this device " + (isIncluded ? "from" : "in") + " label changes", COLOR_TEXT)
	String checkboxValue = (isSame) ? "" : getTooltip("1", inputButtonCheckbox(checkboxButtonName, isIncluded), checkboxHover)
	sectionDeviceColumn(checkboxValue, [color: color, align: "center", width: 1])
	String deviceId = hrefLinkDevicePage(device["id"], device["id"], [small: true, color: color])
	sectionDeviceColumn(deviceId, [color: color, align: "center", width: 1])
	sectionDeviceColumn(device["labelNow"], [color: color])
	String labelNewHover = fpx(14, "Click to manually edit the proposed Device Label", COLOR_TEXT)
	String labelNewText = device["labelNew"] + "  " + getIcon(ICON_PENCIL, [small: true])
	String labelNew = getTooltip("2", inputButtonLink("btnEdit|" + device["id"], labelNewText, [color: color]), labelNewHover)
	sectionDeviceColumn(labelNew, [color: color, bold: isSame ? null : "N"])
	sectionDeviceColumn(device["name"], [color: color, width: 4])
}
def sectionDeviceColumn(String value, Map params = null) {
	String valueFmt1 = value ?: ""
	String valueFmt2 = (params?.bold != null) ? "<strong>$valueFmt1</strong>" : valueFmt1
	String valueFmt3 = (params?.color != null) ? fpx(14, valueFmt2, (String)params.color) : fpx(14, valueFmt2)
	paragraph indent(valueFmt3, 10, (String)params?.align ?: "left"), width: (Integer)params?.width ?: 3
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
// Section:  Label Edit
//-------------------------------------------------------------------------

def sectionDeviceLabelEdit(Map<String,Map<String,String>> linkedDevicesNow) {
	String deviceId = (String)state.deviceIdEdit
	Map<String,String> device = linkedDevicesNow[deviceId]
	String labelNew = device["labelNew"]
	String labelEdit = (String)settings.labelEdit ?: labelNew
	boolean isSpaceStart = (labelEdit =~ /^\s/)
	boolean isSpaceEnd = (labelEdit =~ /\s$/)
	boolean isSame = (labelEdit == labelNew)

	paragraph formatHeader(1, "Device Label")
	paragraph formatHeader(3, device["name"])
	String desc = "Manually edit the proposed Device Label\n" + lpx(8) +
		bullet(3) + "Override the currently proposed Device Label derived from the Device Name\n" +
		bullet(3) + "Return to the main page to apply the proposed Device Label to the device\n"
	paragraph desc
	paragraph formatHeader(2, "Value")
	inputText("labelEdit", "Enter the desired value for the proposed Device Label", [defaultValue: labelNew, EOL: true])
	String msg = fpx(14, "Please press \"Enter\" after changing the value above\n", "grey")
	if (isSpaceStart) msg += fpx(14, "Potential typo:  the new value starts with a space\n", "firebrick")
	if (isSpaceEnd) msg += fpx(14, "Potential typo:  the new value ends with a space\n", "firebrick")
	paragraph msg

	paragraph formatHeader(2, "Action")
	if (!isSame) sectionHeaderColumn("Version", [align: "center", width: 1])
	sectionHeaderColumn("Device ID", [align: "center", width: 1])
	sectionHeaderColumn("Device Label  //  Current")
	sectionHeaderColumn("Device Label  //  Proposed")
	sectionHeaderColumn("Device Name", [width: isSame ? 5 : 4])
	sectionDeviceInfo(device, isSame ? null : "Before", device["labelNew"])
	if (!isSame) sectionDeviceInfo(device, "After", labelEdit)
	paragraph getFormat("line-grey")
	inputButton("btnReturn", isSame ? "Return" : "Cancel", [width: 1])
	paragraph lpx(8) + (isSame ? "Return to the main page" :
		"Keep the originally proposed Device Label (\"Before\") and return to the main page"), width: 11
	if (!isSame) {
		inputButton("btnUpdate", "Update", [width: 1])
		paragraph lpx(8) + "Update the proposed label (\"After\") and return to the main page", width: 11
	}
}
def sectionDeviceInfo(Map<String,String> device, String version, String labelNew) {
	if (version != null) sectionDeviceColumn(version, [align: "center", width: 1])
	sectionDeviceColumn(hrefLinkDevicePage(device["id"], device["id"], [small: true]), [align: "center", width: 1])
	sectionDeviceColumn(device["labelNow"])
	sectionDeviceColumn(labelNew)
	sectionDeviceColumn(device["name"], [width: (version == null) ? 5 : 4])
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
			paragraph formatHeader(2, "Initialization")
			inputBoolean("initializeDeselectDevices", "Deselect devices during app initialization (i.e. \"Done\")")
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
			paragraph formatHeader(2, "Devices")
			String desc = DESC_DEVICES + "\n" + lpx(8)
			desc += bullet(3) + "This app operates only on devices linked from other hubs to this hub, so ...\n"
			desc += bullet(3) + "Select liberally, select all devices (i.e. toggle all on)\n"
			desc += bullet(3) + "Later steps will filter out local devices and offer per-device checkbox selections for finer control\n"
			paragraph desc
			inputCapability("devices_all", DESC_DEVICES, CAPABILITY_ALL)
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
	return "<div style='display:inline-block; width:100%; border-radius:0; color:$COLOR_BLUE; background-color:#FFFFFFFF; padding:15px 15px; margin:5px 0 0 0; box-shadow:0px 0px 5px 1px $COLOR_BLUE;'>${fpx(22, text1)}${text2 == null ? "" : fpx(16, "\n$text2", "#49535c")}</div>"
}
@CompileStatic
static String formatHeader(Integer headerNum, String text, Map params = null) {
	String color1 = getHeaderColor(headerNum,1)
	String color2 = getHeaderColor(headerNum,2)
	String background = " background:linear-gradient(0deg, $color1 0%, $color2 40%, $color2 60%, $color1 100%);"
	String boxShadow = (headerNum in [1,2]) ? "box-shadow:0px 0px 3px 1px rgba(0,0,0,0.3);" : ""
	String margin = (headerNum in [1,2]) ? "margin:5px 0 0 0;" : ""
	boolean isPadding = (params?.padding == null ? true : (Boolean)params.padding)
	String padding = (isPadding) ? "padding:${HEADER_PADDING[headerNum]};" : ""
	String txt = (headerNum == 1) ? fpx(18, text) : text
	return "<div style='color:#FFFFFFFF; $background $padding $margin $boxShadow'>${txt}</div>"
}
@CompileStatic
static String getHeaderColor(Integer headerNum, Integer colorNum) {
	return [
		1: (Map<Integer,String>)[1:"#20599E", 2:COLOR_BLUE],
		2: (Map<Integer,String>)[1:"#197A41", 2:"#32994D"],
		3: (Map<Integer,String>)[1:"#616161", 2:"grey"],
		4: (Map<Integer,String>)[1:"#878787", 2:"#99A3A4"],
	][headerNum][colorNum]
}
@Field public static final Map<Integer,String> HEADER_PADDING = [1:"7px 15px", 2:"7px 15px", 3:"4px 15px", 4:"4px 15px"]

@CompileStatic
static String getFormat(String type, String text = "", Map params = null) { // Modified from @Stephack Code / @BPTWorld
	if (type == "bold-red") return "<div style='color:firebrick; font-weight:bold'>${text}</div>"
	if (type == "font") {
		String font = (String)params?.font ?: "sans-serif"
		return "<span style='font-family:${font}'>${text}</span>"
	}
	if (type == "highlight") {
		String color = (String)params?.color ?: COLOR_TEXT
		return "<div style='color:#ffffff;background-color:${color}'>${text}</div>"
	}
	if (type == "line") return "<div style='width:100%; height:1px; border-bottom:1px solid $COLOR_BLUE;'></div>"
	if (type == "line2") return "<hr style='background-color:$COLOR_BLUE; height: 1px; border: 0;'></hr>"
	if (type == "line-grey") return "<div style='width:100%;height:2px;border-top:1px solid grey'></div>"
	if (type == "underline") return "<span style='text-decoration: underline'>${text}</span>"
	if (type == "description") return "<span style='color:$COLOR_BLUE'>${text}</span>"
	return text
}


@CompileStatic
static String getIcon(String iconClass, Map params = null) {
	String icon = iconClass + (((Boolean)params?.small ?: false) ? " text-sm pr-1" : "")
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
	String footer = "<div style='color:$COLOR_BLUE;text-align:center'>$name  $bullet v${APP_VERSION}  $bullet jdc72</div>"
	paragraph lpx(20)
	paragraph getFormat("line")
	paragraph footer
}


//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Buttons
//-------------------------------------------------------------------------

@CompileStatic
static String inputButtonLink(String buttonName, String linkText, Map params = null) {
	String style = ""
	if ((String)params?.color != null) style += "color:${params.color};"
	if ((String)params?.fontSz != null) style += "font-size:${params.fontSz};"
	String classes = ((String)params?.classes ?: "")
	String title = ((String)params?.titleText == null) ? "" : "title='${params?.titleText}'"
	return "<span class='form-group'><input type='hidden' name='${buttonName}.type' value='button'></span>" +
		"<span><span class='submitOnChange $classes' onclick='buttonClick(this)' style='cursor:pointer;${style}' $title>$linkText</span></span><input type='hidden' name='settings[$buttonName]' value=''>"
}
@CompileStatic
static String inputButtonCheckbox(String buttonName, boolean isChecked, Map params = null) {
	Map parameters = params ?: [:]
	parameters.color = (String)parameters?.color ?: COLOR_BLUE
	parameters.fontSz = (String)parameters?.fontSz ?: "15"
	return inputButtonLink(buttonName, getIcon(isChecked ? ICON_CHECKED : ICON_UNCHECKED), parameters)
}


def inputButton(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "button",
		textColor: params?.textColor ?: COLOR_TEXT,
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false
	)
}


def appButtonHandler(String buttonName) {
	Map<String,Map<String,String>> linkedDevices = (Map<String,Map<String,String>>)state.linkedDevices ?: [:]
	List<String> btnName = buttonName.split("\\|")
	switch (btnName[0]) {
		case "btnInclude":
			String action = btnName[2]
			List<String> deviceIds = (btnName[1] != "ALL") ? [btnName[1]] :
				(action == "exclude") ? (List<String>)state.deviceIdsIncluded : (List<String>)state.deviceIdsExcluded
			logT "appButtonHandler: $buttonName for device IDs = $deviceIds"
			deviceIds?.each { String deviceId ->
				if (linkedDevices[deviceId] == null) return
				if (action == "exclude")
					linkedDevices[deviceId]["include"] = "N"
				else if (action == "include")
					linkedDevices[deviceId]["include"] = "Y"
			}
			state.linkedDevices = linkedDevices
			break
		case "btnApply":
			List<String> deviceIds = (List<String>)state.deviceIdsIncluded
			logT "appButtonHandler: $buttonName for device IDs = $deviceIds"
			DeviceWrapperList devices = getSettingsDeviceWrapperList("devices_all")
			deviceIds?.each { String deviceId ->
				String labelNew = linkedDevices?."$deviceId"?.labelNew
				if (labelNew == null) return
				devices.find { it.id == deviceId }?.setLabel(labelNew)
			}
			break
		case "btnEdit":
			logT "appButtonHandler: $buttonName"
			state.deviceIdEdit = btnName[1]
			break
		case "btnUpdate":
			String deviceId = (String)state.deviceIdEdit
			linkedDevices[deviceId].include = "Y"
			linkedDevices[deviceId].labelManual = "Y"
			linkedDevices[deviceId].labelNew = (String)settings.labelEdit
			if (state.labelsManual == null) state.labelsManual = [:]
			((Map<String,String>)state.labelsManual)[deviceId] = (String)settings.labelEdit
			// fall through into btnReturn
		case "btnReturn":
			logT "appButtonHandler: $buttonName"
			state.remove("deviceIdEdit")
			app.removeSetting("labelEdit")
			break
	}
	state.remove("deviceIdsExcluded")
	state.remove("deviceIdsIncluded")
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
static String hrefLinkExternal(String href, String linkText, Map params = null) {
	String color = (String)params?.color
	String style = (color != null) ? "style='color:$color !important;'" : ""
	return """<a href="$href" target="_blank" $style>$linkText  ${getIcon(ICON_LINK_EXT, params)}</a>"""
}
@CompileStatic
static String hrefLinkDevicePage(String deviceId, String deviceName, Map params = null) {
	return hrefLinkExternal("/device/edit/$deviceId", deviceName, params)
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
	state.remove("deviceIdEdit")
	state.remove("deviceIdsExcluded")
	state.remove("deviceIdsIncluded")
	state.remove("linkedDevices")
	state.remove("labelsManual")
	app.removeSetting("labelEdit")
	if ((Boolean)settings.initializeDeselectDevices ?: false)
		app.removeSetting("devices_all")
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
@Field public static final String COLOR_BLUE = "#1A77C9"
@Field public static final String COLOR_IGNORED = "#B0BABB"
@Field public static final String COLOR_TEXT = "#49535C"
@Field public static final String ICON_CHECKED = "he-checkbox-checked"
@Field public static final String ICON_LINK_EXT = "pi pi-external-link"
@Field public static final String ICON_PENCIL = "pi pi-pencil"
@Field public static final String ICON_UNCHECKED = "he-checkbox-unchecked"
