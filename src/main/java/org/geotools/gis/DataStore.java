package org.geotools.gis;

import java.io.IOException;
import java.util.ArrayList;

import org.geotools.data.FileDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.Layer;

public class DataStore {

	private ArrayList<FileDataStore> storeList = new ArrayList<FileDataStore>();
	
	public SimpleFeatureSource getFeatureSourceByName(String name) throws IOException {
		for (int i = 0; i < storeList.size(); i++) {
			if (name == storeList.get(i).getFeatureSource().getName().toString()) {
				return storeList.get(i).getFeatureSource();
			}
		}
		
		return null;
	}
	
	public void addStore(FileDataStore store) throws IOException {
		this.storeList.add(store);
		
		App.dataTables.featureTypeCBox.removeAllItems();
		
		for (int i = 0; i < storeList.size(); i++) {
			App.dataTables.featureTypeCBox.addItem(storeList.get(i).getFeatureSource().getName().toString());
		}
	}
}
