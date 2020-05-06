package com.myproject.demo.Controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.google.gson.*;
import com.myproject.demo.ElasticSearch.ElasticSearchBean;
import com.myproject.demo.Service.CrudService;
import com.myproject.demo.Service.ETagService;
import com.myproject.demo.Service.TokenService;
import com.nimbusds.jose.JOSEException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.springframework.web.context.request.WebRequest;


/**
 *
 * @Title: UserRestController
 * @author shaodong
 *
 */
@RestController
@RequestMapping(value = "/api")
public class WorkController {

    private  final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private CrudService crudService;
    @Autowired
    private TokenService tokenService;
    @Autowired
    private ETagService eTagService;
    @Autowired
    private ElasticSearchBean elasticSearchBean;

    JsonParser parser = new JsonParser();
    String currentTag = "39c460e1ee996755db00431d6fcfd8e4";

    @ResponseBody
    @PostMapping(value = "/user", produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> addUser(@RequestBody @Valid String input,
                                     @RequestHeader(value="Authorization") String token) throws IOException, ParseException, JOSEException {
        //验证token
        if(!tokenService.validateToken(token.substring(7))) return new ResponseEntity<>("Token Invalid!", HttpStatus.BAD_REQUEST);

        logger.info("开始新增...");

        //验证JSON格式，使用Alibaba的JSON包
        if(!isJsonValid(input)) return new ResponseEntity<>("JSON Input Invalid!", HttpStatus.BAD_REQUEST);
        if(!JSONSchemaCheck(input)) return new ResponseEntity<>("JSON Schema Wrong!", HttpStatus.BAD_REQUEST);

        //将输入转换为JSON格式,存入Redis、建立elasticsearch索引
        JsonObject wholeObj = parser.parse(input).getAsJsonObject();
        boolean saved = crudService.saveJSONObj(wholeObj,"");

        //检查是否成功
        if(!saved) return new ResponseEntity<>("Add Failed! Check your JSON Format!", HttpStatus.BAD_REQUEST);

        //存储与建立检索成功，返回
        JsonObject result = new JsonObject();
        result.add("objectId", wholeObj.get("objectId"));
        result.add("objectType", wholeObj.get("objectType"));
        return new ResponseEntity<>(result.toString(), HttpStatus.OK);

    }

    @ResponseBody
    @DeleteMapping(value = "/user", produces = "application/json;charset=UTF-8")
    public HttpEntity<?> delete(@RequestParam(value = "objectId", required = true) String objectId,
                                @RequestHeader(value="Authorization") String token) throws IOException, ParseException, JOSEException {
        //验证token
        if(!tokenService.validateToken(token.substring(7))) return new ResponseEntity<>("Token Invalid!", HttpStatus.BAD_REQUEST);

        logger.info("开始删除...");

        boolean b =
                crudService.deleteJSON(objectId)
                &&
                        elasticSearchBean.delDoc(objectId)
                ;

        if(!b) new ResponseEntity<>("Delete Failed! Check your Id!", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>("Delete Success!", HttpStatus.OK);
    }

    @ResponseBody
    @GetMapping(value = "/user", produces = "application/json;charset=UTF-8")
    public HttpEntity<?> findByUserId(@RequestParam(value = "objectId", required = true) String objectId,
                                        @RequestHeader(value="Authorization") String token,
                                      WebRequest request ) throws ParseException, JOSEException {
        //验证token
        if(!tokenService.validateToken(token.substring(7))) return new ResponseEntity<>("Token Invalid!", HttpStatus.BAD_REQUEST);

        logger.info("开始查询所有数据...");

        //取出数据
        String result;
        try{
            result = crudService.retrieveJSONObjById(objectId).toString();
        } catch (NullPointerException e){
            return new ResponseEntity<>("Invalid Id!", HttpStatus.BAD_REQUEST);
        }

//        //验证数据是否存在
//        if(result == null)

        //验证eTag
        String ifNoneMatch = request.getHeader("If-None-Match");
        if(request.checkNotModified(ifNoneMatch)){
            return null;
        }
        //生成eTag
        String etag = eTagService.getMD5(result);
        return ResponseEntity
                .ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS))
                .eTag(etag)
                .body(result);
    }

    @ResponseBody
    @PatchMapping(value = "/user", produces = "application/json;charset=UTF-8")
    public HttpEntity<?> changeUser(@RequestBody @Valid String input,
                                    @RequestHeader(value="Authorization") String token,
                                    @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String eTag) throws ParseException, JOSEException {
        //验证token
        if(!tokenService.validateToken(token.substring(7))) return new ResponseEntity<>("Token Invalid!", HttpStatus.BAD_REQUEST);

        logger.info("开始patch操作");

        //验证JSON格式，使用Alibaba包
        if(!isJsonValid(input)) return new ResponseEntity<>("JSON Input Invalid!", HttpStatus.BAD_REQUEST);

        //String currentTag = "testEtag";
        if(eTag != null && eTag.equals(currentTag)) return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

        JsonElement originParse = parser.parse(input);
        JsonObject patchObj = originParse.getAsJsonObject();
        boolean b = crudService.patchJSON(patchObj);
        if(!b) return new ResponseEntity<>("Patch Failed!", HttpStatus.BAD_REQUEST);

        JsonArray arr = patchObj.getAsJsonArray("linkedPlanServices");
        JsonObject result = new JsonObject();
        for(int i=0; i<arr.size();i++) {
            JsonObject storeObj = arr.get(i).getAsJsonObject();
            result.add("objectId", storeObj.get("objectId"));
            result.add("objectType", storeObj.get("objectType"));
        }
        return ResponseEntity.ok().eTag(currentTag).body(result.toString());
    }

    /**
    @ResponseBody
    @PutMapping(value = "/user", produces = "application/json;charset=UTF-8")
    public HttpEntity<?> putUser(@RequestBody @Valid String input,
                                 @RequestHeader(value="Authorization") String token,
                                 @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String eTag) throws ParseException, JOSEException, IOException {
        //验证token
        if(!tokenService.validateToken(token.substring(7))) return new ResponseEntity<>("Token Invalid!", HttpStatus.BAD_REQUEST);

        logger.info("开始put操作");

        if(!isJSONValid(input)) return new ResponseEntity<>("JSON Input Invalid!", HttpStatus.BAD_REQUEST);

        if(eTag != null && eTag.equals(currentTag)) return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

        JsonObject putObj = parser.parse(input).getAsJsonObject();

        boolean b = crudService.putJSON(putObj);
        elasticSearchBean.putRequestInController("id");
        if(!b) return new ResponseEntity<>("Put Failed!", HttpStatus.BAD_REQUEST);

        JsonArray arr = putObj.getAsJsonArray("linkedPlanServices");
        JsonObject result = new JsonObject();
        for(int i=0; i<arr.size();i++) {
            JsonObject storeObj = arr.get(i).getAsJsonObject();
            result.add("objectId", storeObj.get("objectId"));
            result.add("objectType", storeObj.get("objectType"));
        }
        return ResponseEntity.ok().eTag(currentTag).body(result.toString());
    }
     */

    @ResponseBody
    @PostMapping(value = "/token", produces = "application/json;charset=UTF-8")
    public HttpEntity<?> generateToken() throws JOSEException {
        logger.info("开始生成令牌...");
        String s = tokenService.generateToken();

        JsonObject resultObj = new JsonObject();
        resultObj.add("token", parser.parse(s));
        if(s == null) return new ResponseEntity<>("Wrong!", HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(resultObj.toString(), HttpStatus.OK);
    }

    /** Json Format Check*/
    public boolean isJsonValid(String string){
        try {
            parser.parse(string);
        } catch (JsonIOException ex) {
            logger.info("Json Parse Exception:", ex);
            return false;
        }
        return true;
    }
    public boolean JSONSchemaCheck (String input) throws IOException {
        boolean flag = false;
        JsonNode inputNode = JsonLoader.fromString(input);
        JsonNode schemaNode = new JsonNodeReader().fromReader(new FileReader("C:\\Users\\crims\\IDEAProjects\\demo\\schema.json"));
        ProcessingReport report = JsonSchemaFactory.byDefault().getValidator().validateUnchecked(schemaNode, inputNode);
        if (report.isSuccess()) {
            // 校验成功
            System.out.println("校验成功！");
            flag = true;
        }else {
            System.out.println("校验失败！");
            Iterator<ProcessingMessage> it = report.iterator();
            while(it.hasNext()){
                System.out.println(it.next());
            }
        }
        return flag;
    }
}

