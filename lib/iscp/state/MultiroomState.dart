/*
 * Copyright (C) 2019. Mikhail Kulesh
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

import "../../utils/Logging.dart";
import "../ISCPMessage.dart";
import "../messages/BroadcastResponseMsg.dart";
import "../messages/EnumParameterMsg.dart";
import "../messages/FriendlyNameMsg.dart";
import "../messages/MultiroomChannelSettingMsg.dart";
import "../messages/MultiroomDeviceInformationMsg.dart";

class DeviceInfo
{
    int responses;

    final BroadcastResponseMsg responseMsg;
    String friendlyName;
    MultiroomDeviceInformationMsg groupMsg;
    EnumItem<ChannelType> _channelType;

    DeviceInfo(this.responseMsg)
    {
        responses = 1;
        friendlyName = null;
        groupMsg = null;
        _channelType = MultiroomZone.ChannelTypeEnum.defValue;
    }

    bool processFriendlyName(FriendlyNameMsg msg)
    {
        final bool changed = friendlyName != msg.getFriendlyName;
        friendlyName = msg.getFriendlyName;
        return changed;
    }

    bool processMultiroomDeviceInformation(MultiroomDeviceInformationMsg msg)
    {
        groupMsg = msg;
        return true;
    }

    bool processMultiroomChannelSetting(MultiroomChannelSettingMsg msg)
    {
        final bool changed = _channelType.key != msg.channelType.key;
        _channelType = msg.channelType;
        return changed;
    }

    String getId()
    => responseMsg.getDevice;

    String getDeviceName(bool useFriendlyName)
    {
        final String name = (useFriendlyName) ? friendlyName : null;
        return (name != null) ? name : getId();
    }

    EnumItem<ChannelType> getChannelType(int zone)
    => _channelType.key != MultiroomZone.ChannelTypeEnum.defValue.key ? _channelType :
        (groupMsg != null ? groupMsg.getChannelType(zone) : MultiroomZone.ChannelTypeEnum.defValue);
}

class MultiroomState
{
    // search limit
    static const int MAX_DEVICE_RESPONSE_NUMBER = 5;
    int _searchLimit = MAX_DEVICE_RESPONSE_NUMBER;

    // Multiroom: list of devices
    final Map<String, DeviceInfo> _deviceList = Map();

    Map<String, DeviceInfo> get deviceList
    => _deviceList;

    int get deviceNumber
    => deviceList.length;

    List<String> getQueries()
    {
        return [
            FriendlyNameMsg.CODE,
            MultiroomDeviceInformationMsg.CODE,
            MultiroomChannelSettingMsg.CODE
        ];
    }

    // Update logic
    String _isChange(String type, bool change)
    => change ? type : null;

    String process(ISCPMessage msg)
    {
        if (!getQueries().contains(msg.getCode))
        {
            return null;
        }

        final DeviceInfo di = _deviceList.values.firstWhere((t)
            => t.responseMsg.sourceHost == msg.sourceHost, orElse: () => null);
        if (di == null)
        {
            Logging.info(this, "<< warning: received " + msg.getCode + " from "
                + msg.sourceHost + " for unknown device. Ignored.");
            return null;
        }

        if (msg is FriendlyNameMsg)
        {
            return _isChange(FriendlyNameMsg.CODE, di.processFriendlyName(msg));
        }
        else if (msg is MultiroomDeviceInformationMsg)
        {
            return _isChange(MultiroomDeviceInformationMsg.CODE, di.processMultiroomDeviceInformation(msg));
        }
        else if (msg is MultiroomChannelSettingMsg)
        {
            return _isChange(MultiroomChannelSettingMsg.CODE, di.processMultiroomChannelSetting(msg));
        }

        return null;
    }

    bool processBroadcastResponse(BroadcastResponseMsg msg)
    {
        final String id = msg.getDevice;
        DeviceInfo deviceInfo = _deviceList[id];
        if (deviceInfo == null)
        {
            deviceInfo = DeviceInfo(msg);
            _deviceList[id] = deviceInfo;
            return true;
        }
        else
        {
            deviceInfo.responses++;
        }
        return false;
    }

    void startSearch({bool limited = true})
    {
        _searchLimit = limited ? MAX_DEVICE_RESPONSE_NUMBER : -1;
        _deviceList.clear();
    }

    bool isSearchFinished()
    {
        if (_searchLimit < 0)
        {
            return false;
        }
        for (DeviceInfo di in _deviceList.values)
        {
            if (di.responses < MAX_DEVICE_RESPONSE_NUMBER)
            {
                return false;
            }
        }
        return true;
    }

    List<DeviceInfo> getSortedDevices()
    {
        final List<DeviceInfo> retValue = List();
        deviceList.values.forEach((f) => retValue.add(f));
        retValue.sort((a, b) => a.getId().compareTo(b.getId()));
        return retValue;
    }
}