/*
 * Enhanced Controller for Onkyo and Pioneer
 * Copyright (C) 2018-2021 by Mikhail Kulesh
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
 * Select Network Service directly only when NET selector is selected.
 */
public class NetworkServiceMsg extends ISCPMessage
{
    public final static String CODE = "NSV";

    private final ServiceType service;

    public NetworkServiceMsg(@NonNull final ServiceType service)
    {
        super(0, null);
        this.service = service;
    }

    public NetworkServiceMsg(@NonNull final String name)
    {
        super(0, null);
        this.service = searchByName(name);
    }

    public NetworkServiceMsg(NetworkServiceMsg other)
    {
        super(other);
        service = other.service;
    }

    public ServiceType getService()
    {
        return service;
    }

    @NonNull
    @Override
    public String toString()
    {
        return CODE + "[" + service.toString() + "/" + service.getCode() + "]";
    }

    @Override
    public EISCPMessage getCmdMsg()
    {
        final String param = service.getCode() + "0";
        return new EISCPMessage(CODE, param);
    }

    private ServiceType searchByName(@NonNull final String name)
    {
        for (ServiceType t : ServiceType.values())
        {
            if (t.getName().toUpperCase().equals(name.toUpperCase()))
            {
                return t;
            }
        }
        return ServiceType.UNKNOWN;
    }
}

 
