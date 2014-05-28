package org.geotools.gis;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.swing.JMapFrame;
import org.opengis.filter.Filter;

import javax.swing.*;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Calculations {

    public static void loadIfMissing(String name, String path) throws IOException {
        if (!App.mapWindow.map.getUserData().containsKey(name)) {
            App.mapWindow.addLayerFromFile(path);
        }
    }

    public static void calculatePASTATAI() throws IOException {
        loadIfMissing("sven_SAV_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
        loadIfMissing("sven_PLO_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PLO_P.shp");
        loadIfMissing("sven_PAS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PAS_P.shp");

        SimpleFeatureSource buildingsSource = App.dataController.mapData.get("sven_PAS_P");
        SimpleFeatureSource areasSource = App.dataController.mapData.get("sven_PLO_P");
        SimpleFeatureSource polygonsSource = App.dataController.mapData.get("sven_SAV_P");

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        SimpleFeatureType schema = areasSource.getSchema();

        Filter filter;

        try {
            filter = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_SAV_P"));
        } catch (Exception e) {
            filter = Filter.INCLUDE;
        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator;
        SimpleFeature feature;

        String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope bbox = new ReferencedEnvelope(outerFeatures.getBounds().getMinX(), outerFeatures.getBounds().getMaxX(),
                outerFeatures.getBounds().getMinY(), outerFeatures.getBounds().getMaxY(), targetCRS);

        filter = ff.bbox(ff.property(geometryPropertyName), bbox);

        SimpleFeatureCollection filteredAreaFeatures = areasSource.getFeatures(filter);

        IntersectionFeatureCollection intersection = new IntersectionFeatureCollection();

        SimpleFeatureCollection intersectedCollection = intersection.execute(
            filteredAreaFeatures,
            outerFeatures,
            null,
            null,
            IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
            false,
            false
        );

        iterator = intersectedCollection.features();

        HashMap<String, Double> PLOTAIareas = new HashMap<>();

        while (iterator.hasNext()) {
            feature = iterator.next();

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

            if (!PLOTAIareas.containsKey(key)) {
                PLOTAIareas.put(key, ((Geometry)feature.getDefaultGeometry()).getArea());
            } else {
                PLOTAIareas.put(
                    key,
                    PLOTAIareas.get(key) + ((Geometry)feature.getDefaultGeometry()).getArea()
                );
            }

        }

        geometryPropertyName = buildingsSource.getSchema().getGeometryDescriptor().getLocalName();
        targetCRS = buildingsSource.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();

        bbox = new ReferencedEnvelope(outerFeatures.getBounds().getMinX(), outerFeatures.getBounds().getMaxX(),
                outerFeatures.getBounds().getMinY(), outerFeatures.getBounds().getMaxY(), targetCRS);

        filter = ff.bbox(ff.property(geometryPropertyName), bbox);

        SimpleFeatureCollection filteredBuildingsFeatures = buildingsSource.getFeatures(filter);

        intersection = new IntersectionFeatureCollection();

        SimpleFeatureCollection intersectedCollection2 = intersection.execute(
                filteredBuildingsFeatures,
                intersectedCollection,
                null,
                null,
                IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
                false,
                false
        );

        intersection = new IntersectionFeatureCollection();

        SimpleFeatureCollection intersectedCollection3 = intersection.execute(
                intersectedCollection2,
                outerFeatures,
                null,
                null,
                IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
                false,
                false
        );

//        iterator = intersectedCollection2.features();
//        SimpleFeatureType featureType = null;
//
//        ArrayList<SimpleFeature> list = new ArrayList<>();
//
//        int j = 1;
//
//        while (iterator.hasNext()) {
//            feature = iterator.next();
//            featureType = feature.getFeatureType();
//            System.out.println(j++);
//            list.add(feature);
//        }
//
//        intersectedCollection2 = new ListFeatureCollection(featureType, list);

        iterator = intersectedCollection3.features();
        HashMap<String, FeatureInfo> calculations = new HashMap<>();

        int j = 1;

        try {
            while (iterator.hasNext()) {
                feature = iterator.next();

                String type;

                System.out.println(j++ + "/" + intersectedCollection3.size());

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

                String key = feature.getAttribute("sven_SAV_P_SAV").toString() + "_" + type;

                if (!calculations.containsKey(key)) {
                    calculations.put(key, new FeatureInfo());
                }

                FeatureInfo featureInfo = calculations.get(key);

                featureInfo.region = feature.getAttribute("sven_SAV_P_SAV").toString();

                featureInfo.type = type;
                featureInfo.frequency++;
                featureInfo.size += ((Geometry) feature.getAttribute(0)).getArea();
                featureInfo.regionPlot = PLOTAIareas.get(key);
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
        loadIfMissing("sven_SAV_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
        loadIfMissing("sven_PLO_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PLO_P.shp");

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
                        featureInfo.size += ((Geometry)feature.getDefaultGeometry()).getArea();
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
		loadIfMissing(shapfileName, "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\" + shapfileName + ".shp");
        loadIfMissing("sven_SAV_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
		
		SimpleFeatureSource hidroSource = App.dataController.mapData.get(shapfileName);
		SimpleFeatureSource polygonsSource = App.dataController.mapData.get("sven_SAV_P");

        SimpleFeatureType schema = hidroSource.getSchema();
//        String typeName = schema.getTypeName();
//        String geomName = schema.getGeometryDescriptor().getLocalName();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

        Filter filter;

        try {
            filter = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_SAV_P"));
        } catch (Exception e) {
            filter = Filter.INCLUDE;
        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator = outerFeatures.features();
//        SimpleFeatureIterator iterator2;
        SimpleFeature feature;
//        SimpleFeatureType featureType = null;

//        ArrayList<SimpleFeature> filteredFeaturesList = new ArrayList<>();

        String geometryPropertyName = schema.getGeometryDescriptor().getLocalName();
        CoordinateReferenceSystem targetCRS = schema.getGeometryDescriptor().getCoordinateReferenceSystem();

        ReferencedEnvelope bbox = new ReferencedEnvelope(outerFeatures.getBounds().getMinX(), outerFeatures.getBounds().getMaxX(),
                outerFeatures.getBounds().getMinY(), outerFeatures.getBounds().getMaxY(), targetCRS);

        filter = ff.bbox(ff.property(geometryPropertyName), bbox);

        SimpleFeatureCollection filteredFeatures = hidroSource.getFeatures(filter);

//        while (iterator.hasNext()) {
//            feature = iterator.next();
//
//            Geometry geometry = (Geometry) feature.getDefaultGeometry();
//
//            if (!geometry.isValid()) {
//                continue;
//            }
//            Filter innerFilter = ff.intersects(ff.property(geomName), ff.literal(geometry));
//            Query innerQuery = new Query(typeName, innerFilter, Query.ALL_NAMES);
//            SimpleFeatureCollection filtered = hidroSource.getFeatures(innerQuery);
//
//            iterator2 = filtered.features();
//
//            while (iterator2.hasNext()) {
//                feature = iterator2.next();
//
//                featureType = feature.getFeatureType();
//
//                filteredFeaturesList.add(feature);
//            }
//        }
//
//        System.out.println("1: " + filteredFeaturesList.size());

//        SimpleFeatureCollection filteredFeatures = new ListFeatureCollection(featureType, filteredFeaturesList);

        IntersectionFeatureCollection intersection = new IntersectionFeatureCollection();

        SimpleFeatureCollection intersectedCollection = intersection.execute(
            filteredFeatures,
            outerFeatures,
            null,
            null,
            IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
            false,
            false
        );
//
//        SimpleFeatureCollection intersectedCollection = new IntersectedFeatureCollection(
//            filteredFeatures,
//            outerFeatures
//        );

        iterator = intersectedCollection.features();
        Geometry geometry;
        HashMap<String, FeatureInfo> calculations = new HashMap<>();
        Double length;

        try {
            while (iterator.hasNext()) {
                feature = iterator.next();

                geometry = (Geometry) (feature.getDefaultGeometry());
                length = geometry.getLength();

                if (shapfileName.equals("sven_HID_L") && !feature.getAttribute("sven_HID_L_TIPAS").toString().equals("1")) {
                    continue;
                }

                String key = feature.getAttribute("sven_SAV_P_SAV").toString();

                if (!calculations.containsKey(key)) {
                    calculations.put(key, new FeatureInfo());
                }

                FeatureInfo featureInfo = calculations.get(key);

                featureInfo.region = feature.getAttribute("sven_SAV_P_SAV").toString();
                featureInfo.frequency++;
                featureInfo.size += length;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            iterator.close();
        }

        String[][] data = new String[calculations.size()][3];

        Iterator<String> iter = calculations.keySet().iterator();
        int i = 0;
        while (iter.hasNext()) {
            String it = iter.next();

            data[i][0] = calculations.get(it).region;
            data[i][1] = Integer.toString(calculations.get(it).frequency);
            data[i][2] = Double.toString(calculations.get(it).size);

            i++;
        }

        String[] columnNames = { "Region", "Frequency", "Length"};

        JTable table = new JTable(data, columnNames);

        JFrame windows = new JFrame();
        windows.getContentPane().add(new JScrollPane(table));
        windows.setExtendedState(JMapFrame.MAXIMIZED_BOTH);
        windows.setVisible(true);
	}
}
