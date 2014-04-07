package org.geotools.gis;

import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureSource;

public class DataController {

	public Map<String, SimpleFeatureSource> mapData = null;
	
	public DataController() {
		mapData = new HashMap<String, SimpleFeatureSource>();
	}
}
