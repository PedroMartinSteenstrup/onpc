/*
 * Enhanced Music Controller
 * Copyright (C) 2019-2023 by Mikhail Kulesh
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General
 * Public License along with this program.
 */
// @dart=2.9
import 'dart:collection';

import "../../constants/Strings.dart";
import "../../utils/Logging.dart";
import "../messages/CenterLevelCommandMsg.dart";
import "../messages/DeviceNameMsg.dart";
import "../messages/EnumParameterMsg.dart";
import "../messages/FirmwareUpdateMsg.dart";
import "../messages/FriendlyNameMsg.dart";
import "../messages/GoogleCastVersionMsg.dart";
import "../messages/InputSelectorMsg.dart";
import "../messages/ListeningModeMsg.dart";
import "../messages/PowerStatusMsg.dart";
import "../messages/ReceiverInformationMsg.dart";
import "../messages/SubwooferLevelCommandMsg.dart";
import "../messages/ToneCommandMsg.dart";

class ReceiverInformation
{
    static const String BRAND_PIONEER = "Pioneer";

    // From ReceiverInformationMsg
    String _xml;

    String get xml
    => _xml;

    final Map<String, String> _deviceProperties = HashMap<String, String>();

    Map<String, String> get deviceProperties
    => _deviceProperties;

    final List<NetworkService> _networkServices = [];

    List<NetworkService> get networkServices
    => _networkServices;

    final List<Zone> _zones = [];

    List<Zone> get zones
    => _zones;

    final List<Selector> _deviceSelectors = [];

    List<Selector> get deviceSelectors
    => _deviceSelectors;

    final List<Preset> _presetList = [];

    List<Preset> get presetList
    => _presetList;

    final List<String> _controlList = [];
    final Map<String, ToneControl> _toneControls = HashMap<String, ToneControl>();

    Map<String, ToneControl> get toneControls
    => _toneControls;

    // From FriendlyNameMsg, DeviceNameMsg
    String _friendlyName;
    String _deviceName;

    // Power status, from PowerStatusMsg
    PowerStatus _powerStatus;

    PowerStatus get powerStatus
    => _powerStatus;

    // Firmware, from FirmwareUpdateMsg
    EnumItem<FirmwareUpdate> _firmwareStatus;

    EnumItem<FirmwareUpdate> get firmwareStatus
    => _firmwareStatus;

    // Google cast version, from GoogleCastVersionMsg
    String _googleCastVersion;

    String get googleCastVersion
    => _googleCastVersion;

    // Default tone control
    static final ToneControl DEFAULT_BASS_CONTROL = ToneControl(ToneCommandMsg.BASS_KEY, -10, 10, 2);
    static final ToneControl DEFAULT_TREBLE_CONTROL = ToneControl(ToneCommandMsg.TREBLE_KEY, -10, 10, 2);

    ReceiverInformation()
    {
        clear();
    }

    List<String> getQueries(int zone)
    {
        Logging.info(this, "Requesting data for zone " + zone.toString() + "...");
        return [
            ReceiverInformationMsg.CODE,
            FriendlyNameMsg.CODE,
            DeviceNameMsg.CODE,
            PowerStatusMsg.ZONE_COMMANDS[zone],
            FirmwareUpdateMsg.CODE,
            GoogleCastVersionMsg.CODE
        ];
    }

    void clear()
    {
        _xml = "";
        _deviceProperties.clear();
        _networkServices.clear();
        _zones.clear();
        _deviceSelectors.clear();
        _presetList.clear();
        _controlList.clear();
        _friendlyName = null;
        _deviceName = "";
        _powerStatus = PowerStatus.NONE;
        _firmwareStatus = FirmwareUpdateMsg.ValueEnum.defValue;
        _googleCastVersion = Strings.dashed_string;
    }

    void createDefaultReceiverInfo()
    {
        Logging.info(this, "Created default receiver information");

        // By default, add all possible device selectors
        _deviceSelectors.clear();
        InputSelectorMsg.ValueEnum.values.where((e) => e.key != InputSelector.NONE).forEach((it)
        {
            // #265 Add new input selector "SOURCE":
            // "SOURCE" input not allowed for the main zone
            final int zones = it.key == InputSelector.SOURCE ?
                ReceiverInformationMsg.EXT_ZONES : ReceiverInformationMsg.ALL_ZONE;
            final Selector s = Selector(it.getCode, it.description, zones, it.getCode, false);
            _deviceSelectors.add(s);
        });

        // Add default bass and treble limits
        _toneControls.clear();
        _toneControls[ToneCommandMsg.BASS_KEY] = DEFAULT_BASS_CONTROL;
        _toneControls[ToneCommandMsg.TREBLE_KEY] = DEFAULT_TREBLE_CONTROL;
        _toneControls[SubwooferLevelCommandMsg.KEY] =
            ToneControl(SubwooferLevelCommandMsg.KEY, -15, 12, 1);
        _toneControls[CenterLevelCommandMsg.KEY] =
            ToneControl(CenterLevelCommandMsg.KEY, -12, 12, 1);
        // Default zones:
        _zones.clear();
        _zones.addAll(ReceiverInformationMsg.defaultZones);
    }

    bool processReceiverInformation(ReceiverInformationMsg msg)
    {
        _xml = msg.getData;

        _deviceProperties.clear();
        _deviceProperties.addAll(msg.deviceProperties);

        _networkServices.clear();
        _networkServices.addAll(msg.networkServices);

        _zones.clear();
        _zones.addAll(msg.zones);

        _deviceSelectors.clear();
        _deviceSelectors.addAll(msg.deviceSelectors);

        _presetList.clear();
        _presetList.addAll(msg.presetList);

        _controlList.clear();
        _controlList.addAll(msg.controlList);

        _toneControls.clear();
        _toneControls.addAll(msg.toneControls);

        return true;
    }

    bool processFriendlyName(FriendlyNameMsg msg)
    {
        if (_friendlyName == null)
        {
            _friendlyName = "";
        }
        final bool changed = _friendlyName != msg.getFriendlyName;
        _friendlyName = msg.getFriendlyName;
        return changed;
    }

    bool processDeviceName(DeviceNameMsg msg)
    {
        final bool changed = _deviceName != msg.getData;
        _deviceName = msg.getData;
        return changed;
    }

    bool processPowerStatus(PowerStatusMsg msg)
    {
        final bool changed = _powerStatus != msg.getValue.key;
        _powerStatus = msg.getValue.key;
        return changed;
    }

    bool processFirmwareUpdate(FirmwareUpdateMsg msg)
    {
        final bool changed = _firmwareStatus.key != msg.getStatus.key;
        _firmwareStatus = msg.getStatus;
        return changed;
    }

    bool processGoogleCastVersion(GoogleCastVersionMsg msg)
    {
        final bool changed = _googleCastVersion != msg.getData;
        _googleCastVersion = msg.getData;
        return changed;
    }

    String _getProperty(final String prop)
    {
        final String m = _deviceProperties[prop];
        return m == null ? "" : m;
    }

    String get brand
    => _getProperty("brand");

    String get model
    => _getProperty("model");

    String get year
    => _getProperty("year");

    String get firmaware
    => _getProperty("firmwareversion");

    String getIdentifier()
    {
        String identifier = _getProperty("macaddress");
        if (identifier.isEmpty)
        {
            identifier = _getProperty("deviceserial");
        }
        return identifier;
    }

    String getDeviceName(bool useFriendlyName)
    {
        if (useFriendlyName)
        {
            // name from FriendlyNameMsg (NFN)
            if (_friendlyName != null && _friendlyName.isNotEmpty)
            {
                return _friendlyName;
            }
            // fallback to ReceiverInformationMsg
            final String name = _deviceProperties["friendlyname"];
            if (name != null && name.isNotEmpty)
            {
                return name;
            }
        }
        // fallback to model from ReceiverInformationMsg
        return model;
    }

    bool get isOn
    => powerStatus == PowerStatus.ON;

    NetworkService getNetworkService(String id)
    => _networkServices.firstWhere((s) => s.getId == id, orElse: () => null);

    Preset getPreset(int preset)
    => _presetList.firstWhere((p) => p.getId == preset, orElse: () => null);

    int nextEmptyPreset()
    => _presetList.firstWhere((p) => p.isEmpty, orElse: () => Preset(0xFF, 0, "0", "")).getId;

    bool isControlExists(final String control)
    => _controlList.isNotEmpty && _controlList.contains(control);

    bool isListeningModeControl()
    => _controlList.firstWhere((s) => s.startsWith(ListeningModeMsg.CODE), orElse: () => null) != null;

    bool get isReceiverInformation
    => _xml != null && _xml.isNotEmpty;

    bool get isFriendlyName
    => _friendlyName != null;

}

