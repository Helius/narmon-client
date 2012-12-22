package com.ghelius.narodmon;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: eugene
 * Date: 12/22/12
 * Time: 2:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class SensorInfo extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensorinfo);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Sensor sensor = (Sensor)extras.getSerializable("Sensor");
            TextView name = (TextView) findViewById(R.id.text_name);
            TextView location = (TextView) findViewById(R.id.text_location);
            TextView distance = (TextView) findViewById(R.id.text_distance);
            TextView time = (TextView) findViewById(R.id.text_time);
            TextView value = (TextView) findViewById(R.id.text_value);
            TextView type = (TextView) findViewById(R.id.text_type);
            ImageView icon = (ImageView) findViewById(R.id.info_sensor_icon);

            name.setText(sensor.getName());
            location.setText(sensor.getLocation());
            distance.setText(sensor.getDistance().toString());

            String types = getString(R.string.type_unknown);
            switch (sensor.getType()) {
                case 1:
                    types = getString(R.string.type_termometr);
                    icon.setImageResource(R.drawable.termo_icon);
                    break;
                case 2:
                    types = getString(R.string.type_pressure);
                    icon.setImageResource(R.drawable.pressure_icon);
                    break;
                case 3:
                    types = getString(R.string.type_humidity);
                    icon.setImageResource(R.drawable.humid_icon);
                    break;
                default:
                    types = getString(R.string.type_unknown);
                    icon.setImageResource(R.drawable.unknown_icon);
                    break;
            }
            type.setText(types);
            setTitle(types + " sensor");
            value.setText(sensor.getValue());

            long dv = Long.valueOf(sensor.getTime())*1000;// its need to be in milisecond
            Date df = new java.util.Date(dv);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getDefault());
            String vv = sdf.format(df);
            time.setText(vv);
        } else {
            finish();
        }
    }
}