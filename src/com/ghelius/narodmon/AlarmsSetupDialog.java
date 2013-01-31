package com.ghelius.narodmon;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created with IntelliJ IDEA.
 * User: eugene
 * Date: 1/30/13
 * Time: 4:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlarmsSetupDialog extends DialogFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Filtering and sorting");
        View v = inflater.inflate(R.layout.alarms_setup_dialog, null);
        return v;
    }
}
