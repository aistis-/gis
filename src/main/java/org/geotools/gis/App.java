package org.geotools.gis;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.UIManager;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.DefaultMapContext;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.MapContext;
import org.geotools.map.MapLayer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapFrame.Tool;
import org.geotools.swing.data.JFileDataStoreChooser;

public class App {

	 static MapContent map = new MapContent();
	 static JMapFrame show = new JMapFrame(map);
	 
    /**
     * GeoTools Quickstart demo application. Prompts the user for a shapefile and displays its
     * contents on the screen in a map frame
     */
    public void main(String[] args) throws Exception {
    	
    	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
       
        
        JButton btnAddLayer = new JButton("Add Layer");
        btnAddLayer.addActionListener(
        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	File file = JFileDataStoreChooser.showOpenFile("shp", null);
                if (file == null) {
                    return;
                } else {
                	FileDataStore store = null;
					try {
						store = FileDataStoreFinder.getDataStore(file);
					} catch (IOException e1) { System.out.println("Can't oper the file"); }
					
                    try {
						SimpleFeatureSource featureSource = store.getFeatureSource();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    
                    MapLayer layer = new MapLayer();
                    
                    map.addLayer(featureSource);
                }
            }
        });
        
        JButton btnAddFilter = new JButton("Add Filter");

        
        show.enableStatusBar(true);
        show.enableToolBar(true);
        show.enableLayerTable(true);
        show.setTitle("GIS");
        show.setExtendedState(JMapFrame.MAXIMIZED_BOTH); // maximize the window
        
        show.getToolBar().add(btnAddLayer);
        show.getToolBar().add(btnAddFilter);
        
        show.enableInputMethods(true);
        
        show.setVisible(true);
    }
}