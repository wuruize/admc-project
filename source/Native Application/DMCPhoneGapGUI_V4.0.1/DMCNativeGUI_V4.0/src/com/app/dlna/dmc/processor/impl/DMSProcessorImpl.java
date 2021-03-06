package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teleal.cling.controlpoint.ActionCallback;
import org.teleal.cling.controlpoint.ControlPoint;
import org.teleal.cling.model.action.ActionInvocation;
import org.teleal.cling.model.message.UpnpResponse;
import org.teleal.cling.model.meta.Action;
import org.teleal.cling.model.meta.Device;
import org.teleal.cling.model.meta.Service;
import org.teleal.cling.model.types.ServiceType;
import org.teleal.cling.model.types.UnsignedIntegerFourBytes;
import org.teleal.cling.support.contentdirectory.DIDLParser;
import org.teleal.cling.support.model.DIDLContent;
import org.teleal.cling.support.model.DIDLObject;
import org.teleal.cling.support.model.container.Container;
import org.teleal.cling.support.model.item.Item;

import android.util.Log;

import com.app.dlna.dmc.processor.interfaces.DMSProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;

public class DMSProcessorImpl implements DMSProcessor {

	private static final String TAG = DMSProcessorImpl.class.getName();
	private static final String ACTION_REMOVE = "Remove";
	private static final String ACTION_ADD = "Add";
	@SuppressWarnings("rawtypes")
	private Device m_server;
	private ControlPoint m_controlPoint;
	private List<String> m_traceID;
	private int m_currentPageIndex;
	private String m_currentObjectId;
	public static int ITEM_PER_PAGE = 25;
	private List<DIDLObject> m_DIDLObjectList;

	@SuppressWarnings("rawtypes")
	public DMSProcessorImpl(Device device, ControlPoint controlPoint) {
		m_server = device;
		m_controlPoint = controlPoint;
		m_traceID = new ArrayList<String>();
		m_traceID.add("-1");
		m_DIDLObjectList = new ArrayList<DIDLObject>();
		m_currentObjectId = "-1";
	}

	public void dispose() {

	}

	@Override
	public void browse(String objectID, int pageIndex, final DMSProcessorListner listener) {
		m_traceID.add(objectID);
		m_currentPageIndex = 0;
		executeBrowse(objectID, pageIndex, listener);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void executeBrowse(final String objectID, final int pageIndex, final DMSProcessorListner listener) {
		m_currentObjectId = objectID;
		Service cds = m_server.findService(new ServiceType("schemas-upnp-org", "ContentDirectory"));
		if (cds != null) {
			m_currentPageIndex = pageIndex;
			int startIndex = pageIndex * ITEM_PER_PAGE;
			Action action = cds.getAction("Browse");
			ActionInvocation actionInvocation = new ActionInvocation(action);
			actionInvocation.setInput("ObjectID", objectID);
			actionInvocation.setInput("BrowseFlag", "BrowseDirectChildren");
			actionInvocation.setInput("Filter", "*");
			actionInvocation.setInput("StartingIndex", new UnsignedIntegerFourBytes(startIndex));
			actionInvocation.setInput("RequestedCount", new UnsignedIntegerFourBytes(ITEM_PER_PAGE + 1));
			actionInvocation.setInput("SortCriteria", null);
			ActionCallback actionCallback = new ActionCallback(actionInvocation) {

				@Override
				public void success(ActionInvocation invocation) {
					try {
						DIDLParser parser = new DIDLParser();
						DIDLContent content = parser.parse(invocation.getOutput("Result").toString());
						Log.i(TAG, "Container = " + content.getContainers().size() + " Item = "
								+ content.getItems().size());
						Map<String, List<? extends DIDLObject>> result = new HashMap<String, List<? extends DIDLObject>>();
						List<Container> containers = content.getContainers();
						List<Item> items = content.getItems();
						boolean haveNext = false;
						if (containers.size() > ITEM_PER_PAGE) {
							haveNext = true;
							containers.remove(containers.size() - 1);
						} else if (items.size() > ITEM_PER_PAGE || items.size() + containers.size() > ITEM_PER_PAGE) {
							haveNext = true;
							items.remove(items.size() - 1);
						}
						m_DIDLObjectList.clear();
						m_DIDLObjectList.addAll(containers);
						m_DIDLObjectList.addAll(items);
						boolean havePrev = m_currentPageIndex > 0;
						result.put("Containers", containers);
						result.put("Items", items);
						listener.onBrowseComplete(objectID, haveNext, havePrev, result);
					} catch (Exception e) {
						e.printStackTrace();
						listener.onBrowseFail(e.getMessage());
						m_traceID.remove(m_traceID.size() - 1);
					}
				}

				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					Log.e(TAG, defaultMsg);
					listener.onBrowseFail(defaultMsg);
				}
			};
			m_controlPoint.execute(actionCallback);
		}
	}

	@Override
	public void back(DMSProcessorListner listener) {
		m_currentPageIndex = 0;
		int traceSize = m_traceID.size();
		if (traceSize > 2) {
			String parentID = m_traceID.get(traceSize - 2);
			browse(parentID, 0, listener);
			m_traceID.remove(m_traceID.size() - 1);
			m_traceID.remove(m_traceID.size() - 1);
		} else {
		}
	}

	@Override
	public void nextPage(DMSProcessorListner listener) {
		executeBrowse(m_traceID.get(m_traceID.size() - 1), m_currentPageIndex + 1, listener);
	}

	@Override
	public void previousPage(DMSProcessorListner listener) {
		if (m_currentPageIndex > 0)
			executeBrowse(m_traceID.get(m_traceID.size() - 1), m_currentPageIndex - 1, listener);
	}

	@Override
	public DIDLObject getDIDLObject(String objectID) {
		for (DIDLObject object : m_DIDLObjectList) {
			if (object.getId().equals(objectID))
				return object;
		}
		return null;
	}

	@Override
	public void removeCurrentItemsFromPlaylist(PlaylistProcessor playlistProcessor,
			DMSAddRemoveContainerListener actionListener) {
		modifyCurrentItems(playlistProcessor, actionListener, ACTION_REMOVE);
	}

	@Override
	public void addCurrentItemsToPlaylist(PlaylistProcessor playlistProcessor,
			DMSAddRemoveContainerListener actionListener) {
		modifyCurrentItems(playlistProcessor, actionListener, ACTION_ADD);
	}

	private void modifyCurrentItems(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener,
			String actionType) {
		actionListener.onActionStart();
		if (playlistProcessor == null) {
			Log.w(TAG, "Playlist processor = null");
			actionListener.onActionFail(new RuntimeException("Playlist is null"));
			return;
		}
		int count = m_DIDLObjectList.size();
		for (int i = 0; i < count; ++i) {
			addItemToPlaylist(playlistProcessor, actionType, m_DIDLObjectList.get(i));
		}
		actionListener.onActionComplete();
	}

	@Override
	public List<DIDLObject> getAllObjects() {
		return m_DIDLObjectList;
	}

	@Override
	public void addAllToPlaylist(final PlaylistProcessor playlistProcessor,
			final DMSAddRemoveContainerListener actionListener) {
		modifyContainerItemsInPlaylist(playlistProcessor, actionListener, ACTION_ADD);
	}

	@Override
	public void removeAllFromPlaylist(PlaylistProcessor playlistProcessor, DMSAddRemoveContainerListener actionListener) {
		modifyContainerItemsInPlaylist(playlistProcessor, actionListener, ACTION_REMOVE);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void modifyContainerItemsInPlaylist(final PlaylistProcessor playlistProcessor,
			final DMSAddRemoveContainerListener actionListener, final String actionType) {
		actionListener.onActionStart();
		Service cds = m_server.findService(new ServiceType("schemas-upnp-org", "ContentDirectory"));
		if (cds != null) {
			Action action = cds.getAction("Browse");
			ActionInvocation actionInvocation = new ActionInvocation(action);
			actionInvocation.setInput("ObjectID", m_currentObjectId);
			actionInvocation.setInput("BrowseFlag", "BrowseDirectChildren");
			actionInvocation.setInput("Filter", "*");
			actionInvocation.setInput("StartingIndex", new UnsignedIntegerFourBytes(0));
			actionInvocation.setInput("RequestedCount", new UnsignedIntegerFourBytes(0));
			actionInvocation.setInput("SortCriteria", null);
			ActionCallback actionCallback = new ActionCallback(actionInvocation) {

				@Override
				public void success(ActionInvocation invocation) {
					try {
						DIDLParser parser = new DIDLParser();
						DIDLContent content = parser.parse(invocation.getOutput("Result").toString());
						Log.i(TAG, "m_currentObjectId = " + m_currentObjectId + "Container = "
								+ content.getContainers().size() + " Item = " + content.getItems().size());
						List<Item> items = content.getItems();
						if (playlistProcessor == null) {
							actionListener.onActionFail(new RuntimeException("Playlist processor is null"));
						} else {
							int count = items.size();
							for (int i = 0; i < count; ++i) {
								DIDLObject object = items.get(i);
								if (object instanceof Item) {
									addItemToPlaylist(playlistProcessor, actionType, object);
								}
							}
							actionListener.onActionComplete();
						}

					} catch (Exception e) {
						e.printStackTrace();
						actionListener.onActionFail(e);
					}
				}

				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					Log.e(TAG, defaultMsg);
					actionListener.onActionFail(new RuntimeException(defaultMsg));
				}
			};
			m_controlPoint.execute(actionCallback);
		}
	}

	private void addItemToPlaylist(final PlaylistProcessor playlistProcessor, final String actionType, DIDLObject object) {
		if (actionType.equals(ACTION_ADD))
			playlistProcessor.addDIDLObject(object);
		else if (actionType.equals(ACTION_REMOVE))
			playlistProcessor.removeDIDLObject(object);
	}

}
