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

import com.mkulesh.onpc.R;
import com.mkulesh.onpc.iscp.EISCPMessage;
import com.mkulesh.onpc.iscp.ISCPMessage;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

/*
 * NET/USB/CD Play Status (3 letters)
 */
public class PlayStatusMsg extends ISCPMessage
{
    public final static String CODE = "NST";
    public final static String CD_CODE = "CST";

    /*
     * Play Status: "S": STOP, "P": Play, "p": Pause, "F": FF, "R": FR, "E": EOF
     */
    public enum PlayStatus implements CharParameterIf
    {
        STOP('S'), PLAY('P'), PAUSE('p'), FF('F'), FR('R'), EOF('E');
        final Character code;

        PlayStatus(Character code)
        {
            this.code = code;
        }

        public Character getCode()
        {
            return code;
        }
    }

    private PlayStatus playStatus = PlayStatus.EOF;

    /*
     * Repeat Status: "-": Off, "R": All, "F": Folder, "1": Repeat 1, "x": disable
     */
    public enum RepeatStatus implements CharParameterIf
    {
        OFF('-', R.drawable.repeat_off),
        ALL('R', R.drawable.repeat_all),
        FOLDER('F', R.drawable.repeat_folder),
        REPEAT_1('1', R.drawable.repeat_once),
        DISABLE('x', R.drawable.repeat_off);

        final Character code;

        @DrawableRes
        final int imageId;

        RepeatStatus(Character code, @DrawableRes final int imageId)
        {
            this.code = code;
            this.imageId = imageId;
        }

        public Character getCode()
        {
            return code;
        }

        @DrawableRes
        public int getImageId()
        {
            return imageId;
        }
    }

    private RepeatStatus repeatStatus = RepeatStatus.DISABLE;

    /*
     * Shuffle Status: "-": Off, "S": All , "A": Album, "F": Folder, "x": disable
     */
    public enum ShuffleStatus implements CharParameterIf
    {
        OFF('-'), ALL('S'), ALBUM('A'), FOLDER('F'), DISABLE('x');
        final Character code;

        ShuffleStatus(Character code)
        {
            this.code = code;
        }

        public Character getCode()
        {
            return code;
        }
    }

    private ShuffleStatus shuffleStatus = ShuffleStatus.DISABLE;

    PlayStatusMsg(EISCPMessage raw) throws Exception
    {
        super(raw);
        if (data.length() > 0)
        {
            playStatus = (PlayStatus) searchParameter(data.charAt(0), PlayStatus.values(), playStatus);
        }
        if (data.length() > 1)
        {
            repeatStatus = (RepeatStatus) searchParameter(data.charAt(1), RepeatStatus.values(), repeatStatus);
        }
        if (data.length() > 2)
        {
            shuffleStatus = (ShuffleStatus) searchParameter(data.charAt(2), ShuffleStatus.values(), shuffleStatus);
        }
    }

    public PlayStatus getPlayStatus()
    {
        return playStatus;
    }

    public RepeatStatus getRepeatStatus()
    {
        return repeatStatus;
    }

    public ShuffleStatus getShuffleStatus()
    {
        return shuffleStatus;
    }

    @NonNull
    @Override
    public String toString()
    {
        return CODE + "[" + data
                + "; PLAY=" + playStatus.toString()
                + "; REPEAT=" + repeatStatus.toString()
                + "; SHUFFLE=" + shuffleStatus.toString()
                + "]";
    }
}
