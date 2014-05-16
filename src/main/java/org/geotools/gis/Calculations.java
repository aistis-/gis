package org.geotools.gis;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.Filter;

import javax.swing.*;
import javax.xml.crypto.Data;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class Calculations {

    private static void loadIfMissing(String name, String path) throws IOException {
        if (!App.mapWindow.map.getUserData().containsKey(name)) {
            App.mapWindow.addLayerFromFile(path);
        }
    }

    public static void calculatePASTATAI() throws IOException {
        loadIfMissing("RIBOS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
        loadIfMissing("HIDRO_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PLO_P.shp");
        loadIfMissing("HIDRO_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PAS_P.shp");

        SimpleFeatureSource buildingsSource = App.dataController.mapData.get("sven_PAS_P");
        SimpleFeatureSource areasSource = App.dataController.mapData.get("sven_PLO_P");
        SimpleFeatureSource polygonsSource = App.dataController.mapData.get("sven_SAV_P");

        SimpleFeatureType schema2 = areasSource.getSchema();
        String typeName2 = schema2.getTypeName();
        String geomName2 = schema2.getGeometryDescriptor().getLocalName();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        SimpleFeatureType schema = buildingsSource.getSchema();
        String typeName = schema.getTypeName();
        String geomName = schema.getGeometryDescriptor().getLocalName();

        Filter filter;

        try {
            filter = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_SAV_P"));
        } catch (Exception e) {
            filter = Filter.INCLUDE;
        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator = outerFeatures.features();
        SimpleFeatureIterator iteratorJoined;
        SimpleFeatureIterator iteratorJoined2;

        HashMap<String, FeatureInfo> calculations = new HashMap<>();

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                if (!geometry.isValid()) {
                    // skip bad data
                    continue;
                }
                Filter innerFilter = ff.intersects(ff.property(geomName2), ff.literal(geometry));
                Query innerQuery = new Query(typeName2, innerFilter, Query.ALL_NAMES);
                SimpleFeatureCollection join = areasSource.getFeatures(innerQuery);

                System.out.println("Found intersected features: " + join.size());

                SimpleFeatureCollection intersectedCollection = new IntersectedFeatureCollection(
                    join,
                    outerFeatures
                );

                SimpleFeatureIterator iterator2 = intersectedCollection.features();
                SimpleFeature feature2;

                ArrayList<SimpleFeature> list = new ArrayList<>();

                while (iterator2.hasNext()) {
                    feature2 = iterator2.next();

                    list.add(feature2);
                }

                intersectedCollection = DataUtilities.collection(list);

//                IntersectionFeatureCollection inter = new IntersectionFeatureCollection();
//                SimpleFeatureCollection intersectedCollection = inter.execute(
//                        join,
//                        outerFeatures,
//                        null,
//                        null,
//                        IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
//                        false,
//                        false
//                );

                iteratorJoined = intersectedCollection.features();

                while (iteratorJoined.hasNext()) {
                    feature = iteratorJoined.next();
                    geometry = (Geometry) feature.getDefaultGeometry();
//                    geometry = (Geometry) feature.getAttribute(0);

                    if (!geometry.isValid()) {
                        // skip bad data
                        continue;
                    }
                    innerFilter = ff.intersects(ff.property(geomName), ff.literal(geometry));
                    innerQuery = new Query(typeName, innerFilter, Query.ALL_NAMES);
                    join = buildingsSource.getFeatures(innerQuery);

                    SimpleFeatureCollection intersectedCollection2 = new IntersectedFeatureCollection(
                        join,
                        intersectedCollection
                    );

                    iteratorJoined2 = intersectedCollection2.features();

                    System.out.println("Found intersected features with buildings: " + join.size());

                    while (iteratorJoined2.hasNext()) {
                        feature = iteratorJoined2.next();

                        String type;

                        switch (feature.getAttribute(5).toString()) {
                            case "hd1":
                            case "hd2":
                            case "hd3":
                            case "hd4":
                            case "hd9":
                                type = "Hidro";
                                break;
                            case "ms0":
                                type = "Woods";
                                break;
                            case "ms4":
                                type = "Gardens";
                                break;
                            case "pu0":
                                type = "Buildings";
                                break;
                            default:
                                type = "Unknown";
                        }

//                        try {
                            String key = feature.getAttribute("sven_SAV_P_SAV").toString() + "_" + type;

                            if (!calculations.containsKey(key)) {
                                calculations.put(key, new FeatureInfo());
                            }

                            FeatureInfo featureInfo = calculations.get(key);

                            featureInfo.region = feature.getAttribute("sven_SAV_P_SAV").toString();

                            featureInfo.type = type;
                            featureInfo.frequency++;
                            featureInfo.size += ((Geometry) feature.getDefaultGeometry()).getArea();
                            featureInfo.size += Double.parseDouble(feature.getAttribute("sven_PLO_P_SHAPE_area").toString());
                            featureInfo.regionPlot += Double.parseDouble(feature.getAttribute("sven_PLO_P_PLOT").toString());
//                        } catch (NullPointerException e) {
//                            continue;
//                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            iterator.close();
        }

        String[][] data = new String[calculations.size()][6];

        Iterator<String> iter = calculations.keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            String it = iter.next();

            data[i][0] = calculations.get(it).region;
            data[i][1] = calculations.get(it).type;
            data[i][2] = Double.toString(calculations.get(it).size);
            data[i][3] = Integer.toString(calculations.get(it).frequency);
            data[i][4] = Double.toString(calculations.get(it).regionPlot);

            if (0 == calculations.get(it).regionPlot) {
                data[i][5] = "0";
            } else {
                data[i][5] = new DecimalFormat("#.###").format((calculations.get(it).size * 100) / calculations.get(it).regionPlot);
            }

            i++;
        }

        String[] columnNames = { "Region", "Area type", "Area plot", "Building frequency", "Building plot", "Percentage"};

        JTable table = new JTable(data, columnNames);

        JFrame windows = new JFrame();
        windows.getContentPane().add(new JScrollPane(table));
        windows.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
        windows.setVisible(true);

    }

    public static void calculatePLOTAI() throws IOException {
        loadIfMissing("RIBOS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
        loadIfMissing("HIDRO_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PLO_P.shp");

        SimpleFeatureSource areasSource = App.dataController.mapData.get("sven_PLO_P");
        SimpleFeatureSource polygonsSource = App.dataController.mapData.get("sven_SAV_P");

        SimpleFeatureType schema2 = areasSource.getSchema();
        String typeName2 = schema2.getTypeName();
        String geomName2 = schema2.getGeometryDescriptor().getLocalName();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Filter filter;

        try {
            filter = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_SAV_P"));
        } catch (Exception e) {
            filter = Filter.INCLUDE;
        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator = outerFeatures.features();
        SimpleFeatureIterator iteratorJoined;

        HashMap<String, FeatureInfo> calculations = new HashMap<>();

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                try {
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();

                    if (!geometry.isValid()) {
                        // skip bad data
                        continue;
                    }
                    Filter innerFilter = ff.intersects(ff.property(geomName2), ff.literal(geometry));
                    Query innerQuery = new Query(typeName2, innerFilter, Query.ALL_NAMES);
                    SimpleFeatureCollection join = areasSource.getFeatures(innerQuery);

                    System.out.println("Found intersected features: " + join.size());

                    SimpleFeatureCollection intersectedCollection = new IntersectedFeatureCollection(
                        join,
                        outerFeatures
                    );

                    iteratorJoined = intersectedCollection.features();

                    while (iteratorJoined.hasNext()) {
                        feature = iteratorJoined.next();

                        String type;

                        switch (feature.getAttribute("sven_PLO_P_GKODAS").toString()) {
                            case "hd1":
                            case "hd2":
                            case "hd3":
                            case "hd4":
                            case "hd9":
                                type = "Hidro";
                                break;
                            case "ms0":
                                type = "Woods";
                                break;
                            case "ms4":
                                type = "Gardens";
                                break;
                            case "pu0":
                                type = "Buildings";
                                break;
                            default:
                                type = "Unknown";
                        }

                        String key = feature.getAttribute("sven_SAV_P_SAV").toString() + "_" + type;

                        if (!calculations.containsKey(key)) {
                            calculations.put(key, new FeatureInfo());
                        }

                        FeatureInfo featureInfo = calculations.get(key);

                        featureInfo.region = feature.getAttribute("sven_SAV_P_SAV").toString();

                        featureInfo.type = type;
                        featureInfo.frequency++;
                        featureInfo.size += Double.parseDouble(feature.getAttribute("sven_PLO_P_SHAPE_area").toString());
//                        featureInfo.size += ((Geometry)feature.getDefaultGeometry()).getArea();
                        featureInfo.regionPlot = Double.parseDouble(feature.getAttribute("sven_SAV_P_PLOT").toString());
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        } finally {
            iterator.close();
        }

        String[][] data = new String[calculations.size()][6];

        Iterator<String> iter = calculations.keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            String it = iter.next();

            data[i][0] = calculations.get(it).region;
            data[i][1] = calculations.get(it).type;
            data[i][2] = Integer.toString(calculations.get(it).frequency);
            data[i][3] = Double.toString(calculations.get(it).size);
            data[i][4] = Double.toString(calculations.get(it).regionPlot);

            if (0 == calculations.get(it).regionPlot) {
                data[i][5] = "0";
            } else {
                data[i][5] = new DecimalFormat("#.###").format((calculations.get(it).size * 100) / calculations.get(it).regionPlot);
            }

            i++;
        }

        String[] columnNames = { "Region", "Area type", "Type frequency", "Area plot", "Region plot", "Percentage"};

        JTable table = new JTable(data, columnNames);

        JFrame windows = new JFrame();
        windows.getContentPane().add(new JScrollPane(table));
        windows.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
        windows.setVisible(true);
    }

	public static void calculateRiversLength(String shapfileName) throws IOException {
		loadIfMissing("HIDRO_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\" + shapfileName + ".shp");
        loadIfMissing("RIBOS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
		
		SimpleFeatureSource hidroSource = App.dataController.mapData.get(shapfileName);
		SimpleFeatureSource polygonsSource = App.dataController.mapData.get("sven_SAV_P");

        SimpleFeatureType schema2 = hidroSource.getSchema();
        String typeName2 = schema2.getTypeName();
        String geomName2 = schema2.getGeometryDescriptor().getLocalName();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Filter filter;

        try {
            filter = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_SAV_P"));
        } catch (Exception e) {
            filter = Filter.INCLUDE;
        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator = outerFeatures.features();
        SimpleFeatureIterator iteratorJoined;

        HashMap<String, Double> calculations = new HashMap<>();
        Double length;

        try {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                try {
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();

                    if (!geometry.isValid()) {
                        // skip bad data
                        continue;
                    }
                    Filter innerFilter = ff.intersects(ff.property(geomName2), ff.literal(geometry));
                    Query innerQuery = new Query(typeName2, innerFilter, Query.ALL_NAMES);
                    SimpleFeatureCollection join = hidroSource.getFeatures(innerQuery);

                    System.out.println("Found intersected features: " + join.size());

                    SimpleFeatureCollection intersectedCollection = new IntersectedFeatureCollection(
                        join,
                        outerFeatures
                    );

                    SimpleFeatureIterator iterator2 = intersectedCollection.features();
                    SimpleFeature feature2;

                    ArrayList<SimpleFeature> list = new ArrayList<>();

                    while (iterator2.hasNext()) {
                        feature2 = iterator2.next();

                        list.add(feature2);
                    }

                    intersectedCollection = DataUtilities.collection(list);

                    iteratorJoined = intersectedCollection.features();

                    while (iteratorJoined.hasNext()) {
                        feature = iteratorJoined.next();

//                        geometry = (Geometry)(feature.getDefaultGeometry());
//                        geometry.geometryChanged();
//                        length = geometry.getLength();
                        length = Double.parseDouble(feature.getAttribute(shapfileName + "_SHAPE_len").toString());

                        if (null != feature.getAttribute("NO_NAME_SHAPE_len")) {
                            System.out.println("LOL");
                        }

                        if (shapfileName.equals("sven_HID_L") && !feature.getAttribute("sven_HID_L_TIPAS").toString().equals("1")) {
                            length = 0.;
                        }

                        if (calculations.containsKey(feature.getAttribute("sven_SAV_P_SAV").toString())) {
                            calculations.put(
                                feature.getAttribute("sven_SAV_P_SAV").toString(),
                                calculations.get(feature.getAttribute("sven_SAV_P_SAV").toString()) + length
                            );
                        } else {
                            calculations.put(feature.getAttribute("sven_SAV_P_SAV").toString(), length);
                        }
                    }
                } catch (Exception e) {e.printStackTrace(); }
            }
        } finally {
            iterator.close();
        }

        String message = "Calculated lengths:\n";

        for(Map.Entry<String, Double> entry : calculations.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();

            message += key + " " + value.toString() + "\n";
        }

        JOptionPane.showMessageDialog(null, message);
	}
}
