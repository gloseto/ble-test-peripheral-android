/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.webbluetoothcg.bletestperipheral;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.Date;
import java.util.UUID;

import io.github.webbluetoothcg.bletestperipheral.gatt.AllGattCharacteristics;
import io.github.webbluetoothcg.bletestperipheral.gatt.AllGattServices;


public class FoodScaleServiceFragment extends ServiceFragment {

  private static final UUID WEIGHT_SCALE_SERVICE_UUID = AllGattServices.lookup("Food Scale (custom)");

  private static final UUID WEIGHT_MEASUREMENT_LEVEL_UUID = AllGattCharacteristics.lookup("Weight Measurement (custom)");

  private static final int INITIAL_WEIGHT_MEASUREMENT_LEVEL = 10;
  private static final int WEIGHT_MEASUREMENT_LEVEL_MAX = 100;
  private static final String WEIGHT_MEASUREMENT_LEVEL_DESCRIPTION = "This characteristic is used " +
          "to send a weight measurement.";

  private ServiceFragmentDelegate mDelegate;
  // UI
  private EditText mWeightLevelEditText;
  private final OnEditorActionListener mOnEditorActionListener = new OnEditorActionListener() {
    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_DONE) {
        String newWeightLevelString = textView.getText().toString();
        // Need to check if the string is empty since isDigitsOnly returns
        // true for empty strings.
        if (!newWeightLevelString.isEmpty()
            && android.text.TextUtils.isDigitsOnly(newWeightLevelString)) {
          int newWeightLevel = Integer.parseInt(newWeightLevelString);
          if (newWeightLevel <= WEIGHT_MEASUREMENT_LEVEL_MAX) {
            setWeightLevel(newWeightLevel, textView);
          } else {
            Toast.makeText(getActivity(), R.string.weigthScaleLevelTooHigh, Toast.LENGTH_SHORT)
                .show();
          }
        } else {
          Toast.makeText(getActivity(), R.string.weigthScaleLevelIncorrect, Toast.LENGTH_SHORT)
              .show();
        }
      }
      return false;
    }
  };
  private SeekBar mWeightLevelSeekBar;
  private final OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      if (fromUser) {
        setWeightLevel(progress, seekBar);
      }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
  };

  private final OnClickListener mNotifyButtonListener = new OnClickListener() {
    @Override
    public void onClick(View v) {
      mDelegate.sendNotificationToDevices(mWeightLevelCharacteristic);
    }
  };

  // GATT
  private BluetoothGattService mWeightScaleService;
  private BluetoothGattCharacteristic mWeightLevelCharacteristic;

  public FoodScaleServiceFragment() {

    mWeightLevelCharacteristic =
        new BluetoothGattCharacteristic(WEIGHT_MEASUREMENT_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mWeightLevelCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mWeightLevelCharacteristic.addDescriptor(
        Peripheral.getCharacteristicUserDescriptionDescriptor(WEIGHT_MEASUREMENT_LEVEL_DESCRIPTION));

    mWeightScaleService = new BluetoothGattService(WEIGHT_SCALE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mWeightScaleService.addCharacteristic(mWeightLevelCharacteristic);
  }

  // Lifecycle callbacks
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_weight_scale, container, false);

    mWeightLevelEditText = (EditText) view.findViewById(R.id.textView_weigthScaleLevel);
    mWeightLevelEditText.setOnEditorActionListener(mOnEditorActionListener);
    mWeightLevelSeekBar = (SeekBar) view.findViewById(R.id.seekBar_weigthScaleLevel);
    mWeightLevelSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    Button notifyButton = (Button) view.findViewById(R.id.button_weigthScaleLevelNotify);
    notifyButton.setOnClickListener(mNotifyButtonListener);

    setWeightLevel(INITIAL_WEIGHT_MEASUREMENT_LEVEL, null);
    return view;
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      mDelegate = (ServiceFragmentDelegate) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(activity.toString()
          + " must implement ServiceFragmentDelegate");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mDelegate = null;
  }

  public BluetoothGattService getBluetoothGattService() {
    return mWeightScaleService;
  }

  @Override
  public ParcelUuid getServiceUUID() {
    return new ParcelUuid(WEIGHT_SCALE_SERVICE_UUID);
  }

  private void setWeightLevel(int newWeightLevel, View source) {

    mWeightLevelCharacteristic.setValue(newWeightLevel,
      BluetoothGattCharacteristic.FORMAT_UINT16, 0);

    if (source != mWeightLevelSeekBar) {
      mWeightLevelSeekBar.setProgress(newWeightLevel);
    }
    if (source != mWeightLevelEditText) {
      mWeightLevelEditText.setText(Integer.toString(newWeightLevel));
    }
  }

  @Override
  public void notificationsEnabled(BluetoothGattCharacteristic characteristic, boolean indicate) {
    if (characteristic.getUuid() != WEIGHT_MEASUREMENT_LEVEL_UUID) {
      return;
    }
    if (indicate) {
      return;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getActivity(), R.string.notificationsEnabled, Toast.LENGTH_SHORT)
            .show();
      }
    });
  }

  @Override
  public void notificationsDisabled(BluetoothGattCharacteristic characteristic) {
    if (characteristic.getUuid() != WEIGHT_MEASUREMENT_LEVEL_UUID) {
      return;
    }
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getActivity(), R.string.notificationsNotEnabled, Toast.LENGTH_SHORT)
            .show();
      }
    });
  }
}
