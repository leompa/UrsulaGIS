package tasks;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpIOExceptionHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;

import dao.config.Configuracion;
import gui.JFXMain;
import gui.Messages;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;


public class UpdateTask  extends Task<File>{
	private static final String UPDATE_URL ="https://www.ursulagis.com/update/";//TODO cambiar a https
	//private static final String UPDATE_URL = "http://localhost:5000/update/";
	private static final String TASK_CLOSE_ICON = "/gui/event-close.png";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;


	//	private static final Logger logger = LoggerFactory.getLogger(UpdateTask.class);//sl4j

	public static String lastVersionURL=null;
	//	
	//	public UpdateTask(){
	//	}
	private static String lastVersionNumber;

	public File call()  {
		//lastVersionURL="http://s3-sa-east-1.amazonaws.com/ursulagis/downloads/UrsulaGIS0.2.18.jar";
		System.out.println("descargando: "+lastVersionURL);
		GenericUrl url = new GenericUrl(UpdateTask.lastVersionURL);
		HttpRequestFactory requestFactory = createRequestFactory();
		File fout=null;
		try {
			HttpRequest request = requestFactory.buildGetRequest(url);

			HttpResponse response = request.execute();
			GenericUrl reqUrl = response.getRequest().getUrl();
			List<String> parts = reqUrl.getPathParts();
			String fileName = parts.get(parts.size()-1);

			InputStream is = response.getContent();

			//ubico el archivo en appdata/ursulagis
			fout = new File(Configuracion.ursulaGISFolder+File.separator+fileName);

			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fout));
			byte[] bytesIn = new byte[4096];
			int read = 0;
			HttpHeaders headers = response.getHeaders();
			//	System.out.println("headers: "+headers.getCacheControl());
			long ava = headers.getContentLength();
			int readTot=0;
			//			long iniT = System.currentTimeMillis();
			//			long nowT = iniT;
			//			double rate=0;

			NumberFormat dc = NumberFormat.getInstance();
			dc.setGroupingUsed(true);

			while ((read = readRetry(is, bytesIn)) != -1) {//java.net.SocketTimeoutException: Read timed out
				//		System.out.println("read ="+read);
				readTot+=read;
				//				nowT = System.currentTimeMillis();
				//				rate = read/(nowT-iniT+1);
				//				iniT=nowT;

				//	System.out.println("read "+dc.format(readTot)+" out of "+dc.format(ava)+" "+dc.format(rate)+" KB/s");

				super.updateProgress(readTot,ava);//readTot, ava);
				bos.write(bytesIn, 0, read);
			}

			is.close();
			bos.close();
			//Executes the specified string command in a separate process.

			// ProcessBuilder pb = new ProcessBuilder(fout.getPath());
			//pb.start();

		} catch (Exception e) {
			e.printStackTrace();
			//			bos.close();
			//			is.close();
			if(fout!=null)fout.delete();
			fout=null;
		} 
		if(fout!=null){
			instalarNuevaVersion(fout);

		}
		return fout;
	}
//	
//	public static void main(String[] args){
//		UpdateTask ut = new UpdateTask();
//		String msiPath ="C:\\Users\\quero\\AppData\\Roaming\\UrsulaGIS\\UrsulaGIS-0.2.18.msi";
//
//		ut.instalarNuevaVersion(new File(msiPath));
//	}

	private void instalarNuevaVersion(File fout) {
		File bat = new File(fout.getParentFile().getPath()+File.separator+"install.bat");
		try {
			FileWriter fw = new FileWriter(bat);
			String uninstall =  "msiexec.exe /x "+fout.getPath()+" /q \r\n";
			fw.write(uninstall);
			String install =  "msiexec.exe /i "+fout.getPath()+" \r\n";
			fw.write(install);
			fw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		//opcion 1
//fout es un msi entonces solo lo ejecuto

		try {
//		String uninstall = "msiexec.exe /x ";// UrsulaGIS-0.2.18.msi;
//			String executar = uninstall+fout.getPath()+" /q";
			System.out.println("ejecutando: "+bat.getAbsolutePath());
			//XXX parece que no se instala bien sobre otras versiones o no tiene permiso por que no copia los exe
			//TODO probar ejecutar con "Elevate.exe "+bat.getAbsolutePath();
		 Runtime.getRuntime().exec(bat.getAbsolutePath());
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
//		try {
//			String install = "msiexec.exe /i ";// UrsulaGIS-0.2.18.msi
//				String executar = install+fout.getPath();
//				System.out.println("ejecutando: "+executar);
//				Runtime.getRuntime().exec(executar);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//opcion 2 fout es un jar entonces cambio UrsulaGIS.cfg para que ejecute el nuevo jar y modifico el nombre de la carpeta con las dependencias
//requere permisos elevados y posiblemente que la aplicacion este cerrada!
//			try {
//				CustomProperties configProp = new CustomProperties();
//
//				FileReader reader = new FileReader("UrsulaGIS.cfg");
//				configProp.load(reader);
//
//				reader.close();
//
//				configProp.setProperty("app.mainjar", fout.getName());
//				configProp.setProperty("app.version",UpdateTask.lastVersionNumber );
//
//
//				FileWriter writer = new FileWriter("UrsulaGIS.cfg");
//				configProp.store(writer,"");//Calendar.getInstance().getTime().toString());
//				writer.close();
//			} catch (IOException e) {
//
//				e.printStackTrace();
//			}

//TODO editar UrsulaGIS.cfg 
//TODO reemplazar app.mainjar=.\UrsulaGIS0.2.18.jar
//TODO reemplazar app.version=0.2.18
	}


	private HttpRequestFactory createRequestFactory() {
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		//	JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						//	request.setParser(new JsonObjectParser(JSON_FACTORY));
						//request.setRetryOnExecuteIOException(true);
						request.setIOExceptionHandler(
								new HttpIOExceptionHandler(){
									@Override
									public boolean handleIOException(HttpRequest arg0, boolean arg1)
											throws IOException {
										// TODO Auto-generated method stub
										return true;
									}

								});
						request.setContentLoggingLimit(0);
						request.setConnectTimeout(60000);
					//	request.setReadTimeout(60000);
					//	request.setNumberOfRetries(200);
					}
				});
		return requestFactory;
	}


	private int readRetry(InputStream is, byte[] bytesIn) {
		int read =-1;
		for(int i=0;i<10;i++){
			try{
				read= is.read(bytesIn);
				return read;
			}catch(Exception e){
				System.out.println("fallo read "+i);
			}
		}//fin del for trate de leer 10 veces.
		return -1;
	}


	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Actualizando");
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(TASK_CLOSE_ICON));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}

	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}

	/**
	 * hacer un llamado a www.ursulagis.com/update y chequear la ultima version con esta version
	 * actualiza la variable de lastVersion
	 * @return si la ultima version es mas grande que esta version devolver true
	 */
	public static boolean isUpdateAvailable() {
		GenericUrl url = new GenericUrl(UPDATE_URL);//"http://www.ursulagis.com/update");// "http://www.lanacion.com.ar");
		url.put("VERSION", JFXMain.VERSION);
		
		DecimalFormat dc = new DecimalFormat("0,000");
		dc.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
		dc.setGroupingUsed(true);
		
		String userString = dc.format(Math.random()*1000000);
		Configuracion conf = Configuracion.getInstance();
		String usr = conf.getPropertyOrDefault("USER", userString);//si no existia la clave se crea una nueva
		conf.save();
		url.put("USER", usr);
		
		System.out.println("calling url=> "+url);
		//http://localhost:5000/update?VERSION=0.2.26&USER=693,468
		//http://www.ursulagis.com/update?VERSION=0.2.20
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						//						  request.setConnectTimeout(0);
						//					      request.setReadTimeout(0);
					}
				});

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse response = request.execute();		
			
			GenericJson content = null;
			try{
				content = response.parseAs(GenericJson.class);//FIXME Unexpected character ('w' (code 119)): was expecting comma to separate OBJECT entries
				UpdateTask.lastVersionNumber =(String) content.get("lastVersionNumber");

				String message = (String)content.get("mensaje");
				if(message!=null){
					showWelcomeMessage(message);				
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			if(versionToDouble(lastVersionNumber)>versionToDouble(JFXMain.VERSION)){
				UpdateTask.lastVersionURL =(String)content.get("lastVersionURL");
				return true;	
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}	
		return false;
	}

	/**
	 * metodo que muestra el mensaje de bienvenida
	 * @param message
	 */
	private static void showWelcomeMessage(String message) {
		
		Platform.runLater(()->{		    
			WebView webView = new WebView();
			// webView.setPrefSize(600, 400);
			webView.autosize();
			WebEngine engine = webView.getEngine();
			engine.loadContent(message);

			VBox v = new VBox();
			VBox.setVgrow(webView, Priority.ALWAYS);
			VBox.setMargin(webView, new Insets(10,10,10,10));
			v.getChildren().add(webView);
			Stage welcomeStage = new Stage();
			
			double height = webView.getPrefHeight();
			double width = webView.getPrefWidth();
			Scene scene = new Scene(v, width-150+60,height-100+90);
			welcomeStage.setScene(scene);
			welcomeStage.initOwner(JFXMain.stage);
			welcomeStage.getIcons().addAll(JFXMain.stage.getIcons());
			welcomeStage.setTitle(Messages.getString("UpdateTaskWelcome.Title"));//"Bienvenido!");
		//	welcomeStage.show(); // anulo el inicio para que no muestre nada 
			
//			engine.getLoadWorker().stateProperty().addListener((observableState, oldState, newState)->{
//				System.out.println("new state "+newState);
//				if(State.SUCCEEDED.equals(newState)) {
//					welcomeStage.show();
//				}
//			});			
		});
	}

	public static Double versionToDouble(String ver){
		ver= ver.replace(" dev", "");
		String[] v =ver.split("\\.");
		String ret = v[0]+".";
		for(int i=1;i<v.length;i++){
			ret=ret.concat(v[i]);
		}
		try{
			DecimalFormat dc = new DecimalFormat("0.####");
			dc.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(new Locale("EN")));
			dc.setGroupingUsed(true);
			//System.out.println("versionAsDouble "+ dc.parse(ret).doubleValue());//versionAsDouble 0.2241
			return dc.parse(ret).doubleValue();
			//return Double.parseDouble(ret);//ret contiene 0.224111 etc
		}catch(Exception e){
			e.printStackTrace();
			return -1.0;
		}
	}
}
