package org.geotools.gis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.Layer;

public class DataController {

	public Map<String, SimpleFeatureSource> mapData = null;
	
	public DataController() {
		mapData = new HashMap<String, SimpleFeatureSource>();
	}
	
	public Layer getLayerByName(String name) {
		List<Layer> layers = App.mapWindow.map.layers();
		
		for (int i = 0; i < layers.size(); i++) {
			if (name.equals(layers.get(i).getFeatureSource().getName().toString())) {
				return layers.get(i);
			}
		}
		
		return null;
	}
}
