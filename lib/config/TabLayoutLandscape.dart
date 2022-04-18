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
import 'package:flutter/material.dart';

import "../config/CfgTabSettings.dart";
import "../constants/Strings.dart";
import "../utils/Logging.dart";
import "../widgets/CustomDivider.dart";
import "../widgets/CustomNumberPicker.dart";
import "../widgets/CustomTextLabel.dart";
import "CfgAppSettings.dart";
import "CfgTabSettings.dart";
import "CheckableItem.dart";
import "Configuration.dart";

class TabLayoutLandscape extends StatefulWidget
{
    final Configuration configuration;
    final CfgTabSettings tabSettings;

    TabLayoutLandscape(this.configuration, this.tabSettings);

    @override
    _TabLayoutLandscapeState createState()
    => _TabLayoutLandscapeState(configuration, tabSettings);
}

class _TabLayoutLandscapeState extends State<TabLayoutLandscape>
{
    final Configuration _configuration;
    final CfgTabSettings _tabSettings;
    final List<CheckableItem> _itemsLeft = [];
    final List<CheckableItem> _itemsRight = [];
    ScrollController _scrollControllerLeft, _scrollControllerRight;

    _TabLayoutLandscapeState(this._configuration, this._tabSettings)
    {
        _tabSettings.createCheckableItems(_itemsLeft, AppControlGroup.LAND_LEFT, _tabSettings.controlsLandscapeLeft);
        _tabSettings.createCheckableItems(_itemsRight, AppControlGroup.LAND_RIGHT, _tabSettings.controlsLandscapeRight);
    }

    @override
    void initState()
    {
        super.initState();
        _scrollControllerLeft = ScrollController();
        _scrollControllerRight = ScrollController();
    }

    @override
    void dispose()
    {
        _scrollControllerLeft.dispose();
        _scrollControllerRight.dispose();
        super.dispose();
    }

    String _getParameter(final AppControlGroup type)
    => CfgTabSettings.getParameterName(_tabSettings.tab, type);

    @override
    Widget build(BuildContext context)
    {
        Logging.logRebuild(this);

        final Widget controlLists = Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
                Expanded(flex: 20,
                    child: CheckableItem.buildPanel(
                        _itemsLeft.map<Widget>(_buildItemsLeft).toList(),
                        _onReorderLeft,
                        scrollController: _scrollControllerLeft)),
                Expanded(flex: 1,
                    child: VerticalDivider(
                        color: Theme.of(context).disabledColor)),
                Expanded(flex: 20,
                    child: CheckableItem.buildPanel(
                        _itemsRight.map<Widget>(_buildItemsRight).toList(),
                        _onReorderRight,
                        scrollController: _scrollControllerRight)),
            ]
        );

        final Widget columnWidth = Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
                CustomTextLabel.normal(Strings.pref_column_separator),
                CustomNumberPicker(
                    initialValue: _tabSettings.columnSeparator,
                    maxValue: 90,
                    minValue: 10,
                    step: 1,
                    onValue: (value)
                    => _tabSettings.columnSeparator = value)
            ]
        );

        return CheckableItem.buildScaffold(context,
            Strings.drawer_tab_layout + " (" + CfgAppSettings.getTabName(_tabSettings.tab) + ")",
            Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                mainAxisSize: MainAxisSize.max,
                children: [
                    Expanded(child: controlLists),
                    CustomDivider(),
                    columnWidth
                ],
            ),
            _configuration
        );
    }

    Widget _buildItemsLeft(CheckableItem item)
    {
        return item.buildListItem((bool newValue)
        {
            setState(()
            {
                item.checked = newValue;
                CheckableItem.writeToPreference(_configuration, _getParameter(AppControlGroup.LAND_LEFT), _itemsLeft);
            });
        });
    }

    void _onReorderLeft(int oldIndex, int newIndex)
    {
        setState(()
        {
            CheckableItem.reorder(_configuration, _getParameter(AppControlGroup.LAND_LEFT), _itemsLeft, oldIndex, newIndex);
        });
    }

    Widget _buildItemsRight(CheckableItem item)
    {
        return item.buildListItem((bool newValue)
        {
            setState(()
            {
                item.checked = newValue;
                CheckableItem.writeToPreference(_configuration, _getParameter(AppControlGroup.LAND_RIGHT), _itemsRight);
            });
        });
    }

    void _onReorderRight(int oldIndex, int newIndex)
    {
        setState(()
        {
            CheckableItem.reorder(_configuration, _getParameter(AppControlGroup.LAND_RIGHT), _itemsRight, oldIndex, newIndex);
        });
    }
}