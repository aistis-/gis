package org.geotools.gis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;

import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.BasicPolygonStyle;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.data.JFileDataStoreChooser;

public class GUI {

	static MapContent map = new MapContent();
	static JMapFrame show = new JMapFrame(map);
	static QueryLab queries = new QueryLab();
	
	public void create() {
		
		JButton btnAddLayer = new JButton("Add Layer");
	    btnAddLayer.addActionListener(
	    new ActionListener() {
	        public void actionPerformed(ActionEvent e) {
        		try {
					addLayer();
				} catch (Exception e1) { System.out.println("Can't open file"); }
            }
	    });
	    
	    JButton btnData = new JButton("Data tables");
	    btnData.addActionListener(
	    	    new ActionListener() {
	    	        public void actionPerformed(ActionEvent e) {
	            		data();
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
	
	private void addLayer() throws Exception {
	    File file = JFileDataStoreChooser.showOpenFile("shp, tif, rrd", null);
	    if (file == null) {
	        return;
	    }
	
	    FileDataStore store = FileDataStoreFinder.getDataStore(file);
	    FeatureSource featureSource = store.getFeatureSource();
	    
	    Style style = new BasicPolygonStyle();
	
	    Layer layer = new FeatureLayer(featureSource, style);
	    map.addLayer(layer);
	}
	
	private void data() {
		queries.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		queries.setVisible(true);
	}
}
