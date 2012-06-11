package org.cytoscape.cpathsquared.internal;

import org.cytoscape.work.TaskManager;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.session.CyNetworkNaming;
import org.cytoscape.work.undo.UndoSupport;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.property.CyProperty;
import org.cytoscape.io.read.CyNetworkReaderManager;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;

import org.cytoscape.cpathsquared.internal.view.BinarySifVisualStyleFactory;
import org.cytoscape.cpathsquared.internal.CPath2Factory;



import org.osgi.framework.BundleContext;

import org.cytoscape.service.util.AbstractCyActivator;

import java.util.Properties;


public final class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}


	public void start(BundleContext bc) {

		CySwingApplication cySwingApplicationRef = getService(bc,CySwingApplication.class);
		TaskManager<?,?> taskManagerRef = getService(bc,TaskManager.class);
		OpenBrowser openBrowserRef = getService(bc,OpenBrowser.class);
		CyNetworkManager cyNetworkManagerRef = getService(bc,CyNetworkManager.class);
		CyApplicationManager cyApplicationManagerRef = getService(bc,CyApplicationManager.class);
		CyNetworkViewManager cyNetworkViewManagerRef = getService(bc,CyNetworkViewManager.class);
		CyNetworkReaderManager cyNetworkViewReaderManagerRef = getService(bc,CyNetworkReaderManager.class);
		CyNetworkNaming cyNetworkNamingRef = getService(bc,CyNetworkNaming.class);
		CyNetworkFactory cyNetworkFactoryRef = getService(bc,CyNetworkFactory.class);
		CyLayoutAlgorithmManager cyLayoutsRef = getService(bc,CyLayoutAlgorithmManager.class);
		UndoSupport undoSupportRef = getService(bc,UndoSupport.class);
		VisualMappingManager visualMappingManagerRef = getService(bc,VisualMappingManager.class);
		VisualStyleFactory visualStyleFactoryRef = getService(bc,VisualStyleFactory.class);
		VisualMappingFunctionFactory discreteMappingFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=discrete)");
		VisualMappingFunctionFactory passthroughMappingFactoryRef = getService(bc,VisualMappingFunctionFactory.class,"(mapping.type=passthrough)");
		CyProperty cytoscapePropertiesServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		
		BinarySifVisualStyleFactory binarySifVisualStyleUtil = new BinarySifVisualStyleFactory(
				visualStyleFactoryRef,
				visualMappingManagerRef,
				discreteMappingFactoryRef,
				passthroughMappingFactoryRef);
		
		// initialize the internal "God" static factory
		CPath2Factory.init(
				cySwingApplicationRef,
				taskManagerRef,
				openBrowserRef,
				cyNetworkManagerRef,
				cyApplicationManagerRef,
				cyNetworkViewManagerRef,
				cyNetworkViewReaderManagerRef,
				cyNetworkNamingRef,
				cyNetworkFactoryRef,
				cyLayoutsRef,
				undoSupportRef,
				binarySifVisualStyleUtil,
				visualMappingManagerRef,
				cytoscapePropertiesServiceRef);
		
		// register the service
		CPath2CytoscapeWebService cPathWebService = new CPath2CytoscapeWebService();
		registerAllServices(bc, cPathWebService, new Properties());
	}
}

