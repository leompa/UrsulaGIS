package tasks.crear;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.precision.EnhancedPrecisionOp;

import dao.Labor;
import dao.LaborItem;
import dao.Poligono;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraItem;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import javafx.geometry.Point2D;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class CrearFertilizacionMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	Double amount = new Double(0);
	List<Poligono> polis=null;

	public CrearFertilizacionMapTask(FertilizacionLabor labor,List<Poligono> _polis,Double _amount){//RenderableLayer layer, FileDataStore store, double d, Double correccionRinde) {
		super(labor);
		amount=_amount;
		polis=_polis;

	}

	public void doProcess() throws IOException {
		
		//GeometryFactory fact = new GeometryFactory();
		for(Poligono pol : this.polis) {
			FertilizacionItem ci = new FertilizacionItem();
			ci.setDosistHa(amount);
//			ci.setPrecioInsumo(labor.precioInsumoProperty.get());
//			ci.setCostoLaborHa(labor.precioLaborProperty.get());
			labor.setPropiedadesLabor(ci);
			//dosis sembradora va en semillas cada 10mts
			//dosis valorizacion va en unidad de compra; kg o bolsas de 80000 semillas o 50kg

			ci.setGeometry(pol.toGeometry());
			ci.setId(labor.getNextID());
			
			//labor.setNombre(poli.getNombre());
			labor.insertFeature(ci);
		}		
		
		

				
		labor.constructClasificador();

		
		runLater(this.getItemsList());
		updateProgress(0, featureCount);

	}


	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {

		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		//double area2 = cosechaFeature.getAncho()*cosechaFeature.getDistancia();
		DecimalFormat df = new DecimalFormat("0.00");//$NON-NLS-2$

		String tooltipText = new String(// TODO ver si se puede instalar un
				// boton
				// que permita editar el dato
				Messages.getString("ProcessFertMapTask.2") + df.format(fertFeature.getDosistHa()) //$NON-NLS-1$
				+ Messages.getString("ProcessFertMapTask.3") + Messages.getString("ProcessFertMapTask.4") //$NON-NLS-1$ //$NON-NLS-2$
				+ df.format(fertFeature.getImporteHa()) + Messages.getString("ProcessFertMapTask.5") //$NON-NLS-1$
				//+ "Sup: "
				//+ df.format(area * ProyectionConstants.METROS2_POR_HA)
				//+ " m2\n"
				// +"feature: " + featureNumber
				);
		if(area<1){
			tooltipText=tooltipText.concat( Messages.getString("ProcessFertMapTask.6")+df.format(area * ProyectionConstants.METROS2_POR_HA) + Messages.getString("ProcessFertMapTask.7")); //$NON-NLS-1$ //$NON-NLS-2$
			//	tooltipText=tooltipText.concat( "SupOrig: "+df.format(area2 ) + "m2\n");
		} else {
			tooltipText=tooltipText.concat(Messages.getString("ProcessFertMapTask.8")+df.format(area ) + Messages.getString("ProcessFertMapTask.9")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		//List  paths = 
		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);

		//return null;
	}

	protected int getAmountMin() {
		return 3;
	}

	protected int gerAmountMax() {
		return 15;
	}
}// fin del task