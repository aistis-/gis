package org.geotools.gis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.RenderingHints.Key;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.action.SafeAction;
import org.geotools.swing.table.FeatureCollectionTableModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

public class DataTables extends JFrame {

	public DataStore dataStore;
    public JComboBox featureTypeCBox;
    private JTable table;
    private JTextField text;
    
	public DataTables() {
		setExtendedState(JMapFrame.MAXIMIZED_BOTH);
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());

        text = new JTextField(80);
        text.setText("include"); // include selects everything!
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
        
        dataMenu.add(new SafeAction("Get features") {
            public void action(ActionEvent e) throws Throwable {
                filterFeatures();
            }
        });
        dataMenu.add(new SafeAction("Count") {
            public void action(ActionEvent e) throws Throwable {
                countFeatures();
            }
        });
        dataMenu.add(new SafeAction("Geometry") {
            public void action(ActionEvent e) throws Throwable {
                queryFeatures();
            }
        });
//        dataMenu.add(new SafeAction("Create layer from features") {
//            public void action(ActionEvent e) throws Throwable {
//                createFeaturesLayer();
//            }
//        });
        
        featureTypeCBox = new JComboBox();
        menubar.add(featureTypeCBox);
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
