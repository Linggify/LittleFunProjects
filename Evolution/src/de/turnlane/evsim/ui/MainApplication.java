package de.turnlane.evsim.ui;

import java.util.Timer;
import java.util.TimerTask;

import de.turnlane.evsim.World;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApplication extends Application{

	public static void main(String[] args) {
		//start application
		MainApplication.launch(args);
	}
	
	private Canvas m_canvas;
	private World m_world = new World();
	
	@Override
	public void start(Stage stage) throws Exception {
		m_canvas = new Canvas(800, 600);
		m_canvas.autosize();
		
		BorderPane layout = new BorderPane(m_canvas);
		
		m_world = new World();
		m_world.initialize(800, 600, 10, 0.7, 10);
		
		updateImage();
		
		Scene scene = new Scene(layout, 800, 600);
		stage.setScene(scene);
		stage.show();
	}
	
	public void updateImage() {
		m_world.renderWorldOutput(m_canvas);
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				m_world.tick();
				Platform.runLater(() -> updateImage());
				
			}
		}, 0);
	}
}
