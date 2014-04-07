package org.geotools.gis;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.BasicPolygonStyle;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;

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
				} catch (Exception e1) { e1.printStackTrace() ;System.out.println("Can't open file"); }
            }
	    });
	    
	    JButton btnData = new JButton("Data tables");
	    btnData.addActionListener(
    	    new ActionListener() {
    	        public void actionPerformed(ActionEvent e) {
    	        	App.dataTables.setVisible(true);
                }
    	    }
	    );
	    
	    JButton btnSelect = new JButton("Select");
	    btnSelect.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				show.getMapPane().setCursorTool(new CursorTool() {

					boolean dragged = false;
					Point startPosDevice = new Point();
					Point2D startPosWorld = new DirectPosition2D();

					@Override
					public void onMouseClicked(MapMouseEvent ev) {
						selectFeature(ev);
					}

					@Override
					public void onMousePressed(MapMouseEvent ev) {
						startPosDevice.setLocation(ev.getPoint());
						startPosWorld.setLocation(ev.getWorldPos());
					}

					@Override
					public void onMouseDragged(MapMouseEvent ev) {
						dragged = true;
					}

					@Override
					public boolean drawDragBox() {
						return true;
					}

					@Override
					public void onMouseReleased(MapMouseEvent ev) {
						if (dragged && !ev.getPoint().equals(startPosDevice)) {
							dragged = false;
							selectFeatures(ev, startPosDevice);
						}
					}
				});
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
	    show.getToolBar().add(btnSelect);
	    
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
	
	private void selectFeatures(MapMouseEvent ev, Point2D startPosWorld) {

		int startX;
		int startY;

		int width = (int) ev.getX() - (int) startPosWorld.getX();
		int height = (int) ev.getY() - (int) startPosWorld.getY();

		if (width > 0)
			startX = (int) startPosWorld.getX();
		else {
			width = Math.abs(width);
			startX = (int) ev.getX();
		}

		if (height > 0)
			startY = (int) startPosWorld.getY();
		else {
			height = Math.abs(height);
			startY = (int) ev.getY();
		}

		Rectangle screenRect = new Rectangle(startX, startY, width, height);

//		findFeatures(screenRect, ev.isControlDown());
	}
	
	private void selectFeature(MapMouseEvent ev) {

		Point screenPos = ev.getPoint();
		Rectangle screenRect = new Rectangle(screenPos.x - 2, screenPos.y - 2,
				3, 3);

//		findFeatures(screenRect, ev.isControlDown());
	}
	
//	private void findFeatures(Rectangle screenRect, boolean addMore) {
//
//		boolean selected = false;
//		
//		AffineTransform screenToWorld = show.getMapPane()
//				.getScreenToWorldTransform();
//		Rectangle2D worldRect = screenToWorld
//				.createTransformedShape(screenRect).getBounds2D();
//		ReferencedEnvelope bbox = new ReferencedEnvelope(worldRect, show
//				.getMapContent().getCoordinateReferenceSystem());
//
//		/*
//		 * Create a Filter to select features that intersect with the bounding
//		 * box
//		 */
//
//		if (!addMore)
//			for (int i = 0; i < FFSList.size(); i++)
//				FFSList.get(i).clearSelected();
//
//		for (int i = 0; i < FFSList.size(); i++)
//			if (show.getMapContent().layers().get(i).isVisible()) {
//
//				Filter filter = Styler.ff.bbox(Styler.ff.property(FFSList
//						.get(i).geometryAttributeName), bbox);
//
//				try {
//					SimpleFeatureCollection selectedFeatures = FFSList.get(i).featureSource
//							.getFeatures(filter);
//
//					SimpleFeatureIterator iter = selectedFeatures.features();
//
//					try {
//						while (iter.hasNext()) {
//							SimpleFeature feature = iter.next();
//							FFSList.get(i).featuresID
//									.add(feature.getIdentifier());
//							FFSList.get(i).markedFeatures.add(feature);
//							selected = true;
//						}
//
//					} finally {
//						iter.close();
//					}
//
//				} catch (Exception ex) {
//					ex.printStackTrace();
//					return;
//				}
//			}
//		if (!selected)
//			JOptionPane.showMessageDialog(null, "No feature selected");
//		else 
//			for (int i = 0; i < FFSList.size(); i++)
//				displaySelectedFeatures(FFSList.get(i).featuresID, i);
//	}
}