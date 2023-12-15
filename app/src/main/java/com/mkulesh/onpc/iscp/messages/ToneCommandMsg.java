/*
 * Enhanced Music Controller
 * Copyright (C) 2018-2023 by Mikhail Kulesh
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

package com.mkulesh.onpc.iscp.messages;

import android.annotation.SuppressLint;

import com.mkulesh.onpc.iscp.EISCPMessage;
import com.mkulesh.onpc.iscp.ZonedMessage;
import com.mkulesh.onpc.utils.Logging;
import com.mkulesh.onpc.utils.Utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/*
 * Tone/Front (for main zone) and Tone (for zones 2, 3) command
 */
@SuppressWarnings({ "DuplicateExpressions", "RedundantSuppression" })
public class ToneCommandMsg extends ZonedMessage
{
    final static String CODE = "TFR";
    final static String ZONE2_CODE = "ZTN";
    final static String ZONE3_CODE = "TN3";
    // Tone command is not available for zone 4

    public final static String[] ZONE_COMMANDS = new String[]{ CODE, ZONE2_CODE, ZONE3_CODE };

    public final static int NO_LEVEL = 0xFF;

    private final boolean tonJoined;

    public final static String BASS_KEY = "Bass";
    private final static Character BASS_MARKER = 'B';
    private int bassLevel = NO_LEVEL;

    public final static String TREBLE_KEY = "Treble";
    private final static Character TREBLE_MARKER = 'T';
    private int trebleLevel = NO_LEVEL;

    ToneCommandMsg(EISCPMessage raw) throws Exception
    {
        super(raw, ZONE_COMMANDS);
        tonJoined = true;
        for (int i = 0; i < data.length(); i++)
        {
            if (data.charAt(i) == BASS_MARKER && data.length() > i + 2)
            {
                try
                {
                    if (data.charAt(i + 1) == '+')
                    {
                        bassLevel = Integer.parseInt(data.substring(i + 2, i + 3), 16);
                    }
                    else if (data.charAt(i + 1) == '-')
                    {
                        bassLevel = -Integer.parseInt(data.substring(i + 2, i + 3), 16);
                    }
                    else
                    {
                        bassLevel = Integer.parseInt(data.substring(i + 1, i + 3), 16);
                    }
                }
                catch (Exception e)
                {
                    bassLevel = NO_LEVEL;
                }
            }
            if (data.charAt(i) == TREBLE_MARKER && data.length() > i + 2)
            {
                try
                {
                    if (data.charAt(i + 1) == '+')
                    {
                        trebleLevel = Integer.parseInt(data.substring(i + 2, i + 3), 16);
                    }
                    else if (data.charAt(i + 1) == '-')
                    {
                        trebleLevel = -Integer.parseInt(data.substring(i + 2, i + 3), 16);
                    }
                    else
                    {
                        trebleLevel = Integer.parseInt(data.substring(i + 1, i + 3), 16);
                    }
                }
                catch (Exception e)
                {
                    trebleLevel = NO_LEVEL;
                }
            }
        }
    }

    public ToneCommandMsg(int zoneIndex, int bass, int treble)
    {
        super(0, null, zoneIndex);
        tonJoined = false;
        this.bassLevel = bass;
        this.trebleLevel = treble;
    }

    @Override
    public String getZoneCommand()
    {
        return ZONE_COMMANDS[zoneIndex];
    }

    public boolean isTonJoined()
    {
        return tonJoined;
    }

    public int getBassLevel()
    {
        return bassLevel;
    }

    public int getTrebleLevel()
    {
        return trebleLevel;
    }

    @NonNull
    @Override
    public String toString()
    {
        return getZoneCommand() + "[" + data
                + "; ZONE_INDEX=" + zoneIndex
                + "; BASS=" + bassLevel
                + "; TREBLE=" + trebleLevel + "]";
    }

    @Override
    public EISCPMessage getCmdMsg()
    {
        String par = "";
        if (bassLevel != NO_LEVEL)
        {
            par += Utils.intToneToString(BASS_MARKER, bassLevel);
        }
        if (trebleLevel != NO_LEVEL)
        {
            par += Utils.intToneToString(TREBLE_MARKER, trebleLevel);
        }
        return new EISCPMessage(getZoneCommand(), par);
    }

    @Override
    public boolean hasImpactOnMediaList()
    {
        return false;
    }

    /*
     * Denon control protocol
     */
    public static ToneCommandMsg processDcpMessage(@NonNull String dcpMsg)
    {
        // Bass
        for (int i = 0; i < DcpReceiverInformationMsg.DCP_COMMANDS_BASS.length; i++)
        {
            if (dcpMsg.startsWith(DcpReceiverInformationMsg.DCP_COMMANDS_BASS[i]))
            {
                final String par = dcpMsg.substring(
                        DcpReceiverInformationMsg.DCP_COMMANDS_BASS[i].length()).trim();
                try
                {
                    final int level = Integer.parseInt(par) - DcpReceiverInformationMsg.DCP_TON_SHIFT[i];
                    return new ToneCommandMsg(i, level, NO_LEVEL);
                }
                catch (Exception e)
                {
                    Logging.info(ToneCommandMsg.class, "Unable to parse bass level " + par);
                    return null;
                }
            }
        }
        // Treble
        for (int i = 0; i < DcpReceiverInformationMsg.DCP_COMMANDS_TREBLE.length; i++)
        {
            if (dcpMsg.startsWith(DcpReceiverInformationMsg.DCP_COMMANDS_TREBLE[i]))
            {
                final String par = dcpMsg.substring(
                        DcpReceiverInformationMsg.DCP_COMMANDS_TREBLE[i].length()).trim();
                try
                {
                    final int level = Integer.parseInt(par) - DcpReceiverInformationMsg.DCP_TON_SHIFT[i];
                    return new ToneCommandMsg(i, NO_LEVEL, level);
                }
                catch (Exception e)
                {
                    Logging.info(ToneCommandMsg.class, "Unable to parse treble level " + par);
                    return null;
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    @SuppressLint("DefaultLocale")
    public String buildDcpMsg(boolean isQuery)
    {
        if (isQuery)
        {
            final String bassReq = DcpReceiverInformationMsg.DCP_COMMANDS_BASS[zoneIndex] + " " + DCP_MSG_REQ;
            final String trebleReq = DcpReceiverInformationMsg.DCP_COMMANDS_TREBLE[zoneIndex] + " " + DCP_MSG_REQ;
            return bassReq + DCP_MSG_SEP + trebleReq;
        }
        else if (bassLevel != NO_LEVEL)
        {
            final String par = String.format("%02d", bassLevel + DcpReceiverInformationMsg.DCP_TON_SHIFT[zoneIndex]);
            return DcpReceiverInformationMsg.DCP_COMMANDS_BASS[zoneIndex] + " " + par;
        }
        else if (trebleLevel != NO_LEVEL)
        {
            final String par = String.format("%02d", trebleLevel + DcpReceiverInformationMsg.DCP_TON_SHIFT[zoneIndex]);
            return DcpReceiverInformationMsg.DCP_COMMANDS_TREBLE[zoneIndex] + " " + par;
        }
        return null;
    }

}
