package org.geotools.gis;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryTypeImpl;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Delegate that will compute the intersections on the go
 */
class IntersectedFeatureCollection extends DecoratingSimpleFeatureCollection {

    SimpleFeatureCollection features;
    SimpleFeatureType type;

    public IntersectedFeatureCollection(SimpleFeatureCollection delegate,
                                        SimpleFeatureCollection features) {
        super(delegate);
        this.features = features;

    }

    public SimpleFeatureIterator features() {
        return new IntersectedFeatureIterator(delegate.features(), delegate, features, delegate.getSchema(), features.getSchema());
    }

    public Iterator<SimpleFeature> iterator() {
        return new WrappingIterator(features());
    }

    public void close(Iterator<SimpleFeature> close) {
        if (close instanceof WrappingIterator) {
            ((WrappingIterator) close).close();
        }
    }

    public SimpleFeatureType getSchema() {
        return type;
    }
}

/**
 * Builds the intersections while streaming
 */
class IntersectedFeatureIterator implements SimpleFeatureIterator {

    SimpleFeatureIterator delegate;
    SimpleFeatureCollection firstFeatures;
    SimpleFeatureCollection secondFeatures;
    SimpleFeatureCollection subFeatureCollection;
    SimpleFeatureBuilder fb;
    SimpleFeature next;
    SimpleFeature first;
    Integer iterationIndex = 0;
    boolean complete = true;
    boolean added = false;
    SimpleFeatureCollection intersectedGeometries;
    SimpleFeatureIterator iterator;
    String dataGeomName;
    SimpleFeatureType intersectedType;

    public IntersectedFeatureIterator(SimpleFeatureIterator delegate,
                                      SimpleFeatureCollection firstFeatures, SimpleFeatureCollection secondFeatures,
                                      SimpleFeatureType firstFeatureCollectionSchema,
                                      SimpleFeatureType secondFeatureCollectionSchema) {
        this.delegate = delegate;
        this.firstFeatures = firstFeatures;
        this.secondFeatures = secondFeatures;
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        GeometryDescriptor geometryDescriptor = firstFeatureCollectionSchema.getGeometryDescriptor();
        // gather the attributes from the first feature collection
        for (AttributeDescriptor descriptor : firstFeatureCollectionSchema.getAttributeDescriptors()) {
            if (!(descriptor.getType() instanceof GeometryTypeImpl)
                    || (!geometryDescriptor.getName().equals(descriptor.getName()))) {
                AttributeTypeBuilder builder = new AttributeTypeBuilder();
                builder.setName(this.firstFeatures.features().next().getFeatureType().getName().getLocalPart()
                        + "_" + descriptor.getName());
                builder.setNillable(descriptor.isNillable());
                builder.setBinding(descriptor.getType().getBinding());
                builder.setMinOccurs(descriptor.getMinOccurs());
                builder.setMaxOccurs(descriptor.getMaxOccurs());
                builder.setDefaultValue(descriptor.getDefaultValue());
                builder.setCRS(this.firstFeatures.features().next().getFeatureType().getCoordinateReferenceSystem());
                AttributeDescriptor intersectionDescriptor = builder.buildDescriptor(
                        this.firstFeatures.features().next().getFeatureType().getName().getLocalPart()
                                + "_" + descriptor.getName(), descriptor.getType());
                tb.add(intersectionDescriptor);
                // System.out.println(intersectionDescriptor);
                tb.addBinding(descriptor.getType());
            } else {
                tb.add(descriptor);
                // System.out.println(descriptor);
            }

        }
        // gather the attributes from the second feature collection
        geometryDescriptor = secondFeatureCollectionSchema.getGeometryDescriptor();
        for (AttributeDescriptor descriptor : secondFeatureCollectionSchema.getAttributeDescriptors()) {
            if (!(descriptor.getType() instanceof GeometryTypeImpl)
                    || (!geometryDescriptor.getName().equals(descriptor.getName()))) {
                AttributeTypeBuilder builder = new AttributeTypeBuilder();
                builder.setName(this.secondFeatures.features().next().getFeatureType().getName().getLocalPart()
                        + "_" + descriptor.getName());
                builder.setNillable(descriptor.isNillable());
                builder.setBinding(descriptor.getType().getBinding());
                builder.setMinOccurs(descriptor.getMinOccurs());
                builder.setMaxOccurs(descriptor.getMaxOccurs());
                builder.setDefaultValue(descriptor.getDefaultValue());
                builder.setCRS(this.secondFeatures.features().next().getFeatureType().getCoordinateReferenceSystem());
                builder.setNamespaceURI(this.secondFeatures.features().next().getFeatureType().getName().getNamespaceURI());
                builder.setDefaultValue(descriptor.getDefaultValue());
                AttributeDescriptor intersectionDescriptor = builder.buildDescriptor(
                        this.secondFeatures.features().next().getFeatureType().getName().getLocalPart()
                                + "_" + descriptor.getName(), descriptor.getType());
                tb.addBinding(descriptor.getType());
                tb.add(intersectionDescriptor);
                //  System.out.println(intersectionDescriptor);
            }
        }
        tb.setDescription(firstFeatureCollectionSchema.getDescription());
        tb.setCRS(firstFeatureCollectionSchema.getCoordinateReferenceSystem());
        tb.setAbstract(firstFeatureCollectionSchema.isAbstract());
        tb.setSuperType((SimpleFeatureType) firstFeatureCollectionSchema.getSuper());
        tb.setName(firstFeatureCollectionSchema.getName());

        this.fb = new SimpleFeatureBuilder(tb.buildFeatureType());

        subFeatureCollection = this.secondFeatures;

        this.dataGeomName = this.firstFeatures.getSchema().getGeometryDescriptor().getLocalName();
    }

    public void close() {
        delegate.close();
    }

    public boolean hasNext() {
        while ((next == null && delegate.hasNext()) || (next == null && added)) {
            if (complete) {
                first = delegate.next();
                intersectedGeometries = null;
            }
            for (Object attribute : first.getAttributes()) {
                if (attribute instanceof Geometry
                        && attribute.equals(first.getDefaultGeometry())) {
                    Geometry currentGeom = (Geometry) attribute;
                    if (intersectedGeometries == null && !added) {
                        intersectedGeometries = filteredCollection(currentGeom,
                                subFeatureCollection);
                        iterator = intersectedGeometries.features();
                    }
                    try {
                        while (iterator.hasNext()) {
                            added = false;
                            SimpleFeature second = iterator.next();
                            if (currentGeom.getEnvelope().intersects(
                                    ((Geometry) second.getDefaultGeometry()))) {
                                try {
                                    attribute = currentGeom.intersection((Geometry) second.getDefaultGeometry());
                                } catch (Throwable t) {
                                    //t.printStackTrace();
                                    continue;
                                }
                                if (((Geometry) attribute).getNumGeometries() > 0) {
                                    fb.add(attribute);

                                    // add first feature's attributes
                                    for (Object firstAttribute : first.getAttributes()) {
                                        if (!(firstAttribute instanceof Geometry)) {
                                            fb.add(firstAttribute);
                                        }
                                    }
                                    // add second feature's attributes
                                    for (Object secondAttribute : second.getAttributes()) {
                                        if (!(secondAttribute instanceof Geometry)) {
                                            fb.add(secondAttribute);
                                        }
                                    }
                                    next = fb.buildFeature(iterationIndex.toString());
                                    if (iterator.hasNext()) {
                                        complete = false;
                                        added = true;
                                        iterationIndex++;
                                        return next != null;
                                    }
                                    iterationIndex++;
                                }
                            }
                            complete = false;
                        }
                        complete = true;
                    } finally {
                        if (!added) {
                            iterator.close();
                        }
                    }
                }
            }
        }
        return next != null;
    }

    public SimpleFeature next() throws NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException("hasNext() returned false!");
        }

        SimpleFeature result = next;
        next = null;
        return result;
    }

    private SimpleFeatureCollection filteredCollection(Geometry currentGeom,
                                                       SimpleFeatureCollection subFeatureCollection) {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter intersectFilter = ff.intersects(ff.property(dataGeomName), ff.literal(currentGeom));
        SimpleFeatureCollection subFeatureCollectionIntersection = this.subFeatureCollection.subCollection(intersectFilter);
        if (subFeatureCollectionIntersection.size() == 0) {
            subFeatureCollectionIntersection = subFeatureCollection;
        }
        return subFeatureCollectionIntersection;
    }

    public SimpleFeatureType getSchema() {
        return intersectedType;
    }
}
