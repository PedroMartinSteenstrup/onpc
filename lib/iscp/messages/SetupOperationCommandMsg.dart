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

import "../../constants/Drawables.dart";
import "../../constants/Strings.dart";
import "../EISCPMessage.dart";
import "EnumParameterMsg.dart";

enum SetupOperationCommand
{
    UNDEFINED,
    MENU,
    UP,
    DOWN,
    RIGHT,
    LEFT,
    ENTER,
    EXIT,
    HOME,
    QUICK
}

/*
 * Setup Operation Command
 */
class SetupOperationCommandMsg extends EnumParameterMsg<SetupOperationCommand>
{
    static const String CODE = "OSD";

    static const ExtEnum<SetupOperationCommand> ValueEnum = ExtEnum<SetupOperationCommand>([
        EnumItem(SetupOperationCommand.UNDEFINED,
            descr: Strings.dashed_string, defValue: true),
        EnumItem.code(SetupOperationCommand.MENU, "MENU", dcpCode: "MEN ON",
            descrList: Strings.l_cmd_description_setup, icon: Drawables.cmd_setup),
        EnumItem.code(SetupOperationCommand.UP, "UP", dcpCode: "CUP",
            descrList: Strings.l_cmd_description_up, icon: Drawables.cmd_up),
        EnumItem.code(SetupOperationCommand.DOWN, "DOWN", dcpCode: "CDN",
            descrList: Strings.l_cmd_description_down, icon: Drawables.cmd_down),
        EnumItem.code(SetupOperationCommand.RIGHT, "RIGHT", dcpCode: "CRT",
            descrList: Strings.l_cmd_description_right, icon: Drawables.cmd_right),
        EnumItem.code(SetupOperationCommand.LEFT, "LEFT", dcpCode: "CLT",
            descrList: Strings.l_cmd_description_left, icon: Drawables.cmd_left),
        EnumItem.code(SetupOperationCommand.ENTER, "ENTER", dcpCode: "ENT",
            descrList: Strings.l_cmd_description_select, icon: Drawables.cmd_select),
        EnumItem.code(SetupOperationCommand.EXIT, "EXIT", dcpCode: "RTN",
            descrList: Strings.l_cmd_description_return, icon: Drawables.cmd_return),
        EnumItem.code(SetupOperationCommand.HOME, "HOME", dcpCode: "N/A",
            descrList: Strings.l_cmd_description_home, icon: Drawables.cmd_home),
        EnumItem.code(SetupOperationCommand.QUICK, "QUICK", dcpCode: "OPT",
            descrList: Strings.l_cmd_description_quick_menu, icon: Drawables.cmd_quick_menu)
    ]);

    SetupOperationCommandMsg(EISCPMessage raw) : super(CODE, raw, ValueEnum);

    SetupOperationCommandMsg.output(SetupOperationCommand v) : super.output(CODE, v, ValueEnum);

    @override
    bool hasImpactOnMediaList()
    {
        return false;
    }

    /*
     * Denon control protocol
     */
    static const String _DCP_COMMAND = "MN";

    @override
    String? buildDcpMsg(bool isQuery)
    => buildDcpRequest(isQuery, _DCP_COMMAND);
}
