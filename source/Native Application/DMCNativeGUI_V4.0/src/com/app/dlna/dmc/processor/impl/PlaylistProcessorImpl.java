package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import org.teleal.cling.support.model.DIDLObject;

import com.app.dlna.dmc.gui.activity.AppPreference;
import com.app.dlna.dmc.gui.activity.MainActivity;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;
import com.app.dlna.dmc.processor.youtube.YoutubeItem;

public class PlaylistProcessorImpl implements PlaylistProcessor {
	private List<PlaylistItem> m_playlistItems;
	private int m_currentItemIdx;
	private int m_maxSize;
	private Playlist m_data;
	private List<PlaylistListener> m_listeners;

	public PlaylistProcessorImpl(Playlist data, int maxItem) {
		m_playlistItems = new ArrayList<PlaylistItem>();
		m_currentItemIdx = data.getCurrentIdx();
		m_maxSize = maxItem;
		m_data = data;
		m_listeners = new ArrayList<PlaylistProcessor.PlaylistListener>();
	}

	@Override
	public Playlist getData() {
		m_data.setCurrentIdx(m_currentItemIdx);
		return m_data;
	}

	@Override
	public void setData(Playlist data) {
		m_data = data;
	}

	@Override
	public int getMaxSize() {
		return m_maxSize;
	}

	@Override
	public boolean isFull() {
		return m_playlistItems.size() >= m_maxSize;
	}

	@Override
	public void next() {
		List<PlaylistItem> playlistItems = getAllItemsByViewMode();
		if (playlistItems.size() == 0)
			return;
		int currentIdx = playlistItems.indexOf(m_playlistItems.get(m_currentItemIdx));
		++currentIdx;
		if (currentIdx >= playlistItems.size())
			currentIdx = 0;
		m_currentItemIdx = m_playlistItems.indexOf(playlistItems.get(currentIdx));
		// m_currentItemIdx = (m_currentItemIdx + 1) % playlistItems.size();
		// if (m_currentItemIdx < 0 || m_currentItemIdx >= playlistItems.size())
		// {
		// m_currentItemIdx = 0;
		// }
		fireOnNextEvent();
	}

	private void fireOnNextEvent() {
		synchronized (m_listeners) {
			for (PlaylistListener listener : m_listeners) {
				listener.onNext();
			}
		}
	}

	@Override
	public void previous() {
		// List<PlaylistItem> playlistItems = getAllItemsByViewMode();
		// if (playlistItems.size() == 0)
		// return;
		// m_currentItemIdx = (m_currentItemIdx - 1) % playlistItems.size();
		// if (m_currentItemIdx < 0) {
		// m_currentItemIdx = playlistItems.size() - 1;
		// } else if (m_currentItemIdx >= playlistItems.size())
		// m_currentItemIdx = 0;
		List<PlaylistItem> playlistItems = getAllItemsByViewMode();
		if (playlistItems.size() == 0)
			return;
		int currentIdx = playlistItems.indexOf(m_playlistItems.get(m_currentItemIdx));
		--currentIdx;
		if (currentIdx < 0)
			currentIdx = playlistItems.size() - 1;
		m_currentItemIdx = m_playlistItems.indexOf(playlistItems.get(currentIdx));
		fireOnPrevEvent();
	}

	private void fireOnPrevEvent() {
		synchronized (m_listeners) {
			for (PlaylistListener listener : m_listeners) {
				listener.onPrev();
			}
		}
	}

	@Override
	public PlaylistItem getCurrentItem() {
		if (m_currentItemIdx == -1) {
			return null;
		}
		
		if (m_playlistItems.size() > 0 && m_currentItemIdx < m_playlistItems.size()) {
			return m_playlistItems.get(m_currentItemIdx);
		}
		return null;
	}

	@Override
	public int setCurrentItem(int idx) {
		if (0 <= idx && idx < m_playlistItems.size())
			return m_currentItemIdx = idx;
		return -1;
	}

	@Override
	public int setCurrentItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			return m_currentItemIdx = m_playlistItems.indexOf(item);
		}
	}

	@Override
	public PlaylistItem addItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item))
				return item;
			if (m_playlistItems.size() >= m_maxSize) {
				// // remove last item
				// PlaylistItem lastItem =
				// m_playlistItems.get(m_playlistItems.size() - 1);
				// PlaylistManager.deletePlaylistItem(lastItem.getId());
				// m_playlistItems.remove(lastItem);
				return null;
			}
			if (!PlaylistManager.createPlaylistItem(item, m_data.getId()))
				return null;
			m_playlistItems.add(item);
			if (m_playlistItems.size() == 1) {
				m_currentItemIdx = 0;
			}
			return item;
		}
	}

	@Override
	public PlaylistItem removeItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			int itemIdx = -1;
			if ((itemIdx = m_playlistItems.indexOf(item)) >= 0) {
				long id = m_playlistItems.get(itemIdx).getId();
				PlaylistManager.deletePlaylistItem(id);
				m_playlistItems.remove(item);
				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor != null && dmrProcessor.getCurrentTrackURI().equals(item.getUrl())) {
					dmrProcessor.stop();
				}
				if (itemIdx == m_currentItemIdx)
					m_currentItemIdx = 0;
				return item;
			}
			return null;
		}
	}

	@Override
	public List<PlaylistItem> getAllItems() {
		return m_playlistItems;
	}

	@Override
	public boolean containsUrl(String url) {
		List<String> listUrl = new ArrayList<String>();
		for (PlaylistItem item : m_playlistItems) {
			listUrl.add(item.getUrl());
		}
		return listUrl.contains(url);
	}

	@Override
	public PlaylistItem addDIDLObject(DIDLObject object) {
		return addItem(PlaylistItem.createFromDLDIObject(object));
	}

	@Override
	public PlaylistItem removeDIDLObject(DIDLObject object) {
		return removeItem(PlaylistItem.createFromDLDIObject(object));
	}

	@Override
	public PlaylistItem getItemAt(int idx) {
		return m_playlistItems.get(idx);
	}

	@Override
	public void addListener(PlaylistListener listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
		}
	}

	@Override
	public void removeListener(PlaylistListener listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
		}
	}

	@Override
	public void saveState() {
		PlaylistManager.savePlaylistState(getData());
	}

	@Override
	public int getCurrentItemIndex() {
		return m_currentItemIdx;
	}

	@Override
	public PlaylistItem addYoutubeItem(YoutubeItem result) {
		return addItem(createPlaylistItem(result));
	}

	private PlaylistItem createPlaylistItem(YoutubeItem object) {
		PlaylistItem item = new PlaylistItem();
		item.setTitle(object.getTitle());
		item.setUrl(object.getId());
		item.setType(Type.YOUTUBE);
		return item;
	}

	@Override
	public void updateItemList() {
		synchronized (m_playlistItems) {
			m_playlistItems = PlaylistManager.getAllPlaylistItem(getData().getId());
		}
	}

	@Override
	public List<PlaylistItem> getAllItemsByViewMode() {
		List<PlaylistItem> result = new ArrayList<PlaylistItem>();
		switch (AppPreference.getPlaylistViewMode()) {
		case ALL:
			return m_playlistItems;
		case AUDIO_ONLY:
			for (PlaylistItem item : m_playlistItems) {
				if (item.getType().equals(Type.AUDIO_LOCAL) || item.getType().equals(Type.AUDIO_REMOTE))
					result.add(item);
			}
			break;
		case IMAGE_ONLY:
			for (PlaylistItem item : m_playlistItems) {
				if (item.getType().equals(Type.IMAGE_LOCAL) || item.getType().equals(Type.IMAGE_REMOTE))
					result.add(item);
			}
			break;
		case VIDEO_ONLY:
			for (PlaylistItem item : m_playlistItems) {
				if (item.getType().equals(Type.VIDEO_LOCAL) || item.getType().equals(Type.VIDEO_REMOTE)
						|| item.getType().equals(Type.YOUTUBE))
					result.add(item);
			}
			break;
		}
		return result;
	}

	@Override
	public void updateForViewMode() {
		List<PlaylistItem> playlistItems = getAllItemsByViewMode();
		m_currentItemIdx = playlistItems.size() > 0 ? m_playlistItems.indexOf(playlistItems.get(0)) : -1;
	}

}