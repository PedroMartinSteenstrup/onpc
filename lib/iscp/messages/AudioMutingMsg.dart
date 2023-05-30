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
import "../../constants/Drawables.dart";
import "../../constants/Strings.dart";
import "../ConnectionIf.dart";
import "../EISCPMessage.dart";
import "EnumParameterMsg.dart";

enum AudioMuting
{
    NONE,
    OFF,
    ON,
    TOGGLE,
}

/*
 * Audio Muting Command
 */
class AudioMutingMsg extends EnumParameterZonedMsg<AudioMuting>
{
    static const String CODE = "AMT";
    static const String ZONE2_CODE = "ZMT";
    static const String ZONE3_CODE = "MT3";
    static const String ZONE4_CODE = "MT4";

    static const List<String> ZONE_COMMANDS = [CODE, ZONE2_CODE, ZONE3_CODE, ZONE4_CODE];

    static const ExtEnum<AudioMuting> ValueEnum = ExtEnum<AudioMuting>([
        EnumItem.code(AudioMuting.NONE, "N/A",
            descrList: Strings.l_audio_muting_none, defValue: true),
        EnumItem.code(AudioMuting.OFF, "00", dcpCode: "OFF",
            descrList: Strings.l_audio_muting_off),
        EnumItem.code(AudioMuting.ON, "01", dcpCode: "ON",
            descrList: Strings.l_audio_muting_on),
        EnumItem.code(AudioMuting.TOGGLE, "TG", dcpCode: "N/A",
            descrList: Strings.l_audio_muting_toggle,
            icon: Drawables.volume_amp_muting)
    ]);

    static final EnumItem<AudioMuting> TOGGLE = ValueEnum.valueByKey(AudioMuting.TOGGLE);

    AudioMutingMsg(EISCPMessage raw) : super(ZONE_COMMANDS, raw, ValueEnum);

    AudioMutingMsg.toggle(int zoneIndex, EnumItem<AudioMuting> s, ProtoType proto) :
            super.output(ZONE_COMMANDS, zoneIndex, _toggle(s.key, proto), ValueEnum);

    AudioMutingMsg.dcp(int zoneIndex, AudioMuting v) : super.output(ZONE_COMMANDS, zoneIndex, v, ValueEnum);

    @override
    bool hasImpactOnMediaList()
    {
        return false;
    }

    static AudioMuting _toggle(AudioMuting s, ProtoType proto)
    => (proto == ProtoType.ISCP) ? AudioMuting.TOGGLE :
        ((s == AudioMuting.OFF) ? AudioMuting.ON : AudioMuting.OFF);

    /*
     * Denon control protocol
     */
    static const List<String> _DCP_COMMANDS = [ "MU", "Z2MU", "Z3MU" ];

    static List<String> getAcceptedDcpCodes()
    => _DCP_COMMANDS;

    static AudioMutingMsg processDcpMessage(String dcpMsg)
    {
        for (int i = 0; i < _DCP_COMMANDS.length; i++)
        {
            final EnumItem<AudioMuting> s = ValueEnum.valueByDcpCommand(_DCP_COMMANDS[i], dcpMsg);
            if (s != null)
            {
                return AudioMutingMsg.dcp(i, s.key);
            }
        }
        return null;
    }

    @override
    String buildDcpMsg(bool isQuery)
    => buildDcpRequest(isQuery, _DCP_COMMANDS);
}
