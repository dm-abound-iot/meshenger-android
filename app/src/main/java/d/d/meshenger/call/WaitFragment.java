/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package d.d.meshenger.call;

import android.app.Fragment;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.webrtc.StatsReport;

import java.util.HashMap;
import java.util.Map;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;
import static org.webrtc.ContextUtils.getApplicationContext;
import d.d.meshenger.R;
import d.d.meshenger.Log;

/**
 * Show "Calling..." screen and ring/vibrate phone for incoming calls.
 */
public class WaitFragment extends Fragment {
  private static final String TAG = "WaitFragment";
  private ImageButton callAcceptIB;
  private ImageButton callDeclineIB;
  private TextView callNameTV;
  private TextView callStatusTV;

  private Vibrator vibrator;
  private Ringtone ringtone;

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View controlView = inflater.inflate(R.layout.fragment_wait, container, false);

    callAcceptIB = controlView.findViewById(R.id.callAccept);
    callDeclineIB = controlView.findViewById(R.id.callDecline);
    callNameTV = controlView.findViewById(R.id.callName);
    callStatusTV = controlView.findViewById(R.id.callStatus);

    callAcceptIB.setOnClickListener((View view) -> {
      // TODO
    });

    callDeclineIB.setOnClickListener((View view) -> {
        // TODO
    });

    return controlView;
  }

  private void startRinging() {
      Log.d(TAG, "startRinging");
      int ringerMode = ((AudioManager) getActivity().getSystemService(AUDIO_SERVICE)).getRingerMode();

      if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
          return;
      }

      vibrator = ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE));
      long[] pattern = {1500, 800, 800, 800};
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          VibrationEffect vibe = VibrationEffect.createWaveform(pattern, 0);
          vibrator.vibrate(vibe);
      } else {
          vibrator.vibrate(pattern, 0);
      }

      if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
          return;
      }

      ringtone = RingtoneManager.getRingtone(getActivity().getApplicationContext(), RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE));
      ringtone.play();
  }

  private void stopRinging(){
      Log.d(TAG, "stopRinging");
      if (vibrator != null) {
          vibrator.cancel();
          vibrator = null;
      }

      if (ringtone != null){
          ringtone.stop();
          ringtone = null;
      }
  }

  @Override
  public void onStop() {
      stopRinging();
      super.onStop();
  }
}
