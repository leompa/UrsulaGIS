package dao.config;

import java.util.HashMap;
import java.util.Map;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Semilla {
	public static final String SEMILLA_DE_TRIGO = "Semilla de Trigo";
	public static final String SEMILLA_DE_SOJA = "Semilla de Soja";
	public static final String SEMILLA_DE_MAIZ = "Semilla de Maiz";
	private StringProperty nombre = new SimpleStringProperty();
	private Property<Cultivo> productoProperty=new SimpleObjectProperty<Cultivo>();//values().iterator().next());;

	public static Map<String,Semilla> semillas = new HashMap<String,Semilla>();
	static{																		
		semillas.put(SEMILLA_DE_MAIZ,new Semilla(SEMILLA_DE_MAIZ,Cultivo.cultivos.get(Cultivo.MAIZ)));	
		semillas.put(SEMILLA_DE_SOJA,new Semilla(SEMILLA_DE_SOJA,Cultivo.cultivos.get(Cultivo.SOJA)));
		semillas.put(SEMILLA_DE_TRIGO,new Semilla(SEMILLA_DE_TRIGO,Cultivo.cultivos.get(Cultivo.TRIGO)));

	}

	public Semilla(String _nombre, Cultivo producto) {
		nombre.set(_nombre);
		productoProperty.setValue(producto);
	}

	public String getNombre(){
		return this.nombre.get();
	}

	public void setNombre(String n){
		this.nombre.set(n);
	}

	public Cultivo getCultivo(){
		return this.productoProperty.getValue();
	}

	public void setCultivo(Cultivo cultivo){
			this.productoProperty.setValue(cultivo);
	}

	/**
	 * @return the nombre
	 */
	public StringProperty getNombreProperty() {
		return nombre;
	}

	/**
	 * @param nombre the nombre to set
	 */
	public void setNombreProperty(StringProperty nombre) {
		this.nombre = nombre;
	}

	@Override
	public String toString() {
		return nombre.getValue();
	}
}
