package com.ghelius.narodmon;


import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FilterDialog extends DialogFragment implements DialogInterface.OnClickListener {

    final String LOG_TAG = "myLogs";

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle("Filtering and sorting");
        View v = inflater.inflate(R.layout.filter_dialog, null);
//        v.findViewById(R.id.btnYes).setOnClickListener(this);
//        v.findViewById(R.id.btnNo).setOnClickListener(this);
//        v.findViewById(R.id.btnMaybe).setOnClickListener(this);
        return v;
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(LOG_TAG, "Dialog 1: onDismiss");
    }

    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(LOG_TAG, "Dialog 1: onCancel");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }
}
