package gui.controller;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.geotools.data.FileDataStore;

import dao.Labor;
import dao.fertilizacion.FertilizacionLabor;
import dao.siembra.SiembraLabor;
import gui.FertilizacionConfigDialogController;
import gui.JFXMain;
import gui.Messages;
import gui.SiembraConfigDialogController;
import gui.nww.LaborLayer;
import gui.nww.LayerAction;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tasks.importar.ProcessFertMapTask;
import tasks.procesar.CrearSiembraDesdeFertilizacionTask;
import tasks.procesar.ExportarPrescripcionFertilizacionTask;
import tasks.procesar.UnirFertilizacionesMapTask;
import utils.FileHelper;

public class FertilizacionGUIController extends AbstractGUIController {
	public FertilizacionGUIController(JFXMain _main) {
		super(_main);
	}
	
	public void addFertilizacionesRootNodeActions() {
		List<LayerAction> rootNodeP = new ArrayList<LayerAction>();
		rootNodeP.add(new LayerAction((layer)->{
			doOpenFertMap(null);
			return "opened";	
		},Messages.getString("JFXMain.importar")));

		rootNodeP.add(new LayerAction(Messages.getString("JFXMain.unirFertilizaciones"),(layer)->{
			doUnirFertilizaciones();
			return "unidas";	
		},2));
		layerPanel.addAccionesClase(rootNodeP,FertilizacionLabor.class);
	}
	
	public void addAccionesFertilizacion(Map<Class<?>, List<LayerAction>> predicates) {
		List<LayerAction> fertilizacionesP = new ArrayList<LayerAction>();
		predicates.put(FertilizacionLabor.class, fertilizacionesP);
		/**
		 *Accion que permite ediytar una fertilizacion
		 */
		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.editFertAction"),(layer)->{			
			doEditFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "fertilizacion editada" + layer.getName(); 
		}));

		/**
		 * Accion permite exportar la labor como shp
		 */
		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.exportarFertPAction"),(layer)->{			
			doExportPrescripcionFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor Exportada" + layer.getName(); 
		}));

		fertilizacionesP.add(LayerAction.constructPredicate(Messages.getString("JFXMain.generarSiembraDeFertAction"),(layer)->{			
			doGenerarSiembraDesdeFertilizacion((FertilizacionLabor) layer.getValue(Labor.LABOR_LAYER_IDENTIFICATOR));
			return "labor siembraFertilziada Exportada" + layer.getName(); 
		}));
	}
	
	private void doEditFertilizacion(FertilizacionLabor cConfigured ) {
		Optional<FertilizacionLabor> cosechaConfigured=FertilizacionConfigDialogController.config(cConfigured);
		if(cosechaConfigured.isPresent()){
			cConfigured = cosechaConfigured.get();
			ProcessFertMapTask umTask = new ProcessFertMapTask(cConfigured);
			umTask.installProgressBar(progressBox);

			umTask.setOnSucceeded(handler -> {
				this.getLayerPanel().update(this.getWwd());
				umTask.uninstallProgressBar();
				this.wwjPanel.repaint();
				System.out.println(Messages.getString("JFXMain.283")); 
				playSound();
			});//fin del OnSucceeded						
			JFXMain.executorPool.execute(umTask);
		}
	}
	
	private void doGenerarSiembraDesdeFertilizacion(FertilizacionLabor fertilizacionLabor) {
		SiembraLabor labor = new SiembraLabor();
		LaborLayer layer = new LaborLayer();
		boolean directa = true;

		labor.setLayer(layer);
		labor.setNombre(fertilizacionLabor.getNombre()+" "+Messages.getString("JFXMain.255"));  
		Optional<SiembraLabor> siembraConfigured= SiembraConfigDialogController.config(labor);
		if(!siembraConfigured.isPresent()){//
			System.out.println(Messages.getString("JFXMain.256")); 
			labor.dispose();//libero los recursos reservados
			return;
		}		

		//Dialogo preguntar min y max a aplicar y dosis
		Alert minMaxDialog = new Alert(AlertType.CONFIRMATION);	

		NumberFormat df=Messages.getNumberFormat();
		TextField dc = new TextField(df.format(0));
		TextField min = new TextField(df.format(0));
		TextField max = new TextField(df.format(0));

		VBox vb = new VBox();
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.425")),dc)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.426")),min)); 
		vb.getChildren().add(new HBox(new Label(Messages.getString("JFXMain.427")),max)); 

		minMaxDialog.setGraphic(vb);
		minMaxDialog.setTitle(Messages.getString("JFXMain.303")); 
		minMaxDialog.setContentText(Messages.getString("JFXMain.304")); 
		//dateDialog.setHeaderText("Fecha Desde");
		minMaxDialog.initOwner(JFXMain.stage);
		Optional<ButtonType> res = minMaxDialog.showAndWait();
		Double minSem =null,maxSem=null, dosisC=null; 
		if(res.get().equals(ButtonType.OK)){
			try {
				dosisC=df.parse(dc.getText()).doubleValue();
				if(dosisC==0)dosisC=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				minSem=df.parse(min.getText()).doubleValue();
				if(minSem==0)minSem=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			try {
				maxSem=df.parse(max.getText()).doubleValue();
				if(maxSem<=0)maxSem=(double) 0;
			} catch (ParseException e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

		double dosisXha = dosisC;
		double dosisMin = minSem;
		double dosisMax = maxSem;

		//con esto se recibe la relacion si es directa o indirecta
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Tipo de Relacion");
		alert.setHeaderText("Confirmar Relacion Directa");
		alert.setContentText("Seleccione OK para una relacion directa");

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
			directa = true;
		} else {
			directa = false;		
		}

		CrearSiembraDesdeFertilizacionTask siembraFert = new CrearSiembraDesdeFertilizacionTask(labor, fertilizacionLabor, dosisXha,dosisMin,dosisMax,directa);
		siembraFert.installProgressBar(progressBox);

		siembraFert.setOnSucceeded(handler -> {
			SiembraLabor ret = (SiembraLabor)handler.getSource().getValue();
			siembraFert.uninstallProgressBar();
			insertBeforeCompass(getWwd(), ret.getLayer());
			this.getLayerPanel().update(this.getWwd());

			playSound();
			viewGoTo(ret);
			System.out.println("SiembraFertTask succeded"); 
		});
		executorPool.execute(siembraFert);
	}
	
	//permitir al ususario definir el formato. para siembra fertilizada necesita 3 columnas
	//en la linea, al costado de la linea, siembra
	public void doExportPrescripcionFertilizacion(FertilizacionLabor laborToExport) {
		String nombre = laborToExport.getNombre();
		File shapeFile =  FileHelper.getNewShapeFile(nombre);

		ExportarPrescripcionFertilizacionTask ept = new ExportarPrescripcionFertilizacionTask(laborToExport, shapeFile); 
		ept.installProgressBar(progressBox);

		ept.setOnSucceeded(handler -> {
			laborToExport.getLayer().setEnabled(false);
			File ret = (File)handler.getSource().getValue();
			playSound();
			ept.uninstallProgressBar();
			this.doOpenFertMap(Collections.singletonList(ret));
		});
		executorPool.execute(ept);		
	}
	/**
	 * accion ejecutada al presionar el boton openFile Despliega un file
	 * selector e invoca la tarea que muestra el file en pantalla
	 */
	public void doOpenFertMap(List<File> files) {
		List<FileDataStore> stores =FileHelper.chooseShapeFileAndGetMultipleStores(files);
		if (stores != null) {
			for(FileDataStore store : stores){//abro cada store y lo dibujo en el harvestMap individualmente
				FertilizacionLabor labor = new FertilizacionLabor(store);
				labor.setLayer(new LaborLayer());
				Optional<FertilizacionLabor> cosechaConfigured= FertilizacionConfigDialogController.config(labor);
				if(!cosechaConfigured.isPresent()){//
					System.out.println(Messages.getString("JFXMain.308")); 
					continue;
				}							

				ProcessFertMapTask umTask = new ProcessFertMapTask(labor);
				umTask.installProgressBar(progressBox);

				umTask.setOnSucceeded(handler -> {
					FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
					insertBeforeCompass(getWwd(), ret.getLayer());
					this.getLayerPanel().update(this.getWwd());
					umTask.uninstallProgressBar();
					viewGoTo(ret);

					System.out.println(Messages.getString("JFXMain.309")); 
					playSound();
				});//fin del OnSucceeded
				JFXMain.executorPool.execute(umTask);
			}//fin del for stores
		}//if stores != null
	}

	private void doUnirFertilizaciones() {
		List<FertilizacionLabor> fertilizacionesAUnir = main.getFertilizacionesSeleccionadas();//si no hago esto me da un concurrent modification exception al modificar layers en paralelo
		UnirFertilizacionesMapTask umTask = new UnirFertilizacionesMapTask(fertilizacionesAUnir);
		umTask.installProgressBar(progressBox);
		umTask.setOnSucceeded(handler -> {
			FertilizacionLabor ret = (FertilizacionLabor)handler.getSource().getValue();
			if(ret.getLayer()!=null){
				insertBeforeCompass(getWwd(), ret.getLayer());
				this.getLayerPanel().update(this.getWwd());
			}
			umTask.uninstallProgressBar();
			viewGoTo(ret);
			System.out.println(Messages.getString("JFXMain.287")); 
			playSound();
		});//fin del OnSucceeded						
		JFXMain.executorPool.execute(umTask);
	}
	
}
