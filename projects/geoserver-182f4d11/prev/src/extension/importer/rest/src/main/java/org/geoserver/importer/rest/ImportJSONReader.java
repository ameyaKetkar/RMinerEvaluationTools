/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geoserver.importer.Archive;
import org.geoserver.importer.Database;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.UpdateMode;
import org.geoserver.importer.ImportContext.State;
import org.geoserver.importer.ValidationException;
import org.geoserver.importer.mosaic.Mosaic;
import org.geoserver.importer.mosaic.TimeMode;
import org.geoserver.importer.transform.AttributeRemapTransform;
import org.geoserver.importer.transform.AttributesToPointGeometryTransform;
import org.geoserver.importer.transform.CreateIndexTransform;
import org.geoserver.importer.transform.DateFormatTransform;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.IntegerFieldToDateTransform;
import org.geoserver.importer.transform.RasterTransformChain;
import org.geoserver.importer.transform.ReprojectTransform;
import org.geoserver.importer.transform.TransformChain;
import org.geoserver.importer.transform.VectorTransformChain;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ImportJSONReader {

    Importer importer;
    JSONObject json;

    public ImportJSONReader(Importer importer, String in) throws IOException {
        this(importer, new ByteArrayInputStream(in.getBytes()));
    }

    public ImportJSONReader(Importer importer, InputStream in) throws IOException {
        this.importer = importer;
        json = parse(in);
    }

    public ImportJSONReader(Importer importer, JSONObject obj) {
        this.importer = importer;
        json = obj;
    }

    public JSONObject object() {
        return json;
    }

    public ImportContext context() throws IOException {
        ImportContext context = null;
        if (json.has("import")) {
            context = new ImportContext();
            
            json = json.getJSONObject("import");
            if (json.has("id")) {
                context.setId(json.getLong("id"));
            }
            if (json.has("state")) {
                context.setState(State.valueOf(json.getString("state")));
            }
            if (json.has("user")) {
                context.setUser(json.getString("user"));
            }
            if (json.has("archive")) {
                context.setArchive(json.getBoolean("archive"));
            }
            if (json.has("targetWorkspace")) {
                context.setTargetWorkspace(
                    fromJSON(json.getJSONObject("targetWorkspace"), WorkspaceInfo.class));
            }
            if (json.has("targetStore")) {
                context.setTargetStore(
                    fromJSON(json.getJSONObject("targetStore"), StoreInfo.class));
            }
            if (json.has("data")) {
                context.setData(data(json.getJSONObject("data")));
            }
        }
        return context;
    }

    public LayerInfo layer() throws IOException {
        return layer(json);
    }

    LayerInfo layer(JSONObject json) throws IOException {
        CatalogFactory f = importer.getCatalog().getFactory();

        if (json.has("layer")) {
            json = json.getJSONObject("layer");
        }

        //TODO: what about coverages?
        ResourceInfo r = f.createFeatureType();
        if (json.has("name")) {
            r.setName(json.getString("name"));
        }
        if (json.has("nativeName")) {
            r.setNativeName(json.getString("nativeName"));
        }
        if (json.has("srs")) {
            r.setSRS(json.getString("srs"));
            try {
                r.setNativeCRS(CRS.decode(json.getString("srs")));
            }
            catch(Exception e) {
                //should fail later
            }
            
        }
        if (json.has("bbox")) {
            r.setNativeBoundingBox(bbox(json.getJSONObject("bbox")));
        }
        if (json.has("title")) {
            r.setTitle(json.getString("title"));
        }
        if (json.has("abstract")) {
            r.setAbstract(json.getString("abstract"));
        }
        if (json.has("description")) {
            r.setDescription(json.getString("description"));
        }

        LayerInfo l = f.createLayer();
        l.setResource(r);
        //l.setName(); don't need to this, layer.name just forwards to name of underlying resource
        
        if (json.has("style")) {
            JSONObject sobj = new JSONObject();
            sobj.put("defaultStyle", json.get("style"));

            JSONObject lobj = new JSONObject();
            lobj.put("layer", sobj);

            LayerInfo tmp = fromJSON(lobj, LayerInfo.class);
            if (tmp.getDefaultStyle() != null) {
                l.setDefaultStyle(tmp.getDefaultStyle());
            }
            else {
                sobj = new JSONObject();
                sobj.put("style", json.get("style"));
                
                l.setDefaultStyle(fromJSON(sobj, StyleInfo.class));
            }

        }
        return l;
    }

    public ImportTask task() throws IOException {

        if (json.has("task")) {
            json =  json.getJSONObject("task");
        }

        ImportTask task = new ImportTask();

        if (json.has("id")) {
            task.setId(json.getInt("id"));
        }
        if (json.has("updateMode")) {
            task.setUpdateMode(UpdateMode.valueOf(json.getString("updateMode").toUpperCase()));
        } else {
            // if it hasn't been specified by the request, set this to null
            // or else it's possible to overwrite an existing setting
            task.setUpdateMode(null);
        }

        JSONObject data = null;
        if (json.has("data")) {
            data = json.getJSONObject("data");
        }
        else if (json.has("source")) { // backward compatible check for source
            data = json.getJSONObject("source");
        }

        if (data != null) {
            // we only support updating the charset
            if (data.has("charset")) {
                if (task.getData() == null) {
                    task.setData(new ImportData.TransferObject());
                }
                task.getData().setCharsetEncoding(data.getString("charset"));
            }
        }
        if (json.has("target")) {
            task.setStore(fromJSON(json.getJSONObject("target"), StoreInfo.class));
        }

        LayerInfo layer = null; 
        if (json.has("layer")) {
            layer = layer(json.getJSONObject("layer"));
        } else {
            layer = importer.getCatalog().getFactory().createLayer();
        }
        task.setLayer(layer);

        if (json.has("transformChain")) {
            task.setTransform(transformChain(json.getJSONObject("transformChain")));
        }

        return task;
    }

    TransformChain transformChain(JSONObject json) throws IOException {
        String type = json.getString("type");
        TransformChain chain = null;
        if ("vector".equalsIgnoreCase(type) || "VectorTransformChain".equalsIgnoreCase(type)) {
            chain = new VectorTransformChain();
        } else if ("raster".equalsIgnoreCase(type) || "RasterTransformChain".equalsIgnoreCase(type)) {
            chain = new RasterTransformChain();
        } else {
            throw new IOException("Unable to parse transformChain of type " + type);
        }
        JSONArray transforms = json.getJSONArray("transforms");
        for (int i = 0; i < transforms.size(); i++) {
            chain.add(transform(transforms.getJSONObject(i)));
        }
        return chain;
    }

    public ImportTransform transform() throws IOException {
        return transform(json);
    }

    ImportTransform transform(JSONObject json) throws IOException {
        ImportTransform transform;
        String type = json.getString("type");
        if ("DateFormatTransform".equalsIgnoreCase(type)) {
            transform = new DateFormatTransform(json.getString("field"), json.optString("format", null));
        } else if ("IntegerFieldToDateTransform".equalsIgnoreCase(type)) {
            transform = new IntegerFieldToDateTransform(json.getString("field"));
        } else if ("CreateIndexTransform".equalsIgnoreCase(type)) {
            transform = new CreateIndexTransform(json.getString("field"));
        } else if ("AttributeRemapTransform".equalsIgnoreCase(type)) {
            Class clazz;
            try {
                clazz = Class.forName( json.getString("target") );
            } catch (ClassNotFoundException cnfe) {
                throw new ValidationException("unable to locate target class " + json.getString("target"));
            }
            transform = new AttributeRemapTransform(json.getString("field"), clazz);
        } else if ("AttributesToPointGeometryTransform".equalsIgnoreCase(type)) {
            String latField = json.getString("latField");
            String lngField = json.getString("lngField");
            transform = new AttributesToPointGeometryTransform(latField, lngField);
        } else if ("ReprojectTransform".equalsIgnoreCase(type)){
            CoordinateReferenceSystem source = json.has("source") ? crs(json.getString("source")) : null;
            CoordinateReferenceSystem target = json.has("target") ? crs(json.getString("target")) : null;

            try {
                transform = new ReprojectTransform(source, target);
            } 
            catch(Exception e) {
                throw new ValidationException("Error parsing reproject transform", e);
            }
        } else {
            throw new ValidationException("Invalid transform type '" + type + "'");
        }
        return transform;
    }

    public ImportData data() throws IOException {
        return data(json);
    }

    ImportData data(JSONObject json) throws IOException {
        String type = json.getString("type");
        if (type == null) {
            throw new IOException("Data object must specify 'type' property");
        }

        if ("file".equalsIgnoreCase(type)) {
            return file(json);
        }
        else if("directory".equalsIgnoreCase(type)) {
            return directory(json);
        }
        else if("mosaic".equalsIgnoreCase(type)) {
            return mosaic(json);
        }
        else if("archive".equalsIgnoreCase(type)) {
            return archive(json);
        }
        else if ("database".equalsIgnoreCase(type)) {
            return database(json);
        }
        else {
            throw new IllegalArgumentException("Unknown data type: " + type);
        }
    }

    FileData file(JSONObject json) throws IOException {
        if (json.has("file")) {
            //TODO: find out if spatial or not
            String file = json.getString("file");
            return FileData.createFromFile(new File(file));
            //return new FileData(new File(file));
        }
        else {
            //TODO: create a temp file
            return new FileData((File)null);
        }
    }

    Mosaic mosaic(JSONObject json) throws IOException {
        Mosaic m = new Mosaic(json.has("location") ?  new File(json.getString("location")) : 
            Directory.createNew(importer.getUploadRoot()).getFile());
        if (json.has("name")) {
            m.setName(json.getString("name"));
        }
        if (json.containsKey("time")) {
            JSONObject time = json.getJSONObject("time");
            if (!time.containsKey("mode")) {
                throw new IllegalArgumentException("time object must specific mode property as " +
                    "one of " + TimeMode.values());
            }

            m.setTimeMode(TimeMode.valueOf(time.getString("mode").toUpperCase()));
            m.getTimeHandler().init(time);
        }
        return m;
    }

    Archive archive(JSONObject json) throws IOException {
        throw new UnsupportedOperationException("TODO: implement");
    }

    public Directory directory() throws IOException {
        return directory(json);
    }

    Directory directory(JSONObject json) throws IOException {
        if (json.has("location")) {
            return new Directory(new File(json.getString("location")));
        }
        else {
            return Directory.createNew(importer.getUploadRoot());
        }
    }

    Database database(JSONObject json) throws IOException {
        throw new UnsupportedOperationException("TODO: implement");
    }
    
    ReferencedEnvelope bbox(JSONObject json) {
        CoordinateReferenceSystem crs = null;
        if (json.has("crs")) {
            crs = (CoordinateReferenceSystem) 
                new XStreamPersister.CRSConverter().fromString(json.getString("crs"));
        }

        return new ReferencedEnvelope(json.getDouble("minx"), json.getDouble("maxx"), 
            json.getDouble("miny"), json.getDouble("maxy"), crs);
    }

    CoordinateReferenceSystem crs(String srs) {
        try {
            return CRS.decode(srs);
        } catch (Exception e) {
            throw new RuntimeException("Failing parsing srs: " + srs, e);
        }
    }

    JSONObject parse(InputStream in) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        IOUtils.copy(in, bout);
        return JSONObject.fromObject(new String(bout.toByteArray()));
    }

    Object read(InputStream in) throws IOException {
        Object result = null;
        JSONObject json = parse(in);
        // @hack - this should return a ImportTask
        if (json.containsKey("target")) {
            result = fromJSON(json.getJSONObject("target"), DataStoreInfo.class);
        }
        return result;
    }

    <T> T fromJSON(JSONObject json, Class<T> clazz) throws IOException {
        XStreamPersister xp = importer.createXStreamPersisterJSON();
        return (T) xp.load(new ByteArrayInputStream(json.toString().getBytes()), clazz);
    }

    <T> T fromJSON(Class<T> clazz) throws IOException {
        return fromJSON(json, clazz);
    }
}
