package com.app.dlna.dmc.gui.customview.playlist;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;
import app.dlna.controller.v4.R;

import com.app.dlna.dmc.gui.MainActivity;
import com.app.dlna.dmc.gui.customview.adapter.AdapterItem;
import com.app.dlna.dmc.gui.customview.adapter.CustomArrayAdapter;
import com.app.dlna.dmc.gui.customview.listener.DMRListenerView;
import com.app.dlna.dmc.processor.async.AsyncTaskWithProgressDialog;
import com.app.dlna.dmc.processor.interfaces.DMRProcessor;
import com.app.dlna.dmc.processor.interfaces.PlaylistProcessor;
import com.app.dlna.dmc.processor.playlist.Playlist;
import com.app.dlna.dmc.processor.playlist.PlaylistItem;
import com.app.dlna.dmc.processor.playlist.PlaylistManager;

public class PlaylistView extends DMRListenerView {

	private PlaylistToolbar m_playlistToolbar;
	public static final int VM_LIST = 0;
	public static final int VM_DETAILS = 1;
	private int m_viewMode = -1;

	public PlaylistView(Context context) {
		super(context);
		((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
				R.layout.cv_playlist_allitem, this);
		m_listView = (ListView) findViewById(R.id.lv_playlist);
		m_adapter = new CustomArrayAdapter(getContext(), 0);
		m_listView.setAdapter(m_adapter);
		m_listView.setOnItemClickListener(m_playlistItemClick);

		m_playlistToolbar = (PlaylistToolbar) findViewById(R.id.botToolbar);
		m_playlistToolbar.setPlaylistView(this);
		super.updateListView();
		m_viewMode = VM_LIST;
		preparePlaylist();
	}

	public void preparePlaylist() {
		switch (m_viewMode) {
		case VM_DETAILS:
			if (m_adapter.getCount() > 0) {
				if (!(m_adapter.getItem(0).getData() instanceof PlaylistItem))
					m_adapter.clear();
			}
			for (PlaylistItem item : MainActivity.UPNP_PROCESSOR.getPlaylistProcessor().getAllItems()) {
				if (m_adapter.getPosition(new AdapterItem(item)) < 0)
					m_adapter.add(new AdapterItem(item));
			}
			m_playlistToolbar.setVisibility(View.VISIBLE);
			m_playlistToolbar.updateToolbar(m_viewMode);
			break;
		case VM_LIST:
			new AsyncTaskWithProgressDialog<Void, Void, List<Playlist>>("Loading All Playlist") {

				@Override
				protected void onPreExecute() {
				}

				@Override
				protected List<Playlist> doInBackground(Void... params) {
					return PlaylistManager.getAllPlaylist();
				}

				@Override
				protected void onPostExecute(List<Playlist> result) {
					if (m_adapter.getCount() > 0) {
						if (!(m_adapter.getItem(0).getData() instanceof Playlist))
							m_adapter.clear();
					}
					for (Playlist playlist : result)
						if (m_adapter.getPosition(new AdapterItem(playlist)) < 0)
							m_adapter.add(new AdapterItem(playlist));
					m_playlistToolbar.setVisibility(View.GONE);
				}
			}.execute(new Void[] {});
			break;
		default:
			break;
		}

	}

	private OnItemClickListener m_playlistItemClick = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			final Object object = m_adapter.getItem(position).getData();
			if (object instanceof Playlist) {

				new AsyncTaskWithProgressDialog<Void, Void, PlaylistProcessor>("Loading Playlist Items") {

					@Override
					protected PlaylistProcessor doInBackground(Void... params) {
						return PlaylistManager.getPlaylistProcessor((Playlist) object);
					}

					protected void onPostExecute(PlaylistProcessor playlistProcessor) {
						MainActivity.UPNP_PROCESSOR.setPlaylistProcessor(playlistProcessor);
						DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
						if (dmrProcessor != null) {
							dmrProcessor.setPlaylistProcessor(playlistProcessor);
							dmrProcessor.setSeftAutoNext(true);
						}
						m_viewMode = VM_DETAILS;
						preparePlaylist();
						super.onPostExecute(playlistProcessor);
					};
				}.execute(new Void[] {});
			} else if (object instanceof PlaylistItem) {
				PlaylistProcessor playlistProcessor = MainActivity.UPNP_PROCESSOR.getPlaylistProcessor();
				MainActivity.UPNP_PROCESSOR.getDMRProcessor().setPlaylistProcessor(playlistProcessor);
				MainActivity.UPNP_PROCESSOR.getDMRProcessor().setSeftAutoNext(true);
				if (playlistProcessor == null) {
					Toast.makeText(getContext(), "Cannot get playlist", Toast.LENGTH_SHORT).show();
					return;
				}

				DMRProcessor dmrProcessor = MainActivity.UPNP_PROCESSOR.getDMRProcessor();
				if (dmrProcessor == null) {
					Toast.makeText(getContext(), "Cannot connect to renderer", Toast.LENGTH_SHORT).show();
					return;
				}

				playlistProcessor.setCurrentItem((PlaylistItem) object);
				dmrProcessor.setURIandPlay(playlistProcessor.getCurrentItem().getUrl());
				// TODO: high light current item
			}

		}
	};

	public void backToListPlaylist() {
		m_viewMode = VM_LIST;
		preparePlaylist();
	}

	@Override
	public void updateListView() {
		super.updateListView();
		m_viewMode = VM_DETAILS;
		preparePlaylist();
	}
}
