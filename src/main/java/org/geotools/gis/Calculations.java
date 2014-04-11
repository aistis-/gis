package org.geotools.gis;

import java.io.IOException;

import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.filter.Filter;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;

public class Calculations {

	public static void calculateRiversLength() throws IOException {
		if (false == App.mapWindow.map.getUserData().containsKey("HIDRO_L")) {
			App.mapWindow.addLayerFromFile("C:\\Users\\lol\\Desktop\\gis\\LT10shp\\HIDRO_L.shp");
		}
		
		if (false == App.mapWindow.map.getUserData().containsKey("RIBOS_P")) {
			App.mapWindow.addLayerFromFile("C:\\Users\\lol\\Desktop\\gis\\LT10shp\\RIBOS_P.shp");
		}
		
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
		
		SimpleFeatureSource hidroSource = App.dataController.mapData.get("HIDRO_L");
		SimpleFeatureSource polygonsSource = App.dataController.mapData.get("RIBOS_P");
		
	    SimpleFeatureType schema = polygonsSource.getSchema();
	    String typeName = schema.getTypeName();
	    String geomName = schema.getGeometryDescriptor().getLocalName();
	    
	    SimpleFeatureType schema2 = hidroSource.getSchema();
	    String typeName2 = schema2.getTypeName();
	    String geomName2 = schema2.getGeometryDescriptor().getLocalName();
	    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

	    Query outerGeometry = new Query(typeName, Filter.INCLUDE, new String[] { geomName });
	    SimpleFeatureCollection outerFeatures = polygonsSource.getFeatures(outerGeometry);
	    SimpleFeatureIterator iterator = outerFeatures.features();
//	    System.out.println(hidroSource.getFeatures().size());
//	    System.out.println(outerFeatures.size());
	    int max = 0;
	    
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
//	                Filter innerFilter = ff.not( ff.disjoint(ff.property(geomName2), ff.literal( geometry )) );
	                Query innerQuery = new Query(typeName2, innerFilter, Query.NO_NAMES);
	                SimpleFeatureCollection join = hidroSource.getFeatures(innerQuery);
	                int size = join.size();
	                max = Math.max(max, size);
	            } catch (Exception skipBadData) {}
	        }
	    } finally {
	        iterator.close();
	    }
	    System.out.println("At most " + max + " " + typeName2 + " features in a single " + typeName
	            + " feature");
	}
	
}
