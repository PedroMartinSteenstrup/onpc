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

package com.mkulesh.onpc.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mkulesh.onpc.R;
import com.mkulesh.onpc.config.CfgAppSettings;
import com.mkulesh.onpc.iscp.State;
import com.mkulesh.onpc.iscp.messages.AmpOperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.CdPlayerOperationCommandMsg;

import java.util.ArrayList;
import java.util.HashSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

public class RemoteInterfaceFragment extends BaseFragment
{
    private final ArrayList<View> buttons = new ArrayList<>();

    public RemoteInterfaceFragment()
    {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        initializeFragment(inflater, container, R.layout.remote_interface_fragment, CfgAppSettings.Tabs.RI);

        if (activity.getConfiguration().appSettings.isRemoteInterfaceAmp())
        {
            final LinearLayout l = rootView.findViewById(R.id.remote_interface_amp);
            l.setVisibility(View.VISIBLE);
            collectButtons(l, buttons);
        }

        if (activity.getConfiguration().appSettings.isRemoteInterfaceCd())
        {
            final LinearLayout l = rootView.findViewById(R.id.remote_interface_cd);
            l.setVisibility(View.VISIBLE);
            collectButtons(l, buttons);
        }

        for (View b : buttons)
        {
            prepareRiButton(b);
        }

        updateContent();
        return rootView;
    }

    private void prepareRiButton(View b)
    {
        if (b.getTag() == null)
        {
            return;
        }
        String[] tokens = b.getTag().toString().split(":");
        if (tokens.length != 2)
        {
            return;
        }
        final String msgName = tokens[0];

        switch (msgName)
        {
        case AmpOperationCommandMsg.CODE:
        {
            final AmpOperationCommandMsg cmd = new AmpOperationCommandMsg(tokens[1]);
            if (b instanceof AppCompatImageButton)
            {
                prepareButton((AppCompatImageButton) b, cmd, cmd.getCommand().getImageId(), cmd.getCommand().getDescriptionId());

            }
            else
            {
                prepareButtonListeners(b, cmd, null);
            }
            break;
        }
        case CdPlayerOperationCommandMsg.CODE:
        {
            final CdPlayerOperationCommandMsg cmd = new CdPlayerOperationCommandMsg(tokens[1]);
            if (b instanceof AppCompatImageButton)
            {
                prepareButton((AppCompatImageButton) b, cmd, cmd.getCommand().getImageId(), cmd.getCommand().getDescriptionId());

            }
            else
            {
                prepareButtonListeners(b, cmd, null);
            }
            break;
        }
        default:
            break;
        }
    }

    @Override
    protected void updateStandbyView(@Nullable final State state)
    {
        for (View b : buttons)
        {
            setButtonEnabled(b, state != null);
        }
    }

    @Override
    protected void updateActiveView(@NonNull final State state, @NonNull final HashSet<State.ChangeType> eventChanges)
    {
        for (View b : buttons)
        {
            setButtonEnabled(b, true);
        }
    }
}
