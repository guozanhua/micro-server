package com.aol.micro.server.module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestListener;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import com.aol.cyclops.lambda.monads.SequenceM;
import com.aol.micro.server.FunctionalModule;
import com.aol.micro.server.FunctionalModuleLoader;
import com.aol.micro.server.auto.discovery.Rest;
import com.aol.micro.server.auto.discovery.RestResource;
import com.aol.micro.server.config.Classes;
import com.aol.micro.server.rest.jersey.JacksonFeature;
import com.aol.micro.server.rest.jersey.JerseyRestApplication;
import com.aol.micro.server.rest.jersey.JerseySpringIntegrationContextListener;
import com.aol.micro.server.servers.model.ServerData;
import com.aol.micro.server.web.filter.QueryIPRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public interface Module {
	
	default Consumer<HttpServer> getServerConfigManager(){
		return server->{};
	}
	default Consumer<ResourceConfig> getResourceConfigManager(){
		return rc->{};
	}
	default List<String> getPackages(){
		return ImmutableList.of();
	}
	
	default Map<String,String> getPropertyOverrides(){
		return Maps.newHashMap();
	}
	default Set<Class> getSpringConfigurationClasses(){
		return Sets.newHashSet(Classes.CORE_CLASSES.getClasses());
	}
	default List<Class> getRestResourceClasses() {
		return Arrays.asList(RestResource.class);
	}
	default List<Class> getRestAnnotationClasses() {
		return Arrays.asList(Rest.class);
	}
	
	default List<String> getDefaultJaxRsPackages(){
		List list = new ArrayList<>();
		list.addAll(SequenceM.fromStream(FunctionalModuleLoader.INSTANCE.functionalModules.get().stream())
				.filter(module -> module.servletContextListeners()!=null)
				.flatMapCollection(FunctionalModule::jaxRsPackages)
				
				.toList());
		return list;
	}
	
	default List<Class> getDefaultResources(){
		List list = new ArrayList<>();
		list.add(JacksonFeature.class);
		list.addAll(SequenceM.fromStream(FunctionalModuleLoader.INSTANCE.functionalModules.get().stream())
				.filter(module -> module.servletContextListeners()!=null)
				.flatMapCollection(FunctionalModule::jaxRsResources)
				.toList());
		return list;
	}
	
	default List<ServletContextListener> getListeners(ServerData data){
		List<ServletContextListener> list= new ArrayList<>();
		if(data.getRootContext() instanceof WebApplicationContext){
			list.add(new ContextLoaderListener((WebApplicationContext)data
					.getRootContext()));
		}
		list.add(new JerseySpringIntegrationContextListener(data));
		List<FunctionalModule> modules = FunctionalModuleLoader.INSTANCE.functionalModules.get();
		
		list.addAll(SequenceM.fromStream(modules.stream())
				.filter(module -> module.servletContextListeners()!=null)
				.flatMapCollection(FunctionalModule::servletContextListeners)
				.map(fn->fn.apply(data))
				.toList());
		
		return  ImmutableList.copyOf(list);
	}
	default List<ServletRequestListener> getRequestListeners(ServerData data){
		return ImmutableList.of();
	}
	default Map<String,Filter> getFilters(ServerData data) {
		return ImmutableMap.of("/*",new QueryIPRetriever());
	}
	default Map<String,Servlet> getServlets(ServerData data) {
		return ImmutableMap.of();
	}
	
	default  String getJaxWsRsApplication(){
		return JerseyRestApplication.class.getCanonicalName();
	}
	default String getProviders(){
		return "com.aol.micro.server.rest.providers";
	}
	
	public String getContext();
}
