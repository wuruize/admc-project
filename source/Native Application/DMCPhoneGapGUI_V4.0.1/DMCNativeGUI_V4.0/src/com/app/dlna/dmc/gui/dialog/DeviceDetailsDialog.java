package com.app.dlna.dmc.gui.dialog;

import org.teleal.cling.model.meta.Device;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import app.dlna.controller.v4.R;

public class DeviceDetailsDialog extends Dialog {

	protected static final String TAG = DeviceDetailsDialog.class.getName();

	@SuppressWarnings("rawtypes")
	private Device m_device;
	private DeviceDetailsListener m_listener;

	@SuppressWarnings("rawtypes")
	public DeviceDetailsDialog(Context context, Device device, DeviceDetailsListener listener) {
		super(context);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		m_device = device;
		m_listener = listener;

		setContentView(R.layout.dl_devicedetails);
		((Button) findViewById(R.id.btn_select)).setOnClickListener(m_selectClick);
		((Button) findViewById(R.id.btn_writeToTag)).setOnClickListener(m_saveTagClick);
		((Button) findViewById(R.id.btn_close)).setOnClickListener(m_closeClick);

		((TextView) findViewById(R.id.deviceName)).setText(device.getDetails().getFriendlyName());
		((TextView) findViewById(R.id.deviceType)).setText(device.getType().getType());
		((TextView) findViewById(R.id.deviceUDN)).setText(device.getIdentity().getUdn().toString());
	}

	private android.view.View.OnClickListener m_selectClick = new android.view.View.OnClickListener() {

		@Override
		public void onClick(View v) {
			m_listener.onSelectClick(m_device);
			DeviceDetailsDialog.this.dismiss();
		}
	};

	private android.view.View.OnClickListener m_saveTagClick = new android.view.View.OnClickListener() {

		@Override
		public void onClick(View v) {
			m_listener.onWriteTAGClick(m_device);
		}
	};

	private android.view.View.OnClickListener m_closeClick = new android.view.View.OnClickListener() {

		@Override
		public void onClick(View v) {
			DeviceDetailsDialog.this.dismiss();
		}
	};

	public interface DeviceDetailsListener {
		@SuppressWarnings("rawtypes")
		void onSelectClick(Device device);

		@SuppressWarnings("rawtypes")
		void onWriteTAGClick(Device device);
	}
}
