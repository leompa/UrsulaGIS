package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import lombok.Data;


//La informaci�n acerca del cultivo modelado debe contener:
//o Fecha de siembra
//o Fecha estimada de cosecha
//o Duraci�n aproximada de cada etapa fenol�gica
//o Profundidad radicular en cada etapa fenol�gica
//o Consumo h�drico en cada etapa fenol�gica
@Data
@Entity //@Access(AccessType.PROPERTY)
@NamedQueries({
	@NamedQuery(name=Cultivo.FIND_ALL, query="SELECT c FROM Cultivo c ORDER BY lower(c.nombre)") ,
	@NamedQuery(name=Cultivo.FIND_NAME, query="SELECT o FROM Cultivo o where o.nombre = :name") ,
	@NamedQuery(name=Cultivo.COUNT_ALL, query="SELECT COUNT(o) FROM Cultivo o") ,
	
}) 
public class Cultivo implements Comparable<Cultivo>{
	public static final String COUNT_ALL="Cultivo.countAll";
	public static final String FIND_ALL="Cultivo.findAll";
	public static final String FIND_NAME = "Cultivo.findName";
	
//	public static final String GIRASOL = "Girasol";
//	public static final String SOJA = "Soja";
//	public static final String TRIGO = "Trigo";
//	public static final String MAIZ = "Maiz";
//	public static final String SORGO = "Sorgo";
//	public static final String CEBADA = "Cebada";
	
	@Id @GeneratedValue
	private Long id=null;
	
	private String nombre =new String();
	
	//es lo que absorve (kg) la planta para producir una tonelada de grano seco
	private Double absN=new Double(0);
	private Double absP=new Double(0);
	private Double absK=new Double(0);
	private Double absS=new Double(0);
	private Double absCa, absMg, absB, absCl, absCo, absCu, absFe, absMn, absMo, absZn;
	
	
	//mm absorvidos de agua por tn de grano producido
	private Double absAgua=new Double(0);
	private Double aporteMO=new Double(0);
	
	//es lo que se lleva el grano por cada TN 
	private Double extN=new Double(0);
	private Double extP=new Double(0);
	private Double extK=new Double(0);
	private Double extS=new Double(0);
	private Double extCa, extMg, extB, extCl, extCo, extCu, extFe, extMn, extMo, extZn;
	
	private Double rindeEsperado=new Double(0);
	private Double ndviRindeCero=new Double(0);
	
	private Boolean estival = true;
	private Double semPorBolsa = 1.0;
	
//	private Double tasaCrecimientoPendiente=new Double(0);
//	private Double tasaCrecimientoOrigen=new Double(0);
	

	public Cultivo() {
		aporteMO=new Double(0);
		estival = true;
	}
	
	public Cultivo(String _nombre) {
		super();
		this.nombre=_nombre;
	}

	@Override
	public int compareTo(Cultivo arg0) {
		return this.nombre.compareTo(arg0.nombre);
	}
	
	@Override
	public String toString() {
		return nombre;
	}


	public boolean isEstival() {
		return this.estival;
	}


}

