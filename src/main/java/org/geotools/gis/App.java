package org.geotools.gis;

import javax.swing.UIManager;

public class App {
	
	public static MapViewer mapWindow;
	public static DataTables dataTables;
	public static DataController dataController;
	
    public static void main(String[] args) throws Exception {
    	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	
    	mapWindow = new MapViewer();
    	dataTables = new DataTables();
    	dataController = new DataController();

        App.mapWindow.addLayerFromFile("C:\\Users\\lol\\Desktop\\gis\\LTsventoji\\sven_SAV_P.shp");
    }
}