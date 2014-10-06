package com.netflix.elasticcar.identity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.elasticcar.configuration.IConfiguration;
import com.netflix.elasticcar.utils.RetryableCallable;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the central place to create and consume the identity of
 * the instance
 * 
 */
@Singleton
public class InstanceManager {

	private static final Logger logger = LoggerFactory
			.getLogger(InstanceManager.class);
	private final IElasticCarInstanceFactory instanceFactory;
    private final IMembership membership;
    private final IConfiguration config;
	private ElasticCarInstance myInstance;
	private List<ElasticCarInstance> instanceList;

	@Inject
	public InstanceManager(IElasticCarInstanceFactory instanceFactory, IMembership membership,
			IConfiguration config) throws Exception {

		this.instanceFactory = instanceFactory;
		this.membership = membership;
		this.config = config;
		init();
	}

	private void init() throws Exception {
		logger.info("***Deregistering Dead Instance");
		new RetryableCallable<Void>() 
		{
			@Override
			public Void retriableCall() throws Exception 
			{
				deregisterInstance(instanceFactory,config);
				return null;
			}
		}.call();
		
		logger.info("***Registering Instance");
		myInstance = new RetryableCallable<ElasticCarInstance>() 
		{
			@Override
			public ElasticCarInstance retriableCall() throws Exception 
			{
				ElasticCarInstance instance = registerInstance(instanceFactory,config);
				return instance;
			}
		}.call();
		logger.info("ElasticCarInstance Details = "+myInstance.toString());
	}

	private ElasticCarInstance registerInstance(
			IElasticCarInstanceFactory instanceFactory, IConfiguration config) throws Exception {
		return instanceFactory
				.create(config.getAppName(),
						config.getDC() + "." + config.getInstanceId(),
						config.getInstanceId(), config.getHostname(),
						config.getHostIP(), config.getRac(), config.getDC(), config.getASGName(), null);
	}

	private void deregisterInstance(
			IElasticCarInstanceFactory instanceFactory, IConfiguration config) throws Exception {
	    final List<ElasticCarInstance> allInstances = getInstanceList();
	    List<String> asgInstances = membership.getRacMembership();
	    for (ElasticCarInstance dead : allInstances)
	    {
	      // test same region and is it is alive.
	    	  // TODO: Provide Config prop to choose same DC/Region
	      if (!dead.getAsg().equals(config.getASGName()) || !dead.getAvailabilityZone().equals(config.getRac()) || asgInstances.contains(dead.getInstanceId()))
	        continue;
	      logger.info("Found dead instances: " + dead.getInstanceId());
	      instanceFactory.delete(dead);
	    }
	}

	public ElasticCarInstance getInstance()
	{
		return myInstance;
	}
	
	public List<ElasticCarInstance> getAllInstances()
	{
		return getInstanceList();
	}

    private List<ElasticCarInstance> getInstanceList()
    {
        List<ElasticCarInstance> _instances = new ArrayList<ElasticCarInstance>();

        if(config.amITribeNode())
        {
            String[] clusters = StringUtils.split(config.getCommaSeparatedClustersForTribeNode(), ",");
            assert (clusters.length != 0) : "One or more clusters needed";

            for(String clusterName : clusters)
                _instances.addAll(instanceFactory.getAllIds(clusterName));

        }else
            _instances = instanceFactory.getAllIds(config.getAppName());

        if(config.isDebugEnabled())
        {
            for(ElasticCarInstance instance:_instances)
                logger.debug(instance.toString());
        }
        return _instances;
    }

    public boolean isMaster()
    {
        //For Non-dedicated deployments, Return True (Every Node can be a master)
        return (!config.isAsgBasedDedicatedDeployment() || config.getASGName().toLowerCase().contains("master"));
    }
}
