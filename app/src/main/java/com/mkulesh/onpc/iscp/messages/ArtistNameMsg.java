/*
 * Enhanced Music Controller
 * Copyright (C) 2018-2022 by Mikhail Kulesh
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

import com.mkulesh.onpc.iscp.EISCPMessage;
import com.mkulesh.onpc.iscp.ISCPMessage;

import androidx.annotation.NonNull;

/*
 * NET/USB Artist Name (variable-length, 64 ASCII letters max)
 */
public class ArtistNameMsg extends ISCPMessage
{
    public final static String CODE = "NAT";

    ArtistNameMsg(EISCPMessage raw) throws Exception
    {
        super(raw);
    }

    @NonNull
    @Override
    public String toString()
    {
        return CODE + "[" + data + "]";
    }
}
