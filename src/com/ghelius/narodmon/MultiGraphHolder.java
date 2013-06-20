package com.ghelius.narodmon;

import android.content.Context;

import java.util.ArrayList;

public class MultiGraphHolder {
	static private MultiGraphHolder holder = null;
	private ArrayList<MultiGraph> graphs;
	private Context context;

	private MultiGraphHolder (Context context) {
		// TODO: load from file
		graphs = new ArrayList<MultiGraph>();
	}

	static public MultiGraphHolder getInstance (Context context) {
		if (holder == null)
			holder = new MultiGraphHolder(context);
		return holder;
	}

	public void save () {
		// TODO: write to file
	}

	public ArrayList<MultiGraph> getGraphs () {
		return graphs;
	}
}
