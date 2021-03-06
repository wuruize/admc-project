package com.app.dlna.dmc.processor.impl;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;

public class PlaylistProcessorImpl implements PlaylistProcessor {
	private static final String TAG = PlaylistProcessorImpl.class.getName();
	private List<PlaylistItem> m_playlistItems;
	private int m_currentItemIdx;
	private int m_maxSize = 100;
	private List<String> m_listURI;

	public PlaylistProcessorImpl(int maxSize) {
		m_playlistItems = new ArrayList<PlaylistItem>();
		m_currentItemIdx = -1;
		m_maxSize = maxSize;
		m_listURI = new ArrayList<String>();
	}

	@Override
	public int getMaxSize() {
		return m_maxSize;
	}

	@Override
	public boolean isFull() {
		return m_playlistItems.size() >= m_maxSize ? true : false;
	}

	@Override
	public void next() {
		if (m_playlistItems.size() == 0)
			return;
		m_currentItemIdx = (m_currentItemIdx + 1) % m_playlistItems.size();
		if (m_currentItemIdx >= m_playlistItems.size()) {
			m_currentItemIdx = 0;
		}
	}

	@Override
	public void previous() {
		if (m_playlistItems.size() == 0)
			return;
		m_currentItemIdx = (m_currentItemIdx - 1) % m_playlistItems.size();
		if (m_currentItemIdx < 0) {
			m_currentItemIdx = m_playlistItems.size() - 1;
		}
	}

	@Override
	public PlaylistItem getCurrentItem() {
		if (m_playlistItems.size() > 0 && m_currentItemIdx < m_playlistItems.size()) {
			return m_playlistItems.get(m_currentItemIdx);
		}
		return null;
	}

	@Override
	public int setCurrentItem(int idx) {
		if (0 <= idx && idx < m_playlistItems.size()) {
			m_currentItemIdx = idx;
			return m_currentItemIdx;
		}
		return -1;
	}

	@Override
	public int setCurrentItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item)) {
				m_currentItemIdx = m_playlistItems.indexOf(item);
				return m_currentItemIdx;
			}
			return -1;
		}
	}

	@Override
	public boolean addItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item) || m_playlistItems.size() >= m_maxSize)
				return false;
			m_playlistItems.add(item);
			m_listURI.add(item.getUri());
			Log.i(TAG, "Added Item, playlist size: " + m_playlistItems.size());
			if (m_playlistItems.size() == 1) {
				m_currentItemIdx = 0;
			}
			return true;
		}
	}

	@Override
	public boolean removeItem(PlaylistItem item) {
		synchronized (m_playlistItems) {
			if (m_playlistItems.contains(item)) {
				m_playlistItems.remove(item);
				m_listURI.remove(item.getUri());
				Log.i(TAG, "Removed Item, playlist size: " + m_playlistItems.size());
				return true;
			}
			return false;
		}
	}

	@Override
	public List<PlaylistItem> getAllItems() {
		return m_playlistItems;
	}

	@Override
	public boolean containsUrl(String url) {
		return m_listURI.contains(url);
	}

}
