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

import com.mkulesh.onpc.R;
import com.mkulesh.onpc.iscp.EISCPMessage;
import com.mkulesh.onpc.iscp.ISCPMessage;
import com.mkulesh.onpc.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/*
 * HDMI CEC settings
 */
public class HdmiCecMsg extends ISCPMessage
{
    public final static String CODE = "CEC";

    public enum Status implements DcpStringParameterIf
    {
        NONE("N/A", R.string.device_two_way_switch_none),
        OFF("00", R.string.device_two_way_switch_off),
        ON("01", R.string.device_two_way_switch_on),
        TOGGLE("UP", R.string.device_two_way_switch_toggle);

        final String code;

        @StringRes
        final int descriptionId;

        Status(final String code, @StringRes final int descriptionId)
        {
            this.code = code;
            this.descriptionId = descriptionId;
        }

        public String getCode()
        {
            return code;
        }

        @NonNull
        public String getDcpCode()
        {
            return name();
        }

        @StringRes
        public int getDescriptionId()
        {
            return descriptionId;
        }
    }

    private final Status status;

    HdmiCecMsg(EISCPMessage raw) throws Exception
    {
        super(raw);
        status = (Status) searchParameter(data, Status.values(), Status.NONE);
    }

    public HdmiCecMsg(Status level)
    {
        super(0, null);
        this.status = level;
    }

    public Status getStatus()
    {
        return status;
    }

    @NonNull
    @Override
    public String toString()
    {
        return CODE + "[" + status.toString() + "]";
    }

    @Override
    public EISCPMessage getCmdMsg()
    {
        return new EISCPMessage(CODE, status.getCode());
    }

    @Override
    public boolean hasImpactOnMediaList()
    {
        return false;
    }

    public static Status toggle(Status s, Utils.ProtoType proto)
    {
        return proto == Utils.ProtoType.ISCP ? Status.TOGGLE :
                ((s == Status.OFF) ? Status.ON : Status.OFF);
    }

    /*
     * Denon control protocol
     */
    private final static String DCP_COMMAND = "SSHOS";
    private final static String DCP_COMMAND_EXT = "CON";

    @NonNull
    public static ArrayList<String> getAcceptedDcpCodes()
    {
        return new ArrayList<>(Collections.singletonList(DCP_COMMAND));
    }

    @Nullable
    public static HdmiCecMsg processDcpMessage(@NonNull String dcpMsg)
    {
        final Status s = (Status) searchDcpParameter(
                DCP_COMMAND + DCP_COMMAND_EXT, dcpMsg, Status.values());
        return s != null ? new HdmiCecMsg(s) : null;
    }

    @Nullable
    @Override
    public String buildDcpMsg(boolean isQuery)
    {
        // A space is needed for this command
        return DCP_COMMAND + (isQuery ? (" " + DCP_MSG_REQ) :
                DCP_COMMAND_EXT + " " + status.getDcpCode());
    }
}
