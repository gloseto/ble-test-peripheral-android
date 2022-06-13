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


public class WeightScaleServiceFragment extends ServiceFragment {

  private static final UUID WEIGHT_SCALE_SERVICE_UUID = AllGattServices.lookup("Weight Scale");

  private static final UUID WEIGHT_MEASUREMENT_LEVEL_UUID = AllGattCharacteristics.lookup("Weight Measurement");
  private static final UUID WEIGHT_SCALE_FEATURE_UUID = AllGattCharacteristics.lookup("Weight Scale Feature");

  private static final int INITIAL_WEIGHT_MEASUREMENT_LEVEL = 10;
  private static final int WEIGHT_MEASUREMENT_LEVEL_MAX = 100;
  private static final String WEIGHT_MEASUREMENT_LEVEL_DESCRIPTION = "This characteristic is used " +
          "to send a weight measurement.";
  private static final String WEIGHT_SCALE_FEATURE_DESCRIPTION = "This characteristic is used " +
          "to set weight measurement parameters.";

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
            Toast.makeText(getActivity(), R.string.batteryLevelTooHigh, Toast.LENGTH_SHORT)
                .show();
          }
        } else {
          Toast.makeText(getActivity(), R.string.batteryLevelIncorrect, Toast.LENGTH_SHORT)
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
  private BluetoothGattCharacteristic mWeightScaleFeatureCharacteristic;

  public WeightScaleServiceFragment() {
    mWeightLevelCharacteristic =
        new BluetoothGattCharacteristic(WEIGHT_MEASUREMENT_LEVEL_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ);

    mWeightLevelCharacteristic.addDescriptor(
        Peripheral.getClientCharacteristicConfigurationDescriptor());

    mWeightLevelCharacteristic.addDescriptor(
        Peripheral.getCharacteristicUserDescriptionDescriptor(WEIGHT_MEASUREMENT_LEVEL_DESCRIPTION));

    mWeightScaleFeatureCharacteristic =
            new BluetoothGattCharacteristic(WEIGHT_SCALE_FEATURE_UUID,
                    BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                    BluetoothGattCharacteristic.PERMISSION_READ);

    mWeightScaleFeatureCharacteristic.addDescriptor(
            Peripheral.getClientCharacteristicConfigurationDescriptor());

    mWeightScaleFeatureCharacteristic.addDescriptor(
            Peripheral.getCharacteristicUserDescriptionDescriptor(WEIGHT_SCALE_FEATURE_DESCRIPTION));

    mWeightScaleService = new BluetoothGattService(WEIGHT_SCALE_SERVICE_UUID,
        BluetoothGattService.SERVICE_TYPE_PRIMARY);
    mWeightScaleService.addCharacteristic(mWeightLevelCharacteristic);
    mWeightScaleService.addCharacteristic(mWeightScaleFeatureCharacteristic);
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

    /*
     * Weight Scale Feature Bit String Value (32 bit)
     * 0-9    Various Lifetime
     *   - Time Stamp Supported (bit 0)
     *   - Multiple Users Supported (bit 1)
     *   - BMI Supported (bit 2)
     *   - Weight Measurement Resolution bits (bits 3-6)
     *   - Height Measurement Resolution bits (bits 7-9)
     * 10-31  Reserved for Future Use Not defined.
     * ex. 0b00111000 (timestamp unsupported; multiple users unsupported; BMI unsupported; weight resolution 0.005 kg; height measurement unsupported)
     */
    mWeightScaleFeatureCharacteristic.setValue(new byte[]{0b00111001, 0, 0, 0});

    /*
     * Weight Scale Measurement
     * Flags (8bit) + Weight Measurement Value (uint16) = 3 bytes
     *    - Measurement Units (bit 0)
     *    - Time Stamp Present (bit 1)
     *    - User ID Present (bit 2)
     *    - BMI and Height Present (bit 3)
     * [optional] Time Stamp (7 bytes)
     *    <Field name="Year"> <Format>uint16</Format> = 2 bytes
     *    <Field name="Month"> <Format>uint8</Format> = 1 byte
     *    <Field name="Day"> <Format>uint8</Format> = 1 byte
     *    <Field name="Hours"> <Format>uint8</Format> = 1 byte
     *    <Field name="Minutes"> <Format>uint8</Format> = 1 byte
     *    <Field name="Seconds"> <Format>uint8</Format> = 1 byte
     */

    int weight = (int) (newWeightLevel / 0.005);

    mWeightLevelCharacteristic.setValue(new byte[]{0b00000010, 0, 0, 0, 0, 0, 0, 0, 0, 0});
    mWeightLevelCharacteristic.setValue(weight,
      BluetoothGattCharacteristic.FORMAT_UINT16, /* offset */ 1);

    // timestamp
    Date date = new Date();

    mWeightLevelCharacteristic.setValue(date.getYear() + 1900,
            BluetoothGattCharacteristic.FORMAT_UINT16, /* offset */ 3);
    mWeightLevelCharacteristic.setValue(date.getMonth() + 1,
            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 5);
    mWeightLevelCharacteristic.setValue(date.getDate(),
            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 6);
    mWeightLevelCharacteristic.setValue(date.getHours(),
            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 7);
    mWeightLevelCharacteristic.setValue(date.getMinutes(),
            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 8);
    mWeightLevelCharacteristic.setValue(date.getSeconds(),
            BluetoothGattCharacteristic.FORMAT_UINT8, /* offset */ 9);


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
