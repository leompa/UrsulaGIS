package tasks.importar;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.geotools.data.FeatureReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import dao.siembra.SiembraConfig;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gui.Messages;
import tasks.ProcessMapTask;
import utils.ProyectionConstants;

public class ProcessSiembraMapTask extends ProcessMapTask<SiembraItem,SiembraLabor> {	
	//	private int featureCount;
	//	private int featureNumber;

	//	private FileDataStore store = null;
	//	Quadtree featureTree = null;

	private Double precioPasada;
	private Double precioBolsaSemilla;

	//ArrayList<ArrayList<Object>> pathTooltips = new ArrayList<ArrayList<Object>>();

	//public Group map = new Group();

	public ProcessSiembraMapTask(SiembraLabor siembra) {
		super(siembra);
		//		this.layer=map1;
		//		this.store = store;
		//		
		//		precioPasada = precioPasada1;
		//		this.precioBolsaSemilla = precioBolsaSemilla;

	}


	public void doProcess() throws IOException {
		//		System.out.println("doProcess(); "+System.currentTimeMillis());

		FeatureReader<SimpleFeatureType, SimpleFeature> reader =null;
		//	CoordinateReferenceSystem storeCRS =null;
		if(labor.getInStore()!=null){
			if(labor.outCollection!=null)labor.outCollection.clear();
			reader = labor.getInStore().getFeatureReader();
			//		 storeCRS = labor.getInStore().getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.getInStore().getFeatureSource().getFeatures().size();
		} else{//XXX cuando es una grilla los datos estan en outstore y instore es null
			reader = labor.outCollection.reader();
			//	 storeCRS = labor.outCollection.getSchema().getCoordinateReferenceSystem();
			//convierto los features en cosechas
			featureCount=labor.outCollection.size();
		}


		//initCrsTransform(storeCRS);

		int divisor = 1;

		SiembraConfig.Unidad dosisUnit = labor.getConfiguracion().dosisUnitProperty().get();
		Function<Double,Double> dosisToKgHa = (dosis)->dosis*2;

		switch (dosisUnit) {
		case kgHa: 
			dosisToKgHa = (dosis)->dosis;
			break;
		case milPlaHa:
			dosisToKgHa = (milPlHa)->{
				//convertir de miles de semillas por Ha a kg de semilla por Ha
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return (milPlHa)*labor.getSemilla().getPesoDeMil()/1000;
			};
			break;
		case pla10MtLineal:
			dosisToKgHa = (pla10MtLineal)->{
				//convertir de semillas cada 10metros lineales a kg de semilla por Ha
				//1) divido por 10 para obtener semillas por metro lineal
				double plML= pla10MtLineal/10;
				double plm2= plML/labor.getEntreSurco();
				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;
		case pla1MtLineal:
			dosisToKgHa = (pla1MtLineal)->{
				double plML= pla1MtLineal;
				double plm2= plML/labor.getEntreSurco();
				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;
		case plaMetroCuadrado:
			dosisToKgHa = (plm2)->{
				//convertir de semillas cada metro cuadrado a kg de semilla por Ha
				//1) divido por 10 para obtener semillas por metro lineal

				double plHa = plm2*ProyectionConstants.METROS2_POR_HA;
				double gramHa = plHa*labor.getSemilla().getPesoDeMil()/1000;
				double kgHa= gramHa/1000;
				//miltiplico por el peso en gramos de mil semillas y lo divido por mil para convertir a kg
				return kgHa;
			};
			break;

		}

		while (reader.hasNext()) {
			SimpleFeature simpleFeature = reader.next();
			SiembraItem si = labor.constructFeatureContainer(simpleFeature);
			//System.out.println("dosis antes "+si.getDosisHa());//dosis antes 3.9999001026153564
			si.setDosisHa(dosisToKgHa.apply(si.getDosisHa()));
		//	System.out.println("dosis despues "+si.getDosisHa());//dosis despues 24.614769862248348
			Double kgM2 = si.getDosisHa()/ProyectionConstants.METROS2_POR_HA;//kg/m2
			double semM2= (1000*1000*kgM2)/labor.getSemilla().getPesoDeMil();//sem/m2
			si.setDosisML(semM2*labor.getEntreSurco());// 1/entresurco=ml/m2 => sem/m2
			//System.out.println("dosisML despues "+si.getDosisML());//dosisML despues 3.9999001026153564
			featureNumber++;

			updateProgress(featureNumber/divisor, featureCount);
			Object geometry = si.getGeometry();

			/**
			 * si la geometria es un point procedo a poligonizarla
			 */
			if (geometry instanceof Point) {
				//					Point longLatPoint = (Point) geometry;
				//
				//				
				//					if(	lastX!=null && labor.getConfiguracion().correccionDistanciaEnabled()){
				//						
				//						double aMetros=1;// 1/ProyectionConstants.metersToLongLat;
				//					//	BigDecimal x = new BigDecimal();
				//						double deltaY = longLatPoint.getY()*aMetros-lastX.getY()*aMetros;
				//						double deltaX = longLatPoint.getX()*aMetros-lastX.getX()*aMetros;
				//						if(deltaY==0.0 && deltaX ==0.0|| lastX.equals(longLatPoint)){
				//							puntosEliminados++;
				//						//	System.out.println("salteando el punto "+longLatPoint+" porque tiene la misma posicion que el punto anterior "+lastX);
				//							continue;//ignorar este punto
				//						}
				//					
				//						double tan = deltaY/deltaX;//+Math.PI/2;
				//						rumbo = Math.atan(tan);
				//						rumbo = Math.toDegrees(rumbo);//como esto me da entre -90 y 90 le sumo 90 para que me de entre 0 180
				//						rumbo = 90-rumbo;
				//
				//						/**
				//						 * 
				//						 * deltaX=0.0 ;deltaY=0.0
				//						 *	rumbo1=NaN
				//						 *	rumbo0=310.0
				//						 */
				//					
				//						if(rumbo.isNaN()){//como el avance en x es cero quiere decir que esta yerndo al sur o al norte
				//							if(deltaY>0){
				//								rumbo = 0.0;
				//							}else{
				//								rumbo=180.0;
				//							}
				//						}
				//
				//						if(deltaX<0){//si el rumbo estaba en el cuadrante 3 o 4 sumo 180 para volverlo a ese cuadrante
				//							rumbo = rumbo+180;
				//						}
				//						ci.setRumbo(rumbo);
				//
				//					}
				//				
				//					lastX=longLatPoint;
				//					Double alfa  = Math.toRadians(rumbo) + Math.PI / 2;
				//
				//					// convierto el ancho y la distancia a verctores longLat poder
				//					// operar con la posicion del dato
				//					Coordinate anchoLongLat = constructCoordinate(alfa,ancho);
				//					Coordinate distanciaLongLat = constructCoordinate(alfa+ Math.PI / 2,distancia);
				//
				//
				//					if(labor.getConfiguracion().correccionDemoraPesadaEnabled()){
				//						Double corrimientoPesada =	labor.getConfiguracion().getCorrimientoPesada();
				//						Coordinate corrimientoLongLat =constructCoordinate(alfa + Math.PI / 2,corrimientoPesada);
				//						// mover el punto 3.5 distancias hacia atras para compenzar el retraso de la pesada
				//
				//						longLatPoint = longLatPoint.getFactory().createPoint(new Coordinate(longLatPoint.getX()+corrimientoPesada*distanciaLongLat.x,longLatPoint.getY()+corrimientoPesada*distanciaLongLat.y));
				//						//utmPoint = utmPoint.getFactory().createPoint(new Coordinate(utmPoint.getX()-corrimientoLongLat.x,utmPoint.getY()-corrimientoLongLat.y));
				//					}
				//
				//					/**
				//					 * creo la geometria que corresponde a la feature tomando en cuenta si esta activado el filtro de distancia y el de superposiciones
				//					 */				
				//					//				Geometry utmGeom = createGeometryForHarvest(anchoLongLat,
				//					//						distanciaLongLat, utmPoint,pasada,altura,ci.getRindeTnHa());		
				//					Geometry longLatGeom = createGeometryForHarvest(anchoLongLat,
				//							distanciaLongLat, longLatPoint,pasada,altura,ci.getRindeTnHa());
				//					if(longLatGeom == null 
				//							//			|| geom.getArea()*ProyectionConstants.A_HAS*10000<labor.config.supMinimaProperty().doubleValue()
				//							){//con esto descarto las geometrias muy chicas
				//						//System.out.println("geom es null, ignorando...");
				//						continue;
				//					}
				//
				//					/**
				//					 * solo ingreso la cosecha al arbol si la geometria es valida
				//					 */
				//					boolean empty = longLatGeom.isEmpty();
				//					boolean valid = longLatGeom.isValid();
				//					boolean big = (longLatGeom.getArea()*ProyectionConstants.A_HAS>supMinimaHas);
				//					if(!empty
				//							&&valid
				//							&&big//esta fallando por aca
				//							){
				//
				//						//Geometry longLatGeom =	crsAntiTransform(utmGeom);//hasta aca se entrega la geometria correctamente
				//
				//						ci.setGeometry(longLatGeom);//FIXME aca ya perdio la orientacion pero tiene la forma correcta
				//					//	corregirRinde(ci);
				//
				//						labor.insertFeature(ci);//featureTree.insert(geom.getEnvelopeInternal(), cosechaFeature);
				//					} else{
				//						//	System.out.println("no inserto el feature "+featureNumber+" porque tiene una geometria invalida empty="+empty+" valid ="+valid+" area="+big+" "+geom);
				//					}

			} else { // no es point. Estoy abriendo una cosecha de poligonos.
				labor.insertFeature(si);
			}
		}// fin del for que recorre las cosechas por indice
		reader.close();

		labor.constructClasificador();
		runLater(this.getItemsList());
		updateProgress(0, featureCount);
	}

	@Override
	public ExtrudedPolygon  getPathTooltip( Geometry poly,SiembraItem siembraFeature,ExtrudedPolygon  renderablePolygon) {		
		double area = ProyectionConstants.A_HAS(poly.getArea());
		DecimalFormat df = new DecimalFormat("#,###.##");//$NON-NLS-2$
		df.setGroupingUsed(true);
		df.setGroupingSize(3);
		
		//densidad seeds/metro lineal
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.getString("ProcessSiembraMapTask.1")
				+ df.format(siembraFeature.getDosisML()) 
				+ Messages.getString("ProcessSiembraMapTask.2")); //$NON-NLS-1$ //$NON-NLS-2$

		Double seedsSup= siembraFeature.getDosisML()/labor.getEntreSurco();
		if(seedsSup<100) {//plantas por ha
			sb.append(df.format(seedsSup*ProyectionConstants.METROS2_POR_HA) 
					+ " s/"+ Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}else {
			sb.append(df.format(seedsSup) 
					+ " s/"+Messages.getString("ProcessSiembraMapTask.10")); //s/m2
		}
		//kg semillas por ha
		sb.append(Messages.getString("ProcessSiembraMapTask.3") 
				+ df.format(siembraFeature.getDosisHa()) 
				+ Messages.getString("ProcessSiembraMapTask.4")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert l
		sb.append( Messages.getString("ProcessSiembraMapTask.5") 
				+ df.format(siembraFeature.getDosisFertLinea()) 
				+ Messages.getString("ProcessSiembraMapTask.6")); //$NON-NLS-1$ //$NON-NLS-2$
		//fert costo
		sb.append( Messages.getString("ProcessSiembraMapTask.7") 
				+ df.format(siembraFeature.getImporteHa()) 
				+ Messages.getString("ProcessSiembraMapTask.8")); //$NON-NLS-1$ //$NON-NLS-2$

		if(area<1){
			sb.append( Messages.getString("ProcessSiembraMapTask.9")
					+df.format(area * ProyectionConstants.METROS2_POR_HA) 
					+ Messages.getString("ProcessSiembraMapTask.10")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			sb.append(Messages.getString("ProcessSiembraMapTask.11")
					+df.format(area ) 
					+ Messages.getString("ProcessSiembraMapTask.12")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return super.getExtrudedPolygonFromGeom(poly, siembraFeature,sb.toString(),renderablePolygon);	
	}

	protected  int getAmountMin(){return 0;} 
	protected  int gerAmountMax() {return 1;} 

}// fin del task

