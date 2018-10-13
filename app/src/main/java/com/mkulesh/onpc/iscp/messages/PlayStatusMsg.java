package com.mkulesh.onpc.iscp.messages;

import com.mkulesh.onpc.iscp.EISCPMessage;
import com.mkulesh.onpc.iscp.ISCPMessage;

/*
 * NET/USB Play Status (3 letters)
 */
public class PlayStatusMsg extends ISCPMessage
{
    public final static String CODE = "NST";

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
        OFF('-'), ALL('R'), FOLDER('F'), REPEAT_1('1'), DISABLE('x');
        final Character code;

        RepeatStatus(Character code)
        {
            this.code = code;
        }

        public Character getCode()
        {
            return code;
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
