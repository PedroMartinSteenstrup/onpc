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

package com.mkulesh.onpc.fragments;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mkulesh.onpc.R;
import com.mkulesh.onpc.config.CfgAppSettings;
import com.mkulesh.onpc.config.CfgAudioControl;
import com.mkulesh.onpc.iscp.ISCPMessage;
import com.mkulesh.onpc.iscp.State;
import com.mkulesh.onpc.iscp.StateManager;
import com.mkulesh.onpc.iscp.messages.AmpOperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.AudioMutingMsg;
import com.mkulesh.onpc.iscp.messages.CdPlayerOperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.DisplayModeMsg;
import com.mkulesh.onpc.iscp.messages.InputSelectorMsg;
import com.mkulesh.onpc.iscp.messages.ListeningModeMsg;
import com.mkulesh.onpc.iscp.messages.MasterVolumeMsg;
import com.mkulesh.onpc.iscp.messages.MenuStatusMsg;
import com.mkulesh.onpc.iscp.messages.MultiroomChannelSettingMsg;
import com.mkulesh.onpc.iscp.messages.MultiroomDeviceInformationMsg;
import com.mkulesh.onpc.iscp.messages.OperationCommandMsg;
import com.mkulesh.onpc.iscp.messages.PlayStatusMsg;
import com.mkulesh.onpc.iscp.messages.PresetCommandMsg;
import com.mkulesh.onpc.iscp.messages.PresetMemoryMsg;
import com.mkulesh.onpc.iscp.messages.RDSInformationMsg;
import com.mkulesh.onpc.iscp.messages.ReceiverInformationMsg;
import com.mkulesh.onpc.iscp.messages.ServiceType;
import com.mkulesh.onpc.iscp.messages.TimeInfoMsg;
import com.mkulesh.onpc.iscp.messages.TimeSeekMsg;
import com.mkulesh.onpc.iscp.messages.TuningCommandMsg;
import com.mkulesh.onpc.utils.Logging;
import com.mkulesh.onpc.utils.Utils;
import com.mkulesh.onpc.widgets.HorizontalNumberPicker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatSeekBar;

public class ListenFragment extends BaseFragment implements AudioControlManager.MasterVolumeInterface
{
    private HorizontalScrollView listeningModeLayout;
    private LinearLayout soundControlLayout;
    private AppCompatImageButton btnRepeat;
    private AppCompatImageButton btnPrevious;
    private AppCompatImageButton btnPausePlay;
    private AppCompatImageButton btnNext;
    private AppCompatImageButton btnRandom;
    private AppCompatImageButton btnTrackMenu;
    private AppCompatImageButton btnMultiroomGroup;
    private AppCompatButton btnMultiroomChanel;
    private final List<AppCompatImageButton> playbackButtons = new ArrayList<>();
    private final List<AppCompatImageButton> fmDabButtons = new ArrayList<>();
    private final List<AppCompatImageButton> amplifierButtons = new ArrayList<>();
    private AppCompatImageButton positiveFeed, negativeFeed;
    private final List<View> deviceSoundButtons = new ArrayList<>();
    private ImageView cover;
    private AppCompatSeekBar seekBar;
    private final AudioControlManager audioControlManager = new AudioControlManager();

    public ListenFragment()
    {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        initializeFragment(inflater, container, R.layout.monitor_fragment_port, R.layout.monitor_fragment_land, CfgAppSettings.Tabs.LISTEN);
        rootView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        listeningModeLayout = rootView.findViewById(R.id.listening_mode_layout);
        soundControlLayout = rootView.findViewById(R.id.sound_control_layout);

        // Command Buttons
        btnRepeat = rootView.findViewById(R.id.btn_repeat);
        playbackButtons.add(btnRepeat);

        btnPrevious = rootView.findViewById(R.id.btn_previous);
        playbackButtons.add(btnPrevious);

        final AppCompatImageButton btnStop = rootView.findViewById(R.id.btn_stop);
        playbackButtons.add(btnStop);

        btnPausePlay = rootView.findViewById(R.id.btn_pause_play);
        playbackButtons.add(btnPausePlay);

        btnNext = rootView.findViewById(R.id.btn_next);
        playbackButtons.add(btnNext);

        btnRandom = rootView.findViewById(R.id.btn_random);
        playbackButtons.add(btnRandom);

        for (AppCompatImageButton b : playbackButtons)
        {
            final OperationCommandMsg msg = new OperationCommandMsg(
                    ReceiverInformationMsg.DEFAULT_ACTIVE_ZONE, (String) (b.getTag()));
            prepareButton(b, msg, msg.getCommand().getImageId(), msg.getCommand().getDescriptionId());
        }

        // FM/DAB preset and tuning buttons
        fmDabButtons.add(rootView.findViewById(R.id.btn_preset_up));
        fmDabButtons.add(rootView.findViewById(R.id.btn_preset_down));
        fmDabButtons.add(rootView.findViewById(R.id.btn_tuning_up));
        fmDabButtons.add(rootView.findViewById(R.id.btn_tuning_down));
        fmDabButtons.add(rootView.findViewById(R.id.btn_rds_info));
        prepareFmDabButtons();

        // Audio control buttons
        audioControlManager.setActivity(activity, this);
        clearSoundVolumeButtons();

        cover = rootView.findViewById(R.id.tv_cover);
        cover.setContentDescription(activity.getResources().getString(R.string.tv_display_mode));
        prepareButtonListeners(cover, new DisplayModeMsg(DisplayModeMsg.TOGGLE));

        seekBar = rootView.findViewById(R.id.progress_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar)
            {
                // empty
            }

            public void onStopTrackingTouch(SeekBar seekBar)
            {
                if (activity.isConnected())
                {
                    seekTime(progressChanged);
                }
            }
        });

        // Track menu
        {
            btnTrackMenu = rootView.findViewById(R.id.btn_track_menu);
            setButtonEnabled(btnTrackMenu, false);
        }

        // Multiroom
        btnMultiroomGroup = rootView.findViewById(R.id.btn_multiroom_group);
        btnMultiroomChanel = rootView.findViewById(R.id.btn_multiroom_channel);

        // Feeds
        positiveFeed = rootView.findViewById(R.id.btn_positive_feed);
        prepareButtonListeners(positiveFeed, null, () ->
                activity.getStateManager().sendTrackCmd(OperationCommandMsg.Command.F1, true));
        negativeFeed = rootView.findViewById(R.id.btn_negative_feed);
        prepareButtonListeners(negativeFeed, null, () ->
                activity.getStateManager().sendTrackCmd(OperationCommandMsg.Command.F2, true));

        updateContent();
        return rootView;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (activity != null && activity.isConnected())
        {
            activity.getStateManager().setPlaybackMode(true);
            final State state = activity.getStateManager().getState();
            if (state.isUiTypeValid() && !state.isPlaybackMode() && !state.isMenuMode())
            {
                activity.getStateManager().sendMessage(StateManager.LIST_MSG);
            }
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        updateStandbyView(null);
    }

    private void prepareFmDabButtons()
    {
        for (AppCompatImageButton b : fmDabButtons)
        {
            String[] tokens = b.getTag().toString().split(":");
            if (tokens.length != 2)
            {
                continue;
            }
            final String msgName = tokens[0];
            switch (msgName)
            {
            case PresetCommandMsg.CODE:
                final PresetCommandMsg pMsg = new PresetCommandMsg(
                        ReceiverInformationMsg.DEFAULT_ACTIVE_ZONE, tokens[1]);
                if (pMsg.getCommand() != null)
                {
                    prepareButton(b, pMsg, pMsg.getCommand().getImageId(), pMsg.getCommand().getDescriptionId());
                }
                break;
            case TuningCommandMsg.CODE:
                final TuningCommandMsg tMsg = new TuningCommandMsg(
                        ReceiverInformationMsg.DEFAULT_ACTIVE_ZONE, tokens[1]);
                if (tMsg.getCommand() != null)
                {
                    prepareButton(b, tMsg, tMsg.getCommand().getImageId(), tMsg.getCommand().getDescriptionId());
                }
                break;
            case RDSInformationMsg.CODE:
                prepareButton(b, null, R.drawable.cmd_rds_info, R.string.cmd_rds_info);
                prepareButtonListeners(b, null, () ->
                {
                    if (activity != null && activity.isConnected())
                    {
                        final InputSelectorMsg.InputType inp = activity.getStateManager().getState().inputType;
                        final ISCPMessage msg = inp == InputSelectorMsg.InputType.FM ?
                                new RDSInformationMsg(RDSInformationMsg.TOGGLE) : new DisplayModeMsg(DisplayModeMsg.TOGGLE);
                        activity.getStateManager().sendMessage(msg);
                    }
                });
                break;
            }
        }
    }

    private void clearSoundVolumeButtons()
    {
        amplifierButtons.clear();
        deviceSoundButtons.clear();
        soundControlLayout.removeAllViews();
        soundControlLayout.setVisibility(View.GONE);
    }

    private void prepareAmplifierButtons()
    {
        clearSoundVolumeButtons();
        soundControlLayout.setVisibility(View.VISIBLE);
        listeningModeLayout.setVisibility(View.GONE);

        final AmpOperationCommandMsg.Command[] commands = new AmpOperationCommandMsg.Command[]{
                AmpOperationCommandMsg.Command.AMTTG,
                AmpOperationCommandMsg.Command.MVLDOWN,
                AmpOperationCommandMsg.Command.MVLUP
        };

        for (AmpOperationCommandMsg.Command c : commands)
        {
            final AmpOperationCommandMsg msg = new AmpOperationCommandMsg(c.getCode());
            final AppCompatImageButton b = createButton(
                    msg.getCommand().getImageId(), msg.getCommand().getDescriptionId(),
                    msg, msg.getCommand().getCode());
            soundControlLayout.addView(b);
            amplifierButtons.add(b);
        }
    }

    private void prepareDeviceSoundButtons(final State.SoundControlType soundControl,
                                           @NonNull Utils.ProtoType protoType)
    {
        clearSoundVolumeButtons();
        soundControlLayout.setVisibility(View.VISIBLE);

        // Here, we create zone-dependent buttons without command message.
        // The message for active zone will be assigned in updateActiveView
        final boolean isSlider = soundControl == State.SoundControlType.DEVICE_SLIDER ||
                soundControl == State.SoundControlType.DEVICE_BTN_SLIDER;
        if (isSlider && soundControlLayout.getTag() != null && soundControlLayout.getTag().equals("portrait"))
        {
            audioControlManager.createSliderSoundControl(this, soundControlLayout,
                    soundControl == State.SoundControlType.DEVICE_BTN_SLIDER);
        }
        else
        {
            audioControlManager.createButtonsSoundControl(this, soundControlLayout);
        }

        for (int i = 0; i < soundControlLayout.getChildCount(); i++)
        {
            deviceSoundButtons.add(soundControlLayout.getChildAt(i));
        }

        // fast selector for listening mode
        listeningModeLayout.setVisibility(View.VISIBLE);
        final CfgAudioControl ac = activity.getConfiguration().audioControl;
        if (listeningModeLayout.getChildCount() == 1)
        {
            final LinearLayout l = (LinearLayout) listeningModeLayout.getChildAt(0);
            l.removeAllViews();
            for (ListeningModeMsg.Mode m : ac.getSortedListeningModes(true, null, protoType))
            {
                final ListeningModeMsg msg = new ListeningModeMsg(m);
                final AppCompatButton b = createButton(
                        msg.getMode().getDescriptionId(), msg, msg.getMode(), null);
                l.addView(b);
                b.setVisibility(View.GONE);
                deviceSoundButtons.add(b);
            }
        }
    }

    private void setButtonsEnabled(List<AppCompatImageButton> buttons, boolean flag)
    {
        for (AppCompatImageButton b : buttons)
        {
            setButtonEnabled(b, flag);
        }
    }

    private void setButtonsVisibility(List<AppCompatImageButton> buttons, int flag)
    {
        for (AppCompatImageButton b : buttons)
        {
            b.setVisibility(flag);
        }
    }

    @Override
    protected void updateStandbyView(@Nullable final State state)
    {
        ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(TimeInfoMsg.INVALID_TIME);
        ((TextView) rootView.findViewById(R.id.tv_time_end)).setText(TimeInfoMsg.INVALID_TIME);

        TextView track = rootView.findViewById(R.id.tv_track);
        track.setText("");
        updateInputSource(null, R.drawable.media_item_unknown, false);

        ((TextView) rootView.findViewById(R.id.tv_album)).setText("");
        ((TextView) rootView.findViewById(R.id.tv_artist)).setText("");
        ((TextView) rootView.findViewById(R.id.tv_title)).setText("");

        final TextView format = rootView.findViewById(R.id.tv_file_format);
        {
            format.setText("");
            format.setClickable(false);
        }

        cover.setEnabled(false);
        cover.setImageResource(R.drawable.empty_cover);
        Utils.setImageViewColorAttr(activity, cover, android.R.attr.textColor);

        seekBar.setEnabled(false);
        seekBar.setProgress(0);
        setButtonsEnabled(amplifierButtons, state != null);
        for (View b : deviceSoundButtons)
        {
            if (audioControlManager.isVolumeLevel(b))
            {
                updateVolumeLevel(b, state);
            }
            else
            {
                setButtonEnabled(b, false);
            }
        }
        setButtonsVisibility(playbackButtons, View.VISIBLE);
        setButtonsEnabled(playbackButtons, false);
        setButtonsVisibility(fmDabButtons, View.GONE);
        btnTrackMenu.setVisibility(View.GONE);
        updateMultiroomGroupBtn(btnMultiroomGroup, state);
        updateMultiroomChannelBtn(btnMultiroomChanel, state);
        positiveFeed.setVisibility(View.GONE);
        negativeFeed.setVisibility(View.GONE);
    }

    @Override
    protected void updateActiveView(@NonNull final State state, @NonNull final HashSet<State.ChangeType> eventChanges)
    {
        // time seek only
        if (eventChanges.size() == 1 && eventChanges.contains(State.ChangeType.TIME_SEEK))
        {
            updateProgressBar(state);
            return;
        }

        if (eventChanges.contains(State.ChangeType.AUDIO_CONTROL))
        {
            audioControlManager.updateActiveView(state);
        }

        Logging.info(this, "Updating playback monitor");

        // Auto volume control
        final State.SoundControlType soundControl = state.soundControlType(
                activity.getConfiguration().audioControl.getSoundControl(), state.getActiveZoneInfo());
        switch (soundControl)
        {
        case RI_AMP:
            prepareAmplifierButtons();
            break;
        case DEVICE_BUTTONS:
        case DEVICE_SLIDER:
        case DEVICE_BTN_SLIDER:
            prepareDeviceSoundButtons(soundControl, state.protoType);
            break;
        default:
            clearSoundVolumeButtons();
            break;
        }

        // Text (album and artist)
        {
            ((TextView) rootView.findViewById(R.id.tv_album)).setText(state.album);
            ((TextView) rootView.findViewById(R.id.tv_artist)).setText(state.artist);

            final TextView title = rootView.findViewById(R.id.tv_title);
            final TextView format = rootView.findViewById(R.id.tv_file_format);
            if (state.isRadioInput())
            {
                final ReceiverInformationMsg.Preset preset = state.searchPreset();
                final String presetInfo = preset != null ? preset.displayedString(false) : "";
                final TextView album = rootView.findViewById(R.id.tv_album);
                album.setText(presetInfo);
                final String stationInfo = state.isDab() || state.isFm() ? state.stationName : "";
                title.setText(!stationInfo.equals(presetInfo) ? stationInfo : "");
                format.setText(String.format(" %s", state.getFrequencyInfo(activity)));
            }
            else
            {
                title.setText(state.title);
                format.setText(state.fileFormat);
            }
            format.setClickable(true);
            format.setOnClickListener((v) -> showAvInfoDialog(state));
        }

        // service icon and track
        {
            final TextView track = rootView.findViewById(R.id.tv_track);
            track.setText(state.getTrackInfo(activity));
            updateInputSource(state, state.getServiceIcon(), true);
        }

        // cover
        cover.setEnabled(true);
        if (state.cover == null || state.isSimpleInput())
        {
            cover.setImageResource(R.drawable.empty_cover);
            Utils.setImageViewColorAttr(activity, cover, android.R.attr.textColor);
        }
        else
        {
            cover.setColorFilter(null);
            cover.setImageBitmap(state.cover);
        }

        // progress bar
        updateProgressBar(state);

        // buttons
        final ArrayList<String> selectedListeningModes = new ArrayList<>();
        final CfgAudioControl ac = activity.getConfiguration().audioControl;
        for (ListeningModeMsg.Mode m : ac.getSortedListeningModes(false, state.listeningMode, state.protoType))
        {
            selectedListeningModes.add(m.getCode());
        }
        setButtonsEnabled(amplifierButtons, true);
        for (View b : deviceSoundButtons)
        {
            setButtonEnabled(b, true);
            if (b.getTag() instanceof AudioMutingMsg.Status)
            {
                setButtonSelected(b, state.audioMuting == AudioMutingMsg.Status.ON);
                prepareButtonListeners(b, new AudioMutingMsg(
                        state.getActiveZone(), AudioMutingMsg.toggle(state.audioMuting, state.protoType)));
            }
            else if (b.getTag() instanceof MasterVolumeMsg.Command)
            {
                final MasterVolumeMsg.Command cmd = (MasterVolumeMsg.Command) (b.getTag());
                prepareButtonListeners(b, new MasterVolumeMsg(state.getActiveZone(), cmd));
                if (audioControlManager.isAudioControlEnabled())
                {
                    b.setOnLongClickListener(v -> audioControlManager.showAudioControlDialog());
                }
                else
                {
                    b.setOnLongClickListener(v -> Utils.showButtonDescription(activity, v));
                }
            }
            else if (b.getTag() instanceof ListeningModeMsg.Mode)
            {
                final ListeningModeMsg.Mode s = (ListeningModeMsg.Mode) (b.getTag());
                if (selectedListeningModes.contains(s.getCode()))
                {
                    b.setVisibility(View.VISIBLE);
                    setButtonSelected(b, s == state.listeningMode);
                    if (b.isSelected())
                    {
                        b.getParent().requestChildFocus(b, b);
                    }
                }
                else
                {
                    b.setVisibility(View.GONE);
                }
            }
            else if (audioControlManager.isVolumeLevel(b))
            {
                updateVolumeLevel(b, state);
            }
        }

        updateListeningModeLayout();

        // Track and playback menu
        if (state.isRadioInput())
        {
            updatePresetButtons();
            prepareButtonListeners(btnTrackMenu, null, () -> showPresetMemoryDialog(state));
        }
        else
        {
            updatePlaybackButtons(state);
            prepareButtonListeners(btnTrackMenu, null, () ->
                    activity.getStateManager().sendTrackCmd(OperationCommandMsg.Command.MENU, false));
        }

        {
            final boolean isTrackMenu = state.isRadioInput() ||
                    (state.trackMenu == MenuStatusMsg.TrackMenu.ENABLE && state.isPlaying());
            btnTrackMenu.setVisibility(View.VISIBLE);
            setButtonEnabled(btnTrackMenu, isTrackMenu);
        }

        if (state.isMenuMode() && !state.isMediaEmpty())
        {
            popupManager.showTrackMenuDialog(activity, state);
        }
        else
        {
            popupManager.closeTrackMenuDialog();
        }

        // Multiroom groups
        updateMultiroomGroupBtn(btnMultiroomGroup, state);
        updateMultiroomChannelBtn(btnMultiroomChanel, state);

        // Feeds
        updateFeedButton(positiveFeed, state.positiveFeed, state.serviceType);
        updateFeedButton(negativeFeed, state.negativeFeed, state.serviceType);
    }

    private void updatePresetButtons()
    {
        setButtonsVisibility(playbackButtons, View.GONE);
        setButtonsVisibility(fmDabButtons, View.VISIBLE);
        setButtonsEnabled(fmDabButtons, true);
    }

    private void updateInputSource(@Nullable final State state, @DrawableRes int imageId, final boolean visible)
    {
        final AppCompatImageButton btn = rootView.findViewById(R.id.btn_input_selector);
        prepareButton(btn, imageId, R.string.av_info_dialog);
        btn.setVisibility(visible ? View.VISIBLE : View.GONE);
        setButtonEnabled(btn, state != null);
        prepareButtonListeners(btn, null, () -> showAvInfoDialog(state));
    }

    private void showAvInfoDialog(@Nullable final State state)
    {
        if (state == null)
        {
            return;
        }

        final FrameLayout frameView = new FrameLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.dialog_av_info, frameView);

        final Drawable icon = Utils.getDrawable(activity, state.getServiceIcon());
        Utils.setDrawableColorAttr(activity, icon, android.R.attr.textColorSecondary);

        if (getContext() != null && getContext().getResources() != null)
        {
            ((TextView) frameView.findViewById(R.id.av_info_audio_input)).setText(
                    String.format(getContext().getResources().getString(
                            R.string.av_info_input), state.avInfoAudioInput));

            ((TextView) frameView.findViewById(R.id.av_info_audio_output)).setText(
                    String.format(getContext().getResources().getString(
                            R.string.av_info_output), state.avInfoAudioOutput));

            ((TextView) frameView.findViewById(R.id.av_info_video_input)).setText(
                    String.format(getContext().getResources().getString(
                            R.string.av_info_input), state.avInfoVideoInput));

            ((TextView) frameView.findViewById(R.id.av_info_video_output)).setText(
                    String.format(getContext().getResources().getString(
                            R.string.av_info_output), state.avInfoVideoOutput));
        }

        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.av_info_dialog)
                .setIcon(icon)
                .setCancelable(true)
                .setView(frameView)
                .setPositiveButton(activity.getResources().getString(R.string.action_ok), (dialog12, which) -> dialog12.dismiss())
                .create();

        dialog.show();
        Utils.fixIconColor(dialog, android.R.attr.textColorSecondary);
    }

    /*
     * Playback control
     */
    private void updatePlaybackButtons(@NonNull final State state)
    {
        setButtonsVisibility(fmDabButtons, View.GONE);
        setButtonsVisibility(playbackButtons, View.VISIBLE);

        for (AppCompatImageButton b : playbackButtons)
        {
            String opCommand = (String) (b.getTag());

            if (b.getId() == btnPausePlay.getId())
            {
                // For common btnPausePlay button, set desired command (PLAY or PAUSE)
                // depending on current play state
                final boolean isPaused = (state.playStatus == PlayStatusMsg.PlayStatus.STOP ||
                        state.playStatus == PlayStatusMsg.PlayStatus.PAUSE);
                opCommand = isPaused ? OperationCommandMsg.Command.PLAY.toString() :
                        OperationCommandMsg.Command.PAUSE.toString();
            }

            if (state.isCdInput())
            {
                final String ccdCommand = CdPlayerOperationCommandMsg.convertOpCommand(opCommand);
                final CdPlayerOperationCommandMsg msg = new CdPlayerOperationCommandMsg(ccdCommand);
                prepareButton(b, msg, msg.getCommand().getImageId(), msg.getCommand().getDescriptionId());
            }
            else
            {
                final OperationCommandMsg.Command netCommand = (OperationCommandMsg.Command)
                        ISCPMessage.searchParameter(opCommand, OperationCommandMsg.Command.values(), null);
                prepareButton(b, netCommand.getImageId(), netCommand.getDescriptionId());
                // To start play in normal mode, PAUSE shall be issue instead of PLAY command
                final OperationCommandMsg msg = (netCommand == OperationCommandMsg.Command.PLAY) ?
                        new OperationCommandMsg(state.getActiveZone(), OperationCommandMsg.Command.PAUSE.toString()) :
                        new OperationCommandMsg(state.getActiveZone(), netCommand.toString());
                prepareButtonListeners(b, null, () ->
                {
                    if (activity.getStateManager() != null)
                    {
                        if (!state.isPlaybackMode() && state.isUsb() &&
                                (msg.getCommand() == OperationCommandMsg.Command.TRDN ||
                                        msg.getCommand() == OperationCommandMsg.Command.TRUP))
                        {
                            // Issue-44: on some receivers, "TRDN" and "TRUP" for USB only work
                            // in playback mode. Therefore, switch to this mode before
                            // send OperationCommandMsg if current mode is LIST
                            activity.getStateManager().sendTrackMsg(msg, false);
                        }
                        else
                        {
                            activity.getStateManager().sendMessage(msg);
                        }
                    }
                });
            }
            setButtonEnabled(b, true);
        }

        btnRepeat.setImageResource(state.repeatStatus.getImageId());
        if (state.repeatStatus == PlayStatusMsg.RepeatStatus.DISABLE)
        {
            setButtonEnabled(btnRepeat, false);
        }
        else
        {
            setButtonEnabled(btnRepeat, true);
            setButtonSelected(btnRepeat, state.repeatStatus != PlayStatusMsg.RepeatStatus.OFF);
        }

        if (state.shuffleStatus == PlayStatusMsg.ShuffleStatus.DISABLE)
        {
            setButtonEnabled(btnRandom, false);
        }
        else
        {
            setButtonEnabled(btnRandom, true);
            setButtonSelected(btnRandom, state.shuffleStatus != PlayStatusMsg.ShuffleStatus.OFF);
        }

        setButtonEnabled(btnPrevious, state.isPlaying());
        setButtonEnabled(btnNext, state.isPlaying());
        setButtonEnabled(btnPausePlay, state.isOn());
    }

    private void updateListeningModeLayout()
    {
        listeningModeLayout.requestLayout();
        if (listeningModeLayout.getChildCount() == 1)
        {
            final LinearLayout l = (LinearLayout) listeningModeLayout.getChildAt(0);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            if (l.getMeasuredWidth() < listeningModeLayout.getMeasuredWidth())
            {
                params.gravity = Gravity.CENTER;
            }
            l.setLayoutParams(params);
            l.requestLayout();
        }
    }


    /*
     * Multiroom control
     */
    private void updateMultiroomGroupBtn(AppCompatImageButton b, @Nullable final State state)
    {
        if (state != null && activity.isMultiroomAvailable())
        {
            b.setVisibility(View.VISIBLE);
            setButtonEnabled(b, true);
            setButtonSelected(b, state.isMasterDevice());
            b.setContentDescription(activity.getString(R.string.cmd_multiroom_group));

            prepareButtonListeners(b, null, () ->
            {
                if (activity.isConnected())
                {
                    final AlertDialog alertDialog = MultiroomManager.createDeviceSelectionDialog(
                            activity, b.getContentDescription());
                    alertDialog.show();
                    Utils.fixIconColor(alertDialog, android.R.attr.textColorSecondary);
                }
            });
        }
        else
        {
            b.setVisibility(View.GONE);
            setButtonEnabled(b, false);
        }
    }

    private void updateMultiroomChannelBtn(AppCompatButton b, @Nullable final State state)
    {
        MultiroomDeviceInformationMsg.ChannelType ch = state != null ?
                state.multiroomChannel : MultiroomDeviceInformationMsg.ChannelType.NONE;

        if (ch != MultiroomDeviceInformationMsg.ChannelType.NONE && activity.isMultiroomAvailable())
        {
            final MultiroomChannelSettingMsg cmd = new MultiroomChannelSettingMsg(
                    state.getActiveZone() + 1, MultiroomChannelSettingMsg.getUpType(ch));
            b.setVisibility(View.VISIBLE);
            b.setText(ch.toString());
            setButtonEnabled(b, state.isOn());
            final Drawable icon = Utils.getDrawable(activity, R.drawable.cmd_multiroom_channel);
            Utils.setDrawableColorAttr(activity, icon,
                    state.isOn() ? R.attr.colorButtonEnabled : R.attr.colorButtonDisabled);
            b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
            prepareButtonListeners(b, cmd, null);
        }
        else
        {
            b.setVisibility(View.GONE);
            setButtonEnabled(b, false);
            b.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
    }


    /*
     * Volume control
     */
    private void updateVolumeLevel(View view, @Nullable final State state)
    {
        final boolean volumeValid = state != null && state.isOn() && state.volumeLevel != MasterVolumeMsg.NO_LEVEL;
        if (view instanceof AppCompatButton)
        {
            final AppCompatButton b = (AppCompatButton) view;
            final Drawable icon = Utils.getDrawable(activity, R.drawable.volume_audio_control);
            b.setText(volumeValid ?
                    State.getVolumeLevelStr(state.volumeLevel, state.getActiveZoneInfo()) : "");
            setButtonEnabled(b, volumeValid);
            Utils.setDrawableColorAttr(activity, icon, volumeValid ?
                    R.attr.colorButtonEnabled : R.attr.colorButtonDisabled);
            b.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
        }
        else if (view instanceof AppCompatSeekBar)
        {
            final AppCompatSeekBar b = (AppCompatSeekBar) view;
            if (volumeValid)
            {
                final ReceiverInformationMsg.Zone zone = state.getActiveZoneInfo();
                final int maxVolume = Math.min(audioControlManager.getVolumeMax(state, zone),
                        activity.getConfiguration().audioControl.getMasterVolumeMax());
                b.setMax(maxVolume);
                b.setProgress(Math.max(0, state.volumeLevel));

            }
            else
            {
                b.setMax(10);
                b.setProgress(0);
            }
            b.setEnabled(volumeValid);
        }
    }

    @Override
    public void onMasterVolumeMaxUpdate(@NonNull final State state)
    {
        // This callback is called if mater volume maximum is changed.
        // We shall re-scale master volume slider if it is visible
        for (View view : deviceSoundButtons)
        {
            if (view instanceof AppCompatSeekBar && audioControlManager.isVolumeLevel(view))
            {
                updateVolumeLevel(view, state);
            }
        }
    }

    @Override
    public void onMasterVolumeChange(int progressChanged)
    {
        // This callback is called when master volume slider is changed.
        // We shall update the text of the "Audio control" button
        if (!activity.isConnected())
        {
            return;
        }
        final State state = activity.getStateManager().getState();
        for (View view : deviceSoundButtons)
        {
            if (view instanceof AppCompatButton && audioControlManager.isVolumeLevel(view))
            {
                final AppCompatButton b = (AppCompatButton) view;
                final String vol = State.getVolumeLevelStr(progressChanged, state.getActiveZoneInfo());
                b.setText(vol);
            }
        }
    }

    /*
     * Time-seek control
     */
    private void updateProgressBar(@NonNull final State state)
    {
        ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(state.currentTime);
        ((TextView) rootView.findViewById(R.id.tv_time_end)).setText(state.maxTime);
        final int currTime = Utils.timeToSeconds(state.currentTime);
        final int maxTime = Utils.timeToSeconds(state.maxTime);
        if (currTime >= 0 && maxTime >= 0)
        {
            seekBar.setMax(maxTime);
            seekBar.setProgress(currTime);
        }
        else
        {
            seekBar.setMax(1000);
            seekBar.setProgress(0);
        }
        seekBar.setEnabled(state.isPlaying() && state.timeSeek == MenuStatusMsg.TimeSeek.ENABLE);
    }

    private void seekTime(int newSec)
    {
        final State state = activity.getStateManager().getState();
        final int currTime = Utils.timeToSeconds(state.currentTime);
        final int maxTime = Utils.timeToSeconds(state.maxTime);
        if (currTime >= 0 && maxTime >= 0)
        {
            final int hour = newSec / 3600;
            final int min = (newSec - hour * 3600) / 60;
            final int sec = newSec - hour * 3600 - min * 60;
            activity.getStateManager().requestSkipNextTimeMsg(2);
            final TimeSeekMsg msg = new TimeSeekMsg(state.getModel(), hour, min, sec);
            state.currentTime = msg.getTimeAsString();
            ((TextView) rootView.findViewById(R.id.tv_time_start)).setText(state.currentTime);
            activity.getStateManager().sendMessage(msg);
        }
    }

    private void updateFeedButton(final AppCompatImageButton btn, final MenuStatusMsg.Feed feed, ServiceType serviceType)
    {
        btn.setVisibility(feed.isImageValid() ? View.VISIBLE : View.GONE);
        if (feed.isImageValid())
        {
            btn.setImageResource(feed.getImageId());
            setButtonEnabled(btn, true);
            final boolean isSelected = serviceType == ServiceType.AMAZON_MUSIC ?
                    feed == MenuStatusMsg.Feed.LIKE : feed == MenuStatusMsg.Feed.LOVE;
            setButtonSelected(btn, isSelected);
        }
    }

    private void showPresetMemoryDialog(@NonNull final State state)
    {
        final FrameLayout frameView = new FrameLayout(activity);
        activity.getLayoutInflater().inflate(R.layout.dialog_preset_memory, frameView);

        final HorizontalNumberPicker numberPicker = frameView.findViewById(R.id.preset_memory_number);
        numberPicker.minValue = 1;
        numberPicker.maxValue = PresetMemoryMsg.MAX_NUMBER;
        numberPicker.setValue(state.nextEmptyPreset());
        numberPicker.setEnabled(true);

        final Drawable icon = Utils.getDrawable(activity, R.drawable.cmd_track_menu);
        Utils.setDrawableColorAttr(activity, icon, android.R.attr.textColorSecondary);
        final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.cmd_preset_memory)
                .setIcon(icon)
                .setCancelable(true)
                .setView(frameView)
                .setNegativeButton(activity.getResources().getString(R.string.action_cancel), (dialog1, which) ->
                {
                    Utils.showSoftKeyboard(activity, numberPicker, false);
                    dialog1.dismiss();
                })
                .setPositiveButton(activity.getResources().getString(R.string.action_ok), (dialog12, which) ->
                {
                    Utils.showSoftKeyboard(activity, numberPicker, false);
                    // in order to get updated preset list, we need to request Receiver Information
                    activity.getStateManager().requestRIonPreset(true);
                    activity.getStateManager().sendMessage(new PresetMemoryMsg(numberPicker.getValue()));
                })
                .create();

        dialog.show();
        Utils.fixIconColor(dialog, android.R.attr.textColorSecondary);
    }
}
