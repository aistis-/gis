package org.geotools.gis;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import javax.swing.JOptionPane;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class Calculations {

    private static void loadIfMissing(String name, String path) throws IOException {
        if (!App.mapWindow.map.getUserData().containsKey(name)) {
            App.mapWindow.addLayerFromFile(path);
        }
    }

	public static void calculateRiversLength(String shapfileName) throws IOException {
		loadIfMissing("HIDRO_L", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\" + shapfileName + ".shp");
        loadIfMissing("RIBOS_P", "C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
		
//		SimpleFeatureSource hidroSource = App.dataController.mapData.get("HIDRO_L");
//		SimpleFeatureSource polygonsSource = App.dataController.mapData.get("RIBOS_P");
//		
//		SimpleFeatureCollection polygonCollection = polygonsSource.getFeatures();
//	    SimpleFeatureCollection fcResult = null;
//	    final DefaultFeatureCollection found = new DefaultFeatureCollection();
//	    
//	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
//	    SimpleFeature feature = null;
//	    
//	    Filter polyCheck = null;
//	    Filter andFil = null;
//	    Filter boundsCheck = null;
//	    
//	    String qryStr = null;
//	    
//	    
//	    
//	    SimpleFeatureIterator it = polygonCollection.features();
//	    try {
//	        while (it.hasNext()) {
//	            feature = it.next();
//	            BoundingBox bounds = feature.getBounds();
//	            boundsCheck = ff.bbox(ff.property("the_geom"), bounds);
//	            
//	            Geometry geom = (Geometry) feature.getDefaultGeometry();
//	            polyCheck = ff.intersects(ff.property("the_geom"), ff.literal(geom));
//	            
//	            andFil = ff.and(boundsCheck, polyCheck);
//	            
//	            try {
//	                fcResult = hidroSource.getFeatures(andFil);
//	                // go through results and copy out the found features
//	                fcResult.accepts(new FeatureVisitor() {
//	                    public void visit(Feature feature) {
//	                        found.add((SimpleFeature) feature);
//	                    }
//	                }, null);
//	            } catch (IOException e1) {
//	                System.out.println("Unable to run filter for " + feature.getID() + ":" + e1);
//	                continue;
//	            }
//	            
//	        }
//	    } finally {
//	        it.close();
//	    }
		
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

//        Query outerGeometry = new Query(typeName, filter, Query.ALL_NAMES);
//	    SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(outerGeometry);
//        SimpleFeatureIterator iterator = null;
//        IntersectionFeatureCollection intersection = new IntersectionFeatureCollection();
//
//        try {
//            SimpleFeatureCollection intersectedCollection = intersection.execute(
//                hidroSource.getFeatures(),
//                outerFeatures,
//                null,
//                null,
//                IntersectionFeatureCollection.IntersectionMode.SECOND,
//                false,
//                false
//            );
//
//            iterator = intersectedCollection.features();
//            System.out.println(intersectedCollection.size());
//
//        } catch (Exception e) { e.printStackTrace(); }
//
//        SimpleFeature feature;
//
//	    int length = 0;
//        int i = 0;
//        int j = 0;
//
//        if (null != iterator) {
//            try {
//                while (iterator.hasNext()) {
//                    i++;
//
//                    feature = iterator.next();
//
//                    if (null != feature.getAttribute("sven_SAV_P_SAV")) {
//                        j++;
//                    }
//
////                    System.out.println(feature.getAttribute("sven_SAV_P_SAV").toString());
//
////                    length += Double.parseDouble(feature.getAttribute(shapfileName + "_SHAPE_len").toString());
//
//                    System.out.println(feature.getAttribute("sven_SAV_P_SAV").toString());
//                    System.out.println(i);
//                    System.out.println(j);
//                }
//            } catch (Exception e) { e.printStackTrace(); } finally {
//                iterator.close();
//            }
//        }

        SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(filter);
        SimpleFeatureIterator iterator = outerFeatures.features();
        SimpleFeatureIterator iteratorJoined;

        HashMap<String, Double> calculations = new HashMap<String, Double>();
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

                    iteratorJoined = intersectedCollection.features();

                    while (iteratorJoined.hasNext()) {
                        feature = iteratorJoined.next();

                        length = Double.parseDouble(feature.getAttribute(shapfileName + "_SHAPE_len").toString());

                        if (calculations.containsKey(feature.getAttribute("sven_SAV_P_SAV").toString())) {
                            calculations.put(
                                feature.getAttribute("sven_SAV_P_SAV").toString(),
                                calculations.get(feature.getAttribute("sven_SAV_P_SAV").toString()) + length
                            );
                        } else {
                            calculations.put(feature.getAttribute("sven_SAV_P_SAV").toString(), length);
                        }
                    }
                } catch (Exception skipBadData) {}
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
