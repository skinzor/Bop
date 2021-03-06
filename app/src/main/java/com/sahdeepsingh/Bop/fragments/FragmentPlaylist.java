package com.sahdeepsingh.Bop.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.sahdeepsingh.Bop.Adapters.PlaylistRecyclerViewAdapter;
import com.sahdeepsingh.Bop.R;
import com.sahdeepsingh.Bop.playerMain.Main;
import com.sahdeepsingh.Bop.utils.RVUtils;

import java.util.ArrayList;
import java.util.Collections;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class FragmentPlaylist extends Fragment {

    PlaylistRecyclerViewAdapter mfilteredAdapter;
    LinearLayout noData;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public FragmentPlaylist() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        noData = view.findViewById(R.id.noData);
        // Set the adapter
            Context context = view.getContext();
        RecyclerView recyclerView = view.findViewById(R.id.list);
            recyclerView.setLayoutManager(new GridLayoutManager(context, 1));
            ArrayList<String> playlists = Main.songs.getPlaylistNames();
            PlaylistRecyclerViewAdapter playlistRecyclerViewAdapter = new PlaylistRecyclerViewAdapter(playlists);
            recyclerView.setAdapter(playlistRecyclerViewAdapter);
        RVUtils.makenoDataVisible(recyclerView, noData);
        ArrayList<String> filtered = new ArrayList<>(playlists);
        EditText search = view.findViewById(R.id.searchPlaylist);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                filtered.clear();
                charSequence = charSequence.toString().toLowerCase();
                if (charSequence.length() == 0) {
                    filtered.addAll(playlists);
                } else
                    for (int j = 0; j < playlists.size(); j++) {
                        String playlist = playlists.get(j);
                        if (playlist.toLowerCase().contains(charSequence.toString().toLowerCase())) {
                            filtered.add(playlists.get(j));
                        }
                    }
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                Collections.sort(filtered, String::compareToIgnoreCase);
                mfilteredAdapter = new PlaylistRecyclerViewAdapter(filtered);
                recyclerView.setAdapter(mfilteredAdapter);
                mfilteredAdapter.notifyDataSetChanged();
                RVUtils.makenoDataVisible(recyclerView, noData);

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });


        return view;
    }

}
