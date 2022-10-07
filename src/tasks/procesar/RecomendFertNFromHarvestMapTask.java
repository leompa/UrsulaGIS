package tasks.procesar;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

import dao.config.Cultivo;
import dao.config.Fertilizante;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.fertilizacion.FertilizacionItem;
import dao.fertilizacion.FertilizacionLabor;
import dao.suelo.Suelo;
import dao.suelo.SueloItem;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import tasks.crear.CrearFertilizacionMapTask;
import utils.GeometryHelper;
import utils.PolygonValidator;
import utils.ProyectionConstants;

public class RecomendFertNFromHarvestMapTask extends ProcessMapTask<FertilizacionItem,FertilizacionLabor> {
	private CosechaLabor cosecha;
	private List<Suelo> suelos;
	private List<FertilizacionLabor> fertilizaciones;
	private Double minFert=null;
	private Double maxFert=null;

	public RecomendFertNFromHarvestMapTask(FertilizacionLabor labor,CosechaLabor cosechaEstimada, List<Suelo> _suelos, List<FertilizacionLabor> _fert) {
		super(labor);
		this.cosecha =cosechaEstimada;
		this.suelos=_suelos;
		this.fertilizaciones=_fert;
	}

	public void doProcess() {	
		try {
			featureCount = cosecha.outCollection.size();
			//List<FertilizacionItem> itemsToShow = new ArrayList<FertilizacionItem>();
			Cultivo cultivo = cosecha.getCultivo();
			Fertilizante fert = this.labor.fertilizanteProperty.getValue();

			//TODO calcular el balance de nitrogeno en el suelo luego de la fertilizacion 
			//y completar lo que haga falta para la cosecha estimada;
			List<CosechaItem> cItems = new ArrayList<CosechaItem>();
			FeatureReader<SimpleFeatureType, SimpleFeature> reader =cosecha.outCollection.reader();
			while (reader.hasNext()) {
				SimpleFeature simpleFeature = reader.next();
				CosechaItem ci = cosecha.constructFeatureContainerStandar(simpleFeature,false);
				cItems.add(ci);
			}
			reader.close();
			ForkJoinPool myPool = new ForkJoinPool(4);
			myPool.submit(() ->{

				cItems.parallelStream().forEach(cItem->{
					try {
						FertilizacionItem fi = new FertilizacionItem();			
						//synchronized(labor){							
						fi.setId(labor.getNextID());
						labor.setPropiedadesLabor(fi);
						//}
						Geometry geom = PolygonValidator.validate(cItem.getGeometry());
						if(geom==null) {
							System.out.println("item geom es null");
							return;
						}
						Double areaGeom =  ProyectionConstants.A_HAS(geom.getArea());
						fi.setGeometry(geom);
						double absN = cItem.getRindeTnHa()*cultivo.getAbsN();
						double dispNSuelo = getNDisponibleSuelo(geom);//incluye el organico a mineralizar.

						//System.out.println("kg disponible suelo: " + dispNSuelo+ "areaGeom:"+areaGeom);
						dispNSuelo = dispNSuelo / areaGeom;

						double dispNFert = getNDisponibleFert(geom) / areaGeom;
						//System.out.println("absN="+absN+" dispSuelo="+dispNSuelo+" dispNFert="+dispNFert);
						double nAAplicar= absN-dispNSuelo-dispNFert;


						nAAplicar = Math.max(0, nAAplicar);
						double reposicionN = nAAplicar / (fert.getPorcN()/100);

						//	System.out.println("Rinde "+cItem.getAmount()+" Nreq "+absN+" nAAplicar="+nAAplicar+" "+fert.getNombre()+" "+reposicionN);

						if(this.minFert != null && this.minFert>0 &&this.minFert > reposicionN) {
							reposicionN = minFert;
						}
						if(this.maxFert != null && this.maxFert>0 && this.maxFert < reposicionN) {
							reposicionN = maxFert;
						}
						fi.setDosistHa(reposicionN);
						fi.setElevacion(10d);
						labor.setPropiedadesLabor(fi);
						//segun el cultivo de la cosecha

						//System.out.println("insertando "+fi);
						labor.insertFeature(fi);
						//itemsToShow.add(fi);
						featureNumber++;
						//System.out.println("inserte la feature "+featureNumber+" "+fi);
						updateProgress(featureNumber, featureCount);
					}catch(Exception e) {
						System.err.println("error al procesar el item "+cItem);
						e.printStackTrace();
					}
				});
			}
			//list.parallelStream().forEach(/* Do Something */);
					).get();
			System.out.println("construyendo clasificador");
			labor.constructClasificador();
			runLater(getItemsList());
			updateProgress(0, featureCount);	
		}catch(Exception e) {
			e.printStackTrace();
		}
	}


	private Double getNDisponibleFert(Geometry geometry) {
		Double kgNFert = new Double(0);
		kgNFert=	fertilizaciones.parallelStream().flatMapToDouble(fertilizacion->{
			List<FertilizacionItem> items = fertilizacion.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			Fertilizante fertilizante = fertilizacion.fertilizanteProperty.getValue();
			return items.parallelStream().flatMapToDouble(item->{
				Double kgNHa = (Double) item.getDosistHa() * fertilizante.getPorcN()/100;
				Geometry fertGeom = item.getGeometry();				
				fertGeom = PolygonValidator.validate(fertGeom);//puede ser null
				if(fertGeom == null || !fertGeom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(fertGeom, geometry);
					//Geometry inteseccionGeom = geometry.intersection(fertGeom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				return DoubleStream.of( kgNHa * area);				
			});
		}).sum();
		//			System.out.println("la cantidad de ppmP acumulado en el suelo es = "
		//					+ kPFert);

		return kgNFert;
	}

	private Double getNDisponibleSuelo(Geometry geometry) {
		Double kgNSuelo = new Double(0);
		boolean estival = this.cosecha.getCultivo().isEstival();
		kgNSuelo=	suelos.stream().flatMapToDouble(suelo->{
			List<SueloItem> items = suelo.cachedOutStoreQuery(geometry.getEnvelopeInternal());
			//System.out.println("hay "+items.size()+" items en el suelo "+suelo.nombre);
			return items.parallelStream().flatMapToDouble(item->{
				//Double kgNHa= (Double) item.getPpmN()*suelo.getDensidad();
				//	double kgSueloHa = ProyectionConstants.METROS2_POR_HA*0.6*suelo.getDensidad();
				Double kgNHa=suelo.getKgNHa(item);// (Double) item.getPpmN()*kgSueloHa*Fertilizante.porcN_NO3/1000000;
				double porcOrgDisponible = (1d/3);// esto era 0.0 porque estaba inicializado con la division entera de 1/3 que es 0. //que feo!
				if(estival)porcOrgDisponible=(2d/3);
				Double kgNOrganicoHa= suelo.getKgNOrganicoHa(item)*porcOrgDisponible;//divido la mineralizacion anual a la mitad. pero depende del cultivo

				Geometry geom = item.getGeometry();				
				geom = PolygonValidator.validate(geom);
				if(!geom.intersects(geometry))return DoubleStream.of(0.0);
				Double area = 0.0;
				try {
					//XXX posible punto de error/ exceso de demora/ inneficicencia
					Geometry inteseccionGeom = GeometryHelper.getIntersection(geom, geometry);
					//Geometry inteseccionGeom = geometry.intersection(geom);// Computes a
					area = ProyectionConstants.A_HAS(inteseccionGeom.getArea());
					// Geometry
				} catch (Exception e) {
					e.printStackTrace();
				}				
				//System.out.println("los kgN/ha para ppmN: "+item.getPpmN()+" es "+kgNHa+ " has es " +area);
				return DoubleStream.of( (kgNHa +kgNOrganicoHa)* area);				
			});
		}).sum();
		//					System.out
		//					.println("la cantidad de kgNSuelo acumulado en el suelo es = "
		//							+ kgNSuelo+" en "+ ProyectionConstants.A_HAS(geometry.getArea()));

		return kgNSuelo;
	}

	@Override
	protected ExtrudedPolygon getPathTooltip(Geometry poly, FertilizacionItem fertFeature,ExtrudedPolygon  renderablePolygon) {
		double area = poly.getArea() * ProyectionConstants.A_HAS();// 30224432.818;//pathBounds2.getHeight()*pathBounds2.getWidth();
		String tooltipText = CrearFertilizacionMapTask.builTooltipText(fertFeature, area); 
		return super.getExtrudedPolygonFromGeom(poly, fertFeature,tooltipText,renderablePolygon);
	}

	protected int getAmountMin() {
		return 0;
	}

	protected int gerAmountMax() {
		return 1000;
	}

	public void setMinFert(Double _minFert) {
		this.minFert=_minFert;

	}
	public void setMaxFert(Double _maxFert) {
		this.maxFert=_maxFert;

	}
}
