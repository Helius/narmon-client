package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;

import java.io.*;
import java.util.ArrayList;

public class MultiGraphHolder {
	static private MultiGraphHolder holder = null;
	private ArrayList<MultiGraph> graphs;
	private static final String fileName = "graphs.cfg";
	private static final String TAG = "narodmon-graphHolder";
	private Context context;

	private MultiGraphHolder (Context context) {
		this.context = context;
		try {
			FileInputStream fis = context.openFileInput(fileName);
			ObjectInputStream is = new ObjectInputStream(fis);
			graphs = (ArrayList<MultiGraph>) is.readObject();
			is.close();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
			e.getMessage();
			// file was not found, class not found, etc, first start?
			Log.w(TAG, "config file not found, create new configuration");
			graphs = new ArrayList<MultiGraph>();
		}
	}

	static public MultiGraphHolder getInstance (Context context) {
		if (holder == null)
			holder = new MultiGraphHolder(context);
		return holder;
	}

	public void save () {
		try {
			FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
			ObjectOutputStream os;
			try {
				os = new ObjectOutputStream(fos);
			} catch (IOException e) {
				e.printStackTrace();
				e.getMessage();
				Log.e(TAG,"can't create objectOutputStream");
				return;
			}
			try {
				os.writeObject(graphs);
			} catch (IOException e) {
				e.printStackTrace();
				e.getMessage();
				Log.e(TAG,"can't create write object");
			}
			try {
				os.close();
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
				e.getMessage();
				Log.e(TAG,"can't close file and stream");
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG,"Can't open config file");
		}
	}

	public ArrayList<MultiGraph> getGraphs () {
		return graphs;
	}

	public void add(MultiGraph multiGraph) {
		graphs.add(multiGraph);
		save();
	}

	public void delete(int pos) {
		graphs.remove(pos);
	}
}
