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

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

/*
 * Amplifier Operation Command
 */
public class AmpOperationCommandMsg extends ISCPMessage
{
    public final static String CODE = "CAP";

    public enum Command implements StringParameterIf
    {
        MVLUP(R.string.amp_cmd_volume_up, R.drawable.volume_amp_up),
        MVLDOWN(R.string.amp_cmd_volume_down, R.drawable.volume_amp_down),
        SLIUP(R.string.amp_cmd_selector_up, R.drawable.input_selector_up),
        SLIDOWN(R.string.amp_cmd_selector_down, R.drawable.input_selector_down),
        AMTON(R.string.amp_cmd_audio_muting_on, R.drawable.volume_amp_muting),
        AMTOFF(R.string.amp_cmd_audio_muting_off, R.drawable.volume_amp_muting),
        AMTTG(R.string.amp_cmd_audio_muting_toggle, R.drawable.volume_amp_muting),
        PWRON(R.string.amp_cmd_system_on, R.drawable.menu_power_standby),
        PWROFF(R.string.amp_cmd_system_standby, R.drawable.menu_power_standby),
        PWRTG(R.string.amp_cmd_system_on_toggle, R.drawable.menu_power_standby);

        @StringRes
        final int descriptionId;

        @DrawableRes
        final int imageId;

        Command(@StringRes final int descriptionId, @DrawableRes final int imageId)
        {
            this.descriptionId = descriptionId;
            this.imageId = imageId;
        }

        public String getCode()
        {
            return toString();
        }

        @StringRes
        public int getDescriptionId()
        {
            return descriptionId;
        }

        @DrawableRes
        public int getImageId()
        {
            return imageId;
        }
    }

    private final Command command;

    public AmpOperationCommandMsg(final String command)
    {
        super(0, null);
        this.command = (Command) searchParameter(command, Command.values(), null);
    }

    public Command getCommand()
    {
        return command;
    }

    @NonNull
    @Override
    public String toString()
    {
        return CODE + "[" + (command == null ? "null" : command.toString()) + "]";
    }

    @Override
    public EISCPMessage getCmdMsg()
    {
        return command == null ? null : new EISCPMessage(CODE, command.getCode());
    }

    @Override
    public boolean hasImpactOnMediaList()
    {
        return false;
    }
}
