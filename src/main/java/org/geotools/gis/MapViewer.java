package org.geotools.gis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.BasicPolygonStyle;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;

public class MapViewer {

	public MapContent map;
	public JMapFrame show;
	
	public MapViewer() {
		
		map = new MapContent();
		show = new JMapFrame(map);
			
		JButton btnAddLayer = new JButton("Add Layer");
	    btnAddLayer.addActionListener(
	    new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
        		try {
        			addLayerFromFile();
				} catch (Exception e1) { System.out.println("Can't open file"); }
            }
	    });
	    
	    JButton btnData = new JButton("Data tables");
	    btnData.addActionListener(
	    	    new ActionListener() {
	    	        public void actionPerformed(ActionEvent e) {
	    	        	App.dataTables.setVisible(true);
	                }
	    	    });
	    
	    show.enableStatusBar(true);
	    show.enableToolBar(true);
	    show.enableLayerTable(true);
	    show.setTitle("GIS");
	    show.setExtendedState(JMapFrame.MAXIMIZED_BOTH); // maximize the window
	    
	    show.getToolBar().addSeparator();
	    show.getToolBar().add(btnAddLayer);
	    show.getToolBar().add(btnData);
	    
	    show.enableInputMethods(true);
	    
	    show.setVisible(true);
	}
	
	private void addLayerFromFile() throws Exception {
	    File file = JFileDataStoreChooser.showOpenFile("shp, tif, rrd", null);
	    if (file == null) {
	        return;
	    }
	
	    FileDataStore store = FileDataStoreFinder.getDataStore(file);
	    SimpleFeatureSource source = store.getFeatureSource();
	    
	    App.dataController.mapData.put(source.getName().toString(), source);
	    addMapLayer(source);
	}
	
	public void addMapLayer(SimpleFeatureSource source) {
	    
	    Style style = new BasicPolygonStyle();
	
	    Layer layer = new FeatureLayer(source, style);
	    
	    map.addLayer(layer);
	}
}