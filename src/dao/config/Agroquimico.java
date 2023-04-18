package dao.config;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import dao.ordenCompra.Producto;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(value = AccessLevel.PUBLIC)
@Entity @Access(AccessType.FIELD)
@NamedQueries({
	@NamedQuery(name=Agroquimico.FIND_ALL, query="SELECT o FROM Agroquimico o ORDER BY lower(o.nombre)") ,
	@NamedQuery(name=Agroquimico.FIND_NAME, query="SELECT o FROM Agroquimico o where o.nombre = :name") ,
}) 
public class Agroquimico extends Producto implements Comparable<Agroquimico>{
	public static final String FIND_ALL="Agroquimico.findAll";
	public static final String FIND_NAME="Agroquimico.findName";
	
	public static Map<String,Agroquimico> getAgroquimicosDefault(){
		HashMap<String,Agroquimico> agroquimicos = new HashMap<String,Agroquimico>();
		agroquimicos.put("RoundUp(lts)",new Agroquimico("RoundUp(lts)"));	
		agroquimicos.put("Superwet(lts)",new Agroquimico("Superwet(lts)"));
		agroquimicos.put("Atrazina(lts)",new Agroquimico("Atrazina(lts)"));
		agroquimicos.put("Cletodim(lts)",new Agroquimico("Cletodim(lts)"));
		
		agroquimicos.put("Rizospray extremo(lts)",new Agroquimico("Rizospray extremo(lts)"));
		agroquimicos.put("Benazolin(lts)",new Agroquimico("Benazolin(lts)"));
		agroquimicos.put("Fomesafen(lts)",new Agroquimico("Fomesafen(lts)"));
		agroquimicos.put("Glifosato 66%(lts)",new Agroquimico("Glifosato 66%(lts)"));
		agroquimicos.put("Dinotefuran(lts)",new Agroquimico("Dinotefuran(lts)"));
		agroquimicos.put("Abamectina 1,8(lts)",new Agroquimico("Abamectina 1,8(lts)"));
		agroquimicos.put("Coragen(lts)",new Agroquimico("Coragen(lts)"));
		agroquimicos.put("Opera(lts)",new Agroquimico("Opera(lts)"));		
		agroquimicos.put("Haloxifop 90% (Galant max)(lts)",new Agroquimico("Haloxifop 90% (Galant max)(lts)"));	
		return agroquimicos;
	}	
	
	public Agroquimico() {
	}

	
	public Agroquimico(String _nombre) {
		nombre=_nombre;
	}

	@Override
	public String toString() {
		return nombre;
	}
	
	@Override
	public int compareTo(Agroquimico p) {
		return super.compareTo(p);	
	}
}
