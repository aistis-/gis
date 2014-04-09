package org.geotools.gis;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.Identifier;
import org.opengis.geometry.coordinate.Polygon;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import javax.swing.JOptionPane;

public class SelectionTool extends CursorTool {

    private StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
    private FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
    private final Point startPoint;
    private final Point2D startGeomPoint;
    private boolean dragged;

    private HashMap<Layer, HashSet<Identifier>> selectedFeatureIds;

    public SelectionTool() {
        super();
        startPoint = new Point();
        startGeomPoint = new DirectPosition2D();
        dragged = false;
        selectedFeatureIds = new HashMap<>();
    }

    public Rule createRule(Color outlineColor, Color fillColor, float lineWidth, SimpleFeatureSource featureSource) {
        GeometryDescriptor descriptor = featureSource.getSchema().getGeometryDescriptor();
        String geometryAttributeName = descriptor.getLocalName();

        Symbolizer symbolizer = null;
        Fill fill = null;
        Stroke stroke = sf.createStroke(ff.literal(outlineColor), ff.literal(lineWidth));

        GeometryType geomType = descriptor.getType();
        Class<?> typeClass = geomType.getBinding();
        if (Polygon.class.isAssignableFrom(typeClass) || MultiPolygon.class.isAssignableFrom(typeClass)) {
            fill = sf.createFill(ff.literal(fillColor), ff.literal(0.8f));
            symbolizer = sf.createPolygonSymbolizer(stroke, fill, geometryAttributeName);
        } else if (LineString.class.isAssignableFrom(typeClass) || MultiLineString.class.isAssignableFrom(typeClass)) {
            symbolizer = sf.createLineSymbolizer(stroke, geometryAttributeName);
        } else {
            fill = sf.createFill(ff.literal(fillColor), ff.literal(0.8f));

            Mark mark = sf.getCircleMark();
            mark.setFill(fill);
            mark.setStroke(stroke);

            Graphic graphic = sf.createDefaultGraphic();
            graphic.graphicalSymbols().clear();
            graphic.graphicalSymbols().add(mark);
            graphic.setSize(ff.literal(2 * lineWidth));

            symbolizer = sf.createPointSymbolizer(graphic, geometryAttributeName);
        }

        Rule rule = sf.createRule();
        rule.symbolizers().add(symbolizer);
        return rule;
    }
    
    public Style createDefaultStyle(SimpleFeatureSource featureSource) {
        Rule rule = createRule(Color.BLACK, Color.GRAY, 1f, featureSource);

        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        fts.rules().add(rule);

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        return style;
    }

    private void selectFeatures(Filter filter, Layer layer) {
        HashSet<Identifier> selectedIds = getSelectedLayerFeatureSet(layer);

        try {
            SimpleFeatureCollection selectedFeatures = (SimpleFeatureCollection) layer.getFeatureSource().getFeatures(filter);
            SimpleFeatureIterator iter = selectedFeatures.features();
            while (iter.hasNext()) {
                SimpleFeature feature = iter.next();
                toggleSelectFeature(feature, selectedIds);
            }
            iter.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Layer: " + layer.getTitle() + " filtering error");
        }
        updateMapView(layer, selectedIds);
    }

    private HashSet<Identifier> getSelectedLayerFeatureSet(Layer layer) {
        HashSet<Identifier> set = selectedFeatureIds.get(layer);
        if (set == null) {
            set = new HashSet<>();
            selectedFeatureIds.put(layer, set);
        }
        return set;
    }

    public ReferencedEnvelope getSelectedEnvelope() {
        ReferencedEnvelope envelope = new ReferencedEnvelope();
        try {
            for (Layer layer : selectedFeatureIds.keySet()) {
                if (layer.isSelected()) {
                    FeatureCollection features = layer.getFeatureSource().getFeatures(ff.id(selectedFeatureIds.get(layer)));
                    envelope.expandToInclude(features.getBounds());					
                }
            }
        } 
        catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Feature envelop error");
        }
        if (envelope.isEmpty())            
            return null;
        else
            return envelope;
    }

    public void updateMapView(Layer layer, HashSet<Identifier> selectedIds) {
        FeatureTypeStyle fts = sf.createFeatureTypeStyle();
        SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();

        if (selectedIds.isEmpty()) {
            Rule rule = createRule(Color.BLACK, Color.GRAY, 1f, featureSource);
            fts.rules().add(rule);
        } 
        else {
            Rule selectedRule = createRule(Color.BLACK, Color.CYAN, 1f, featureSource);
            Rule otherRule = createRule(Color.BLACK, Color.GRAY, 1f, featureSource);

            selectedRule.setFilter(ff.id(selectedIds));
            otherRule.setFilter(ff.not(ff.id(selectedIds)));

            fts.rules().add(selectedRule);
            fts.rules().add(otherRule);
        }

        Style style = sf.createStyle();
        style.featureTypeStyles().add(fts);
        ((FeatureLayer) layer).setStyle(style);
    }

    private void toggleSelectFeature(SimpleFeature feature, HashSet<Identifier> ids) {
        if (ids.contains(feature.getIdentifier())) {
            ids.remove(feature.getIdentifier());
        } else {
            ids.add(feature.getIdentifier());
        }
    }

    public void clearSelection(){
        HashSet<Identifier> emptySet = new HashSet<>();
        for (Layer layer : selectedFeatureIds.keySet()){
            selectedFeatureIds.get(layer).clear();
            updateMapView(layer, emptySet);
        }
    }

    private Filter createClickFilter(Point screenPos, SimpleFeatureSource featureSource) {
        String geometryAttributeName = featureSource.getSchema().getGeometryDescriptor().getLocalName();

        Rectangle screenRect = new Rectangle(screenPos.x - 2, screenPos.y - 2, 5, 5);

        AffineTransform screenToWorld = getMapPane().getScreenToWorldTransform();
        Rectangle2D worldRect = screenToWorld.createTransformedShape(screenRect).getBounds2D();
        ReferencedEnvelope bbox = new ReferencedEnvelope(worldRect, getMapPane().getMapContent().getCoordinateReferenceSystem());

        Filter filter = ff.bbox(ff.property(geometryAttributeName), bbox);
        
        return filter;
    }

    @Override
    public void onMouseClicked(MapMouseEvent ev) {
        MapContent content = getMapPane().getMapContent();
        for (Layer layer : content.layers()) {
            if (layer.isSelected()) {
                SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
                Filter filter = createClickFilter(ev.getPoint(), featureSource);
                selectFeatures(filter, layer);
            }
        }
    }
    
    @Override
    public void onMousePressed(MapMouseEvent ev) {
        startPoint.setLocation(ev.getPoint());
        startGeomPoint.setLocation(ev.getWorldPos());
    }

    @Override
    public void onMouseDragged(MapMouseEvent ev) {
        dragged = true;
    }

    @Override
    public void onMouseReleased(MapMouseEvent ev) {
        if (dragged && !ev.getPoint().equals(startPoint)) {
            Envelope2D env = new Envelope2D();
            env.setFrameFromDiagonal(startGeomPoint, ev.getWorldPos());
            dragged = false;

            MapContent content = getMapPane().getMapContent();
            for (Layer layer : content.layers()) {
                if (layer.isSelected()) {
                    SimpleFeatureSource featureSource = (SimpleFeatureSource) layer.getFeatureSource();
                    Filter filter = createDragFilter(env, featureSource);

                    selectFeatures(filter, layer);
                }
            }
        }
    }

    private Filter createDragFilter(Envelope2D env, SimpleFeatureSource featureSource) {
        String geometryAttributeName = featureSource.getSchema().getGeometryDescriptor().getLocalName();
        ReferencedEnvelope bbox = new ReferencedEnvelope(env, getMapPane().getMapContent().getCoordinateReferenceSystem());
        
        Filter filter = ff.bbox(ff.property(geometryAttributeName), bbox);
        return filter;
    }

    public boolean drawDragBox() {
        return true;
    }

    public void selectFeatures(Layer layer, HashSet<Identifier> selectedIds) {
        getSelectedLayerFeatureSet(layer).clear();

        Filter filter = ff.id(selectedIds);
        selectFeatures(filter, layer);
    }
    
    public Filter getSelectedFeatures(Layer layer) {

        Filter filter = ff.id(selectedFeatureIds.get(layer));
        
        return filter;
    }
    
    public HashMap<Layer, HashSet<Identifier>> getSelectedFeatureIds() {
            return selectedFeatureIds;
    }
    
}
