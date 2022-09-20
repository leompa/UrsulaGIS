package tasks;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.ManyToOne;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.ArrayMap;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import dao.OrdenDeCompra.OrdenCompra;
import dao.config.Configuracion;
import dao.recorrida.Recorrida;
import api.StandardResponse;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import utils.DAH;


public class CotizarOdenDeCompraOnlineTask extends Task<String> {
	
	private static final String MMG_GUI_EVENT_CLOSE_PNG = "/gui/event-close.png";
	public static final String ZOOM_TO_KEY = "ZOOM_TO";

	public static final String BASE_URL = "https://www.ursulagis.com";
	//public static final String BASE_URL = "http://localhost:5000";
	public static final String INSERT_URL = BASE_URL+"/api/orden/insert/";
	//public static final String INSERT_URL = "http://localhost:5000/api/recorridas/insert/";
	private ProgressBar progressBarTask;
	private Pane progressPane;
	private Label progressBarLabel;
	private HBox progressContainer;

	
	private OrdenCompra ordenCompra =null;
	private String mail=null;
	
	
	public CotizarOdenDeCompraOnlineTask(OrdenCompra orden) {
		this.ordenCompra = orden;
		//this.mail=mail;
		System.out.println("compartiendo OrdenCompra "+orden);
		System.out.println("muestras "+orden.getItems().size());
	}

	@Override
	protected String call()  {
		try {
		// TODO call www.ursulagis.com/api/recorridas/insert/
		GenericUrl url = new GenericUrl(INSERT_URL);	
		
		Gson gson = new GsonBuilder().serializeNulls().setExclusionStrategies( getJSonStrategy()).create();
		
//		Gson gson = new GsonBuilder().create();
	//	 Gson gson = new Gson();
		    
	//	    Staff obj = new Staff();

		    // 1. Java object to JSON file
		//    gson.toJson(obj, new FileWriter("C:\\projects\\staff.json"));
		 //   recorrida.muestras.clear();
		System.out.println("convirtirndo OrdenCompra a json "+ordenCompra);
	  	String json_body = gson.toJson(ordenCompra, OrdenCompra.class);
	  	System.out.println("sending OrdenCompra "+ json_body);
	  	//String document_id = document.getId();
	  	//String resource_url = "https://api.mendeley.com/documents/" + document_id;
	  	//GenericUrl gen_url = new GenericUrl(resource_url);
	  	
	
	  	final HttpContent content = new ByteArrayContent("application/json", json_body.getBytes("UTF8") );
	 
		//final HttpContent req_content = new JsonHttpContent(new JacksonFactory(), content);

		HttpResponse response = makePostRequest(url,content);
		InputStream resContent = response.getContent();
		Reader reader = new InputStreamReader(resContent);

		StandardResponse standarResponse =  new Gson().fromJson(reader, StandardResponse.class);
		System.out.println("standarResponse = "+standarResponse);
		//StandardResponse standarResponse = response.parseAs(StandardResponse.class);
		//Recorrida r = new Gson().fromJson((String) resContent.get("data"), Recorrida.class);
		StandardResponse.StatusResponse status = standarResponse.getStatus();
		System.out.println("response status = "+status);
		if(StandardResponse.StatusResponse.SUCCESS.equals(status)) {
		//com.google.api.client.util.ArrayMap data =(ArrayMap) resContent.get("data");
			JsonElement data = standarResponse.getData();
		//Map<String,String> message = (Map<String, String>) resContent.get("data");
		//System.out.println("message "+message);
		if(data !=null) {
			// Failed to invoke public dao.OrdenDeCompra.Producto() with no args
			//FIXME cambiar estrategia de producto a JoinedTable y crear un descerializador que convierta de producto a productoGenerico o fertilizacion etc.
			OrdenCompra dbOrdenCompra = gson.fromJson(data, OrdenCompra.class);
			Long id = dbOrdenCompra.getId();
			DAH.save(dbOrdenCompra);//merge local recorrida
			//	Long id = dbRecorrida.getId();
			String dbUrl = dbOrdenCompra.getUrl();
		//java.math.BigDecimal id = (java.math.BigDecimal) data.get("id");
		//String prettyresponse = resContent.toPrettyString();
		//System.out.println("prettyresponse "+prettyresponse);
	
		/*
			prettyresponse {
		  "status" : "SUCCESS",
		  "data" : {
		    "id" : 4,
		    "nombre" : "",
		    "observacion" : "",
		    "posicion" : "",
		    "latitude" : 0.0,
		    "longitude" : 0.0,
		    "muestras" : [ ]
		  }
		}
		 */
		//String urlGoto = "https://www.ursulagis.com/api/recorridas/4/";
		String urlGoto =dbUrl;// "https://www.ursulagis.com/api/orden_compra/"+id+"/";
		return urlGoto;
		}
		return "status Success but data null";
		} else {//status is not Success
			String message =standarResponse.getMessage();
			return status+" "+message;
		}
		}catch(Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
		
	}

	private ExclusionStrategy getJSonStrategy() {
		ExclusionStrategy strategy = new ExclusionStrategy() {
			@Override
			public boolean shouldSkipClass(Class<?> arg0) {
				return false;
			}
			@Override
			public boolean shouldSkipField(FieldAttributes arg0) {
				if (arg0.getAnnotation(ManyToOne.class) != null)
					return false;
				if(arg0.getName().equals("ordenCompra")) {
					System.out.println("skipping ordenCompra in json "+arg0);
					return true;
				}

				return false;
			}
		};
		return strategy;
	}
	
	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makeGetRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);


					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildGetRequest(url);//(url);
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}
	
	/**
	 * metodo que ejecuta un request
	 * @param url
	 * @return HttResponse
	 */
	private HttpResponse makePostRequest(GenericUrl url,HttpContent req_content){
		HttpResponse response = null;
		HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

		JsonFactory JSON_FACTORY = new JacksonFactory();
		HttpRequestFactory requestFactory =
				HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
					@Override
					public void initialize(HttpRequest request) {
						request.setParser(new JsonObjectParser(JSON_FACTORY));
						request.setReadTimeout(0);
						request.setConnectTimeout(0);
						HttpHeaders headers = request.getHeaders();//USER=693,468
						headers.set("USER", Configuracion.getInstance().getPropertyOrDefault("USER", "nonefound"));
					}
				});//java.net.SocketException: Address family not supported by protocol family: connect

		try {
			HttpRequest request = requestFactory.buildPostRequest(url, req_content);//(url);
			response= request.execute();
		} catch (Exception e) {			
			e.printStackTrace();
			return null;// si no se pudo hacer el request devuelvo null. puede ser por falta de conexion u otra cosa
		}	
		return response;
	}
	
	public void installProgressBar(Pane progressBox) {
		this.progressPane= progressBox;
		progressBarTask = new ProgressBar();			
		progressBarTask.setProgress(0);

		progressBarTask.progressProperty().bind(this.progressProperty());
		progressBarLabel = new Label("Compartiendo Recorrida "+this.ordenCompra.getDescription());
		progressBarLabel.setTextFill(Color.BLACK);


		Button cancel = new Button();
		cancel.setOnAction(ae->{
			System.out.println("cancelando el ProcessMapTask");
			this.cancel();
			this.uninstallProgressBar();
		});
		Image imageDecline = new Image(getClass().getResourceAsStream(MMG_GUI_EVENT_CLOSE_PNG));
		cancel.setGraphic(new ImageView(imageDecline));

		//progressBarLabel.setStyle("-fx-color: black");
		progressContainer = new HBox();
		progressContainer.getChildren().addAll(cancel,progressBarLabel,progressBarTask);
		progressBox.getChildren().add(progressContainer);


	}
	public void uninstallProgressBar() {		
		progressPane.getChildren().remove(progressContainer);
	}
	
	
}
