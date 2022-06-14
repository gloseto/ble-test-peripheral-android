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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

import io.github.webbluetoothcg.bletestperipheral.gatt.AllGattServices;


public class Peripherals extends ListActivity {

  private static final List<String> SUPPORTED_PERIPHERALS = Arrays.asList(
          "Battery Service", "Heart Rate", "Health Thermometer", "Weight Scale", "Food Scale (custom)");
  private static final String[] PERIPHERALS_NAMES = AllGattServices.getAllServices();
  public final static String EXTRA_PERIPHERAL_SERVICE = "PERIPHERAL_SERVICE";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_peripherals_list);
    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
        /* layout for the list item */ android.R.layout.simple_list_item_1,
        /* id of the TextView to use */ android.R.id.text1,
        /* values for the list */ PERIPHERALS_NAMES);
    setListAdapter(adapter);
  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);

    String peripheralService = PERIPHERALS_NAMES[position];
    if (SUPPORTED_PERIPHERALS.contains(peripheralService)) {
      Intent intent = new Intent(this, Peripheral.class);
      intent.putExtra(EXTRA_PERIPHERAL_SERVICE, peripheralService);
      startActivity(intent);
    } else {
      Toast.makeText(getApplicationContext(),
              peripheralService + " - Service doesn't exist", Toast.LENGTH_SHORT).show();
      Log.wtf(Peripherals.class.getCanonicalName(), peripheralService + " - Service doesn't exist");
    }
  }

}
