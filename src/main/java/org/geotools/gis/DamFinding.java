package org.geotools.gis;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

public class DamFinding {

    static HashMap<String, SimpleFeature> lowestPlaces = new HashMap<>();
    static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
    static SimpleFeatureSource reliefSource;
    static SimpleFeatureSource buildingsSource;

    static double minPlot;
    static double minDepth;
    static double maxDepth;

    static HashMap<String, List<String>> visitedGeometry = new HashMap<>();

    public static void findPlaceForDam() throws IOException {
        parametersDialog();

        Calculations.loadIfMissing("sven_HID_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_HID_L.shp");
        Calculations.loadIfMissing("sven_REL_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_REL_P.shp");
        Calculations.loadIfMissing("sven_PAS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PAS_P.shp");
        Calculations.loadIfMissing("sven_PLO_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_PLO_P.shp");

        Filter filterRivers;
        Filter filterRelief;

        SimpleFeatureSource hidroSource = App.dataController.mapData.get("sven_HID_L");
        reliefSource = App.dataController.mapData.get("sven_REL_P");
        buildingsSource = App.dataController.mapData.get("sven_PAS_P");

        try {
            filterRivers = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_HID_L"));
            filterRelief = App.mapWindow.selectionTool.getSelectedFeatures(App.dataController.getLayerByName("sven_REL_P"));
        } catch (Exception e) {
            filterRivers = Filter.INCLUDE;
            filterRelief = Filter.INCLUDE;
        }

        filterRivers = ff.and(filterRivers, ff.equals(ff.property("TIPAS"), ff.literal("1"))); // filters merge

        // get only selected features and ony rivers
        SimpleFeatureCollection featuresRivers = hidroSource.getFeatures(filterRivers);
        SimpleFeatureCollection featuresRelief = reliefSource.getFeatures(filterRelief);

        System.out.println("Found rivers (features): " + featuresRivers.size());
        System.out.println("Found relief polygons (features): " + featuresRelief.size());

        IntersectionFeatureCollection intersection = new IntersectionFeatureCollection();

        SimpleFeatureCollection intersectedHidroAndRelief = intersection.execute(
            featuresRelief,
            featuresRivers,
            null,
            null,
            IntersectionFeatureCollection.IntersectionMode.FIRST,
            false,
            false
        );

        System.out.println("Intersected features: " + intersectedHidroAndRelief.size());

        System.out.println("Collecting lowest places...");

        intersectedHidroAndRelief.accepts(new FeatureVisitor() {
            public void visit(Feature feature) {
            SimpleFeature simpleFeature = (SimpleFeature) feature;

            addLowestPlace(lowestPlaces, simpleFeature);
            }
        }, null);

        System.out.println("Found different lowest places: " + lowestPlaces.size());

        SimpleFeatureType featureType = null;
        List<SimpleFeature> lowestFeaturesList = new ArrayList<>();
        List<SimpleFeature> floodedAreas = new ArrayList<>();

        for(Map.Entry<String, SimpleFeature> entry : lowestPlaces.entrySet()) {
            SimpleFeature value = entry.getValue();

            featureType = value.getFeatureType();
            lowestFeaturesList.add(value); // format layer

            SimpleFeature floodedFeature = findFloodedArea(value);

            if (null != floodedFeature) {
                floodedAreas.add(floodedFeature);
            }
        }

        SimpleFeatureSource resultSource = DataUtilities.source(
            new ListFeatureCollection(featureType, lowestFeaturesList)
        );
        App.mapWindow.addMapLayer(resultSource, "Lowest filtered areas");

        resultSource = DataUtilities.source(
                new ListFeatureCollection(featureType, floodedAreas)
        );
        App.mapWindow.addMapLayer(resultSource, "Generated flooded areas");
        App.dataController.mapData.put("Generated flooded areas", resultSource);
        App.dataTables.featureTypeCBox.addItem("Generated flooded areas");

        resultSource = DataUtilities.source(featuresRivers);

        App.mapWindow.addMapLayer(resultSource, "Rivers");


    }

    private static void parametersDialog() {
        String param1 = JOptionPane.showInputDialog("Minimalus tvenkinio plotas");
        String param2 = JOptionPane.showInputDialog("Minimalus tvenkinio gylis");
        String param3 = JOptionPane.showInputDialog("Maksimalus tvenkinio gylis");

        minPlot = Double.parseDouble(param1);
        minDepth = Double.parseDouble(param2);
        maxDepth = Double.parseDouble(param3);
    }

    private static SimpleFeature findFloodedArea(SimpleFeature firstFeature) throws IOException {

        Geometry firstFeatureGeometry = (Geometry) ((Geometry) firstFeature.getDefaultGeometry()).clone();
        Geometry featureGeometry = (Geometry) firstFeature.getDefaultGeometry();
        Geometry temp;

        double biggestDepth = 0;

        if (!visitedGeometry.containsKey(firstFeatureGeometry.toString())) {
            do {
                temp = featureGeometry;
                firstFeatureGeometry = firstFeatureGeometry.buffer(2);

                ff = CommonFactoryFinder.getFilterFactory2();

                Filter innerFilter = ff.intersects(ff.property(
                                reliefSource.getSchema().getGeometryDescriptor().getLocalName()),
                        ff.literal(firstFeatureGeometry)
                );
                SimpleFeatureCollection collection = reliefSource.getFeatures(innerFilter);

                SimpleFeatureIterator iterator = collection.features();

                double lowest = Double.parseDouble(firstFeature.getAttribute("sven_REL_P_Aukstis").toString());

                List<String> geometryExtendedList = new ArrayList<>();

                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();

                    if (!geometryExtendedList.contains(feature.getDefaultGeometry().toString())) {
                        if (lowest > Double.parseDouble(feature.getAttribute("Aukstis").toString())) {
                            lowest = Double.parseDouble(feature.getAttribute("Aukstis").toString());
                        }


                        if (Double.parseDouble(feature.getAttribute("Aukstis").toString()) - lowest <= maxDepth) {
                            featureGeometry = featureGeometry.union((Geometry) feature.getDefaultGeometry());
                        }

                        if (biggestDepth <  Double.parseDouble(feature.getAttribute("Aukstis").toString()) - lowest) {
                            biggestDepth = Double.parseDouble(feature.getAttribute("Aukstis").toString()) - lowest;
                        }

                        geometryExtendedList.add(feature.getDefaultGeometry().toString());
                    }
                }
            } while (temp.toString().equals(featureGeometry.toString()));
        }

        if (featureGeometry.getArea() < minPlot || biggestDepth < minDepth) {
            return null;
        }

//        Filter innerFilter = ff.intersects(ff.property(
//                        buildingsSource.getSchema().getGeometryDescriptor().getLocalName()),
//                ff.literal(featureGeometry)
//        );
//        SimpleFeatureCollection collection = buildingsSource.getFeatures(innerFilter);
//
//        if (0 != collection.size()) {
//            System.out.println("One of areas was discarded because it had buildings");
//            return null;
//        }

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(firstFeature.getFeatureType());

        builder.add(featureGeometry);

        return builder.buildFeature("fid");
    }

    private static void addLowestPlace(HashMap<String, SimpleFeature> lowestPlaces, SimpleFeature simpleFeature) {
        String riverName = simpleFeature.getAttribute("sven_HID_L_VARDAS").toString();

        if (riverName.isEmpty()) {
            riverName = simpleFeature.getAttribute("sven_HID_L_GKODAS").toString();
        }

        if (!riverName.isEmpty()) {
            if (lowestPlaces.containsKey(riverName)) {
                if (Double.parseDouble(lowestPlaces.get(riverName).getAttribute("sven_REL_P_Aukstis").toString())
                        > Double.parseDouble(simpleFeature.getAttribute("sven_REL_P_Aukstis").toString())) {
                    lowestPlaces.put(riverName, simpleFeature);
                }
            } else {
                lowestPlaces.put(riverName, simpleFeature);
            }
        }
    }
}
