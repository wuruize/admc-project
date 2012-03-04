package com.app.dlna.dmc.gui.plugin.devices;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Icon;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.model.types.UDN;
import org.teleal.common.util.Base64Coder;

import android.util.Log;

import com.app.dlna.dmc.gui.UIWithPhonegapActivity;
import com.app.dlna.dmc.processor.interfaces.UpnpProcessor.UpnpProcessorListener;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class DevicesPlugin extends Plugin implements UpnpProcessorListener {
	private static final String TAG = DevicesPlugin.class.getSimpleName();
	private static final String ACTION_START = "start";
	private static final String ACTION_STOP = "stop";
	private static final String ACTION_SET_DMS = "setDMS";
	private static final String ACTION_SET_DMR = "setDMR";
	@SuppressWarnings("rawtypes")
	private List<Device> m_dms_list = new ArrayList<Device>();
	@SuppressWarnings("rawtypes")
	private List<Device> m_dmr_list = new ArrayList<Device>();

	@SuppressWarnings("rawtypes")
	@Override
	public PluginResult execute(String action, JSONArray data, String callID) {

		PluginResult result = new PluginResult(Status.OK);
		if (ACTION_START.equals(action)) {
			Log.i(TAG, "Call start");
			UIWithPhonegapActivity.UPNP_PROCESSOR.addListener(this);
			for (Device device : UIWithPhonegapActivity.UPNP_PROCESSOR.getDMSList()) {
				addDMS(device);
			}
			for (Device device : UIWithPhonegapActivity.UPNP_PROCESSOR.getDMRList()) {
				addDMR(device);
			}
		} else if (ACTION_STOP.equals(action)) {
			Log.e(TAG, "Call stop");
			UIWithPhonegapActivity.UPNP_PROCESSOR.removeListener(this);
		} else if (ACTION_SET_DMS.equals(action)) {
			Log.e(TAG, "Call SetDMS");
			try {
				setDMS(data.getString(0));
			} catch (Exception ex) {
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_SET_DMR.equals(action)) {
			Log.e(TAG, "Call SetDMR");
			try {
				setDMR(data.getString(0));
			} catch (Exception ex) {
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		}
		return result;
	}

	private void setDMR(String udn) {
		UIWithPhonegapActivity.UPNP_PROCESSOR.setCurrentDMR(new UDN(udn));
	}

	private void setDMS(String udn) {
		UIWithPhonegapActivity.UPNP_PROCESSOR.setCurrentDMS(new UDN(udn));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceAdded(Device device) {
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				addDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				addDMR(device);
			}
		}
	}

	// @SuppressWarnings("rawtypes")
	// private void addDMR(Device device) {
	// if (!m_dmr_list.contains(device)) {
	// m_dmr_list.add(device);
	// String dmr_html = createDeviceElement(device, "dmr");
	// sendJavascript("add_device(" + dmr_html + ",'dmr');");
	// }
	// }

	@SuppressWarnings("rawtypes")
	private void addDMR(Device device) {
		if (!m_dmr_list.contains(device)) {
			m_dmr_list.add(device);
			String jsonString = createDeviceElement(device, "dmr");
			sendJavascript("add_device(" + jsonString + ",'dmr');");
		}
	}

	@SuppressWarnings("rawtypes")
	private void addDMS(Device device) {
		if (!m_dms_list.contains(device)) {
			m_dms_list.add(device);
			String jsonString = createDeviceElement(device, "dms");
			Log.e(TAG, "JsonString = " + jsonString);
			sendJavascript("add_device(" + jsonString + ",'dms');");
		}
	}

	@SuppressWarnings("rawtypes")
	private String createDeviceElement(Device device, String type) {
		JSONObject jsonDevice = new JSONObject();
		final String udn = device.getIdentity().getUdn().getIdentifierString();
		String deviceImage = "";
		String deviceAddress = "";
		String deviceName = "";
		deviceName = device.getDetails().getFriendlyName();
		if (device instanceof RemoteDevice) {
			deviceAddress = ((RemoteDevice) device).getIdentity().getDescriptorURL().getAuthority();
			final Icon[] icons = device.getIcons();
			if (icons != null && icons[0] != null && icons[0].getUri() != null) {

				final RemoteDevice remoteDevice = (RemoteDevice) device;

				deviceImage = remoteDevice.getIdentity().getDescriptorURL().getProtocol() + "://"
						+ remoteDevice.getIdentity().getDescriptorURL().getAuthority() + icons[0].getUri().toString();
			}
		} else {
			deviceAddress = "Local Device";
			byte[] bytes = device.getIcons()[0].getData();
			if (bytes.length != 0) {
				String base64String = new String(Base64Coder.encode(bytes));
				Log.e(TAG, base64String);
				deviceImage = "data:image/png;base64," + base64String;
			}
		}
		try {
			jsonDevice.put("name", deviceName);
			jsonDevice.put("type", type);
			jsonDevice.put("udn", udn);
			jsonDevice.put("icon", deviceImage);
			jsonDevice.put("address", deviceAddress);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonDevice.toString();
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void onDeviceRemoved(Device device) {
		if (device.getType().getNamespace().equals("schemas-upnp-org")) {
			if (device.getType().getType().equals("MediaServer")) {
				removeDMS(device);
			} else if (device.getType().getType().equals("MediaRenderer")) {
				removeDMR(device);
			}
		}
	}

	@SuppressWarnings("rawtypes")
	private void removeDMR(Device device) {
		sendJavascript("remove_device('" + device.getIdentity().getUdn().getIdentifierString() + "');");
		m_dmr_list.remove(device);
	}

	@SuppressWarnings("rawtypes")
	private void removeDMS(Device device) {
		sendJavascript("remove_device('" + device.getIdentity().getUdn().getIdentifierString() + "');");
		m_dms_list.remove(device);
	}

	@Override
	public void onStartComplete() {
	}

}
