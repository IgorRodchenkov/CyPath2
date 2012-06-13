package org.cytoscape.cpathsquared.internal;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.cytoscape.io.webservice.NetworkImportWebServiceClient;
import org.cytoscape.io.webservice.SearchWebServiceClient;
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 * CPathSquared Web Service UI, integrated into the Cytoscape Web Services Framework.
 */
public final class CPath2CytoscapeWebService extends AbstractWebServiceGUIClient 
	implements NetworkImportWebServiceClient, SearchWebServiceClient
{
	// Display name of this client.
    private static final String DISPLAY_NAME = CPath2Factory.serverName + " Client";

    
    //TODO where it is used? (not in this GUI as far as I can see...)
	@Override
	public TaskIterator createTaskIterator(final Object query) {

		TaskIterator taskIterator = new TaskIterator(new Task() {
			@Override
			public void run(TaskMonitor taskMonitor) throws Exception {
				String idStrs[] = ((String) query).split(" "); //TODO consider using a Collection
				String ids[] = new String[idStrs.length];
				for (int i = 0; i < ids.length; i++) {
					ids[i] = idStrs[i].trim();
				}

				TaskIterator iterator = new TaskIterator(
						new ExecuteGetByUriTask(ids,
								CPath2Factory.downloadMode,
								CPath2Factory.serverName));
				CPath2Factory.getTaskManager().execute(iterator);
			}

			@Override
			public void cancel() {
			}
		});

		return taskIterator;
	}
    
    /**
     * Creates a new Web Services client.
     */
    public CPath2CytoscapeWebService() {
    	super(CPath2Factory.cPathUrl, DISPLAY_NAME, makeDescription());
    	
        JTabbedPane tabbedPane = new JTabbedPane();
    	JPanel searchPanel = CPath2Factory.createSearchPanel();
        tabbedPane.add("Search", searchPanel);
        tabbedPane.add("Top Pathways", CPath2Factory.createTopPathwaysPanel());
        tabbedPane.add("Options", CPath2Factory.createOptionsPane());
        tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent ev) {
		        JTabbedPane pane = (JTabbedPane)ev.getSource();
		        if(pane.getSelectedIndex() == 1)
		        	CPath2Factory.loadTopPathwaysOnce();
			}
		});
        
    	JPanel mainPanel = new JPanel();
        mainPanel.setPreferredSize(new Dimension (500,400));
        mainPanel.setLayout (new BorderLayout());
        mainPanel.add(tabbedPane, BorderLayout.CENTER);       
        searchPanel.requestFocusInWindow();

    	gui = mainPanel;
    }

    private static String makeDescription() {
        String desc = CPath2Factory.INFO_ABOUT;
        desc = desc.replaceAll("<span class='bold'>", "<B>");
        desc = desc.replaceAll("</span>", "</B>");
        return "<html><body>" + desc + "</body></html>";
	}

}
