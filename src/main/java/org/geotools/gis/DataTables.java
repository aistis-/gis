package org.geotools.gis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.FilterFilter;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.map.Layer;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.omg.CORBA.IdentifierHelper;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.Identifier;

public class DataTables extends JFrame {

    public JComboBox featureTypeCBox;
    private JTable table;
    private JTextField text;
    
	public DataTables() throws Exception {
		setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include");
        getContentPane().add(text, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setModel(new DefaultTableModel(0, 0));
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));

        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        JMenuBar menubar = new JMenuBar();
        setJMenuBar(menubar);

        JMenu dataMenu = new JMenu("Actions");
        menubar.add(dataMenu);
        pack();
        
        dataMenu.add(new SafeAction("Show only selected on map") {
            public void action(ActionEvent e) throws Throwable {
                filterSelectedFeatures();
            }
        });
        dataMenu.add(new SafeAction("Count by query") {
            public void action(ActionEvent e) throws Throwable {
                countFeatures();
            }
        });
        dataMenu.add(new SafeAction("Use filter") {
            public void action(ActionEvent e) throws Throwable {
                filterFeatures();
            }
        });
        dataMenu.add(new SafeAction("Get geometry") {
            public void action(ActionEvent e) throws Throwable {
                queryFeatures();
            }
        });
        
        dataMenu.addSeparator();
        
        dataMenu.add(new SafeAction("Show selected features on map") {
            public void action(ActionEvent e) throws Throwable {
            	if (table.getSelectedRowCount() > 0) {                    
                    HashSet<Identifier> selectedIds = new HashSet<Identifier>();
                    
                    Layer layer = App.dataController.getLayerByName((String) featureTypeCBox.getSelectedItem());

                    FeatureIterator iterator = layer.getFeatureSource().getFeatures().features();

                    int selected = 0;
                    
					while (iterator.hasNext()) {
						Feature feature = iterator.next();

						for (int i = 0; i < table.getSelectedRowCount(); i++) {
							if (feature.getIdentifier().getID().equals(table.getValueAt(table.getSelectedRows()[i], 0))) {
								selectedIds.add(feature.getIdentifier());
								selected++;
							}
	    			    }
						
						if (selected == table.getSelectedRowCount()) {
							break;
						}
					}
                    
                    App.mapWindow.selectionTool.selectFeatures(layer, selectedIds);
            	}
            }
        });
        
        featureTypeCBox = new JComboBox();
        menubar.add(featureTypeCBox);
    }
	
	private void filterSelectedFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = App.dataController.mapData.get(typeName);

        Filter filter = App.mapWindow.selectionTool.getSelectedFeatures(
    		App.dataController.getLayerByName((String) featureTypeCBox.getSelectedItem())
		);

        SimpleFeatureCollection features = source.getFeatures(filter);
        
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
	
	private void filterFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = App.dataController.mapData.get(typeName);

        Filter filter = CQL.toFilter(text.getText());

        SimpleFeatureCollection features = source.getFeatures(filter);
        
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
    
    private void countFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = App.dataController.mapData.get(typeName);

        Filter filter = CQL.toFilter(text.getText());
        SimpleFeatureCollection features = source.getFeatures(filter);

        int count = features.size();
        JOptionPane.showMessageDialog(text, "Number of selected features:" + count);
        
        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
    
    private void queryFeatures() throws Exception {
        String typeName = (String) featureTypeCBox.getSelectedItem();
        SimpleFeatureSource source = App.dataController.mapData.get(typeName);

        FeatureType schema = source.getSchema();
        String name = schema.getGeometryDescriptor().getLocalName();

        Filter filter = CQL.toFilter(text.getText());

        Query query = new Query(typeName, filter, new String[] { name });

        SimpleFeatureCollection features = source.getFeatures(query);

        FeatureCollectionTableModel model = new FeatureCollectionTableModel(features);
        table.setModel(model);
    }
}
