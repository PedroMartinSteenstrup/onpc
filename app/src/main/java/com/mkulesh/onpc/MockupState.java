package com.mkulesh.onpc;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.mkulesh.onpc.iscp.messages.InputSelectorMsg;
import com.mkulesh.onpc.iscp.messages.ListTitleInfoMsg;
import com.mkulesh.onpc.iscp.messages.MenuStatusMsg;
import com.mkulesh.onpc.iscp.messages.NetworkServiceMsg;
import com.mkulesh.onpc.iscp.messages.PlayStatusMsg;
import com.mkulesh.onpc.iscp.messages.PowerStatusMsg;
import com.mkulesh.onpc.iscp.messages.ReceiverInformationMsg;

import java.util.ArrayList;

class MockupState extends State
{
    MockupState(Context context)
    {
        //Common
        powerStatus = PowerStatusMsg.PowerStatus.ON;
        deviceProperties.put("brand", "Onkyo");
        deviceProperties.put("model", "NS-6130");
        deviceProperties.put("year", "2016");
        deviceProperties.put("firmwareversion", "1234-5678-910");
        deviceCover = BitmapFactory.decodeResource(context.getResources(), R.drawable.device_connect);
        deviceSelectors.add(new ReceiverInformationMsg.Selector("2B", "Network", "2B", false));
        deviceSelectors.add(new ReceiverInformationMsg.Selector("29", "Front USB", "29", true));
        deviceSelectors.add(new ReceiverInformationMsg.Selector("2A", "Rear USB", "2A", true));
        inputType = InputSelectorMsg.InputType.NET;

        // Track info
        cover = null;
        album = "Album";
        artist = "Artist";
        title = "Long title of song";
        currentTime = "00:00:59";
        maxTime = "00:10:15";
        trackInfo = "0001/0022";
        fileFormat = "FLAC/44hHz/16b";

        // Playback
        playStatus = PlayStatusMsg.PlayStatus.PLAY;
        repeatStatus = PlayStatusMsg.RepeatStatus.ALL;
        shuffleStatus = PlayStatusMsg.ShuffleStatus.ALL;
        timeSeek = MenuStatusMsg.TimeSeek.ENABLE;

        // Navigation
        serviceType = ListTitleInfoMsg.ServiceType.NET;
        layerInfo = ListTitleInfoMsg.LayerInfo.NET_TOP;
        numberOfLayers = 0;
        numberOfItems = 9;
        titleBar = "Net";
        serviceItems = new ArrayList<>();
        serviceItems.add(new NetworkServiceMsg("Music Server"));
        serviceItems.add(new NetworkServiceMsg("SPOTIFY"));
        serviceItems.add(new NetworkServiceMsg("TuneIn"));
        serviceItems.add(new NetworkServiceMsg("Deezer"));
        serviceItems.add(new NetworkServiceMsg("Airplay"));
        serviceItems.add(new NetworkServiceMsg("Tidal"));
        serviceItems.add(new NetworkServiceMsg("Chromecast built-in"));
        serviceItems.add(new NetworkServiceMsg("FlareConnect"));
        serviceItems.add(new NetworkServiceMsg("Play Queue"));
        itemsChanged = true;
    }
}
