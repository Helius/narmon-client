package com.ghelius.narodmon;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

public class LoginDialog extends android.support.v4.app.DialogFragment implements DialogInterface.OnClickListener  {
	static final private String TAG = "narodmon-loginDialog";
	private LoginEventListener listener = null;
	private SharedPreferences prefs;
	private Button loginButton = null;
	private TextView loginStatus = null;
	private CheckBox autoCheckBox;
	private TextView passwordTextView;
	private TextView loginTextView;

	public void updateLoginStatus() {
		if (listener.loginStatus() == MainActivity.LoginStatus.LOGIN)
			if (isVisible()) dismiss();
		Log.d(TAG,"update login status");
		updateLoginStatusText();
		updateButtonText();
	}

	private void updateLoginStatusText () {
		if (loginStatus != null) {
			setFeildsEnable(true);
			if (listener.loginStatus() == MainActivity.LoginStatus.LOGIN)
				loginStatus.setText(getString(R.string.login_dialog_youarelogin));
			else if (listener.loginStatus() == MainActivity.LoginStatus.LOGOUT)
				loginStatus.setText(getString(R.string.login_dialog_youarenotlogin));
			else if (listener.loginStatus() == MainActivity.LoginStatus.ERROR)
				loginStatus.setText(getString(R.string.login_dialog_autherror));
		}
	}

	private void updateButtonText () {
		if (loginButton != null) {
			if (listener.loginStatus() == MainActivity.LoginStatus.LOGIN) {
				loginButton.setText(getString(R.string.login_dialog_button_logout_text));
			} else {
				loginButton.setText(getString(R.string.login_dialog_button_login_text));
			}
			loginButton.setEnabled (true);
		}
	}

	interface LoginEventListener {
		void login();
		void logout();
		SharedPreferences getPreference ();
		MainActivity.LoginStatus loginStatus();
	}
	public void setOnChangeListener (LoginEventListener listener) {
		this.listener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		getDialog().setTitle(this.getString(R.string.login_dialog_title));
		final View v = inflater.inflate(R.layout.login_dialog_activity, null);
		if (listener != null)
			prefs = listener.getPreference();


		loginTextView = (TextView) v.findViewById(R.id.login_textview);
		loginTextView.setText(prefs.getString(getString(R.string.pref_key_login),""));
		loginTextView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				prefs.edit().putString(getString(R.string.pref_key_login), s.toString()).commit();
			}
		});

		passwordTextView = (TextView) v.findViewById (R.id.passwordTextEdit);
		passwordTextView.setText(prefs.getString(getString(R.string.pref_key_passwd), ""));
		passwordTextView.addTextChangedListener(new TextWatcher() {
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				prefs.edit().putString(getString(R.string.pref_key_passwd), s.toString()).commit();
			}
		});

		autoCheckBox = (CheckBox) v.findViewById(R.id.loginAutoCheckBox);
		autoCheckBox.setChecked(prefs.getBoolean(getString(R.string.pref_key_autologin),false));
		autoCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				prefs.edit().putBoolean(getString(R.string.pref_key_autologin),isChecked).commit();
			}
		});

		loginStatus = (TextView) v.findViewById(R.id.loginAsText);

		loginButton = (Button) v.findViewById(R.id.loginButton);
		loginButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginButton.setEnabled(false);
				setFeildsEnable(false);
				if (listener.loginStatus() == MainActivity.LoginStatus.LOGIN)
					listener.logout();
				else
					listener.login();
			}
		});

		updateButtonText();
		updateLoginStatusText();
		return v;
	}

	private void setFeildsEnable (boolean enable) {
		if (enable) {
			passwordTextView.setEnabled(true);
			loginTextView.setEnabled(true);
			autoCheckBox.setEnabled(true);
		} else {
			passwordTextView.setEnabled(false);
			loginTextView.setEnabled(false);
			autoCheckBox.setEnabled(false);
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
	}
}