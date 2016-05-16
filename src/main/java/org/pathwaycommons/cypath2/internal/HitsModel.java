package org.pathwaycommons.cypath2.internal;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.cytoscape.work.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cpath.client.CPathClient.Direction;
import cpath.client.util.CPathException;
import cpath.service.GraphType;
import cpath.service.jaxb.SearchHit;
import cpath.service.jaxb.SearchResponse;
import cpath.service.jaxb.TraverseEntry;
import cpath.service.jaxb.TraverseResponse;

/**
 * Contains information regarding the currently selected set of interaction bundles.
 *
 */
final class HitsModel extends Observable {

    private SearchResponse response;
    
    final String title;
    
    final Map<String, Integer> numHitsByTypeMap =  new ConcurrentSkipListMap<String, Integer>();
    final Map<String, Integer> numHitsByOrganismMap = new ConcurrentSkipListMap<String, Integer>();
    final Map<String, Integer> numHitsByDatasourceMap = new ConcurrentSkipListMap<String, Integer>();       
    final Map<String, String> hitsSummaryMap = new ConcurrentHashMap<String, String>();
    final Map<String, Collection<NvpListItem>> hitsPathwaysMap = new ConcurrentHashMap<String, Collection<NvpListItem>>();
    final Map<String, String> hitsDetailsMap = new ConcurrentHashMap<String, String>();

    // full-text search query parameter
    volatile String searchFor = "Interaction"; 
    // advanced (graph or multiple items import) query parameter 
    volatile GraphType graphType = null;
    volatile Direction direction;
    
	private static final Logger LOGGER = LoggerFactory.getLogger(HitsModel.class);
    
    public HitsModel(String title, TaskManager taskManager) 
    {
		this.title = title;
	}
    
    public int getNumRecords() {
        if (response != null && !response.isEmpty()) {
            return response.getSearchHit().size();
        } else {
            return -1;
        }
    }

    
    /**
     * Refresh the model and notify all observers INFO_ABOUT it's changed.
     * 
     * @param response
     */
	public synchronized void update(final SearchResponse response) {
		this.response = response;
		
		numHitsByTypeMap.clear();
		numHitsByOrganismMap.clear();
		numHitsByDatasourceMap.clear();
		hitsSummaryMap.clear();
		hitsPathwaysMap.clear();
		hitsDetailsMap.clear();
		
		// get and save the summary info for all hits at once
		ExecutorService exec = Executors.newFixedThreadPool(10);
		for (final SearchHit record : response.getSearchHit())
		{
			exec.submit(new Runnable() {		
				@Override
				public void run() {
					summarize(record);
				}
			});
		}

		exec.shutdown(); //prevents new jobs
		try {
			exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted unexpectedly!");
		}
		
		// notify observers (panels and jlists)
		HitsModel.this.setChanged();
		HitsModel.this.notifyObservers(response);
	}

    
    public SearchResponse getSearchResponse() {
        return response;
    }

    
	private void summarize(final SearchHit hit) {       
		// do catalog (by type, organism, source) -
		String type = hit.getBiopaxClass();
        Integer count = numHitsByTypeMap.get(type);
        if (count != null) {
        	numHitsByTypeMap.put(type, count + 1);
        } else {
        	numHitsByTypeMap.put(type, 1);
        }
        
        for(String org : hit.getOrganism()) {
        	Integer i = numHitsByOrganismMap.get(org);
            if (i != null) {
                numHitsByOrganismMap.put(org, i + 1);
            } else {
            	numHitsByOrganismMap.put(org, 1);
            }
        }
        
        for(String ds : hit.getDataSource()) {
        	Integer i = numHitsByDatasourceMap.get(ds);
            if (i != null) {
                numHitsByDatasourceMap.put(ds, i + 1);
            } else {
            	numHitsByDatasourceMap.put(ds, 1);
            }
        }
				
		// get/create and show hit's summary
		final String uri = hit.getUri();
		StringBuilder html = new StringBuilder();
		html.append("<html>")
			//a link to be intercepted and replaced with a well-formed cpath2 query later
			.append("<h2><a href='").append(uri).append("'>Click to extract and import this network")
			.append("</a></h2>")
			.append("<h3>BioPAX Object:</h3>")
			.append("<strong>Type:</strong> <em>").append(hit.getBiopaxClass()).append("</em><br/>")
			.append("<strong>URI :</strong> ").append(uri).append("<br/>");		
		
		if (hit.getName() != null)
			html.append("<strong>Name:</strong> ").append(hit.getName()).append("<br/>");
		
		String primeExcerpt = hit.getExcerpt();
		if (primeExcerpt != null)
			html.append("<h3>Excerpt:</h3>")
				.append("<span class='excerpt'>")
					.append(primeExcerpt)
						.append("</span><br/>");
		
		//get datasource URIs
		List<String> items = hit.getDataSource();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Data sources:</h3>");
			for(String it : items) {
				String name = App.uriToDatasourceNameMap.get(it);
				name = (name == null || name.isEmpty()) ? it : name;
				html.append(name).append("<br/>");
			}
		}
		
		//get organism URIs
		items = hit.getOrganism();
		if (items != null && !items.isEmpty()) {
			html.append("<h3>Organisms:</h3>");
			for(String it : items) {
				String name = App.uriToOrganismNameMap.get(it);
				name = (name == null || name.isEmpty()) ? it : name;
				html.append(name).append("<br/>");
			}
		}
		
		html.append("</html>");		
		hitsSummaryMap.put(uri, html.toString());		
	}


	/**
	 * Gets additional parent pathways' and members'
	 * names and counts from the server, etc., and store/cache 
	 * in a temporary map to re-use in the future.
	 * 
	 * @param hit
	 * @return content - details about the search hit
	 */
	public String fetchDetails(final SearchHit hit) {
        	
		// get/create and show hit's summary
		final String uri = hit.getUri();
		
		if(hitsDetailsMap.containsKey(uri))
			return hitsDetailsMap.get(uri);
				
		StringBuilder html = new StringBuilder("<html>");
		
		//shortcut when the hit is just an unused protein/molecule reference from the Warehouse
		if(searchFor.equalsIgnoreCase("EntityReference")) {
			TraverseResponse res = traverse("EntityReference/entityReferenceOf", Collections.singleton(uri));
			if(res == null || res.isEmpty()) {
				html.append("This standard ").append(hit.getBiopaxClass())
					.append(" is not part of any bio-network currently loaded in the server.</html>");
				hitsDetailsMap.put(uri, html.toString());
				return hitsDetailsMap.get(uri);
			}
		}		
		
		//list parent pathways		
		if(!hit.getPathway().isEmpty() && //and unless the only parent is this same pathway
			!( hit.getPathway().size() == 1 && uri.equalsIgnoreCase(hit.getPathway().get(0))) ) 
		{
			html.append("<h3>Parent Pathways:</h3><ul>");
			final Set<String> ppwUris = new HashSet<String>(hit.getPathway()); // a hack for not unique URIs (a cpath2 indexing bug...)
			TraverseResponse ppwNames = traverse("Named/displayName", ppwUris);
			if (ppwNames != null) {
				Map<String, String> ppwUriToNameMap = new HashMap<String, String>();
				for (TraverseEntry e : ppwNames.getTraverseEntry()) {
					String name = (e.getValue().isEmpty()) ? e.getUri() : e.getValue().get(0);
					ppwUriToNameMap.put(e.getUri(), name);
				}
				// add parent pathways's components counts
				TraverseResponse ppwComponents = traverse("Pathway/pathwayComponent", ppwUris); // this gets URIs of pathways
				for (TraverseEntry e : ppwComponents.getTraverseEntry()) {
					String name = ppwUriToNameMap.get(e.getUri());
					ppwUriToNameMap.put(e.getUri(), name + " (" + e.getValue().size() + " processes)");
				}
				
				// create html links
				for(String ppwUri : ppwUriToNameMap.keySet()) {
					html.append("<li><a href='").append(ppwUri).append("'>")
						.append(ppwUriToNameMap.get(ppwUri)).append("</a></li>");
				}				
			}
			html.append("</ul>");		
		}			
		
		// list members (if any)
		String path = null;
		if("Pathway".equalsIgnoreCase(hit.getBiopaxClass()))
			path = "Pathway/pathwayComponent";
		else if("Complex".equalsIgnoreCase(hit.getBiopaxClass()))
			path = "Complex/component";
		else if(searchFor.equalsIgnoreCase("Interaction"))
			path = "Interaction/participant";
		else if(searchFor.equalsIgnoreCase("PhysicalEntity"))
			path = "PhysicalEntity/memberPhysicalEntity";
		else if(searchFor.equalsIgnoreCase("EntityReference"))
			path = "EntityReference/memberEntityReference";		
		assert (path != null);

		// request members's names
		TraverseResponse members = traverse(path + ":Named/displayName", 
				Collections.singleton(uri));
		if (members == null) // no names? - get uris then	
			members = traverse(path, Collections.singleton(uri));
		
		if (members != null) {
			List<String> values  = members.getTraverseEntry().get(0).getValue();
			if (!values.isEmpty())
				html.append("<h3>Contains ").append(values.size())
				.append(" (direct) members:</h3>")
				.append(StringUtils.join(values, "<br/>"));
		}
		
		html.append("</html>");	
		
		//store for re-using later
		hitsDetailsMap.put(uri, html.toString());
				
		return hitsDetailsMap.get(uri);
	}	
	
	
	private TraverseResponse traverse(String path, Set<String> uris) {
		if(LOGGER.isDebugEnabled())
	   		LOGGER.debug("traverse: path=" + path);
	   		
        TraverseResponse res = null;
		try {
			res = App.client.createTraverseQuery()
				.propertyPath(path)
				.sources(uris)
				.result();
		} catch (CPathException e) {
			LOGGER.error("traverse: " + path + 
				" failed; uris:" + uris.toString(), e);
		}
				
       	return res;
	}

}