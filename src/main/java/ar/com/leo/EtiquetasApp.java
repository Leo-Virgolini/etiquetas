package ar.com.leo;

import ar.com.leo.api.ml.MercadoLibreAPI;
import ar.com.leo.pickit.service.PickitGenerator;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class EtiquetasApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ar/com/leo/ui/MainView.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("Pickit y Etiquetas");
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/ar/com/leo/ui/icons8-productos-64.png")));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        MercadoLibreAPI.shutdownExecutors();
        PickitGenerator.shutdownExecutors();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
