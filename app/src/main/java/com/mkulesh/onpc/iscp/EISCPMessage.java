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

package com.mkulesh.onpc.iscp;

import com.mkulesh.onpc.utils.Utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

import androidx.annotation.NonNull;

public class EISCPMessage
{
    private final static String MSG_START = "ISCP";
    private final static String INVALID_MSG = "INVALID";
    public final static int CR = 0x0D;
    public final static int LF = 0x0A;
    private final static int EOF = 0x1A;
    private final static Character START_CHAR = '!';
    private final static int MIN_MSG_LENGTH = 22;
    public final static String QUERY = "QSTN";
    final static int LOG_LINE_LENGTH = 160;

    private final int messageId;
    private final int headerSize, dataSize, version;
    private final Character modelCategoryId;
    private final String code;
    private final String parameters;

    public EISCPMessage(int messageId, byte[] bytes, int startIndex, int headerSize, int dataSize) throws Exception
    {
        this.messageId = messageId;
        this.headerSize = headerSize;
        this.dataSize = dataSize;
        version = getVersion(bytes, startIndex);
        final String body = getRawMessage(bytes, startIndex);

        if (body.length() < 5)
        {
            throw new Exception("Can not decode message body: length " + body.length() + " is invalid");
        }
        if (body.charAt(0) != START_CHAR)
        {
            throw new Exception("Can not find start character in the raw message");
        }

        modelCategoryId = body.charAt(1);
        code = body.substring(2, 5);
        parameters = (body.length() > 5) ? body.substring(5) : "";
    }

    public EISCPMessage(final Character modelCategoryId, final String code, final String parameters)
    {
        messageId = 0;
        headerSize = 16;
        dataSize = 2 + code.length() + parameters.length() + 1;
        version = 1;
        this.modelCategoryId = modelCategoryId;
        this.code = code;
        this.parameters = parameters;
    }

    public EISCPMessage(final String code, final String parameters)
    {
        this('1', code, parameters);
    }

    @NonNull
    @Override
    public String toString()
    {
        String res = MSG_START + "/v" + version + "[" + headerSize + "," + dataSize + "]: " + code + "(";
        if (isMultiline())
        {
            float ln = (float) parameters.length() / (float) LOG_LINE_LENGTH;
            res += (int) Math.ceil(ln);
            res += " lines)";
        }
        else
        {
            res += parameters;
            res += ")";
        }
        return res;
    }

    private boolean isMultiline()
    {
        return parameters.length() > LOG_LINE_LENGTH;
    }

    int getMsgSize()
    {
        return headerSize + dataSize;
    }

    int getMessageId()
    {
        return messageId;
    }

    Character getModelCategoryId()
    {
        return modelCategoryId;
    }

    public String getCode()
    {
        return code;
    }

    String getParameters()
    {
        return parameters;
    }

    static int getMsgStartIndex(byte[] bytes)
    {
        if (bytes.length < MSG_START.length())
        {
            return -1;
        }
        for (int i = 0; i < bytes.length; i++)
        {
            if (bytes[i] == MSG_START.charAt(0) &&
                    bytes[i + 1] == MSG_START.charAt(1) &&
                    bytes[i + 2] == MSG_START.charAt(2) &&
                    bytes[i + 3] == MSG_START.charAt(3))
            {
                return i;
            }
        }
        return -1;
    }

    static int getHeaderSize(byte[] bytes, int startIndex) throws Exception
    {
        // Header Size : 4 bytes after "ISCP"
        if (startIndex + MSG_START.length() + 4 <= bytes.length)
        {
            try
            {
                return ByteBuffer.wrap(bytes, startIndex + MSG_START.length(), 4).getInt();
            }
            catch (Exception e)
            {
                throw new Exception("Can not decode header size: " + e.getLocalizedMessage());
            }
        }
        return -1;
    }

    static int getDataSize(byte[] bytes, int startIndex) throws Exception
    {
        // Data Size : 4 bytes after Header Size
        if (startIndex + MSG_START.length() + 8 <= bytes.length)
        {
            try
            {
                return ByteBuffer.wrap(bytes, startIndex + MSG_START.length() + 4, 4).getInt();
            }
            catch (Exception e)
            {
                throw new Exception("Can not decode data size: " + e.getLocalizedMessage());
            }
        }
        return -1;
    }

    private int getVersion(byte[] bytes, int startIndex) throws Exception
    {
        // Version : 1 byte after Data Size
        try
        {
            if (startIndex + MSG_START.length() + 9 <= bytes.length)
            {
                final byte[] intBytes = new byte[]{ 0, 0, 0, 0 };
                intBytes[3] = bytes[startIndex + MSG_START.length() + 8];
                return ByteBuffer.wrap(intBytes).getInt();
            }
        }
        catch (Exception e)
        {
            throw new Exception("Can not decode version: " + e.getLocalizedMessage());
        }
        return -1;
    }

    private boolean isSpecialCharacter(byte val)
    {
        return val == EOF || val == CR || val == LF;
    }

    private String getRawMessage(byte[] bytes, int startIndex) throws Exception
    {
        try
        {
            if (headerSize > 0 && dataSize > 0 && startIndex + headerSize + dataSize <= bytes.length)
            {
                int actualLength = 0;
                for (int i = 0; i < dataSize; i++)
                {
                    byte val = bytes[startIndex + headerSize + i];
                    if (isSpecialCharacter(val))
                    {
                        break;
                    }
                    actualLength++;
                }
                final byte[] stringBytes = Utils.catBuffer(bytes, startIndex + headerSize, actualLength);
                return new String(stringBytes, Utils.UTF_8);
            }
        }
        catch (Exception e)
        {
            throw new Exception("Can not decode raw message: " + e.getLocalizedMessage());
        }
        return INVALID_MSG;
    }

    byte[] getBytes()
    {
        byte[] parametersBin = parameters.getBytes(Utils.UTF_8);
        int dSize = 2 + code.length() + parametersBin.length + 1;

        if (headerSize + dSize < MIN_MSG_LENGTH)
        {
            return null;
        }
        final byte[] bytes = new byte[headerSize + dSize];
        Arrays.fill(bytes, (byte) 0);

        // Message header
        for (int i = 0; i < MSG_START.length(); i++)
        {
            bytes[i] = (byte) MSG_START.charAt(i);
        }

        // Header size
        byte[] size = ByteBuffer.allocate(4).putInt(headerSize).array();
        System.arraycopy(size, 0, bytes, 4, size.length);

        // Data size
        size = ByteBuffer.allocate(4).putInt(dSize).array();
        System.arraycopy(size, 0, bytes, 8, size.length);

        // Version
        bytes[12] = (byte) version;

        // CMD
        bytes[16] = (byte) START_CHAR.charValue();
        bytes[17] = (byte) modelCategoryId.charValue();
        for (int i = 0; i < code.length(); i++)
        {
            bytes[i + 18] = (byte) code.charAt(i);
        }

        // Parameters
        System.arraycopy(parametersBin, 0, bytes, 21, parametersBin.length);

        // End char
        bytes[21 + parametersBin.length] = (byte) LF;
        return bytes;
    }

    public boolean isQuery()
    {
        return getParameters().equalsIgnoreCase(EISCPMessage.QUERY);
    }
}
