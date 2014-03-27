package org.geotools.gis;

import javax.swing.UIManager;

public class App {
	 
    public static void main(String[] args) throws Exception {
    	
    	UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    	
    	GUI program = new GUI();
    	program.create();
    }
}