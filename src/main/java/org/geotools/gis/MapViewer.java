package org.geotools.gis;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
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
import org.opengis.geometry.Envelope;

public class MapViewer {

	public MapContent map;
	public JMapFrame show;
	
	private SelectionTool selectionTool = new SelectionTool();
	
	public MapViewer() {
		
		map = new MapContent();
		show = new JMapFrame(map);
	    
		JButton btnAddLayer = new JButton("");
	    btnAddLayer.setToolTipText("Add new layer");
	    btnAddLayer.setIcon(new ImageIcon(MapViewer.class.getResource("/icons/import.png")));
	    btnAddLayer.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				try {
        			addLayerFromFile();
				} catch (Exception e1) { e1.printStackTrace() ;System.out.println("Can't open file"); }
            }
	    });
	    
	    JButton btnData = new JButton("");
	    btnData.setToolTipText("Data tables");
	    btnData.setIcon(new ImageIcon(MapViewer.class.getResource("/icons/data.png")));
	    btnData.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				App.dataTables.setVisible(true);
            }
	    });
	    
	    JButton btnSelect = new JButton("");
	    btnSelect.setToolTipText("Select objects");
	    btnSelect.setIcon(new ImageIcon(MapViewer.class.getResource("/org/geotools/swing/icons/pointer.png")));
	    btnSelect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				show.getMapPane().setCursorTool(selectionTool);
			}
	    });
	    
	    JButton btnZoomSelected = new JButton("");
	    btnZoomSelected.setToolTipText("Zooms to selected objects");
	    btnZoomSelected.setIcon(new ImageIcon(MapViewer.class.getResource("/icons/zoom.png")));
	    btnZoomSelected.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Envelope envelope = selectionTool.getSelectedEnvelope();
				
				if (null != envelope) {
					show.getMapPane().setDisplayArea(envelope);
				}
			}
	    });
	    
	    JButton btnClear = new JButton("");
	    btnClear.setToolTipText("Clear selected features");
	    btnClear.setIcon(new ImageIcon(MapViewer.class.getResource("/icons/clear.png")));
	    btnClear.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				show.getMapPane().setCursorTool(selectionTool);
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
	    show.getToolBar().add(btnClear);
	    show.getToolBar().remove(0);
	    show.getToolBar().add(btnSelect, 0);
	    show.getToolBar().add(btnZoomSelected, 1);
	    
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
	    
	    if (App.dataController.mapData.containsKey(source.getName().toString())) {
	        return;
	    }
	    
	    App.dataController.mapData.put(source.getName().toString(), source);
	    App.dataTables.featureTypeCBox.addItem(source.getName().toString());
	    
	    addMapLayer(source);
	}
	
	public void addMapLayer(SimpleFeatureSource source) {
	    
	    Style style = new BasicPolygonStyle();
	
	    Layer layer = new FeatureLayer(source, style);
	    
	    map.addLayer(layer);
	}
}