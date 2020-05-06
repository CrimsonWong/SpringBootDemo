package com.myproject.demo.ElasticSearch;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.RequestLine;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.XContentType;
import com.google.gson.JsonObject;


import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class ElasticSearchBean {
    public ElasticSearchBean() {}

    private RestClient client;

    JsonParser parser = new JsonParser();

    private void initConnection(){
        client = RestClient.builder(
                new HttpHost("localhost", 9200, "http"),
                new HttpHost("localhost", 9201, "http")).build();
    }

    private void closeConnection() throws IOException {
        client.close();
        client = null;
    }

    public String putCreateDoc(String ObjWithParent) throws IOException {
        initConnection();
        JsonObject sendObject;
        String[] splitInput = ObjWithParent.split("----");
        if(splitInput.length == 1) {
            //爷爷节点
            sendObject = parser.parse(splitInput[0]).getAsJsonObject();
            sendObject.add("relation_type", new JsonPrimitive("plan"));

            String objectId = sendObject.get("objectId").getAsString();
            // /wholeplan/_doc/{Id}
            Request request = new Request("PUT", "/wholeplan/_doc/"+objectId);
            request.setJsonEntity(sendObject.toString());
            Response response = client.performRequest(request);
            String responseBody = EntityUtils.toString(response.getEntity());
            closeConnection();
            return responseBody;
        }
        //儿子节点或孙子节点
        String obj = ObjWithParent.split("----")[0];
        String type = ObjWithParent.split("----")[1];
        String parentId = ObjWithParent.split("----")[2];

        sendObject = parser.parse(obj).getAsJsonObject();
        //封装关系信息
        JsonObject relation = new JsonObject();
        relation.add("name", new JsonPrimitive(type));
        relation.add("parent", new JsonPrimitive(parentId));
        sendObject.add("relation_type", relation);

        String objectId = sendObject.get("objectId").getAsString();

        // /wholeplan/_doc/{Id}?routing={}
        Request request = new Request("PUT", "/wholeplan/_doc/"+objectId+"?routing=12xvxc345ssdsds-508");
        request.setJsonEntity(sendObject.toString());
        Response response = client.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        closeConnection();
        return responseBody;
    }

    public String getDoc(String id) throws IOException {
        initConnection();
        Request request = new Request("GET", "/orders/_doc/" + id);
        Response response = client.performRequest(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        closeConnection();
        return responseBody;
    }

    public boolean delDoc(String id) throws IOException {
        try{
            String esId = id.split("_")[0];
            //特殊情况，删整个plan
            if(esId.equals("12xvxc345ssdsds-508")) {
                initConnection();
                //"/wholeplan/_doc/" + esId
                Request request = new Request("DELETE", "/wholeplan");
                Response response = client.performRequest(request);
                String responseBodyString = EntityUtils.toString(response.getEntity());
                //responseBody = parser.parse(responseBodyString).getAsJsonObject();
                closeConnection();
                return true;
            }
            //发请求拿id
            JsonObject returnObj = sendRequest("GET", esId);
            //拿到此id对应的Type
            String objectType = returnObj.get("_source").getAsJsonObject().get("relation_type").getAsJsonObject().get("name").getAsString();
            //使用query search拿到以该id对应obj的所有子obj
            JsonObject resultQuery = sendQueryRequest(esId, objectType);
            //拿到结果集
            JsonArray matchArr = resultQuery.get("hits").getAsJsonObject().get("hits").getAsJsonArray();
            if(matchArr.isJsonNull()) {
                sendRequest("DELETE",esId);
                return true;
            }
            //建立删除用的list并加入此id,和结果集中找到的此id下的子id
            List<String> deleteIdList = new ArrayList<>();
            deleteIdList.add(esId);
            for(int i=0; i<matchArr.size(); i++){
                String getId = matchArr.get(i).getAsJsonObject().get("_id").getAsString();
                deleteIdList.add(getId);
            }
            //遍历deleteList，删除结果
            for(int j=0; j<deleteIdList.size(); j++){
                String deleteId = deleteIdList.get(j);
                sendRequest("DELETE", deleteId);
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public JsonObject sendRequest(String action, String address){
        JsonObject responseBody;
        try{
            initConnection();
            //"/wholeplan/_doc/" + esId
            Request request = new Request(action, "/wholeplan/_doc/" + address + "?routing=12xvxc345ssdsds-508");
            Response response = client.performRequest(request);
            String responseBodyString = EntityUtils.toString(response.getEntity());
            responseBody = parser.parse(responseBodyString).getAsJsonObject();
            closeConnection();
        } catch(IOException io){
            System.out.println(io);
            return null;
        } catch(Exception e) {
            System.out.println(e);
            return null;
        }
        return responseBody;
    }

    public JsonObject sendQueryRequest(String id, String type){
        JsonObject responseBody;
        try{
            initConnection();
            //建立request
            Request request = new Request("GET", "/wholeplan/_search");
            //准备query body
            String queryBody = "{\n" +
                    "    \"query\": {\n" +
                    "        \"has_parent\" : {\n" +
                    "            \"parent_type\" : " + type + ",\n" +
                    "            \"query\" : {\n" +
                    "                \"match\" : {\"_id\" : " + id + "}\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";
            JsonObject sendObj = parser.parse(queryBody).getAsJsonObject();
            request.setJsonEntity(sendObj.toString());
            Response response = client.performRequest(request);
            String responseBodyString = EntityUtils.toString(response.getEntity());
            responseBody = parser.parse(responseBodyString).getAsJsonObject();
            closeConnection();
        } catch(IOException io){
            System.out.println(io);
            return null;
        } catch(Exception e) {
            System.out.println(e);
            return null;
        }
        return responseBody;
    }

    public void putRequestInController(String id) throws IOException {
        String esId = id.split("_")[0];
        //sendRequest("PUT",esId);
        delDoc("27283xvx9asdff-504");
        delDoc("27283xvx9sdf-507");
    }

}
