package com.hellohealth.apigateway.service.router;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import com.hellohealth.apigateway.config.NacosConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;


/**
 *
 * 通过nacos下发动态路由配置,监听Nacos中gateway-route配置
 *
 */
@Component
@DependsOn({"nacosConfig"}) // 依赖于gatewayConfig bean
public class DynamicRouteServiceImplByNacos {

    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;


    private ConfigService configService;

    @PostConstruct
    public void init() {
        //log.info("gateway route init...");
        try{
            configService = initConfigService();
            if(configService == null){
                //log.warn("initConfigService fail");
                return;
            }
            String configInfo = configService.getConfig(NacosConfig.NACOS_ROUTE_DATA_ID, NacosConfig.NACOS_ROUTE_GROUP, NacosConfig.DEFAULT_TIMEOUT);
            //log.info("获取网关当前配置:\r\n{}",configInfo);
            System.out.println("获取网关当前配置:\r\n"+configInfo);
            List<RouteDefinition> definitionList = JSON.parseArray(configInfo, RouteDefinition.class);
            for(RouteDefinition definition : definitionList){
                //log.info("update route : {}",definition.toString());
                dynamicRouteService.add(definition);
            }
        } catch (Exception e) {
            //log.error("初始化网关路由时发生错误",e);
        }
        dynamicRouteByNacosListener(NacosConfig.NACOS_ROUTE_DATA_ID,NacosConfig.NACOS_ROUTE_GROUP);
    }

    /**
     * 监听Nacos下发的动态路由配置
     * @param dataId
     * @param group
     */
    public void dynamicRouteByNacosListener (String dataId, String group){
        try {
            configService.addListener(dataId, group, new Listener()  {
                @Override
                public void receiveConfigInfo(String configInfo) {
                    //log.info("进行网关更新:\n\r{}",configInfo);
                    System.out.println("进行网关更新:\n\r"+configInfo);
                    List<RouteDefinition> definitionList = JSON.parseArray(configInfo, RouteDefinition.class);
                    //log.info("update route : {}",definitionList.toString());
                    dynamicRouteService.updateList(definitionList);
                }
                @Override
                public Executor getExecutor() {
                    //log.info("getExecutor\n\r");
                    return null;
                }
            });
        } catch (NacosException e) {
            //log.error("从nacos接收动态路由配置出错!!!",e);
        }
    }

    /**
     * 初始化网关路由 nacos config
     * @return
     */
    private ConfigService initConfigService(){
        try{
            Properties properties = new Properties();
            properties.setProperty("serverAddr",NacosConfig.NACOS_SERVER_ADDR);
            properties.setProperty("namespace",NacosConfig.NACOS_NAMESPACE);
            return configService= NacosFactory.createConfigService(properties);
        } catch (Exception e) {
            //log.error("初始化网关路由时发生错误",e);
            System.out.println("初始化网关路由时发生错误");
            return null;
        }
    }
}