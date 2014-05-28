package org.geotools.gis;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import java.io.IOException;

public class DamFinding {
    public static void findPlaceForDam() throws IOException {
        Calculations.loadIfMissing("sven_HID_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_HID_L.shp");
        Calculations.loadIfMissing("sven_REL_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_REL_P.shp");

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        SimpleFeature feature;
        Filter filterRivers;
        Filter filterRelief;

        SimpleFeatureSource hidroSource = App.dataController.mapData.get("sven_HID_L");
        SimpleFeatureSource reliefSource = App.dataController.mapData.get("sven_REL_P");

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
                IntersectionFeatureCollection.IntersectionMode.INTERSECTION,
                false,
                false
        );

//        SimpleFeatureIterator iterator = intersectedHidroAndRelief.features();
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
//        intersectedHidroAndRelief = new ListFeatureCollection(featureType, list);

        intersectedHidroAndRelief.accepts(new FeatureVisitor() {
            public void visit(Feature feature) {
                System.out.println(((SimpleFeature) feature).getProperties());
            }
        }, null);

//        while (iterator.hasNext()) {
//            feature = iterator.next();
//
//            if (feature.getAttribute("TIPAS").toString().equals("1")) {
//
//            }
//        }
    }
}
