/**************************************************/
/** For development only. Do not copy to Hubitat. */

import com.hubitat.hub.executor.AppExecutor
import groovy.transform.BaseScript

@BaseScript AppExecutor appExecutor
/**************************************************/


/**
 *  Battery Tracker
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
 *    1.0.0  @  2025-03-01  -  Initial release
 *
 */

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.Field
import java.text.SimpleDateFormat
import com.hubitat.app.*

@Field static final String APP_NAME = "Battery Tracker Test"
@Field static final String APP_VERSION = "1.0.0"
@Field static final String COMM_LINK = "https://community.hubitat.com/t/***/***"


definition(
	name: APP_NAME,
	namespace: "jdc72",
	author: "Jeffrey D. Chapman",
	description: "App to detect, track, and manage battery life",
	category: "Convenience",
	singleInstance: true,
	singleThreaded: true,
	installOnOpen: true,
	iconUrl: "",
	iconX2Url: "",
	iconX3Url: "")


preferences {
	page(name: "pageMain")
	page(name: "pageMain_BatteryDeviceData")
	page(name: "pageMain_BatteryDeviceTrackers")
	page(name: "pageMain_BatteryDeviceReport")
	page(name: "pageMain_AdditionalSettings")
	page(name: "pageBatteryDataManual")
	page(name: "pageBatteryDataDetected")
	page(name: "pageBatteryDataNames")
	page(name: "pageBatteryDataMigrate")
	page(name: "pageBatteryDataMigrateDevices")
	page(name: "pageBatteryDeviceTrackers")
	page(name: "pageBatteryDeviceTrackers_LastActivityTrackers")
	page(name: "pageBatteryDeviceTrackers_LowBatteryTrackers")
	page(name: "pageBatteryDeviceTrackers_NewBatteryTrackers")
	page(name: "pageBatteryDeviceTrackers_OldBatteryTrackers")
	page(name: "pageBatteryDeviceTracker")
	page(name: "pageBatteryDeviceTracker_Alerts")
	page(name: "pageBatteryDeviceTracker_Devices")
	page(name: "pageBatteryDeviceTracker_Run")
	page(name: "pageBatteryDeviceTracker_CopyDelete")
	page(name: "pageBatteryDeviceTrackerData")
	page(name: "pageGlobalDevices")
	page(name: "pageGlobalDevices_TrackedDevices")
	page(name: "pageGlobalDevices_PausedDevices")
	page(name: "pageGlobalDevices_NotificationDevices")
	page(name: "pageGlobalSchedule")
	page(name: "pagePauseDevices")
	page(name: "pagePauseDeviceUntil")
	page(name: "pageManualChecks")
	page(name: "pageRequestConfirm")
}


//=========================================================================
// Pages
//=========================================================================

//-------------------------------------------------------------------------
// Page:  Main
//-------------------------------------------------------------------------

def pageMain(Map params) {
	dynamicPage(name: "pageMain", title: "", install: true, uninstall: true) {
		Map<String,Object> parameters = getPageParameterInputs("pageMain", params)
		setPageParameters("pageMain", parameters)
		String pageTab = parameters?.pageTab ?: getPageTab("pageMain")
		if (pageTab == null || !PAGE_MAIN_TABS.collect { it.name }.contains(pageTab))
			pageTab = "BatteryDeviceData"
		"pageMain_${pageTab}"()
	}
}

String titleMainMenu() {
	return formatTitle(getBatteryTrackerLabel(), "Manage device data and notifications related to battery life.")
}
def sectionMainMenu() {
	state.remove("configure")
	deletePageHistory()
	deletePageLink()
	deleteCurrentBatteryDataNamesIfDefault()
	deleteMigrateBatteryData()
	paragraph "<div class='menu' style='margin:0'>" + getPageMenuList("pageMain", PAGE_MAIN_TABS, true) + "</div>"
}
@Field public static final List<Map<String,String>> PAGE_MAIN_TABS = [
	[name: "BatteryDeviceData",  display: "Battery Device Data"],
	[name: "BatteryDeviceTrackers", display: "Battery Device Trackers"],
	[name: "BatteryDeviceReport", display: "Battery Device Report"],
	[name: "AdditionalSettings", display: "Additional Settings"]
]


def pageMain_BatteryDeviceData(Map params) {
	setPageTab("pageMain", "BatteryDeviceData")
	dynamicPage(name: "pageMain_BatteryDeviceData", title: "", install: true, uninstall: true) {
		Integer width = 7
		section(titleMainMenu()) {
			sectionMainMenu()
			paragraph formatHeader(2, "Battery Data")
			String msg = DESC_BATTERY_DATA + "\n" + lpx(8)
			msg += bullet(3) + "Battery type " + fpx(14, "(e.g. AA x 2)") + "\n"
			msg += bullet(3) + "Change date for the current battery " + fpx(14, "(e.g. 2024/09/14)") + "\n"
			msg += bullet(3) + "Lifespans of previous batteries " + fpx(14, "(in days; e.g. 365,479,231)") + "\n"
			paragraph msg
			paragraph formatHeader(3, "Data Values"), width: width
			href(
				name: "pageBatteryDataManualManage",
				page: "pageBatteryDataManual",
				title: "Manual Edits",
				description: DESC_BATTERY_DATA_MANUAL,
				width: width
			)
			href(
				name: "pageBatteryDataObservedManage",
				page: "pageBatteryDataDetected",
				title: "Observations",
				description: DESC_BATTERY_DATA_OBSERVED,
				width: width,
				params: [dataStore: STORE_OBSERVED]
			)
			href(
				name: "pageBatteryDataRecordedManage",
				page: "pageBatteryDataDetected",
				title: "Automatic Updates",
				description: DESC_BATTERY_DATA_RECORDED,
				width: width,
				params: [dataStore: STORE_RECORDED]
			)
			href(
				name: "pageBatteryDataMigrateManage",
				page: "pageBatteryDataMigrate",
				title: "Migration",
				description: DESC_BATTERY_DATA_MIGRATE,
				width: width
			)
			paragraph formatHeader(3, "Data Names"), width: width
			href(
				name: "pageBatteryDataNamesManage",
				page: "pageBatteryDataNames",
				title: "Names",
				description: DESC_BATTERY_DATA_NAMES,
				width: width
			)
			sectionFooter()
		}
	}
}
def sectionBatteryDeviceData() {
	sectionTopMenu()
	paragraph formatTitle("Battery Device Data")
}

@Field public static final String DESC_BATTERY_DATA = "Manage battery information for devices in the Data section of Device Details"
@Field public static final String DESC_BATTERY_DATA_MANUAL = "Select devices and manually edit their battery information"
@Field public static final String DESC_BATTERY_DATA_MIGRATE = "Migrate battery information from other Device Data fields/names"
@Field public static final String DESC_BATTERY_DATA_NAMES = "Manage the Device Data names used to store battery information"
@Field public static final String DESC_BATTERY_DATA_OBSERVED = "Review and submit (or reject) battery updates detected by New Battery trackers"
@Field public static final String DESC_BATTERY_DATA_RECORDED = "Acknowledge or revert battery updates automatically applied by New Battery trackers"

def pageMain_BatteryDeviceTrackers() {
	setPageTab("pageMain", "BatteryDeviceTrackers")
	dynamicPage(name: "pageMain_BatteryDeviceTrackers", title: "", install: true, uninstall: true) {
		Integer width = 7
		section(titleMainMenu()) {
			sectionMainMenu()
			paragraph formatHeader(2, "Battery Trackers")
			String msgT = "Create and manage battery tracking on devices to detect, notify, and record.\n"
			msgT += lpx(8)
			msgT += bullet(3) + "Dead battery (i.e. last activity)\n"
			msgT += bullet(3) + "Low battery\n"
			msgT += bullet(3) + "New battery\n"
			msgT += bullet(3) + "Old battery (with respect to life expectancy based on previous batteries)\n"
			paragraph msgT
			href(
				name: "pageBatteryDeviceTrackersManage",
				page: "pageBatteryDeviceTrackers",
				title: "Battery Trackers",
				description: getDescription_BatteryDeviceTrackers()
			)
			paragraph ""
			paragraph formatHeader(2, "Global Devices")
			String msgD = "Select global devices that all battery trackers can utilize.\n"
			msgD += lpx(8)
			msgD += bullet(3) + "Battery devices to track\n"
			msgD += bullet(3) + "Battery devices on which to pause tracking\n"
			msgD += bullet(3) + "Notification devices\n"
			paragraph msgD
			href(
				name: "pageGlobalDevicesManage",
				page: "pageGlobalDevices",
				title: "Global Devices",
				description: getDescription_GlobalDevices(),
				width: width
			)
			paragraph ""
			paragraph formatHeader(2, "Global Schedule")
			paragraph "Set a global schedule that all battery trackers can utilize."
			href(
				name: "pageGlobalScheduleManage",
				page: "pageGlobalSchedule",
				title: "Global Schedule",
				description: getDescription_GlobalSchedule(),
				width: width
			)
			sectionFooter()
		}
	}
}
@CompileStatic
String getDescription_BatteryDeviceTrackers() {
	List<String> descObjects = getObjectsAndDescriptions().collect { it.fullS }
	String desc = getFormat("description", descObjects?.collect { bullet(3) + it }?.join("\n"))
	return (desc.isEmpty() ? "" : desc + lpx(6)) + "Configure battery tracking on devices\n"
}
@CompileStatic
String getDescription_GlobalDevices() {
	String desc = getObjectDescriptionDevicesBase()
	return ((desc == null) ? "" : getFormat("description", desc) + lpx(6)) + "Select global devices\n"
}
@CompileStatic
String getDescription_GlobalSchedule() {
	String desc = isObjectScheduleSetup(OBJECT_ID_APP) ?
		getFormat("description", getObjectDescriptionTime(OBJECT_ID_APP).fullL) : ""
	return ((desc.isEmpty()) ? "Set a " : desc + lpx(6) + "Manage the ") + "global schedule\n"
}

def pageMain_AdditionalSettings() {
	setPageTab("pageMain", "AdditionalSettings")
	dynamicPage(name: "pageMain_AdditionalSettings", title: "", install: true, uninstall: true) {
		section(titleMainMenu()) {
			sectionMainMenu()
			paragraph formatHeader(2, "Application Label")
			inputText("batteryTrackerLabel", "Application label for the Battery Tracker",
				[defaultValue:"Battery Tracker", required:true])
			paragraph formatHeader(2, "Logging")
			inputBoolean("enableLogging", "Enable debug logging")
			paragraph ""
			sectionFooter()
		}
	}
}
String getBatteryTrackerLabel() {
	String label = (String)settings.batteryTrackerLabel
	return (label != null && !label.isEmpty()) ? label : APP_NAME
}


//-------------------------------------------------------------------------
// Page:  Battery Data Manual
// Page:  Battery Data Detected
//-------------------------------------------------------------------------

def pageBatteryDataManual(Map params) {
	String dataStore = STORE_MANUAL

	dynamicPage(name: "pageBatteryDataManual", nextPage: "pageMain") {
		section {
			addToPageHistory("pageBatteryDataManual", "N/A")
			sectionBatteryDeviceData()
			paragraph formatHeader(1, "Manual Control")
			paragraph "Record battery information for selected devices in the Data section of Device Details.", width: 10
			inputBooleanBatteryDataHelp(dataStore, 2)
			paragraph formatHeader(2, "Battery Devices")
			inputCapability("data_devices_battery", TITLE_DEVICES_EDIT, CAPABILITY_BATTERY)
			if (settings.data_devices_battery != null)
				input(name: "data_devices_battery_none", title: TITLE_DEVICES_UNSELECT, type: "button")
			//paragraph ""

			HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataNow = getDataStore(dataStore)
			HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataNew = [:]
			DeviceWrapperList devices = getSettingsDeviceWrapperList("data_devices_battery")
			devices?.each { DeviceWrapper device ->
				dataNew[device.id] = [
					device: getDeviceIdentity(device,"data_devices_battery"),
					(STATE_CURRENT): getDeviceData(device)
				]
				HashMap<String,HashMap<String,String>> dataPreviousNow = dataNow?."${device.id}"?."$STATE_PREVIOUS"
				HashMap<String,HashMap<String,String>> dataPendingNow = dataNow?."${device.id}"?."$STATE_PENDING"
				HashMap<String,HashMap<String,String>> dataIgnoredNow = dataNow?."${device.id}"?."$STATE_IGNORED"
				if (dataPreviousNow != null) dataNew[device.id][STATE_PREVIOUS] = dataPreviousNow
				if (dataPendingNow != null) dataNew[device.id][STATE_PENDING] = dataPendingNow
				if (dataIgnoredNow != null) dataNew[device.id][STATE_IGNORED] = dataIgnoredNow
			}
			setDataStore(dataStore, dataNew)
            sectionDeviceDataManagement(dataStore, dataNew,
				["Inputs", STATE_PREVIOUS, STATE_CURRENT, STATE_PENDING])
			sectionFooter()
		}
	}
}


def pageBatteryDataDetected(Map params) {
	if (getStateConfigureValueString("dataStore") == null && params != null) {
		String dataStore = (String)((Map)params)?.dataStore
		state.configure = [:]
		setStateConfigureValue("dataStore", dataStore)
	}
	resetStateConfigTo(["dataStore"])
	String dataStore = getStateConfigureValueString("dataStore")
	String header = (dataStore == STORE_OBSERVED) ? "Observed" : "Recorded"
	String description = (dataStore == STORE_OBSERVED) ? DESC_BATTERY_DATA_OBSERVED : DESC_BATTERY_DATA_RECORDED

	dynamicPage(name: "pageBatteryDataDetected", nextPage: "pageMain") {
		section {
			addToPageHistory("pageBatteryDataDetected", "N/A")
			sectionBatteryDeviceData()
			paragraph formatHeader(1, "$header Battery Changes")
			paragraph description, width: 10
			HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
			if (data == null || data.isEmpty())
				paragraph "New batteries have not been detected recently.  Click the \"${getFormat("font", fpx(18, "<strong>Run Checks</strong>"), "calibri")}\" button to check again."
			else {
				inputBooleanBatteryDataHelp(dataStore, 2)
			}
			sectionDeviceDataManagement(dataStore, data,
				["Inputs", STATE_PREVIOUS, STATE_CURRENT, STATE_PENDING, "Cache", "Checks"])
			sectionFooter()
		}
	}
}


def sectionDeviceDataManagement(String dataStore,
    HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data, List<String> dataControls) {
	HashMap<String,Integer> buttonCounts = [:]
	List<String> controls = dataControls
	if (data == null || data.isEmpty()) {
		controls -= ["Inputs", STATE_PREVIOUS, STATE_PREVIOUS_MIGRATE, STATE_CURRENT, STATE_CURRENT_MIGRATE, STATE_PENDING]
		if (state."$dataStore" == null) controls -= ["Cache"]
	} else {
		List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataIgnored =
			getDeviceDataState(data, [STATE_IGNORED])
		List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataPending =
			getDeviceDataState(data, null, [STATE_IGNORED])
		buttonCounts = getDevicesButtonCounts(dataPending, dataIgnored)
		sectionDeviceDataValues(dataStore, dataPending, dataIgnored, buttonCounts)
		sectionDeviceDataWarnings(dataStore, dataPending + dataIgnored)
	}
	sectionDeviceDataControl(dataStore, controls, buttonCounts)
}
def sectionDeviceDataValues(String dataStore,
		List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataPending,
		List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataIgnored, HashMap<String,Integer> buttonCounts) {
	paragraph formatHeader(2, "Battery Values")
	if (isHelpEnabledForBatteryData(dataStore)) {
		List<HashMap<String,String>> states =
			[[(getFormat("underline", "State")): getFormat("underline", "Definition")],
			 [(STATE_CURRENT): "Current values in Device Data"],
			 [(STATE_PENDING): "Changed values to update in Device Data"],
			 [(STATE_PREVIOUS): "Previous values in Device Data before the Current values overwrote them"]]
		if (dataStore == STORE_MIGRATE) {
			states.add([(STATE_CURRENT_MIGRATE): "Current values in Device Data to convert/migrate"])
			states.add([(STATE_PREVIOUS_MIGRATE): "Deleted values from Device Data after converting/migrating"])
		}
		List<HashMap<String,String>> buttons =
			[[(getFormat("underline", "Button")): getFormat("underline", "Action")]]
		if (buttonCounts[STATE_PENDING] > 0)
			buttons.add(["Submit": "Record pending values into Device Data"])
		if (buttonCounts[STATE_PREVIOUS] > 0)
			buttons.add(["Restore": "Restore previous values as pending changes"])
		if (dataPending != null && !dataPending.isEmpty())
			buttons.add([(getIcon(ICON_CHECKED)): "Device is included in button actions (click to exclude)"])
		if (dataIgnored != null && !dataIgnored.isEmpty())
			buttons.add([(getIcon(ICON_UNCHECKED)): "Device is excluded from further button actions (click to include)"])
		paragraphLegend(states.collect { it.keySet()[0] }, 1)
		paragraphLegend(states.collect { it.values()[0] }, 5)
		paragraphLegend(buttons.collect { it.keySet()[0] }, 1)
		paragraphLegend(buttons.collect { it.values()[0] }, 5)
	}
	getDeviceDataColumns(dataStore).each { HashMap<String,String> columnDef ->
		String columnName = columnDef.colName
		if (columnDef.type == DATA_INCLUDE) {
			boolean allPending = (dataPending != null && !dataPending.isEmpty() &&
				(dataIgnored == null || dataIgnored.isEmpty()))
			String buttonName = nameButtonDeviceData(dataStore, "All", "All",
				allPending ? "Exclude All Devices" : "Include All Devices", null)
			columnName = inputButtonCheckbox(buttonName, allPending, "white")
		}
		String columnHdr = indent(gap() + fpx(12, columnName), columnDef.margin?.toInteger() ?: 10, columnDef.alignHdr)
		paragraph formatHeader(3, columnHdr, false), width: columnDef.widthHdr
	}
	sectionDeviceDataValues(dataStore, dataPending, buttonCounts)
	if (dataIgnored != null && !dataIgnored.isEmpty()) {
		if (!dataPending.isEmpty()) paragraph getFormat("line-grey")
		sectionDeviceDataValues(dataStore, dataIgnored)
	}
}
def sectionDeviceDataValues(String dataStore, List<HashMap<String,HashMap<String,HashMap<String,String>>>> data,
	HashMap<String,Integer> buttonCounts = null) {
	data.each { HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice ->
		String deviceId = dataDevice.device.id.value
		boolean isIgnored = dataDevice.containsKey(STATE_IGNORED)
		int numRows = (int)getStatesAll().count { String state -> dataDevice[state] != null }
		String spacing = ((numRows - 1) % 2 == 1 ? lpx(12) : "") + "\n" * ((int)(numRows - 1) / 2)
		getDeviceDataColumns(dataStore).each { HashMap<String,String> columnDef ->
			ArrayList<String> dataColumn = []
			if (columnDef.type == DATA_INCLUDE) {
				String buttonName = nameButtonDeviceData(dataStore, "All", "All",
					isIgnored ? "Include" : "Exclude", deviceId)
				String selectBox = spacing + inputButtonCheckbox(buttonName, !isIgnored)
				if (isIgnored) paragraphColumn(fpx(14, selectBox, COLOR_IGNORED), columnDef)
				else paragraphColumn(fpx(14, selectBox), columnDef)
				return
			} else if (columnDef.type == DATA_NAME) {
				String name = spacing + hrefLinkDevicePage(deviceId, dataDevice.device.name.value)
				String color = (isIgnored) ? COLOR_IGNORED : "black"
				paragraphColumn(fpx(14, name, color), columnDef)
				return
			}
			String dataKey = (columnDef.data ?: "value")
			getStatesAll().each { String state ->
				if (dataDevice[state] == null) return
				if (columnDef.type == DATA_STATE) {
					String color = (isIgnored) ? COLOR_IGNORED : getTextColorByState(state)
					dataColumn.add(fpx(12, state.capitalize(), color))
					return
				}
				String valueCurrent = dataDevice."$STATE_CURRENT"?."${columnDef.type}"?."$dataKey" ?: NO_VALUE
				String value = dataDevice."$state"?."${columnDef.type}"?."$dataKey" ?: NO_VALUE
				boolean isError = dataDevice[state][columnDef.type]?.containsKey("error")
				boolean isDeleted = (state == STATE_PREVIOUS_MIGRATE)
				if (state == STATE_PENDING && !dataDevice[state].containsKey(columnDef.type))
					value = valueCurrent
				if (value == null || value.isEmpty())
					value = NO_VALUE
				if (value != valueCurrent || isError)
					value = "<strong>$value</strong>"
				if (isError)
					value = "$value *"
				String color = (isIgnored) ? COLOR_IGNORED :
					(isDeleted) ? COLOR_PREVIOUS_MIGRATE :
					(isError) ? COLOR_ERROR :
					valueCurrent == value ? "black" : getTextColorByState(state)
				dataColumn.add(fpx(12, value, color))
			}
			paragraphColumn(dataColumn.join("\n"), columnDef)
		}
		ArrayList<String> buttons = []
		if (isIgnored)
			buttons = ["space", "space"]
		else {
			int buttonsMax = buttonCounts["numDeviceButtonsMax"]
			buttons.add((buttonsMax == 2 && dataDevice[STATE_PENDING] != null) ? "Submit" : "space")
			buttons.add((buttonsMax == 2 && dataDevice[STATE_PREVIOUS] != null &&
				!isDeviceDataStateEqualTo(dataDevice, STATE_PREVIOUS, STATE_PENDING)) ? "Restore" :
				(buttonsMax == 1 && dataDevice[STATE_PENDING] != null ? "Submit" :
				(buttonsMax == 1 && dataDevice[STATE_PREVIOUS] != null ? "Restore" : "space")))
		}
		buttons.each { String button ->
			if (button == "space") paragraph "", width: 1
			else inputButtonDeviceData(dataStore, button,
				[deviceId:deviceId, width:1, textColor:getTextColorByButton(button)])
		}
	}
}
def sectionDeviceDataWarnings(String dataStore, List<HashMap<String,HashMap<String,HashMap<String,String>>>> data) {
	Set<String> errors = []
	data?.each { HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice ->
		[STATE_CURRENT, STATE_PENDING].each { String state ->
			if (dataStore == STORE_MIGRATE && state == STATE_CURRENT) return
			dataDevice[state]?.findAll {
				it.value?.error != null
			}?.each {
				errors.add(it.key)
				logW ":: ${dataDevice.device.name} :: $state :: ${it.value?.error}"
			}
		}
	}
	ArrayList<String> messages = ["Errors detected in device data. Please check the logs for specifics."]
	if (errors.contains(DATA_DATE)) {
		ArrayList<String> msg = ["Battery Date formatting errors detected. These errors prevent ..."]
		msg += bullet(3) + "Computations of current battery age, battery lifespans, and remaining battery life estimates."
		msg += bullet(3) + "Reports from displaying proper values for the attributes above."
		msg += bullet(3) + "Old Battery Trackers from notifying of batteries approaching end-of-life."
		messages.add(msg.join("\n"))
	}
	if (errors.contains(DATA_LIFE)) {
		ArrayList<String> msg = ["Battery Lifespan errors detected. These errors prevent ..."]
		msg += bullet(3) + "Computations of average battery age, battery lifespans, and remaining battery life estimates."
		msg += bullet(3) + "Reports from displaying proper values for the attributes above."
		msg += bullet(3) + "Old Battery Trackers from notifying of batteries approaching end-of-life."
		if (dataStore in [STORE_OBSERVED, STORE_RECORDED])
			msg += "The value does not reflect the new battery information. Please adjust manually."
		messages.add(msg.join("\n"))
	}
	if (!errors.isEmpty())
		paragraph fpx(14, messages.join("\n" + lpx(10)), COLOR_ERROR)
}
def paragraphLegend(List<String> text, Integer width = 12) {
	paragraph indent(fpx(12, text.join("\n"), "grey"), 10), width: width
}
def paragraphColumn(String text, HashMap<String,String> columnDef) {
	paragraph indent(text, columnDef.margin?.toInteger() ?: 10, columnDef.align), width: columnDef.width
}

def sectionDeviceDataControl(String dataStore, List<String> dataControls, HashMap<String,Integer> buttonCounts) {
	if (!dataControls.isEmpty()) paragraph formatHeader(2, "Controls")
	if (dataControls.contains("Inputs")) sectionDeviceDataControlInputs(dataStore, buttonCounts)
	if (!dataControls.isEmpty()) {
		String header = "Bulk Controls" + (!isHelpEnabledForBatteryData(dataStore) ? "" :
			fpx(12, "  -  apply actions in bulk to all battery-related fields on all devices", "#DBE5E5"))
		paragraph formatHeader(3, header)
	}
	if (dataControls.contains(STATE_CURRENT_MIGRATE)) sectionDeviceDataButtonsPreviousMigrate(dataStore, buttonCounts)
	if (dataControls.contains(STATE_PREVIOUS)) sectionDeviceDataButtonsPrevious(dataStore, buttonCounts)
	if (dataControls.contains(STATE_CURRENT)) sectionDeviceDataButtonsCurrent(dataStore)
	if (dataControls.contains(STATE_CURRENT_MIGRATE)) sectionDeviceDataButtonsCurrentMigrate(dataStore, buttonCounts)
	if (dataControls.contains(STATE_PENDING)) sectionDeviceDataButtonsPending(dataStore, buttonCounts)
	if (dataControls.contains("Cache")) {
		String descCache = (dataStore == STORE_MIGRATE) ? "migration data" : "new-battery observations"
		sectionDeviceDataButton(dataStore, "Clear cache of $descCache", "Clear Cache")
	}
	if (dataControls.contains("Checks"))
		sectionDeviceDataButton(dataStore, "Run checks for new batteries", "Run Checks")
}

def sectionDeviceDataControlInputs(String dataStore, HashMap<String,Integer> buttonCounts) {
	if (isHelpEnabledForBatteryData(dataStore)) {
		boolean isReset = (buttonCounts[DATA_DATE] > 0 || buttonCounts[DATA_LIFE] > 0 || buttonCounts[DATA_TYPE] > 0)
		List<String> instructions = ["Edit the battery information in Device Data to desired values.",
			bullet(3) + "Control actions are applied to all included devices.",
			bullet(3) + "Exclude a device to prevent control actions from updating its values."]
		if (isReset) instructions.add(bullet(3) + "\"Reset\" is the only action applied to excluded devices.")
		List<HashMap<String,String>> buttons =
			[[(getFormat("underline", "Button")): getFormat("underline", "Action")],
			 ["Update": "Applies the updated values in a pending state, but does not alter the Device Data yet"],
			 ["Remove": "Removes the current values in a pending state, but does not alter the Device Data yet"]]
		if (isReset) buttons.add(["Reset": "Erases any currently pending changes"])
		paragraphLegend(instructions, 6)
		paragraphLegend(buttons.collect { it.keySet()[0] }, 1)
		paragraphLegend(buttons.collect { it.values()[0] }, 5)
	}
	String header = "Individual Controls" + (!isHelpEnabledForBatteryData(dataStore) ? "" :
		fpx(12, "  -  apply actions to individual battery-related fields on all devices", "#DBE5E5"))
	paragraph formatHeader(3, header)
	sectionDeviceDataInput(dataStore, DATA_TYPE, "data_battery_type",
		TITLE_DATA_TYPE, buttonCounts[DATA_TYPE] > 0)
	sectionDeviceDataInput(dataStore, DATA_DATE, "data_battery_date",
		TITLE_DATA_DATE, buttonCounts[DATA_DATE] > 0)
	if (settings.data_battery_life_auto == false) {
		sectionDeviceDataInput(dataStore, DATA_LIFE, "data_battery_life",
			TITLE_DATA_LIFE, buttonCounts[DATA_LIFE] > 0)
	}
	inputBoolean("data_battery_life_auto", fpx(15, TITLE_DATA_LIFE_AUTO), [defaultValue:true])
	paragraph ""
}
def sectionDeviceDataInput(String dataStore, String dataType, String inputName, String inputTitle, boolean resetButton) {
	if (dataType == DATA_DATE)
		inputDate(inputName, fpx(15, inputTitle), [width:6])
	else
		inputText(inputName, fpx(15, inputTitle), [width:6])

	String dataValueInput = (String)settings."$inputName"
	String dataValue = (dataType != DATA_DATE) ? dataValueInput : (dataValueInput == null) ? "" :
		formatDateTime(formatDateTime(dataValueInput, DATETIME_FORMAT_SETTING), DATETIME_FORMAT_DATA)
	inputButtonDeviceData(dataStore, "Update", [dataType:dataType, dataValue:dataValue, width:1])
	inputButtonDeviceData(dataStore, "Remove", [dataType:dataType, width:1, EOL:!resetButton])
	if (resetButton)
		inputButtonDeviceData(dataStore, "Reset", [dataType:dataType, width:1, EOL:true, textColor:COLOR_PENDING])

	if (!isDeviceDataValid(dataType, dataValue, true)) {
		String valueDisplay = (dataValue == null || dataValue.isEmpty()) ? "null" : dataValue
		paragraph getFormat("bold-red", "Value ($valueDisplay) is invalid (e.g. format). Please correct. Otherwise application features may fail.")
	}
}

def sectionDeviceDataButtonsCurrent(String dataStore) {
	if (dataStore == STORE_MANUAL) {
        sectionDeviceDataButton(dataStore, "Update battery type, date, and lifespans for all devices",
	        "Update All Values", "Inputs", STATE_PENDING)
	    sectionDeviceDataButton(dataStore, "Remove battery data fields from all devices",
		    "Remove All Values", "[Deletion]", STATE_PENDING)
	}
}
def sectionDeviceDataButtonsCurrentMigrate(String dataStore, HashMap<String,Integer> buttonCounts) {
	if (buttonCounts[STATE_CURRENT_MIGRATE] > 0) {
		sectionDeviceDataButton(dataStore, "Migrate data (copy and/or convert) for all devices",
			"Migrate All Values", STATE_CURRENT_MIGRATE, STATE_PENDING)
	}
}
def sectionDeviceDataButtonsPending(String dataStore, HashMap<String,Integer> buttonCounts) {
	if (buttonCounts[STATE_PENDING] > 0) {
		sectionDeviceDataButton(dataStore, "Reset/clear pending changes for all devices",
			"Reset Pending Values", STATE_PENDING, STATE_CURRENT, COLOR_PENDING)
		sectionDeviceDataButton(dataStore, "Finalize/submit pending changes for all devices",
			"Submit Pending Values", STATE_PENDING, "Device", COLOR_PENDING)
	}
}
def sectionDeviceDataButtonsPrevious(String dataStore, HashMap<String,Integer> buttonCounts) {
	if (buttonCounts[STATE_PREVIOUS] > 0) {
		sectionDeviceDataButton(dataStore, "Restore previous values for all devices",
			"Restore Previous Values", STATE_PREVIOUS, STATE_PENDING, COLOR_PREVIOUS)
	}
}
def sectionDeviceDataButtonsPreviousMigrate(String dataStore, HashMap<String,Integer> buttonCounts) {
	String dataNameMigrate = (String)settings."data_name_migrate"
	String dataNameNow = getDeviceDataName((String)settings."data_type_migrate")
	if (buttonCounts[STATE_CURRENT_MIGRATE] > 0 && dataNameMigrate != dataNameNow) {
		sectionDeviceDataButton(dataStore, "Delete pre-migration values !!! No recovery",
			"Delete Pre-Migration Values", STATE_CURRENT_MIGRATE, "[Deletion]", COLOR_CURRENT_MIGRATE)
	}
}
def sectionDeviceDataButton(String dataStore, String text, String action,
	String stateFrom = null, String stateTo = null, String textColor = COLOR_CURRENT) {
	boolean isStates = (stateFrom != null || stateTo != null)
	paragraph formatHeader(4, fpx(14, text)), width: isStates ? 4 : 6
	if (isStates) paragraph formatHeader(4, indent(fpx(14, "$stateFrom  ->  $stateTo") + gap(),
		10, "right"), false), width: 2
	inputButtonDeviceData(dataStore, action, [width:3, EOL:true, textColor:textColor])
}


@Field public static final String TITLE_DATA_DATE = "Enter the <strong>battery change date</strong>"
@Field public static final String TITLE_DATA_LIFE = "Enter the previous <strong>battery lifespans</strong> in days (comma-separated numbers)"
@Field public static final String TITLE_DATA_LIFE_AUTO = "Compute battery lifespans automatically from the battery change date"
@Field public static final String TITLE_DATA_TYPE = "Enter the <strong>battery type</strong> (e.g. AA)"
@Field public static final String TITLE_DEVICES_EDIT = "Select battery devices to edit"
@Field public static final String TITLE_DEVICES_UNSELECT = "Unselect all devices"

List<LinkedHashMap<String,String>> getDeviceDataColumns(String dataStore) {
	if (dataStore == STORE_MIGRATE) {
		String dataType = (String)settings."data_type_migrate"
		return [
			[type: DATA_INCLUDE, colName: "", width: "1", widthHdr: "1", align: "center", alignHdr: "center"],
			[type: DATA_NAME, colName: "Device Name", width: "3", widthHdr: "3", align: "left", alignHdr: "left"],
			[type: DATA_STATE, colName: "State", width: "1", widthHdr: "1", align: "left", alignHdr: "left"],
			[type: dataType, data: "name", colName: "Device Data Name", width: "2", widthHdr: "2", align: "left", alignHdr: "left"],
			[type: dataType, data: "value", colName: "Device Data Value", width: "3", widthHdr: "5", align: "left", alignHdr: "left"],
		]
	}
	boolean isPercentage = (dataStore != STORE_MANUAL)
	List<LinkedHashMap<String,String>> columns = [
		[type: DATA_INCLUDE, colName: "", width: "1", widthHdr: "1", align: "center", alignHdr: "center"],
		[type: DATA_NAME, colName: "Device Name", width: "3", widthHdr: "3", align: "left", alignHdr: "left"],
		[type: DATA_STATE, colName: "State", width: "1", widthHdr: "1", align: "left", alignHdr: "left"],
		[type: DATA_TYPE, colName: "Type", width: "1", widthHdr: "1", align: "left", alignHdr: "left"],
		[type: DATA_PERCENTAGE, colName: "%", width: "1", widthHdr: "1", align: "right", alignHdr: "center", margin: "45"],
		[type: DATA_DATE, colName: "Chg Date", width: "1", widthHdr: "1", align: "center", alignHdr: "center"],
        [type: DATA_LIFE, colName: "Lifespan",
         width: isPercentage ? "2" : "3", widthHdr: isPercentage ? "4" : "5", align: "left", alignHdr: "left"],
	]
    if (isPercentage) return columns
    return columns[0,1,2,3,5,6]
}

@CompileStatic
static List<HashMap<String,HashMap<String,HashMap<String,String>>>> getDeviceDataState(
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data,
	ArrayList<String> statesInclude = null, ArrayList<String> statesExclude = null) {
	return data?.values()?.findAll { HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice ->
		(statesInclude == null || statesInclude.isEmpty() || statesInclude.every {dataDevice.containsKey(it) }) &&
		(statesExclude == null || statesExclude.isEmpty() || !statesExclude.any {dataDevice.containsKey(it) })
	}?.sort { a, b ->
		a.device.name.value <=> b.device.name.value
	}
}
static HashMap<String,Integer> getDevicesButtonCounts(
	List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataPending,
	List<HashMap<String,HashMap<String,HashMap<String,String>>>> dataIgnored) {
	HashMap<String,Integer> buttonInfo = ["numDeviceButtonsMax": 0,
		"numDevices": (dataPending?.size() ?: 0) + (dataIgnored?.size() ?: 0),
		(DATA_DATE): 0, (DATA_LIFE): 0, (DATA_TYPE): 0,
		(STATE_PENDING): 0, (STATE_PREVIOUS): 0, (STATE_CURRENT_MIGRATE): 0, (STATE_IGNORED): dataIgnored?.size() ?: 0]
	dataPending.each { HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice ->
		int numDeviceButtons = 0
		if (dataDevice.containsKey(STATE_PENDING)) {
			buttonInfo[(String)STATE_PENDING] = 1
			numDeviceButtons += 1
			if (dataDevice[STATE_PENDING].containsKey(DATA_DATE)) buttonInfo[(String)DATA_DATE] = 1
			if (dataDevice[STATE_PENDING].containsKey(DATA_LIFE)) buttonInfo[(String)DATA_LIFE] = 1
			if (dataDevice[STATE_PENDING].containsKey(DATA_TYPE)) buttonInfo[(String)DATA_TYPE] = 1
		}
		if (dataDevice.containsKey(STATE_PREVIOUS) &&
			!isDeviceDataStateEqualTo(dataDevice, (String)STATE_PREVIOUS, (String)STATE_PENDING)) {
			buttonInfo[(String)STATE_PREVIOUS] = 1
			numDeviceButtons += 1
		}
		if (dataDevice.containsKey(STATE_CURRENT_MIGRATE))
			buttonInfo[(String)STATE_CURRENT_MIGRATE] = 1
		if (numDeviceButtons > buttonInfo["numDeviceButtonsMax"])
			buttonInfo["numDeviceButtonsMax"] = numDeviceButtons
	}
	return buttonInfo
}


@CompileStatic
static String getTextColorByButton(String button) {
	if (button == "Restore") return COLOR_PREVIOUS
	if (button == "Submit") return COLOR_PENDING
	return COLOR_CURRENT
}
@CompileStatic
static String getTextColorByState(String state) {
	switch (state) {
		case STATE_PENDING: return COLOR_PENDING
		case STATE_PREVIOUS: return COLOR_PREVIOUS
		case STATE_PREVIOUS_MIGRATE: return COLOR_PREVIOUS_MIGRATE
		case STATE_CURRENT_MIGRATE: return COLOR_CURRENT_MIGRATE
	}
	return COLOR_CURRENT
}
@Field public static final String NO_VALUE = "[no value]"


//-------------------------------------------------------------------------
// Page:  Battery Data Names
//-------------------------------------------------------------------------

def pageBatteryDataNames() {
	resetStateConfigTo(["deviceDataName_*"])

	dynamicPage(name: "pageBatteryDataNames", nextPage: "pageMain") {
		section {
			addToPageHistory("pageBatteryDataNames", "Battery Data Names")
			sectionBatteryDeviceData()
			paragraph formatHeader(1, "Device Data Names")
			paragraph "Select names (i.e. keys) for the battery information stored in the Data section of Device Details."
			[DATA_TYPE, DATA_DATE, DATA_LIFE].each { String dataType ->
				String nameTitle = TITLE_DATA_TYPE(dataType)
				String namePrvKey = "deviceDataName_${dataType}_previous"
				String nameNewKey = "deviceDataName_${dataType}_new"
				String nameNewValue = settings."$nameNewKey"
				String nameNowValue = getDeviceDataName(dataType)
				if (nameNewValue?.contains(" ")) {
					nameNewValue = removeSpaces(nameNewValue)
					app.updateSetting(nameNewKey, [type: "text", value: nameNewValue])
				}
				String nameSubmitKey = "deviceDataName_${dataType}"
				String nameSubmitted = getStateConfigureValueString(nameSubmitKey)
				boolean isNameSubmitted = (nameSubmitted != null && !nameSubmitted.isEmpty())
				if (isNameSubmitted && nameSubmitted == nameNewValue && nameNewValue != nameNowValue) {
					app.updateSetting(namePrvKey, [type: "text", value: nameNowValue])
					app.removeSetting(nameNewKey)
					setDeviceDataName(dataType, nameNewValue)
					nameNowValue = nameNewValue
					nameNewValue = null
				}
				String namePrvValue = settings."$namePrvKey"
				paragraph formatHeader(3, nameTitle)
				String msg = bullet(3) + DESC_DATA_TYPE(dataType) + "\n"
				if (isNameSubmitted && namePrvValue != null) {
					msg += bullet(3) + "\"$namePrvValue\" is the previous name "
					msg += "under which $nameTitle information was stored\n"
				}
				msg += bullet(3) + "\"$nameNowValue\" is the current name under which $nameTitle information is stored"
				paragraph msg
				if (isNameSubmitted) {
					paragraph "Please consider migrating the battery information in your devices from $namePrvValue to $nameNowValue"
					paragraph hrefButton("Migrate",
						"./pageBatteryDataMigrate?dataMigrationType=${dataType}")
				}
				String title = "Enter the data name used to store $nameTitle information (no spaces please)"
				inputText(nameNewKey, title, [width:6, endOfLine:true])
				if (nameNewValue != null && nameNewValue != nameNowValue) {
					input(name: "deviceDataName|${nameSubmitKey}", title: "Submit", type: "button", newLineAfter: true)
				}
				paragraph ""
			}
			sectionFooter()
		}
	}
}

@CompileStatic
static String DESC_DATA_TYPE(String dataType) {
	switch (dataType) {
		case DATA_DATE: return "<strong>Battery Date</strong> information contains the most recent battery change date " + fpx(14, "(e.g. 2024/11/06)")
		case DATA_LIFE: return "<strong>Battery Lifespan</strong> information contains the lifespans of previous batteries " + fpx(14, "(in days, e.g. 365,479,231)")
	}
	return "<strong>Battery Type</strong> information contains descriptive text about the battery type " + fpx(14, "(e.g. AA x 2)")
}
@CompileStatic
static String TITLE_DATA_TYPE(String dataType) {
	switch (dataType) {
		case DATA_DATE: return "Battery Date"
		case DATA_LIFE: return "Battery Lifespan"
	}
	return "Battery Type"
}


//-------------------------------------------------------------------------
// Page:  Battery Data Migrate
//-------------------------------------------------------------------------

def pageBatteryDataMigrate(Map params) {
	Map<String,Object> parameters = getPageParameterInputs("pageBatteryDataMigrate", params)
	if (parameters != null && !parameters.isEmpty()) {
		parameters["pageNext"] = getPageLast("pageBatteryDataMigrate") ?: "pageMain"
		String dataType = (String)parameters?.dataMigrationType
		String dataName = (String)settings."deviceDataName_${dataType}_previous"
		app.updateSetting("data_type_migrate", [type: "enum", value: dataType])
		app.updateSetting("data_name_migrate", [type: "text", value: dataName])
		app.removeSetting("data_date_format")
		app.removeSetting("data_life_units")
		parameters["dataMigrationType"] = dataType
		parameters["dataMigrationName"] = dataName
		parameters.remove("dataMigrationDateFormat")
		parameters.remove("dataMigrationLifeUnits")
		setPageParameters("pageBatteryDataMigrate", parameters)
	}
	parameters = getPageParameters("pageBatteryDataMigrate")
	resetStateConfigTo(["deviceDataName_*"])
	String dataNameMigratePrevious = parameters?.dataMigrationName
	String dataType = parameters?.dataMigrationType
	String dateFormat = parameters?.dataMigrationDateFormat
	String lifeUnits = parameters?.dataMigrationLifeUnits
	String dataStore = STORE_MIGRATE

	dynamicPage(name: "pageBatteryDataMigrate", nextPage: parameters?.pageNext ?: "pageMain") {
		section {
			addToPageHistory("pageBatteryDataMigrate", "Battery Data Migration")
			sectionBatteryDeviceData()
			paragraph formatHeader(1, "Data Migration")
			paragraph "Migrate battery information for selected devices in the Data section of Device Details (i.e. move data from old field names/keys to the current names/keys)", width: 10
			inputBooleanBatteryDataHelp(dataStore, 2)
			paragraph formatHeader(2, "Battery Devices")
			paragraph "Select devices with battery data to migrate to different Device Data names.\n"
			Integer devicesNum = ((DeviceWrapperList)settings.data_devices_all)?.size() ?: 0
			href(
				name: "pageBatteryDataMigrateDevicesManage",
				page: "pageBatteryDataMigrateDevices",
				title: getFormat("description", ((devicesNum > 0) ? "$devicesNum" : "No") + " devices selected"),
				description: fpx(14, (devicesNum > 0) ? "Click to edit" : "Select migration devices")
			)
			paragraph ""
			paragraph formatHeader(2, "Battery Data Name")
			String dataTypeNew = (String)settings."data_type_migrate" ?: dataType
			String dateFormatNew = (String)settings."data_date_format"
			String lifeUnitsNew = (String)settings."data_life_units" ?: UNIT_DAYS
			if (dataType == null && dataTypeNew != null)
				dataType = parameters?.dataMigrationType = dataTypeNew
			if (dateFormat == null && dateFormatNew != null)
				dateFormat = parameters?.dataMigrationDateFormat = dateFormatNew
			if (lifeUnits == null && lifeUnitsNew != null)
				lifeUnits = parameters?.dataMigrationLifeUnits = lifeUnitsNew
			if (dataType != dataTypeNew)
				app.removeSetting("data_name_migrate")
			if (dataType != dataTypeNew || dateFormat != dateFormatNew || lifeUnits != lifeUnitsNew) {
				dataType = parameters?.dataMigrationType = dataTypeNew
				dateFormat = parameters?.dataMigrationDateFormat = dateFormatNew
				lifeUnits = parameters?.dataMigrationLifeUnits = lifeUnitsNew
				deleteDataStore(dataStore)
			}
			inputEnum("data_type_migrate", TITLE_TYPE_MIGRATE, OPTIONS_DATA_TYPE(), [defaultValue:dataType])
			if (dataType == DATA_DATE)
				inputText("data_date_format", TITLE_DATE_FORMAT)
			else {
				dateFormat = (String)null
				parameters?.remove("dataMigrationDateFormat")
				app.removeSetting("data_date_format")
			}
			if (dataType == DATA_LIFE)
				inputEnum("data_life_units", TITLE_LIFE_FORMAT, [UNIT_DAYS, UNIT_WEEKS], [defaultValue:UNIT_DAYS])
			else {
				lifeUnits = (String)null
				parameters?.remove("dataMigrationLifeUnits")
				app.removeSetting("data_life_units")
			}
			String dataNamePrevious = (String)settings."deviceDataName_${dataType}_previous"
			String dataNameMigrate = (String)settings."data_name_migrate" ?: dataNameMigratePrevious ?: dataNamePrevious
			if (dataNameMigrate != dataNameMigratePrevious)
				deleteDataStore(dataStore)
			if (dataNameMigrate != null)
				parameters["dataMigrationName"] = dataNameMigrate
			inputText("data_name_migrate", TITLE_NAME_MIGRATE, [defaultValue:dataNameMigrate])
			paragraph ""

			if (dataNameMigrate != null &&
				(dataType != DATA_DATE || dateFormat != null) && (dataType != DATA_LIFE || lifeUnits != null)) {
				HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataNow = getDataStore(dataStore)
				HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataNew = [:]
				DeviceWrapperList devices = getSettingsDeviceWrapperList("data_devices_all")
				devices?.each { DeviceWrapper device ->
					if (device.controllerType == "LNK") return
					HashMap<String,HashMap<String,HashMap<String,String>>> dataDeviceNew = [
						device: getDeviceIdentity(device, "data_devices_all"),
						(STATE_CURRENT): getDeviceData(device, [(dataType): getDeviceDataName(dataType)], true)
					]
					HashMap<String,HashMap<String,String>> dataCurrentMigrateNow =
						dataNow?."${device.id}"?."$STATE_CURRENT_MIGRATE"
					if (dataCurrentMigrateNow == null)
						dataCurrentMigrateNow = getDeviceData(device, [(dataType): dataNameMigrate])
					HashMap<String,HashMap<String,String>> dataPreviousMigrateNow =
						dataNow?."${device.id}"?."$STATE_PREVIOUS_MIGRATE"
					HashMap<String,HashMap<String,String>> dataPreviousNow = dataNow?."${device.id}"?."$STATE_PREVIOUS"
					HashMap<String,HashMap<String,String>> dataPendingNow = dataNow?."${device.id}"?."$STATE_PENDING"
					HashMap<String,HashMap<String,String>> dataIgnoredNow = dataNow?."${device.id}"?."$STATE_IGNORED"
					if (dataPreviousMigrateNow != null && dataNameMigratePrevious != null &&
						dataPreviousMigrateNow[dataType]?.name == dataNameMigratePrevious)
						dataDeviceNew[STATE_PREVIOUS_MIGRATE] = dataPreviousMigrateNow
					if (dataCurrentMigrateNow != null) dataDeviceNew[STATE_CURRENT_MIGRATE] = dataCurrentMigrateNow
					if (dataPreviousNow != null) dataDeviceNew[STATE_PREVIOUS] = dataPreviousNow
					if (dataPendingNow != null) dataDeviceNew[STATE_PENDING] = dataPendingNow
					if (dataIgnoredNow != null) dataDeviceNew[STATE_IGNORED] = dataIgnoredNow

					if (dataDeviceNew[STATE_CURRENT_MIGRATE]?."$dataType" != null ||
						dataPreviousMigrateNow?."$dataType" != null)
						dataNew[device.id] = dataDeviceNew
				}
				if (dataNew == null || dataNew.isEmpty())
					paragraph "<strong>No data to migrate.  The selected devices do not have values stored under the name \"$dataNameMigrate\".</strong>"
				setDataStore(dataStore, dataNew)
				sectionDeviceDataManagement(dataStore, dataNew,
					[STATE_PREVIOUS, STATE_PREVIOUS_MIGRATE, STATE_CURRENT_MIGRATE, STATE_CURRENT, STATE_PENDING, "Cache"])
			}
			sectionFooter()
			setPageParameters("pageBatteryDataMigrate", parameters)
		}
	}
}


@Field public static final String TITLE_DATE_FORMAT = "Enter the date format of the currently stored dates (e.g. \"yyyy/MM/dd\", which is the new format used by the app)"
@Field public static final String TITLE_LIFE_FORMAT = "Select the measurement unit of the currently stored lifespans (e.g. \"days\", which is the measurement unit used by the app)"
@Field public static final String TITLE_NAME_MIGRATE = "Enter the data name that currently stores the battery information to migrate"
@Field public static final String TITLE_TYPE_MIGRATE = "Select the battery data to migrate"

@CompileStatic
List<LinkedHashMap<String,String>> OPTIONS_DATA_TYPE() {
	return [
		[(DATA_DATE): "The \"Battery Date\" field that contains the most recent battery change date (current name is " + getDeviceDataName(DATA_DATE) + ")"],
		[(DATA_LIFE): "The \"Battery Lifespan\" field that contains the lifespans of previous batteries (current name is " + getDeviceDataName(DATA_LIFE) + ")"],
		[(DATA_TYPE): "The \"Battery Type\" field that contains descriptive text about the battery type (current name is " + getDeviceDataName(DATA_TYPE) + ")"]]
}


//-------------------------------------------------------------------------
// Page:  Battery Data Migration Devices
//-------------------------------------------------------------------------

def pageBatteryDataMigrateDevices() {
	state.remove("configure")

	dynamicPage(name: "pageBatteryDataMigrateDevices", nextPage: "pageBatteryDataMigrate") {
		section {
			addToPageHistory("pageBatteryDataMigrateDevices", "N/A")
			sectionTopMenu()
			paragraph formatTitle("Battery Data")
			paragraph formatHeader(1, "Device Data Migration")
			paragraph formatHeader(2, "Battery Devices")
			inputCapability("data_devices_all", TITLE_DEVICES_MIGRATE, CAPABILITY_ALL)
			paragraph fpx(14, "Hint: select all devices, as this process only operates on devices with data in the old names.", "grey")
			DeviceWrapperList devices = getSettingsDeviceWrapperList("data_devices_all")
			ArrayList<String> msgHubMesh = []
			devices.findAll { it.controllerType == "LNK" }?.sort { a,b ->
				a.displayName <=> b.displayName
			}?.each {
				msgHubMesh.add(bullet(3) + it.displayName)
			}
			if (!msgHubMesh.isEmpty()) {
				String msg = "The following devices are HubMesh devices. This hub cannot modify them, so they will not appear for data migration.\n"
				paragraph fpx(14, msg + msgHubMesh.join("\n"), COLOR_ERROR)
			}
			sectionFooter()
		}
	}
}
@Field public static final String TITLE_DEVICES_MIGRATE = "Select battery devices with data to migrate"


//-------------------------------------------------------------------------
// Page:  Battery Device Trackers
//-------------------------------------------------------------------------

def pageBatteryDeviceTrackers(Map params) {
	String pageTab = getPageTab("pageBatteryDeviceTrackers") ?: "LastActivityTrackers"
	"pageBatteryDeviceTrackers_${pageTab}"(params)
}

def sectionBatteryDeviceTrackers(String tabName, Map params) {
	tryCompleteRequestAction(params)
	cleanupObjects()

	Map<String,Object> parameters = getPageParameterInputs("pageBatteryDeviceTrackers", params)
	setPageParameters("pageBatteryDeviceTrackers", parameters)
	setPageTab("pageMain", "BatteryDeviceTrackers")
	setPageTab("pageBatteryDeviceTrackers", removeSpaces(tabName))
	addToPageHistory("pageBatteryDeviceTrackers", tabName)

	sectionTopMenu()
	paragraph formatTitle("Battery Device Trackers")
	sectionPageMenu("pageBatteryDeviceTrackers", PAGE_TRACKERS_TABS, true)
}
@Field public static final List<Map<String,String>> PAGE_TRACKERS_TABS = [
	[name: "LastActivityTrackers", display: "Last Activity Trackers"],
	[name: "LowBatteryTrackers", display: "Low Battery Trackers"],
	[name: "NewBatteryTrackers", display: "New Battery Trackers"],
	[name: "OldBatteryTrackers", display: "Old Battery Trackers"]
]

def pageBatteryDeviceTrackers_LastActivityTrackers(Map params) {
	dynamicPage(name: "pageBatteryDeviceTrackers_LastActivityTrackers", nextPage: "pageMain") {
		sectionBatteryTracker(OBJECT_MAP[OBJECT_LAST], "Last Activity Trackers", params)
	}
}
def pageBatteryDeviceTrackers_LowBatteryTrackers(Map params) {
	dynamicPage(name: "pageBatteryDeviceTrackers_LowBatteryTrackers", nextPage: "pageMain") {
		sectionBatteryTracker(OBJECT_MAP[OBJECT_LOW], "Low Battery Trackers", params)
	}
}
def pageBatteryDeviceTrackers_NewBatteryTrackers(Map params) {
	dynamicPage(name: "pageBatteryDeviceTrackers_NewBatteryTrackers", nextPage: "pageMain") {
		sectionBatteryTracker(OBJECT_MAP[OBJECT_NEW], "New Battery Trackers", params)
	}
}
def pageBatteryDeviceTrackers_OldBatteryTrackers(Map params) {
	dynamicPage(name: "pageBatteryDeviceTrackers_OldBatteryTrackers", nextPage: "pageMain") {
		sectionBatteryTracker(OBJECT_MAP[OBJECT_OLD], "Old Battery Trackers", params)
	}
}


//-------------------------------------------------------------------------
// Page:  Battery Device Tracker
//-------------------------------------------------------------------------

def sectionBatteryTracker(HashMap<String,String> om, String tabName, Map params) {
	section {
		sectionBatteryDeviceTrackers(tabName, params)

		String descObjectType = removeSpaces(om.descC)
		List<String> objectIds = getObjectIds(om)
		Integer objectsSz = objectIds?.size() ?: 0

		paragraph formatHeader(2, "${om.descC.replace('-', ' ')}s")
		paragraph getDescriptionTrackers()[om.type]
		href(
			name: "page${descObjectType}Add",
			page: "pageBatteryDeviceTracker",
			title: "Add Tracker",
			description: "Click to add a new ${om.descC}",
			params: [objectType: om.type]
		)
		objectIds.eachWithIndex { String objectId, Integer idx ->
			href(
				name: "page${descObjectType}Manage",
				page: "pageBatteryDeviceTracker",
				title: hrefTitle(getObjectDescription(om, objectId).fullL),
				description: lpx(8) + "Click to edit/copy/delete",
				width: objectsSz > 1 ? 10 : 12,
				params: [objectType: om.type, objectId: objectId]
			)
			inputButtonsSwapOrder(om.type, objectId, objectsSz, idx)
		}
		sectionFooter()
	}
}

def pageBatteryDeviceTracker(Map params) {
	if (!tryCompleteRequestAction(params) && !isPageCurrent("pageBatteryDeviceTracker")) {
		Map<String,Object> parameters = getPageParameterInputs("pageBatteryDeviceTracker", params)
		if (parameters != null) {
			parameters.pageNext = getPageLast("pageBatteryDeviceTracker") ?: "pageBatteryDeviceTrackers"
			if (parameters.objectId == null)
				parameters.objectId = addObject(OBJECT_MAP[(String)((Map)parameters)?.objectType])
			setPageParameters("pageBatteryDeviceTracker", parameters)
		}
	}
	String pageTab = getPageTab("pageBatteryDeviceTracker")
	if (pageTab in ["CopyDelete",null]) pageTab = "Alerts"
	"pageBatteryDeviceTracker_${pageTab}"()
}

def sectionBatteryDeviceTracker(String tabName, String objectType, String objectId, Map<String,Boolean> required = null) {
	tryDeleteObjectSettingsUnused(objectId)
	addToPageHistory("pageBatteryDeviceTracker", "Battery Tracker")
	setPageTab("pageBatteryDeviceTracker", tabName)
	sectionTopMenu()
	HashMap<String,String> om = OBJECT_MAP[objectType]
	String objectNum = parseObjectIdNum(objectId).toString()
	paragraph formatTitle(om.descC.replace('-', ' '))
	paragraph formatHeader(1, "${om.descC.replace('-', ' ')} #${objectNum}")
	paragraph getDescriptionTrackers()[om.type]
	List<Map<String,String>> tabs = PAGE_TRACKER_TABS.collect { [*:it] }
	if (required?.alerts) tabs.find { it.name == "Alerts" }["required"] = "true"
	if (required?.devices) tabs.find { it.name == "Devices" }["required"] = "true"
	sectionPageMenu("pageBatteryDeviceTracker", tabs, true)
}
Map<String,Boolean> getBatteryDeviceTrackerRequiredTodo(String objectType, String objectId) {
	Map<String,Boolean> required = [alerts: false, devices: false]
	if (!isObjectAlertSetup(objectType, objectId) || !isObjectScheduleSetup(objectId))
		required.alerts = true
	if (!isObjectDevicesSetup(objectId))
		required.devices = true
	return required
}
@Field public static final List<Map<String,String>> PAGE_TRACKER_TABS = [
	[name: "Alerts", display: "Define Alerts"],
	[name: "Devices", display: "Define Devices"],
	[name: "Run", display: "Run"],
	[name: "CopyDelete", display: "Copy/Delete"]
]
static Map<String,String> getDescriptionTrackers() {
	String descLA = bullet(3) + "If the device has not communicated recently, the battery might be dead.\n"
	descLA += bullet(3) + "Notify if the last device communication was too long ago."
	String descLow = bullet(3) + "The battery percentage indicates that it may be nearing its end of life.\n"
	descLow += bullet(3) + "Notify if the battery percentage is too low.\n"
	descLow += bullet(3) + "Notify if the battery percentage drops too much."
	String descNew = bullet(3) + "The battery percentage indicates that the battery was likely replaced.\n"
	descNew += bullet(3) + "Notify if the battery percentage is high enough.\n"
	descNew += bullet(3) + "Notify if the battery percentage increases enough.\n"
	descNew += bullet(3) + "Record change dates, battery life, etc. in Device Data."
	String descOld = bullet(3) + "Compare the age of the current battery against its life expectancy (average age of previous batteries).\n"
	descOld += bullet(3) + "Requires change date and life expectancy information in Device Data (recorded manually or via New Battery trackers).\n"
	descOld += bullet(3) + "Notify if the device battery is old enough that it may be nearing its end of life."
	return [(OBJECT_LAST): descLA, (OBJECT_LOW) : descLow, (OBJECT_NEW) : descNew, (OBJECT_OLD) : descOld]
}


def pageBatteryDeviceTracker_Alerts(Map params) {
	Map<String,Object> parameters = getPageParameters("pageBatteryDeviceTracker")
	String objType = parameters.objectType
	String objId = parameters.objectId
	if (!isObjectId(objId)) return "${parameters.pageNext}"()
	Map<String,Boolean> requiredTodo = getBatteryDeviceTrackerRequiredTodo(objType, objId)
	String pageNext = (requiredTodo.devices) ? "pageBatteryDeviceTracker_Devices" : parameters.pageNext
	HashMap<String,String> om = OBJECT_MAP[objType]
	Integer width = 7

	dynamicPage(name: "pageBatteryDeviceTracker_Alerts", nextPage: pageNext) {
		section {
			sectionBatteryDeviceTracker("Alerts", objType, objId, requiredTodo)
			paragraph formatHeader(2, "Alerts")
			paragraph formatHeader(3, "Detection"), width: width
			if (objType == OBJECT_LOW) {
				sectionThresholds(objType, objId)
			} else if (objType == OBJECT_NEW) {
				sectionThresholds(objType, objId)
				inputBoolean("${objId}_observe", TITLE_NEW_OBSERVE)
				if (settings."${objId}_observe" == true) {
					inputBoolean("${objId}_record", TITLE_NEW_RECORD)
				}
			} else if (objType == OBJECT_LAST) {
				boolean required = settings."${objId}_intervalDays" == null &&
					settings."${objId}_intervalHours" == null &&
					settings."${objId}_intervalMinutes" == null
				paragraph "Alert when the devices have been inactive for more than ..."
				inputTimeInterval("${objId}_intervalDays", "days", [required:required, width:2])
				inputTimeInterval("${objId}_intervalHours", "hours", [required:required, width:2])
				inputTimeInterval("${objId}_intervalMinutes", "minutes", [required:required, width:3, EOL:true])
				if (required)
					paragraph "** ${om.descC}s require at least one of days, hours, or minutes"
			} else if (objType == OBJECT_OLD) {
				inputEnum("${objId}_intervalRelative", "",
					OPTIONS_INTERVAL_RELATIVE(), [defaultValue:RELATIVE_AT, required:true, width:width, EOL:true])
				if ((settings."${objId}_intervalRelative" ?: RELATIVE_AT) != RELATIVE_AT)
					inputTimeInterval("${objId}_intervalDays", fpx(14, "by this many days"), [width:width])
				else app.removeSetting("${objId}_intervalDays")
			}
			paragraph ""
			paragraph formatHeader(3, "Schedule"), width: width
			boolean isGlobalScheduleAvailable = isObjectScheduleSetup(OBJECT_ID_APP)
			if (!isGlobalScheduleAvailable && isObjectUsingGlobalSchedule(objId))
				copyObjectScheduleFromBackup(objId)
			if (isObjectUsingGlobalSchedule(objId))
				deleteObjectSchedule(objId)
			else
				sectionSchedule(objId)
			if (isGlobalScheduleAvailable) {
				String globalSchedule = getObjectDescriptionTime(OBJECT_ID_APP).fullS.replace("[", "on ") - " @ " - "]"
				String globalScheduleLink = hrefLink("pageGlobalSchedule", globalSchedule)
				String title = "Use the global schedule ${fpx(14, globalScheduleLink)}"
				inputBoolean("${objId}_useGlobalSchedule", title, [defaultValue:true])
			}
			if (objType == OBJECT_NEW)
				paragraph fpx(14, "New-battery alerts do not repeat.  They occur once for each new battery.")
			sectionFooter()
		}
	}
}
def sectionThresholds(String objectType, String objectId) {
	boolean required = settings."${objectId}_threshold_absolute" == null &&
		settings."${objectId}_threshold_relative" == null
	inputBatteryPercentage("${objectId}_threshold_absolute",
		TITLE_ABSOLUTE[objectType], RANGE_ABSOLUTE[objectType], [required:required, width:7, EOL:true])
	inputBatteryPercentage("${objectId}_threshold_relative",
		TITLE_RELATIVE[objectType], RANGE_RELATIVE, [required:required, width:7, EOL:true])
	if (objectType == OBJECT_NEW)
		inputTimeInterval("${objectId}_intervalDaysSuppress", TITLE_DAYS_SUPPRESS,
			[width:7, defaultValue:21, EOL:true])
}
@Field public static final ArrayList<String> OPTIONS_DAYOFWEEK =
	["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
@Field public static final String OPTIONS_INTERVAL_RELATIVE_BASE1 = "Alert when the current battery's life is "
@Field public static final String OPTIONS_INTERVAL_RELATIVE_BASE2 = " the average battery life"
@CompileStatic
static Map<String,String> OPTIONS_INTERVAL_RELATIVE() {
	return [
		(RELATIVE_BEFORE): OPTIONS_INTERVAL_RELATIVE_BASE1 + RELATIVE_BEFORE + OPTIONS_INTERVAL_RELATIVE_BASE2,
		(RELATIVE_AT)    : OPTIONS_INTERVAL_RELATIVE_BASE1 + RELATIVE_AT + OPTIONS_INTERVAL_RELATIVE_BASE2,
		(RELATIVE_AFTER) : OPTIONS_INTERVAL_RELATIVE_BASE1 + RELATIVE_AFTER + OPTIONS_INTERVAL_RELATIVE_BASE2]
}
@Field public static final Map<String,String> RANGE_ABSOLUTE = [LBN: "1..100", NBN: "0..99"]
@Field public static final String RANGE_RELATIVE = "1..99"

@Field public static final Map<String,String> TITLE_ABSOLUTE = [
	LBN: "Alert when the battery percentage goes below ...",
	NBN: "Alert when the battery percentage goes above ..."
]
@Field public static final Map<String,String> TITLE_RELATIVE = [
	LBN: "Alert when the battery percentage decreases more than ...",
	NBN: "Alert when the battery percentage increases more than ..."
]
@Field public static final String TITLE_DAYS_SUPPRESS = "Do not alert if the recorded battery change date or most recent alert date is within this many days [0 to always alert].\n" + fpx(12, "This prevents battery fluctuations from triggering false alarms.", "grey")
@Field public static final String TITLE_NEW_OBSERVE = "Automatically observe battery changes for later recording in device Data"
@Field public static final String TITLE_NEW_RECORD = "Automatically record observed battery changes in device Data"


def pageBatteryDeviceTracker_Devices(Map params) {
	Map<String,Object> parameters = getPageParameters("pageBatteryDeviceTracker")
	String objType = parameters.objectType
	String objId = parameters.objectId
	if (!isObjectId(objId)) return "${parameters.pageNext}"()
	Map<String,Boolean> requiredTodo = getBatteryDeviceTrackerRequiredTodo(objType, objId)
	String pageNext = (requiredTodo.alerts) ? "pageBatteryDeviceTracker_Alerts" : parameters.pageNext
	Integer width = 7

	dynamicPage(name: "pageBatteryDeviceTracker_Devices", nextPage: pageNext) {
		section {
			sectionBatteryDeviceTracker("Devices", objType, objId, requiredTodo)
			paragraph formatHeader(2, "Devices")
			paragraph fpx(14, "Select a combination of global and local devices for use by this tracker.", "grey")
			paragraph formatHeader(3, "Tracked Devices"), width: width
			boolean required = (settings.app_devices_tracked == null || !isObjectUsingAppDevices(objId, "tracked"))
			sectionLocalDeviceType(objType, objId, CAPABILITY_BATTERY, "tracked", required, width)
			paragraph formatHeader(3, "Paused Devices"), width: width
			sectionLocalDeviceType(objType, objId, CAPABILITY_BATTERY, "paused", false, width)
			paragraph formatHeader(3, "Notification Devices"), width: width
			sectionLocalDeviceType(objType, objId, CAPABILITY_NOTIFICATION, "notification", false, width)
			sectionFooter()
		}
	}
}
def sectionLocalDeviceType(String objectType, String objectId, String capability, String deviceType,
	boolean devicesRequired, Integer width = 12) {
	boolean isPausedDevices = (deviceType == "paused")
	boolean isUsingAppDevices = isObjectUsingAppDevices(objectId, deviceType)
	String descGlobal = (capability == CAPABILITY_NOTIFICATION) ? "global" : "globally"
	//jdc: only show if global devices are set already
	if (!isPausedDevices || isObjectUsingAppDevices(objectId, "tracked"))
		inputBoolean("${objectId}_devices_${deviceType}_use_app",
			"Include the $descGlobal $deviceType devices", [defaultValue:true])
	else if (isPausedDevices)
		app.updateSetting("${objectId}_devices_${deviceType}_use_app", [type: "bool", value: false])
	String descTitle = TITLE_DEVICES[deviceType]
	if (!isUsingAppDevices)
		descTitle = descTitle.replace("additional ", "")
	if (isPausedDevices)
		hrefPauseDevices(objectType, objectId, descTitle, width)
	else
		inputCapability("${objectId}_devices_${deviceType}", descTitle, capability,
			[required:devicesRequired, width:width])
	if (isUsingAppDevices && !isPausedDevices) {
		Set<String> deviceIdsApp = getSettingsDevicesMap(OBJECT_ID_APP, deviceType, false, false)?.keySet()
		DeviceWrapperList devicesObj = getSettingsDeviceWrapperList("${objectId}_devices_${deviceType}")
		List<DeviceWrapper> deviceDupes = devicesObj.findAll { deviceIdsApp.contains(it.id) }
		ArrayList<String> msg = []
		deviceDupes.each { DeviceWrapper device -> msg.add(bullet(3) + device.displayName) }
		if (!msg.isEmpty()) {
			String msgHdr = "The following local selections are global devices, so the tracker already includes them.  They can be deselected.\n"
			paragraph fpx(14, msgHdr + msg.join("\n"), COLOR_ERROR)
		}
	}
	paragraph ""
}
@Field public static final HashMap<String,String> TITLE_DEVICES = [
	"notification": "Select additional notification devices for this tracker  (i.e. local notification devices)",
	"paused"      : "Select additional battery devices on which to pause tracking  (i.e. locally paused devices)",
	"tracked"     : "Select additional battery devices to monitor with this tracker  (i.e. locally tracked devices)"
]


def pageBatteryDeviceTracker_Run(Map params) {
	Map<String,Object> parameters = getPageParameters("pageBatteryDeviceTracker")
	String objType = parameters.objectType
	String objId = parameters.objectId
	if (!isObjectId(objId)) return "${parameters.pageNext}"()

	dynamicPage(name: "pageBatteryDeviceTracker_Run", nextPage: parameters.pageNext) {
		section {
			sectionBatteryDeviceTracker("Run", objType, objId)
			paragraph formatHeader(2, "Run")
			if (settings."${OBJECT_ID_APP}_devices_notification" != null || settings."${objId}_devices_notification" != null)
				hrefBatteryDeviceTrackerData(objType, objId, "Run the tracker now (with notifications)", true, 7)
			hrefBatteryDeviceTrackerData(objType, objId, "Run the tracker now (without notifications)", false, 7)
			sectionFooter()
		}
	}
}


def pageBatteryDeviceTracker_CopyDelete(Map params) {
	Map<String,Object> parameters = getPageParameters("pageBatteryDeviceTracker")
	String objType = parameters.objectType
	String objId = parameters.objectId
	if (!isObjectId(objId)) return "${parameters.pageNext}"()

	dynamicPage(name: "pageBatteryDeviceTracker_CopyDelete", nextPage: parameters.pageNext) {
		section {
			sectionBatteryDeviceTracker("CopyDelete", objType, objId)
			if (isObjectSetupComplete(objType, objId)) {
				paragraph formatHeader(2, "Copy")
				sectionCopy([objectType: objType, objectId: objId, width: "7"])
			} else paragraph "Tracker setup is incomplete and cannot be copied."

			paragraph formatHeader(2, "Deletion")
			sectionDelete([objectType: objType, objectId: objId, width: "7"])
			sectionFooter()
		}
	}
}


//-------------------------------------------------------------------------
// Page:  Battery Device Tracker Data
//-------------------------------------------------------------------------

def hrefBatteryDeviceTrackerData(String objectType, String objectId, String title, boolean notify, Integer width = 12) {
	href(
		name: "pageBatteryDeviceTrackerData${objectId}",
		page: "pageBatteryDeviceTrackerData",
		title: title,
		description: "Click to execute the tracker, view the results, and pause alerting devices",
		width: width,
		params: [objectType: objectType, objectId: objectId, notify: notify]
	)
}

@CompileStatic
static List<LinkedHashMap<String,String>> getBatteryDeviceTrackerDataColumns(String objectType, String pauseIcon, boolean pauseOnly = false) {
	List<LinkedHashMap<String,String>> columnInfo = [
		[type: REPORT_INFO, colName: "Alert", width: "1", widthHdr: "1", align: "center", alignHdr: "center"]]
	List<LinkedHashMap<String,String>> columnName = [
		[type: REPORT_NAME_LINK, colName: "Device Name", width: "4", widthHdr: "4", align: "left", alignHdr: "left", margin: "20"]]
	List<LinkedHashMap<String,String>> columnType = [
		[type: REPORT_TYPE, colName: "Type", width: "2", widthHdr: "2", align: "left", alignHdr: "left", margin: "20"]]
	List<LinkedHashMap<String,String>> columnsPause = [
		[type: REPORT_PAUSE, colName: "Pause   ${pauseIcon}", width: "1", widthHdr: "1", align: "center", alignHdr: "center"],
		[type: REPORT_UNPAUSE_DATE, colName: "Unpause Date", width: "2", widthHdr: "2", align: "center", alignHdr: "center"]]
	List<LinkedHashMap<String,String>> columnsPercentage = [
		[type: REPORT_PERCENTAGE, colName: "%", width: "1", widthHdr: "1", align: "right", margin: "20", alignHdr: "center"],
		[type: REPORT_PERCENTAGE_CHANGE, colName: "% Change", width: "1", widthHdr: "1", align: "right", margin: "20", alignHdr: "right", marginHdr: "20"]]
	Map<String,List<LinkedHashMap<String,String>>> columnsObject = [
		(OBJECT_LAST): [
		 [type: REPORT_LAST_ACTIVITY, colName: "Last Activity", width: "2", widthHdr: "2", align: "center", alignHdr: "center"]],
		(OBJECT_LOW): columnsPercentage,
		(OBJECT_NEW): columnsPercentage,
		(OBJECT_OLD): [
		 [type: REPORT_AGE_CURRENT, colName: "Age Now", width: "1", widthHdr: "1", align: "right", margin: "15", alignHdr: "right"],
		 [type: REPORT_AGE_ALERT, colName: "Age Alert", width: "1", widthHdr: "1", align: "right", margin: "15", alignHdr: "right"]]
	]
	if (pauseOnly)
		return columnName + columnsPause + [[type: "space", width: "5", widthHdr: "5"]]
	return columnInfo + columnName + columnType + columnsObject[objectType] + columnsPause
}

def pageBatteryDeviceTrackerData(Map params) {
	if (!isPageCurrent("pageBatteryDeviceTrackerData")) {
		Map<String,Object> parameters = getPageParameterInputs("pageBatteryDeviceTrackerData", params)
		if (parameters != null) {
			boolean isNotify = (parameters.notify != null) ? (Boolean)parameters?.notify : true
			String objectId = (String)parameters?.objectId
			parameters.data = runBatteryTrackerManually(OBJECT_MAP[(String)parameters?.objectType], objectId, isNotify) ?: []
			parameters.pageNext = getPageLast("pageBatteryDeviceTrackerData") ?: "pageBatteryDeviceTracker"
			setPageParameters("pageBatteryDeviceTrackerData", parameters)
		}
	}
	Map<String,Object> parameters = getPageParameters("pageBatteryDeviceTrackerData")
	String objectType = parameters.objectType
	String objectId = parameters.objectId
	Map<String,String> objectDesc = getObjectDescription(OBJECT_MAP[objectType], objectId)
	List<Map<String,String>> data = (List<Map<String,String>>)parameters.data
	if (!isObjectId(objectId) || data == null) return "${parameters.pageNext}"()

	dynamicPage(name: "pageBatteryDeviceTrackerData", nextPage: parameters.pageNext) {
		section {
			addToPageHistory("pageBatteryDeviceTrackerData", "Battery Tracker Data")
			sectionTopMenu()
			paragraph formatTitle(objectDesc.name)
			paragraph objectDesc.bodyL
			paragraph formatHeader(1, "Tracker Alerts")
			String pageLink = pageLinkButton("Pause Devices", "pageBatteryDeviceTrackerData", "pagePauseDevices", [objectType: objectType, objectId: objectId])
			paragraph "View and pause devices with active alerts detected by the battery tracker.", width: 10
			inputBooleanPageHelpInline("pageBatteryDeviceTrackerData", 2)
			if (isPageHelpEnabled("pageBatteryDeviceTrackerData") && !data.isEmpty()) {
				paragraph bullet(3) + "Hover over the alert icon ${getIcon(ICON_ALERT)} to view the alert details.\n" + getDescriptionPauseDevices() + bullet(3) + "To pause devices without current alerts, please use the tracker's $pageLink page.\n"
			}
			paragraph formatHeader(2, "Device Alerts")
			if (data.isEmpty())
				paragraph "No devices have triggered alerts."
			else {
				data.sort { a, b -> a[REPORT_NAME].toLowerCase() <=> b[REPORT_NAME].toLowerCase() }
				sectionBatteryDeviceTrackerData(objectType, objectId, data, "pageBatteryDeviceTrackerData")
			}
			sectionFooter("pageBatteryDeviceTrackerData")
		}
	}
}
def sectionBatteryDeviceTrackerData(String objectType, String objectId, List<Map<String,String>> data, String page, boolean pauseOnly = false) {
	Map<String,Map<String,String>> devicesPaused = getObjectDevicesPaused(objectId)
	boolean allPaused = data.every { devicesPaused.containsKey(it[REPORT_ID]) }
	String deviceIds = data.collect { it[REPORT_ID] }?.join(",")
	String buttonPauseAll = nameObjectButtonDevicePause(objectId, deviceIds, allPaused)
	String headerPause = inputButtonCheckbox(buttonPauseAll, allPaused, "white")
	getBatteryDeviceTrackerDataColumns(objectType, headerPause, pauseOnly).each { HashMap<String,String> columnDef ->
		if (columnDef.type == "space") { paragraph "", width: columnDef.widthHdr; return }
		String hdr = indent(gap() + fpx(12, columnDef.colName), columnDef.margin?.toInteger() ?: 10, columnDef.alignHdr)
		paragraph formatHeader(3, hdr, false), width: columnDef.widthHdr
	}
	data.each { Map<String,String> dataDevice ->
		String deviceId = dataDevice[REPORT_ID]
		String deviceName = dataDevice[REPORT_NAME]
		Map<String,String> devicePaused = devicesPaused[dataDevice.id]
		boolean isPaused = (devicePaused != null)
		if (dataDevice[REPORT_NAME_LINK] == null)
			dataDevice[REPORT_NAME_LINK] = hrefLinkDevicePage(deviceId, deviceName)
		if (dataDevice[REPORT_MESSAGE] != null)
			dataDevice[REPORT_INFO] = getTooltip("dt", getIcon(ICON_ALERT, "color:#1A77C9"),
				dataDevice[REPORT_MESSAGE].replace(deviceName, "<strong>$deviceName</strong>"))
		String pauseButtonName = nameObjectButtonDevicePause(objectId, deviceId, isPaused)
		dataDevice[REPORT_PAUSE] = inputButtonCheckbox(pauseButtonName, isPaused)
		String unpauseDate = devicePaused?.date
		dataDevice[REPORT_UNPAUSE_DATE] = !isPaused ? "" :
			pageLinkButton(unpauseDate == null ? getIcon(ICON_CALENDAR) : formatPausedDate(unpauseDate),
				page, "pagePauseDeviceUntil", [objectType: objectType, objectId: objectId, id: deviceId, name: deviceName])

		getBatteryDeviceTrackerDataColumns(objectType, headerPause, pauseOnly).each { HashMap<String,String> columnDef ->
			if (columnDef.type == "space") { paragraph "", width: columnDef.width; return }
			paragraphColumn(dataDevice[columnDef.type] ?: "", columnDef)
		}
	}
}


//-------------------------------------------------------------------------
// Page:  Global Devices
//-------------------------------------------------------------------------

def pageGlobalDevices(Map params) {
	setPageTab("pageMain", "BatteryDeviceTrackers")

	Map<String,Object> parameters = getPageParameterInputs("pageGlobalDevices", params)
	setPageParameters("pageGlobalDevices", parameters)
	String pageTab = getPageTab("pageGlobalDevices") ?: "TrackedDevices"
	"pageGlobalDevices_${pageTab}"()
}

def sectionGlobalDevices(String tabName) {
	addToPageHistory("pageGlobalDevices", "Global Devices")
	setPageTab("pageGlobalDevices", tabName)
	sectionTopMenu()
	paragraph formatTitle("Global Devices")
	String desc = "Global devices are available to use within all battery trackers.\n"
	desc += bullet(3) + "Configure them once to avoid repeated selections of the same devices.\n"
	desc += bullet(3) + "Each tracker can optionally include or exclude them from usage.\n"
	desc += bullet(3) + "(Each tracker also supports local devices configured specifically for that tracker only.)"
	paragraph indent(desc, 10)
	sectionPageMenu("pageGlobalDevices", PAGE_GLOBAL_DEVICES_TABS, true)
}
@Field public static final List<Map<String,String>> PAGE_GLOBAL_DEVICES_TABS = [
	[name: "TrackedDevices", display: "Tracked Devices"],
	[name: "PausedDevices", display: "Paused Devices"],
	[name: "NotificationDevices", display: "Notification Devices"]
]

def pageGlobalDevices_TrackedDevices() {
	dynamicPage(name: "pageGlobalDevices_TrackedDevices", nextPage: "pageMain") {
		section {
			sectionGlobalDevices("TrackedDevices")
			paragraph formatHeader(2, "Tracked Devices")
			paragraph "Tracked devices are battery devices whose battery and activity information is observed and reported."
			String descT = "The globally tracked devices are available for all battery trackers.\n"
			descT += bullet(3) + "If a device is desired for most (but not all) trackers,\n"
			descT += bullet(3) + "Add it to globally tracked devices and then pause it locally within trackers."
			paragraph descT
			inputCapability("app_devices_tracked", TITLE_DEVICES_TRACK_ALL, CAPABILITY_BATTERY, [width:GLOBAL_SCHEDULES_WIDTH])
			sectionFooter()
		}
	}
}
def pageGlobalDevices_PausedDevices() {
	dynamicPage(name: "pageGlobalDevices_PausedDevices", nextPage: "pageMain") {
		section {
			sectionGlobalDevices("PausedDevices")
			paragraph formatHeader(2, "Paused Devices")
			paragraph "Paused devices are battery devices on which to pause tracking."
			String descP = "The globally paused devices are available to pause from all battery trackers.  They might be ...\n"
			descP += bullet(3) + "Temporary pauses (e.g. aware of a low battery and no longer need notifications) or\n"
			descP += bullet(3) + "Permanent pauses (e.g. do not want tracking, but want inclusion in reports)."
			paragraph descP
			hrefPauseDevices(null, OBJECT_ID_APP, TITLE_DEVICES_PAUSED_ALL, GLOBAL_SCHEDULES_WIDTH)
			sectionFooter()
		}
	}
}
def pageGlobalDevices_NotificationDevices() {
	dynamicPage(name: "pageGlobalDevices_NotificationDevices", nextPage: "pageMain") {
		section {
			sectionGlobalDevices("NotificationDevices")
			paragraph formatHeader(2, "Notification Devices")
			paragraph "The global notification devices are available to send notifications for all battery trackers."
			inputCapability("app_devices_notification", TITLE_DEVICES_NOTIFICATIONS_ALL, CAPABILITY_NOTIFICATION, [width:GLOBAL_SCHEDULES_WIDTH])
			sectionFooter()
		}
	}
}
@Field public static final Integer GLOBAL_SCHEDULES_WIDTH = 7
@Field public static final String TITLE_DEVICES_TRACK_ALL = "Select battery devices to track"
@Field public static final String TITLE_DEVICES_PAUSED_ALL = "Select battery devices on which to pause all tracking"
@Field public static final String TITLE_DEVICES_NOTIFICATIONS_ALL = "Select notification devices for all notifications"


//-------------------------------------------------------------------------
// Page:  Global Schedule
//-------------------------------------------------------------------------

def pageGlobalSchedule(Map params) {
	dynamicPage(name: "pageGlobalSchedule", nextPage: getPageLast("pageGlobalSchedule") ?: "pageMain") {
		section {
			addToPageHistory("pageGlobalSchedule", "Global Schedule")
			sectionTopMenu()
			paragraph formatTitle("Global Schedule")
			String descAll = "The global schedule is available to use within all battery trackers.\n"
			descAll += bullet(3) + "Configure it once to avoid repeated selections of the same schedule.\n"
			descAll += bullet(3) + "Each tracker can optionally utilize the global schedule or configure a local schedule.\n"
			paragraph descAll
			paragraph formatHeader(2, "Schedule")
			sectionSchedule(OBJECT_ID_APP)
			if (isObjectScheduleSetup(OBJECT_ID_APP)) {
				backupObjectSchedule(OBJECT_ID_APP)
				sectionObjectSchedules()
			}
			sectionFooter("pageGlobalSchedule")
		}
	}
}
def sectionSchedule(String objectId) {
	boolean required = (objectId != OBJECT_ID_APP)
	input(
		name: "${objectId}_checkTime",
		title: "Check at ...",
		type: "time",
		required: required,
		width: 2,
		newLine: true,
		submitOnChange: true
	)
	input(
		name: "${objectId}_checkDaysOfWeek",
		title: "on ...",
		type: "enum",
		options: OPTIONS_DAYOFWEEK,
		multiple: true,
		offerAll: true,
		required: required,
		width: 5,
		newLineAfter: true,
		submitOnChange: true
	)
}
def sectionObjectSchedules() {
	List<Map<String,String>> trackers = getObjectsAndDescriptions()
	if (trackers.isEmpty()) return
	paragraph ""
	paragraph formatHeader(2, "Battery Trackers")
	boolean allGlobal = trackers.every { isObjectUsingGlobalSchedule(it.objectId) }
	String buttonNameAll = nameObjectButtonGlobalSchedule("All", allGlobal ? "local" : "global")
	String scheduleBoxAll = inputButtonCheckbox(buttonNameAll, allGlobal, "white")
	paragraph formatHeader(3, indent(gap() + fpx(12, "Use Global Schedule     ${scheduleBoxAll}"), 30, "right"), false), width: 2
	paragraph formatHeader(3, indent(gap() + fpx(12, "Battery Tracker"), 20, "left"), false), width: 10
	String infoIcon = getIcon(ICON_INFO, "color:#1A77C9")
	trackers.each { Map<String,String> tracker ->
		boolean isGlobal = isObjectUsingGlobalSchedule(tracker.objectId)
		String buttonName = nameObjectButtonGlobalSchedule(tracker.objectId, isGlobal ? "local" : "global")
		String scheduleBox = inputButtonCheckbox(buttonName, isGlobal)
		String trackerName = pageLinkButton(tracker.name, "pageGlobalSchedule", "pageBatteryDeviceTracker",
			[objectType: tracker.objectType, objectId: tracker.objectId])
		String info = getTooltip("gs", infoIcon, tracker.fullL, "bottom:140%; right:0%")
		paragraph indent(scheduleBox, 30, "right"), width: 2
		paragraph indent(trackerName + tracker.bodyS + "  " + info, 20, "left"), width: 10
	}
}


//-------------------------------------------------------------------------
// Page:  Pause Devices
//-------------------------------------------------------------------------

def hrefPauseDevices(String objectType, String objectId, String title, Integer width = 12) {
	href(
		name: "pagePauseDevices${objectId}",
		page: "pagePauseDevices",
		title: title,
		description: getDescription_PausedDevices(objectId),
		width: width,
		params: [objectType: objectType, objectId: objectId]
	)
}

def pagePauseDevices(Map params) {
	if (!isPageCurrent("pagePauseDevices")) {
		Map<String,Object> parameters = getPageParameterInputs("pagePauseDevices", params)
		if (parameters != null) {
			parameters.pageNext = getPageLast("pagePauseDevices") ?: "pageMain"
			setPageParameters("pagePauseDevices", parameters)
		}
	}
	Map<String,Object> parameters = getPageParameters("pagePauseDevices")
	String objectType = parameters.objectType
	String objectId = parameters.objectId
	Map<String,String> objectDesc = getObjectDescription(OBJECT_MAP[objectType], objectId)
	if (!isObjectId(objectId)) return "${parameters.pageNext}"()
	boolean isGlobal = (objectId == OBJECT_ID_APP)

	dynamicPage(name: "pagePauseDevices", nextPage: parameters.pageNext) {
		section {
			addToPageHistory("pagePauseDevices", "Pause Devices")
			sectionTopMenu()
			paragraph formatTitle(objectDesc.name)
			paragraph objectDesc.bodyL
			paragraph formatHeader(1, "Pause Devices")
			String descTrack = isGlobal ? "all battery trackers" : "the battery tracker"
			paragraph "Pause or unpause devices to control their inclusion in executions of $descTrack.", width: 10
			inputBooleanPageHelpInline("pagePauseDevices", 2)
			if (isPageHelpEnabled("pagePauseDevices")) {
				String descAll = getDescriptionPauseDevices()
				if (!isGlobal) {
					String pageLink = hrefButtonLink("pageGlobalDevices_PausedDevices", "Global Devices", null, "#1A77C9", 15)
					descAll += bullet(3) + "To (un)pause globally paused devices, please use the $pageLink page.\n"
				}
				paragraph descAll
			}
			paragraph formatHeader(2, "Devices")
			List<DeviceWrapper> devices = getSettingsDevicesList(objectId, "tracked", false, false)
			if (objectId != OBJECT_ID_APP)
				devices += getSettingsDevicesList(OBJECT_ID_APP, "tracked", true)
			if (devices.isEmpty()) {
				paragraph "No devices are being tracked, so nothing is available to pause."
			} else {
				List<Map<String,String>> data = []
				if (isObjectUsingAppDevices(objectId, "paused")) {
					Set<String> deviceIdsAppPaused = getObjectDevicesPaused(OBJECT_ID_APP)?.keySet() ?: []
					devices.removeAll { deviceIdsAppPaused.contains(it.id) }
				}
				devices.unique { a,b -> a.id <=> b.id }
				devices.each { data.add([(REPORT_ID): it.id, (REPORT_NAME): it.displayName]) }
				data.sort { a, b -> a[REPORT_NAME].toLowerCase() <=> b[REPORT_NAME].toLowerCase() }
				sectionBatteryDeviceTrackerData(objectType, objectId, data, "pagePauseDevices", true)
			}
			sectionFooter("pagePauseDevices")
		}
	}
}
@CompileStatic
static String getDescriptionPauseDevices() {
	return bullet(3) + "Click the Pause checkbox ${getIcon(ICON_UNCHECKED)} to pause tracking on the device. (Click the header's checkbox to pause tracking on all devices.)\n" + bullet(3) + "Click the Pause checkbox ${getIcon(ICON_CHECKED)} to resume tracking on the device. (Click the header's checkbox to resume tracking on all devices.)\n" + bullet(3) + "Click the calendar icon ${getIcon(ICON_CALENDAR)} for a paused device to schedule an unpause date, at which tracking automatically resumes on the device.\n" + bullet(3) + "Click the unpause date for a paused device to clear its unpause date, so tracking remains paused on the device (until manual unpausing via the Pause checkbox ${getIcon(ICON_CHECKED)}).\n"
}


@CompileStatic
String getDescription_PausedDevices(String objectId) {
	List<String> descDevices = []
	Map<String,Map<String,String>> devicesPaused = getObjectDevicesPaused(objectId)
	if (!devicesPaused.isEmpty()) {
		getSettingsDevicesList(objectId, "tracked", false).each { DeviceWrapper device ->
			Map<String,String> devicePaused = devicesPaused[device.id]
			if (devicePaused == null) return
			String date = devicePaused.date == null ? "" : "   [until " + formatPausedDate(devicePaused.date) + "]"
			descDevices.add(device.displayName + date)
		}
	}
	return descDevices.isEmpty() ? "Click to set" : getFormat("description", descDevices?.join("\n"))
}
@CompileStatic
static String formatPausedDate(String dt) {
	return formatDateTime(formatDateTime(dt, DATETIME_FORMAT_SETTING), DATETIME_FORMAT_PAUSE)
}


//-------------------------------------------------------------------------
// Page:  Pause Device Until
//-------------------------------------------------------------------------

def pagePauseDeviceUntil(Map params) {
	if (!isPageCurrent("pagePauseDeviceUntil")) {
		Map<String,Object> parameters = getPageParameterInputs("pagePauseDeviceUntil", params)
		if (parameters != null) {
			String untilDate = getObjectDevicePausedDate((String)parameters.objectId, (String)parameters.id)
			if (untilDate != null && settings."app_pauseDeviceUntil_${parameters.id}" == null)
				app.updateSetting("app_pauseDeviceUntil_${parameters.id}", [type: "date", value: untilDate])
			else if (untilDate == null)
				app.removeSetting("app_pauseDeviceUntil_${parameters.id}")
			parameters.pageNext = getPageLast("pagePauseDeviceUntil") ?: "pageMain"
			setPageParameters("pagePauseDeviceUntil", parameters)
		}
	}
	Map<String,Object> parameters = getPageParameters("pagePauseDeviceUntil")
	Map<String,String> deviceInfo = [id: (String)parameters?.id, name: (String)parameters?.name]
	String objectId = parameters.objectId
	Map<String,String> objectDesc = getObjectDescription(OBJECT_MAP[parameters.objectType], objectId)
	if (!isObjectId(objectId) || deviceInfo == null) return "${parameters.pageNext}"()

	dynamicPage(name: "pagePauseDeviceUntil", nextPage: parameters.pageNext) {
		section {
			addToPageHistory("pagePauseDeviceUntil", "N/A")
			sectionTopMenu()
			paragraph formatTitle(objectDesc.name)
			paragraph objectDesc.bodyL
			paragraph formatHeader(1, "Paused Device - ${deviceInfo.name}")
			String descAll = "Control when a paused device automatically unpauses for future tracker executions.\n"
			descAll += bullet(3) + "Set a specific date for resumed tracking on the device.\n"
			descAll += bullet(3) + "Clear, or leave unset, to prevent unpausing automatically at a future date.\n"
			paragraph descAll
			paragraph formatHeader(2, "Resume (Unpause) Date")
			String untilDate = getObjectDevicePausedDate(objectId, deviceInfo.id)
			String untilDateNew = settings."app_pauseDeviceUntil_${deviceInfo.id}"
			inputDate("app_pauseDeviceUntil_${deviceInfo.id}", fpx(15, TITLE_UNPAUSE_DATE), [width:6])
			if (untilDateNew != null && untilDateNew <= getDateToday(DATETIME_FORMAT_SETTING)) {
				app.removeSetting("app_pauseDeviceUntil_${deviceInfo.id}")
				paragraph "Please select a date that is tomorrow or later."
			} else {
				if ((untilDate == null && untilDateNew != null) ||
					(untilDate != null && untilDateNew != null && untilDate != untilDateNew))
					addObjectDevicePausedDate(objectId, deviceInfo.id, untilDateNew)
				else if (untilDate != null && untilDateNew == null)
					deleteObjectDevicePausedDate(objectId, deviceInfo.id)
			}
			sectionFooter()
		}
	}
}
@Field public static final String TITLE_UNPAUSE_DATE = "Enter the date on which to resume tracking on this device"


//-------------------------------------------------------------------------
// Page:  Manual Checks
//-------------------------------------------------------------------------

def pageManualChecks(Map params) {
	String pageNext = getPageLast("pageManualChecks") ?: "pageMain"
	List<String> objectTypes = (List<String>)params?.objectTypes ?: [OBJECT_LAST, OBJECT_LOW, OBJECT_NEW, OBJECT_OLD]
	objectTypes?.each { String objectType ->
		runBatteryTrackersManually(objectType)
	}
	"$pageNext"()
}


//-------------------------------------------------------------------------
// Page:  Request Confirm/Cancel
//-------------------------------------------------------------------------

def sectionCopy(HashMap<String,String> params) {
	params.request = "copy"
	sectionRequestConfirm(params)
}
def sectionDelete(HashMap<String,String> params) {
	params.request = "delete"
	sectionRequestConfirm(params)
}
def sectionRenumber(HashMap params) {
	params.request = "renumber"
	params.descExtra = "s, in the displayed order below, from 1 to N"
	params.width = 6
	sectionRequestConfirm(params)
}
def sectionRequestConfirm(HashMap params) {
	HashMap<String,String> om = OBJECT_MAP[(String)params.objectType]
	params.objectMap = om
	String requestUC = ((String)params.request).capitalize()
	String nameRoot = removeSpaces(om.descC)
	String extra = params.descExtra ?: ""
	href(
		name: "page${nameRoot}${requestUC}",
		page: "pageRequestConfirm",
		title: requestUC,
		description: "Click to ${params.request} the tracker${extra}",
		width: params.width ?: 12,
		params: params
	)
}

def pageRequestConfirm(params) {
	if (state.configure == null)
		state.configure = [:]
	if (getStateConfigureValueMap("request") == null && params != null) {
		HashMap<String,String> om = ((Map)params).objectMap as HashMap<String,String>
		Map request = [type: (String)((Map)params).request]
		request.typeNoun = (request.type == "delete" ? "deletion" : request.type)
		request.typeNounUC = capitalizeWords(request.typeNoun)
		request.objectMap = om
		request.objectId = ((Map)params).objectId
		String objectNum = parseObjectIdNum(request.objectId).toString()
		request.objectDesc = "${om.descC.replace('-', ' ')} #${objectNum}"
		setStateConfigureValue("request", request)
	}
	Map request = getStateConfigureValueMap("request")
	if (request == null) return pageBatteryDeviceTracker(null)
	dynamicPage(name: "pageRequestConfirm", nextPage: "pageBatteryDeviceTracker") {
		section(formatTitle("Device Tracker ${request.typeNounUC}")) {
			paragraph "Confirm ${request.typeNoun} of <b>${request.objectDesc}</b>"
			sectionRequestConfirmCancel((String)request.type)
			sectionRequestConfirmCancel("cancel")
			sectionFooter()
		}
	}
}
def sectionRequestConfirmCancel(String requestType) {
	paragraph ""
	href(
		name: "pageRequest_${requestType}",
		page: (requestType == "cancel") ? "pageBatteryDeviceTracker" : "pageBatteryDeviceTrackers",
		title: "<b>${capitalizeWords(requestType)}</b>",
		description: hrefDescription("Click to $requestType and return"),
		params: [requestAction: requestType]
	)
}


@CompileStatic
boolean tryCompleteRequestAction(params) {
	if (params == null || ((String)((Map)params).requestAction ?: "cancel") == "cancel") return false

	Map request = getStateConfigureValueMap("request")
	HashMap<String,String> om = (Map)request?.objectMap as HashMap<String,String>
	String objectId = (String)request?.objectId
	if (om != null) {
		switch ((String)request?.type) {
			case "copy": copyObject(om, objectId); break
			case "delete": deleteObject(om, objectId); break
			case "renumber": renumberObjects(om); break
		}
	}
	return true
}


//-------------------------------------------------------------------------
// Page:  Report Results
//-------------------------------------------------------------------------

@CompileStatic
static List<LinkedHashMap<String,String>> getReportColumns() {
	return [
		[type: REPORT_NAME, colName: "Name", width: "3", sort: "string", alignHdr: "left", alignData: "left",
		 descName: "Device Name", descDetail: "Display name of the device"],
		[type: REPORT_TYPE, colName: "Type", width: "1", sort: "string", alignHdr: "left", alignData: "left",
		 descName: "Battery Type", descDetail: "Type of battery in the device"],
		[type: REPORT_PERCENTAGE, colName: "%", width: "1", sort: "intIncrease",
		 alignHdr: "center", marginHdr: "30", alignData: "right", marginData: "35",
		 descName: "Battery Percentage", descDetail: "Current battery percentage"],
		[type: REPORT_DATE, colName: "Chg Date", width: "1", sort: "string", alignHdr: "center", alignData: "center",
		 descName: "Battery Date", descDetail: "Change date of the current battery"],
		[type: REPORT_AGE_CURRENT, colName: "Age Now", width: "1", sort: "intDecrease",
		 alignHdr: "center", marginHdr: "20", alignData: "right", marginData: "20",
		 descName: "Battery Age (Current)", descDetail: "Current age of the current battery"],
		[type: REPORT_TIME_TO_AVG, colName: "Tm To Avg", width: "1", sort: "intIncrease",
		 alignHdr: "center", marginHdr: "15", alignData: "right", marginData: "15",
		 descName: "Time To Average Age", descDetail: "Time to reach the average battery age of the device"],
		[type: REPORT_AGE_AVG, colName: "Age Avg (Last 4 Ages)", width: "2", sort: "intDecrease",
		 alignHdr: "left", alignData: "left",
		 descName: "Battery Age (Average)", descDetail: "Average age of batteries in the device (and last 4 ages)"],
		[type: REPORT_LAST_ACTIVITY, colName: "Last Activity", width: "2", sort: "intIncrease",
		 alignHdr: "left", alignData: "left",
		 descName: "Last Activity", descDetail: "Time of last activity reported from the device"]
	]
}


def pageMain_BatteryDeviceReport() {
	setPageTab("pageMain", "BatteryDeviceReport")
	String lifeUnits = (String)settings.reportLifeUnits ?: UNIT_DAYS
	String dwk = (lifeUnits == UNIT_DAYS) ? "d" : "wk"
	List<HashMap<String,HashMap<String,String>>> devicesData = []
	getSettingsDevicesAllList("tracked", !(settings.reportPaused ?: false))?.each { DeviceWrapper device ->
		String blank = "--"
		String dateBig = "9999/99/99"
		String stringBig = "zzzzzzzzzzzzzzz"
		Long intBig = 99999999999999
		Long intSmall = -99999999999999
		Long ageAvgMs = getBatteryLifeAverage(device) ?: intSmall
		Long ageNowMs = getBatteryAgeMs(device, (new Date()).getTime()) ?: intSmall
		Long timeToAgeAvgMs = (ageAvgMs != intSmall && ageNowMs != intSmall) ? ageAvgMs - ageNowMs : intBig
		Date lastActivity = device.getLastActivity()
		Long percentage = (Long)device.currentValue("battery")
		if (percentage == null) percentage = intBig
		HashMap<String,HashMap<String,String>> dataBattery = getDeviceData(device)
		String life = !isDeviceDataValid(DATA_LIFE, dataBattery[DATA_LIFE]?.value) ? null :
			dataBattery[DATA_LIFE]?.value?.split(",")?.take(4)?.collect{
				getBatteryAge(daysToMs(it.trim().toDouble()), lifeUnits) }?.join(", ")

		HashMap<String,HashMap<String,String>> dataDevice = [
			(REPORT_NAME): [sort: device.displayName, display: hrefLinkDevicePage(device.id, device.displayName, true)],
			(REPORT_TYPE): [sort: dataBattery[DATA_TYPE]?.value ?: stringBig, display: dataBattery[DATA_TYPE]?.value ?: blank],
			(REPORT_PERCENTAGE): [sort: percentage.toString(),
				display: (percentage == intBig) ? blank : percentage + "%"],
			(REPORT_DATE): [sort: dataBattery[DATA_DATE]?.value ?: dateBig, display: dataBattery[DATA_DATE]?.value ?: blank],
			(REPORT_AGE_CURRENT): [sort: ageNowMs.toString(),
				display: (ageNowMs == intSmall) ? blank : getBatteryAge(ageNowMs, lifeUnits) + " $dwk"],
			(REPORT_TIME_TO_AVG): [sort: timeToAgeAvgMs.toString(),
				display: (timeToAgeAvgMs == intBig) ? blank : getBatteryAge(timeToAgeAvgMs, lifeUnits) + " $dwk"],
			(REPORT_AGE_AVG): [sort: ageAvgMs.toString(),
				display: (ageAvgMs == intSmall) ? blank : getBatteryAge(ageAvgMs, lifeUnits) + " $dwk ($life $dwk)"],
			(REPORT_LAST_ACTIVITY): [sort: (lastActivity?.getTime() ?: intBig).toString(),
				display: (lastActivity == null) ? blank : formatDateTime(lastActivity)]
		]
		devicesData.add(dataDevice)
	}

	dynamicPage(name: "pageMain_BatteryDeviceReport", install: true, uninstall: true) {
		section(titleMainMenu()) {
			sectionMainMenu()
			paragraph formatHeader(2, "Battery Report")
			paragraph "Display battery and activity details of devices configured in the Global Devices and Battery Trackers sections."
			if (devicesData == null || devicesData.isEmpty()) {
				paragraph "No devices to report."
			} else {
				paragraph formatHeader(3, "Settings")
				inputEnum("reportSortKey", fpx(14, "Sort the report results by ..."),
					getPageReport_Report_SortOptions(), [defaultValue:REPORT_PERCENTAGE, required:true, width:3, EOL:true])
				inputEnum("reportLifeUnits", fpx(14, "Display ages and time in ..."),
					[UNIT_DAYS, UNIT_WEEKS], [defaultValue:UNIT_DAYS, required:true, width:3, EOL:true])
				inputBoolean("reportPaused", fpx(14, "Include paused devices in the report"))
				paragraph formatHeader(3, "Results")
				printReportLegend()
				String sortKey = (String)settings.reportSortKey ?: REPORT_PERCENTAGE

				getReportColumns().each { HashMap<String,String> columnDef ->
					String data = ""
					int rowNum = 0; int rowNumHeader = 21
					sortDeviceReport(devicesData, sortKey).each { HashMap<String,HashMap<String,String>> deviceData ->
						rowNum = (rowNum == rowNumHeader) ? 1 : rowNum + 1
						data += formatHeaderName(columnDef, rowNum, rowNumHeader)
						data += formatDeviceDatum(deviceData[columnDef.type].display, columnDef, sortKey, rowNum)
					}
					paragraph lpx(28, data), width: columnDef.width
				}
			}
			sectionFooter()
		}
	}
}
@CompileStatic
static List<HashMap<String,HashMap<String,String>>> sortDeviceReport(
		List<HashMap<String,HashMap<String,String>>> devicesData, String sortKey) {
	devicesData.sort { a, b ->
		a[REPORT_NAME].sort <=> b[REPORT_NAME].sort
	}
	switch (getReportColumns().find { it.type == sortKey }.sort) {
		case "string":
			return devicesData.sort { a, b -> a[sortKey].sort <=> b[sortKey].sort }
		case "intDecrease":
			return devicesData.sort { a, b -> b[sortKey].sort.toLong() <=> a[sortKey].sort.toLong() }
		case "intIncrease":
			return devicesData.sort { a, b -> a[sortKey].sort.toLong() <=> b[sortKey].sort.toLong() }
	}
	return devicesData
}
def printReportLegend() {
	String colNames = formatLegendName("Column Name") + lpx(8)
	String descNames = formatLegendName("Sort Name") + lpx(8)
	String details = formatLegendName("Description") + lpx(8)
	getReportColumns().each { HashMap<String,String> columnDef ->
		colNames += indent(fpx(12, columnDef.colName), 10)
		descNames += indent(fpx(12, columnDef.descName), 10)
		details += indent(fpx(12, columnDef.descDetail), 10)
	}
	paragraph colNames, width: 3
	paragraph descNames, width: 2
	paragraph details, width: 7
}
static String formatLegendName(String text) {
	return formatHeader(4, indent(gap() + fpx(12, text), 10, "left"), false)
}
static String formatHeaderName(HashMap<String,String> columnDef, int rowNum, int rowNumHeader) {
	if (rowNum % rowNumHeader != 1) return ""
	return formatHeader(4, indent(gap() + fpx(12, columnDef.colName),
		columnDef.marginHdr?.toInteger() ?: 10, columnDef.alignHdr), false)
}
static String formatDeviceDatum(String value, HashMap<String,String> columnDef, String sortKey, int rowNum) {
	String valueFmt = indent(fpx(12, value, (columnDef.type == sortKey) ? "firebrick" : "black"),
		columnDef.marginData?.toInteger() ?: 10, columnDef.alignData)
	if (rowNum % 2 == 0)
		return getFormat("highlight", valueFmt, "#CFFAFC")
	else return valueFmt
}
static List<Map<String,String>> getPageReport_Report_SortOptions() {
	return getReportColumns().collect { LinkedHashMap<String,String> columnDef ->
		[(columnDef.type): columnDef.descName]
	}.sort { a, b ->
		a.entrySet().iterator().next().getValue() <=> b.entrySet().iterator().next().getValue()
	}
}


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
static String getIcon(String iconClass, String style) { return """<i class="$iconClass" style="$style"></i>""" }
@CompileStatic
static String getIconMaterialIcons(String icon) { return "<i class='material-icons'>$icon</i>" }
@CompileStatic
static def getImage(String type, Integer height = 40, Integer width = 15) {	// Modified from @Stephack Code / BPTWorld
	String loc = "<img src=https://github.com/jdc72/Hubitat/tree/master/resources/images/"
	if (type == "Blank") return "${loc}Hubitat_blank.png height=${height} width=${width}>"
}


@CompileStatic
static String bullet(Integer spaces) { return "&bull;" + String.format("%" + spaces + "s", " ") }


@CompileStatic
static String capitalizeWords(String text, String joinText = " ") {
	return text.split(/\W/).collect { String it -> it.capitalize() }.join(joinText)
}
@CompileStatic
static String removeSpaces(String text) { return text.replaceAll("\\s","") }
@CompileStatic
static List<String> removeSpaces(List<String> list) { return list.collect { removeSpaces(it) } }


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
static String pxTab(Integer textSize, String text, String color = (String)null) {
	return lpx(textSize, "\t" + fpx(textSize, text, color))
}
@CompileStatic
static String px(Integer textSize, String text = "\n", String color = (String)null) {
	return lpx(textSize, fpx(textSize, text, color))
}


@CompileStatic
static GString gap() { return (GString)getImage("Blank", 30, 1) }
@CompileStatic
static String indent(String text, Integer indentSize = 0, String indentSide = "left") {
	return "<div style='text-align:${indentSide};margin-${indentSide}:${indentSize}px'>${text}</div>"
}


@CompileStatic
static String hrefDescription(String desc, Integer textSize = 6) {
	return desc.size() > 0 ? lpx(textSize) + getFormat("description", desc) : HREF_INSTRUCTIONS_SET
}
@CompileStatic
static String hrefTitle(String desc) { return desc.replaceAll('[\n\r]$', "") }


@CompileStatic
static String buttonIcon(String name) { // Copied from @jtp10181
	return "<span class='p-button-icon p-button-icon-left pi " + name + "' data-pc-section='icon'></span>"
}

@CompileStatic
static String getStyleMenu() {
	String style = ".menu {display:inline-block; width:100%; box-sizing:border-box; background:white;} " +
		".menu-secondary {border:1px solid LightGray; border-style:solid none} " +
		".nav > li {display:inline-block; padding:0; margin:0px 10px;} .nav > li > button {padding:9px 10px} " +
		".nav button {background:none; border:none; padding:0; font-size:15px; font-family:sans-serif; color:#49535c; text-decoration:none; cursor:pointer;} " +
		".nav > li.primary.requiredTodo > button {color:red;} " +
		".nav > li.secondary {margin:0px 5px;} .nav > li.secondary.first {margin:0px 10px;}" +
		".nav > li.secondary > button {color:grey; font-size:14px;} " +
		".nav > li.active.primary > button, .nav > li.active.primary > button:focus {border-bottom:2px solid green; padding-bottom:7px} " +
		".nav > li.active.secondary > button, .nav > li.active.secondary > button:focus {border-bottom:2px solid grey; padding-bottom:7px} " +
		".nav > li.active > button:hover, .nav > li > button:hover {border-bottom:3px solid #1A77C9; padding-bottom:6px} "
	return "<style>${style}</style>"
}


@CompileStatic
static String getTooltip(String id, String text, String tipText, String style = "bottom:140%; left:0%") {
	return getStyleTooltip(id, style) + "<span class='tooltip-${id}'>${text}<span class='tooltipText'>${tipText}</span></span>"
}
@CompileStatic
static String getStyleTooltip(String id, String style) {
	return "<style>.tooltip-${id} {display:inline-block; position:relative} .tooltip-${id} .tooltipText{display:none; position:absolute; width:max-content; $style; z-index:999; border-radius:6px; background-color:#FFFFFFFF; padding:15px 15px; box-shadow:0px 0px 10px 5px rgba(0,0,0,0.5);} .tooltip-${id}:hover .tooltipText{display:inline-block}</style>"
}
@CompileStatic
static String getIconListSeparator(String color = "currentColor", Integer size = 14) {
	return "<li class='p-menuitem-separator mx-0 sm:mx-1' data-pc-section='separator'><svg color='$color' width='$size' height='$size' viewBox='0 0 $size $size' margin='0 0 0 0' fill='none' xmlns='http://www.w3.org/2000/svg' class='p-icon' aria-hidden='true' data-pc-section='separatoricon'><path d='M4.38708 13C4.28408 13.0005 4.18203 12.9804 4.08691 12.9409C3.99178 12.9014 3.9055 12.8433 3.83313 12.7701C3.68634 12.6231 3.60388 12.4238 3.60388 12.2161C3.60388 12.0084 3.68634 11.8091 3.83313 11.6622L8.50507 6.99022L3.83313 2.31827C3.69467 2.16968 3.61928 1.97313 3.62287 1.77005C3.62645 1.56698 3.70872 1.37322 3.85234 1.22959C3.99596 1.08597 4.18972 1.00371 4.3928 1.00012C4.59588 0.996539 4.79242 1.07192 4.94102 1.21039L10.1669 6.43628C10.3137 6.58325 10.3962 6.78249 10.3962 6.99022C10.3962 7.19795 10.3137 7.39718 10.1669 7.54416L4.94102 12.7701C4.86865 12.8433 4.78237 12.9014 4.68724 12.9409C4.59212 12.9804 4.49007 13.0005 4.38708 13Z' fill='currentColor'></path></svg></li>"
}


//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Buttons
//-------------------------------------------------------------------------

def inputButtonLink(String buttonName, String linkText, String titleText, String classes, String style, Integer width) {
	paragraph inputButtonLink(buttonName, linkText, titleText, classes, style), width: width
}
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


def inputButtonDeviceData(String dataStore, String action, Map params = null) {
	String dataType = (String)params?.dataType ?: "All"
	String dataValue = (String)params?.dataValue ?: "NA"
	input(
		name: nameButtonDeviceData(dataStore, dataType, dataValue, action, (String)params?.deviceId),
		title: action,
		type: "button",
		textColor: params?.textColor ?: "black",
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false
	)
}


def inputButtonsSwapOrder(String objectType, String objectId, int size, int idx) {
	if (size <= 1) return
	String directionLeft = (idx < size - 1) ? DIRECTION_INCREMENT : DIRECTION_DECREMENT
	inputButtonSwapDirection(objectType, objectId, directionLeft, "Left")
	String directionRight = (idx > 0) ? DIRECTION_DECREMENT : DIRECTION_INCREMENT
	inputButtonSwapDirection(objectType, objectId, directionRight, "Right")
}
def inputButtonSwapDirection(String objectType, String objectId, String direction, String side) {
	input(
		name: nameObjectButtonSwapOrder(objectType, objectId, direction, side),
		title: (direction == DIRECTION_INCREMENT) ? "↓↓↓" : "↑↑↑",
		type: "button",
		width: 1
	)
}


def appButtonHandler(String buttonName) {
	if (pageLinkButtonHandler(buttonName) ||
		inputBooleanButtonHandler(buttonName) ||
		inputBooleanGroupButtonHandler(buttonName)) {
		// nothing more to do
	} else if (buttonName == "data_devices_battery_none") {
		app.removeSetting("data_devices_battery")
	} else if (buttonName.startsWith("schedule|")) {
		HashMap<String,String> params = parseObjectButtonGlobalScheduleName(buttonName)
		modifyObjectSchedule((params.objectId == "All") ? getObjectIdsAll() : [params.objectId], params.schedule)
	} else if (buttonName.startsWith("swapOrder|")) {
		HashMap<String,String> params = parseObjectButtonSwapOrderName(buttonName)
		HashMap<String,String> om = OBJECT_MAP[params.objectType]
		swapObjectsOrder(om, params.objectId, params.direction)
	} else if (buttonName.startsWith("devicePause|")) {
		HashMap<String,String> params = parseObjectButtonDevicePauseName(buttonName)
		List<String> deviceIds =  params.deviceId.split(",")
		deviceIds?.each { String deviceId ->
			if (params.action == "pause") {
				if (!isObjectDevicePaused(params.objectId, deviceId))
					addToObjectDevicesPaused(params.objectId, deviceId)
			} else if (params.action == "unpause")
				deleteFromObjectDevicesPaused(params.objectId, deviceId)
		}
	} else if (buttonName.startsWith("deviceDataName|")) {
		List<Number> indexes = buttonName.findIndexValues { it == '|' }
		String key = buttonName.substring((int)indexes[0] + 1)
		setStateConfigureValue(key, (String)settings."${key}_new")
	} else if (buttonName.startsWith("deviceData|")) {
		HashMap<String,String> params = parseButtonDeviceDataName(buttonName)
		switch (params.action) {
			case "Clear Cache":
				deleteDataStore(params.dataStore)
				break
			case "Delete Pre-Migration Values":
				deletePreMigrationDeviceDataAll(params.dataStore)
				break
			case "Exclude":
			case "Include":
				excludeIncludeDeviceDataInPending(params.action, params.dataStore, params.deviceId)
				break
			case "Exclude All Devices":
			case "Include All Devices":
				excludeIncludeDeviceDataAllInPending(params.action, params.dataStore)
				break
			case "Migrate All Values":
				migrateDeviceDataAllToPending(params.dataStore)
				break
			case "Remove":
				removeDeviceDataFromPending(params.dataStore, params.dataType)
				break
			case "Remove All Values":
				removeDeviceDataAllFromPending(params.dataStore)
				break
			case "Reset":
				resetDeviceDataInPending(params.dataStore, params.dataType)
				break
			case "Reset Pending Values":
				resetDeviceDataAllInPending(params.dataStore)
				break
			case "Restore":
				restoreDeviceDataToPending(params.dataStore, params.deviceId)
				break
			case "Restore Previous Values":
				restoreDeviceDataAllToPending(params.dataStore)
				break
			case "Run Checks":
				runBatteryTrackersManually(OBJECT_NEW)
				break
			case "Submit":
				submitDeviceDataInPending(params.dataStore, params.deviceId)
				break
			case "Submit Pending Values":
				submitDeviceDataInPending(params.dataStore)
				break
			case "Update":
				String dataValue = ((String)params.dataValue == "NA") ? null : params.dataValue
				updateDeviceDataInPending(params.dataStore, params.dataType, dataValue)
				break
			case "Update All Values":
				updateDeviceDataAllInPending(params.dataStore)
				break
		}
	}
}


//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Boolean
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

def inputBooleanState(String booleanName, String linkText, int width = 12) {
	boolean valueNow = isInputBooleanSet(booleanName)
	String buttonName = "booleanBtn|$booleanName|" + (valueNow ? "false" : "true")
	inputButtonLink(buttonName, linkText, null, null, (String)null, width)
}
boolean isInputBooleanSet(String booleanName) {
	return state.containsKey("boolean_${booleanName}")
}
boolean inputBooleanButtonHandler(String buttonName) {
	if (!buttonName.startsWith("booleanBtn|")) return false
	List<String> btnList = buttonName.split("\\|")
	if (btnList[2] == "false")
		state.remove("boolean_${btnList[1]}")
	else
		state."boolean_${btnList[1]}" = true
	return true
}

def inputBooleanState(String booleanGroupName, String booleanName, String linkText, int width = 12) {
	inputBooleanState(booleanGroupName, booleanName, linkText, null, null, width)
}
def inputBooleanState(String booleanGroupName, String booleanName, String linkText, String classes, String style, int width = 12) {
	boolean valueNow = isInputBooleanSet(booleanGroupName, booleanName)
	String buttonName = "booleanGrpBtn|$booleanGroupName|$booleanName|" + (valueNow ? "false" : "true")
	inputButtonLink(buttonName, linkText, null, classes, style, width)
}
boolean isInputBooleanSet(String booleanGroupName, String booleanName) {
	return ((HashSet<String>)state."$booleanGroupName")?.contains(booleanName)
}
boolean inputBooleanGroupButtonHandler(String buttonName) {
	if (!buttonName.startsWith("booleanGrpBtn|")) return false
	List<String> btnList = buttonName.split("\\|")
	if (btnList[3] == "false") {
		((ArrayList<String>)state."${btnList[1]}")?.removeElement(btnList[2])
		if (((ArrayList)state."${btnList[1]}").isEmpty()) state.remove(btnList[1])
	} else {
		if (state."${btnList[1]}" == null)
			state."${btnList[1]}" = (ArrayList<String>)[btnList[2]]
		else ((ArrayList<String>)state."${btnList[1]}").add(btnList[2])
	}
	return true
}

def inputBooleanCheckbox(String booleanName, String linkText, boolean isChecked, Integer szFont = 14, int width = 12) {
	String text = fpx(szFont, linkText) + "  " + getIcon(isChecked ? ICON_CHECKED : ICON_UNCHECKED)
	inputBooleanState(booleanName, text, width)
}

def inputBooleanPageHelpInline(String page, Integer width = 12) {
	boolean isHelpOn = isInputBooleanSet("pageHelpInline", page)
	String color = (isHelpOn ? "#1A77C9" : "grey")
	String textHover = "Click to <strong>" + (isHelpOn ? "hide</strong> the" : "show</strong>") + " inline help"
	String textDisplay = "<div class='flex' style='color:$color'>" + fpx(14, "Help  ") +
		getIcon((isHelpOn ? "he-help_2" : "he-help_1") + " text-xl align-bottom") + "</div>"
	String text = getTooltip("Help", textDisplay, textHover, "top:140%; right:0%")
	inputBooleanState("pageHelpInline", page, text, "pull-right", "margin-right:5px'", width)
}
boolean isPageHelpEnabled(String page) {
	return isInputBooleanSet("pageHelpInline", page)
}

//-------------------------------------------------------------------------
// Page:  ALL:  Input Helpers:  Etc.
//-------------------------------------------------------------------------

def inputBatteryPercentage(String name, String title, String range, Map params = null) {
	input(
		name: name,
		title: title,
		description: "$range (leave unset to skip)",
		type: "number",
		range: range,
		required: params?.required ?: false,
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false,
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

def inputDate(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "date",
		width: params?.width ?: 12,
		required: params?.required ?: false,
		submitOnChange: true
	)
}

def inputEnum(String name, String title, options, Map params = null) {
	input(
		name: name,
		title: title,
		type: "enum",
		options: options,
		multiple: params?.multiple ?: false,
		offerAll: params?.multiple ?: false,
		defaultValue: params?.defaultValue,
		required: params?.required ?: false,
		width: params?.width ?: 12,
		newLineAfter: params?.EOL ?: false,
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

def inputTimeInterval(String name, String title, Map params = null) {
	input(
		name: name,
		title: title,
		type: "number",
		description: "",
		defaultValue: params?.defaultValue,
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
static String hrefButton(String buttonName, String href) { // Copied from @jtp10181
	String output = ""
	output += """<button onClick="location.href='""" + href + """'" class="p-button p-component mr-2 mb-2" type="button" aria-label="hrefButton" data-pc-name="button" data-pc-section="root" data-pd-ripple="true">"""
	output += buttonName
	output += """<span role="presentation" aria-hidden="true" data-p-ink="true" data-p-ink-active="false" class="p-ink" data-pc-name="ripple" data-pc-section="root"></span></button>"""
	return output
}


@CompileStatic
static String hrefButtonLink(String pageName, String linkText, params, String color, Integer size) {
	String style = "cursor:pointer;font-size:${size}px;color:$color;background-color:unset;border:none;padding:0;text-align:left;"
	return hrefButtonLink(pageName, linkText, params, style)
}
@CompileStatic
static String hrefButtonLink(String pageName, String linkText, params = null, String style = null) {
	String uuParams = (params instanceof Map) ? URLEncoder.encode("${new JsonBuilder(params)}", "UTF-8") : ""
	def pageNum = (params instanceof Map) ? params.i : params
	String cssStyle = (style == null) ? "" : "style='$style'"
	//<button type=\"button\" name=\"_action_href_pageGlobalDevicesMenu|pageGlobalDevices|1\"></button>
	return """<input type="hidden" name="params_for_action_href_name|${pageName}|${pageNum}" value="${uuParams}"><button type="button" name="_action_href_name|${pageName}|${pageNum}" $cssStyle>${linkText}</button>"""
}

@CompileStatic
static String hrefLink(String href, String linkText, String color = null) {
	return """<a href="$href"><span style="color:$color">$linkText</span></a>"""
}
@CompileStatic
static String hrefLinkExternal(String href, String linkText, boolean small = false) {
	return linkText + "  " + """<a href="$href" target="_blank">${getIcon(ICON_LINK_EXT, small)}</a>"""
}
@CompileStatic
static String hrefLinkDevicePage(String deviceId, String deviceName, boolean small = false) {
	return hrefLinkExternal("/device/edit/$deviceId", deviceName, small)
}
Map<String,Object> getHrefLinkParameters() { // Modified from @jtp10181
	String queryString = settings.hubitatQueryString
	if (queryString == null) return null
	Map<String,Object> parameters = [:]
	List<Map<String,Object>> queryParams = (List<Map<String,Object>>)(new JsonSlurper().parseText(queryString))
	queryParams?.each { parameters[(String)it.name] = it.value }
	app.removeSetting("hubitatQueryString")
	return parameters.isEmpty() ? null : parameters
}


@CompileStatic
static String pageLinkButton(String linkText, String pageFrom, String pageTo, Map params = null,
	String color = "#1A77C9", Integer szFont = 15) {
	String parameters = (params != null && !params.isEmpty()) ?
		URLEncoder.encode("${new JsonBuilder(params)}", "UTF-8") : ""
	String buttonName = "btnPage|$pageFrom|$pageTo|$parameters|"
	return inputButtonLink(buttonName, linkText, null, color, szFont)
}
boolean pageLinkButtonHandler(String buttonName) {
	if (!buttonName.startsWith("btnPage|")) return false
	List<String> btnList = buttonName.split("\\|")
	state.pageLink = [pageFrom: btnList[1], pageTo: btnList[2], parameters: btnList[3]]
	return true
}
void deletePageLink() { state.remove("pageLink") }
String getPageLinkTo(String pageFrom) {
	Map<String,Object> page = (Map<String,Object>)state.pageLink
	if (page != null && pageFrom == (String)page.pageFrom) {
		return "<button type='button' id='${page.pageTo}' name='_action_href_name|${page.pageTo}|1' hidden>NA</button>" +
			"<script>{document.getElementById('${page.pageTo}').click()}</script>"
	}
	return ""
}
Map<String,Object> getPageLinkToParameters(String pageTo) {
	Map<String,Object> parameters = null
	Map<String,Object> page = (Map<String,Object>)state.pageLink
	if (page != null && pageTo == (String)page.pageTo) {
		if (page.parameters != null) {
			String paramsJSON = (String)page.parameters
			parameters = (paramsJSON == null || paramsJSON.isEmpty()) ? null :
				(Map<String,Object>)(new JsonSlurper()).parseText(URLDecoder.decode(paramsJSON, "UTF-8"))
		}
		state.remove("pageLink")
	}
	return parameters
}


//-------------------------------------------------------------------------
// Page:  ALL:  Navigation Helpers
//-------------------------------------------------------------------------

void addToPageHistory(String pageName, String displayName) {
	Map<String,String> page = [name: pageName, display: displayName]
	if (state.pageHistory == null)
		state.pageHistory = (List<Map<String,String>>)[page]
	else {
		List<Map<String,String>> pageHistory = (List<Map<String,String>>)state.pageHistory
		int idx = pageHistory.findIndexOf { it.name == pageName }
		if (idx >= 0)
			((List<Map<String,String>>)state.pageHistory).subList(idx, pageHistory.size()).clear()
		((List<Map<String,String>>)state.pageHistory).add(page)
	}
}
void deletePageHistory() { state.remove("pageHistory") }
List<Map<String,String>> getPageHistory() { return (List<Map<String,String>>)state.pageHistory }
String getPageLast(String pageCurrent) {
	List<Map<String,String>> pageHistory = getPageHistory() ?: []
	int idx = pageHistory.findLastIndexOf { it.name == pageCurrent }
	if (idx > 0)
		return pageHistory[idx-1].name
	else if (pageHistory.size() > 0 && idx < 0)
		return pageHistory.last().name
	return null
}
CompileStatic
boolean isPageCurrent(String pageName) {
	return (getPageHistory()?.last()?.name == pageName)
}

@CompileStatic
Map<String,Object> getPageParameterInputs(String page, Map params = null) {
	Map<String,Object> parameters = [:]
	params?.each { parameters[(String)it.key] = it.value }
	getHrefLinkParameters()?.each { parameters[(String)it.key] = it.value }
	getPageLinkToParameters(page)?.each { parameters[(String)it.key] = it.value }
	return parameters.isEmpty() ? null : parameters
}

Map<String,Object> getPageParameters(String pageName) {
	return (Map<String,Object>)((Map<String,Map<String,Object>>)state.pageParameters)?."$pageName" ?: [:]
}
void setPageParameters(String pageName, Map<String,Object> parameters) {
	if (state.pageParameters == null) state.pageParameters = (Map<String,Map<String,Object>>)[(pageName): parameters]
	else ((Map<String,Map<String,Object>>)state.pageParameters)[pageName] = parameters
}

String getPageTab(String pageName) { return (String)((Map<String,String>)state.pageTabs)?."$pageName" }
void setPageTab(String pageName, String tabName) {
	if (state.pageTabs == null) state.pageTabs = (Map<String,String>)[(pageName): tabName]
	else ((Map<String,String>)state.pageTabs)[pageName] = tabName
}


def sectionFooter(String pageTo = null) {
	String bullet = bullet(1)
	String footer = "<div style='color:#1A77C9;text-align:center'>$APP_NAME  $bullet v${APP_VERSION}  $bullet jdc72</div>"
	if (pageTo != null) footer += getPageLinkTo(pageTo)
	paragraph lpx(20)
	paragraph getFormat("line")
	paragraph footer
}


def sectionTopMenu() {
	String menu = getPageMenuListItems("pageMain", PAGE_MAIN_TABS, false, true)
	List<Map<String,String>> pageHistory = getPageHistory()?.dropRight(1)
	pageHistory?.each { Map<String,String> page ->
		menu += getIconListSeparator("grey")
		menu += getPageMenuListItems(page.name, (List<Map<String,String>>)[page], false, false)
	}
	paragraph "<div class='menu-secondary'>" + getPageMenuList(menu) + "</div>"
}
def sectionPageMenu(String pageParent, List<Map<String,String>> tabs, boolean isPageMain = false) {
	paragraph "<div class='menu'>" + getPageMenuList(pageParent, tabs, isPageMain) + "</div>"
}
@CompileStatic
static String getPageMenuList(String list) { return "${getStyleMenu()}<ul class='nav'>" + list + "</ul>" }
@CompileStatic
String getPageMenuList(String pageParent, List<Map<String,String>> pages, boolean isMenuMain = false, boolean isPageMain = false) {
	return "${getStyleMenu()}<ul class='nav'>" + getPageMenuListItems(pageParent, pages, isMenuMain, isPageMain) + "</ul>"
}
@CompileStatic
String getPageMenuListItems(String pageParent, List<Map<String,String>> pages, boolean isMenuMain = false, boolean isPageMain = false) {
	String pageCurrent = getPageTab(pageParent)
	String first = "secondary"
	String level = (isMenuMain ? "primary" : "secondary")
	String list = ""
	pages.each { Map<String,String> page ->
		String displayName = (isMenuMain ? "<strong>${page.display}</strong>" : page.display)
		String pageName = (isMenuMain || isPageMain ? pageParent + "_" + page.name : pageParent)
		//Map<String,String> params = (isMenuMain ? null : [pageTab: page.name])
		String active = (page.name == pageCurrent || (!isMenuMain && !isPageMain)) ? "active" : ""
		String required = (page.required != null ? "requiredTodo" : "")
		first = (first == "secondary") ? "first" : ""
		list += "<li class='$active $level $first $required'>${hrefButtonLink(pageName, displayName)}</li>"
	}
	return list
}


//=========================================================================
// Descriptions:  Generic Helpers
//=========================================================================

@CompileStatic
List<Map<String,String>> getObjectsAndDescriptions() {
	List<Map<String,String>> objectsDesc = []
	objectsDesc.addAll(getObjectsAndDescriptions(OBJECT_LAST))
	objectsDesc.addAll(getObjectsAndDescriptions(OBJECT_LOW))
	objectsDesc.addAll(getObjectsAndDescriptions(OBJECT_NEW))
	objectsDesc.addAll(getObjectsAndDescriptions(OBJECT_OLD))
	return objectsDesc
}
@CompileStatic
List<Map<String,String>> getObjectsAndDescriptions(String objectType) {
	List<Map<String,String>> values = []
	HashMap<String,String> om = OBJECT_MAP[objectType]
	getObjectIds(om)?.each { String objectId ->
		Map<String,String> desc = getObjectDescription(om, objectId)
		desc["objectType"] = objectType
		desc["objectId"] = objectId
		values.add(desc)
	}
	return values.sort { it.fullS }
}


@CompileStatic
Map<String,String> getObjectDescription(Map<String,String> om, String objectId) {
	if (objectId == OBJECT_ID_APP) return [name: "Global Devices"]
	String icon = " " + getIcon(ICON_SETTINGS) + " "
	String descNameSL = "${om.descC.replace('-',' ')} #" + parseObjectIdNum(objectId)
	String descNameML = descNameSL + "\n" + lpx(6)
	Map<String,String> descTime = getObjectDescriptionTime(objectId)
	Map<String,String> desc = [bodyS: "", bodyL: ""]
	switch (om.type) {
		case OBJECT_LAST:
			// descC #num @ time [days] @ last activity > time
			String descInterval = getObjectDescriptionInterval(objectId)
			String descLA = "last activity > $descInterval"
			desc = [bodyS: (String)"${descTime.fullS}${getObjectDescriptionValue(false, descLA, icon)}",
					bodyL: (String)"${descTime.fullL}${getObjectDescriptionValue(true, descLA, "for")}"]
			break
		case OBJECT_LOW:
		case OBJECT_NEW:
			// descC #num @ time [days] @ level (N%), change (N%)
			Map<String,String> descThresholds = [bodyS: "", bodyL: ""]
			String descAbs = getObjectDescriptionPercentageThresholdAbsolute(om.type, objectId)
			String descRel = getObjectDescriptionPercentageThresholdRelative(objectId)
			String descSuppress = getObjectDescriptionPercentageSuppress(objectId)
			List<String> descList = []
			if (descAbs) descList.add(descAbs)
			if (descRel) descList.add(descRel)
			descThresholds.bodyS = " $icon " + descList.join(" $icon ")
			descList.each { descThresholds.bodyL += getObjectDescriptionValue(true, it, "for") }
			if (descSuppress) descThresholds.bodyL += descSuppress
			desc = [bodyS: (String)"${descTime.fullS}${descThresholds.bodyS}",
					bodyL: (String)"${descTime.fullL}${descThresholds.bodyL}"]
			break
		case OBJECT_OLD:
			// descC #num @ time [days] @ life is [at/N days] </>/== avg
			String descInterval = getObjectDescriptionInterval(objectId)
			String descReference = getObjectDescriptionReference(objectId)
			Map<String,String> descOld = [
				bodyS: getObjectDescriptionValue(false, "life ${descInterval}${descReference}", icon),
				bodyL: getObjectDescriptionValue(true, "life ${descInterval}${descReference}", "for")]
			desc = [bodyS: (String)"${descTime.fullS}${descOld.bodyS}",
					bodyL: (String)"${descTime.fullL}${descOld.bodyL}"]
			break
	}
	desc.bodyL += lpx(6) + getObjectDescriptionDevices(objectId)
	String bodyL = getFormat("description", desc.bodyL)
	return [name: descNameSL,
			bodyS: desc.bodyS,
			fullS: descNameSL + desc.bodyS,
			bodyL: bodyL,
			fullL: descNameML + bodyL]
}
@CompileStatic
String getObjectDescriptionDevices(String objectId) {
	String desc = getObjectDescriptionDevicesBase(objectId)
	return (desc == null) ? bullet(3) + "No configured devices" : desc
}
String getObjectDescriptionDevicesBase(String objectId = null) {
	List<String> descDevices = []
	["tracked", "paused", "notification"].each { String deviceType ->
		ArrayList<String> descType = []
		if (isObjectUsingAppDevices(objectId, deviceType)) {
			int devicesNumApp = getObjectDevicesCount(OBJECT_ID_APP, deviceType)
			String descGlobal = (deviceType == "notification") ? "global" : "globally"
			if (devicesNumApp > 0) descType.add("$devicesNumApp " + (objectId != null ? descGlobal : ""))
		}
		if (objectId != null) {
			int devicesNumObj = getObjectDevicesCount(objectId, deviceType)
			String descLocal = (deviceType == "notification") ? "local" : "locally"
			if (devicesNumObj > 0) descType.add("$devicesNumObj $descLocal")
		}
		if (!descType.isEmpty()) descDevices.add(descType.join(", ") + " $deviceType device(s).")
	}
	return descDevices.isEmpty() ? null : descDevices.collect { bullet(3) + it }.join("\n")
}
int getObjectDevicesCount(String objectId, String deviceType) {
	return deviceType == "paused" ? (getObjectDevicesPaused(objectId)?.keySet()?.size() ?: 0) :
		((DeviceWrapperList)settings."${objectId}_devices_${deviceType}")?.size() ?: 0
}
String getObjectDescriptionInterval(String objectId) {
	Long days = settings."${objectId}_intervalDays" ?: 0
	Long hours = settings."${objectId}_intervalHours" ?: 0
	Long minutes = settings."${objectId}_intervalMinutes" ?: 0
	Long totalMin = minutes + hours * 60 + days * 1440
	Long d = (Long)(totalMin / 1440)
	Long h = (Long)((totalMin - d * 1440) / 60)
	Long m = totalMin % 60
	List<String> desc = []
	if (d > 0) desc.add((String)(d == 0 ? "" : "$d day${d == 1 ? '' : 's'}"))
	if (h > 0) desc.add((String)(h == 0 ? "" : "$h hour${h == 1 ? '' : 's'}"))
	if (m > 0) desc.add((String)(m == 0 ? "" : "$m minute${m == 1 ? '' : 's'}"))
	return desc.isEmpty() ? "" : desc.join(", ") + " "
}
String getObjectDescriptionPercentageThresholdAbsolute(String objectType, String objectId) {
	BigDecimal absolute = settings."${objectId}_threshold_absolute"
	String cmpLevel = (objectType == OBJECT_LOW) ? "<" : ">"
	return absolute ? "level $cmpLevel ${absolute}%" : null
}
String getObjectDescriptionPercentageThresholdRelative(String objectId) {
	BigDecimal change = settings."${objectId}_threshold_relative"
	return change ? "change > ${change}%" : null
}
String getObjectDescriptionPercentageSuppress(String objectId) {
	BigDecimal days = settings."${objectId}_intervalDaysSuppress"
	return (days ?: 0) == 0 ? null : bullet(3) +
		fpx(14, "Suppress alerts for $days days after battery changes\n")
}
String getObjectDescriptionReference(String objectId) {
	String relative = settings."${objectId}_intervalRelative"
	String descRelative = (relative == RELATIVE_AT) ? "==" : (relative == RELATIVE_AFTER) ? ">" : "<"
	return "$descRelative avg life"
}
Map<String,String> getObjectDescriptionTime(String objectId) {
	String iconClock = " " + getIcon(ICON_CLOCK) + " "
	String iconDate = " " + getIcon(ICON_CALENDAR) + " "
	String timeDesc = formatTime(toDateTime(getObjectScheduleTime(objectId)))
	List<String> daysList = getObjectScheduleDays(objectId) - "Toggle All On/Off"
	String days = (daysList == OPTIONS_DAYOFWEEK) ? "" : daysList.collect { it.substring(0, 3) }.toString()
	String daysDescSL = days.isEmpty() ? "" : " $iconDate " + (days - "[" - "]")
	String daysDescML = days.isEmpty() ? "" : " on " + days
	return [fullS: getObjectDescriptionValue(false, "${timeDesc}${daysDescSL}", iconClock),
			fullL: getObjectDescriptionValue(true, "${timeDesc}${daysDescML}", "@")]
}
@CompileStatic
static String getObjectDescriptionValue(boolean full, String text, String delimiter = "@") {
	return full ? bullet(3) + "Check $delimiter " + text + "\n" : " $delimiter " + text
}


//=========================================================================
// Settings:  Helpers
//=========================================================================

void copySetting(String name, String value, String type) {
	if (settings."$value" != null) {
		String typeUse = type.startsWith("device.") ? "capability.*" : type
		app.updateSetting(name, [type: typeUse, value: settings."$value"])
	}
}
@CompileStatic
void copySetting(String rootCopy, String rootSource, String namePostfix, String type) {
	copySetting("${rootCopy}_${namePostfix}", "${rootSource}_${namePostfix}", type)
}


void deleteSettingsRoot(String root, boolean underscore = false) {
	String rootUse = root + (underscore ? "_" : "")
	settings.findAll {
		((String)it.key).startsWith(rootUse)
	}.keySet().each {
		app.removeSetting((String)it)
	}
}


//=========================================================================
// State:  Helpers
//=========================================================================

void copyState(String rootCopy, String rootSource, String namePostfix) {
	state."${rootCopy}_${namePostfix}" = state."${rootSource}_${namePostfix}"
}


void deleteStateRoot(String root, boolean underscore = false) {
	String rootUse = root + (underscore ? "_" : "")
	state.findAll {
		((String)it.key).startsWith(rootUse)
	}.keySet().each {
		state.remove((String)it)
	}
}


void deleteStateConfigureValue(String key) {
	if (state.configure != null && key != null) ((Map)state.configure).remove(key)
}
boolean getStateConfigureValueBoolean(String key) { return (Boolean)state.configure?."$key" ?: false }
boolean setStateConfigureValue(String key, boolean value) {
	if (key != null) {
		initializeStateConfigure()
		((Map)state.configure)[key] = value
	}
	return value
}
List getStateConfigureValueList(String key) { return (List)state.configure?."$key" }
List setStateConfigureValue(String key, List value) {
	if (key != null) {
		initializeStateConfigure()
		((Map)state.configure)[key] = value
	}
	return value
}
Map getStateConfigureValueMap(String key) { return (Map)state.configure?."$key" }
Map setStateConfigureValue(String key, Map value) {
	if (key != null) {
		initializeStateConfigure()
		((Map)state.configure)[key] = value
	}
	return value
}
String getStateConfigureValueString(String key) { return (String)state.configure?."$key" }
String setStateConfigureValue(String key, String value) {
	if (key != null) {
		initializeStateConfigure()
		((Map)state.configure)[key] = value
	}
	return value
}
void initializeStateConfigure() { if (!state.containsKey("configure")) state.configure = [:] }


void resetStateConfigTo(List<String> keysKeep) {
	if (state.containsKey("configure")) {
		Map<String,String> configure = state.configure as Map<String,String>
		List<String> keysKeepExact = keysKeep?.findAll { !it.endsWith("*") }
		List<String> keysKeepWildcard = keysKeep?.findAll { it.endsWith("*") }?.
			collect { it.replaceAll(/\*/, "") }
		configure.keySet().findAll { String key ->
			!keysKeepExact.any { it == key } &&
			!keysKeepWildcard.any { key.startsWith(it) }
		}.each { String key ->
			configure.remove(key)
		}
		state.configure = configure
	}
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
List<DeviceWrapper> getSettingsDevicesList(
	String objectId, String deviceType, boolean excludePaused, boolean checkUseApp = true) {
	return getSettingsDevicesMap(objectId, deviceType, excludePaused, checkUseApp)?.values()?.sort {
		a, b -> a.displayName <=> b.displayName
	}
}
List<DeviceWrapper> getSettingsDevicesAllList(String deviceType, boolean excludePaused) {
	Map<String,DeviceWrapper> devicesMap = [:]
	getObjectIdsAll(true).each {
		devicesMap = devicesMap + getSettingsDevicesMap(it, deviceType, excludePaused, false)
	}
	return devicesMap?.values()?.sort { a,b ->
		a.displayName <=> b.displayName
	}
}

Map<String,DeviceWrapper> getSettingsDevicesMap(
	String objectId, String deviceType, boolean excludePaused, boolean checkUseApp = true) {
	if (objectId == null && deviceType == null) return [:]
	boolean tryAppInclude = checkUseApp && isObjectUsingAppDevices(objectId, deviceType)
	boolean tryAppExclude = checkUseApp && excludePaused && isObjectUsingAppDevices(objectId, "paused")

	Map<String,DeviceWrapper> devicesInclude = [:]
	if (tryAppInclude) {
		devicesInclude = getSettingsDevicesMap("${OBJECT_ID_APP}_devices_${deviceType}")
		if (tryAppExclude)
			getObjectDevicesPaused(OBJECT_ID_APP)?.keySet()?.each { devicesInclude.remove(it) }
	}
	devicesInclude = devicesInclude + getSettingsDevicesMap("${objectId}_devices_${deviceType}")
	if (excludePaused) {
		getObjectDevicesPaused(objectId)?.keySet()?.each { devicesInclude.remove(it) }
	}
	return devicesInclude
}
HashMap<String,DeviceWrapper> getSettingsDevicesMap(String key) {
	HashMap<String,DeviceWrapper> devices = [:]
	if (key == null) return devices
	(DeviceWrapperList)settings."$key"?.each { DeviceWrapper device -> devices[device.id] = device }
	return devices
}


//=========================================================================
// Objects:  Definition Getters,Setters
//=========================================================================

@Field public static final HashMap<String,HashMap<String,String>> OBJECT_MAP = [
	LAN: [type: "LAN", abbr: "lan", numbers: "lanNumbers", ids: "lanIds", descC: "Last-Activity Tracker"],
	LBN: [type: "LBN", abbr: "lbn", numbers: "lbnNumbers", ids: "lbnIds",  descC: "Low-Battery Tracker"],
	NBN: [type: "NBN", abbr: "nbn", numbers: "nbnNumbers", ids: "nbnIds",  descC: "New-Battery Tracker"],
	OBN: [type: "OBN", abbr: "obn", numbers: "obnNumbers", ids: "obnIds",  descC: "Old-Battery Tracker"]
]


List<String> getObjectIdsAll(boolean includeApp = false) {
	ArrayList<String> objectIds = (includeApp) ? [OBJECT_ID_APP] : []
	OBJECT_MAP.values().collect { it.ids }.each {
		objectIds.addAll((ArrayList<String>)state."$it" ?: [])
	}
	return objectIds
}
List<String> getObjectIds(HashMap<String,String> om) { return (List<String>)state."${om.ids}" }
@CompileStatic
void addToObjectIds(HashMap<String,String> om, String objectId) {
	if (getObjectIds(om) == null) setObjectIds(om, [])
	List<String> objectIds = getObjectIds(om)
	if (!objectIds.contains(objectId)) objectIds.add(objectId)
	setObjectIds(om, objectIds)
}
void setObjectIds(HashMap<String,String> om, List<String> objectIds) {
	objectIds?.sort { parseObjectIdNum(it) }
	state."${om.ids}" = objectIds
}
void deleteObjectIds(HashMap<String,String> om) { state.remove(om.ids) }
void deleteFromObjectIds(HashMap<String,String> om, String objectId) {
	((List)state."${om.ids}")?.removeElement(objectId)
}


List<Integer> getObjectNumbers(HashMap<String,String> om) {
	return (List<Integer>)state."${om.numbers}"?.sort()
}
void addToObjectNumbers(HashMap<String,String> om, Integer objectNum) {
	if (state."${om.numbers}" == null)
		state."${om.numbers}" = []
	((List<Integer>)state."${om.numbers}").add(objectNum)
	((List<Integer>)state."${om.numbers}").sort()
}
void deleteObjectNumbers(HashMap<String,String> om) { state.remove(om.numbers) }
void deleteFromObjectNumbers(HashMap<String,String> om, int objectNum) {
	((List)state."${om.numbers}")?.removeElement(objectNum)
}


Map<String,Map<String,String>> getObjectDevicesPaused(String objectId) {
	String today = getDateToday(DATETIME_FORMAT_SETTING)
	Map<String,Map<String,String>> devices = (Map<String,Map<String,String>>)state."${objectId}_devicesPaused" ?: [:]
	devices.findAll {
		it.value.containsKey("date") && it.value.date <= today
	}?.each {
		devices.remove(it)
	}
	if (devices.isEmpty()) deleteObjectDevicesPaused(objectId)
	return devices
}
Map<String,String> getObjectDevicePaused(String objectId, String deviceId) {
	Map<String,Map<String,String>> devices = getObjectDevicesPaused(objectId)
	return devices[deviceId]
}
@CompileStatic
String getObjectDevicePausedDate(String objectId, String deviceId) {
	return getObjectDevicePaused(objectId, deviceId)?.date
}
void addToObjectDevicesPaused(String objectId, String deviceId, String dateUntil = null) {
	if (state."${objectId}_devicesPaused" == null)
		state."${objectId}_devicesPaused" = [:]
	HashMap<String,String> pauseInfo = []
	if (dateUntil != null) pauseInfo["date"] = dateUntil
	((Map<String,Map<String,String>>)state."${objectId}_devicesPaused")[deviceId] = pauseInfo
}
@CompileStatic
String addObjectDevicePausedDate(String objectId, String deviceId, String unpauseDate) {
	Map<String,Map<String,String>> devices = getObjectDevicesPaused(objectId)
	Map<String,String> device = devices[deviceId]
	if (device == null) return null
	device["date"] = unpauseDate
	return unpauseDate
}
void deleteObjectDevicesPaused(String objectId) {
	String key = "${objectId}_devicesPaused"
	state.remove(key)
}
void deleteFromObjectDevicesPaused(String objectId, String deviceId) {
	Map<String,Map<String,String>> devices = getObjectDevicesPaused(objectId)
	devices?.remove(deviceId)
	if (devices.isEmpty()) deleteObjectDevicesPaused(objectId)
}
@CompileStatic
void deleteObjectDevicePausedDate(String objectId, String deviceId) {
	Map<String,Map<String,String>> devices = getObjectDevicesPaused(objectId)
	Map<String,String> device = devices[deviceId]
	if (device == null) return
	device.remove("date")
}


List<String> getObjectScheduleDays(String objectId) {
	return (List<String>)settings."${objectId}_checkDaysOfWeek"
		?: (List<String>)settings.app_checkDaysOfWeek ?: (List<String>)settings.app_checkDaysOfWeek_bak
}
String getObjectScheduleTime(String objectId) {
	return (String)settings."${objectId}_checkTime"
		?: (String)settings.app_checkTime ?: (String)settings.app_checkTime_bak
}


void setObjectUsingLocalSchedule(String objectId) {
	app.updateSetting("${objectId}_useGlobalSchedule", [type: "bool", value: false])
}


@CompileStatic
boolean isObjectId(String objectId) {
	return objectId != null && (objectId == OBJECT_ID_APP || getObjectIdsAll()?.contains(objectId))
}
@CompileStatic
boolean isObjectNumber(HashMap<String,String> om, int objectNum) {
	return getObjectNumbers(om)?.contains(objectNum)
}


@CompileStatic
boolean isObjectDevicePaused(String objectId, String deviceId) {
	return getObjectDevicesPaused(objectId)?.containsKey(deviceId)
}


boolean isObjectSetupComplete(String objectType, String objectId) {
	if (!isObjectDevicesSetup(objectId) ||
		!isObjectScheduleSetup(objectId) ||
		!isObjectAlertSetup(objectType, objectId))
		return false
	return true
}
boolean isObjectAlertSetup(String objectType, String objectId) {
	switch (objectType) {
		case OBJECT_LAST:
			if (settings."${objectId}_intervalDays" == null &&
				settings."${objectId}_intervalHours" == null &&
				settings."${objectId}_intervalMinutes" == null)
				return false
			break
		case OBJECT_LOW:
		case OBJECT_NEW:
			if (settings."${objectId}_threshold_absolute" == null &&
				settings."${objectId}_threshold_relative" == null)
				return false
			break
		case OBJECT_OLD:
			if (settings."${objectId}_intervalRelative" == null ||
				(settings."${objectId}_intervalRelative" != RELATIVE_AT &&
				 settings."${objectId}_intervalDays" == null))
				return false
			break
	}
	return true
}
boolean isObjectDevicesSetup(String objectId) {
	return (settings."${objectId}_devices_tracked" != null ||
		(settings.app_devices_tracked != null) &&
		 isObjectUsingAppDevices(objectId, "tracked"))
}
boolean isObjectScheduleSetup(String objectId) {
	return (settings."${objectId}_checkTime" != null && settings."${objectId}_checkDaysOfWeek" != null) ||
		isObjectUsingGlobalSchedule(objectId)
}


boolean isObjectUsingAppDevices(String objectId, String keyDevices) {
	return objectId != OBJECT_ID_APP && (Boolean)settings."${objectId}_devices_${keyDevices}_use_app" != false
}
boolean isObjectUsingGlobalSchedule(String objectId) {
	if (objectId == OBJECT_ID_APP) return false
	Boolean isUsingGlobalSchedule = (Boolean)settings."${objectId}_useGlobalSchedule"
	return (isUsingGlobalSchedule != null ? isUsingGlobalSchedule : true)
}


@CompileStatic
static String nameObjectId(HashMap<String,String> om, int objectNum) {
	return "${om.abbr}${objectNum}"
}


@CompileStatic
static String parseObjectId(String id) {
	List<Number> indexes = id.findIndexValues { it == '_' }
	return (indexes.size() < 1) ? id : id.substring(0, (Integer)indexes[0])
}
@CompileStatic
static String parseObjectIdType(String id) {
	return id.substring(0, 3).toUpperCase()
}
@CompileStatic
static Integer parseObjectIdNum(String id) {
	List<Number> indexes = id.findIndexValues { it == '_' }
	String num = (indexes.size() < 1) ? id.substring(3) : id.substring(3, (Integer)indexes[0])
	return num.toInteger()
}


@CompileStatic
static String nameObjectButtonGlobalSchedule(String objectId, String schedule) {
	return "schedule|$schedule|$objectId"
}
@CompileStatic
static HashMap<String,String> parseObjectButtonGlobalScheduleName(String buttonName) {
	List<Number> indexes = buttonName.findIndexValues { it == '|' }
	return [schedule: buttonName.substring((int)indexes[0] + 1, (int)indexes[1]),
			objectId: buttonName.substring((int)indexes[1] + 1)]
}


@CompileStatic
static String nameObjectButtonDevicePause(String objectId, String deviceId, boolean isPaused) {
	String action = (isPaused ? "unpause" : "pause")
	return "devicePause|$action|$deviceId|$objectId"
}
@CompileStatic
static HashMap<String,String> parseObjectButtonDevicePauseName(String buttonName) {
	List<Number> indexes = buttonName.findIndexValues { it == '|' }
	return [action  : buttonName.substring((int)indexes[0] + 1, (int)indexes[1]),
			deviceId: buttonName.substring((int)indexes[1] + 1, (int)indexes[2]),
			objectId: buttonName.substring((int)indexes[2] + 1)]
}


@CompileStatic
static String nameObjectButtonSwapOrder(String objectType, String objectId, String direction, String side) {
	return "swapOrder|$direction|$side|$objectType|$objectId"
}
@CompileStatic
static HashMap<String,String> parseObjectButtonSwapOrderName(String buttonName) {
	List<Number> indexes = buttonName.findIndexValues { it == '|' }
	return [direction : buttonName.substring((int)indexes[0] + 1, (int)indexes[1]),
			objectType: buttonName.substring((int)indexes[2] + 1, (int)indexes[3]),
			objectId  : buttonName.substring((int)indexes[3] + 1)]
}


//=========================================================================
// Objects:  Definition Setup
//=========================================================================

@CompileStatic
String addObject(HashMap<String,String> om, boolean findLowestNumber = true) {
	List<Integer> objectNumbers = cloneIntegerArrayList(getObjectNumbers(om))
	Integer objectNewNum = findNextIntegerInCollection(objectNumbers, findLowestNumber)
	return addObject(om, objectNewNum)
}
@CompileStatic
String addObject(HashMap<String,String> om, String objectNewId) {
	Integer objectNewNum = parseObjectIdNum(objectNewId)
	return addObject(om, objectNewNum)
}
@CompileStatic
String addObject(HashMap<String,String> om, int objectNewNum) {
	String objectNewId = nameObjectId(om, objectNewNum)
	addToObjectIds(om, objectNewId)
	addToObjectNumbers(om, objectNewNum)
	if(!isObjectScheduleSetup(OBJECT_ID_APP))
		setObjectUsingLocalSchedule(objectNewId)
	logI "[${removeSpaces(om.descC)}=${objectNewId}] added"
	return objectNewId
}


@CompileStatic
void cleanupObjects() {
	OBJECT_MAP.keySet().each { String objectType ->
		tryDeleteObjectsSettingsUnused(objectType)
		deleteMalformedObjects(objectType)
	}
}
void cleanupObjectSchedules() {
	boolean isGlobalScheduleSetup = isObjectScheduleSetup(OBJECT_ID_APP)
	getObjectIdsAll()?.each { String objectId ->
		if (isObjectUsingGlobalSchedule(objectId)) {
			if (isGlobalScheduleSetup)
				deleteObjectSchedule(objectId)
			else
				copyObjectScheduleFromBackup(objectId)
		}
	}
	deleteObjectSchedule(OBJECT_ID_APP, true)
}


@CompileStatic
String copyObject(
	HashMap<String,String> om, String objectIdNow, boolean findLowestNumber = true) {
	String objectIdNew = addObject(om, findLowestNumber)
	return copyObjectDetails(objectIdNow, objectIdNew)
}
@CompileStatic
String copyObject(
	HashMap<String,String> om, String objectIdNow, String objectIdNew) {
	addObject(om, objectIdNew)
	return copyObjectDetails(objectIdNow, objectIdNew)
}
@CompileStatic
String copyObjectDetails(String objectIdNow, String objectIdNew) {
	copyState(objectIdNew, objectIdNow, "devicesPaused")
	copySetting(objectIdNew, objectIdNow, "devices_paused_use_app", "bool")
	copySetting(objectIdNew, objectIdNow, "devices_notification", "capability.notification")
	copySetting(objectIdNew, objectIdNow, "devices_notification_use_app", "bool")
	copySetting(objectIdNew, objectIdNow, "devices_tracked", "capability.battery")
	copySetting(objectIdNew, objectIdNow, "devices_tracked_use_app", "bool")
	copySetting(objectIdNew, objectIdNow, "intervalDays", "number")
	copySetting(objectIdNew, objectIdNow, "intervalHours", "number")
	copySetting(objectIdNew, objectIdNow, "intervalMinutes", "number")
	copySetting(objectIdNew, objectIdNow, "intervalRelative", "enum")
	copySetting(objectIdNew, objectIdNow, "useGlobalSchedule", "bool")
	copySetting(objectIdNew, objectIdNow, "threshold_absolute", "number")
	copySetting(objectIdNew, objectIdNow, "threshold_relative", "number")
	copyObjectSchedule(objectIdNow, objectIdNew)
	return objectIdNew
}
@CompileStatic
void copyObjectSchedule(String objectIdNow, String objectIdNew) {
	copySetting(objectIdNew, objectIdNow, "checkDaysOfWeek", "enum")
	copySetting(objectIdNew, objectIdNow, "checkTime", "time")
}
@CompileStatic
void copyObjectScheduleFromGlobal(String objectId) {
	setObjectUsingLocalSchedule(objectId)
	copyObjectSchedule(OBJECT_ID_APP, objectId)
}
@CompileStatic
void copyObjectScheduleFromBackup(String objectId) {
	setObjectUsingLocalSchedule(objectId)
	copySetting("${objectId}_checkDaysOfWeek", "${OBJECT_ID_APP}_checkDaysOfWeek_bak", "enum")
	copySetting("${objectId}_checkTime", "${OBJECT_ID_APP}_checkTime_bak", "time")
}
@CompileStatic
void backupObjectSchedule(String objectId) {
	copySetting("${objectId}_checkDaysOfWeek_bak", "${objectId}_checkDaysOfWeek", "enum")
	copySetting("${objectId}_checkTime_bak", "${objectId}_checkTime", "time")
}


@CompileStatic
void deleteObject(HashMap<String,String> om, String objectId) {
	Integer objectNum = parseObjectIdNum(objectId)
	deleteFromObjectNumbers(om, objectNum)
	deleteFromObjectIds(om, objectId)
	deleteSettingsRoot(objectId, true)
	deleteStateRoot(objectId, false)
	tryDeleteObjectState(om)
	logI "[${removeSpaces(om.descC)}=${objectId}] deleted"
}
@CompileStatic
void tryDeleteObjectState(HashMap<String,String> om) {
	if (getObjectNumbers(om)?.isEmpty())
		deleteObjectNumbers(om)
	if (getObjectIds(om)?.isEmpty())
		deleteObjectIds(om)
}
void deleteObjectSchedule(String objectId, boolean backup = false) {
	app.removeSetting("${objectId}_useGlobalSchedule" + (backup ? "_bak" : ""))
	app.removeSetting("${objectId}_checkDaysOfWeek" + (backup ? "_bak" : ""))
	app.removeSetting("${objectId}_checkTime" + (backup ? "_bak" : ""))
}


@CompileStatic
void tryDeleteObjectsSettingsUnused(String objectType) {
	getObjectIds(OBJECT_MAP[objectType])?.each { String objectId ->
		tryDeleteObjectSettingsUnused(objectId)
	}
}
void tryDeleteObjectSettingsUnused(String objectId) {
	tryDeleteObjectSettingsNumber("${objectId}_intervalDays")
	tryDeleteObjectSettingsNumber("${objectId}_intervalHours")
	tryDeleteObjectSettingsNumber("${objectId}_intervalMinutes")
	if (settings."${objectId}_observe" != true) {
		app.removeSetting("${objectId}_observe")
		app.removeSetting("${objectId}_record")
	}
	if (settings."${objectId}_devices_notification_use_app" == true)
		app.removeSetting("${objectId}_devices_notification_use_app")
	if (settings."${objectId}_devices_paused_use_app" == true)
		app.removeSetting("${objectId}_devices_paused_use_app")
	if (settings."${objectId}_devices_tracked_use_app" == true)
		app.removeSetting("${objectId}_devices_tracked_use_app")
	if (settings."${objectId}_useGlobalSchedule" == true)
		app.removeSetting("${objectId}_useGlobalSchedule")
}
void tryDeleteObjectSettingsNumber(String key) {
	if (settings."$key" == null || settings."$key" == 0)
		app.removeSetting(key)
}


void deleteMalformedObjects(String objectType) {
	HashMap<String,String> om = OBJECT_MAP[objectType]
	Set<String> objectsToDelete = []
	settings.findAll {
		((String)it.key).endsWith("_checkTime") &&
		((String)it.key).startsWith(om.abbr)
	}.keySet().each { name ->
		Integer objectNum = parseObjectIdNum((String)name)
		if (!isObjectNumber(om, objectNum))
			objectsToDelete.add(nameObjectId(om, objectNum))
	}
	getObjectIds(om)?.findAll { String objectId ->
		!isObjectSetupComplete(objectType, objectId)
	}?.each { String objectId ->
		objectsToDelete.add(objectId)
	}
	objectsToDelete.each { String objectId ->
		deleteObject(om, objectId)
	}
	tryDeleteObjectState(om)
}


@CompileStatic
void modifyObjectSchedule(List<String> objectIds, String schedule) {
	objectIds?.each { String objectId ->
		if (schedule == "global")
			deleteObjectSchedule(objectId)
		else if (schedule == "local") {
			if (isObjectScheduleSetup(OBJECT_ID_APP))
				copyObjectScheduleFromGlobal(objectId)
			else
				copyObjectScheduleFromBackup(objectId)
		}
	}
}


@CompileStatic
void renumberObjects(HashMap<String,String> om) {
	getObjectIds(om)?.each { String objectId ->
		copyObject(om, objectId, false)
		deleteObject(om, objectId)
	}
	getObjectIds(om)?.each { String objectId ->
		copyObject(om, objectId)
		deleteObject(om, objectId)
	}
}


@CompileStatic
void swapObjectsOrder(HashMap<String,String> om, String objectIdNow, String direction) {
	List<String> objectIds = getObjectIds(om)
	if (objectIds.size() < 1) return

	Integer idxNow = objectIds?.indexOf(objectIdNow)
	Integer idxNew = (idxNow > 0 && DIRECTION_DECREMENT == direction) ? idxNow - 1 :
		(idxNow < objectIds.size() - 1 && DIRECTION_INCREMENT == direction) ? idxNow + 1 : idxNow
	if (idxNew == idxNow) return

	String objectIdNew = objectIds[idxNew]
	String objectIdTemp = copyObject(om, objectIdNew, false)
	deleteObject(om, objectIdNew)
	copyObject(om, objectIdNow, objectIdNew)
	deleteObject(om, objectIdNow)
	copyObject(om, objectIdTemp, objectIdNow)
	deleteObject(om, objectIdTemp)
}


//=========================================================================
// Battery Device Data:  Definitions
//=========================================================================

HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> getDataStore(
	String dataStore, boolean createIfNull = false) {
	if (state."$dataStore" == null && createIfNull)
		state."$dataStore" = [:]
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data =
		(HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>>)state."$dataStore"
	return data
}

void deleteDataStore(String dataStore) {
	state.remove(dataStore)
}

void setDataStore(String dataStoreName, HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataStore) {
	dataStore?.values()?.each { dataDevice ->
		[STATE_CURRENT_MIGRATE, STATE_PENDING].each { String state ->
			if (!dataDevice.containsKey(state)) return
			if (dataDevice[state] == null || dataDevice[state]?.isEmpty())
				dataDevice.remove(state)
		}
		[STATE_PREVIOUS, STATE_PENDING].each { String state ->
			if (!dataDevice.containsKey(state)) return
			if (isDeviceDataStateEqualTo(dataDevice, STATE_CURRENT, state))
				dataDevice.remove(state)
		}
	}
	if (dataStore == null || dataStore.isEmpty())
		deleteDataStore(dataStoreName)
	else state."$dataStoreName" = dataStore
}


void deleteCurrentBatteryDataNamesIfDefault() {
	[DATA_DATE, DATA_LIFE, DATA_TYPE].each { String dataType ->
		String key = "deviceDataName_${dataType}_current"
		if ((String)settings."$key" == dataType)
			app.removeSetting(key)
	}
}


boolean isHelpEnabledForBatteryData(String dataStore) {
	return isPageHelpEnabled("pageBatteryData_${dataStore}")
}
def inputBooleanBatteryDataHelp(String dataStore, Integer width = 12) {
	inputBooleanPageHelpInline("pageBatteryData_${dataStore}", width)
}


void deleteMigrateBatteryData() {
	app.removeSetting("data_type_migrate")
	app.removeSetting("data_date_format")
	app.removeSetting("data_life_units")
	app.removeSetting("data_name_migrate")
	[DATA_DATE, DATA_LIFE, DATA_TYPE].each { String dataType ->
		app.removeSetting("deviceDataName_${dataType}_new")
	}
}


String getDeviceDataName(String dataType) {
	return (String)settings."deviceDataName_${dataType}_current" ?: dataType
}
void setDeviceDataName(String dataType, String value) {
	app.updateSetting("deviceDataName_${dataType}_current", [type: "text", value: value])
}

@CompileStatic
HashMap<String,String> getDeviceDataTypeNames(boolean includePercentage = false) {
	HashMap<String,String> dataNames = [:]
	[DATA_DATE, DATA_LIFE, DATA_TYPE].each { dataNames[it] = getDeviceDataName(it) }
	if (includePercentage) dataNames[DATA_PERCENTAGE] = DATA_PERCENTAGE
	return dataNames
}


@CompileStatic
static String nameButtonDeviceData(String dataStore, String dataType, String dataValue,
	String action, String deviceId) {
	return "deviceData|$dataStore|$dataType|$dataValue|$action|$deviceId"
}
@CompileStatic
static HashMap<String,String> parseButtonDeviceDataName(String buttonName) {
	List<Number> indexes = buttonName.findIndexValues { it == '|' }
	HashMap<String,String> buttonInfo = [
		dataStore: buttonName.substring((int)indexes[0] + 1, (int)indexes[1]),
		dataType : buttonName.substring((int)indexes[1] + 1, (int)indexes[2]),
		dataValue: buttonName.substring((int)indexes[2] + 1, (int)indexes[3]),
		action   : buttonName.substring((int)indexes[3] + 1, (int)indexes[4])]
	String deviceId = buttonName.substring((int)indexes[4] + 1)
	if (deviceId != null && deviceId != "null")
		buttonInfo.deviceId = deviceId
	return buttonInfo
}


//=========================================================================
// Battery Device Data: Helpers: State Management
//=========================================================================

void initializeBatteryLevels() {
	OBJECT_MAP.keySet().each { String objectType ->
		HashMap<String,String> om = OBJECT_MAP[objectType]
		getObjectIds(om)?.findAll { String objectId ->
			requiresBatteryLevelsState(objectType)
		}?.each { String objectId ->
			HashMap<String,Integer> batteryLevelsNew = [:]
			HashMap<String,Integer> batteryLevelsNow = getBatteryLevels(objectType, objectId)
			getSettingsDevicesList(objectId, "tracked", false)?.each { DeviceWrapper device ->
				Integer batteryLevel = batteryLevelsNow?."${device.id}"
				batteryLevelsNew."${device.id}" = (batteryLevel != null) ? batteryLevel :
					(Integer)device.currentValue("battery")
			}
			if (!batteryLevelsNew.isEmpty())
				setBatteryLevels(objectType, objectId, batteryLevelsNew)
			else deleteBatteryLevels(objectType, objectId)
		}
	}
}


HashMap<String,Integer> getBatteryLevels(String objectType, String objectId) {
	return state."${getBatteryLevelsName(objectType, objectId)}"
}
void setBatteryLevels(String objectType, String objectId, HashMap<String,Integer> levels) {
	state."${getBatteryLevelsName(objectType, objectId)}" = levels
}
void setBatteryLevel(String objectType, String objectId, DeviceWrapper device, Integer level) {
	if (device == null || level == null) return
	HashMap<String,Integer> batteryLevels = getBatteryLevels(objectType, objectId)
	if (batteryLevels == null) batteryLevels = [:]
	batteryLevels."${device.id}" = level
	setBatteryLevels(objectType, objectId, batteryLevels)
}
void deleteBatteryLevels(String objectType, String objectId) {
	state.remove(getBatteryLevelsName(objectType, objectId))
}

@CompileStatic
static String getBatteryLevelsName(String objectType, String objectId) {
	return "${objectId}_batteryLevels" + (objectType == OBJECT_NEW ? "Low" : "High")
}
@CompileStatic
static boolean requiresBatteryLevelsState(String objectType) {
	return (objectType in [OBJECT_LOW, OBJECT_NEW])
}


void initializeNotificationDates() {
	getObjectIds(OBJECT_MAP[OBJECT_NEW])?.each { String objectId ->
		HashMap<String,Integer> noteDatesNew = [:]
		HashMap<String,Integer> noteDatesNow = getNotificationDates(objectId)
		List<DeviceWrapper> devicesBattery = getSettingsDevicesList(objectId, "tracked", false)
		devicesBattery?.each { DeviceWrapper device ->
			String noteDate = noteDatesNow?."${device.id}"
			if (noteDate != null)
				noteDatesNew."${device.id}" = noteDate
		}
		if (!noteDatesNew.isEmpty())
			setNotificationDates(objectId, noteDatesNew)
		else deleteNotificationDates(objectId)
	}
}


HashMap<String,Integer> getNotificationDates(String objectId) {
	return state."${getNewNotificationDatesName(objectId)}"
}
void setNotificationDates(String objectId, HashMap<String,Integer> levels) {
	state."${getNewNotificationDatesName(objectId)}" = levels
}
void setNotificationDate(String objectId, DeviceWrapper device, String date) {
	if (device == null || date == null) return
	HashMap<String,Integer> noteDates = getNotificationDates(objectId)
	if (noteDates == null) noteDates = [:]
	noteDates."${device.id}" = date
	setNotificationDates(objectId, noteDates)
}
void deleteNotificationDates(String objectId) {
	state.remove(getNewNotificationDatesName(objectId))
}

@CompileStatic
static String getNewNotificationDatesName(String objectId) {
	return "${objectId}_notificationDatesHigh"
}


//=========================================================================
// Battery Device Data:  Actions
//=========================================================================

void computeBatteryLifespanForPending(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	String dataType = DATA_LIFE
	String dataName = getDeviceDataName(dataType)
	data.values()?.each { dataDevice ->
		if (!dataDevice.containsKey(STATE_PENDING)) return
		HashMap<String,String> life = computeBatteryLifespanForPending(
			dataName, dataDevice[STATE_CURRENT], dataDevice[STATE_PENDING])
		if (life != null) {
			if (life.containsKey("value") && !life["value"].isEmpty())
				dataDevice[STATE_PENDING][dataType] = life
			if (life.containsKey("error"))
				logW ":: ${dataDevice.device.name} :: cannot compute Battery Lifespan: ${life["error"]}"
		}
	}
	setDataStore(dataStore, data)
}
@CompileStatic
static HashMap<String,String> computeBatteryLifespanForPending(String dataName,
	HashMap<String,HashMap<String,String>> dataNow, HashMap<String,HashMap<String,String>> dataNew) {
	String lifeNow = dataNow[DATA_LIFE]?.value ?: ""
	String dateNow = dataNow[DATA_DATE]?.value
	String dateNew = dataNew[DATA_DATE]?.value
	HashMap<String,String> dataLife = [name: dataName, value: lifeNow]
	if (dateNow == null || dateNew == null || dateNew == dateNow) return dataLife
	Long dateNowMilliseconds
	Long dateNewMilliseconds
	if (!lifeNow.isEmpty() && !isDeviceDataValid(DATA_LIFE, lifeNow)) {
		dataLife["error"] = (String)"The current Battery Lifespan ($lifeNow) is in an unexpected format"
		return dataLife
	}
	try {
		Date dateNowDt = new SimpleDateFormat(DATETIME_FORMAT_DATA)?.parse(dateNow)
		dateNowMilliseconds = dateNowDt?.getTime()
	} catch (ignored) {
		dataLife["error"] = (String)"The current Battery Date ($dataNow) is in an unexpected format"
		return dataLife
	}
	try {
		Date dateNewDt = new SimpleDateFormat(DATETIME_FORMAT_DATA)?.parse(dateNew)
		dateNewMilliseconds = dateNewDt?.getTime()
	} catch (ignored) {
		dataLife["error"] = (String)"The new Battery Date ($dateNew) is in an unexpected format"
		return dataLife
	}
	if (dateNowMilliseconds > dateNewMilliseconds) {
		dataLife["error"] = (String)"The new Battery Date ($dateNew) is older than the previous battery date ($dateNow)"
		return dataLife
	}
	Long lifeMilliseconds = dateNewMilliseconds - dateNowMilliseconds
	Long lifeWeeks = Math.round((Double)((Double)lifeMilliseconds / (Double)86400000))
	// days = milliseconds / (24 * 60 * 60 * 1000)
	ArrayList<String> lifeNew = (ArrayList<String>)[lifeWeeks.toString()]
	if (!lifeNow.isEmpty())
		lifeNew.addAll(removeSpaces(lifeNow).split(','))
	return [name: dataName, value: lifeNew.join(', ')]
}


HashMap<String,HashMap<String,String>> convertDeviceData(HashMap<String,HashMap<String,String>> data) {
	HashMap<String,HashMap<String,String>> dataNew = [:]
	data?.each {
		String dataType = it.key
		String dataValue = it.value.value
		dataNew[dataType] = [name: getDeviceDataName(dataType), value: dataValue]
		if (dataType == DATA_DATE) {
			String dateFmtNow = (String)settings.data_date_format
			try {
				dataNew[dataType]["value"] = formatDateTime(formatDateTime(dataValue, dateFmtNow), DATETIME_FORMAT_DATA)
			} catch (e) {
				dataNew[dataType]["value"] = dataValue
				dataNew[dataType]["error"] = (String)"Exception trying to parse date of $dataValue in format \"$dateFmtNow\": " + e.toString()
			}
		} else if (dataType == DATA_LIFE) {
			Double lifeFactor = (((String)settings.data_life_units ?: UNIT_WEEKS) == UNIT_WEEKS ? 7.0 : 1.0)
			try {
				dataNew[dataType]["value"] = dataValue?.split(',')?.collect { String lifeValue ->
					Math.round(Double.parseDouble(lifeValue.trim()) * lifeFactor)?.toInteger()
				}?.join(", ")
			} catch (e) {
				dataNew[dataType]["value"] = dataValue
				dataNew[dataType]["error"] = (String)"Error trying to convert Battery Life value of $dataValue. Only numbers (with comma separators) are supported: " + e.toString()
			}
		}
	}
	return dataNew
}


@CompileStatic
void deletePreMigrationDeviceDataAll(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.each { it ->
		data[it.key] = deletePreMigrationDeviceData(it.value)
	}
	setDataStore(dataStore, data)
}
HashMap<String,HashMap<String,HashMap<String,String>>> deletePreMigrationDeviceData(
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice) {
	if (dataDevice == null || dataDevice.containsKey(STATE_IGNORED) || !dataDevice.containsKey(STATE_CURRENT_MIGRATE))
		return dataDevice
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDeviceNew =
		(HashMap<String,HashMap<String,HashMap<String,String>>>)[device: dataDevice.device]
	getSettingsDeviceWrapperList(dataDevice.device.key.value, dataDevice.device.id.value)?.each { DeviceWrapper device ->
		HashMap<String,String> dataTypeNames = [:]
		dataDevice[STATE_CURRENT_MIGRATE].each { datum ->
			String dataType = datum.key
			String dataName = datum.value.name
			dataTypeNames[dataType] = dataName
			device?.removeDataValue(dataName)
		}
		[STATE_PREVIOUS, STATE_CURRENT, STATE_PENDING].each { String state ->
			if (dataDevice.containsKey(state))
				dataDeviceNew."$state" = dataDevice[state]
		}
		dataDeviceNew[STATE_PREVIOUS_MIGRATE] = dataDevice[STATE_CURRENT_MIGRATE]
	}
	return dataDeviceNew
}


@CompileStatic
void excludeIncludeDeviceDataInPending(String action, String dataStore, String deviceId) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice = data[deviceId]
	if (dataDevice == null) return
	data[deviceId] = excludeIncludeDeviceData(action, dataDevice)
	setDataStore(dataStore, data)
}
@CompileStatic
void excludeIncludeDeviceDataAllInPending(String action, String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> dataNew = [:]
	if (data == null) return
	data.values()?.each { dataDevice ->
		dataNew[dataDevice.device.id.value] = excludeIncludeDeviceData(action, dataDevice)
	}
	setDataStore(dataStore, dataNew)
}
static HashMap<String,HashMap<String,HashMap<String,String>>> excludeIncludeDeviceData(String action,
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice) {
	if (action.startsWith("Exclude"))
		dataDevice."$STATE_IGNORED" = getDeviceDataEmpty()
	else if (action.startsWith("Include"))
		dataDevice.remove(STATE_IGNORED)
	return dataDevice
}


@CompileStatic
HashMap<String,HashMap<String,String>> getDeviceData(
	DeviceWrapper device, HashMap<String,String> dataTypeNames = null, boolean returnNull = false) {
	if (device == null) return [:]
	HashMap<String,String> dataNames = dataTypeNames ?: getDeviceDataTypeNames(true)
	HashMap<String,HashMap<String,String>> data = [:]
	if (dataNames[DATA_PERCENTAGE] != null) {
		Integer percentage = (Integer)device.currentValue("battery")
		data[DATA_PERCENTAGE] = [name: dataNames[DATA_PERCENTAGE], value: percentage?.toString() ?: "0"]
	}
	[DATA_DATE, DATA_LIFE, DATA_TYPE].each { String dataType ->
		String dataName = dataNames[dataType]
		if (dataName == null) return
		String dataValue = device?.getDeviceDataByName(dataName)
		if (dataValue != null || returnNull) data[dataType] = [name: dataName, value: dataValue]
	}
	return data
}
@CompileStatic
static HashMap<String,HashMap<String,String>> getDeviceDataEmpty() {
	return new HashMap<String,HashMap<String,String>>()
}
@CompileStatic
static HashMap<String,HashMap<String,String>> getDeviceIdentity(DeviceWrapper device, String devicesKey) {
	if (device == null) return [:]
	HashMap<String,String> id = (HashMap<String,String>)[value: device.id]
	HashMap<String,String> name = (HashMap<String,String>)[value: device.displayName]
	HashMap<String,String> key = (HashMap<String,String>)[value: devicesKey]
	return [id: id, name: name, key: key]
}
HashMap<String,HashMap<String,String>> getDeviceIdentityByObject(DeviceWrapper device, String objectId) {
	if (device == null) return [:]
	DeviceWrapperList devicesApp = (DeviceWrapperList)settings.app_devices_tracked
	DeviceWrapperList devicesObj = (DeviceWrapperList)settings."${objectId}_devices_tracked"
	String devicesKey =
		devicesApp?.any { it.id == device.id } ? "app_devices_tracked" :
		devicesObj?.any { it.id == device.id } ? "${objectId}_devices_tracked" : null
	return getDeviceIdentity(device, devicesKey)
}


@CompileStatic
static boolean isDeviceDataStateEqualTo(HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice,
	String state1, String state2) {
	if (dataDevice[state1] == null || dataDevice[state2] == null) return false
	for (String dataType in [DATA_DATE, DATA_LIFE, DATA_TYPE]) {
		String valueCurrent = dataDevice[STATE_CURRENT][dataType]?.value ?: "--"
		String value1 = getDeviceDataCompareValue(dataDevice, state1, dataType, valueCurrent)
		String value2 = getDeviceDataCompareValue(dataDevice, state2, dataType, valueCurrent)
		if (value1 != value2) return false
		String name1 = dataDevice[state1][dataType]?.name
		String name2 = dataDevice[state2][dataType]?.name
		if (name1 != null && name2 != null && name1 != name2) return false
	}
	return true
}
@CompileStatic
static String getDeviceDataCompareValue(HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice,
	String state, String dataType, String dataValueCurrent) {
	String value = (dataDevice[state][dataType]?.value ?: "--")
	return (state != STATE_PENDING ? value : (dataDevice[state].containsKey(dataType) ? value : dataValueCurrent))
}


@CompileStatic
static boolean isDeviceDataValid(String dataType, String dataValue, boolean allowNullEmpty = false) {
	if (!allowNullEmpty && (dataValue == null || dataValue.isEmpty()))
		return false
	if (dataType == DATA_DATE) {
		if (!isDateFormat(dataValue))
			return false
	} else if (dataType == DATA_LIFE) {
		if (dataValue?.split(',')?.any { String it -> !it.isNumber() })
			return false
	}
	return true
}


void migrateDeviceDataAllToPending(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	data.values()?.each { dataDevice ->
		if (dataDevice.containsKey(STATE_IGNORED)) return
		if (!dataDevice.containsKey(STATE_PENDING))
			dataDevice."$STATE_PENDING" = getDeviceDataEmpty()

		HashMap<String,HashMap<String,String>> dataMigrated = convertDeviceData(dataDevice[STATE_CURRENT_MIGRATE])
		dataMigrated.each { datum ->
			String dataType = datum.key
			HashMap<String,String> dataTypeMigrated = datum.value
			dataDevice[STATE_PENDING][dataType] = dataTypeMigrated
			if (dataTypeMigrated?.error) logW ":: ${dataDevice.device.name} :: ${dataTypeMigrated?.error}"
		}
	}
	setDataStore(dataStore, data)
}


void removeDeviceDataFromPending(String dataStore, String dataType) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.values()?.each { dataDevice ->
		if (dataDevice.containsKey(STATE_IGNORED)) return
		if (!dataDevice.containsKey(STATE_PENDING))
			dataDevice."$STATE_PENDING" = getDeviceDataEmpty()
		if (dataDevice[STATE_CURRENT][dataType] != null)
			dataDevice[STATE_PENDING][dataType] = [name: getDeviceDataName(dataType), value: ""]
		else if (dataDevice[STATE_PENDING][dataType] != null)
			dataDevice[STATE_PENDING].remove(dataType)
	}
	setDataStore(dataStore, data)
}
@CompileStatic
void removeDeviceDataAllFromPending(String dataStore) {
	removeDeviceDataFromPending(dataStore, DATA_DATE)
	removeDeviceDataFromPending(dataStore, DATA_LIFE)
	removeDeviceDataFromPending(dataStore, DATA_TYPE)
}


void resetDeviceDataInPending(String dataStore, String dataType) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.values()?.each { dataDevice ->
		if (!dataDevice.containsKey(STATE_IGNORED) &&
			 dataDevice.containsKey(STATE_PENDING)) {
			dataDevice[STATE_PENDING].remove(dataType)
			if (dataType == DATA_DATE && settings.data_battery_life_auto == true)
				dataDevice[STATE_PENDING].remove(DATA_LIFE)
		}
	}
	setDataStore(dataStore, data)
}
@CompileStatic
void resetDeviceDataAllInPending(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.values()?.each { dataDevice ->
		if (!dataDevice.containsKey(STATE_IGNORED))
			dataDevice.remove(STATE_PENDING)
	}
	setDataStore(dataStore, data)
}


@CompileStatic
void restoreDeviceDataAllToPending(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.each { it ->
		data[it.key] = restoreDeviceDataToPending(it.value)
	}
	setDataStore(dataStore, data)
}
@CompileStatic
void restoreDeviceDataToPending(String dataStore, String deviceId) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null || data[deviceId] == null) return
	data[deviceId] = restoreDeviceDataToPending(data[deviceId])
	setDataStore(dataStore, data)
}
HashMap<String,HashMap<String,HashMap<String,String>>> restoreDeviceDataToPending(
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice) {
	if (dataDevice == null || dataDevice.containsKey(STATE_IGNORED) || !dataDevice.containsKey(STATE_PREVIOUS))
		return dataDevice
	if (!dataDevice.containsKey(STATE_PENDING))
		dataDevice."$STATE_PENDING" = [:]
	getDeviceDataTypeNames(false).each { dataTypeName ->
		String dataType = dataTypeName.key
		String dataName = dataTypeName.value
		HashMap<String,String> dataTypePrevious = dataDevice[STATE_PREVIOUS][dataType]
		if (dataTypePrevious == null) {
			if (dataDevice[STATE_CURRENT][dataType] != null)
				dataDevice[STATE_PENDING][dataType] = [name: dataName, value: ""]
			else dataDevice[STATE_PENDING].remove(dataType)
		} else dataDevice[STATE_PENDING][dataType] = [name: dataTypePrevious.name, value: dataTypePrevious.value]
	}
	return dataDevice
}


@CompileStatic
void submitDeviceDataInPending(String dataStore) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null) return
	data.each { it ->
		data[it.key] = submitDeviceDataInPending(it.value)
	}
	setDataStore(dataStore, data)
}
@CompileStatic
void submitDeviceDataInPending(String dataStore, String deviceId) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	if (data == null || data[deviceId] == null) return
	data[deviceId] = submitDeviceDataInPending(data[deviceId])
	setDataStore(dataStore, data)
}
HashMap<String,HashMap<String,HashMap<String,String>>> submitDeviceDataInPending(
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDevice) {
	if (dataDevice == null || dataDevice.containsKey(STATE_IGNORED) || !dataDevice.containsKey(STATE_PENDING))
		return dataDevice
	HashMap<String,HashMap<String,HashMap<String,String>>> dataDeviceNew =
		(HashMap<String,HashMap<String,HashMap<String,String>>>)[device: dataDevice.device]
	getSettingsDeviceWrapperList(dataDevice.device.key.value, dataDevice.device.id.value)?.each { DeviceWrapper device ->
		boolean updated = false
		HashMap<String,String> dataTypeNames = [:]
		dataDevice[STATE_PENDING].each { datum ->
			String dataType = datum.key
			String dataName = datum.value.name
			String dataValue = datum.value.value
			dataTypeNames[dataType] = dataName
			HashMap<String,String> dataPrevious = dataDevice[STATE_CURRENT][dataType]

			if (dataValue != dataPrevious?.value || dataName != dataPrevious?.name) {
				updated = true
				if (dataValue == null || dataValue.isEmpty())
					device?.removeDataValue(dataName)
				else if (dataType != DATA_PERCENTAGE)
					device?.updateDataValue(dataName, dataValue)
			}
		}
		[STATE_PREVIOUS_MIGRATE, STATE_CURRENT_MIGRATE].each { String state ->
			if (dataDevice.containsKey(state))
				dataDeviceNew."$state" = dataDevice[state]
		}
		dataDeviceNew."$STATE_CURRENT" = getDeviceData(device, dataTypeNames)
		if (updated)
			dataDeviceNew[STATE_PREVIOUS] = dataDevice[STATE_CURRENT]
	}
	return dataDeviceNew
}


void updateDeviceDataAllInPending(String dataStore) {
	if (settings.data_battery_date != null)
		updateDeviceDataInPending(dataStore, DATA_DATE,
			formatDateTime(formatDateTime((String)settings.data_battery_date, DATETIME_FORMAT_SETTING), DATETIME_FORMAT_DATA))
	if (settings.data_battery_life != null &&
		settings.data_battery_life_auto == false)
		updateDeviceDataInPending(dataStore, DATA_LIFE, (String)settings.data_battery_life)
	if (settings.data_battery_type != null)
		updateDeviceDataInPending(dataStore, DATA_TYPE, (String)settings.data_battery_type)
}
void updateDeviceDataInPending(String dataStore, String dataType, String dataValue) {
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore)
	data.values()?.each { dataDevice ->
		if (dataDevice.containsKey(STATE_IGNORED)) return
		if (!dataDevice.containsKey(STATE_PENDING))
			dataDevice."$STATE_PENDING" = getDeviceDataEmpty()
		dataDevice[STATE_PENDING]."$dataType" = [name: getDeviceDataName(dataType), value: dataValue]
		if (isDeviceDataStateEqualTo(dataDevice, (String)STATE_PENDING, (String)STATE_CURRENT))
			dataDevice.remove(STATE_PENDING)
	}
	setDataStore(dataStore, data)
	if (dataType == DATA_DATE && settings.data_battery_life_auto == true)
		computeBatteryLifespanForPending(dataStore)
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
	unschedule()
	unsubscribe()
	app.updateLabel(getBatteryTrackerLabel())
	cleanupObjects()
	cleanupObjectSchedules()
	deleteSettingsRoot("app_pauseDeviceUntil_")
	state.remove("clonedName")

	initializeBatteryLevels()
	initializeNotificationDates()
	subscribeTimes()
	subscribeDate()
	handlerUpdateDate()

	refresh()
}


void refresh() {
	logI "[REFRESH] nothing to refresh"
}


//=========================================================================
// Application Status:  Processing
//=========================================================================

void subscribeDate() {
	schedule('0 0 0 ? * *', "handlerUpdateDate")
}


void handlerUpdateDate() {
	String today = getDateToday(DATETIME_FORMAT_SETTING)
	app.updateSetting("data_battery_date", [type: "date", value: today])
	app.removeSetting("data_devices_battery")
	deleteDataStore(STORE_MANUAL)
}


void subscribeTimes() {
	getObjectIdsAll()?.each { String objectId ->
		Date dt = toDateTime(getObjectScheduleTime(objectId))
		Map data = [objectType: parseObjectIdType(objectId), objectId: objectId]
		schedule(dt, "handlerTime", [data: data, overwrite: false])
	}
}


void handlerTime(Map data) {
	String objectId = (String)data.objectId
	List<String> daysOfWeek = getObjectScheduleDays(objectId)
	if (!daysOfWeek.contains(getDayOfWeek())) return

	String objectType = (String)data.objectType
	HashMap<String,String> om = OBJECT_MAP[objectType]

	logD "[TIME] ${getObjectDescription(om, objectId).fullS}"
	runBatteryTracker(objectType, objectId)
}
void runBatteryTrackersManually(String objectType) {
	HashMap<String,String> om = OBJECT_MAP[objectType]
	getObjectIds(om)?.each { String objectId ->
		runBatteryTrackerManually(om, objectId, true)
	}
}
List<Map<String,String>>  runBatteryTrackerManually(Map<String,String> om, String objectId, boolean isNotify = true) {
	logD "[MANUAL] ${getObjectDescription(om, objectId).fullS}"
	if (requiresBatteryLevelsState(om.type) &&
		getBatteryLevels(om.type, objectId) == null) {
		initializeBatteryLevels()
	}
	return runBatteryTracker(om.type, objectId, isNotify)
}
List<Map<String,String>> runBatteryTracker(String objectType, String objectId, boolean isNotify = true) {
	List<DeviceWrapper> devicesBattery = getSettingsDevicesList(objectId, "tracked", true)
	List<Map<String,String>> deviceAlerts = []

	switch (objectType) {
		case OBJECT_LAST:
			deviceAlerts = handleLastActivity(objectId, devicesBattery)
			break
		case OBJECT_LOW:
			deviceAlerts = handleBatteryThreshold(objectId, true, devicesBattery)
			break
		case OBJECT_NEW:
			setStateConfigureValue("today", getDateToday())
			setStateConfigureValue("todayMs", getDateTodayMs().toString())
			deviceAlerts = handleBatteryThreshold(objectId, false, devicesBattery)
			break
		case OBJECT_OLD:
			deviceAlerts = handleOldBattery(objectId, devicesBattery)
			break
	}
	List<DeviceWrapper> devicesNotes = (!isNotify || deviceAlerts.isEmpty()) ? [] :
		getSettingsDevicesList(objectId, "notification", false)

	deviceAlerts?.each { Map<String,String> deviceAlert ->
		if ("warn" == deviceAlert.msgLevel)
			logW ":: " + deviceAlert.msg
		else logD ":: " + deviceAlert.msg

		devicesNotes?.each { DeviceWrapper deviceNote ->
			if (!deviceNote.isDisabled())
				deviceNote.deviceNotification(deviceAlert.msg)
		}
	}
	return deviceAlerts
}


List<Map<String,String>> handleBatteryThreshold(String objectId, boolean isLowCheck, List<DeviceWrapper> devicesBattery) {
	List<Map<String,String>> deviceAlerts = []
	Integer thresholdAbsolute = settings."${objectId}_threshold_absolute"
	Integer thresholdRelative = settings."${objectId}_threshold_relative"
	devicesBattery?.each { DeviceWrapper device ->
		if (device.isDisabled()) return
		Map<String,String> deviceAlert
		HashMap<String,HashMap<String,String>> dataNow = getDeviceData(device)
		try {
			deviceAlert = deviceBatteryIssues(device, dataNow) ?: isLowCheck ?
				handleLowBatteryThreshold(objectId, device, dataNow, thresholdAbsolute, thresholdRelative) :
				handleNewBatteryThreshold(objectId, device, dataNow, thresholdAbsolute, thresholdRelative)
		} catch (e) {
			deviceAlert = deviceBatteryException(device, dataNow, e)
		}
		if (deviceAlert != null) deviceAlerts.add(deviceAlert)
	}
	return deviceAlerts
}
Map<String,String> handleLowBatteryThreshold(String objectId, DeviceWrapper device,
	HashMap<String,HashMap<String,String>> dataNow,	Integer thresholdAbsolute, Integer thresholdRelative) {
	boolean setBattery = false
	Integer batteryNow = dataNow[DATA_PERCENTAGE]?.value?.toInteger()
	Integer batteryOldHigh = (Integer)getBatteryLevels(OBJECT_LOW, objectId)?."${device.id}"
	if (batteryOldHigh == null) batteryOldHigh = 100
	Integer batteryChange = (batteryOldHigh - batteryNow)
	ArrayList<String> msg = []
	if (thresholdAbsolute != null && batteryNow < thresholdAbsolute) {
		msg.add("level ${batteryNow}% < ${thresholdAbsolute}%")
	}
	if (batteryNow > batteryOldHigh)
		setBattery = true
	else if (thresholdRelative != null && batteryChange > thresholdRelative) {
		msg.add("decrease ${batteryChange}% > ${thresholdRelative}%")
		setBattery = true
	}
	if (setBattery) setBatteryLevel(OBJECT_LOW, objectId, device, batteryNow)

	if (!msg.isEmpty())
		return getDeviceAlert(device, dataNow, batteryNow, -1 * batteryChange, "debug", msg)
	return null
}
Map<String,String> handleNewBatteryThreshold(String objectId, DeviceWrapper device,
	HashMap<String,HashMap<String,String>> dataNow, Integer thresholdAbsolute, Integer thresholdRelative) {
	boolean setBattery = false
	Integer batteryNow = dataNow[DATA_PERCENTAGE]?.value?.toInteger()
	Integer batteryOldLow = (Integer)getBatteryLevels(OBJECT_NEW, objectId)?."${device.id}" ?: 0
	Integer batteryChange = (batteryNow - batteryOldLow)
	ArrayList<String> msg = []
	if (batteryNow > batteryOldLow) {
		if (thresholdAbsolute != null &&
			batteryNow > thresholdAbsolute && batteryOldLow <= thresholdAbsolute) {
			msg.add("level ${batteryNow}% > ${thresholdAbsolute}%")
			setBattery = true
		}
		if (thresholdRelative != null) {
			if (batteryChange > thresholdRelative) {
				msg.add("increase ${batteryChange}% > ${thresholdRelative}%")
				setBattery = true
			}
		}
	} else setBattery = true

	if (setBattery) setBatteryLevel(OBJECT_NEW, objectId, device, batteryNow)

	if (!msg.isEmpty()) {
		Long todayMs = getStateConfigureValueString("todayMs").toLong()
		Long suppressDurationMs = ((Long)settings."${objectId}_intervalDaysSuppress" ?: 0) * 86400 * 1000
		String lateNoteDate = getNotificationDates(objectId)?."${device.id}" ?: "2000/01/01"
		Long lastNoteMs = new SimpleDateFormat(DATETIME_FORMAT_DATA)?.parse(lateNoteDate)?.getTime() ?: 0
		Long batteryAgeMs = getBatteryAgeMs(device, (new Date()).getTime()) ?: todayMs
		if (batteryAgeMs >= suppressDurationMs && todayMs - lastNoteMs >= suppressDurationMs) {
			storeDeviceData(objectId, device, batteryOldLow)
			setNotificationDate(objectId, device, getStateConfigureValueString("today"))
			return getDeviceAlert(device, dataNow, batteryNow, batteryChange, "debug", msg)
		}
		//jdc: TEMPORARY
		else return getDeviceAlert(device, dataNow, batteryNow, batteryChange, "filtered", msg)
	}
	return null
}
void storeDeviceData(String objectId, DeviceWrapper device, Integer batteryPrevious) {
	if (settings."${objectId}_observe" != true) return
	boolean record = (settings."${objectId}_record" == true)
	String dataStore = (record) ? STORE_RECORDED : STORE_OBSERVED
	HashMap<String,HashMap<String,String>> dataDevice = getDeviceIdentityByObject(device, objectId)
	HashMap<String,HashMap<String,String>> dataNow = getDeviceData(device)
	HashMap<String,HashMap<String,String>> dataNew = getDeviceData(device)
    dataNow[DATA_PERCENTAGE] = [name: DATA_PERCENTAGE, value: batteryPrevious.toString()]
	dataNew[DATA_DATE] = [name: getDeviceDataName(DATA_DATE), value: getDateToday()]
	HashMap<String,String> life = computeBatteryLifespanForPending(getDeviceDataName(DATA_LIFE), dataNow, dataNew)
	if (life != null) {
		if (life.containsKey("value") && !life["value"].isEmpty())
			dataNew[DATA_LIFE] = life
		if (life.containsKey("error"))
			logW ":: ${dataDevice.device.name} :: cannot compute Battery Lifespan: ${life["error"]}"
	}
	HashMap<String,HashMap<String,HashMap<String,HashMap<String,String>>>> data = getDataStore(dataStore, true)
	data[device.id] = [device: dataDevice, (STATE_CURRENT): dataNow, (STATE_PENDING): dataNew]
	setDataStore(dataStore, data)
	if (record) submitDeviceDataInPending(dataStore)
}


List<Map<String,String>> handleLastActivity(String objectId, List<DeviceWrapper> devicesBattery) {
	List<Map<String,String>> deviceAlerts = []
	Long days = settings."${objectId}_intervalDays" ?: 0
	Long hours = settings."${objectId}_intervalHours" ?: 0
	Long minutes = settings."${objectId}_intervalMinutes" ?: 0
	Long thresholdMilliseconds = (minutes + hours * 60 + days * 1440) * 60000
	Long thresholdTimeMilliseconds = (new Date()).getTime() - thresholdMilliseconds
	devicesBattery?.each { DeviceWrapper device ->
		if (device.isDisabled()) return
		if (device.getLastActivity()?.getTime() <= thresholdTimeMilliseconds) {
			String lastActivity = device.getLastActivity()?.format(DATETIME_FORMAT_LAST_ACTIVITY, location.timeZone)
			String lastActivityNote = device.getLastActivity()?.format(DATETIME_FORMAT_LAST_ACTIVITY_NOTE, location.timeZone)
			String msg = (lastActivity == null) ? "has not reported activity" : "without activity since $lastActivityNote"
			Map<String,String> deviceAlert = [(REPORT_LAST_ACTIVITY): lastActivity ?: ""]
			deviceAlerts.add(getDeviceAlert(deviceAlert, device, "debug", msg))
		}
	}
	return deviceAlerts
}


List<Map<String,String>> handleOldBattery(String objectId, List<DeviceWrapper> devicesBattery) {
	List<Map<String,String>> deviceAlerts = []
	Long days = settings."${objectId}_intervalDays" ?: 0
	String intervalRelative = settings."${objectId}_intervalRelative"
	Long intervalFactor = (intervalRelative == RELATIVE_AT) ? 0 :
		(intervalRelative == RELATIVE_AFTER) ? 1 : -1
	Long intervalMilliseconds = days * 1440 * 60000 * intervalFactor
	Long nowMilliseconds = (new Date()).getTime()
	devicesBattery?.each { DeviceWrapper device ->
		if (device.isDisabled()) return
		Long batteryLifeAvgMilliseconds = getBatteryLifeAverage(device)
		Long batteryAgeMilliseconds = getBatteryAgeMs(device, nowMilliseconds)
		if (batteryAgeMilliseconds == null || batteryLifeAvgMilliseconds == null) return
		if (batteryAgeMilliseconds > batteryLifeAvgMilliseconds + intervalMilliseconds) {
			String batteryAge = getBatteryAge(batteryAgeMilliseconds)
			String batteryAgeAlert = getBatteryAge(batteryLifeAvgMilliseconds + intervalMilliseconds)
			String msg = "battery age ($batteryAge days) is older than the threshold ($batteryAgeAlert days)"
			Map<String,String> deviceAlert = [
				(REPORT_AGE_CURRENT): "$batteryAge days",
				(REPORT_AGE_ALERT): "$batteryAgeAlert days",
			]
			deviceAlerts.add(getDeviceAlert(deviceAlert, device, "debug", msg))
		}
	}
	return deviceAlerts
}
@CompileStatic
static String getBatteryAge(Long milliseconds, String units = UNIT_DAYS) {
	Long msPerUnit = (units == UNIT_WEEKS) ? 604800000 : 86400000
	return Math.round((Double)((Double)milliseconds / msPerUnit)).toString()
}
@CompileStatic
Long getBatteryAgeMs(DeviceWrapper device, Long nowMilliseconds, boolean warn = false) {
	String batteryDate = device.getDeviceDataByName(getDeviceDataName(DATA_DATE))
	if (batteryDate == null) {
		if (warn) logW "${device.displayName} does not have Battery Date in its Data"
		return null
	}
	try {
		Date batteryDateDt = new SimpleDateFormat(DATETIME_FORMAT_DATA)?.parse(batteryDate)
		Long batteryDateMilliseconds = batteryDateDt?.getTime()
		if (batteryDateMilliseconds == null || nowMilliseconds == null) return null
		return nowMilliseconds - batteryDateMilliseconds
	} catch (e) {
		logW "Exception trying to parse ${device.displayName} BatteryDate of $batteryDate.  Expected format is \"$DATETIME_FORMAT_DATA\"."
		logW e.toString()
		return null
	}
}
@CompileStatic
Long getBatteryLifeAverage(DeviceWrapper device) {
	String batteryLifeDays = device.getDeviceDataByName(getDeviceDataName(DATA_LIFE))
	List<String> lifeDays = batteryLifeDays?.split(",")?.toList()
	if (lifeDays == null) return null
	if (lifeDays.any { !it.isNumber() }) {
		logW ":: ${device.displayName} :: Data for Battery Life includes non-numerical values in the CSV."
		return null
	}
	Double lifeDaysAvg = average(lifeDays.collect { Double.parseDouble(it) })
	return daysToMs(lifeDaysAvg)
}


@CompileStatic
static Map<String,String> getDeviceAlert(DeviceWrapper device, HashMap<String,HashMap<String,String>> dataNow,
	Integer batteryNow, Integer batteryChange, String msgLevel, List<String> msg) {
	return [
		(REPORT_ID): device.id,
		(REPORT_NAME): device.displayName,
		(REPORT_TYPE): dataNow[DATA_TYPE]?.value,
		(REPORT_PERCENTAGE): batteryNow.toString() + "%",
		(REPORT_PERCENTAGE_CHANGE): printBatteryPercentageChange(batteryChange),
		(REPORT_MESSAGE): (String)("${device.displayName}${printBatteryType(dataNow)} battery " + msg.join(", ")),
		"msgLevel": msgLevel,
	]
}
@CompileStatic
static Map<String,String> getDeviceAlert(DeviceWrapper device, HashMap<String,HashMap<String,String>> dataNow,
	Integer batteryNow, String msgLevel, String msg) {
	Map<String,String> deviceAlert = [
		(REPORT_ID): device.id,
		(REPORT_NAME): device.displayName,
		(REPORT_TYPE): dataNow[DATA_TYPE]?.value,
		(REPORT_MESSAGE): msg,
		"msgLevel": msgLevel,
	]
	if (batteryNow != null) deviceAlert[(REPORT_PERCENTAGE)] = batteryNow.toString() + "%"
	return deviceAlert
}
@CompileStatic
Map<String,String> getDeviceAlert(Map<String,String> deviceAlert, DeviceWrapper device, String msgLevel, String msg) {
	HashMap<String,HashMap<String,String>> dataNow = getDeviceData(device)
	deviceAlert[REPORT_ID] = device.id
	deviceAlert[REPORT_NAME] = device.displayName
	deviceAlert[REPORT_TYPE] = dataNow[DATA_TYPE]?.value
	deviceAlert[REPORT_MESSAGE] = (String)("${device.displayName}${printBatteryType(dataNow)} " + msg)
	deviceAlert["msgLevel"] = msgLevel
	return deviceAlert
}

@CompileStatic
static String printBatteryPercentageChange(Integer change) {
	String chg = "${Math.abs(change)}%"
	return (change == 0) ? chg : (change > 0) ? "+ $chg" : "— $chg"
}
@CompileStatic
static String printBatteryType(HashMap<String,HashMap<String,String>> dataNow) {
	return (dataNow[DATA_TYPE]?.value != null ? " (${dataNow[DATA_TYPE]?.value})" : "")
}

@CompileStatic
static Map<String,String> deviceBatteryException(DeviceWrapper device, HashMap<String,HashMap<String,String>> dataNow, Exception e) {
	String msg = "Caught an exception checking the battery value for ${device.displayName} :: " + e.toString()
	return getDeviceAlert(device, dataNow, null, "warn", msg)
}
@CompileStatic
static Map<String,String> deviceBatteryIssues(DeviceWrapper device, HashMap<String,HashMap<String,String>> dataNow) {
	String msg = null
	Integer deviceBattery = dataNow[DATA_PERCENTAGE]?.value?.toInteger()
	if (deviceBattery == null) msg = "${device.displayName} is not reporting battery level"
	if (deviceBattery > 100) msg = "${device.displayName} battery level ${deviceBattery}% exceeds 100"
	if (msg != null)  return getDeviceAlert(device, dataNow, deviceBattery, "warn", msg)
	return null
}


//=========================================================================
// Utilities
//=========================================================================

@CompileStatic
static double average(List<Double> list) {
	Double sum = (Double)0.0
	if (!list.isEmpty()) {
		list.each { sum += it }
		return sum.doubleValue() / list.size()
	}
	return sum
}


@CompileStatic
static ArrayList<Integer> cloneIntegerArrayList(Object list) {
	if (list == null) return null
	return ((ArrayList<Integer>)((ArrayList<Integer>)list)?.clone())
}


@CompileStatic
static Long daysToMs(Double days) {
	// milliseconds = days * 24 * 60 * 60 * 1000
	return (days * (Double)86400000).toLong()
}


@CompileStatic
static int findNextIntegerInCollection(List<Integer> list, boolean findLowestNumber = true) {
	if (list == null) return 0
	Integer numNew = -1
	if (findLowestNumber) {
		List<Integer> listCopy = cloneIntegerArrayList(list)
		listCopy.sort().eachWithIndex { Integer num, Integer idx ->
			if (numNew < 0 && num.toInteger() > idx) numNew = idx
		}
		if (numNew < 0) numNew = listCopy.size()
	} else numNew = Collections.max(list) + 1

	return numNew
}

@CompileStatic
static String formatDateTime(Date dt, String format = "yyyy/MM/dd hh:mm a") {
	Calendar c = Calendar.getInstance()
	c.setTime(dt)
	SimpleDateFormat dateFmt = new SimpleDateFormat(format)
	return dateFmt.format(c.getTime())
}
@CompileStatic
static Date formatDateTime(String dt, String format = "yyyy/MM/dd hh:mm a") {
	SimpleDateFormat dateFmt = new SimpleDateFormat(format)
	Date dateTime = dateFmt.parse(dt)
	return dateTime
}
@CompileStatic
static String formatTime(Date dt) {
	Calendar c = Calendar.getInstance()
	c.setTime(dt)
	SimpleDateFormat dateFmt = new SimpleDateFormat("hh:mm a")
	return dateFmt.format(c.getTime())
}


@CompileStatic
static String getDateToday(String format = DATETIME_FORMAT_DATA) {
	Calendar c = Calendar.getInstance()
	SimpleDateFormat dateFmt = new SimpleDateFormat(format)
	return dateFmt.format(c.getTime())
}
@CompileStatic
static Long getDateTodayMs() {
	Calendar c = Calendar.getInstance()
	c.set(Calendar.HOUR_OF_DAY, 0)
	c.set(Calendar.MINUTE, 0)
	c.set(Calendar.SECOND, 0)
	c.set(Calendar.MILLISECOND, 0)
	return c.getTimeInMillis()
}


@CompileStatic
static String getDayOfWeek() {
	Calendar c = Calendar.getInstance()
	SimpleDateFormat dateFmt = new SimpleDateFormat("EEEEE")
	return dateFmt.format(c.getTime())
}


@CompileStatic
static boolean isDateFormat(String dt, String format = DATETIME_FORMAT_DATA) {
	SimpleDateFormat dateFormat = new SimpleDateFormat(format)
	dateFormat.setLenient(false)
	try {
		dateFormat.parse(dt)
		return true
	} catch (ignored) {
		return false
	}
}


void log(String level, String msg) {
	log."$level"("Battery Tracker $msg")
}
void logD(String msg) {
	if (settings.enableLogging == true) log "debug", msg
}
@CompileStatic
void logI(String msg) {
	log "info", msg
}
@CompileStatic
void logW(String msg) {
	log "warn", msg
}


//=========================================================================
// Constants
//=========================================================================

@CompileStatic
static List<String> getStatesAll() {
	return [STATE_PREVIOUS_MIGRATE, STATE_CURRENT_MIGRATE, STATE_PREVIOUS, STATE_CURRENT, STATE_PENDING]
}
@Field public static final String CAPABILITY_ALL = "*"
@Field public static final String CAPABILITY_BATTERY = "battery"
@Field public static final String CAPABILITY_NOTIFICATION = "notification"
@Field public static final String COLOR_CURRENT = "black"
@Field public static final String COLOR_CURRENT_MIGRATE = "#042C6B"
@Field public static final String COLOR_ERROR = "#F57940"
@Field public static final String COLOR_IGNORED = "grey"
@Field public static final String COLOR_PENDING = "green"
@Field public static final String COLOR_PREVIOUS = "firebrick"
@Field public static final String COLOR_PREVIOUS_MIGRATE = "grey"
@Field public static final String DATA_DATE = "batteryDate"
@Field public static final String DATA_INCLUDE = "Include"
@Field public static final String DATA_LIFE = "batteryLifeDays"
@Field public static final String DATA_NAME = "Name"
@Field public static final String DATA_PERCENTAGE = "%"
@Field public static final String DATA_STATE = "Status"
@Field public static final String DATA_TYPE = "batteryType"
@Field public static final String DIRECTION_DECREMENT = "DECREMENT"
@Field public static final String DIRECTION_INCREMENT = "INCREMENT"
@Field public static final String DATETIME_FORMAT_DATA = "yyyy/MM/dd"
@Field public static final String DATETIME_FORMAT_PAUSE = "MMM dd, yyyy"
@Field public static final String DATETIME_FORMAT_SETTING = "yyyy-MM-dd"
@Field public static final String DATETIME_FORMAT_LAST_ACTIVITY = "MMM dd, yyyy @ HH:mm"
@Field public static final String DATETIME_FORMAT_LAST_ACTIVITY_NOTE = "E, MMM dd @ HH:mm"
@Field public static final String HREF_INSTRUCTIONS_SET = "Click to set"
@Field public static final String ICON_ALERT = "he-warning1 text-xl"
@Field public static final String ICON_CALENDAR = "he-calendar1"
@Field public static final String ICON_CHECKED = "he-checkbox-checked"
@Field public static final String ICON_CLOCK = "he-clock"
@Field public static final String ICON_INFO = "fa-regular fa-circle-info text-xl"
@Field public static final String ICON_LINK_EXT = "pi pi-external-link"
@Field public static final String ICON_SETTINGS = "he-settings1"
@Field public static final String ICON_UNCHECKED = "he-checkbox-unchecked"
@Field public static final String OBJECT_ID_APP = "app"
@Field public static final String OBJECT_LAST = "LAN"
@Field public static final String OBJECT_LOW = "LBN"
@Field public static final String OBJECT_NEW = "NBN"
@Field public static final String OBJECT_OLD = "OBN"
@Field public static final String RELATIVE_AFTER = "above"
@Field public static final String RELATIVE_AT = "at"
@Field public static final String RELATIVE_BEFORE = "below"
@Field public static final String REPORT_AGE_ALERT = "ageAlert"
@Field public static final String REPORT_AGE_AVG = "ageAvg"
@Field public static final String REPORT_AGE_CURRENT = "ageCurrent"
@Field public static final String REPORT_DATE = "dt"
@Field public static final String REPORT_LAST_ACTIVITY = "activityLast"
@Field public static final String REPORT_ID = "id"
@Field public static final String REPORT_INFO = "info"
@Field public static final String REPORT_MESSAGE = "msg"
@Field public static final String REPORT_NAME = "name"
@Field public static final String REPORT_NAME_LINK = "nameLink"
@Field public static final String REPORT_PAUSE = "pause"
@Field public static final String REPORT_PERCENTAGE = "%"
@Field public static final String REPORT_PERCENTAGE_CHANGE = "change%"
@Field public static final String REPORT_TIME_TO_AVG = "tmToAgeAvg"
@Field public static final String REPORT_TYPE = "type"
@Field public static final String REPORT_UNPAUSE_DATE = "unpauseDate"
@Field public static final String STATE_CURRENT = "Current"
@Field public static final String STATE_CURRENT_MIGRATE = "Pre-Migration"
@Field public static final String STATE_IGNORED = "Ignored"
@Field public static final String STATE_PENDING = "Pending"
@Field public static final String STATE_PREVIOUS = "Previous"
@Field public static final String STATE_PREVIOUS_MIGRATE = "Deleted"
@Field public static final String STORE_MANUAL = "dataManual"
@Field public static final String STORE_MIGRATE = "dataMigrate"
@Field public static final String STORE_OBSERVED = "dataObserved"
@Field public static final String STORE_RECORDED = "dataRecorded"
@Field public static final String UNIT_DAYS = "Days"
@Field public static final String UNIT_WEEKS = "Weeks"
