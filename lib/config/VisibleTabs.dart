/*
 * Enhanced Music Controller
 * Copyright (C) 2019-2023 by Mikhail Kulesh
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

import 'package:flutter/material.dart';

import "../constants/Strings.dart";
import "../iscp/ConnectionIf.dart";
import "../utils/Convert.dart";
import "../utils/Logging.dart";
import "CfgAppSettings.dart";
import "CheckableItem.dart";
import "Configuration.dart";

class VisibleTabs extends StatefulWidget
{
    final Configuration configuration;

    VisibleTabs(this.configuration);

    @override
    _VisibleTabsState createState()
    => _VisibleTabsState(configuration);
}

class _VisibleTabsState extends State<VisibleTabs> with ProtoTypeMix
{
    final Configuration _configuration;
    late String _parameter;
    final List<CheckableItem> _items = [];

    _VisibleTabsState(this._configuration)
    {
        setProtoType(_configuration.protoType);
        _parameter = CfgAppSettings.VISIBLE_TABS;
        _createItems();
    }

    void _createItems()
    {
        final List<String> defItems = [];
        AppTabs.values.forEach((i) => defItems.add(Convert.enumToString(i)));
        for (CheckableItem sp in CheckableItem.readFromPreference(_configuration, _parameter, defItems))
        {
            for (AppTabs i in AppTabs.values)
            {
                if (!CfgAppSettings.isTabEnabled(i, protoType))
                {
                    continue;
                }
                final String code = Convert.enumToString(i);
                if (code == sp.code)
                {
                    _items.add(CheckableItem(code, CfgAppSettings.getTabName(i), sp.checked));
                }
            }
        }
    }

    @override
    Widget build(BuildContext context)
    {
        Logging.logRebuild(this);
        return CheckableItem.buildList(context,
            _items.map<Widget>(_buildListItem).toList(),
            Strings.pref_visible_tabs,
            _onReorder,
            _configuration);
    }

    Widget _buildListItem(CheckableItem item)
    {
        return item.buildListItem((bool newValue)
        {
            setState(()
            {
                item.checked = newValue;
                CheckableItem.writeToPreference(_configuration, _parameter, _items);
            });
        });
    }

    void _onReorder(int oldIndex, int newIndex)
    {
        setState(()
        {
            CheckableItem.reorder(_configuration, _parameter, _items, oldIndex, newIndex);
        });
    }
}