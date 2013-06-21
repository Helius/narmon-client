package com.ghelius.narodmon;

import java.io.Serializable;
import java.util.ArrayList;

public class MultiGraph implements Serializable {
	String name;
	ArrayList<Integer> ids = new ArrayList<Integer>();

	MultiGraph (String name, ArrayList<Integer> ids) {
		this.name = name;
		this.ids.addAll(ids);
	}
}
