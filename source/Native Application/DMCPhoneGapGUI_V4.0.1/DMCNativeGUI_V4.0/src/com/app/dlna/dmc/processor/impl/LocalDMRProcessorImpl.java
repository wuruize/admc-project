package com.app.dlna.dmc.processor.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.SurfaceHolder;

import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistItem.Type;

public class LocalDMRProcessorImpl implements DMRProcessor {
	private static final int SLEEP_INTERVAL = 1000;
	private List<DMRProcessorListner> m_listeners;
	private MediaPlayer m_player;
	private PlaylistItem m_currentItem;
	private PlaylistProcessor m_playlistProcessor;
	private AudioManager m_audioManager;
	private int m_maxVolume;
	private boolean m_selfAutoNext;
	private boolean m_isRunning;
	private static final int STATE_PLAYING = 0;
	private static final int STATE_STOPED = 1;
	private static final int STATE_PAUSED = 2;
	protected static final String TAG = LocalDMRProcessorImpl.class.getName();
	private int m_currentState;
	private SurfaceHolder m_holder;

	private class UpdateThread extends Thread {
		@Override
		public void run() {
			while (m_isRunning) {
				if (m_player.isPlaying()) {
					int currentPosition = m_player.getCurrentPosition() / 1000;
					fireUpdatePositionEvent(currentPosition, m_player.getDuration() / 1000);
					m_currentState = STATE_PLAYING;
				}
				switch (m_currentState) {
				case STATE_PLAYING:
					fireOnPlayingEvent();
					break;
				case STATE_PAUSED:
					fireOnPausedEvent();
					break;
				case STATE_STOPED:
					fireOnStopedEvent();
					break;
				}
				updateHolder();
				try {
					Thread.sleep(SLEEP_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public LocalDMRProcessorImpl(Context context) {
		m_listeners = new ArrayList<DMRProcessor.DMRProcessorListner>();
		m_currentItem = new PlaylistItem();
		m_player = new MediaPlayer();
		m_player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		m_player.setOnPreparedListener(m_preparedListener);
		m_player.setOnInfoListener(m_infoListener);
		m_player.setOnCompletionListener(m_completeListener);
		m_player.setOnErrorListener(m_onErrorListener);
		m_player.setScreenOnWhilePlaying(true);
		m_audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		m_maxVolume = m_audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		m_isRunning = true;
		m_selfAutoNext = true;
		new UpdateThread().start();
	}

	private OnPreparedListener m_preparedListener = new OnPreparedListener() {

		@Override
		public void onPrepared(MediaPlayer mp) {
			int width = mp.getVideoWidth();
			int height = mp.getVideoHeight();

			if (width != 0 && height != 0 && m_holder != null && m_holder.getSurface().isValid()) {
				m_holder.setFixedSize(width, height);
			}
			mp.start();
			m_currentState = STATE_PLAYING;
		}

	};

	private OnInfoListener m_infoListener = new OnInfoListener() {

		@Override
		public boolean onInfo(MediaPlayer mp, int what, int extra) {
			return false;
		}
	};

	private OnCompletionListener m_completeListener = new OnCompletionListener() {

		@Override
		public void onCompletion(MediaPlayer mp) {
			mp.reset();
			fireOnStopedEvent();
			if (m_playlistProcessor != null && m_selfAutoNext)
				m_playlistProcessor.next();
		}
	};

	private OnErrorListener m_onErrorListener = new OnErrorListener() {

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.i(TAG, "On error");
			mp.reset();
			fireOnStopedEvent();
			if (m_playlistProcessor != null && m_selfAutoNext)
				m_playlistProcessor.next();
			return true;
		}
	};

	@Override
	public void play() {
		try {
			m_player.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void pause() {
		try {
			m_player.pause();
			m_currentState = STATE_PAUSED;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void stop() {
		try {
			m_player.seekTo(0);
			m_player.pause();
			m_currentState = STATE_STOPED;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void seek(String position) {
		try {
			String[] elements = position.split(":");
			long miliSec = new Integer(elements[0]) * 3600 + new Integer(elements[1]) * 60 + new Integer(elements[2]);
			miliSec *= 1000;
			m_player.seekTo((int) miliSec);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void setVolume(int newVolume) {
		try {
			m_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_VIBRATE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public int getVolume() {
		return m_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	@Override
	public int getMaxVolume() {
		return m_maxVolume;
	}

	@Override
	public void addListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			if (!m_listeners.contains(listener))
				m_listeners.add(listener);
		}
	}

	@Override
	public void removeListener(DMRProcessorListner listener) {
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}

	}

	@Override
	public void dispose() {
		m_listeners.clear();
		m_player.release();
		m_player = null;
		m_isRunning = false;
	}

	@Override
	public String getName() {
		return "Local Player";
	}

	@Override
	public void setPlaylistProcessor(PlaylistProcessor playlistProcessor) {
		m_playlistProcessor = playlistProcessor;
	}

	@Override
	public void setSeftAutoNext(boolean autoNext) {
		m_selfAutoNext = autoNext;
	}

	@Override
	public String getCurrentTrackURI() {
		return m_currentItem != null ? m_currentItem.getUrl() : "";
	}

	@Override
	public void setRunning(boolean running) {
		m_isRunning = running;
		if (running)
			new UpdateThread().start();
	}

	private void fireUpdatePositionEvent(long current, long max) {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onUpdatePosition(current, max);
			}
		}
	}

	private void fireOnStopedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onStoped();
			}
		}
	}

	private void fireOnPausedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPaused();
			}
		}
	}

	private void fireOnPlayingEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListner listener : m_listeners) {
				listener.onPlaying();
			}
		}
	}

	@Override
	public void setURIandPlay(PlaylistItem item) {
		String url = item.getUrl();
		if (getCurrentTrackURI().equals(url))
			return;
		m_currentItem = item;
		switch (item.getType()) {
		case AUDIO: {
			m_player.stop();
			m_player.reset();
			m_player.setDisplay(null);
			try {
				m_player.setDataSource(url);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			m_player.prepareAsync();
			break;
		}
		case VIDEO: {
			m_player.stop();
			m_player.reset();
			if (m_holder != null && m_holder.getSurface().isValid()) {
				try {
					m_player.setDisplay(m_holder);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			try {
				m_player.setDataSource(url);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			m_player.prepareAsync();
			break;
		}
		case IMAGE: {
			stop();
			m_player.setDisplay(null);
			break;
		}
		default:
			break;
		}

	}

	public void setSurfaceHolder(SurfaceHolder holder) {
		m_holder = holder;
		updateHolder();
	}

	private void updateHolder() {
		if (m_holder != null && m_currentItem.getType().equals(Type.VIDEO) && m_holder.getSurface().isValid())
			try {
				m_player.setDisplay(m_holder);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
	}
}
