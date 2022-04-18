/*
 * Enhanced Music Controller
 * Copyright (C) 2019-2022 by Mikhail Kulesh
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
import "package:flutter/material.dart";

import "../config/CheckableItem.dart";
import "../config/Configuration.dart";
import "../constants/Strings.dart";
import "../iscp/StateManager.dart";
import "../iscp/messages/EnumParameterMsg.dart";
import "../iscp/messages/InputSelectorMsg.dart";
import "../iscp/messages/ListTitleInfoMsg.dart";
import "../iscp/messages/PowerStatusMsg.dart";
import "../iscp/messages/ReceiverInformationMsg.dart";
import "../utils/Logging.dart";
import "../widgets/CustomTextButton.dart";
import "UpdatableView.dart";


class InputSelectorView extends UpdatableView
{
    static const List<String> UPDATE_TRIGGERS = [
        StateManager.ZONE_EVENT,
        PowerStatusMsg.CODE,
        ReceiverInformationMsg.CODE,
        InputSelectorMsg.CODE,
        ListTitleInfoMsg.CODE
    ];

    InputSelectorView(final ViewContext viewContext) : super(viewContext, UPDATE_TRIGGERS);

    @override
    Widget createView(BuildContext context, VoidCallback updateCallback)
    {
        Logging.logRebuild(this);

        final List<Widget> buttons = [];

        final List<Selector> sortedSelectors = _getSortedDeviceSelectors(
            false, state.mediaListState.inputType, state.receiverInformation.deviceSelectors);
        sortedSelectors.forEach((deviceSelector)
        {
            final EnumItem<InputSelector> selectorEnum =
            InputSelectorMsg.ValueEnum.valueByCode(deviceSelector.getId);
            if (selectorEnum.key != InputSelector.NONE)
            {
                final InputSelectorMsg cmd = InputSelectorMsg.output(state.getActiveZone, selectorEnum.key);
                buttons.add(CustomTextButton(
                    configuration.friendlyNames ? deviceSelector.getName : selectorEnum.description.toUpperCase(),
                    isEnabled: state.isConnected,
                    isSelected: state.isOn && state.mediaListState.inputType.code == selectorEnum.code,
                    onPressed: ()
                    {
                        if (!state.isOn)
                        {
                            stateManager.sendMessage(PowerStatusMsg.output(state.getActiveZone, PowerStatus.ON));
                        }
                        stateManager.sendMessage(cmd, waitingForData: true);
                    })
                );
            }
        });

        if (buttons.isEmpty)
        {
            buttons.add(CustomTextButton(Strings.dashed_string, isEnabled: false, isSelected: false));
        }

        return SingleChildScrollView(scrollDirection: Axis.horizontal, child: Row(children: buttons));
    }

    List<Selector> _getSortedDeviceSelectors(bool allItems, EnumItem<InputSelector> activeItem, final List<Selector> defaultItems)
    {
        final List<Selector> result = [];
        final List<String> defItems = [];
        defaultItems.forEach((i) => defItems.add(i.getId));
        final String par = configuration.getModelDependentParameter(Configuration.SELECTED_DEVICE_SELECTORS);
        for (CheckableItem sp in CheckableItem.readFromPreference(configuration, par, defItems))
        {
            final bool visible = allItems || sp.checked ||
                (activeItem.key != InputSelector.NONE && activeItem.getCode == sp.code);
            for (Selector i in defaultItems)
            {
                if (visible && i.getId == sp.code)
                {
                    result.add(i);
                }
            }
        }
        return result;
    }
}