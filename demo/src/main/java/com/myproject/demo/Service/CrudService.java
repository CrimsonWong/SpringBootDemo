package com.myproject.demo.Service;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.myproject.demo.Dao.Dao;
import com.myproject.demo.Event.ESEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;


/**
 *
 * @Title: CrudService
 * @author shaodong
 *
 */
@Service
public class CrudService {

    private  final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private Dao dao;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    /** Add entity, id as key, the whole object as value, for demo 1 use*/
    public boolean addEntity(String objectId, JSONObject jsonObject) {
        boolean flag = false;
        try{
            String store = JSONObject.toJSONString(jsonObject);
            dao.addInputJSON(objectId, store);
            flag = true;
        }catch(Exception e){
            logger.error("新增失败!",e);
        }
        return flag;
    }
    public boolean deleteEntity(String objectId) {
        boolean flag = false;
        try{
            dao.deleteInputJSON(objectId);
            flag = true;
        }catch(Exception e){
            logger.error("删除失败!",e);
        }
        return flag;
    }
    public Object findByEntityId(String objectId) {
        return dao.findInputJSON(objectId);
    }

    /** for individually saving use, demo 2*/
    // relation: ----" "----" "
    public boolean saveJSONObj(JsonObject obj, String relation){
        //拿到此obj的id和type
        String outKey = getIdAndType(obj);
        Iterator iter = obj.keySet().iterator();
        //用于存Redis
        Map<String,String> outMap = new HashMap<String, String>();
        //用于存elasticsearch
        JsonObject outObj = new JsonObject();
        //遍历此jsonObj所有key
        while(iter.hasNext()){
            //当前key
            String attribute = iter.next().toString();
            //拿到这个key和它对应的parentKey
            String saveKey = outKey + "_" + attribute;
            //key的值是primitive,直接存入map,存入outObj
            if(obj.get(attribute).isJsonPrimitive()) {
                outMap.put(attribute,obj.get(attribute).getAsString());
                outObj.add(attribute,obj.get(attribute));
            }
            //key的值是另一个obj
            else if(obj.get(attribute).isJsonObject()) {
                //拿到这个obj的id和type
                JsonObject valObj = obj.get(attribute).getAsJsonObject();
                String saveValue = getIdAndType(valObj);
                Set saveSet = new HashSet();
                saveSet.add(saveValue);
                //存入Redis
                logger.info(saveKey + ": " + saveSet);
                dao.addInputJSON(saveKey,saveSet);
                //准备存入es，传递属性名和parentId
                String nextRelation = "----"+attribute+"----"+obj.get("objectId").getAsString();
                saveJSONObj(valObj,nextRelation);
            }
            //key的值是一个array
            else if(obj.get(attribute).isJsonArray()) {
                JsonArray valArr = obj.get(attribute).getAsJsonArray();
                Set saveSet = new HashSet();
                //遍历array里的每个元素，即obj，同上一个if操作
                for(int i=0; i<valArr.size();i++) {
                    JsonObject inObj = valArr.get(i).getAsJsonObject();
                    String saveValue = getIdAndType(inObj);
                    saveSet.add(saveValue);
                    String nextRelation = "----"+attribute+"----"+obj.get("objectId").getAsString();
                    saveJSONObj(inObj, nextRelation);
                }
                logger.info(saveKey + ": " + saveSet);
                dao.addInputJSON(saveKey,saveSet);
            }
            else logger.info("wrong");
        }
        //将这个key的primitive value存入redis
        logger.info(outKey + ": " + outMap);
        dao.addInputJSON(outKey,outMap);
        //Index事务
        if(relation.equals("")) creatQueue(outObj.toString());
        else creatQueue(outObj.toString() + relation);
        return true;
    }
    public String getIdAndType(JsonObject obj){
        return obj.get("objectId").getAsString() + "_" + obj.get("objectType").getAsString();
    }

    /** for retrieve use, demo 2*/
    public JsonObject retrieveJSONObjById(String objectId){
        Map<String,String> plan = (Map)dao.findInputJSON(objectId);
        if(plan==null) return null;
        //dao.findInputJSON(objectId + "_planCostShares");
        JsonObject result = new JsonObject();
        for(Map.Entry<String, String> entry : plan.entrySet()) {
            result.addProperty(entry.getKey(), entry.getValue());
        }
        if(dao.findInputJSON(objectId + "_linkedPlanServices") != null){
            Set set = (Set)dao.findInputJSON(objectId + "_linkedPlanServices");
            JsonArray array = new JsonArray();
            for (Object o : set) {
                JsonObject out = retrieveJSONObjById(o.toString());
                array.add(out);
            }
            result.add("linkedPlanServices", array);
        }
        if(dao.findInputJSON(objectId + "_planCostShares") != null){
            Set set = (Set)dao.findInputJSON(objectId + "_planCostShares");
            for (Object o : set) result.add("planCostShares", retrieveJSONObjById(o.toString()));
        }
        if(dao.findInputJSON(objectId + "_linkedService") != null){
            Set set = (Set)dao.findInputJSON(objectId + "_linkedService");
            for (Object o : set) result.add("linkedService", retrieveJSONObjById(o.toString()));
        }
        if(dao.findInputJSON(objectId + "_planserviceCostShares") != null){
            Set set = (Set)dao.findInputJSON(objectId + "_planserviceCostShares");
            for (Object o : set) result.add("planserviceCostShares", retrieveJSONObjById(o.toString()));
        }
        return result;
    }

    /** for delete use, demo 2*/
    public boolean deleteJSON(String objectId){
        try{
            dao.deleteInputJSON(objectId);
            if(dao.findInputJSON(objectId + "_linkedPlanServices") != null) dao.deleteInputJSON(objectId + "_linkedPlanServices");
            if(dao.findInputJSON(objectId + "_planCostShares") != null) dao.deleteInputJSON(objectId + "_planCostShares");
            if(dao.findInputJSON(objectId + "_linkedService") != null) dao.deleteInputJSON(objectId + "_linkedService");
            if(dao.findInputJSON(objectId + "_planserviceCostShares") != null) dao.deleteInputJSON(objectId + "_planserviceCostShares");
        } catch (Exception e){
            logger.info("删除错误：" + e);
            return false;
        }
        return true;
    }

    /** for patch use, demo 2*/
    public boolean patchJSON(JsonObject obj){
        boolean flag = false;
        JsonArray arr = obj.getAsJsonArray("linkedPlanServices");
        String target = "12xvxc345ssdsds-508_plan_linkedPlanServices";
        for(int i=0; i<arr.size();i++) {
            JsonObject storeObj = arr.get(i).getAsJsonObject();
            String newVal = getIdAndType(storeObj);
            try{
                saveJSONObj(storeObj,"----linkedPlanServices----12xvxc345ssdsds-508");
                Set set = (Set)dao.findInputJSON(target);
                set.add(newVal);
                dao.addInputJSON(target,set);
                flag = true;
            } catch (Exception e){
                logger.error("Patch失败!",e);
            }
        }
        return flag;
    }

    /** for patch use, demo 2*/
    public boolean putJSON(JsonObject obj){
        boolean flag = false;
        JsonArray arr = obj.getAsJsonArray("linkedPlanServices");
        String target = "12xvxc345ssdsds-508_plan_linkedPlanServices";
        for(int i=0; i<arr.size();i++) {
            JsonObject storeObj = arr.get(i).getAsJsonObject();
            String newVal = getIdAndType(storeObj);
            try{
                saveJSONObj(storeObj,"----linkedPlanServices----12xvxc345ssdsds-508");
                Set set = new HashSet();
                        //(Set)dao.findInputJSON(target);
                set.add(newVal);
                dao.addInputJSON(target,set);
                flag = true;
            } catch (Exception e){
                logger.error("Put失败!",e);
            }
        }
        return flag;
    }

    /** 创建存储队列事件，事件激活后监听器将监听到此事件并向elasticsearch进行index操作*/
    public void creatQueue(String ObjWithParent){
        ESEvent event = new ESEvent(this, ObjWithParent);
        eventPublisher.publishEvent(event);
    }

}

