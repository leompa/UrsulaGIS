package dao.fertilizacion;

import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.geotools.feature.SchemaException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import dao.Clasificador;
import dao.LaborItem;
import dao.Labor;
import dao.LaborConfig;
import dao.config.Configuracion;
import dao.config.Fertilizante;
import dao.cosecha.CosechaConfig;
import dao.cosecha.CosechaItem;
import dao.cosecha.CosechaLabor;
import dao.siembra.SiembraItem;
import dao.siembra.SiembraLabor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FertilizacionLabor extends Labor<FertilizacionItem> {
	public static final String COLUMNA_KG_HA = "Kg_FertHa";
	public static final String COLUMNA_PRECIO_FERT = "Precio Kg Fert";
	public static final String COLUMNA_PRECIO_PASADA = "Precio labor/Ha";	
	public static final String COLUMNA_IMPORTE_HA = "importe_ha";

	private static final String FERTILIZANTE_DEFAULT = "FERTILIZANTE_DEFAULT";

	private static final String COSTO_LABOR_FERTILIZACION = "costoLaborFertilizacion";

	public StringProperty colKgHaProperty;

	public Property<Fertilizante> fertilizanteProperty=null;

	public FertilizacionLabor() {
		initConfig();
	}

	public FertilizacionLabor(FileDataStore store) {
		super(store);
		initConfig();
	}

	private void initConfig() {
		List<String> availableColums = this.getAvailableColumns();		
		Configuracion properties = getConfigLabor().getConfigProperties();

		colKgHaProperty = initStringProperty(FertilizacionLabor.COLUMNA_KG_HA,properties,availableColums);
		colAmount= new SimpleStringProperty(FertilizacionLabor.COLUMNA_KG_HA);//Siempre tiene que ser el valor al que se mapea segun el item para el outcollection

			

		String fertKEY = properties.getPropertyOrDefault(FertilizacionLabor.FERTILIZANTE_DEFAULT,
				Fertilizante.FOSFATO_DIAMONICO_DAP);
		fertilizanteProperty = new SimpleObjectProperty<Fertilizante>(Fertilizante.fertilizantes.get(fertKEY));//values().iterator().next());
		fertilizanteProperty.addListener((obs, bool1, bool2) -> {
			properties.setProperty(FertilizacionLabor.FERTILIZANTE_DEFAULT,
					bool2.getNombre());
		});
	}


	@Override
	public String getTypeDescriptors() {
		/*
		 * 	getCantFertHa(),
				getPrecioFert(),
				getPrecioPasada(),
				getImporteHa()
		 */
		String type = FertilizacionLabor.COLUMNA_KG_HA + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_FERT + ":Double,"
				+ FertilizacionLabor.COLUMNA_PRECIO_PASADA + ":Double,"
				+ FertilizacionLabor.COLUMNA_IMPORTE_HA + ":Double";
		return type;
	}

	@Override
	public FertilizacionItem constructFeatureContainer(SimpleFeature next) {
		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainer(fi,next);
		fi.setDosistHa( LaborItem.getDoubleFromObj(next
				.getAttribute(colKgHaProperty.get())));
		setPropiedadesLabor(fi);
		return fi;
	}

	public void setPropiedadesLabor(FertilizacionItem fi){
		fi.setPrecioInsumo(this.precioInsumoProperty.get());
		fi.setCostoLaborHa(this.precioLaborProperty.get());	
	}

	@Override
	public FertilizacionItem constructFeatureContainerStandar(
			SimpleFeature next, boolean newIDS) {
		FertilizacionItem fi = new FertilizacionItem(next);
		super.constructFeatureContainerStandar(fi,next,newIDS);

		fi.setDosistHa( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_KG_HA)));		
//		fi.setPrecioInsumo( LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_PRECIO_FERT)));
//		fi.setCostoLaborHa(LaborItem.getDoubleFromObj(next.getAttribute(COSTO_LABOR_FERTILIZACION)));
//		fi.setImporteHa(LaborItem.getDoubleFromObj(next.getAttribute(COLUMNA_IMPORTE_HA)));
		setPropiedadesLabor(fi);
		return fi;
	}

	@Override
	protected DoubleProperty initPrecioLaborHaProperty() {
		return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}

	
	@Override
	protected DoubleProperty initPrecioInsumoProperty() {
		return initDoubleProperty(FertilizacionLabor.COLUMNA_PRECIO_FERT,  "0", config.getConfigProperties());
	//	return initDoubleProperty(FertilizacionLabor.COSTO_LABOR_FERTILIZACION,"0",config.getConfigProperties());
	}
	
	@Override
	public LaborConfig getConfigLabor() {
		if(config==null){
			config = new FertilizacionConfig();
		}
		return config;
	}



}
