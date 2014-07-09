package com.netflix.elasticcar.utils;

import com.netflix.elasticcar.identity.ElasticCarInstance;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EsUtils 
{
    private static final Logger logger = LoggerFactory.getLogger(EsUtils.class);
    private static final String HOST_NAME = "host_name";
    private static final String ID = "id";
    private static final String APP_NAME = "app_name";
    private static final String INSTANCE_ID = "instance_id";
    private static final String AVAILABILITY_ZONE = "availability_zone";
    private static final String PUBLIC_IP = "public_ip";
    private static final String DC = "dc";
    private static final String UPDATE_TIME = "update_time";
    private static final String HTTP_TAG = "http://";
    private static final String URL_PORT_SEPARATOR = ":";
    private static final String ELASTICSEARCH_HTTP_PORT = "7104";
    private static final String URL_PATH_SEPARATOR = "/";
    private static final String URL_QUERY_SEPARATOR = "?";
    private static final String REPOSITORY_VERIFICATION_PARAM = "_snapshot";
    private static final String SNAPSHOT_COMPLETION_PARAM = "wait_for_completion=true";
    private static final String DEFAULT_SNAPSHOT_IGNORE_AVAILABLE_PARAM = "true";
    private static final char PATH_SEP = File.separatorChar;
    private static final String S3_REPO_DATE_FORMAT = "yyyyMMdd";
    private static final DateTimeZone currentZone = DateTimeZone.UTC;



    @SuppressWarnings("unchecked")
	public static JSONObject transformEsCarInstanceToJson(List<ElasticCarInstance> instances)
    {
    		JSONObject esJsonInstances = new JSONObject();
    		
    		for(int i=0;i<instances.size();i++)
    		{
    	   		JSONArray esJsonInstance = new JSONArray();
    	   		
    	   	 	JSONObject jsInstance = new JSONObject();
    			jsInstance.put(HOST_NAME, instances.get(i).getHostName());
    			jsInstance.put(ID, instances.get(i).getId());
    			jsInstance.put(APP_NAME, instances.get(i).getApp());
    			jsInstance.put(INSTANCE_ID, instances.get(i).getInstanceId());
    			jsInstance.put(AVAILABILITY_ZONE, instances.get(i).getAvailabilityZone());
    			jsInstance.put(PUBLIC_IP, instances.get(i).getHostIP());
    			jsInstance.put(DC, instances.get(i).getDC());
    			jsInstance.put(UPDATE_TIME, instances.get(i).getUpdatetime());
    			esJsonInstance.add(jsInstance);
    			esJsonInstances.put("instance-"+i,jsInstance);
    		}    	
    		
    		JSONObject allInstances = new JSONObject();
    		allInstances.put("instances", esJsonInstances);
    		return allInstances;
    }
    
	public static List<ElasticCarInstance> getEsCarInstancesFromJson(JSONObject instances)
    {
		List<ElasticCarInstance> esCarInstances = new ArrayList<ElasticCarInstance>();
		
		JSONObject topLevelInstance = (JSONObject) instances.get("instances");
		
		for(int i=0;;i++)
		{
			if(topLevelInstance.get("instance-"+i) == null)
				break;
			JSONObject eachInstance = (JSONObject) topLevelInstance.get("instance-"+i);
			//Build ElasticCarInstance
			ElasticCarInstance escInstance = new ElasticCarInstance();
			escInstance.setApp((String) eachInstance.get(APP_NAME));
			escInstance.setAvailabilityZone((String) eachInstance.get(AVAILABILITY_ZONE));
			escInstance.setDC((String) eachInstance.get(DC));
			escInstance.setHostIP((String) eachInstance.get(PUBLIC_IP));
			escInstance.setHostName((String) eachInstance.get(HOST_NAME));
			escInstance.setId((String) eachInstance.get(ID));
			escInstance.setInstanceId((String) eachInstance.get(INSTANCE_ID));
			escInstance.setUpdatetime((Long) eachInstance.get(UPDATE_TIME));
			//Add to the list
			esCarInstances.add(escInstance);
		}
  		
    		return esCarInstances;
    }


    /**
     * Repository Name is Today's Date in yyyyMMdd format eg. 20140630
     * @return Repository Name
     */
    public static String getS3RepositoryName()
    {
        DateTime dt = new DateTime();
        DateTime dtGmt = dt.withZone(currentZone);
        return formatDate(dtGmt,S3_REPO_DATE_FORMAT);
    }

    public static String formatDate(DateTime dateTime, String dateFormat)
    {
        DateTimeFormatter fmt = DateTimeFormat.forPattern(dateFormat);
        return dateTime.toString(fmt);
    }


//    public static Set<String> getExistingRepositoryNames(String httpUrl) throws UnsupportedEncodingException, IllegalStateException, IOException, ParseException {
//        HttpGet repoResponse = new HttpGet(httpUrl);
//        HttpResponse response = client.execute(repoResponse);
//
//        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
//        StringBuilder builder = new StringBuilder();
//        for (String line = null; (line = reader.readLine()) != null;) {
//            builder.append(line).append("\n");
//        }
//        JSONParser jsonParser = new JSONParser();
//        JSONObject jsonObject = (JSONObject) jsonParser.parse(builder.toString());
//        if (jsonObject == null){
//            System.out.println("No Repositories exist,hence returning");
//            return null;
//        }
//
//        return jsonObject.keySet();
//    }
//
//
//    public static String getCreateS3RepositoryJsonBody(String bucket,
//                                                       String region, String clusterName) {
//
//        JSONObject urlSettingsParams = new JSONObject();
//        String createRepoJson = "";
//        try {
//
//            urlSettingsParams.put("bucket", bucket);
//            urlSettingsParams.put("region", region);
//            urlSettingsParams.put("base_path", clusterName
//                    + EsConfig.URL_PATH_SEPARATOR + getS3RepositoryName());
//
//            JSONObject urlParams = new JSONObject();
//            urlParams.put("type", "s3");
//            urlParams.put("settings", urlSettingsParams);
//            createRepoJson = urlParams.toString();
//        } catch (Exception ex) {
//            System.out.println("Exception caught during JSONObject Creation");
//            throw new RuntimeException(ex);
//        }
//
//        System.out.println("Create Repo Params -> " + createRepoJson);
//        return createRepoJson;
//    }
//
//    /**
//     * eg. http://0.0.0.0:7104/_snapshot/s3_repo
//     * @param hostName
//     * @param S3_Repo_Name
//     * @return
//     */
//    public static String getUrlToCreateS3Repository(String hostName,String S3_Repo_Name){
//        StringBuilder sb = new StringBuilder();
//        sb.append(EsConfig.HTTP_TAG);
//        sb.append(hostName);
//        sb.append(EsConfig.URL_PORT_SEPARATOR);
//        sb.append(EsConfig.ELASTICSEARCH_HTTP_PORT);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(EsConfig.REPOSITORY_VERIFICATION_PARAM);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(S3_Repo_Name);
//
//        System.out.println("REST Endpoint for Creating S3 Repository = "+sb.toString());
//        return sb.toString();
//    }
//
//    /**
//     * eg. http://ec2-50-19-28-170.compute-1.amazonaws.com:7104/_snapshot/
//     * @param hostName
//     * @param S3_Repo_Name
//     * @return
//     */
//    public static String getUrlToCheckIfRepoExists(String hostName,String S3_Repo_Name){
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(EsConfig.HTTP_TAG);
//        sb.append(hostName);
//        sb.append(EsConfig.URL_PORT_SEPARATOR);
//        sb.append(EsConfig.ELASTICSEARCH_HTTP_PORT);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(EsConfig.REPOSITORY_VERIFICATION_PARAM);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//
//        System.out.println("REST Endpoint to verify if Repository exists = "+sb.toString());
//
//        return sb.toString();
//    }
//
//    /*
//     * ec2-50-19-28-170.compute-1.amazonaws.com:7104/_snapshot/20140320/snapshot_1?wait_for_completion=true
//     *
//     * {
//     *  "indices": "chronos_test",
//     *  "ignore_unavailable": "true",
//     *  "include_global_state": false
//     * }
//     *
//     */
//    public static String getUrlToTakeSnapshot(String hostName,String S3_Repo_Name,String indices,boolean includeIndexNameInSnapshot){
//        String snapshotName = getSnapshotName(indices,includeIndexNameInSnapshot);
//        StringBuilder sb = new StringBuilder();
//        sb.append(EsConfig.HTTP_TAG);
//        sb.append(hostName);
//        sb.append(EsConfig.URL_PORT_SEPARATOR);
//        sb.append(EsConfig.ELASTICSEARCH_HTTP_PORT);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(EsConfig.REPOSITORY_VERIFICATION_PARAM);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(S3_Repo_Name);
//        sb.append(EsConfig.URL_PATH_SEPARATOR);
//        sb.append(snapshotName);
//        sb.append(EsConfig.URL_QUERY_SEPARATOR);
//        sb.append(EsConfig.SNAPSHOT_COMPLETION_PARAM);
//
//        System.out.println("***Snapshot Name = "+snapshotName);
//        System.out.println("REST Endpoint for Creating Snapshot = "+sb.toString());
//        return sb.toString();
//    }
//
//	/* {
//	 *   "indices": "index1,index2",
//	 *   "ignore_unavailable": "true",
//	 *   "include_global_state": false
//	 * }
//	 */
//    /**
//     *
//     * @param indices eg. index1,index2
//     * @param ignoreUnavailable
//     * @param includeGlobalState
//     * @return
//     */
//    public static String getTakeSnapshotJsonBody(String indices,
//                                                 String ignoreUnavailable, boolean includeGlobalState) {
//
//        JSONObject urlIndexParams = new JSONObject();
//        String ignore_unavailable = (ignoreUnavailable == null || ignoreUnavailable.isEmpty()) ? EsConfig.DEFAULT_SNAPSHOT_IGNORE_AVAILABLE_PARAM : ignoreUnavailable;
//        try {
//            if (indices != null && !indices.isEmpty() && !indices.toLowerCase().equals("all")) {
//                urlIndexParams.put("indices", indices);
//            }
//            urlIndexParams.put("ignore_unavailable", ignoreUnavailable);
//            urlIndexParams.put("include_global_state", includeGlobalState);
//
//            String takeSnapshotJson = urlIndexParams.toString();
//            System.out.println("Take Snapshot Params -> " + takeSnapshotJson);
//            return takeSnapshotJson;
//        } catch (Exception ex) {
//            System.out.println("Exception caught during JSONObject Creation");
//            throw new RuntimeException(ex);
//        }
//
//    }
}